package com.softwarearchetypes.accounting.postingrules;

import com.softwarearchetypes.accounting.Transaction;
import java.util.List;

public interface PostingCalculator {

    // IF posting rules are outside the main accounting model
    // calculator should return ExecuteTransactionCommand instead
    List<Transaction> calculate(TargetAccounts accounts, PostingContext context);
}
