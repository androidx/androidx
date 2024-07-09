/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.AndroidViewConfigurationApi34.getScaledHandwritingGestureLineMargin
import androidx.compose.ui.platform.AndroidViewConfigurationApi34.getScaledHandwritingSlop

/**
 * A [ViewConfiguration] with Android's default configurations. Derived from
 * [android.view.ViewConfiguration]
 */
class AndroidViewConfiguration(private val viewConfiguration: android.view.ViewConfiguration) :
    ViewConfiguration {
    override val longPressTimeoutMillis: Long
        get() = android.view.ViewConfiguration.getLongPressTimeout().toLong()

    override val doubleTapTimeoutMillis: Long
        get() = android.view.ViewConfiguration.getDoubleTapTimeout().toLong()

    override val doubleTapMinTimeMillis: Long
        get() = 40

    override val touchSlop: Float
        get() = viewConfiguration.scaledTouchSlop.toFloat()

    override val handwritingSlop: Float
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                getScaledHandwritingSlop(viewConfiguration)
            } else {
                super.handwritingSlop
            }

    override val maximumFlingVelocity: Float
        get() = viewConfiguration.scaledMaximumFlingVelocity.toFloat()

    override val handwritingGestureLineMargin: Float
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                getScaledHandwritingGestureLineMargin(viewConfiguration)
            } else {
                super.handwritingGestureLineMargin
            }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private object AndroidViewConfigurationApi34 {
    fun getScaledHandwritingSlop(viewConfiguration: android.view.ViewConfiguration) =
        viewConfiguration.scaledHandwritingSlop.toFloat()

    fun getScaledHandwritingGestureLineMargin(viewConfiguration: android.view.ViewConfiguration) =
        viewConfiguration.scaledHandwritingGestureLineMargin.toFloat()
}
