package io.github.ieswar23.forkly.screenshots

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.ieswar23.forkly.data.local.ForklyDatabase
import io.github.ieswar23.forkly.data.local.dao.AddressDao
import io.github.ieswar23.forkly.data.local.dao.CartDao
import io.github.ieswar23.forkly.data.local.dao.MenuDao
import io.github.ieswar23.forkly.data.local.dao.OrderDao
import io.github.ieswar23.forkly.data.local.dao.RestaurantDao
import io.github.ieswar23.forkly.di.DataModule
import java.io.File
import java.util.UUID
import javax.inject.Singleton

/**
 * Same graph as [DataModule], but every test gets a fresh in-memory database and its own
 * DataStore file, so screenshot tests never leak state into each other.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])
object TestDataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ForklyDatabase =
        Room.inMemoryDatabaseBuilder(context, ForklyDatabase::class.java).build()

    @Provides fun provideRestaurantDao(db: ForklyDatabase): RestaurantDao = db.restaurantDao()
    @Provides fun provideMenuDao(db: ForklyDatabase): MenuDao = db.menuDao()
    @Provides fun provideCartDao(db: ForklyDatabase): CartDao = db.cartDao()
    @Provides fun provideOrderDao(db: ForklyDatabase): OrderDao = db.orderDao()
    @Provides fun provideAddressDao(db: ForklyDatabase): AddressDao = db.addressDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            File(context.cacheDir, "test_prefs_${UUID.randomUUID()}.preferences_pb")
        }
}
