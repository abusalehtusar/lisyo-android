package dev.abu.material3.innertube.pages

import dev.abu.material3.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)