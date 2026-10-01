package io.github.ieswar23.forkly.data.repository

import io.github.ieswar23.forkly.data.local.dao.AddressDao
import io.github.ieswar23.forkly.data.mapper.toDomain
import io.github.ieswar23.forkly.data.mapper.toEntity
import io.github.ieswar23.forkly.di.IoDispatcher
import io.github.ieswar23.forkly.domain.model.Address
import io.github.ieswar23.forkly.domain.model.AddressLabel
import io.github.ieswar23.forkly.util.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface AddressRepository {
    val addresses: Flow<List<Address>>

    /** The address orders are delivered to; falls back to the first saved address. */
    val selectedAddress: Flow<Address?>

    suspend fun save(address: Address): Long
    suspend fun delete(id: Long)
    suspend fun select(id: Long)
    suspend fun seedDefaultsIfEmpty()
}

@Singleton
class AddressRepositoryImpl @Inject constructor(
    private val dao: AddressDao,
    private val preferences: PreferencesRepository,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AddressRepository {

    override val addresses: Flow<List<Address>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(io)

    override val selectedAddress: Flow<Address?> =
        combine(addresses, preferences.preferences) { list, prefs ->
            list.firstOrNull { it.id == prefs.selectedAddressId } ?: list.firstOrNull()
        }

    override suspend fun save(address: Address): Long = withContext(io) {
        val createdAt = if (address.id == 0L) clock.now() else dao.getById(address.id)?.createdAt ?: clock.now()
        val id = dao.upsert(address.toEntity(createdAt))
        if (address.id == 0L) preferences.setSelectedAddressId(id)
        id
    }

    override suspend fun delete(id: Long) = withContext(io) {
        dao.delete(id)
        val prefs = preferences.preferences.first()
        if (prefs.selectedAddressId == id) preferences.setSelectedAddressId(null)
    }

    override suspend fun select(id: Long) = preferences.setSelectedAddressId(id)

    override suspend fun seedDefaultsIfEmpty() = withContext(io) {
        if (dao.count() > 0) return@withContext
        val defaults = listOf(
            Address(
                label = AddressLabel.HOME,
                houseDetails = "Flat 302, Lakeview Residency",
                area = "Road No. 12, Banjara Hills",
                landmark = "Opp. KBR Park gate 3",
                city = "Hyderabad",
                pincode = "500034",
                receiverName = "Aarav Reddy",
                receiverPhone = "+91 98480 12345",
            ),
            Address(
                label = AddressLabel.WORK,
                houseDetails = "Tower B, 7th floor, Skyview Tech Park",
                area = "HITEC City, Madhapur",
                landmark = "Near Cyber Towers",
                city = "Hyderabad",
                pincode = "500081",
                receiverName = "Aarav Reddy",
                receiverPhone = "+91 98480 12345",
            ),
        )
        defaults.forEachIndexed { index, address -> dao.upsert(address.toEntity(clock.now() + index)) }
    }
}
