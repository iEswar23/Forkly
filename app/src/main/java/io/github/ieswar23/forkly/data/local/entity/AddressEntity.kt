package io.github.ieswar23.forkly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val customLabel: String,
    val houseDetails: String,
    val area: String,
    val landmark: String,
    val city: String,
    val pincode: String,
    val receiverName: String,
    val receiverPhone: String,
    val createdAt: Long,
)
