package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PricingConfiguration {

    private final CalculatorRepository repository;
    private final PricingFacade facade;

    PricingConfiguration(CalculatorRepository repository, PricingFacade facade) {
        this.repository = repository;
        this.facade = facade;
    }

    public static PricingConfiguration inMemory(Clock clock) {
        InMemoryCalculatorsRepository repository = new InMemoryCalculatorsRepository();
        PricingFacade facade = new PricingFacade(repository, clock);
        facade.addCalculator(
                "simple-fixed-20",
                CalculatorType.SIMPLE_FIXED,
                new Parameters(Map.of("amount", Money.pln(BigDecimal.valueOf(20)))));
        facade.addCalculator(
                "simple-interest-6",
                CalculatorType.SIMPLE_INTEREST,
                new Parameters(Map.of("annualRate", BigDecimal.valueOf(6))));
        return new PricingConfiguration(repository, facade);
    }

    public PricingFacade pricingFacade() {
        return facade;
    }
}

interface CalculatorRepository {
    void save(Calculator calculator);

    Optional<Calculator> findByName(String name);

    Optional<Calculator> findById(CalculatorId id);

    Collection<Calculator> findAll();

    Collection<Calculator> findByIds(Collection<CalculatorId> ids);
}

class InMemoryCalculatorsRepository implements CalculatorRepository {
    private final Set<Calculator> calculators = new HashSet<>();

    @Override
    public void save(Calculator calculator) {
        calculators.add(calculator);
    }

    @Override
    public Optional<Calculator> findByName(String name) {
        return calculators.stream().filter(c -> c.name().equals(name)).findFirst();
    }

    @Override
    public Optional<Calculator> findById(CalculatorId id) {
        return calculators.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    @Override
    public Collection<Calculator> findAll() {
        return new HashSet<>(calculators);
    }

    @Override
    public Collection<Calculator> findByIds(Collection<CalculatorId> ids) {
        Set<CalculatorId> idSet = new HashSet<>(ids);
        return calculators.stream().filter(c -> idSet.contains(c.getId())).toList();
    }
}

interface ComponentRepository {
    void save(Component component);

    Optional<Component> findByName(String name);

    Optional<Component> findById(ComponentId id);

    Collection<Component> findAll();

    Collection<Component> findByNames(Collection<String> names);
}

class InMemoryComponentRepository implements ComponentRepository {
    private final Map<ComponentId, Component> components = new java.util.HashMap<>();

    @Override
    public void save(Component component) {
        // Replace component with same ID (for updates)
        components.put(component.id(), component);
    }

    @Override
    public Optional<Component> findByName(String name) {
        return components.values().stream()
                .filter(c -> c.name().equals(name))
                .findFirst()
                .map(this::refreshComponent); // Odśwież przed zwróceniem
    }

    @Override
    public Optional<Component> findById(ComponentId id) {
        return Optional.ofNullable(components.get(id))
                .map(this::refreshComponent); // Odśwież przed zwróceniem
    }

    @Override
    public Collection<Component> findAll() {
        return components.values().stream()
                .map(this::refreshComponent)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Collection<Component> findByNames(Collection<String> names) {
        Set<String> nameSet = new HashSet<>(names);
        return components.values().stream()
                .filter(c -> nameSet.contains(c.name()))
                .map(this::refreshComponent)
                .toList();
    }

    /**
     * Odświeża CompositeComponent pobierając świeże referencje do dzieci z repo. Działa jak JOIN w
     * SQL - zawsze zwraca aktualne dane.
     */
    private Component refreshComponent(Component component) {
        if (!(component instanceof CompositeComponent composite)) {
            return component; // SimpleComponent - nie wymaga odświeżenia
        }

        // Odśwież wszystkie wersje pobierając świeże dzieci
        java.util.List<CompositeComponentVersion> refreshedVersions =
                composite.versions().stream().map(this::refreshVersion).toList();

        return new CompositeComponent(composite.id(), composite.name(), refreshedVersions);
    }

    /** Odświeża wersję composite pobierając świeże referencje do dzieci po ID. */
    private CompositeComponentVersion refreshVersion(CompositeComponentVersion version) {
        // Pobierz świeże dzieci po ich ID (bez odświeżania, żeby uniknąć rekurencji)
        java.util.List<Component> freshChildren =
                version.children().stream()
                        .map(
                                child ->
                                        components.get(
                                                child.id())) // Bezpośrednio z mapy, bez refresh
                        .filter(java.util.Objects::nonNull)
                        .toList();

        return new CompositeComponentVersion(
                freshChildren,
                version.dependencies(),
                version.applicabilityConstraint(),
                version.validity(),
                version.definedAt());
    }
}
