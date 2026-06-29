/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.camera2.testing

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.WindowInsets
import android.view.WindowMetrics
import androidx.annotation.RequiresApi
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.util.ReflectionHelpers

/**
 * Custom shadow for `WindowManagerImpl` to correctly support multiple displays under Robolectric.
 *
 * Intercepts calls to [getMaximumWindowMetrics] on API 30+ and calculates bounds using the actual
 * display associated with the context, rather than defaulting to display 0.
 */
@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.R)
@Implements(className = "android.view.WindowManagerImpl", minSdk = Build.VERSION_CODES.R)
public class TestShadowWindowManager {
    @RealObject private lateinit var windowManagerImpl: Any

    @Implementation
    protected fun getMaximumWindowMetrics(): WindowMetrics {
        val context = ReflectionHelpers.getField<Context>(windowManagerImpl, "mContext")
        val display = context.display
        val displaySize = Point()
        display.getRealSize(displaySize)
        val rect = Rect(0, 0, displaySize.x, displaySize.y)
        val windowInsets = WindowInsets.Builder().build()
        return WindowMetrics(rect, windowInsets)
    }
}
