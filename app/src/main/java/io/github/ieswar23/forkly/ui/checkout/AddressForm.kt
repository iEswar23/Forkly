package io.github.ieswar23.forkly.ui.checkout

import io.github.ieswar23.forkly.domain.model.Address
import io.github.ieswar23.forkly.domain.model.AddressLabel

enum class AddressField { HOUSE, AREA, CITY, PINCODE, NAME, PHONE }

/** Editable, validated representation of an [Address]. */
data class AddressForm(
    val id: Long = 0,
    val label: AddressLabel = AddressLabel.HOME,
    val customLabel: String = "",
    val houseDetails: String = "",
    val area: String = "",
    val landmark: String = "",
    val city: String = "Hyderabad",
    val pincode: String = "",
    val receiverName: String = "",
    val receiverPhone: String = "",
) {
    fun errors(): Map<AddressField, String> = buildMap {
        if (houseDetails.isBlank()) put(AddressField.HOUSE, "Enter flat / house details")
        if (area.isBlank()) put(AddressField.AREA, "Enter area or street")
        if (city.isBlank()) put(AddressField.CITY, "Enter city")
        if (!pincode.matches(Regex("[1-9][0-9]{5}"))) put(AddressField.PINCODE, "Enter a valid 6-digit pincode")
        if (receiverName.isBlank()) put(AddressField.NAME, "Enter receiver's name")
        val allDigits = receiverPhone.filter { it.isDigit() }
        val digits = if (allDigits.length == 12 && allDigits.startsWith("91")) allDigits.drop(2) else allDigits
        if (digits.length != 10 || digits.first() !in '6'..'9') put(AddressField.PHONE, "Enter a valid 10-digit mobile number")
    }

    val isValid: Boolean get() = errors().isEmpty()

    fun toAddress(): Address = Address(
        id = id,
        label = label,
        customLabel = customLabel.trim(),
        houseDetails = houseDetails.trim(),
        area = area.trim(),
        landmark = landmark.trim(),
        city = city.trim(),
        pincode = pincode.trim(),
        receiverName = receiverName.trim(),
        receiverPhone = receiverPhone.trim(),
    )

    companion object {
        fun from(address: Address) = AddressForm(
            id = address.id,
            label = address.label,
            customLabel = address.customLabel,
            houseDetails = address.houseDetails,
            area = address.area,
            landmark = address.landmark,
            city = address.city,
            pincode = address.pincode,
            receiverName = address.receiverName,
            receiverPhone = address.receiverPhone,
        )
    }
}
