package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedNetworkDao {

  @Query("SELECT * FROM redes_detectadas ORDER BY fecha_registro DESC")
  fun getAllNetworks(): Flow<List<ScannedNetworkEntity>>

  @Query("SELECT * FROM redes_detectadas WHERE bssid_mac = :bssidMac LIMIT 1")
  suspend fun getNetworkByBssid(bssidMac: String): ScannedNetworkEntity?

  @Upsert
  suspend fun upsertNetwork(entity: ScannedNetworkEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(entities: List<ScannedNetworkEntity>)

  @Query("SELECT COUNT(*) FROM redes_detectadas")
  fun getNetworksCount(): Flow<Int>

  @Query("DELETE FROM redes_detectadas WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM redes_detectadas")
  suspend fun clearAll()
}
