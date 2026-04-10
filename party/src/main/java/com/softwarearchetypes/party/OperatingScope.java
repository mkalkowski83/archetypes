package com.softwarearchetypes.party;

import static com.softwarearchetypes.common.Preconditions.checkArgument;
import static com.softwarearchetypes.common.StringUtils.isNotBlank;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Base interface for all operating scopes that constrain capabilities. Operating scopes define
 * WHERE, WHEN, HOW MUCH, etc. a capability applies.
 */
public sealed interface OperatingScope
        permits OperatingScope.LocationScope,
                OperatingScope.TemporalScope,
                OperatingScope.QuantityScope,
                OperatingScope.SkillLevelScope,
                OperatingScope.ProtocolScope,
                OperatingScope.ProductScope,
                OperatingScope.ResourceScope {

    String scopeType();

    boolean satisfies(OperatingScope requirement);

    // === LocationScope ===

    record LocationScope(Set<String> locations) implements OperatingScope {

        public LocationScope {
            checkArgument(locations != null && !locations.isEmpty(), "Locations cannot be empty");
            locations = Set.copyOf(locations);
        }

        public static LocationScope of(String... locations) {
            return new LocationScope(Set.of(locations));
        }

        public static LocationScope of(Set<String> locations) {
            return new LocationScope(locations);
        }

        @Override
        public String scopeType() {
            return "LOCATION";
        }

        @Override
        public boolean satisfies(OperatingScope requirement) {
            if (!(requirement instanceof LocationScope required)) return false;
            return locations.containsAll(required.locations());
        }

        public boolean includes(String location) {
            return locations.contains(location);
        }

        public LocationScope expandWith(String... additional) {
            var expanded = new HashSet<>(locations);
            expanded.addAll(Set.of(additional));
            return new LocationScope(expanded);
        }
    }

    // === TemporalScope ===

    record TemporalScope(Set<DayOfWeek> days, LocalTime startTime, LocalTime endTime)
            implements OperatingScope {

        public static final TemporalScope ALWAYS =
                new TemporalScope(Set.of(DayOfWeek.values()), LocalTime.MIN, LocalTime.MAX);

        public TemporalScope {
            checkArgument(days != null && !days.isEmpty(), "Days cannot be empty");
            checkArgument(startTime != null && endTime != null, "Times cannot be null");
            days = Set.copyOf(days);
        }

        public static TemporalScope always() {
            return ALWAYS;
        }

        public static TemporalScope workingHours() {
            return workingDays(LocalTime.of(8, 0), LocalTime.of(18, 0));
        }

        public static TemporalScope workingDays(LocalTime start, LocalTime end) {
            return new TemporalScope(
                    Set.of(
                            DayOfWeek.MONDAY,
                            DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY),
                    start,
                    end);
        }

        @Override
        public String scopeType() {
            return "TEMPORAL";
        }

        @Override
        public boolean satisfies(OperatingScope requirement) {
            if (!(requirement instanceof TemporalScope required)) return false;
            boolean daysCovered = days.containsAll(required.days());
            boolean timesCovered =
                    !startTime.isAfter(required.startTime())
                            && !endTime.isBefore(required.endTime());
            return daysCovered && timesCovered;
        }

        public boolean isAvailable(DayOfWeek day, LocalTime time) {
            return days.contains(day) && !time.isBefore(startTime) && !time.isAfter(endTime);
        }
    }

    // === QuantityScope ===

    record QuantityScope(int maxQuantity, QuantityPeriod period) implements OperatingScope {

        public enum QuantityPeriod {
            PER_DAY,
            PER_WEEK,
            PER_MONTH,
            PER_YEAR,
            UNLIMITED
        }

        public QuantityScope {
            checkArgument(
                    maxQuantity > 0 || period == QuantityPeriod.UNLIMITED,
                    "Max quantity must be positive");
        }

        public static QuantityScope unlimited() {
            return new QuantityScope(Integer.MAX_VALUE, QuantityPeriod.UNLIMITED);
        }

        public static QuantityScope maxPerDay(int max) {
            return new QuantityScope(max, QuantityPeriod.PER_DAY);
        }

        public static QuantityScope maxPerWeek(int max) {
            return new QuantityScope(max, QuantityPeriod.PER_WEEK);
        }

        public static QuantityScope maxPerMonth(int max) {
            return new QuantityScope(max, QuantityPeriod.PER_MONTH);
        }

        @Override
        public String scopeType() {
            return "QUANTITY";
        }

        @Override
        public boolean satisfies(OperatingScope requirement) {
            if (!(requirement instanceof QuantityScope required)) return false;
            if (period == QuantityPeriod.UNLIMITED) return true;
            if (required.period() == QuantityPeriod.UNLIMITED) return true;
            return period == required.period() && maxQuantity >= required.maxQuantity();
        }

        public boolean allows(int quantity) {
            return period == QuantityPeriod.UNLIMITED || quantity <= maxQuantity;
        }
    }

    // === SkillLevelScope ===

    record SkillLevelScope(String level, int rank) implements OperatingScope {

        public static final SkillLevelScope JUNIOR = new SkillLevelScope("Junior", 1);
        public static final SkillLevelScope MID = new SkillLevelScope("Mid", 2);
        public static final SkillLevelScope SENIOR = new SkillLevelScope("Senior", 3);
        public static final SkillLevelScope EXPERT = new SkillLevelScope("Expert", 4);

        public SkillLevelScope {
            checkArgument(isNotBlank(level), "Skill level cannot be blank");
            checkArgument(rank > 0, "Rank must be positive");
        }

        public static SkillLevelScope of(String level) {
            return switch (level.toLowerCase()) {
                case "junior" -> JUNIOR;
                case "mid" -> MID;
                case "senior" -> SENIOR;
                case "expert", "advanced" -> EXPERT;
                default -> new SkillLevelScope(level, 1);
            };
        }

        public static SkillLevelScope of(String level, int rank) {
            return new SkillLevelScope(level, rank);
        }

        @Override
        public String scopeType() {
            return "SKILL_LEVEL";
        }

        @Override
        public boolean satisfies(OperatingScope requirement) {
            if (!(requirement instanceof SkillLevelScope required)) return false;
            return this.rank >= required.rank();
        }

        public boolean isAtLeast(SkillLevelScope other) {
            return this.rank >= other.rank();
        }
    }

    // === ProtocolScope ===

    record ProtocolScope(Set<String> protocols) implements OperatingScope {

        public ProtocolScope {
            checkArgument(protocols != null && !protocols.isEmpty(), "Protocols cannot be empty");
            protocols = Set.copyOf(protocols);
        }

        public static ProtocolScope of(String... protocols) {
            return new ProtocolScope(Set.of(protocols));
        }

        @Override
        public String scopeType() {
            return "PROTOCOL";
        }

        @Override
        public boolean satisfies(OperatingScope requirement) {
            if (!(requirement instanceof ProtocolScope required)) return false;
            return protocols.containsAll(required.protocols());
        }

        public boolean supports(String protocol) {
            return protocols.contains(protocol);
        }
    }

    // === ProductScope ===

    record ProductScope(Set<String> products) implements OperatingScope {

        public ProductScope {
            checkArgument(products != null && !products.isEmpty(), "Products cannot be empty");
            products = Set.copyOf(products);
        }

        public static ProductScope of(String... products) {
            return new ProductScope(Set.of(products));
        }

        @Override
        public String scopeType() {
            return "PRODUCT";
        }

        @Override
        public boolean satisfies(OperatingScope requirement) {
            if (!(requirement instanceof ProductScope required)) return false;
            return products.containsAll(required.products());
        }

        public boolean covers(String product) {
            return products.contains(product);
        }
    }

    // === ResourceScope ===

    record ResourceScope(Set<String> resources) implements OperatingScope {

        public ResourceScope {
            checkArgument(resources != null && !resources.isEmpty(), "Resources cannot be empty");
            resources = Set.copyOf(resources);
        }

        public static ResourceScope of(String... resources) {
            return new ResourceScope(Set.of(resources));
        }

        public static ResourceScope vehicles(String... vehicles) {
            return of(vehicles);
        }

        @Override
        public String scopeType() {
            return "RESOURCE";
        }

        @Override
        public boolean satisfies(OperatingScope requirement) {
            if (!(requirement instanceof ResourceScope required)) return false;
            return resources.containsAll(required.resources());
        }

        public boolean hasResource(String resource) {
            return resources.contains(resource);
        }
    }
}
