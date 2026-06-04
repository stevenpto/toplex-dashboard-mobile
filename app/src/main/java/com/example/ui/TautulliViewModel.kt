package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface TestConnectionState {
    object Idle : TestConnectionState
    object Testing : TestConnectionState
    data class Success(val serverName: String) : TestConnectionState
    data class Error(val message: String) : TestConnectionState
}

class TautulliViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _serverType = MutableStateFlow("tautulli")
    val serverType: StateFlow<String> = _serverType.asStateFlow()

    private val _useDemoMode = MutableStateFlow(true)
    val useDemoMode: StateFlow<Boolean> = _useDemoMode.asStateFlow()

    private val _activityState = MutableStateFlow<UiState<TautulliSessions>>(UiState.Loading)
    val activityState: StateFlow<UiState<TautulliSessions>> = _activityState.asStateFlow()

    private val _historyState = MutableStateFlow<UiState<List<TautulliHistoryItem>>>(UiState.Loading)
    val historyState: StateFlow<UiState<List<TautulliHistoryItem>>> = _historyState.asStateFlow()

    private val _testState = MutableStateFlow<TestConnectionState>(TestConnectionState.Idle)
    val testState: StateFlow<TestConnectionState> = _testState.asStateFlow()

    // Library statistics fetched from Plex /library/sections endpoints (non-critical, non-blocking)
    private val _plexLibraryStats = MutableStateFlow<PlexLibraryStats?>(null)
    val plexLibraryStats: StateFlow<PlexLibraryStats?> = _plexLibraryStats.asStateFlow()

    private val _movieDetailState = MutableStateFlow<UiState<PlexMovieDetail>?>(null)
    val movieDetailState: StateFlow<UiState<PlexMovieDetail>?> = _movieDetailState.asStateFlow()

    // Holds mock sessions for real-time progress simulation
    private val _simulatedSessions = MutableStateFlow<List<TautulliSession>>(emptyList())
    private var simulationJob: Job? = null
    private var refreshJob: Job? = null
    private val plexUserMap = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Strip ALL whitespace (including embedded newlines from bad pastes) from tokens/keys. */
    private fun sanitizeToken(raw: String): String = raw.replace(Regex("\\s+"), "")

    init {
        viewModelScope.launch {
            try {
                // Read configuration on startup
                _serverUrl.value = prefs.serverUrl.first()
                _apiKey.value = sanitizeToken(prefs.apiKey.first())
                _serverType.value = prefs.serverType.first()
                _useDemoMode.value = prefs.useDemoMode.first()
            } catch (e: Exception) {
                // Fallback gracefully on datastore read error
                _serverUrl.value = ""
                _apiKey.value = ""
                _serverType.value = "tautulli"
                _useDemoMode.value = true
            }

            // Initialize correct mode
            if (_useDemoMode.value) {
                loadSimulatedData()
            } else {
                startPollingRealServer()
            }
        }
    }

    fun startPollingRealServer() {
        simulationJob?.cancel()
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                try {
                    fetchRealData()
                } catch (e: Exception) {
                    // Prevent crash in background loop
                }
                delay(10000) // Poll real server every 10 seconds
            }
        }
    }

    private fun loadSimulatedData() {
        refreshJob?.cancel()
        _activityState.value = UiState.Loading
        _historyState.value = UiState.Loading

        // Setup gorgeous simulated streams
        _simulatedSessions.value = listOf(
            TautulliSession(
                user = "steve_cinemaphile",
                username = "Steve",
                title = "Dune: Part Two",
                type = "movie",
                progressPercent = 38,
                state = "playing",
                sessionId = "sess_1",
                ratingKey = "rating_1",
                product = "Plex Media Player",
                player = "Apple TV 4K",
                ipAddress = "192.168.1.144",
                transcodeDecision = "direct play",
                videoResolution = "4K UHD",
                year = "2024",
                thumb = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=400&q=80"
            ),
            TautulliSession(
                user = "jessypinkman",
                username = "Jesse",
                title = "Ozymandias",
                type = "episode",
                progressPercent = 82,
                state = "playing",
                sessionId = "sess_2",
                ratingKey = "rating_2",
                product = "Plex Web",
                player = "Firefox (Windows)",
                ipAddress = "172.56.21.90",
                transcodeDecision = "transcode",
                videoResolution = "1080p (HEVC to H264)",
                year = "2013",
                grandparentTitle = "Breaking Bad",
                parentTitle = "Season 5",
                thumb = "https://images.unsplash.com/photo-1578301978693-85fa9c0320b9?w=400&q=80"
            ),
            TautulliSession(
                user = "cooper_nasa",
                username = "Cooper",
                title = "Interstellar",
                type = "movie",
                progressPercent = 54,
                state = "paused",
                sessionId = "sess_3",
                ratingKey = "rating_3",
                product = "Plex for Nvidia Shield",
                player = "Shield Android TV",
                ipAddress = "192.168.1.102",
                transcodeDecision = "direct stream",
                videoResolution = "1080p",
                year = "2014",
                thumb = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&q=80"
            )
        )

        val now = System.currentTimeMillis() / 1000L
        val wk = 604800L
        val mockHistory = listOf(
            TautulliHistoryItem(id=101, date=now-1800, duration=8100, friendlyName="Apple TV 4K", user="steve", title="Oppenheimer", type="movie", watchedStatus=1, player="Apple TV 4K", ipAddress="192.168.1.144", thumb="https://images.unsplash.com/photo-1444703686981-a3abbc4d4fe3?w=200&q=80", ratingKey="101"),
            TautulliHistoryItem(id=102, date=now-7200, duration=3300, friendlyName="iPad Pro", user="sarah_k", title="Connor's Wedding", parentTitle="Season 4", grandparentTitle="Succession", type="episode", watchedStatus=1, player="Plex for iOS", ipAddress="10.0.0.45", thumb="https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=200&q=80", grandparentThumb="https://images.unsplash.com/photo-1578022761797-b8636ac1773c?w=300&q=80", ratingKey="102"),
            TautulliHistoryItem(id=103, date=now-14400, duration=4500, friendlyName="Plex Web", user="mike_sh", title="Everything Everywhere All at Once", type="movie", watchedStatus=1, player="Chrome (OSX)", ipAddress="99.102.34.12", thumb="https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=200&q=80", ratingKey="103"),
            TautulliHistoryItem(id=104, date=now-86400, duration=2700, friendlyName="FireStick", user="sarah_k", title="Pilot", parentTitle="Season 1", grandparentTitle="Severance", type="episode", watchedStatus=1, player="Plex for FireTV", ipAddress="192.168.1.189", thumb="https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=200&q=80", grandparentThumb="https://images.unsplash.com/photo-1497366216548-37526070297c?w=300&q=80", ratingKey="104"),
            TautulliHistoryItem(id=105, date=now-wk-54000, duration=8200, friendlyName="LG WebOS TV", user="steve", title="Dune: Part Two", type="movie", watchedStatus=1, player="LG TV Client", ipAddress="192.168.1.115", thumb="https://images.unsplash.com/photo-1534447677768-be436bb09401?w=200&q=80", ratingKey="105"),
            TautulliHistoryItem(id=106, date=now-wk-108000, duration=2900, friendlyName="Shield TV", user="jesse", title="Ozymandias", parentTitle="Season 5", grandparentTitle="Breaking Bad", type="episode", watchedStatus=1, player="Shield Android TV", ipAddress="192.168.1.102", thumb="https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=200&q=80", grandparentThumb="https://images.unsplash.com/photo-1509347528160-9a9e33742cdb?w=300&q=80", ratingKey="106"),
            TautulliHistoryItem(id=107, date=now-2*wk-43200, duration=9900, friendlyName="Apple TV 4K", user="cooper", title="Interstellar", type="movie", watchedStatus=1, player="Apple TV 4K", ipAddress="10.0.0.77", thumb="https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=200&q=80", ratingKey="107"),
            TautulliHistoryItem(id=108, date=now-2*wk-90000, duration=2400, friendlyName="Plex Web", user="mike_sh", title="The Bear — S2 Premiere", parentTitle="Season 2", grandparentTitle="The Bear", type="episode", watchedStatus=1, player="Chrome (OSX)", ipAddress="99.102.34.12", thumb="https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=200&q=80", grandparentThumb="https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=300&q=80", ratingKey="108"),
            TautulliHistoryItem(id=109, date=now-3*wk-21600, duration=8100, friendlyName="Shield TV", user="cooper", title="Oppenheimer", type="movie", watchedStatus=1, player="Shield Android TV", ipAddress="192.168.1.102", thumb="https://images.unsplash.com/photo-1444703686981-a3abbc4d4fe3?w=200&q=80", ratingKey="109"),
            TautulliHistoryItem(id=110, date=now-3*wk-136800, duration=8400, friendlyName="Plex Web", user="sarah_k", title="The Shawshank Redemption", type="movie", watchedStatus=1, player="Firefox (Windows)", ipAddress="10.0.0.45", thumb="https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=200&q=80", ratingKey="110"),
            TautulliHistoryItem(id=111, date=now-4*wk-18000, duration=4500, friendlyName="iPhone 15", user="jesse", title="Everything Everywhere All at Once", type="movie", watchedStatus=1, player="Plex for iOS", ipAddress="172.56.21.90", thumb="https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=200&q=80", ratingKey="111"),
            TautulliHistoryItem(id=112, date=now-4*wk-72000, duration=2700, friendlyName="iPad Pro", user="sarah_k", title="The Confrontation", parentTitle="Season 1", grandparentTitle="Severance", type="episode", watchedStatus=1, player="Plex for iOS", ipAddress="10.0.0.45", thumb="https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=200&q=80", grandparentThumb="https://images.unsplash.com/photo-1497366216548-37526070297c?w=300&q=80", ratingKey="112")
        )

        _historyState.value = UiState.Success(mockHistory)

        // Launch Simulation loop to advance playing sessions by 1% each second
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                try {
                    val current = _simulatedSessions.value
                    val updated = current.map { session_item ->
                        if (session_item.state == "playing") {
                            val nextPct = (session_item.progressPercent ?: 0) + 1
                            session_item.copy(progressPercent = if (nextPct > 100) 0 else nextPct)
                        } else {
                            session_item
                        }
                    }
                    _simulatedSessions.value = updated

                    // Build sessions container
                    val sessionsContainer = TautulliSessions(
                        streamCount = updated.size,
                        sessions = updated,
                        totalBandwidth = "38700", // 38.7 Mbps
                        lanBandwidth = "25200",   // 25.2 Mbps
                        wanBandwidth = "13500"    // 13.5 Mbps
                    )
                    _activityState.value = UiState.Success(sessionsContainer)
                } catch (e: Exception) {
                    // Prevent crash inside simulation loop
                }
            }
        }
    }

    private suspend fun fetchRealData() {
        val url = _serverUrl.value
        val api = _apiKey.value
        val type = _serverType.value
        if (url.isEmpty() || api.isEmpty()) {
            _activityState.value = UiState.Error("Server credentials missing. Please set them up in Connection Settings.")
            _historyState.value = UiState.Error("Server credentials missing.")
            return
        }

        if (type == "plex") {
            fetchPlexData(url, api)
            return
        }

        try {
            val service = TautulliApiClient.getService(url)
            val activityResponse = service.getActivity(apiKey = api)
            if (activityResponse.response.result == "success") {
                val sessions = activityResponse.response.data ?: TautulliSessions()
                val updatedSessionsList = sessions.sessions?.map { session ->
                    val originalThumb = session.thumb ?: ""
                    val updatedThumb = if (originalThumb.isNotEmpty() && !originalThumb.startsWith("http")) {
                        var cleanUrl = url.trim()
                        if (!cleanUrl.endsWith("/")) cleanUrl = "$cleanUrl/"
                        val relative = originalThumb.removePrefix("/")
                        "${cleanUrl}api/v2?apikey=${api}&cmd=get_thumbnail&thumb=${relative}"
                    } else {
                        originalThumb
                    }
                    session.copy(thumb = updatedThumb)
                } ?: emptyList()
                _activityState.value = UiState.Success(sessions.copy(sessions = updatedSessionsList))
            } else {
                _activityState.value = UiState.Error(activityResponse.response.message ?: "Failed to load activity from server")
            }

            val historyResponse = service.getHistory(apiKey = api)
            if (historyResponse.response.result == "success") {
                _historyState.value = UiState.Success(historyResponse.response.data?.data ?: emptyList<TautulliHistoryItem>())
            } else {
                _historyState.value = UiState.Error(historyResponse.response.message ?: "Failed to load history")
            }
        } catch (e: Exception) {
            _activityState.value = UiState.Error("Connection error: ${e.localizedMessage ?: "Unknown error"}")
            _historyState.value = UiState.Error("Connection error.")
        }
    }

    private suspend fun fetchPlexData(baseUrl: String, rawToken: String) {
        val token = sanitizeToken(rawToken)
        withContext(Dispatchers.IO) {
            // Plex servers require client identification headers for authenticated endpoints.
            // Without these, /status/sessions (and other admin endpoints) return HTTP 401
            // even with a valid token — because Plex uses them to validate the request origin.
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .addHeader("X-Plex-Platform", "Android")
                        .addHeader("X-Plex-Client-Identifier", "toplex-dashboard-android")
                        .addHeader("X-Plex-Product", "Toplex Dashboard")
                        .addHeader("X-Plex-Version", "1.0.0")
                        .addHeader("X-Plex-Device", "Android Phone")
                        .addHeader("X-Plex-Provides", "controller")
                        .addHeader("X-Plex-Token", token)
                        .build()
                    chain.proceed(req)
                }
                .build()

            var sanitizedUrl = baseUrl.trim()
            if (!sanitizedUrl.startsWith("http://") && !sanitizedUrl.startsWith("https://")) {
                sanitizedUrl = "http://$sanitizedUrl"
            }
            if (!sanitizedUrl.endsWith("/")) {
                sanitizedUrl = "$sanitizedUrl/"
            }

            // 0. Fetch Users/Accounts map from Plex Server directly
            plexUserMap["1"] = "Owner" // server admin/owner is typically accountID 1
            try {
                val accountsRequest = Request.Builder()
                    .url("${sanitizedUrl}accounts?X-Plex-Token=$token&accept=json")
                    .build()
                val accountsResponse = client.newCall(accountsRequest).execute()
                val accountsBody = accountsResponse.body?.string() ?: ""
                if (accountsResponse.isSuccessful && accountsBody.isNotEmpty()) {
                    val accountsJson = JSONObject(accountsBody)
                    val mc = accountsJson.optJSONObject("MediaContainer")
                    val accountArray = mc?.optJSONArray("Account")
                    if (accountArray != null) {
                        for (i in 0 until accountArray.length()) {
                            val acc = accountArray.getJSONObject(i)
                            val id = acc.optString("id")
                            val name = acc.optString("name").ifEmpty { acc.optString("title").ifEmpty { acc.optString("username") } }
                            if (id.isNotEmpty() && name.isNotEmpty()) {
                                plexUserMap[id] = name
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 0b. Also fetch shared/friend accounts from Plex.tv — the local /accounts endpoint
            // only returns home/managed users. Friends (shared users) have large Plex.tv account
            // IDs (e.g. 281356194) that only appear in the Plex.tv friends list.
            try {
                val friendsRequest = Request.Builder()
                    .url("https://plex.tv/api/v2/friends?X-Plex-Token=$token")
                    .addHeader("Accept", "application/json")
                    .build()
                val friendsResponse = client.newCall(friendsRequest).execute()
                val friendsBody = friendsResponse.body?.string() ?: ""
                if (friendsResponse.isSuccessful && friendsBody.isNotEmpty()) {
                    val friendsArray = org.json.JSONArray(friendsBody)
                    for (i in 0 until friendsArray.length()) {
                        val friend = friendsArray.getJSONObject(i)
                        val id = friend.optString("id")
                        val name = friend.optString("username")
                            .ifEmpty { friend.optString("title") }
                            .ifEmpty { friend.optString("friendlyName") }
                        if (id.isNotEmpty() && name.isNotEmpty()) {
                            plexUserMap[id] = name
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 1. Fetch Sessions
            try {
                val sessionRequest = Request.Builder()
                    .url("${sanitizedUrl}status/sessions?X-Plex-Token=$token&accept=json")
                    .addHeader("Accept", "application/json")
                    .build()

                val sessionResponse = client.newCall(sessionRequest).execute()
                val sessionBody = sessionResponse.body?.string() ?: ""
                if (sessionResponse.isSuccessful && sessionBody.isNotEmpty()) {
                    val parsedSessions = parsePlexSessions(sessionBody, sanitizedUrl, token)
                    _activityState.value = UiState.Success(parsedSessions)
                } else if (sessionResponse.code == 401) {
                    _activityState.value = UiState.Error("Plex Auth Failed (401) — Token invalid or insufficient permissions")
                } else {
                    _activityState.value = UiState.Error("Plex Session Error: HTTP ${sessionResponse.code}")
                }
            } catch (e: Exception) {
                _activityState.value = UiState.Error("Plex Connection Error: ${e.localizedMessage ?: "Unknown error"}")
            }

            // 2. Fetch History — sorted newest-first so we always get the most recent plays
            // within the 1000-item page. mindate filters to year-to-date server-side, matching
            // what the web backend (plexapi plex.history(mindate=year_start)) returns.
            // If the server has "Track Play History" disabled this endpoint returns empty —
            // in that case we fall back to querying each library section for watched items.
            val startOfYearTs = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.MONTH, 0)
                set(java.util.Calendar.DAY_OF_MONTH, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis / 1000L
            try {
                val historyRequest = Request.Builder()
                    .url("${sanitizedUrl}status/sessions/history/all?sort=viewedAt:desc&mindate=$startOfYearTs&X-Plex-Token=$token&X-Plex-Container-Start=0&X-Plex-Container-Size=1000&accept=json")
                    .addHeader("Accept", "application/json")
                    .build()

                val historyResponse = client.newCall(historyRequest).execute()
                val historyBody = historyResponse.body?.string() ?: ""
                if (historyResponse.isSuccessful && historyBody.isNotEmpty()) {
                    val parsedHistory = parsePlexHistory(historyBody, sanitizedUrl, token, client)
                    if (parsedHistory.isNotEmpty()) {
                        _historyState.value = UiState.Success(parsedHistory)
                    } else {
                        // History endpoint returned a valid response but 0 items — likely
                        // "Track Play History" is disabled. Fall back to library watched items.
                        val fallback = fetchWatchedItemsFromLibrary(client, sanitizedUrl, token)
                        _historyState.value = UiState.Success(fallback)
                    }
                } else {
                    val fallback = fetchWatchedItemsFromLibrary(client, sanitizedUrl, token)
                    _historyState.value = UiState.Success(fallback)
                }
            } catch (e: Exception) {
                val fallback = fetchWatchedItemsFromLibrary(client, sanitizedUrl, token)
                _historyState.value = UiState.Success(fallback)
            }

            // Fetch library stats (films/shows added this year) — non-critical
            fetchPlexLibraryStats(client, sanitizedUrl, token)
        }
    }

    private fun parsePlexSessions(bodyString: String, baseUrl: String, token: String): TautulliSessions {
        val sessionsList = mutableListOf<TautulliSession>()
        try {
            val root = JSONObject(bodyString)
            val mc = root.optJSONObject("MediaContainer") ?: return TautulliSessions()
            val metadataArray = mc.optJSONArray("Metadata")
            
            if (metadataArray != null) {
                for (i in 0 until metadataArray.length()) {
                    val item = metadataArray.getJSONObject(i)
                    val userObj = item.optJSONObject("User")
                    val user = userObj?.optString("title") ?: "Plex User"
                    val userAccountId = userObj?.optString("id") ?: ""
                    
                    // Track username mapping dynamically
                    if (userAccountId.isNotEmpty() && user.isNotEmpty()) {
                        plexUserMap[userAccountId] = user
                    }
                    
                    val playerObj = item.optJSONObject("Player")
                    val state = playerObj?.optString("state") ?: "playing"
                    val player = playerObj?.optString("title") ?: "Plex Player"
                    val product = playerObj?.optString("product") ?: "Plex Client"
                    val ipAddress = playerObj?.optString("address") ?: ""
                    
                    val sessionObj = item.optJSONObject("Session")
                    val sessionId = sessionObj?.optString("id") ?: "plex_sess_$i"
                    
                    val transcodeObj = item.optJSONObject("TranscodeSession")
                    val videoDecision = transcodeObj?.optString("videoDecision") ?: "direct play"
                    
                    val viewOffset = item.optLong("viewOffset", 0L)
                    val duration = item.optLong("duration", 1L)
                    val progressPercent = if (duration > 0L) ((viewOffset.toFloat() / duration.toFloat()) * 100).toInt() else 0
                    
                    val relativeThumb = item.optString("thumb") ?: ""
                    val thumbUrl = if (relativeThumb.isNotEmpty()) {
                        var sanitizedUrl = baseUrl.trim()
                        if (!sanitizedUrl.endsWith("/")) sanitizedUrl = "$sanitizedUrl/"
                        val cleanThumb = relativeThumb.removePrefix("/")
                        "${sanitizedUrl}${cleanThumb}?X-Plex-Token=$token"
                    } else {
                        ""
                    }

                    val seasonNum = item.optString("parentIndex") ?: ""
                    val episodeNum = item.optString("index") ?: ""

                    sessionsList.add(
                        TautulliSession(
                            user = user,
                            username = user,
                            title = item.optString("title") ?: "Unknown",
                            type = item.optString("type") ?: "movie",
                            progressPercent = progressPercent,
                            state = state,
                            sessionId = sessionId,
                            ratingKey = item.optString("ratingKey") ?: "",
                            product = product,
                            player = player,
                            ipAddress = ipAddress,
                            transcodeDecision = videoDecision,
                            videoResolution = item.optString("videoResolution") ?: "1080p",
                            year = item.optString("year") ?: "",
                            grandparentTitle = item.optString("grandparentTitle") ?: "",
                            parentTitle = item.optString("parentTitle") ?: "",
                            season = seasonNum,
                            episode = episodeNum,
                            thumb = thumbUrl
                        )
                    )
                }
            }

            var totalBw = mc.optLong("totalBandwidth", 0L)
            var lanBw = mc.optLong("lanBandwidth", 0L)
            var wanBw = mc.optLong("wanBandwidth", 0L)

            if (totalBw == 0L && metadataArray != null) {
                for (i in 0 until metadataArray.length()) {
                    val item = metadataArray.getJSONObject(i)
                    totalBw += item.optLong("bandwidth", 0L)
                }
                lanBw = totalBw
            }
            
            return TautulliSessions(
                streamCount = sessionsList.size,
                sessions = sessionsList,
                totalBandwidth = totalBw.toString(),
                lanBandwidth = lanBw.toString(),
                wanBandwidth = wanBw.toString()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return TautulliSessions()
        }
    }

    private fun parsePlexHistory(
        bodyString: String,
        baseUrl: String = "",
        token: String = "",
        client: OkHttpClient? = null
    ): List<TautulliHistoryItem> {
        val historyList = mutableListOf<TautulliHistoryItem>()
        try {
            val root = JSONObject(bodyString)
            val mc = root.optJSONObject("MediaContainer") ?: return emptyList()
            val metadataArray = mc.optJSONArray("History") ?: mc.optJSONArray("Metadata")
                ?: return emptyList()

            // ── Phase 1: harvest embedded Account sub-objects and find unresolved IDs ──
            val unresolvedIds = mutableSetOf<String>()
            for (i in 0 until metadataArray.length()) {
                val item = metadataArray.getJSONObject(i)
                val accountId = item.optString("accountID").ifEmpty { item.optString("accountId") }
                if (accountId.isEmpty()) continue

                // Some PMS versions embed Account directly in each history item
                val subObj = item.optJSONObject("Account")
                val embeddedName = subObj?.optString("name")?.takeIf { it.isNotEmpty() }
                    ?: subObj?.optString("title")?.takeIf { it.isNotEmpty() }

                if (embeddedName != null) {
                    plexUserMap[accountId] = embeddedName
                } else if (!plexUserMap.containsKey(accountId) && accountId != "1") {
                    unresolvedIds.add(accountId)
                }
            }

            // ── Phase 2: resolve each unknown ID via GET /accounts/{id} on the server ──
            // This is the authoritative source — every accountID in history has a
            // corresponding account entry on the same server.
            if (client != null && baseUrl.isNotEmpty() && token.isNotEmpty()) {
                for (accountId in unresolvedIds) {
                    try {
                        val resp = client.newCall(
                            Request.Builder()
                                .url("${baseUrl}accounts/$accountId?X-Plex-Token=$token&accept=json")
                                .addHeader("Accept", "application/json")
                                .build()
                        ).execute()
                        val body = resp.body?.string() ?: ""
                        if (resp.isSuccessful && body.isNotEmpty()) {
                            val accMc = JSONObject(body).optJSONObject("MediaContainer")
                            val accArr = accMc?.optJSONArray("Account")
                            if (accArr != null && accArr.length() > 0) {
                                val acc = accArr.getJSONObject(0)
                                val name = acc.optString("name").ifEmpty { acc.optString("title") }
                                    .ifEmpty { acc.optString("username") }
                                if (name.isNotEmpty()) plexUserMap[accountId] = name
                            }
                        }
                    } catch (e: Exception) { /* ignore per-account fetch failure */ }
                }
            }

            // ── Phase 3: build the history list with fully resolved user names ──
            for (i in 0 until metadataArray.length()) {
                val item = metadataArray.getJSONObject(i)

                val accountId = item.optString("accountID").ifEmpty { item.optString("accountId") }
                val user = when {
                    accountId.isNotEmpty() ->
                        plexUserMap[accountId] ?: if (accountId == "1") "Owner" else "User $accountId"
                    else -> item.optString("user").ifEmpty { "Plex User" }
                }
                val device = item.optString("device").ifEmpty { "Plex Player" }
                val historyKey = item.optString("historyKey").ifEmpty { "hist_$i" }
                val parsedId = historyKey.substringAfterLast("/").toIntOrNull() ?: i
                val viewedAt = item.optLong("viewedAt", 0L)

                val rawDuration = item.optLong("duration", 0L)
                val duration = when {
                    rawDuration > 360000 -> rawDuration / 1000L
                    rawDuration > 0      -> rawDuration
                    else -> if (item.optString("type") == "movie") 5400L else 1800L
                }

                val relativeThumb = item.optString("thumb")
                val fullThumbUrl = if (relativeThumb.isNotEmpty() && baseUrl.isNotEmpty()) {
                    val cleanThumb = relativeThumb.removePrefix("/")
                    "${baseUrl}${cleanThumb}?X-Plex-Token=$token"
                } else relativeThumb

                val relativeGrandparentThumb = item.optString("grandparentThumb")
                val fullGrandparentThumbUrl = if (relativeGrandparentThumb.isNotEmpty() && baseUrl.isNotEmpty()) {
                    val cleanThumb = relativeGrandparentThumb.removePrefix("/")
                    "${baseUrl}${cleanThumb}?X-Plex-Token=$token"
                } else relativeGrandparentThumb

                val ratingKey = item.optString("ratingKey")

                historyList.add(
                    TautulliHistoryItem(
                        id = parsedId,
                        date = viewedAt,
                        duration = duration,
                        friendlyName = device,
                        user = user,
                        title = item.optString("title").ifEmpty { "Unknown" },
                        parentTitle = item.optString("parentTitle"),
                        grandparentTitle = item.optString("grandparentTitle"),
                        type = item.optString("type").ifEmpty { "movie" },
                        watchedStatus = 1,
                        player = device,
                        ipAddress = "",
                        thumb = fullThumbUrl,
                        grandparentThumb = fullGrandparentThumbUrl,
                        ratingKey = ratingKey
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return historyList
    }

    /**
     * Fallback when /status/sessions/history/all returns empty ("Track Play History" disabled).
     * Queries each library section for items that have been watched (lastViewedAt > 0),
     * sorted by most-recently-viewed first. One entry per item is synthesised — viewCount
     * is not expanded into repeated entries to keep the data honest.
     */
    private fun fetchWatchedItemsFromLibrary(
        client: OkHttpClient,
        sanitizedUrl: String,
        token: String
    ): List<TautulliHistoryItem> {
        val result = mutableListOf<TautulliHistoryItem>()
        try {
            val sectionsBody = client.newCall(
                Request.Builder()
                    .url("${sanitizedUrl}library/sections?X-Plex-Token=$token")
                    .addHeader("Accept", "application/json")
                    .build()
            ).execute().use { it.body?.string() } ?: return result

            val dirs = JSONObject(sectionsBody)
                .optJSONObject("MediaContainer")
                ?.optJSONArray("Directory") ?: return result

            for (i in 0 until dirs.length()) {
                val d = dirs.getJSONObject(i)
                val key = d.optString("key")
                val libType = d.optString("type") // "movie" or "show"
                if (key.isEmpty() || libType !in listOf("movie", "show")) continue

                // type=1 → movies, type=4 → episodes
                val plexType = if (libType == "movie") "1" else "4"
                val url = "${sanitizedUrl}library/sections/$key/all" +
                    "?type=$plexType&sort=lastViewedAt:desc" +
                    "&X-Plex-Container-Start=0&X-Plex-Container-Size=500" +
                    "&X-Plex-Token=$token"

                try {
                    val body = client.newCall(
                        Request.Builder().url(url).addHeader("Accept", "application/json").build()
                    ).execute().use { it.body?.string() } ?: continue

                    val items = JSONObject(body)
                        .optJSONObject("MediaContainer")
                        ?.optJSONArray("Metadata") ?: continue

                    for (j in 0 until items.length()) {
                        val item = items.getJSONObject(j)
                        val lastViewedAt = item.optLong("lastViewedAt", 0L)
                        if (lastViewedAt == 0L) continue // never watched — skip

                        // Plex reports duration in milliseconds; convert to seconds
                        val rawDuration = item.optLong("duration", 0L)
                        val duration = when {
                            rawDuration > 360_000L -> rawDuration / 1000L
                            rawDuration > 0L       -> rawDuration
                            else -> if (libType == "movie") 5400L else 1800L
                        }

                        val relativeThumb = item.optString("thumb")
                        val fullThumbUrl = if (relativeThumb.isNotEmpty()) {
                            val clean = relativeThumb.removePrefix("/")
                            "${sanitizedUrl}${clean}?X-Plex-Token=$token"
                        } else relativeThumb

                        val relativeGrandparentThumb = item.optString("grandparentThumb")
                        val fullGrandparentThumbUrl = if (relativeGrandparentThumb.isNotEmpty()) {
                            val clean = relativeGrandparentThumb.removePrefix("/")
                            "${sanitizedUrl}${clean}?X-Plex-Token=$token"
                        } else relativeGrandparentThumb

                        val ratingKey = item.optString("ratingKey")
                        val resolvedUser = plexUserMap["1"] ?: "Owner"

                        result.add(
                            TautulliHistoryItem(
                                id = ratingKey.toIntOrNull() ?: j,
                                date = lastViewedAt,
                                duration = duration,
                                friendlyName = "Plex",
                                user = resolvedUser,
                                title = item.optString("title").ifEmpty { "Unknown" },
                                parentTitle = item.optString("parentTitle"),
                                grandparentTitle = item.optString("grandparentTitle"),
                                type = if (libType == "movie") "movie" else "episode",
                                watchedStatus = 1,
                                player = "Plex",
                                ipAddress = "",
                                thumb = fullThumbUrl,
                                grandparentThumb = fullGrandparentThumbUrl,
                                ratingKey = ratingKey
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Skip this section on error, continue to next
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    // Call from UI to trigger test connection
    fun testConnection(tempUrl: String, tempKey: String) {
        val cleanKey = sanitizeToken(tempKey)
        viewModelScope.launch {
            _testState.value = TestConnectionState.Testing
            try {
                // Try Tautulli
                val service = TautulliApiClient.getService(tempUrl)
                try {
                    val testResponse = service.getServerFriendlyName(apiKey = cleanKey)
                    if (testResponse.response.result == "success") {
                        val serverName = testResponse.response.data?.serverName ?: "Tautulli Monitor"
                        _testState.value = TestConnectionState.Success("$serverName (Tautulli)")
                        return@launch
                    }
                } catch (e: Exception) {
                    // fall through to Plex test fallback
                }

                // Try Plex Media Server directly
                withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(8, TimeUnit.SECONDS)
                        .addInterceptor { chain ->
                            val req = chain.request().newBuilder()
                                .addHeader("X-Plex-Platform", "Android")
                                .addHeader("X-Plex-Client-Identifier", "toplex-dashboard-android")
                                .addHeader("X-Plex-Product", "Toplex Dashboard")
                                .addHeader("X-Plex-Version", "1.0.0")
                                .addHeader("X-Plex-Device", "Android Phone")
                                .addHeader("X-Plex-Provides", "controller")
                                .build()
                            chain.proceed(req)
                        }
                        .build()

                    var sanitizedUrl = tempUrl.trim()
                    if (!sanitizedUrl.startsWith("http://") && !sanitizedUrl.startsWith("https://")) {
                        sanitizedUrl = "http://$sanitizedUrl"
                    }
                    if (!sanitizedUrl.endsWith("/")) {
                        sanitizedUrl = "$sanitizedUrl/"
                    }

                    val requestUrl = "${sanitizedUrl}?X-Plex-Token=$cleanKey"
                    val request = Request.Builder()
                        .url(requestUrl)
                        .addHeader("Accept", "application/json")
                        .build()

                    val response = client.newCall(request).execute()
                    val bodyString = response.body?.string() ?: ""
                    if (response.isSuccessful && bodyString.isNotEmpty()) {
                        var friendlyName = ""
                        if (bodyString.startsWith("{") || bodyString.contains("MediaContainer")) {
                            try {
                                val json = JSONObject(bodyString)
                                val mc = json.optJSONObject("MediaContainer")
                                friendlyName = mc?.optString("friendlyName") ?: ""
                            } catch (je: Exception) {
                                // fall to regex
                            }
                        }
                        if (friendlyName.isEmpty()) {
                            val match = "friendlyName=\"([^\"]+)\"".toRegex().find(bodyString)
                            friendlyName = match?.groupValues?.get(1) ?: "Plex Server"
                        }
                        _testState.value = TestConnectionState.Success("$friendlyName (Plex Media Server)")
                    } else {
                        _testState.value = TestConnectionState.Error("Server returned code ${response.code}. Check credentials.")
                    }
                }
            } catch (e: Exception) {
                _testState.value = TestConnectionState.Error("Connection failed: ${e.localizedMessage ?: "Check address and network."}")
            }
        }
    }

    // Clear connection test feedback
    fun clearTestState() {
        _testState.value = TestConnectionState.Idle
    }

    // Call from UI to save new credentials and connect
    fun saveCredentials(url: String, key: String) {
        viewModelScope.launch {
            val cleanUrl = url.trim()
            val cleanKey = sanitizeToken(key)
            _serverUrl.value = cleanUrl
            _apiKey.value = cleanKey
            _useDemoMode.value = false

            // Auto-detect server type
            val calculatedType = withContext(Dispatchers.IO) {
                try {
                    val service = TautulliApiClient.getService(cleanUrl)
                    val testResponse = service.getServerFriendlyName(apiKey = cleanKey)
                    if (testResponse.response.result == "success") {
                        "tautulli"
                    } else {
                        "plex"
                    }
                } catch (e: Exception) {
                    "plex"
                }
            }

            _serverType.value = calculatedType
            prefs.saveServerCredentials(cleanUrl, cleanKey, calculatedType)
            prefs.setDemoMode(false)

            startPollingRealServer()
        }
    }

    // Toggles between Real and Demo mode
    fun toggleDemoMode(enable: Boolean) {
        viewModelScope.launch {
            _useDemoMode.value = enable
            prefs.setDemoMode(enable)
            if (enable) {
                loadSimulatedData()
            } else {
                startPollingRealServer()
            }
        }
    }

    fun forceRefresh() {
        viewModelScope.launch {
            if (_useDemoMode.value) {
                loadSimulatedData()
            } else {
                _activityState.value = UiState.Loading
                _historyState.value = UiState.Loading
                fetchRealData()
            }
        }
    }

    fun clearMovieDetail() {
        _movieDetailState.value = null
    }

    fun fetchMovieDetail(ratingKey: String) {
        if (ratingKey.isEmpty()) return
        viewModelScope.launch {
            _movieDetailState.value = UiState.Loading

            if (_useDemoMode.value) {
                // Build demo detail from existing history data
                val history = (_historyState.value as? UiState.Success)?.data ?: emptyList()
                val item = history.firstOrNull { it.ratingKey == ratingKey }
                _movieDetailState.value = UiState.Success(buildDemoMovieDetail(ratingKey, item?.title ?: "", item?.thumb ?: ""))
                return@launch
            }

            val url   = _serverUrl.value
            val api   = _apiKey.value
            val type  = _serverType.value
            if (url.isEmpty() || api.isEmpty()) {
                _movieDetailState.value = UiState.Error("Server credentials missing.")
                return@launch
            }

            try {
                if (type == "tautulli") {
                    val service  = TautulliApiClient.getService(url)
                    val response = service.getMetadata(apiKey = api, ratingKey = ratingKey)
                    if (response.response.result == "success") {
                        val d = response.response.data
                        if (d != null) {
                            var cleanUrl = url.trim()
                            if (!cleanUrl.endsWith("/")) cleanUrl = "$cleanUrl/"
                            val thumbUrl = if (!d.thumb.isNullOrEmpty()) {
                                val relative = d.thumb.removePrefix("/")
                                "${cleanUrl}api/v2?apikey=${api}&cmd=get_thumbnail&thumb=${relative}"
                            } else ""
                            _movieDetailState.value = UiState.Success(
                                PlexMovieDetail(
                                    ratingKey  = ratingKey,
                                    title      = d.title ?: "Unknown",
                                    year       = d.year?.toString(),
                                    summary    = d.summary,
                                    genres     = d.genres?.mapNotNull { it.tag?.takeIf(String::isNotEmpty) } ?: emptyList(),
                                    directors  = d.directors?.mapNotNull { it.tag?.takeIf(String::isNotEmpty) } ?: emptyList(),
                                    actors     = d.actors?.mapNotNull { it.tag?.takeIf(String::isNotEmpty) } ?: emptyList(),
                                    duration   = formatMetadataDuration(d.duration),
                                    rating     = d.rating?.toFloat(),
                                    contentRating = d.contentRating,
                                    thumb      = thumbUrl
                                )
                            )
                        } else {
                            _movieDetailState.value = UiState.Error("No metadata returned for this item.")
                        }
                    } else {
                        _movieDetailState.value = UiState.Error(response.response.message ?: "Metadata fetch failed.")
                    }
                } else {
                    // Plex direct mode — call /library/metadata/{ratingKey}
                    withContext(Dispatchers.IO) {
                        var sanitizedUrl = url.trim()
                        if (!sanitizedUrl.startsWith("http://") && !sanitizedUrl.startsWith("https://")) {
                            sanitizedUrl = "http://$sanitizedUrl"
                        }
                        if (!sanitizedUrl.endsWith("/")) sanitizedUrl = "$sanitizedUrl/"
                        val token  = sanitizeToken(api)
                        val client = OkHttpClient.Builder()
                            .connectTimeout(5, TimeUnit.SECONDS)
                            .readTimeout(8, TimeUnit.SECONDS)
                            .addInterceptor { chain ->
                                chain.proceed(
                                    chain.request().newBuilder()
                                        .addHeader("X-Plex-Platform", "Android")
                                        .addHeader("X-Plex-Client-Identifier", "toplex-dashboard-android")
                                        .addHeader("X-Plex-Token", token)
                                        .build()
                                )
                            }
                            .build()
                        val request = Request.Builder()
                            .url("${sanitizedUrl}library/metadata/$ratingKey?X-Plex-Token=$token")
                            .addHeader("Accept", "application/json")
                            .build()
                        val response = client.newCall(request).execute()
                        val body = response.body?.string() ?: ""
                        if (response.isSuccessful && body.isNotEmpty()) {
                            val detail = parsePlexMetadataResponse(body, sanitizedUrl, token)
                            _movieDetailState.value = if (detail != null) {
                                UiState.Success(detail)
                            } else {
                                UiState.Error("Could not parse movie metadata.")
                            }
                        } else {
                            _movieDetailState.value = UiState.Error("Plex returned HTTP ${response.code}")
                        }
                    }
                }
            } catch (e: Exception) {
                _movieDetailState.value = UiState.Error("Detail fetch error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    /**
     * Fetches library-level stats from Plex API:
     * - Counts movies/shows added this calendar year via /library/sections/{key}/all sorted by addedAt desc
     * Must be called from within a withContext(Dispatchers.IO) block.
     */
    private fun formatMetadataDuration(durationMs: Long?): String? {
        if (durationMs == null || durationMs <= 0L) return null
        val totalSecs = durationMs / 1000L
        val h = totalSecs / 3600L
        val m = (totalSecs % 3600L) / 60L
        return if (h > 0L) "${h}h ${m}m" else "${m}m"
    }

    private fun parsePlexMetadataResponse(bodyString: String, baseUrl: String, token: String): PlexMovieDetail? {
        return try {
            val root  = JSONObject(bodyString)
            val mc    = root.optJSONObject("MediaContainer") ?: return null
            val arr   = mc.optJSONArray("Metadata") ?: return null
            if (arr.length() == 0) return null
            val item  = arr.getJSONObject(0)
            val ratingKey = item.optString("ratingKey")
            val title     = item.optString("title").ifEmpty { "Unknown" }
            val year      = item.optInt("year", 0).let { if (it > 0) it.toString() else null }
            val summary   = item.optString("summary").ifEmpty { null }
            val durationMs = item.optLong("duration", 0L)
            val rating    = item.optDouble("rating", 0.0).let { if (it > 0.0) it.toFloat() else null }
            val contentRating = item.optString("contentRating").ifEmpty { null }
            val relThumb  = item.optString("thumb")
            val thumbUrl  = if (relThumb.isNotEmpty()) {
                "${baseUrl}${relThumb.removePrefix("/")}?X-Plex-Token=$token"
            } else ""
            fun jsonTags(key: String): List<String> {
                val a = item.optJSONArray(key) ?: return emptyList()
                return (0 until a.length()).mapNotNull { a.getJSONObject(it).optString("tag").ifEmpty { null } }
            }
            PlexMovieDetail(
                ratingKey     = ratingKey,
                title         = title,
                year          = year,
                summary       = summary,
                genres        = jsonTags("Genre"),
                directors     = jsonTags("Director"),
                actors        = jsonTags("Role"),
                duration      = formatMetadataDuration(durationMs),
                rating        = rating,
                contentRating = contentRating,
                thumb         = thumbUrl
            )
        } catch (e: Exception) { null }
    }

    private fun buildDemoMovieDetail(ratingKey: String, title: String, thumb: String): PlexMovieDetail {
        return when {
            title.contains("Oppenheimer", ignoreCase = true) -> PlexMovieDetail(
                ratingKey, title, "2023",
                "The story of J. Robert Oppenheimer and the development of the atomic bomb during World War II.",
                listOf("Drama", "History", "Thriller"), listOf("Christopher Nolan"),
                listOf("Cillian Murphy", "Emily Blunt", "Matt Damon", "Robert Downey Jr."),
                "3h 0m", 8.3f, "R", thumb
            )
            title.contains("Dune", ignoreCase = true) -> PlexMovieDetail(
                ratingKey, title, "2024",
                "Paul Atreides unites with Chani and the Fremen to seek revenge against those who destroyed his family.",
                listOf("Sci-Fi", "Adventure", "Drama"), listOf("Denis Villeneuve"),
                listOf("Timothée Chalamet", "Zendaya", "Rebecca Ferguson", "Josh Brolin"),
                "2h 46m", 8.5f, "PG-13", thumb
            )
            title.contains("Interstellar", ignoreCase = true) -> PlexMovieDetail(
                ratingKey, title, "2014",
                "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.",
                listOf("Sci-Fi", "Drama", "Adventure"), listOf("Christopher Nolan"),
                listOf("Matthew McConaughey", "Anne Hathaway", "Jessica Chastain"),
                "2h 49m", 8.7f, "PG-13", thumb
            )
            title.contains("Everything Everywhere", ignoreCase = true) -> PlexMovieDetail(
                ratingKey, title, "2022",
                "An aging Chinese immigrant is swept up in an insane adventure in which she alone can save existence.",
                listOf("Sci-Fi", "Comedy", "Action"), listOf("Daniels"),
                listOf("Michelle Yeoh", "Stephanie Hsu", "Jamie Lee Curtis", "Ke Huy Quan"),
                "2h 19m", 7.8f, "R", thumb
            )
            title.contains("Shawshank", ignoreCase = true) -> PlexMovieDetail(
                ratingKey, title, "1994",
                "Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.",
                listOf("Drama"), listOf("Frank Darabont"),
                listOf("Tim Robbins", "Morgan Freeman", "Bob Gunton"),
                "2h 22m", 9.3f, "R", thumb
            )
            else -> PlexMovieDetail(
                ratingKey, title.ifEmpty { "Unknown Movie" }, "2024",
                "An exciting cinematic experience available on your Plex server. Connect to a real server to view full details.",
                listOf("Drama"), listOf("Director"),
                listOf("Lead Actor", "Supporting Actor"),
                "2h 0m", 7.5f, "PG-13", thumb
            )
        }
    }

    private fun fetchPlexLibraryStats(client: OkHttpClient, sanitizedUrl: String, token: String) {
        try {
            // Step 1: Discover library sections
            val sectionsBody = client.newCall(
                Request.Builder()
                    .url("${sanitizedUrl}library/sections?X-Plex-Token=$token")
                    .addHeader("Accept", "application/json")
                    .build()
            ).execute().use { it.body?.string() } ?: return

            val dirs = JSONObject(sectionsBody)
                .optJSONObject("MediaContainer")
                ?.optJSONArray("Directory") ?: return

            // Collect ALL section keys per type — the server may have multiple movie or
            // TV libraries (e.g. "Movies" + "Movies - 4K"); the web iterates all of them.
            val movieKeys = mutableListOf<String>()
            val tvKeys    = mutableListOf<String>()
            for (i in 0 until dirs.length()) {
                val d = dirs.getJSONObject(i)
                val key = d.optString("key")
                if (key.isEmpty()) continue
                when (d.optString("type")) {
                    "movie" -> movieKeys.add(key)
                    "show"  -> tvKeys.add(key)
                }
            }

            // Start of current year as Unix epoch (seconds)
            val startOfYear = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.MONTH, 0)
                set(java.util.Calendar.DAY_OF_MONTH, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis / 1000L

            // Step 2: Count items added this year — sum across ALL sections of each type
            fun countAddedThisYear(sectionKey: String, plexType: String): Int {
                if (sectionKey.isEmpty()) return 0
                return try {
                    val url = "${sanitizedUrl}library/sections/$sectionKey/all" +
                        "?type=$plexType&sort=addedAt:desc" +
                        "&X-Plex-Container-Start=0&X-Plex-Container-Size=500" +
                        "&X-Plex-Token=$token"
                    val body = client.newCall(
                        Request.Builder().url(url).addHeader("Accept", "application/json").build()
                    ).execute().use { it.body?.string() } ?: return 0

                    val items = JSONObject(body)
                        .optJSONObject("MediaContainer")
                        ?.optJSONArray("Metadata") ?: return 0

                    var count = 0
                    for (i in 0 until items.length()) {
                        // Items are sorted newest-first; stop once we go before this year
                        val addedAt = items.getJSONObject(i).optLong("addedAt", 0L)
                        if (addedAt >= startOfYear) count++ else break
                    }
                    count
                } catch (e: Exception) { 0 }
            }

            val moviesAdded = movieKeys.sumOf { countAddedThisYear(it, "1") }
            val showsAdded  = tvKeys.sumOf    { countAddedThisYear(it, "2") }

            _plexLibraryStats.value = PlexLibraryStats(
                moviesAddedThisYear = moviesAdded,
                showsAddedThisYear  = showsAdded
            )
        } catch (e: Exception) {
            // Library stats are non-critical — swallow silently
        }
    }
}
