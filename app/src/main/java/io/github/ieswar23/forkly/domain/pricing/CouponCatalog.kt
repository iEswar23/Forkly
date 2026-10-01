package io.github.ieswar23.forkly.domain.pricing

import io.github.ieswar23.forkly.domain.model.Coupon
import io.github.ieswar23.forkly.domain.model.CouponType

/** Coupons currently live on Forkly. Codes are matched case-insensitively. */
object CouponCatalog {
    val WELCOME50 = Coupon(
        code = "WELCOME50",
        title = "50% OFF up to ₹100",
        description = "Valid on orders above ₹149",
        type = CouponType.PercentOff(percent = 50, maxDiscountPaise = 100_00),
        minOrderPaise = 149_00,
    )
    val FREEDEL = Coupon(
        code = "FREEDEL",
        title = "Free delivery",
        description = "No delivery fee on orders above ₹199",
        type = CouponType.FreeDelivery,
        minOrderPaise = 199_00,
    )
    val FEAST120 = Coupon(
        code = "FEAST120",
        title = "Flat ₹120 OFF",
        description = "Valid on orders above ₹599",
        type = CouponType.FlatOff(amountPaise = 120_00),
        minOrderPaise = 599_00,
    )
    val SWEET20 = Coupon(
        code = "SWEET20",
        title = "20% OFF up to ₹80",
        description = "Valid on orders above ₹249",
        type = CouponType.PercentOff(percent = 20, maxDiscountPaise = 80_00),
        minOrderPaise = 249_00,
    )

    val all: List<Coupon> = listOf(WELCOME50, FREEDEL, FEAST120, SWEET20)

    fun find(code: String): Coupon? = all.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
}
