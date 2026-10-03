package io.github.ieswar23.forkly.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ieswar23.forkly.data.local.ALL_MIGRATIONS
import io.github.ieswar23.forkly.data.local.ForklyDatabase
import io.github.ieswar23.forkly.data.local.dao.AddressDao
import io.github.ieswar23.forkly.data.local.dao.CartDao
import io.github.ieswar23.forkly.data.local.dao.MenuDao
import io.github.ieswar23.forkly.data.local.dao.OrderDao
import io.github.ieswar23.forkly.data.local.dao.RestaurantDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ForklyDatabase =
        Room.databaseBuilder(context, ForklyDatabase::class.java, ForklyDatabase.NAME)
            .addMigrations(*ALL_MIGRATIONS)
            // Only for paths without a migration (e.g. a downgrade); the cache refills from the API.
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideRestaurantDao(db: ForklyDatabase): RestaurantDao = db.restaurantDao()
    @Provides fun provideMenuDao(db: ForklyDatabase): MenuDao = db.menuDao()
    @Provides fun provideCartDao(db: ForklyDatabase): CartDao = db.cartDao()
    @Provides fun provideOrderDao(db: ForklyDatabase): OrderDao = db.orderDao()
    @Provides fun provideAddressDao(db: ForklyDatabase): AddressDao = db.addressDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("forkly_prefs") }
}
