package io.github.ieswar23.forkly.screenshots

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import io.github.ieswar23.forkly.MainActivity
import io.github.ieswar23.forkly.data.local.dao.OrderDao
import io.github.ieswar23.forkly.data.repository.AddressRepository
import io.github.ieswar23.forkly.data.repository.CartRepository
import io.github.ieswar23.forkly.data.repository.OrderRepository
import io.github.ieswar23.forkly.data.repository.RestaurantRepository
import io.github.ieswar23.forkly.data.repository.SeedOrders
import io.github.ieswar23.forkly.di.ClockModule
import io.github.ieswar23.forkly.domain.model.CartLine
import io.github.ieswar23.forkly.domain.model.CustomizationSelection
import io.github.ieswar23.forkly.domain.model.OrderStatus
import io.github.ieswar23.forkly.domain.pricing.PricingCalculator
import io.github.ieswar23.forkly.util.Clock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Renders the real app (MainActivity + Hilt graph + Room + Retrofit/MockInterceptor) on the JVM with
 * Robolectric native graphics and records README screenshots with Roborazzi.
 *
 * `./gradlew recordRoborazziDebug` writes the PNGs to `docs/screenshots/`. A plain `testDebugUnitTest`
 * still runs every flow end to end (catching crashes), but skips image capture.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
