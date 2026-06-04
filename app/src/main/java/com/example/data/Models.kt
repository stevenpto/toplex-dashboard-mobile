package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TautulliResponse<T>(
    @Json(name = "response") val response: ResponsePayload<T>
)

@JsonClass(generateAdapter = true)
data class ResponsePayload<T>(
    @Json(name = "result") val result: String,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: T?
)

@JsonClass(generateAdapter = true)
data class TautulliSessions(
    @Json(name = "stream_count") val streamCount: Int? = 0,
    @Json(name = "sessions") val sessions: List<TautulliSession>? = emptyList(),
    @Json(name = "total_bandwidth") val totalBandwidth: String? = "0",
    @Json(name = "lan_bandwidth") val lanBandwidth: String? = "0",
    @Json(name = "wan_bandwidth") val wanBandwidth: String? = "0"
)

@JsonClass(generateAdapter = true)
data class TautulliSession(
    @Json(name = "user") val user: String? = "",
    @Json(name = "username") val username: String? = "",
    @Json(name = "title") val title: String? = "",
    @Json(name = "type") val type: String? = "movie", // "movie", "episode", "track"
    @Json(name = "progress_percent") val progressPercent: Int? = 0,
    @Json(name = "state") val state: String? = "playing", // "playing", "paused"
    @Json(name = "session_id") val sessionId: String? = "",
    @Json(name = "rating_key") val ratingKey: String? = "",
    @Json(name = "product") val product: String? = "",
    @Json(name = "player") val player: String? = "",
    @Json(name = "ip_address") val ipAddress: String? = "",
    @Json(name = "transcode_decision") val transcodeDecision: String? = "direct play",
    @Json(name = "video_resolution") val videoResolution: String? = "1080p",
    @Json(name = "year") val year: String? = "",
    @Json(name = "grandparent_title") val grandparentTitle: String? = "",
    @Json(name = "parent_title") val parentTitle: String? = "",
    @Json(name = "season") val season: String? = "",
    @Json(name = "episode") val episode: String? = "",
    @Json(name = "thumb") val thumb: String? = "",
    @Json(name = "view_offset") val viewOffset: Long? = 0L,
    @Json(name = "duration") val duration: Long? = 0L
)

@JsonClass(generateAdapter = true)
data class TautulliHistoryContainer(
    @Json(name = "data") val data: List<TautulliHistoryItem>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class TautulliHistoryItem(
    @Json(name = "id") val id: Int? = 0,
    @Json(name = "date") val date: Long? = 0L,
    @Json(name = "duration") val duration: Long? = 0L,
    @Json(name = "friendly_name") val friendlyName: String? = "",
    @Json(name = "user") val user: String? = "",
    @Json(name = "title") val title: String? = "",
    @Json(name = "parent_title") val parentTitle: String? = "",
    @Json(name = "grandparent_title") val grandparentTitle: String? = "",
    @Json(name = "type") val type: String? = "movie",
    @Json(name = "watched_status") val watchedStatus: Int? = 0,
    @Json(name = "player") val player: String? = "",
    @Json(name = "ip_address") val ipAddress: String? = "",
    // Plex-specific fields for poster image construction
    @Json(name = "thumb") val thumb: String? = "",
    @Json(name = "grandparent_thumb") val grandparentThumb: String? = "",
    @Json(name = "rating_key") val ratingKey: String? = ""
)

// Library-level statistics fetched from Plex /library/sections API
// Not deserialized from JSON — populated directly by ViewModel
data class PlexLibraryStats(
    val moviesAddedThisYear: Int = 0,
    val showsAddedThisYear: Int = 0
)

@JsonClass(generateAdapter = true)
data class TautulliHomeStatRow(
    @Json(name = "title") val title: String? = "",
    @Json(name = "plays") val plays: Int? = 0,
    @Json(name = "user") val user: String? = "",
    @Json(name = "duration") val duration: Long? = 0L,
    @Json(name = "rating_key") val ratingKey: String? = "",
    @Json(name = "thumb") val thumb: String? = ""
)

// Simplify server verification model
@JsonClass(generateAdapter = true)
data class TautulliServerInfo(
    @Json(name = "server_name") val serverName: String? = ""
)

// Generic tag object used inside metadata arrays (genres, directors, actors)
@JsonClass(generateAdapter = true)
data class TautulliTag(
    @Json(name = "tag") val tag: String? = ""
)

// Full movie metadata returned by Tautulli get_metadata command
@JsonClass(generateAdapter = true)
data class TautulliMetadataItem(
    @Json(name = "rating_key") val ratingKey: String? = "",
    @Json(name = "title") val title: String? = "",
    @Json(name = "year") val year: Int? = null,
    @Json(name = "summary") val summary: String? = "",
    @Json(name = "duration") val duration: Long? = null,   // milliseconds
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "content_rating") val contentRating: String? = "",
    @Json(name = "thumb") val thumb: String? = "",
    @Json(name = "genres") val genres: List<TautulliTag>? = emptyList(),
    @Json(name = "directors") val directors: List<TautulliTag>? = emptyList(),
    @Json(name = "actors") val actors: List<TautulliTag>? = emptyList()
)

// Resolved movie detail shown in the detail dialog
data class PlexMovieDetail(
    val ratingKey: String,
    val title: String,
    val year: String?,
    val summary: String?,
    val genres: List<String>,
    val directors: List<String>,
    val actors: List<String>,
    val duration: String?,
    val rating: Float?,
    val contentRating: String?,
    val thumb: String?
)
