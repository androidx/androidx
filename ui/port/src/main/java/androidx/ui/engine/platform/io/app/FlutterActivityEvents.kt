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
     * @see android.app.Activity.onPause
     */
    fun onPause()

    /**
     * @see android.app.Activity.onStart
     */
    fun onStart()

    /**
     * @see android.app.Activity.onResume
     */
    fun onResume()

    /**
     * @see android.app.Activity.onPostResume
     */
    fun onPostResume()

    /**
     * @see android.app.Activity.onDestroy
     */
    fun onDestroy()

    /**
     * @see android.app.Activity.onStop
     */
    fun onStop()

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