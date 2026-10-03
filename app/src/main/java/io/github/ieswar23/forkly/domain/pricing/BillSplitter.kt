package io.github.ieswar23.forkly.domain.pricing

/**
 * One person's part of a split bill. Person 1 is always the orderer ("You").
 * Every amount is in paise.
 */
data class BillShare(
    /** 1-based position; 1 is the person who places the order. */
    val person: Int,
    val amountPaise: Long,
    /** The rider tip included in [amountPaise] when the orderer covers it alone (0 otherwise). */
    val tipCoveredPaise: Long,
) {
    val isOrderer: Boolean get() = person == 1
}

data class BillSplit(
    val totalPaise: Long,
    val tipPaise: Long,
    val people: Int,
    val splitTip: Boolean,
    val shares: List<BillShare>,
    /** The even share before rounding; the first [roundedUpCount] people pay one paisa more. */
    val baseSharePaise: Long,
    /** How many people pay one extra paisa so the shares add up to [totalPaise] exactly. */
    val roundedUpCount: Int,
) {
    val sumPaise: Long get() = shares.sumOf { it.amountPaise }
}

/**
 * Splits a bill between friends, exactly to the paisa.
 *
 * The splittable amount is divided evenly; any remainder `r` (always fewer paise than people) is
 * spread one paisa at a time over the first `r` people, so shares never differ by more than one
 * paisa and always add up to the bill total. When the tip isn't split, it is taken out of the shared
 * amount and added to the orderer's share.
 */
class BillSplitter {

    fun split(totalPaise: Long, tipPaise: Long, people: Int, splitTip: Boolean): BillSplit {
        require(people in MIN_PEOPLE..MAX_PEOPLE) { "A bill can be split between $MIN_PEOPLE and $MAX_PEOPLE people, not $people" }
        require(totalPaise >= 0) { "Total can't be negative" }
        require(tipPaise in 0..totalPaise) { "Tip must be between 0 and the bill total" }

        val ordererOnlyTip = if (splitTip) 0L else tipPaise
        val shared = totalPaise - ordererOnlyTip
        val base = shared / people
        val remainder = (shared % people).toInt()

        val shares = (1..people).map { person ->
            val extraPaisa = if (person <= remainder) 1L else 0L
            val tip = if (person == 1) ordererOnlyTip else 0L
            BillShare(person = person, amountPaise = base + extraPaisa + tip, tipCoveredPaise = tip)
        }
        return BillSplit(
            totalPaise = totalPaise,
            tipPaise = tipPaise,
            people = people,
            splitTip = splitTip,
            shares = shares,
            baseSharePaise = base,
            roundedUpCount = remainder,
        )
    }

    companion object {
        const val MIN_PEOPLE = 2
        const val MAX_PEOPLE = 10

        fun clampPeople(people: Int): Int = people.coerceIn(MIN_PEOPLE, MAX_PEOPLE)
    }
}
