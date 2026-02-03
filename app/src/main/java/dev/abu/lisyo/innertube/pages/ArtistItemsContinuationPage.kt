package dev.abu.lisyo.innertube.pages

import dev.abu.lisyo.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
