package com.softwarearchetypes.accounting;

import com.softwarearchetypes.common.events.EventPublisher;
import com.softwarearchetypes.common.events.InMemoryEventsPublisher;
import java.time.Clock;

public class AccountingConfiguration {

    private final Clock clock;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionBuilderFactory transactionBuilderFactory;
    private final EventPublisher eventPublisher;
    private final AccountingFacade accountingFacade;

    AccountingConfiguration(
            Clock clock,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransactionBuilderFactory transactionBuilderFactory,
            EventPublisher eventPublisher,
            AccountingFacade accountingFacade) {
        this.clock = clock;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionBuilderFactory = transactionBuilderFactory;
        this.eventPublisher = eventPublisher;
        this.accountingFacade = accountingFacade;
    }

    public static AccountingConfiguration inMemory(Clock clock) {
        EntryRepository entryRepository = new InMemoryEntryRepository();
        EntryAllocations entryAllocations = new EntryAllocations(entryRepository);
        AccountRepository accountRepository = new InMemoryAccountRepo(entryRepository);
        TransactionRepository transactionRepository = new InMemoryTransactionRepo();
        TransactionBuilderFactory transactionBuilderFactory =
                new TransactionBuilderFactory(
                        accountRepository,
                        transactionRepository,
                        entryAllocations,
                        entryRepository,
                        clock);
        EventPublisher eventPublisher = new InMemoryEventsPublisher();
        AccountViewQueries accountViewQueries =
                new AccountViewQueries(accountRepository, entryRepository);
        AccountingFacade accountingFacade =
                new AccountingFacade(
                        clock,
                        accountRepository,
                        accountViewQueries,
                        transactionRepository,
                        transactionBuilderFactory,
                        eventPublisher);
        return new AccountingConfiguration(
                clock,
                accountRepository,
                transactionRepository,
                transactionBuilderFactory,
                eventPublisher,
                accountingFacade);
    }

    public AccountingFacade facade() {
        return accountingFacade;
    }

    public TransactionBuilderFactory transactionBuilderFactory() {
        return transactionBuilderFactory;
    }

    AccountRepository accountRepository() {
        return accountRepository;
    }

    public EventPublisher eventPublisher() {
        return eventPublisher;
    }
}
