package com.softwarearchetypes.planvsexecution.repaymentanalysis;

import com.softwarearchetypes.planvsexecution.repaymentanalysis.delta.DeltaResult;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.LatePaymentCondition;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.ModificationRule;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.OnTimePaymentCondition;
import com.softwarearchetypes.planvsexecution.repaymentanalysis.modification.PaymentScheduleModifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigurablePaymentSchedule {

    private PaymentSchedule activeSchedule;
    private final List<ModificationRule> rules;
    private final Set<ModificationRule> appliedOnceRules;

    private ConfigurablePaymentSchedule(
            PaymentSchedule initialSchedule, List<ModificationRule> rules) {
        this.activeSchedule = initialSchedule;
        this.rules = new ArrayList<>(rules);
        this.appliedOnceRules = new HashSet<>();
    }

    public PaymentSchedule activeSchedule() {
        return activeSchedule;
    }

    List<ModificationRule> rules() {
        return List.copyOf(rules);
    }

    ConfigurablePaymentSchedule fulfilled(
            List<ModificationRule> fulfilledRules, DeltaResult deltaResult) {
        for (ModificationRule rule : fulfilledRules) {
            if (alreadyApplied(rule)) {
                continue;
            }

            activeSchedule = rule.modifier().modify(activeSchedule, deltaResult);
            if (rule.applyOnce()) {
                appliedOnceRules.add(rule);
            }
        }
        return this;
    }

    private boolean alreadyApplied(ModificationRule rule) {
        return rule.applyOnce() && appliedOnceRules.contains(rule);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PaymentSchedule initialSchedule;
        private final List<ModificationRule> rules = new ArrayList<>();

        public Builder initialSchedule(PaymentSchedule schedule) {
            this.initialSchedule = schedule;
            return this;
        }

        public Builder addRule(ModificationRule rule) {
            this.rules.add(rule);
            return this;
        }

        public Builder onLatePayment(int count, PaymentScheduleModifier modifier) {
            return addRule(ModificationRule.once(LatePaymentCondition.atLeast(count), modifier));
        }

        public Builder onOnTimePayment(int count, PaymentScheduleModifier modifier) {
            return addRule(ModificationRule.once(OnTimePaymentCondition.atLeast(count), modifier));
        }

        public ConfigurablePaymentSchedule build() {
            if (initialSchedule == null) {
                throw new IllegalStateException("initialSchedule must be set");
            }
            return new ConfigurablePaymentSchedule(initialSchedule, rules);
        }
    }
}
