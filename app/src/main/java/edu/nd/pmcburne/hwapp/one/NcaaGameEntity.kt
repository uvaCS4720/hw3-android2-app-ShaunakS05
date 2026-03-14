package edu.nd.pmcburne.hwapp.one

import androidx.room.Entity

@Entity(
    tableName = "games",
    primaryKeys = ["gender", "dateKey", "id"]
)
data class NcaaGameEntity(
    val gender: String,
    val dateKey: String,
    val id: String,
    val awayTeam: String,
    val homeTeam: String,
    val awayScore: String,
    val homeScore: String,
    val startTime: String,
    val gameState: String,
    val currentPeriod: String,
    val contestClock: String,
    val finalMessage: String,
    val awayWinner: Boolean,
    val homeWinner: Boolean
)

fun NcaaGameEntity.toModel(): NcaaGame =
    NcaaGame(
        id = id,
        awayTeam = awayTeam,
        homeTeam = homeTeam,
        awayScore = awayScore,
        homeScore = homeScore,
        startTime = startTime,
        gameState = gameState,
        currentPeriod = currentPeriod,
        contestClock = contestClock,
        finalMessage = finalMessage,
        awayWinner = awayWinner,
        homeWinner = homeWinner
    )

fun NcaaGame.toEntity(gender: String, dateKey: String): NcaaGameEntity =
    NcaaGameEntity(
        gender = gender,
        dateKey = dateKey,
        id = id,
        awayTeam = awayTeam,
        homeTeam = homeTeam,
        awayScore = awayScore,
        homeScore = homeScore,
        startTime = startTime,
        gameState = gameState,
        currentPeriod = currentPeriod,
        contestClock = contestClock,
        finalMessage = finalMessage,
        awayWinner = awayWinner,
        homeWinner = homeWinner
    )