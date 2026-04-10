package com.softwarearchetypes.rules.discounting.config.reflection;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.predicates.AndPredicate;
import com.softwarearchetypes.rules.predicates.LogicalPredicate;
import com.softwarearchetypes.rules.predicates.NotPredicate;
import com.softwarearchetypes.rules.predicates.OrPredicate;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

public class ReflectionBeanWriter {

    public void writeBean(String prefix, Object bean, Map<String, String> out) {
        if (bean == null) {
            return;
        }
        Class<?> clazz = bean.getClass();

        out.put(prefix + ".class", clazz.getName());

        // special Value Objects

        if (bean instanceof Money money) {
            writeMoney(prefix, money, out);
            return;
        }

        if (bean instanceof Percentage percentage) {
            writePercentage(prefix, percentage, out);
            return;
        }

        if (bean instanceof LogicalPredicate<?> pred) {
            writeLogicalPredicate(prefix, pred, out);
            return;
        }

        if (clazz.isRecord()) {
            writeRecordArgs(prefix, bean, clazz, out);
        } else {
            writeConstructorArgs(prefix, bean, clazz, out);
        }
    }

    /**
     * clientPred.root = n1 clientPred.n1.type = AND / OR / NOT / LEAF clientPred.n1.left = n2
     * clientPred.n1.right = n3 ...
     *
     * <p>for LEAF: clientPred.nX.type = LEAF clientPred.nX.class = ... clientPred.nX.arg0 = ...
     */
    public void writeLogicalPredicate(
            String basePrefix, LogicalPredicate<?> root, Map<String, String> out) {
        if (root == null) {
            return;
        }
        NodeIdGenerator gen = new NodeIdGenerator();
        String rootId = gen.nextId();
        out.put(basePrefix + ".root", rootId);
        writeLogicalNode(basePrefix, rootId, root, out, gen);
    }

    // --- LOGIC TREE SERIALIZATION -----------------------------------------

    private void writeLogicalNode(
            String basePrefix,
            String nodeId,
            LogicalPredicate<?> predicate,
            Map<String, String> out,
            NodeIdGenerator gen) {
        String nodePrefix = basePrefix + "." + nodeId;

        switch (predicate) {
            case AndPredicate<?> and -> {
                out.put(nodePrefix + ".type", "AND");

                String leftId = gen.nextId();
                String rightId = gen.nextId();
                out.put(nodePrefix + ".left", leftId);
                out.put(nodePrefix + ".right", rightId);

                writeLogicalNode(basePrefix, leftId, (LogicalPredicate<?>) and.left(), out, gen);
                writeLogicalNode(basePrefix, rightId, (LogicalPredicate<?>) and.right(), out, gen);
            }
            case OrPredicate<?> or -> {
                out.put(nodePrefix + ".type", "OR");

                String leftId = gen.nextId();
                String rightId = gen.nextId();
                out.put(nodePrefix + ".left", leftId);
                out.put(nodePrefix + ".right", rightId);

                writeLogicalNode(basePrefix, leftId, (LogicalPredicate<?>) or.left(), out, gen);
                writeLogicalNode(basePrefix, rightId, (LogicalPredicate<?>) or.right(), out, gen);
            }
            case NotPredicate<?> not -> {
                out.put(nodePrefix + ".type", "NOT");

                String childId = gen.nextId();
                out.put(nodePrefix + ".child", childId);
                writeLogicalNode(basePrefix, childId, not.child(), out, gen);
            }

            case null, default -> {
                // LEAF – ordinary data leaf
                out.put(nodePrefix + ".type", "LEAF");

                Class<?> leafClass = predicate.getClass();
                out.put(nodePrefix + ".class", leafClass.getName());

                if (leafClass.isRecord()) {
                    writeRecordArgs(nodePrefix, predicate, leafClass, out);
                } else {
                    writeConstructorArgs(nodePrefix, predicate, leafClass, out);
                }
            }
        }
    }

    private static final class NodeIdGenerator {
        private int counter = 1;

        String nextId() {
            return "n" + counter++;
        }
    }

    private void writeMoney(String prefix, Money money, Map<String, String> out) {
        out.put(prefix + ".money.amount", money.value().toString());
        out.put(prefix + ".money.currency", money.currency());
    }

    private void writePercentage(String prefix, Percentage percentage, Map<String, String> out) {
        out.put(prefix + ".percentage.value", percentage.value().toString());
    }

    private void writeRecordArgs(
            String prefix, Object bean, Class<?> clazz, Map<String, String> out) {
        var components = clazz.getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            try {
                var accessor = components[i].getAccessor();
                Object value = accessor.invoke(bean);
                writeValue(prefix + ".arg" + i, value, out);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(
                        "Cannot read record component " + components[i].getName(), e);
            }
        }
    }

    private void writeConstructorArgs(
            String prefix, Object bean, Class<?> clazz, Map<String, String> out) {
        // Simplification: take first constructor
        // values from getters
        Constructor<?> ctor = clazz.getDeclaredConstructors()[0];
        var params = ctor.getParameters();

        for (java.lang.reflect.Parameter param : params) {
            String paramName =
                    param.getName(); // uwaga: potrzeba przełącznika -parameters dla kompilatora
            // aby w bytecode były zachowana nazwy parametrów
            Method accessor = findAccessor(clazz, paramName);
            try {
                Object value = accessor.invoke(bean);
                writeValue(prefix + "." + paramName, value, out);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(
                        "Cannot read value for param " + paramName + " of " + clazz.getName(), e);
            }
        }
    }

    private Method findAccessor(Class<?> clazz, String paramName) {
        // try "getXxx"
        String capitalized = Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1);
        String getterName = "get" + capitalized;
        try {
            return clazz.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            // try filed name ("amount()", "value()")
            try {
                return clazz.getMethod(paramName);
            } catch (NoSuchMethodException e2) {
                throw new RuntimeException(
                        "Cannot find accessor for param " + paramName + " in " + clazz.getName());
            }
        }
    }

    private void writeValue(String prefix, Object value, Map<String, String> out) {
        if (value == null) {
            return;
        }

        Class<?> type = value.getClass();

        if (isSimpleType(type)) {
            if (Enum.class.isAssignableFrom(type)) {
                out.put(prefix, ((Enum<?>) value).name());
            } else {
                out.put(prefix, value.toString());
            }
        } else {
            // value object or nested - recursion
            writeBean(prefix, value, out);
        }
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type)
                || Enum.class.isAssignableFrom(type);
    }
}
