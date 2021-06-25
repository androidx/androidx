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

package androidx.slidingpanelayout.widget.helpers

import android.app.Activity
import android.graphics.Rect
import androidx.core.util.Consumer
import androidx.window.DisplayFeature
import androidx.window.FoldingFeature
import androidx.window.WindowBackend
import androidx.window.WindowLayoutInfo
import androidx.window.WindowManager
import java.util.concurrent.Executor

/**
 * A fake window backend for testing. The fake window backend provides a basic implementation
 * folding in middle to mimic fold/unfold behavior in real world.
 */
public class FakeWindowBackend(private val foldAxis: FoldAxis) : WindowBackend {
    private var foldState = FoldingFeature.State.HALF_OPENED
    private var foldWidth: Int = 0
    private var windowLayoutInfoCallback: Consumer<WindowLayoutInfo>? = null
    private var windowLayoutInfoExecutor: Executor? = null

    public enum class FoldAxis {
        HORIZONTAL,
        VERTICAL
    }

    public companion object {
        public fun getFoldPosition(activity: Activity, foldAxis: FoldAxis, foldWidth: Int): Rect {
            val windowSize = WindowManager(activity).getCurrentWindowMetrics().bounds
            return midScreenFold(windowSize, foldAxis, foldWidth)
        }

        private fun midScreenFold(windowBounds: Rect, foldAxis: FoldAxis, foldWidth: Int): Rect {
            return when (foldAxis) {
                FoldAxis.HORIZONTAL -> {
                    Rect(
                        0,
                        windowBounds.height() / 2 - foldWidth / 2,
                        windowBounds.width(),
                        windowBounds.height() / 2 + foldWidth / 2
                    )
                }
                FoldAxis.VERTICAL -> {
                    Rect(
                        windowBounds.width() / 2 - foldWidth / 2,
                        0,
                        windowBounds.width() / 2 + foldWidth / 2,
                        windowBounds.height()
                    )
                }
            }
        }
    }

    override fun registerLayoutChangeCallback(
        activity: Activity,
        executor: Executor,
        callback: Consumer<WindowLayoutInfo>
    ) {
        windowLayoutInfoCallback = callback
        windowLayoutInfoExecutor = executor
        executor.execute { callback.accept(getWindowLayoutInfo(activity)) }
    }

    override fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>) {
        windowLayoutInfoCallback = null
        windowLayoutInfoExecutor = null
    }

    /**
     * Set the width of fold
     */
    public fun setFoldWidth(width: Int) {
        foldWidth = width
    }

    /**
     * Toggle folding state between [FoldingFeature.State.HALF_OPENED] and
     * [FoldingFeature.State.FLAT] Initial state is [FoldingFeature.State.HALF_OPENED]
     */
    public fun toggleFoldState(activity: Activity) {
        foldState = if (foldState == FoldingFeature.State.HALF_OPENED) {
            FoldingFeature.State.FLAT
        } else {
            FoldingFeature.State.HALF_OPENED
        }
        windowLayoutInfoExecutor?.execute {
            windowLayoutInfoCallback?.accept(getWindowLayoutInfo(activity))
        }
    }

    private fun getWindowLayoutInfo(activity: Activity): WindowLayoutInfo {
        val windowSize = WindowManager(activity).getCurrentWindowMetrics().bounds
        val featureRect = midScreenFold(windowSize, foldAxis, foldWidth)
        val displayFeature = FoldingFeature(featureRect, FoldingFeature.Type.FOLD, foldState)
        if (foldState == FoldingFeature.State.FLAT) {
            assert(!displayFeature.isSeparating)
        } else {
            assert(displayFeature.isSeparating)
        }
        val featureList = ArrayList<DisplayFeature>()
        featureList.add(displayFeature)
        return WindowLayoutInfo.Builder().setDisplayFeatures(featureList).build()
    }
}