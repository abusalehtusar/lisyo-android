package dev.abu.lisyo.innertube.models.body

import dev.abu.lisyo.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)
