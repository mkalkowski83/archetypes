package com.softwarearchetypes.inventory.waitlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class WaitListTest {

    @Test
    void fifoReturnsEntriesInOrderAdded() {
        // given
        WaitList<String> waitList = WaitList.fifo(10);
        waitList.add(WaitListEntry.of("first"));
        waitList.add(WaitListEntry.of("second"));
        waitList.add(WaitListEntry.of("third"));

        // when/then
        assertThat(waitList.poll().map(WaitListEntry::payload)).contains("first");
        assertThat(waitList.poll().map(WaitListEntry::payload)).contains("second");
        assertThat(waitList.poll().map(WaitListEntry::payload)).contains("third");
        assertThat(waitList.poll()).isEmpty();
    }

    @Test
    void priorityReturnsHigherPriorityFirst() {
        // given
        WaitList<String> waitList = WaitList.priority(10);
        waitList.add(WaitListEntry.of("low", 3));
        waitList.add(WaitListEntry.of("high", 1));
        waitList.add(WaitListEntry.of("medium", 2));

        // when/then - lower number = higher priority
        assertThat(waitList.poll().map(WaitListEntry::payload)).contains("high");
        assertThat(waitList.poll().map(WaitListEntry::payload)).contains("medium");
        assertThat(waitList.poll().map(WaitListEntry::payload)).contains("low");
    }

    @Test
    void criteriaSelectsFirstMatchingEntry() {
        // given
        WaitList<Integer> waitList = WaitList.criteria(10);
        waitList.add(WaitListEntry.of(500)); // too big
        waitList.add(WaitListEntry.of(200)); // fits
        waitList.add(WaitListEntry.of(100)); // also fits but second

        // when - select first that fits in 300
        Optional<WaitListEntry<Integer>> selected = waitList.selectNext(qty -> qty <= 300);

        // then
        assertThat(selected.map(WaitListEntry::payload)).contains(200);
        assertThat(waitList.size()).isEqualTo(2); // 500 and 100 remain
    }

    @Test
    void criteriaLeavesNonMatchingEntriesInQueue() {
        // given
        WaitList<Integer> waitList = WaitList.criteria(10);
        waitList.add(WaitListEntry.of(500));
        waitList.add(WaitListEntry.of(400));

        // when - nothing fits in 300
        Optional<WaitListEntry<Integer>> selected = waitList.selectNext(qty -> qty <= 300);

        // then
        assertThat(selected).isEmpty();
        assertThat(waitList.size()).isEqualTo(2); // both remain
    }

    @Test
    void cannotExceedCapacity() {
        // given
        WaitList<String> waitList = WaitList.fifo(2);
        waitList.add(WaitListEntry.of("first"));
        waitList.add(WaitListEntry.of("second"));

        // when/then
        assertThatThrownBy(() -> waitList.add(WaitListEntry.of("third")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Waitlist full");
    }

    @Test
    void canRemoveEntryById() {
        // given
        WaitList<String> waitList = WaitList.fifo(10);
        WaitListEntry<String> entry = WaitListEntry.of("to-remove");
        waitList.add(entry);
        waitList.add(WaitListEntry.of("stays"));

        // when
        boolean removed = waitList.removeById(entry.id());

        // then
        assertThat(removed).isTrue();
        assertThat(waitList.size()).isEqualTo(1);
        assertThat(waitList.contains(entry.id())).isFalse();
    }

    @Test
    void peekDoesNotRemoveEntry() {
        // given
        WaitList<String> waitList = WaitList.fifo(10);
        waitList.add(WaitListEntry.of("peeked"));

        // when
        Optional<WaitListEntry<String>> peeked = waitList.peek();

        // then
        assertThat(peeked.map(WaitListEntry::payload)).contains("peeked");
        assertThat(waitList.size()).isEqualTo(1);
    }

    @Test
    void reportsCorrectAvailableCapacity() {
        // given
        WaitList<String> waitList = WaitList.fifo(5);
        waitList.add(WaitListEntry.of("one"));
        waitList.add(WaitListEntry.of("two"));

        // then
        assertThat(waitList.capacity()).isEqualTo(5);
        assertThat(waitList.size()).isEqualTo(2);
        assertThat(waitList.availableCapacity()).isEqualTo(3);
        assertThat(waitList.isFull()).isFalse();
    }
}
