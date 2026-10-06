package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [FighterEntity].
 */
@Dao
interface FighterDao {
    @Query("SELECT * FROM fighters ORDER BY name ASC")
    fun getAllFighters(): Flow<List<FighterEntity>>

    @Query("SELECT * FROM fighters WHERE isPlayer = :isPlayer ORDER BY name ASC")
    fun getFightersByOwnership(isPlayer: Boolean): Flow<List<FighterEntity>>

    @Query("SELECT * FROM fighters WHERE id = :id")
    suspend fun getFighterById(id: String): FighterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFighter(fighter: FighterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFighters(fighters: List<FighterEntity>)

    @Update
    suspend fun updateFighter(fighter: FighterEntity)

    @Delete
    suspend fun deleteFighter(fighter: FighterEntity)

    @Query("DELETE FROM fighters WHERE id = :id")
    suspend fun deleteFighterById(id: String)

    @Query("DELETE FROM fighters")
    suspend fun deleteAllFighters()
}
