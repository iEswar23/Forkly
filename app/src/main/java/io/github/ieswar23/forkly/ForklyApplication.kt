package io.github.ieswar23.forkly

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.ieswar23.forkly.data.repository.AddressRepository
import io.github.ieswar23.forkly.data.repository.OrderRepository
import io.github.ieswar23.forkly.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ForklyApplication : Application() {

    @Inject lateinit var addressRepository: AddressRepository
    @Inject lateinit var orderRepository: OrderRepository

    @Inject
    @field:ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            addressRepository.seedDefaultsIfEmpty()
            orderRepository.seedHistoryIfNeeded()
            orderRepository.resumeActiveOrders()
        }
    }
}
