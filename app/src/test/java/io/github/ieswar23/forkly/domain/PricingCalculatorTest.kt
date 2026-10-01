package io.github.ieswar23.forkly.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.forkly.domain.model.BillBreakdown
import io.github.ieswar23.forkly.domain.model.Coupon
import io.github.ieswar23.forkly.domain.model.CouponError
import io.github.ieswar23.forkly.domain.model.CouponType
import io.github.ieswar23.forkly.domain.model.CouponValidation
import io.github.ieswar23.forkly.domain.pricing.CouponCatalog
import io.github.ieswar23.forkly.domain.pricing.PricingCalculator
import io.github.ieswar23.forkly.fakes.TestData.line
import org.junit.Assert.assertThrows
import org.junit.Test

class PricingCalculatorTest {

    private val calculator = PricingCalculator()

    @Test
    fun `empty cart produces an empty bill`() {
        assertThat(calculator.calculate(emptyList(), distanceKm = 2.0, couponCode = "WELCOME50")).isEqualTo(BillBreakdown.EMPTY)
    }

    @Test
    fun `basic bill adds packaging, delivery and 5 percent GST`() {
        val bill = calculator.calculate(listOf(line("a", 200_00)), distanceKm = 2.0)

        assertThat(bill.itemTotalPaise).isEqualTo(200_00)
        assertThat(bill.packagingFeePaise).isEqualTo(5_00)
        assertThat(bill.deliveryFeePaise).isEqualTo(35_00)
        assertThat(bill.gstPaise).isEqualTo(10_25) // 5% of ₹205
        assertThat(bill.totalPaise).isEqualTo(250_25)
        assertThat(bill.couponDiscountPaise).isEqualTo(0)
    }

    @Test
    fun `multiple lines sum quantities for item total and packaging`() {
        val bill = calculator.calculate(listOf(line("a", 150_00, quantity = 2), line("b", 99_00, quantity = 3)), distanceKm = 1.0)

        assertThat(bill.itemTotalPaise).isEqualTo(597_00)
        assertThat(bill.packagingFeePaise).isEqualTo(25_00) // 5 units × ₹5
        assertThat(bill.deliveryFeePaise).isEqualTo(0) // above free-delivery threshold
        assertThat(bill.gstPaise).isEqualTo(31_10)
        assertThat(bill.totalPaise).isEqualTo(653_10)
    }

    @Test
    fun `delivery fee grows per started km beyond 3 km and is capped`() {
        assertThat(calculator.deliveryFeeFor(0.5)).isEqualTo(35_00)
        assertThat(calculator.deliveryFeeFor(3.0)).isEqualTo(35_00)
        assertThat(calculator.deliveryFeeFor(3.1)).isEqualTo(42_00)
        assertThat(calculator.deliveryFeeFor(5.2)).isEqualTo(56_00)
        assertThat(calculator.deliveryFeeFor(12.0)).isEqualTo(65_00)
    }

    @Test
    fun `packaging fee is capped at 30 rupees`() {
        val bill = calculator.calculate(listOf(line("a", 20_00, quantity = 8)), distanceKm = 1.0)
        assertThat(bill.packagingFeePaise).isEqualTo(30_00)
    }

    @Test
    fun `delivery becomes free exactly at the threshold`() {
        val atThreshold = calculator.calculate(listOf(line("a", 499_00)), distanceKm = 4.0)
        assertThat(atThreshold.deliveryFeePaise).isEqualTo(0)
        assertThat(atThreshold.deliveryFeeBeforeWaiverPaise).isEqualTo(42_00)
        assertThat(atThreshold.isDeliveryFree).isTrue()
        assertThat(atThreshold.amountToFreeDeliveryPaise).isEqualTo(0)

        val justBelow = calculator.calculate(listOf(line("a", 498_00)), distanceKm = 4.0)
        assertThat(justBelow.deliveryFeePaise).isEqualTo(42_00)
        assertThat(justBelow.amountToFreeDeliveryPaise).isEqualTo(1_00)
    }

    @Test
    fun `percentage coupon below its cap discounts the exact percentage`() {
        val bill = calculator.calculate(listOf(line("a", 160_00)), distanceKm = 2.0, couponCode = "WELCOME50")

        assertThat(bill.appliedCoupon).isEqualTo(CouponCatalog.WELCOME50)
        assertThat(bill.couponDiscountPaise).isEqualTo(80_00)
        // GST is charged after the discount: 5% of (160 − 80 + 5)
        assertThat(bill.gstPaise).isEqualTo(4_25)
        assertThat(bill.totalPaise).isEqualTo(85_00 + 4_25 + 35_00)
    }

