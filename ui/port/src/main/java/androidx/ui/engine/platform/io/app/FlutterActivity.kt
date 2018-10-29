package androidx.ui.engine.platform.io.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.ui.engine.platform.io.plugin.common.PluginRegistry
import androidx.ui.engine.platform.io.view.FlutterNativeView
import androidx.ui.engine.platform.io.view.FlutterView

open class FlutterActivity
    : ComponentActivity(), FlutterView.Provider, PluginRegistry,
    FlutterActivityDelegate.ViewFactory {

    private val delegate = FlutterActivityDelegate(this, this, lifecycle)

    // These aliases ensure that the methods we forward to the delegate adhere
    // to relevant interfaces versus just existing in FlutterActivityDelegate.
    private val eventDelegate: FlutterActivityEvents = TODO() // delegate
    private val viewProvider: FlutterView.Provider = TODO() // delegate
    private val pluginRegistry: PluginRegistry = TODO() // delegate

    /**
     * Returns the Flutter view used by this activity; will be null before
     * [.onCreate] is called.
     */
    override val flutterView get() = viewProvider.flutterView

    /**
     * Hook for subclasses to customize the creation of the
     * `FlutterView`.
     *
     *
     * The default implementation returns `null`, which will cause the
     * activity to use a newly instantiated full-screen view.
     */
    override fun createFlutterView(context: Context): FlutterView? {
        return null
    }

    /**
     * Hook for subclasses to customize the creation of the
     * `FlutterNativeView`.
     *
     *
     * The default implementation returns `null`, which will cause the
     * activity to use a newly instantiated native view object.
     */
    override fun createFlutterNativeView(): FlutterNativeView? {
        return null
    }

    override fun retainFlutterNativeView(): Boolean {
        return false
    }

    override fun hasPlugin(pluginKey: String): Boolean {
        return pluginRegistry.hasPlugin(pluginKey)
    }

    override fun <T> valuePublishedByPlugin(pluginKey: String): T {
        return pluginRegistry.valuePublishedByPlugin(pluginKey)
    }

    override fun registrarFor(pluginKey: String): PluginRegistry.Registrar {
        return pluginRegistry.registrarFor(pluginKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventDelegate.onCreate(savedInstanceState)
    }

    override fun onBackPressed() {
        if (!eventDelegate.onBackPressed()) {
            super.onBackPressed()
        }
    }

    // @Override - added in API level 23
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        eventDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (!eventDelegate.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNewIntent(intent: Intent) {
        eventDelegate.onNewIntent(intent)
    }

    public override fun onUserLeaveHint() {
        eventDelegate.onUserLeaveHint()
    }

    override fun onTrimMemory(level: Int) {
        eventDelegate.onTrimMemory(level)
    }

    override fun onLowMemory() {
        eventDelegate.onLowMemory()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        eventDelegate.onConfigurationChanged(newConfig)
    }
}