package com.softwarearchetypes.planvsexecution.productionanalysis;

import com.softwarearchetypes.planvsexecution.productionanalysis.delta.DeltaResult;
import com.softwarearchetypes.planvsexecution.productionanalysis.modification.ModificationRule;
import com.softwarearchetypes.planvsexecution.productionanalysis.modification.ScheduleModifier;
import com.softwarearchetypes.planvsexecution.productionanalysis.modification.UnderProductionCondition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A production plan that can be modified based on delta analysis. This is where SIMULATIONS happen
 * - the plan evolves without changing reality.
 */
public class ConfigurableProductionPlan {

    private ProductionPlan activePlan;
    private final List<ModificationRule> rules;
    private final Set<ModificationRule> appliedOnceRules;

    private ConfigurableProductionPlan(ProductionPlan initialPlan, List<ModificationRule> rules) {
        this.activePlan = initialPlan;
        this.rules = new ArrayList<>(rules);
        this.appliedOnceRules = new HashSet<>();
    }

    public ProductionPlan activePlan() {
        return activePlan;
    }

    List<ModificationRule> rules() {
        return List.copyOf(rules);
    }

    void fulfilled(List<ModificationRule> fulfilledRules, DeltaResult deltaResult) {
        for (ModificationRule rule : fulfilledRules) {
            if (alreadyApplied(rule)) {
                continue;
            }

            activePlan = rule.modifier().modify(activePlan, deltaResult);
            if (rule.applyOnce()) {
                appliedOnceRules.add(rule);
            }
        }
    }

    private boolean alreadyApplied(ModificationRule rule) {
        return rule.applyOnce() && appliedOnceRules.contains(rule);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProductionPlan initialPlan;
        private final List<ModificationRule> rules = new ArrayList<>();

        public Builder initialPlan(ProductionPlan plan) {
            this.initialPlan = plan;
            return this;
        }

        public Builder addRule(ModificationRule rule) {
            this.rules.add(rule);
            return this;
        }

        public Builder onUnderProduction(int minQuantity, ScheduleModifier modifier) {
            return addRule(
                    ModificationRule.once(UnderProductionCondition.atLeast(minQuantity), modifier));
        }

        public ConfigurableProductionPlan build() {
            if (initialPlan == null) {
                throw new IllegalStateException("initialPlan must be set");
            }
            return new ConfigurableProductionPlan(initialPlan, rules);
        }
    }
}
