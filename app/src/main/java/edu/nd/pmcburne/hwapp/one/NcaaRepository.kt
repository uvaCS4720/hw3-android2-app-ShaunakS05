package edu.nd.pmcburne.hwapp.one

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NcaaRepository(
    private val dao: NcaaGameDao
) {
    suspend fun loadGames(
        context: Context,
        dateMillis: Long,
        isWomen: Boolean
    ): Pair<List<NcaaGame>, Boolean> {
        val gender = if (isWomen) "women" else "men"
        val dateKey = apiDateKey(dateMillis)

        if (isOnline(context)) {
            return try {
                val remoteGames = fetchNcaaGames(dateMillis, isWomen)

                dao.replaceGamesForDay(
                    gender = gender,
                    dateKey = dateKey,
                    games = remoteGames.map { it.toEntity(gender, dateKey) }
                )

                remoteGames to false
            } catch (e: Exception) {
                dao.getGamesForDay(gender, dateKey).map { it.toModel() } to true
            }
        }

        return dao.getGamesForDay(gender, dateKey).map { it.toModel() } to true
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}