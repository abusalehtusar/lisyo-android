package dev.abu.lisyo.innertube.models.body

import dev.abu.lisyo.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SubscribeBody(
    val channelIds: List<String>,
    val context: Context,
)
