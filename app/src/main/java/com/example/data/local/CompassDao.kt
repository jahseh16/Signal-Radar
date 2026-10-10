package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompassDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWaypoint(waypoint: CompassWaypointEntity): Long

  @Delete
  suspend fun deleteWaypoint(waypoint: CompassWaypointEntity)

  @Query("DELETE FROM compass_waypoints WHERE id = :id")
  suspend fun deleteWaypointById(id: Long)

  @Query("SELECT * FROM compass_waypoints ORDER BY created_at DESC")
  fun getAllWaypoints(): Flow<List<CompassWaypointEntity>>

  @Query("SELECT COUNT(*) FROM compass_waypoints")
  fun getWaypointsCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrackPoint(point: CompassTrackPointEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrackPoints(points: List<CompassTrackPointEntity>)

  @Query("SELECT * FROM compass_track_points WHERE session_key = :sessionKey ORDER BY timestamp ASC")
  fun getTrackPointsForSession(sessionKey: String): Flow<List<CompassTrackPointEntity>>

  @Query("SELECT * FROM compass_track_points ORDER BY timestamp DESC LIMIT :limit")
  fun getRecentTrackPoints(limit: Int): Flow<List<CompassTrackPointEntity>>

  @Query("DELETE FROM compass_track_points WHERE session_key = :sessionKey")
  suspend fun clearSessionTrackPoints(sessionKey: String)

  @Query("DELETE FROM compass_track_points")
  suspend fun clearAllTrackPoints()
}
