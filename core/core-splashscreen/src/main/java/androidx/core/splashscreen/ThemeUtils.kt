/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.splashscreen

import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

/**
 * Utility function for applying themes fixes to the system bar when the [SplashScreenViewProvider]
 * is added and removed.
 */
@RequiresApi(31)
internal object ThemeUtils {
    object Api31 {

        /**
         * Apply the theme's values for the system bar appearance to the decorView.
         *
         * This needs to be called when the [SplashScreenViewProvider] is added and after it's been
         * removed.
         */
        @JvmStatic
        @JvmOverloads
        @DoNotInline
        fun applyThemesSystemBarAppearance(
            theme: Resources.Theme,
            decor: View,
            tv: TypedValue = TypedValue()
        ) {
            var appearance = 0
            val mask =
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            if (theme.resolveAttribute(android.R.attr.windowLightStatusBar, tv, true)) {
                if (tv.data != 0) {
                    appearance = appearance or WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                }
            }
            if (theme.resolveAttribute(android.R.attr.windowLightNavigationBar, tv, true)) {
                if (tv.data != 0) {
                    appearance =
                        appearance or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                }
            }
            decor.windowInsetsController!!.setSystemBarsAppearance(appearance, mask)
        }
    }
}
