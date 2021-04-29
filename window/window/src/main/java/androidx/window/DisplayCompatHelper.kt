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

package androidx.window

import android.graphics.Point
import android.os.Build
import android.view.Display
import android.view.DisplayCutout
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
internal object DisplayCompatHelperApi17 {

    @Suppress("DEPRECATION")
    fun getRealSize(display: Display, point: Point) {
        display.getRealSize(point)
    }
}

@RequiresApi(Build.VERSION_CODES.P)
internal object DisplayCompatHelperApi28 {

    fun safeInsetLeft(displayCutout: DisplayCutout): Int {
        return displayCutout.safeInsetLeft
    }

    fun safeInsetTop(displayCutout: DisplayCutout): Int {
        return displayCutout.safeInsetTop
    }

    fun safeInsetRight(displayCutout: DisplayCutout): Int {
        return displayCutout.safeInsetRight
    }

    fun safeInsetBottom(displayCutout: DisplayCutout): Int {
        return displayCutout.safeInsetBottom
    }
}