package com.softwarearchetypes.accounting.postingrules;

import com.softwarearchetypes.accounting.Transaction;
import java.util.List;

public interface PostingRule {

    PostingRuleId id();

    String name();

    boolean isEligible(PostingContext context);

    List<Transaction> execute(PostingContext context);

    default int priority() {
        return 100;
    }
}
