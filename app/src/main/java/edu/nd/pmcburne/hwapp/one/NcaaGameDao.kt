package edu.nd.pmcburne.hwapp.one

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface NcaaGameDao {
    @Query("""
        SELECT * FROM games
        WHERE gender = :gender AND dateKey = :dateKey
        ORDER BY startTime
    """)
    suspend fun getGamesForDay(gender: String, dateKey: String): List<NcaaGameEntity>

    @Query("DELETE FROM games WHERE gender = :gender AND dateKey = :dateKey")
    suspend fun deleteGamesForDay(gender: String, dateKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<NcaaGameEntity>)

    @Transaction
    suspend fun replaceGamesForDay(
        gender: String,
        dateKey: String,
        games: List<NcaaGameEntity>
    ) {
        deleteGamesForDay(gender, dateKey)
        insertAll(games)
    }
}