@HiltAndroidTest
@UninstallModules(ClockModule::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, sdk = [34], qualifiers = PHONE)
class AppScreenshotTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Real time, but always starting at 5:40 PM today, so time-dependent screens (delivery slots)
     * look the same whenever the screenshots are recorded.
     */
    @BindValue
    @JvmField
    val clock: Clock = clockStartingTodayAt(hour = 17, minute = 40)

    @Inject lateinit var restaurantRepository: RestaurantRepository
    @Inject lateinit var cartRepository: CartRepository
    @Inject lateinit var addressRepository: AddressRepository
    @Inject lateinit var orderRepository: OrderRepository
    @Inject lateinit var orderDao: OrderDao
    @Inject lateinit var pricing: PricingCalculator

    @Before
    fun setUp() {
        hiltRule.inject()
        // Mirrors ForklyApplication.onCreate (HiltTestApplication replaces it in tests).
        runBlocking {
            addressRepository.seedDefaultsIfEmpty()
            orderRepository.seedHistoryIfNeeded()
        }
    }

    @Test
    fun home() {
        runBlocking { fillCart() }
        waitForText("Top rated near you")
        settle()
        capture("01_home")
    }

    @Test
    fun restaurantMenu() {
        runBlocking { fillCart() }
        openShahiDastarkhwan()
        capture("02_restaurant")
    }

    @Test
    fun customizationSheet() {
        openShahiDastarkhwan()
        composeRule.onAllNodesWithText("ADD").onFirst().performClick()
        waitForText("Customise as per your taste")
        settle()
        captureScreen("03_customize")
    }

    @Test
    fun search() {
        waitForText("Top rated near you")
        composeRule.onAllNodesWithText("Search").onFirst().performClick()
        waitUntil { composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(hasSetTextAction()).performTextInput("biryani")
        // Dish matches come from the menus the repository prefetches in the background.
        waitUntil {
            composeRule.onAllNodes(hasText("Dishes (", substring = true)).fetchSemanticsNodes().any { node ->
                node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { !it.text.endsWith("(0)") }
            }
        }
        composeRule.onNode(hasText("Dishes (", substring = true)).performClick()
        waitForText("Mutton Dum Biryani")
        settle()
        capture("04_search")
    }

    @Test
    fun cartWithCoupon() {
        runBlocking { fillCart() }
        waitForText("Top rated near you")
        composeRule.onAllNodesWithText("Cart").onFirst().performClick()
        waitForText("'FEAST120' applied")
        // Coupon, tip and the full bill in one frame.
        composeRule.onNode(verticalList).performScrollToIndex(2)
        settle()
        capture("05_cart")
    }

    @Test
    fun orderTracking() {
        runBlocking { insertActiveOrder() }
        waitForText("Top rated near you")
        composeRule.onAllNodesWithText("Orders").onFirst().performClick()
        waitForText("Track order")
        composeRule.onNodeWithText("Track order").performClick()
        waitForText("Arriving in")
        settle()
        capture("06_tracking")
    }

    @Test
    @Config(qualifiers = "+night")
    fun homeDark() {
        runBlocking { fillCart() }
        waitForText("Top rated near you")
        settle()
        capture("07_home_dark")
    }

    @Test
    fun splitBill() {
        runBlocking { fillCart() }
        waitForText("Top rated near you")
        composeRule.onAllNodesWithText("Cart").onFirst().performClick()
        waitForText("'FEAST120' applied")
        composeRule.onNode(verticalList).performScrollToNode(hasText("Split bill"))
        composeRule.onNodeWithText("Split bill").performClick()
        waitForText("Each person pays")
        repeat(2) {
            composeRule.onNodeWithContentDescription("Add a person").performClick()
            composeRule.waitForIdle()
        }
        // The orderer covers the rider tip; friends split the rest.
        composeRule.onNodeWithText("Split the rider tip").performClick()
        waitForText("Friend 3")
        settle()
        captureScreen("08_split_bill")
    }

    @Test
    fun scheduleDelivery() {
        runBlocking { fillCart() }
        waitForText("Top rated near you")
        composeRule.onAllNodesWithText("Cart").onFirst().performClick()
        waitForText("Proceed to checkout")
        composeRule.onNodeWithText("Proceed to checkout").performClick()
        waitForText("Delivery time")
        // "2 Delivery time" sits after the address cards, the add button and the instructions.
        val deliveryTimeIndex = runBlocking { addressRepository.addresses.first().size } + 3
        composeRule.onNode(verticalList).performScrollToIndex(deliveryTimeIndex)
        composeRule.onNodeWithText("Schedule").performClick()
        waitForText("7:30 PM")
        composeRule.onNodeWithText("7:30 PM").performClick()
        waitForText("Arriving today, 7:30 – 8:00 PM")
        composeRule.onNode(verticalList).performScrollToIndex(deliveryTimeIndex)
        settle()
        capture("09_schedule_delivery")
    }

    // region helpers

    private val verticalList = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    private fun openShahiDastarkhwan() {
        waitForText("Top rated near you")
        composeRule.onNode(verticalList).performScrollToNode(hasText("Shahi Dastarkhwan"))
        composeRule.onAllNodesWithText("Shahi Dastarkhwan").onFirst().performClick()
        waitForText("Hyderabadi Chicken Dum Biryani")
        settle()
    }

    /** A realistic cart built from the real menu (via the real repositories) with a coupon and tip. */
    private suspend fun fillCart() {
        restaurantRepository.refresh().getOrThrow()
        restaurantRepository.ensureMenu(SHAHI).getOrThrow()
        val items = restaurantRepository.getMenuItems(
            listOf("$SHAHI-01", "$SHAHI-07", "$SHAHI-14", "$SHAHI-16"),
        ).associateBy { it.id }
        val quantities = mapOf("$SHAHI-01" to 2, "$SHAHI-07" to 1, "$SHAHI-14" to 2, "$SHAHI-16" to 1)
        quantities.forEach { (id, qty) ->
            val item = items.getValue(id)
            val selection = if (item.isCustomizable) CustomizationSelection.defaultFor(item) else null
            cartRepository.add(CartLine.from(item, selection, qty))
        }
        cartRepository.setCoupon("FEAST120")
        cartRepository.setTip(30_00)
    }

    /** An order that is currently out for delivery (~75 s into the accelerated 2-minute timeline). */
    private suspend fun insertActiveOrder() {
        val now = clock.now()
        val (order, items) = SeedOrders.build(now, pricing).first()
        orderDao.insert(
            order.copy(
                id = "FK52907314",
                status = OrderStatus.OUT_FOR_DELIVERY.name,
                placedAt = now - TimeUnit.SECONDS.toMillis(75),
                deliveredAt = null,
                userRating = null,
                riderName = "Syed Farhan",
                riderVehicle = "TS 07 GK 2291",
            ),
            items.map { it.copy(orderId = "FK52907314") },
        )
    }

    private fun waitForText(text: String) = waitUntil {
        composeRule.onAllNodesWithText(text, substring = false).fetchSemanticsNodes().isNotEmpty()
    }

    /**
     * Polls with real time (the mock API sleeps 300–700 ms on OkHttp threads) while also advancing
     * Robolectric's paused main looper, so delayed main-thread work keeps flowing.
     */
    private fun waitUntil(timeoutMs: Long = 15_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            composeRule.waitForIdle()
            if (condition()) return
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for the UI" }
            Thread.sleep(50)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
        }
    }

    /** Lets pending loads, enter transitions and animations finish before capturing. */
    private fun settle() {
        repeat(10) {
            Thread.sleep(50)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            composeRule.mainClock.advanceTimeBy(100)
            composeRule.waitForIdle()
        }
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage("$OUTPUT_DIR/$name.png", roborazziOptions = OPTIONS)
    }

    /** Captures every window, so modal bottom sheets (which live in their own window) are included. */
    private fun captureScreen(name: String) {
        captureScreenRoboImage("$OUTPUT_DIR/$name.png", roborazziOptions = OPTIONS)
    }

    // endregion

    private companion object {
        const val SHAHI = "shahi-dastarkhwan"
        const val OUTPUT_DIR = "../docs/screenshots"
        val OPTIONS = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5))
    }
}

private fun clockStartingTodayAt(hour: Int, minute: Int): Clock {
    val start = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val realStart = System.currentTimeMillis()
    return Clock { start + (System.currentTimeMillis() - realStart) }
}

/** Pixel 7-sized phone. */
private const val PHONE = "w411dp-h891dp-normal-long-notround-xxhdpi"
