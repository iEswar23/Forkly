package io.github.ieswar23.forkly.domain.model

enum class AddressLabel(val title: String) { HOME("Home"), WORK("Work"), OTHER("Other") }

data class Address(
    val id: Long = 0,
    val label: AddressLabel,
    val customLabel: String = "",
    val houseDetails: String,
    val area: String,
    val landmark: String,
    val city: String,
    val pincode: String,
    val receiverName: String,
    val receiverPhone: String,
) {
    val displayLabel: String get() = if (label == AddressLabel.OTHER && customLabel.isNotBlank()) customLabel else label.title

    /** The neighbourhood shown in the home header, e.g. "Banjara Hills". */
    val locality: String get() = area.split(",").last().trim()

    val fullLine: String
        get() = buildList {
            add(houseDetails)
            add(area)
            if (landmark.isNotBlank()) add("Near $landmark")
            add("$city $pincode")
        }.joinToString(", ")
}
