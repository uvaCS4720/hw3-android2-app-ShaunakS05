package edu.nd.pmcburne.hwapp.one

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import edu.nd.pmcburne.hwapp.one.ui.theme.HWStarterRepoTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.height
import java.util.TimeZone
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

data class NcaaGame(
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HWStarterRepoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DatePickerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerScreen(modifier: Modifier = Modifier) {
    var showDatePicker by remember {mutableStateOf(false)}
    var selectedDateMillis by remember{ mutableStateOf<Long?>(null)}

    val datePickerState = rememberDatePickerState()
    var isWomen by remember {mutableStateOf(false)}
    var games by remember {mutableStateOf<List<NcaaGame>>(emptyList())}
    var isLoading by remember {mutableStateOf(false)}
    var errorMessage by remember {mutableStateOf<String?>(null)}
    var refreshKey by remember {mutableStateOf(0)}


    LaunchedEffect(selectedDateMillis, isWomen, refreshKey) {
        val pickedDate = selectedDateMillis
        if (pickedDate == null){
            games = emptyList()
            errorMessage = null
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        try {
            games = fetchNcaaGames(
                dateMillis = pickedDate,
                isWomen = isWomen
            )
        } catch (e: Exception) {
            games = emptyList()
            errorMessage = e.message ?: "Failed to load games."
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text (text = "Selected date: ${formatDate(selectedDateMillis)}")
        Text (text = "Selected: ${if (isWomen) "Women" else "Men"}")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {showDatePicker=true}) {
                Text("Pick a date")
        }
            Spacer(modifier = Modifier.width(20.dp))

            Text("Men")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isWomen,
                onCheckedChange = { isWomen = it}
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Women")
        }

        Spacer(modifier = Modifier.height(16.dp))

        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = {
                if (selectedDateMillis != null) {
                    refreshKey++
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {

            when {
                selectedDateMillis == null -> {
                    Text("Choose a date to load NCAA games.")
                }

                isLoading -> {
                    CircularProgressIndicator()
                }

                errorMessage != null -> {
                    Text("Error: $errorMessage")
                }

                games.isEmpty() -> {
                    Text("No games found for selected date")
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(games) { game ->
                            GameRow(game)
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker){
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false},
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false}
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun GameRow(game: NcaaGame) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
    ) {
        Text(text = "Away: ${game.awayTeam}")
        Text(text = "Home: ${game.homeTeam}")
        Text(text = "Status: ${statusText(game)}")
        if (isInProgress(game) || isFinal(game)) {
            Text(text = "Score: ${displayScore(game)}")
        } else {
            Text(text = "Start time: ${game.startTime}")
        }

        if (isInProgress(game)){
            Text(text = "Period / Time remaining: ${periodClockText(game)}")
        }

        if (isFinal(game)){
            Text(text= "Period / Time remaining: Final")
        }

        winnerText(game)?.let {
            Text(text = it)
        }
    }
}

private fun displayScore(game: NcaaGame): String {
    val away = if (game.awayScore.isBlank()) "-" else game.awayScore
    val home = if (game.homeScore.isBlank()) "-" else game.homeScore
    return "${game.awayTeam} $away - $home ${game.homeTeam}"
}

private suspend fun fetchNcaaGames(dateMillis: Long, isWomen: Boolean): List<NcaaGame> = withContext(
    Dispatchers.IO){
    val url = buildNcaaUrl(dateMillis, isWomen)
    val jsonText = URL(url).readText()
    parseNcaaGames(jsonText)
}

private fun buildNcaaUrl(dateMillis: Long, isWomen: Boolean): String{
    val gender = if (isWomen) "women" else "men"
    val formatter  = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    val datePath = formatter.format(Date(dateMillis))
    return "https://ncaa-api.henrygd.me/scoreboard/basketball-$gender/d1/$datePath"
}

private fun parseNcaaGames(jsonText: String): List<NcaaGame>{
    val root = JSONObject(jsonText)
    val gamesArray = root.optJSONArray("games") ?: return emptyList()

    val results = mutableListOf<NcaaGame>()

    for(i in 0 until gamesArray.length()){
        val wrapper = gamesArray.optJSONObject(i) ?: continue
        val game = wrapper.optJSONObject("game") ?: continue

        val away = game.optJSONObject("away")
        val home = game.optJSONObject("home")
        val awayNames = away?.optJSONObject("names")
        val homeNames = home?.optJSONObject("names")

        results.add(
            NcaaGame(
                id = game.optString("gameID"),
                awayTeam = awayNames?.optString("short").orEmpty(),
                homeTeam = homeNames?.optString("short").orEmpty(),
                awayScore = away?.optString("score").orEmpty(),
                homeScore = home?.optString("score").orEmpty(),
                startTime = game.optString("startTime"),
                gameState = game.optString("gameState"),
                currentPeriod = game.optString("currentPeriod"),
                contestClock = game.optString("contestClock"),
                finalMessage = game.optString("finalMessage"),
                awayWinner = away?.optBoolean("winner") == true,
                homeWinner = home?.optBoolean("winner") == true
            )
        )
    }
    return results
}

private fun isFinal(game: NcaaGame): Boolean{
    val state = game.gameState.lowercase()
    return "final" in state ||
            game.currentPeriod.equals("FINAL", ignoreCase = true) ||
            game.finalMessage.contains("final", ignoreCase = true)
}


private fun isInProgress(game: NcaaGame): Boolean {
    if (isFinal(game)) return false

    val state = game.gameState.lowercase()
    return "live" in state ||
            "progress" in state ||
            (
                game.currentPeriod.isNotBlank() &&
                        !game.currentPeriod.equals("FINAL", ignoreCase = true) &&
                        game.contestClock.isNotBlank()
                    )
}

private fun statusText(game: NcaaGame): String {
    return when {
        isFinal(game) -> "Finished"
        isInProgress(game) -> "Currently being played"
        else -> "Upcoming"
    }
}

private fun scoreText(game: NcaaGame): String {
    val away = if (game.awayScore.isBlank()) "-" else game.awayScore
    val home = if (game.homeScore.isBlank()) "-" else game.homeScore
    return "${game.awayTeam} $away - $home ${game.homeTeam}"
}

private fun winnerText(game: NcaaGame): String? {
    if (!isFinal(game)) return null

    return when{
        game.awayWinner -> "Winner: ${game.awayTeam}"
        game.homeWinner -> "Winner: ${game.homeTeam}"
        else -> null
    }
}

private fun periodClockText(game: NcaaGame): String {
    return if (isFinal(game)){
        "Final"
    } else {
        "${game.currentPeriod} - ${game.contestClock}"
    }
}

fun formatDate(millis: Long?): String {
    if (millis == null) return "No date selected"
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(millis))
}


@Preview(showBackground = true)
@Composable
fun DatePickerPreview() {
    HWStarterRepoTheme {
        DatePickerScreen()
    }
}