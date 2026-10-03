package io.github.ieswar23.forkly.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.ieswar23.forkly.data.local.entity.AddressEntity
import io.github.ieswar23.forkly.data.local.entity.BannerEntity
import io.github.ieswar23.forkly.data.local.entity.CartItemEntity
import io.github.ieswar23.forkly.data.local.entity.CategoryEntity
import io.github.ieswar23.forkly.data.local.entity.MenuItemEntity
import io.github.ieswar23.forkly.data.local.entity.OrderItemEntity
import io.github.ieswar23.forkly.data.local.entity.OrderWithItems
import io.github.ieswar23.forkly.data.local.entity.RestaurantEntity
import io.github.ieswar23.forkly.data.remote.dto.BannerDto
import io.github.ieswar23.forkly.data.remote.dto.CategoryDto
import io.github.ieswar23.forkly.data.remote.dto.CustomizationGroupDto
import io.github.ieswar23.forkly.data.remote.dto.MenuDto
import io.github.ieswar23.forkly.data.remote.dto.RestaurantDto
import io.github.ieswar23.forkly.domain.model.Address
import io.github.ieswar23.forkly.domain.model.AddressLabel
import io.github.ieswar23.forkly.domain.model.Banner
import io.github.ieswar23.forkly.domain.model.CartLine
import io.github.ieswar23.forkly.domain.model.Category
import io.github.ieswar23.forkly.domain.model.CustomizationGroup
import io.github.ieswar23.forkly.domain.model.CustomizationOption
import io.github.ieswar23.forkly.domain.model.MenuItem
import io.github.ieswar23.forkly.domain.model.Order
import io.github.ieswar23.forkly.domain.model.OrderItem
import io.github.ieswar23.forkly.domain.model.OrderStatus
import io.github.ieswar23.forkly.domain.model.PaymentMethod
import io.github.ieswar23.forkly.domain.model.Restaurant
import io.github.ieswar23.forkly.domain.model.Rider
import io.github.ieswar23.forkly.domain.model.SelectionType

private val gson = Gson()
private val groupListType = object : TypeToken<List<CustomizationGroupDto>>() {}.type

// ---- Remote -> Local ----

fun RestaurantDto.toEntity(position: Int) = RestaurantEntity(
    id = id, name = name, cuisines = cuisines, area = area, rating = rating, ratingCount = ratingCount,
    deliveryTimeMins = deliveryTimeMins, costForTwo = costForTwo, distanceKm = distanceKm,
    isPureVeg = isPureVeg, offerText = offerText, offerCode = offerCode, emoji = emoji,
    gradientStart = gradientStart, gradientEnd = gradientEnd, tagline = tagline, openHours = openHours,
    position = position,
)

fun MenuDto.toEntities(): List<MenuItemEntity> = sections.flatMapIndexed { sectionIndex, section ->
    section.items.mapIndexed { index, item ->
        MenuItemEntity(
            id = item.id,
            restaurantId = restaurantId,
            section = section.name,
            sectionOrder = sectionIndex,
            position = index,
            name = item.name,
            description = item.description,
            pricePaise = item.price * 100L,
            isVeg = item.isVeg,
            isBestseller = item.isBestseller,
            emoji = item.emoji,
            customizationsJson = gson.toJson(item.customizations.orEmpty()),
        )
    }
}

fun BannerDto.toEntity(position: Int) = BannerEntity(
    id = id, title = title, subtitle = subtitle, emoji = emoji, gradientStart = gradientStart,
    gradientEnd = gradientEnd, couponCode = couponCode, category = category, position = position,
)

fun CategoryDto.toEntity(position: Int) = CategoryEntity(id = id, name = name, emoji = emoji, position = position)

// ---- Local -> Domain ----

fun RestaurantEntity.toDomain(isFavorite: Boolean = false) = Restaurant(
    id = id, name = name, cuisines = cuisines, area = area, rating = rating, ratingCount = ratingCount,
    deliveryTimeMins = deliveryTimeMins, costForTwo = costForTwo, distanceKm = distanceKm,
    isPureVeg = isPureVeg, offerText = offerText, offerCode = offerCode, emoji = emoji,
    gradientStart = gradientStart, gradientEnd = gradientEnd, tagline = tagline, openHours = openHours,
    isFavorite = isFavorite,
)

