package com.glassgrab.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface GlassGrabRoute : NavKey {
    @Serializable
    data object Home : GlassGrabRoute

    @Serializable
    data object Downloads : GlassGrabRoute

    @Serializable
    data object Library : GlassGrabRoute

    @Serializable
    data object Settings : GlassGrabRoute

    @Serializable
    data class Player(val mediaUri: String, val title: String) : GlassGrabRoute

    @Serializable
    data object DesignGallery : GlassGrabRoute
}