    @Test
    fun `percentage coupons respect their maximum discount`() {
        val welcome = calculator.calculate(listOf(line("a", 400_00)), distanceKm = 2.0, couponCode = "WELCOME50")
        assertThat(welcome.couponDiscountPaise).isEqualTo(100_00)

        val sweet = calculator.calculate(listOf(line("a", 500_00)), distanceKm = 2.0, couponCode = "SWEET20")
        assertThat(sweet.couponDiscountPaise).isEqualTo(80_00)
    }

    @Test
    fun `coupon below minimum order is reported and not applied`() {
        val bill = calculator.calculate(listOf(line("a", 100_00)), distanceKm = 2.0, couponCode = "WELCOME50")

        assertThat(bill.appliedCoupon).isNull()
        assertThat(bill.couponDiscountPaise).isEqualTo(0)
        val error = bill.couponError as CouponError.MinOrderNotMet
        assertThat(error.shortByPaise).isEqualTo(49_00)
        assertThat(error.coupon.code).isEqualTo("WELCOME50")
    }

    @Test
    fun `free delivery coupon waives the distance-based fee`() {
        val bill = calculator.calculate(listOf(line("a", 250_00)), distanceKm = 4.0, couponCode = "FREEDEL")

        assertThat(bill.deliveryFeePaise).isEqualTo(0)
        assertThat(bill.deliverySavingsPaise).isEqualTo(42_00)
        assertThat(bill.couponDiscountPaise).isEqualTo(0)
        assertThat(bill.totalSavingsPaise).isEqualTo(42_00)
    }

    @Test
    fun `free delivery savings are not double counted when the threshold is already met`() {
        val bill = calculator.calculate(listOf(line("a", 520_00)), distanceKm = 2.0, couponCode = "FREEDEL")
        assertThat(bill.totalSavingsPaise).isEqualTo(35_00)
    }

    @Test
    fun `flat coupon subtracts a fixed amount`() {
        val bill = calculator.calculate(listOf(line("a", 600_00)), distanceKm = 2.0, couponCode = "FEAST120")
        assertThat(bill.couponDiscountPaise).isEqualTo(120_00)
        assertThat(bill.gstPaise).isEqualTo(24_25) // 5% of (600 − 120 + 5)
    }

    @Test
    fun `flat discount never exceeds the item total`() {
        val huge = Coupon("BIG", "Flat ₹500", "", CouponType.FlatOff(500_00), minOrderPaise = 0)
        assertThat(calculator.discountFor(huge, itemTotalPaise = 300_00)).isEqualTo(300_00)
    }

    @Test
    fun `coupon codes are trimmed and case-insensitive`() {
        val validation = calculator.validateCoupon("  welcome50 ", itemTotalPaise = 200_00)
        assertThat(validation).isEqualTo(CouponValidation.Valid(CouponCatalog.WELCOME50))
    }

    @Test
    fun `unknown coupon is rejected with the normalised code`() {
        val validation = calculator.validateCoupon("bogus10", itemTotalPaise = 200_00)
        assertThat(validation).isEqualTo(CouponValidation.Invalid(CouponError.NotFound("BOGUS10")))
    }

    @Test
    fun `validating against an empty cart is rejected`() {
        assertThat(calculator.validateCoupon("WELCOME50", 0)).isEqualTo(CouponValidation.Invalid(CouponError.EmptyCart))
    }

    @Test
    fun `GST is rounded half-up to the nearest paisa`() {
        // taxable = 95.10 + 5.00 = ₹100.10 → 5% = 500.5 paise → 501
        val bill = calculator.calculate(listOf(line("a", 95_10)), distanceKm = 1.0)
        assertThat(bill.gstPaise).isEqualTo(5_01)
    }

    @Test
    fun `tip is added to the total without tax`() {
        val withoutTip = calculator.calculate(listOf(line("a", 300_00)), distanceKm = 2.0)
        val withTip = calculator.calculate(listOf(line("a", 300_00)), distanceKm = 2.0, tipPaise = 30_00)

        assertThat(withTip.gstPaise).isEqualTo(withoutTip.gstPaise)
        assertThat(withTip.totalPaise - withoutTip.totalPaise).isEqualTo(30_00)
    }

    @Test
    fun `negative tip is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            calculator.calculate(listOf(line("a", 300_00)), distanceKm = 2.0, tipPaise = -1)
        }
    }

    @Test
    fun `blank coupon code is ignored`() {
        val bill = calculator.calculate(listOf(line("a", 300_00)), distanceKm = 2.0, couponCode = "  ")
        assertThat(bill.couponError).isNull()
        assertThat(bill.appliedCoupon).isNull()
    }
}
