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

@file:Suppress("DEPRECATION") // TODO(b/173739071) Remove DeviceState

package androidx.window.sample.backend

import android.app.Activity
import android.graphics.Point
import android.graphics.Rect
import androidx.core.util.Consumer
import androidx.window.DisplayFeature
import androidx.window.FoldingFeature
import androidx.window.WindowBackend
import androidx.window.WindowLayoutInfo
import java.util.concurrent.Executor

/**
 * A sample backend that will have a fold in the middle of the screen. The {@link FoldAxis}
 * specifies which axis is followed. This sample backend can be used to model devices that open
 * like a clam shell or a book. This is relative to the display's dimensions. For displays that
 * are taller than they are wide with a short fold axis they will mimic a horizontal fold. For
 * displays that are wider than they are tall with a short fold axis will mimic a vertical fold.
 *
 * <p>The {@link DeviceState} is fixed to have an opened posture. This sample implementation
 * ignores the device state listener. Combine with {@link InitialValueWindowBackendDecorator} to
 * receive the initial value through the listener.
 *
 * <p>The {@link WindowLayoutInfo} is also fixed to have a {@link TYPE_FOLD} where the longest
 * dimension runs parallel to the specified {@link FoldAxis}. The fold is placed in the middle
 * of the display. Like the {@link DeviceState}, the sample implementation ignores the window
 * layout info listener. Combine with {@link InitialValueWindowBackendDecorator} to
 * receive the initial value through the listener.
 */
class MidScreenFoldBackend(private val foldAxis: FoldAxis) : WindowBackend {
    /**
     * The side which the fold axis should be parallel to.
     */
    enum class FoldAxis {
        LONG_DIMENSION,
        SHORT_DIMENSION
    }

    /**
     * @param activity Currently running {@link Activity}.
     * @return A fake {@link WindowLayoutInfo} with a fold in the middle matching the {@link
     * FoldAxis}.
     */
    private fun getWindowLayoutInfo(activity: Activity): WindowLayoutInfo {
        val windowSize = activity.calculateWindowSizeExt()
        val featureRect = foldRect(windowSize)

        val displayFeature =
            FoldingFeature(
                featureRect,
                FoldingFeature.Type.FOLD,
                FoldingFeature.State.FLAT
            )
        val featureList = ArrayList<DisplayFeature>()
        featureList.add(displayFeature)
        return WindowLayoutInfo.Builder().setDisplayFeatures(featureList).build()
    }

    private fun foldRect(windowSize: Point): Rect {
        return when (foldAxis) {
            FoldAxis.LONG_DIMENSION -> longDimensionFold(windowSize)
            FoldAxis.SHORT_DIMENSION -> shortDimensionFold(windowSize)
        }
    }

    private fun shortDimensionFold(windowSize: Point): Rect {
        return if (windowSize.x >= windowSize.y) { // Landscape
            Rect(windowSize.x / 2, 0, windowSize.x / 2, windowSize.y)
        } else { // Portrait
            Rect(0, windowSize.y / 2, windowSize.x, windowSize.y / 2)
        }
    }

    private fun longDimensionFold(windowSize: Point): Rect {
        return if (windowSize.x >= windowSize.y) { // Landscape
            Rect(0, windowSize.y / 2, windowSize.x, windowSize.y / 2)
        } else { // Portrait
            Rect(windowSize.x / 2, 0, windowSize.x / 2, windowSize.y)
        }
    }

    override fun registerLayoutChangeCallback(
        activity: Activity,
        executor: Executor,
        callback: Consumer<WindowLayoutInfo>
    ) {
        executor.execute { callback.accept(getWindowLayoutInfo(activity)) }
    }

    override fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>) {
    }
}