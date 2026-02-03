package dev.abu.material3.innertube.pages

import dev.abu.material3.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
