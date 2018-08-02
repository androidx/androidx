package androidx.ui.engine.platform.io.platform

import android.view.View

/**
 * A handle to an Android view to be embedded in the Flutter hierarchy.
 */
interface PlatformView {
    /**
     * Returns the Android view to be embedded in the Flutter hierarchy.
     */
    val view: View

    /**
     * Dispose this platform view.
     *
     *
     * The [PlatformView] object is unusable after this method is called.
     *
     *
     * Plugins implementing [PlatformView] must clear all references to the View object and the PlatformView
     * after this method is called. Failing to do so will result in a memory leak.
     */
    fun dispose()
}