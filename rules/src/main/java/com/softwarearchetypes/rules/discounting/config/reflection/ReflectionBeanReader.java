package com.softwarearchetypes.rules.discounting.config.reflection;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.predicates.AndPredicate;
import com.softwarearchetypes.rules.predicates.LogicalPredicate;
import com.softwarearchetypes.rules.predicates.NotPredicate;
import com.softwarearchetypes.rules.predicates.OrPredicate;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;

public class ReflectionBeanReader {

    private final Map<String, String> props;

    public ReflectionBeanReader(Map<String, String> props) {
        this.props = Objects.requireNonNull(props);
    }

    public <T> T readBean(String prefix, Class<T> expectedType) {
        String classKey = prefix + ".class";
        String className = props.get(classKey);

        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException(
                    "No entry '" + classKey + "' for type " + expectedType.getName());
        }

        Class<?> rawClass;
        try {
            rawClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Can not load class: " + className, e);
        }

        if (!expectedType.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException(
                    "Class " + className + " not compatibile with " + expectedType.getName());
        }

        @SuppressWarnings("unchecked")
        Class<? extends T> clazz = (Class<? extends T>) rawClass;

        // specjalne typy Value Object
        if (Money.class.isAssignableFrom(clazz)) {
            @SuppressWarnings("unchecked")
            T money = (T) readMoney(prefix);
            return money;
        }

        if (Percentage.class.isAssignableFrom(clazz)) {
            @SuppressWarnings("unchecked")
            T percentage = (T) readPercentage(prefix);
            return percentage;
        }

        if (LogicalPredicate.class.isAssignableFrom(clazz)) {
            @SuppressWarnings("unchecked")
            T predicate = (T) readLogicalPredicate(prefix);
            return predicate;
        }

