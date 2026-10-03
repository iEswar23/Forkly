package io.github.ieswar23.forkly.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.forkly.data.local.ALL_MIGRATIONS
import io.github.ieswar23.forkly.data.local.ForklyDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Opens a database written by schema version 1 with the current Room database, so Room runs the
 * migrations and then validates every table against today's entities. Existing orders must survive
 * and read back as "deliver now".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test
    fun `version 1 orders survive the upgrade and are not scheduled`() {
        createVersion1Database()

        val db = Room.databaseBuilder(context, ForklyDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val order = runBlocking { db.orderDao().getById("FK48213907") }
            assertThat(order).isNotNull()
            assertThat(order!!.order.restaurantName).isEqualTo("Shahi Dastarkhwan")
            assertThat(order.order.totalPaise).isEqualTo(1_043_70)
            assertThat(order.order.scheduledFor).isNull()
            assertThat(order.items.single().name).isEqualTo("Chicken 65")
            assertThat(db.openHelper.readableDatabase.version).isEqualTo(2)
        } finally {
            db.close()
        }
    }

    private fun createVersion1Database() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) = VERSION_1_SCHEMA.forEach(db::execSQL)
                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    },
                )
                .build(),
        )
        helper.writableDatabase.apply {
            execSQL(
                """
                INSERT INTO orders VALUES ('FK48213907', 'shahi-dastarkhwan', 'Shahi Dastarkhwan', '🍛', 'Banjara Hills',
                  95500, 2000, 0, 0, 4870, 2000, 104370, NULL, 'UPI', 'Home', 'Banjara Hills', '', 'DELIVERED',
                  1000, 3000, 'Mohammed Imran', '+91 99086 45120', 'TS 13 FA 7316', 4.8, 2400, 5)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO order_items (orderId, menuItemId, name, emoji, isVeg, quantity, unitPricePaise, customizationSummary, selectedOptionIds)
                VALUES ('FK48213907', 'shahi-dastarkhwan-07', 'Chicken 65', '🍗', 0, 1, 26900, '', '')
                """.trimIndent(),
            )
        }
        helper.close()
    }

    private companion object {
        /** The schema Room generated for version 1 (exportSchema is off, so it is recorded here). */
        val VERSION_1_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `restaurants` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `cuisines` TEXT NOT NULL, `area` TEXT NOT NULL, `rating` REAL NOT NULL, `ratingCount` INTEGER NOT NULL, `deliveryTimeMins` INTEGER NOT NULL, `costForTwo` INTEGER NOT NULL, `distanceKm` REAL NOT NULL, `isPureVeg` INTEGER NOT NULL, `offerText` TEXT, `offerCode` TEXT, `emoji` TEXT NOT NULL, `gradientStart` TEXT NOT NULL, `gradientEnd` TEXT NOT NULL, `tagline` TEXT NOT NULL, `openHours` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `menu_items` (`id` TEXT NOT NULL, `restaurantId` TEXT NOT NULL, `section` TEXT NOT NULL, `sectionOrder` INTEGER NOT NULL, `position` INTEGER NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `pricePaise` INTEGER NOT NULL, `isVeg` INTEGER NOT NULL, `isBestseller` INTEGER NOT NULL, `emoji` TEXT NOT NULL, `customizationsJson` TEXT NOT NULL, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_menu_items_restaurantId` ON `menu_items` (`restaurantId`)",
            "CREATE INDEX IF NOT EXISTS `index_menu_items_name` ON `menu_items` (`name`)",
            "CREATE TABLE IF NOT EXISTS `banners` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT NOT NULL, `emoji` TEXT NOT NULL, `gradientStart` TEXT NOT NULL, `gradientEnd` TEXT NOT NULL, `couponCode` TEXT, `category` TEXT, `position` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `favorites` (`restaurantId` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`restaurantId`))",
            "CREATE TABLE IF NOT EXISTS `recent_searches` (`query` TEXT NOT NULL, `searchedAt` INTEGER NOT NULL, PRIMARY KEY(`query`))",
            "CREATE TABLE IF NOT EXISTS `cart_items` (`lineId` TEXT NOT NULL, `restaurantId` TEXT NOT NULL, `menuItemId` TEXT NOT NULL, `name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `isVeg` INTEGER NOT NULL, `unitPricePaise` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, `customizationSummary` TEXT NOT NULL, `selectedOptionIds` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`lineId`))",
            "CREATE TABLE IF NOT EXISTS `cart_meta` (`id` INTEGER NOT NULL, `couponCode` TEXT, `tipPaise` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `orders` (`id` TEXT NOT NULL, `restaurantId` TEXT NOT NULL, `restaurantName` TEXT NOT NULL, `restaurantEmoji` TEXT NOT NULL, `restaurantArea` TEXT NOT NULL, `itemTotalPaise` INTEGER NOT NULL, `packagingFeePaise` INTEGER NOT NULL, `deliveryFeePaise` INTEGER NOT NULL, `discountPaise` INTEGER NOT NULL, `gstPaise` INTEGER NOT NULL, `tipPaise` INTEGER NOT NULL, `totalPaise` INTEGER NOT NULL, `couponCode` TEXT, `paymentMethod` TEXT NOT NULL, `addressLabel` TEXT NOT NULL, `addressLine` TEXT NOT NULL, `deliveryInstructions` TEXT NOT NULL, `status` TEXT NOT NULL, `placedAt` INTEGER NOT NULL, `deliveredAt` INTEGER, `riderName` TEXT NOT NULL, `riderPhone` TEXT NOT NULL, `riderVehicle` TEXT NOT NULL, `riderRating` REAL NOT NULL, `riderDeliveries` INTEGER NOT NULL, `userRating` INTEGER, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `order_items` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `orderId` TEXT NOT NULL, `menuItemId` TEXT NOT NULL, `name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `isVeg` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, `unitPricePaise` INTEGER NOT NULL, `customizationSummary` TEXT NOT NULL, `selectedOptionIds` TEXT NOT NULL, FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_order_items_orderId` ON `order_items` (`orderId`)",
            "CREATE TABLE IF NOT EXISTS `addresses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `label` TEXT NOT NULL, `customLabel` TEXT NOT NULL, `houseDetails` TEXT NOT NULL, `area` TEXT NOT NULL, `landmark` TEXT NOT NULL, `city` TEXT NOT NULL, `pincode` TEXT NOT NULL, `receiverName` TEXT NOT NULL, `receiverPhone` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3259a6f79293e9ce3469329225d97ff1')",
        )
    }
}
