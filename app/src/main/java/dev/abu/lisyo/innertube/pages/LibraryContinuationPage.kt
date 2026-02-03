package dev.abu.lisyo.innertube.pages

import dev.abu.lisyo.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)