        // rekord vs zwykła klasa
        if (clazz.isRecord()) {
            return instantiateRecord(prefix, clazz);
        } else {
            return instantiatePojo(prefix, clazz);
        }
    }

    private <T> T instantiateRecord(String prefix, Class<T> clazz) {
        var components = clazz.getRecordComponents();
        Class<?>[] paramTypes = new Class<?>[components.length];
        Object[] args = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            paramTypes[i] = component.getType();
            String argKey = prefix + ".arg" + i;
            args[i] = readValue(argKey, component.getType());
        }

        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create record " + clazz.getName(), e);
        }
    }

    private <T> T instantiatePojo(String prefix, Class<T> clazz) {
        Constructor<?> ctor = chooseConstructor(clazz);
        Parameter[] params = ctor.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            String paramName =
                    p.getName(); // wymaga -parameters przy kompilacji !!!!!!!!!!!!!!!!!!!
            Class<?> paramType = p.getType();

            String simpleKey = prefix + "." + paramName;

            // 1) simple type saved as prefix.paramName
            if (isSimpleType(paramType)) {
                String raw = props.get(simpleKey);
                if (raw == null) {
                    if (paramType.isPrimitive()) {
                        throw new IllegalArgumentException("No primitive type: " + simpleKey);
                    }
                    args[i] = null;
                } else {
                    args[i] = convertSimple(raw, paramType);
                }
                continue;
            }

            // 2) Money
            if (Money.class.isAssignableFrom(paramType)) {
                args[i] = readMoney(simpleKey);
                continue;
            }

            // 3) Percentage
            if (Percentage.class.isAssignableFrom(paramType)) {
                args[i] = readPercentage(simpleKey);
                continue;
            }

            // 4) LogicalPredicate – tree : .root and nX.*
            if (LogicalPredicate.class.isAssignableFrom(paramType)) {
                args[i] = readLogicalPredicate(simpleKey);
                continue;
            }

            // 5) nested beam, should have prefix.paramName.class
            if (props.containsKey(simpleKey + ".class")) {
                args[i] = readBean(simpleKey, paramType);
                continue;
            }

            if (paramType.isPrimitive()) {
                throw new IllegalArgumentException(
                        "No config for primitive param " + paramName + " (" + simpleKey + ")");
            } else {
                args[i] = null;
            }
        }

        try {
            @SuppressWarnings("unchecked")
            T instance = (T) ctor.newInstance(args);
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(
                    "Could not create " + clazz.getName() + " from prefix '" + prefix + "'", e);
        }
    }

    private Constructor<?> chooseConstructor(Class<?> clazz) {
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        if (ctors.length == 0) {
            throw new IllegalStateException(
                    "Class " + clazz.getName() + " has no public constructor");
        }
        if (ctors.length == 1) {
            ctors[0].setAccessible(true);
            return ctors[0];
        }
        // can add logic @Inject / @JsonCreator
        throw new IllegalStateException(
                "Class "
                        + clazz.getName()
                        + " has many constructors – specify it in ReflectionBeanReader");
    }

    private Money readMoney(String prefix) {
        String amountKey = prefix + ".money.amount";
        String currencyKey = prefix + ".money.currency";

        String amountStr = props.get(amountKey);
        String currCode = props.get(currencyKey);

        if (amountStr == null || currCode == null) {
            throw new IllegalArgumentException(
                    "No money data at: '"
                            + prefix
                            + "' (expected "
                            + amountKey
                            + " i "
                            + currencyKey
                            + ")");
        }

        BigDecimal amount = new BigDecimal(amountStr);

        return Money.of(amount, currCode);
    }

    private Percentage readPercentage(String prefix) {
        String key = prefix + ".percentage.value";
        String valueStr = props.get(key);
        if (valueStr == null) {
            throw new IllegalArgumentException("No percentage value at: '" + key + "'");
        }

        BigDecimal val = new BigDecimal(valueStr);

        return Percentage.of(val);
    }

    /**
     * prefix.root = n1 prefix.n1.type = AND / OR / NOT / LEAF prefix.n1.left = n2 prefix.n1.right =
     * n3 ...
     */
    public LogicalPredicate<?> readLogicalPredicate(String basePrefix) {
        String rootId = props.get(basePrefix + ".root");
        if (rootId == null || rootId.isBlank()) {
            return null;
        }
        return readLogicalNode(basePrefix, rootId);
    }

    private LogicalPredicate<?> readLogicalNode(String basePrefix, String nodeId) {
        String nodePrefix = basePrefix + "." + nodeId;
        String type = props.get(nodePrefix + ".type");
        if (type == null) {
            throw new IllegalArgumentException("No '" + nodePrefix + ".type' for node " + nodeId);
        }

        return switch (type) {
            case "AND" -> {
                String leftId = props.get(nodePrefix + ".left");
                String rightId = props.get(nodePrefix + ".right");
                if (leftId == null || rightId == null) {
                    throw new IllegalArgumentException("NO left/right for node AND: " + nodePrefix);
                }
                LogicalPredicate left = readLogicalNode(basePrefix, leftId);
                LogicalPredicate right = readLogicalNode(basePrefix, rightId);
                yield new AndPredicate<>(left, right);
            }
            case "OR" -> {
                String leftId = props.get(nodePrefix + ".left");
                String rightId = props.get(nodePrefix + ".right");
                if (leftId == null || rightId == null) {
                    throw new IllegalArgumentException("No left/right for node OR: " + nodePrefix);
                }
                LogicalPredicate left = readLogicalNode(basePrefix, leftId);
                LogicalPredicate right = readLogicalNode(basePrefix, rightId);
                yield new OrPredicate<>(left, right);
            }
            case "NOT" -> {
                String childId = props.get(nodePrefix + ".child");
                if (childId == null) {
                    throw new IllegalArgumentException("No child for node NOT: " + nodePrefix);
                }
                LogicalPredicate<?> child = readLogicalNode(basePrefix, childId);
                yield new NotPredicate<>(child);
            }
            case "LEAF" -> {
                // save as simple bean
                // nodePrefix.class = ...
                // nodePrefix.<paramName> = ...
                String className = props.get(nodePrefix + ".class");
                if (className == null) {
                    throw new IllegalArgumentException(
                            "No " + nodePrefix + ".class for predicate leaf");
                }
                try {
                    Class<?> leafClass = Class.forName(className);
                    Object bean =
                            instantiatePojo(
                                    nodePrefix, leafClass); // or instantiateRecord, if rekord
                    if (!(bean instanceof LogicalPredicate<?> lp)) {
                        throw new IllegalArgumentException(
                                "Leaf "
                                        + nodePrefix
                                        + " of class "
                                        + className
                                        + " does not implement LogicalPredicate");
                    }
                    yield lp;
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(
                            "Could not load leaf class: " + className, e);
                }
            }
            default ->
                    throw new IllegalArgumentException(
                            "Unknown type for logical node '"
                                    + type
                                    + "' for a prefix "
                                    + nodePrefix);
        };
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type)
                || Enum.class.isAssignableFrom(type)
                || type == Boolean.class
                || type == Character.class
                || type == LocalDate.class
                || type == LocalDateTime.class;
    }

    private Object readValue(String key, Class<?> targetType) {
        // simple types, without prefix
        if (isSimpleType(targetType)) {
            String raw = props.get(key);
            if (raw == null) {
                if (targetType.isPrimitive()) {
                    throw new IllegalArgumentException("No value for primitive: " + key);
                }
                return null;
            }
            return convertSimple(raw, targetType);
        }

        // expecting: typu key.class, key.money.*, key.percentage.*, ...
        if (Money.class.isAssignableFrom(targetType)) {
            return readMoney(key);
        }

        if (Percentage.class.isAssignableFrom(targetType)) {
            return readPercentage(key);
        }

        if (LogicalPredicate.class.isAssignableFrom(targetType)) {
            return readLogicalPredicate(key);
        }

        if (props.containsKey(key + ".class")) {
            return readBean(key, targetType);
        }

        // null or exception?
        if (targetType.isPrimitive()) {
            throw new IllegalArgumentException("No config for primitive: " + key);
        }
        return null;
    }

    private Object convertSimple(String raw, Class<?> targetType) {
        if (targetType == String.class) {
            return raw;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(raw);
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(raw);
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(raw);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(raw);
        }
        if (targetType == BigDecimal.class) {
            return new BigDecimal(raw);
        }
        if (targetType == BigInteger.class) {
            return new BigInteger(raw);
        }
        if (targetType.isEnum()) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Class<? extends Enum> enumType = (Class<? extends Enum>) targetType;
            return Enum.valueOf(enumType, raw);
        }
        if (targetType == LocalDate.class) {
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Illegal date  (LocalDate): " + raw, e);
            }
        }
        if (targetType == LocalDateTime.class) {
            try {
                return LocalDateTime.parse(raw);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Illegal date (LocalDateTime): " + raw, e);
            }
        }

        throw new IllegalArgumentException("Unsuported type: " + targetType.getName());
    }
}
