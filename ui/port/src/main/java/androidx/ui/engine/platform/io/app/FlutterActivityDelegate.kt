package androidx.ui.engine.platform.io.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources.NotFoundException
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.lifecycle.GenericLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.ui.engine.platform.io.view.FlutterNativeView
import androidx.ui.engine.platform.io.view.FlutterView
import java.util.ArrayList

/**
 * Class that performs the actual work of tying Android {@link Activity}
 * instances to Flutter.
 *
 * <p>This exists as a dedicated class (as opposed to being integrated directly
 * into {@link FlutterActivity}) to facilitate applications that don't wish
 * to subclass {@code FlutterActivity}. The most obvious example of when this
 * may come in handy is if an application wishes to subclass the Android v4
 * support library's {@code FragmentActivity}.</p>
 *
 * <h3>Usage:</h3>
 * <p>To wire this class up to your activity, simply forward the events defined
 * in {@link FlutterActivityEvents} from your activity to an instance of this
 * class. Optionally, you can make your activity implement
 * {@link PluginRegistry} and/or {@link io.flutter.view.FlutterView.Provider}
 * and forward those methods to this class as well.</p>
 */
class FlutterActivityDelegate(
    val activity: Activity,
    val viewFactory: ViewFactory,
    lifecycle: Lifecycle
) {
    // TODO: : FlutterActivityEvents, FlutterView.Provider, PluginRegistry

    companion object {
        private val SPLASH_SCREEN_META_DATA_KEY: String =
                "io.flutter.app.android.SplashScreenUntilFirstFrame"
        private val TAG: String = "FlutterActivityDelegate"
        private val matchParent: LayoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    /**
     * Specifies the mechanism by which Flutter views are created during the
     * operation of a {@code FlutterActivityDelegate}.
     *
     * <p>A delegate's view factory will be consulted during
     * {@link #onCreate(Bundle)}. If it returns {@code null}, then the delegate
     * will fall back to instantiating a new full-screen {@code FlutterView}.</p>
     *
     * <p>A delegate's native view factory will be consulted during
     * {@link #onCreate(Bundle)}. If it returns {@code null}, then the delegate
     * will fall back to instantiating a new {@code FlutterNativeView}. This is
     * useful for applications to override to reuse the FlutterNativeView held
     * e.g. by a pre-existing background service.</p>
     */
    interface ViewFactory {
        fun createFlutterView(context: Context): FlutterView?

        fun createFlutterNativeView(): FlutterNativeView?

        /**
         * Hook for subclasses to indicate that the {@code FlutterNativeView}
         * returned by {@link #createFlutterNativeView()} should not be destroyed
         * when this activity is destroyed.
         */
        fun retainFlutterNativeView(): Boolean
    }

    init {
        // Migration/Andrey: We added it instead of the manual delegation of all this
        // lifecycle callbacks from an Activity. It would help us to not be tied
        // to FlutterActivity in the future.
        lifecycle.addObserver(GenericLifecycleObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> onCreate()
                Lifecycle.Event.ON_PAUSE -> onPause()
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_DESTROY -> onDestroy()
                else -> { }
            }
        })
    }

