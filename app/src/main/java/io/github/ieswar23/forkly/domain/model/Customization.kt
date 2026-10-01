package io.github.ieswar23.forkly.domain.model

/**
 * The options a customer picked for a customizable [MenuItem], keyed by group id.
 * Pure logic so the bottom sheet, the cart and the tests share one source of truth.
 */
data class CustomizationSelection(
    val selected: Map<String, Set<String>> = emptyMap(),
) {
    fun isSelected(groupId: String, optionId: String): Boolean =
        selected[groupId]?.contains(optionId) == true

    /** Toggles an option, honouring single-choice groups and the group's max selection limit. */
    fun toggle(group: CustomizationGroup, optionId: String): CustomizationSelection {
        val current = selected[group.id].orEmpty()
        val updated: Set<String> = when (group.type) {
            SelectionType.SINGLE -> setOf(optionId)
            SelectionType.MULTIPLE -> when {
                optionId in current -> current - optionId
                current.size >= group.maxSelections -> current
                else -> current + optionId
            }
        }
        return copy(selected = selected + (group.id to updated))
    }

    fun isComplete(groups: List<CustomizationGroup>): Boolean =
        groups.filter { it.required }.all { selected[it.id].orEmpty().isNotEmpty() }

    fun chosenOptions(groups: List<CustomizationGroup>): List<CustomizationOption> =
        groups.flatMap { group -> group.options.filter { isSelected(group.id, it.id) } }

    fun unitPricePaise(item: MenuItem): Long =
        item.pricePaise + chosenOptions(item.customizations).sumOf { it.priceDeltaPaise }

    fun summary(groups: List<CustomizationGroup>): String =
        chosenOptions(groups).joinToString(", ") { it.name }

    fun optionIds(groups: List<CustomizationGroup>): List<String> =
        groups.flatMap { group -> group.options.filter { isSelected(group.id, it.id) }.map { "${group.id}:${it.id}" } }

    companion object {
        /** Pre-selects the first option of every required single-choice group. */
        fun defaultFor(item: MenuItem): CustomizationSelection = CustomizationSelection(
            item.customizations
                .filter { it.type == SelectionType.SINGLE && it.required && it.options.isNotEmpty() }
                .associate { it.id to setOf(it.options.first().id) },
        )
    }
}
