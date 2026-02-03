package dev.abu.lisyo.innertube.models.body

import dev.abu.lisyo.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)
