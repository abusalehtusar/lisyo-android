package dev.abu.lisyo.innertube.pages

import dev.abu.lisyo.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
