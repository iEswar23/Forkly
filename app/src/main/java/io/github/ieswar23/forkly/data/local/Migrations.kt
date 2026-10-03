package io.github.ieswar23.forkly.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v2: orders can be scheduled for a delivery slot. Existing orders were all "deliver now" (NULL). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `orders` ADD COLUMN `scheduledFor` INTEGER")
    }
}

/** Every schema migration, oldest first. Order history survives app updates. */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
