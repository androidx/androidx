package androidx.ui.engine.platform.io.app

import android.content.Intent
import android.os.Bundle
import android.content.ComponentCallbacks2
import androidx.ui.engine.platform.io.plugin.common.PluginRegistry

interface FlutterActivityEvents
    : ComponentCallbacks2,
        PluginRegistry.ActivityResultListener,
        PluginRegistry.RequestPermissionsResultListener {
    /**
     * @see android.app.Activity.onCreate
     */
    fun onCreate(savedInstanceState: Bundle?)

    /**
     * @see android.app.Activity.onNewIntent
     */
    fun onNewIntent(intent: Intent)

    /**
     * Invoked when the activity has detected the user's press of the back key.
     *
     * @return `true` if the listener handled the event; `false`
     * to let the activity continue with its default back button handling.
     * @see android.app.Activity.onBackPressed
     */
    fun onBackPressed(): Boolean

    /**
     * @see android.app.Activity.onUserLeaveHint
     */
    fun onUserLeaveHint()
}