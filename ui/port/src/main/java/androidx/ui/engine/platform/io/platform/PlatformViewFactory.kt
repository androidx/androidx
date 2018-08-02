package androidx.ui.engine.platform.io.platform

import android.content.Context

interface PlatformViewFactory {
    /**
     * Creates a new Android view to be embedded in the Flutter hierarchy.
     *
     * @param context the context to be used when creating the view, this is different than FlutterView's context.
     * @param viewId unique identifier for the created instance, this value is known on the Dart side.
     */
    fun create(context: Context, viewId: Int): PlatformView
}