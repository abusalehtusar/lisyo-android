package dev.abu.material3.innertube.pages

import dev.abu.material3.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