fun MenuItemEntity.toDomain(): MenuItem {
    val groups: List<CustomizationGroupDto> = gson.fromJson(customizationsJson, groupListType) ?: emptyList()
    return MenuItem(
        id = id,
        restaurantId = restaurantId,
        section = section,
        name = name,
        description = description,
        pricePaise = pricePaise,
        isVeg = isVeg,
        isBestseller = isBestseller,
        emoji = emoji,
        customizations = groups.map { it.toDomain() },
    )
}

private fun CustomizationGroupDto.toDomain() = CustomizationGroup(
    id = id,
    title = title,
    type = if (type.equals("MULTIPLE", ignoreCase = true)) SelectionType.MULTIPLE else SelectionType.SINGLE,
    required = required,
    maxSelections = maxSelections.coerceAtLeast(1),
    options = options.map { CustomizationOption(it.id, it.name, it.priceDelta * 100L) },
)

fun BannerEntity.toDomain() = Banner(id, title, subtitle, emoji, gradientStart, gradientEnd, couponCode, category)

fun CategoryEntity.toDomain() = Category(id, name, emoji)

fun CartItemEntity.toDomain() = CartLine(
    lineId = lineId, restaurantId = restaurantId, menuItemId = menuItemId, name = name, emoji = emoji,
    isVeg = isVeg, unitPricePaise = unitPricePaise, quantity = quantity,
    customizationSummary = customizationSummary, selectedOptionIds = selectedOptionIds, addedAt = addedAt,
)

fun CartLine.toEntity() = CartItemEntity(
    lineId = lineId, restaurantId = restaurantId, menuItemId = menuItemId, name = name, emoji = emoji,
    isVeg = isVeg, unitPricePaise = unitPricePaise, quantity = quantity,
    customizationSummary = customizationSummary, selectedOptionIds = selectedOptionIds, addedAt = addedAt,
)

fun AddressEntity.toDomain() = Address(
    id = id,
    label = runCatching { AddressLabel.valueOf(label) }.getOrDefault(AddressLabel.OTHER),
    customLabel = customLabel,
    houseDetails = houseDetails,
    area = area,
    landmark = landmark,
    city = city,
    pincode = pincode,
    receiverName = receiverName,
    receiverPhone = receiverPhone,
)

fun Address.toEntity(createdAt: Long) = AddressEntity(
    id = id, label = label.name, customLabel = customLabel, houseDetails = houseDetails, area = area,
    landmark = landmark, city = city, pincode = pincode, receiverName = receiverName,
    receiverPhone = receiverPhone, createdAt = createdAt,
)

fun OrderWithItems.toDomain() = Order(
    id = order.id,
    restaurantId = order.restaurantId,
    restaurantName = order.restaurantName,
    restaurantEmoji = order.restaurantEmoji,
    restaurantArea = order.restaurantArea,
    items = items.map { it.toDomain() },
    itemTotalPaise = order.itemTotalPaise,
    packagingFeePaise = order.packagingFeePaise,
    deliveryFeePaise = order.deliveryFeePaise,
    discountPaise = order.discountPaise,
    gstPaise = order.gstPaise,
    tipPaise = order.tipPaise,
    totalPaise = order.totalPaise,
    couponCode = order.couponCode,
    paymentMethod = runCatching { PaymentMethod.valueOf(order.paymentMethod) }.getOrDefault(PaymentMethod.CASH),
    addressLabel = order.addressLabel,
    addressLine = order.addressLine,
    deliveryInstructions = order.deliveryInstructions,
    status = runCatching { OrderStatus.valueOf(order.status) }.getOrDefault(OrderStatus.PLACED),
    placedAt = order.placedAt,
    deliveredAt = order.deliveredAt,
    rider = Rider(order.riderName, order.riderPhone, order.riderVehicle, order.riderRating, order.riderDeliveries),
    userRating = order.userRating,
    scheduledFor = order.scheduledFor,
)

fun OrderItemEntity.toDomain() = OrderItem(
    menuItemId = menuItemId, name = name, emoji = emoji, isVeg = isVeg, quantity = quantity,
    unitPricePaise = unitPricePaise, customizationSummary = customizationSummary,
    selectedOptionIds = selectedOptionIds,
)
