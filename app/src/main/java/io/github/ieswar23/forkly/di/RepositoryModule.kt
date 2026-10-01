package io.github.ieswar23.forkly.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ieswar23.forkly.data.repository.AddressRepository
import io.github.ieswar23.forkly.data.repository.AddressRepositoryImpl
import io.github.ieswar23.forkly.data.repository.CartRepository
import io.github.ieswar23.forkly.data.repository.CartRepositoryImpl
import io.github.ieswar23.forkly.data.repository.DataStorePreferencesRepository
import io.github.ieswar23.forkly.data.repository.OrderRepository
import io.github.ieswar23.forkly.data.repository.OrderRepositoryImpl
import io.github.ieswar23.forkly.data.repository.PreferencesRepository
import io.github.ieswar23.forkly.data.repository.RestaurantRepository
import io.github.ieswar23.forkly.data.repository.RestaurantRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindRestaurantRepository(impl: RestaurantRepositoryImpl): RestaurantRepository

    @Binds @Singleton
    abstract fun bindCartRepository(impl: CartRepositoryImpl): CartRepository

    @Binds @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds @Singleton
    abstract fun bindAddressRepository(impl: AddressRepositoryImpl): AddressRepository

    @Binds @Singleton
    abstract fun bindPreferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository
}
