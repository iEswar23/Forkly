package io.github.ieswar23.forkly.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.forkly.domain.pricing.BillSplitter
import org.junit.Assert.assertThrows
import org.junit.Test

class BillSplitterTest {

    private val splitter = BillSplitter()

    @Test
    fun `an evenly divisible bill gives everyone the same share`() {
        val split = splitter.split(totalPaise = 1_200_00, tipPaise = 0, people = 4, splitTip = true)

        assertThat(split.shares.map { it.amountPaise }).containsExactly(300_00L, 300_00L, 300_00L, 300_00L)
        assertThat(split.baseSharePaise).isEqualTo(300_00)
        assertThat(split.roundedUpCount).isEqualTo(0)
    }

    @Test
    fun `the remainder goes one paisa at a time to the first people`() {
        // ₹1,000.01 between 3: 33,333 paise each with 2 paise left over.
        val split = splitter.split(totalPaise = 1_000_01, tipPaise = 0, people = 3, splitTip = true)

        assertThat(split.shares.map { it.amountPaise }).containsExactly(333_34L, 333_34L, 333_33L).inOrder()
        assertThat(split.roundedUpCount).isEqualTo(2)
        assertThat(split.sumPaise).isEqualTo(1_000_01)
    }

    @Test
    fun `shares never differ by more than a paisa and always add up to the total`() {
        for (total in listOf(0L, 1L, 9L, 99L, 101L, 12_345L, 99_999L, 1_234_567L)) {
            for (people in BillSplitter.MIN_PEOPLE..BillSplitter.MAX_PEOPLE) {
                val split = splitter.split(total, tipPaise = 0, people = people, splitTip = true)
                val amounts = split.shares.map { it.amountPaise }
                assertThat(amounts.sum()).isEqualTo(total)
                assertThat(amounts.max() - amounts.min()).isAtMost(1)
                // Larger shares come first.
                assertThat(amounts).isInOrder(reverseOrder<Long>())
            }
        }
    }

    @Test
    fun `a split tip is shared like the rest of the bill`() {
        val split = splitter.split(totalPaise = 1_030_00, tipPaise = 30_00, people = 2, splitTip = true)

        assertThat(split.shares.map { it.amountPaise }).containsExactly(515_00L, 515_00L)
        assertThat(split.shares.map { it.tipCoveredPaise }).containsExactly(0L, 0L)
    }

    @Test
    fun `an unsplit tip is added to the orderer's share only`() {
        val split = splitter.split(totalPaise = 1_030_00, tipPaise = 30_00, people = 2, splitTip = false)

        val (you, friend) = split.shares
        assertThat(you.isOrderer).isTrue()
        assertThat(you.amountPaise).isEqualTo(530_00)
        assertThat(you.tipCoveredPaise).isEqualTo(30_00)
        assertThat(friend.amountPaise).isEqualTo(500_00)
        assertThat(friend.tipCoveredPaise).isEqualTo(0)
        assertThat(split.sumPaise).isEqualTo(1_030_00)
    }

    @Test
    fun `an unsplit tip and an uneven remainder still add up exactly`() {
        // ₹1,234.57 incl. ₹50 tip between 4: ₹1,184.57 shared → 29,614 each + 1 paisa for the first.
        val split = splitter.split(totalPaise = 1_234_57, tipPaise = 50_00, people = 4, splitTip = false)

        assertThat(split.shares.map { it.amountPaise }).containsExactly(346_15L, 296_14L, 296_14L, 296_14L).inOrder()
        assertThat(split.baseSharePaise).isEqualTo(296_14)
        assertThat(split.roundedUpCount).isEqualTo(1)
        assertThat(split.sumPaise).isEqualTo(1_234_57)
    }

    @Test
    fun `persons are numbered from 1 with the orderer first`() {
        val split = splitter.split(totalPaise = 500_00, tipPaise = 0, people = 5, splitTip = true)

        assertThat(split.shares.map { it.person }).containsExactly(1, 2, 3, 4, 5).inOrder()
        assertThat(split.shares.count { it.isOrderer }).isEqualTo(1)
    }

    @Test
    fun `people outside 2 to 10 are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { splitter.split(100_00, 0, people = 1, splitTip = true) }
        assertThrows(IllegalArgumentException::class.java) { splitter.split(100_00, 0, people = 11, splitTip = true) }
        assertThrows(IllegalArgumentException::class.java) { splitter.split(100_00, 0, people = 0, splitTip = true) }
    }

    @Test
    fun `negative totals and impossible tips are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { splitter.split(-1, 0, people = 2, splitTip = true) }
        assertThrows(IllegalArgumentException::class.java) { splitter.split(100_00, -1, people = 2, splitTip = true) }
        assertThrows(IllegalArgumentException::class.java) { splitter.split(100_00, 100_01, people = 2, splitTip = false) }
    }

    @Test
    fun `clampPeople keeps the stepper within range`() {
        assertThat(BillSplitter.clampPeople(-3)).isEqualTo(2)
        assertThat(BillSplitter.clampPeople(2)).isEqualTo(2)
        assertThat(BillSplitter.clampPeople(7)).isEqualTo(7)
        assertThat(BillSplitter.clampPeople(25)).isEqualTo(10)
    }
}
