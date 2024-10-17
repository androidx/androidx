package androidx.window

import android.app.Activity
import android.app.Application
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Assume.assumeTrue

open class WindowTestUtils {
    companion object {

        @RequiresApi(Build.VERSION_CODES.R)
        fun createOverlayWindowContext(): Context {
            val context = ApplicationProvider.getApplicationContext<Application>()
            return context
                .createDisplayContext(
                    context
                        .getSystemService(DisplayManager::class.java)
                        .getDisplay(Display.DEFAULT_DISPLAY)
                )
                .createWindowContext(
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    /* options= */ null
                )
        }

        fun assumeVendorApiLevel(level: Int) {
            val apiLevel = WindowSdkExtensions.getInstance().extensionVersion
            assumeTrue(apiLevel == level)
        }

        fun assumeAtLeastVendorApiLevel(min: Int) {
            val apiLevel = WindowSdkExtensions.getInstance().extensionVersion
            assumeTrue(apiLevel >= min)
        }

        fun assumeBeforeVendorApiLevel(max: Int) {
            val apiLevel = WindowSdkExtensions.getInstance().extensionVersion
            assumeTrue(apiLevel < max)
            assumeTrue(apiLevel > 0)
        }

        fun isInMultiWindowMode(activity: Activity): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                activity.isInMultiWindowMode
            } else false
        }

        fun assumePlatformBeforeR() {
            assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
        }

        fun assumePlatformROrAbove() {
            assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        }

        fun assumePlatformBeforeU() {
            assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        }

        fun assumePlatformUOrAbove() {
            assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        }

        /**
         * Creates and launches an activity performing the supplied actions at various points in the
         * activity lifecycle.
         *
         * @param initialAction the action that will run once before the activity is created.
         * @param verifyAction the action to run once after each change in activity lifecycle state.
         */
        fun runActionsAcrossActivityLifecycle(
            scenarioRule: ActivityScenarioRule<TestActivity>,
            initialAction: ActivityScenario.ActivityAction<TestActivity>,
            verifyAction: ActivityScenario.ActivityAction<TestActivity>
        ) {
            val scenario = scenarioRule.scenario
            scenario.onActivity(initialAction)
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity(verifyAction)
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.onActivity(verifyAction)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity(verifyAction)
        }
    }
}