//    // TODO(Migration/Filip): Is this ok?
//    override val flutterView: FlutterView = FlutterView()
//
//    // The implementation of PluginRegistry forwards to flutterView.
//    override fun hasPlugin(key: String): Boolean {
//        return flutterView.getPluginRegistry().hasPlugin(key)
//    }
//
//    override fun <T> valuePublishedByPlugin(pluginKey: String): T {
//        return flutterView.getPluginRegistry().valuePublishedByPlugin(pluginKey)
//    }
//
//    override fun registrarFor(pluginKey: String): PluginRegistry.Registrar {
//        return flutterView.getPluginRegistry().registrarFor(pluginKey)
//    }
//
//    override fun onRequestPermissionsResult(
//            requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
//        return flutterView.getPluginRegistry().onRequestPermissionsResult(requestCode, permissions, grantResults)
//    }

    /*
     * Method onRequestPermissionResult(int, String[], int[]) was made
     * unavailable on 2018-02-28, following deprecation. This comment is left as
     * a temporary tombstone for reference, to be removed on 2018-03-28 (or at
     * least four weeks after release of unavailability).
     *
     * https://github.com/flutter/flutter/wiki/Changelog#typo-fixed-in-flutter-engine-android-api
     */

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean {
//        return flutterView.getPluginRegistry().onActivityResult(requestCode, resultCode, data)
//    }

    private fun onCreate() {
        TODO()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            val window = activity.getWindow()
//            window.addFlags(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//            window.setStatusBarColor(0x40000000)
//            window.getDecorView().setSystemUiVisibility(PlatformPlugin.DEFAULT_SYSTEM_UI)
//        }
//
//        val args = getArgsFromIntent(activity.getIntent())
//        FlutterMain.ensureInitializationComplete(activity.getApplicationContext(), args)
//
//        flutterView = viewFactory.createFlutterView(activity)
//        if (flutterView == null) {
//            FlutterNativeView nativeView = viewFactory.createFlutterNativeView()
//            flutterView = new FlutterView(activity, null, nativeView)
//            flutterView.setLayoutParams(matchParent)
//            activity.setContentView(flutterView)
//            launchView = createLaunchView()
//            if (launchView != null) {
//                addLaunchView()
//            }
//        }
//
//        // When an activity is created for the first time, we direct the
//        // FlutterView to re-use a pre-existing Isolate rather than create a new
//        // one. This is so that an Isolate coming in from the ViewFactory is
//        // used.
//        val reuseIsolate = true
//
//        if (loadIntent(activity.getIntent(), reuseIsolate)) {
//            return
//        }
//        if (!flutterView.getFlutterNativeView().isApplicationRunning()) {
//          val appBundlePath = FlutterMain.findAppBundlePath(activity.getApplicationContext())
//          if (appBundlePath != null) {
//            flutterView.runFromBundle(appBundlePath, null, "main", reuseIsolate)
//          }
//        }
    }

//    override fun onNewIntent(intent: Intent) {
//        // Only attempt to reload the Flutter Dart code during development. Use
//        // the debuggable flag as an indicator that we are in development mode.
//        if (!isDebuggable() || !loadIntent(intent)) {
//            flutterView.getPluginRegistry().onNewIntent(intent)
//        }
//    }

    private fun isDebuggable(): Boolean {
        return activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun onPause() {
        TODO()
//        val app = activity.applicationContext as Application
//        if (app is FlutterApplication) {
//            val flutterApp = app as FlutterApplication
//            if (activity.equals(flutterApp.getCurrentActivity())) {
//                flutterApp.setCurrentActivity(null)
//            }
//        }
    }

    private fun onResume() {
        TODO()
//        val app = activity.applicationContext as Application
//        if (app is FlutterApplication) {
//            val flutterApp = app as FlutterApplication
//            flutterApp.setCurrentActivity(activity)
//        }
    }

    private fun onDestroy() {
        TODO()
//        val app = activity.applicationContext as Application
//        if (app is FlutterApplication) {
//            val flutterApp = app as FlutterApplication
//            if (activity.equals(flutterApp.getCurrentActivity())) {
//                flutterApp.setCurrentActivity(null)
//            }
//        }
//        if (flutterView != null) {
//            val detach = flutterView.getPluginRegistry().onViewDestroy(flutterView.getFlutterNativeView())
//            if (detach || viewFactory.retainFlutterNativeView()) {
//                // Detach, but do not destroy the FlutterView if a plugin
//                // expressed interest in its FlutterNativeView.
//                flutterView.detach()
//            } else {
//                flutterView.destroy()
//            }
//        }
    }

//    override fun onBackPressed(): Boolean {
//        if (flutterView != null) {
//            flutterView.popRoute()
//            return true
//        }
//        return false
//    }
//
//    override fun onUserLeaveHint() {
//        flutterView.getPluginRegistry().onUserLeaveHint()
//    }
//
//
//    override fun onTrimMemory(level: Int) {
//        // Use a trim level delivered while the application is running so the
//        // framework has a chance to react to the notification.
//        if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
//            flutterView.onMemoryPressure()
//        }
//    }

//    override fun onLowMemory() {
//        flutterView.onMemoryPressure()
//    }

//    override fun onConfigurationChanged(newConfig: Configuration) {}

    private fun getArgsFromIntent(intent: Intent): Array<String>? {
        // Before adding more entries to this list, consider that arbitrary
        // Android applications can generate intents with extra data and that
        // there are many security-sensitive args in the binary.
        val args = ArrayList<String>()
        if (intent.getBooleanExtra("trace-startup", false)) {
            args.add("--trace-startup")
        }
        if (intent.getBooleanExtra("start-paused", false)) {
            args.add("--start-paused")
        }
        if (intent.getBooleanExtra("use-test-fonts", false)) {
            args.add("--use-test-fonts")
        }
        if (intent.getBooleanExtra("enable-dart-profiling", false)) {
            args.add("--enable-dart-profiling")
        }
        if (intent.getBooleanExtra("enable-software-rendering", false)) {
            args.add("--enable-software-rendering")
        }
        if (intent.getBooleanExtra("skia-deterministic-rendering", false)) {
            args.add("--skia-deterministic-rendering")
        }
        if (intent.getBooleanExtra("trace-skia", false)) {
            args.add("--trace-skia")
        }
        if (intent.getBooleanExtra("verbose-logging", false)) {
            args.add("--verbose-logging")
        }
        if (!args.isEmpty()) {
            return args.toTypedArray()
        }
        return null
    }

//    private fun loadIntent(intent: Intent): Boolean {
//        val reuseIsolate = false
//        return loadIntent(intent, reuseIsolate)
//    }

//    private fun loadIntent(intent: Intent, reuseIsolate: Boolean): Boolean {
//        val action = intent.action
//        if (Intent.ACTION_RUN == action) {
//            val route = intent.getStringExtra("route")
//            var appBundlePath = intent.dataString
//            if (appBundlePath == null) {
//                // Fall back to the installation path if no bundle path
//                // was specified.
//                appBundlePath = FlutterMain.findAppBundlePath(activity.applicationContext)
//            }
//            if (route != null) {
//                flutterView.setInitialRoute(route)
//            }
//            if (!flutterView.getFlutterNativeView().isApplicationRunning()) {
//                flutterView.runFromBundle(appBundlePath, null, "main", reuseIsolate)
//            }
//            return true
//        }
//
//        return false
//    }

    /**
     * Creates a [View] containing the same [Drawable] as the one set as the
     * `windowBackground` of the parent activity for use as a launch splash view.
     *
     * Returns null if no `windowBackground` is set for the activity.
     */
    private fun createLaunchView(): View? {
        if (!showSplashScreenUntilFirstFrame()) {
            return null
        }
        val launchScreenDrawable = getLaunchScreenDrawableFromActivityTheme() ?: return null
        val view = View(activity)
        view.layoutParams = matchParent
        // TODO: view.background = launchScreenDrawable
        return view
    }

    /**
     * Extracts a {@link Drawable} from the parent activity's {@code windowBackground}.
     *
     * {@code android:windowBackground} is specifically reused instead of a other attributes
     * because the Android framework can display it fast enough when launching the app as opposed
     * to anything defined in the Activity subclass.
     *
     * Returns null if no {@code windowBackground} is set for the activity.
     */
    @SuppressWarnings("deprecation")
    private fun getLaunchScreenDrawableFromActivityTheme(): Drawable? {
        val typedValue = TypedValue()
        if (!activity.getTheme()
                        .resolveAttribute(android.R.attr.windowBackground, typedValue, true)) {
            return null
        }
        if (typedValue.resourceId == 0) {
            return null
        }
        try {
            return activity.getResources().getDrawable(typedValue.resourceId)
        } catch (e: NotFoundException) {
            Log.e(TAG, "Referenced launch screen windowBackground resource does not exist")
            return null
        }
    }

    /**
     * Let the user specify whether the activity's `windowBackground` is a launch screen
     * and should be shown until the first frame via a <meta-data> tag in the activity.
    </meta-data> */
    @SuppressLint("WrongConstant")
    private fun showSplashScreenUntilFirstFrame(): Boolean {
        try {
            val activityInfo = activity.packageManager.getActivityInfo(
                    activity.componentName,
                    PackageManager.GET_META_DATA or PackageManager.GET_ACTIVITIES)
            val metadata = activityInfo.metaData
            return metadata != null && metadata.getBoolean(SPLASH_SCREEN_META_DATA_KEY)
        } catch (e: NameNotFoundException) {
            return false
        }
    }

    /**
    * Show and then automatically animate out the launch view.
    *
    * If a launch screen is defined in the user application's AndroidManifest.xml as the
    * activity's {@code windowBackground}, display it on top of the {@link FlutterView} and
    * remove the activity's {@code windowBackground}.
    *
    * Fade it out and remove it when the {@link FlutterView} renders its first frame.
    */
//    private fun addLaunchView() {
//        if (launchView == null) {
//            return
//        }
//
//        activity.addContentView(launchView, matchParent)
//        flutterView.addFirstFrameListener(FlutterView.FirstFrameListener() {
//            @Override
//            public void onFirstFrame() {
//                FlutterActivityDelegate.this.launchView.animate()
//                    .alpha(0f)
//                    // Use Android's default animation duration.
//                    .setListener(new AnimatorListenerAdapter() {
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            // Views added to an Activity's addContentView is always added to its
//                            // root FrameLayout.
//                            ((ViewGroup) FlutterActivityDelegate.this.launchView.getParent())
//                                .removeView(FlutterActivityDelegate.this.launchView);
//                            FlutterActivityDelegate.this.launchView = null;
//                        }
//                    });
//
//                FlutterActivityDelegate.this.flutterView.removeFirstFrameListener(this);
//            }
//        });
//
//        // Resets the activity theme from the one containing the launch screen in the window
//        // background to a blank one since the launch screen is now in a view in front of the
//        // FlutterView.
//        //
//        // We can make this configurable if users want it.
//        activity.setTheme(android.R.style.Theme_Black_NoTitleBar);
//    }
}