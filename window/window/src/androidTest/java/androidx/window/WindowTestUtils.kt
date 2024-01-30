package androidx.window

import android.app.Application
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.window.core.ExtensionsUtil
import org.junit.Assume.assumeTrue

open class WindowTestUtils {
    companion object {

        @RequiresApi(Build.VERSION_CODES.R)
        fun createOverlayWindowContext(): Context {
            val context = ApplicationProvider.getApplicationContext<Application>()
            return context.createDisplayContext(
                context.getSystemService(DisplayManager::class.java)
                    .getDisplay(Display.DEFAULT_DISPLAY)
            ).createWindowContext(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                /* options= */ null
            )
        }

        @OptIn(androidx.window.core.ExperimentalWindowApi::class)
        fun assumeAtLeastVendorApiLevel(min: Int) {
            val apiLevel = ExtensionsUtil.safeVendorApiLevel
            assumeTrue(apiLevel >= min)
        }

        @OptIn(androidx.window.core.ExperimentalWindowApi::class)
        fun assumeBeforeVendorApiLevel(max: Int) {
            val apiLevel = ExtensionsUtil.safeVendorApiLevel
            assumeTrue(apiLevel < max)
            assumeTrue(apiLevel > 0)
        }
    }
}
