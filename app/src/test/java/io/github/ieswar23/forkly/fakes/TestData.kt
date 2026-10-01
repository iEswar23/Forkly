package io.github.ieswar23.forkly.fakes

import io.github.ieswar23.forkly.domain.model.Address
import io.github.ieswar23.forkly.domain.model.AddressLabel
import io.github.ieswar23.forkly.domain.model.CartLine
import io.github.ieswar23.forkly.domain.model.CustomizationGroup
import io.github.ieswar23.forkly.domain.model.CustomizationOption
import io.github.ieswar23.forkly.domain.model.MenuItem
import io.github.ieswar23.forkly.domain.model.Restaurant
import io.github.ieswar23.forkly.domain.model.SelectionType

object TestData {
    fun restaurant(
        id: String,
        name: String = id.replaceFirstChar { it.uppercase() },
        rating: Double = 4.2,
        deliveryTimeMins: Int = 30,
        costForTwo: Int = 400,
        isPureVeg: Boolean = false,
        offerText: String? = null,
        cuisines: List<String> = listOf("North Indian"),
        distanceKm: Double = 2.0,
        ratingCount: Int = 1000,
    ) = Restaurant(
        id = id,
        name = name,
        cuisines = cuisines,
        area = "Banjara Hills",
        rating = rating,
        ratingCount = ratingCount,
        deliveryTimeMins = deliveryTimeMins,
        costForTwo = costForTwo,
        distanceKm = distanceKm,
        isPureVeg = isPureVeg,
        offerText = offerText,
        offerCode = null,
        emoji = "🍛",
        gradientStart = "#E8452C",
        gradientEnd = "#FF8A3D",
        tagline = "Tasty",
        openHours = "11:00 AM – 11:00 PM",
    )

    fun menuItem(
        id: String,
        restaurantId: String,
        pricePaise: Long,
        customizations: List<CustomizationGroup> = emptyList(),
        isVeg: Boolean = true,
    ) = MenuItem(
        id = id,
        restaurantId = restaurantId,
        section = "Mains",
        name = "Dish $id",
        description = "",
        pricePaise = pricePaise,
        isVeg = isVeg,
        isBestseller = false,
        emoji = "🍲",
        customizations = customizations,
    )

    val sizeGroup = CustomizationGroup(
        id = "size",
        title = "Size",
        type = SelectionType.SINGLE,
        required = true,
        maxSelections = 1,
        options = listOf(
            CustomizationOption("regular", "Regular", 0),
            CustomizationOption("large", "Large", 150_00),
        ),
    )

    val toppingsGroup = CustomizationGroup(
        id = "toppings",
        title = "Toppings",
        type = SelectionType.MULTIPLE,
        required = false,
        maxSelections = 2,
        options = listOf(
            CustomizationOption("cheese", "Cheese", 60_00),
            CustomizationOption("olives", "Olives", 40_00),
            CustomizationOption("jalapeno", "Jalapeño", 40_00),
        ),
    )

    fun line(
        lineId: String,
        unitPricePaise: Long,
        quantity: Int = 1,
        restaurantId: String = "r1",
        addedAt: Long = 0,
    ) = CartLine(
        lineId = lineId,
        restaurantId = restaurantId,
        menuItemId = lineId.substringBefore("|"),
        name = "Item $lineId",
        emoji = "🍲",
        isVeg = true,
        unitPricePaise = unitPricePaise,
        quantity = quantity,
        customizationSummary = "",
        selectedOptionIds = emptyList(),
        addedAt = addedAt,
    )

    val homeAddress = Address(
        id = 1,
        label = AddressLabel.HOME,
        houseDetails = "Flat 302, Lakeview Residency",
        area = "Road No. 12, Banjara Hills",
        landmark = "",
        city = "Hyderabad",
        pincode = "500034",
        receiverName = "Aarav Reddy",
        receiverPhone = "+91 98480 12345",
    )
}
