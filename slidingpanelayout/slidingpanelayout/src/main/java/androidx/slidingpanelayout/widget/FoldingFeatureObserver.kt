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

package androidx.slidingpanelayout.widget

import android.app.Activity
import androidx.window.FoldingFeature
import androidx.window.WindowInfoRepo
import androidx.window.WindowLayoutInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * A device folding feature observer is used to notify listener when there is a folding feature
 * change.
 */
internal class FoldingFeatureObserver(
    private val windowInfoRepo: WindowInfoRepo,
    private val executor: Executor
) {
    private var job: Job? = null
    private var onFoldingFeatureChangeListener: OnFoldingFeatureChangeListener? = null

    /**
     * Interface definition for a callback to be invoked when there is a folding feature change
     */
    internal interface OnFoldingFeatureChangeListener {
        /**
         * Callback method to update window layout when there is a folding feature change
         */
        fun onFoldingFeatureChange(foldingFeature: FoldingFeature)
    }

    /**
     * Register a listener that can be notified when there is a folding feature change.
     *
     * @param onFoldingFeatureChangeListener The listener to be added
     */
    fun setOnFoldingFeatureChangeListener(
        onFoldingFeatureChangeListener: OnFoldingFeatureChangeListener
    ) {
        this.onFoldingFeatureChangeListener = onFoldingFeatureChangeListener
    }

    /**
     * Registers a callback for layout changes of the window for the supplied [Activity].
     * Must be called only after the it is attached to the window.
     */
    fun registerLayoutStateChangeCallback() {
        job?.cancel()
        job = CoroutineScope(executor.asCoroutineDispatcher()).launch {
            windowInfoRepo.windowLayoutInfo
                .mapNotNull { info -> getFoldingFeature(info) }
                .distinctUntilChanged()
                .collect { nextFeature ->
                    onFoldingFeatureChangeListener?.onFoldingFeatureChange(nextFeature)
                }
        }
    }

    /**
     * Unregisters a callback for window layout changes of the [Activity] window.
     */
    fun unregisterLayoutStateChangeCallback() {
        job?.cancel()
    }

    private fun getFoldingFeature(windowLayoutInfo: WindowLayoutInfo): FoldingFeature? {
        return windowLayoutInfo.displayFeatures
            .firstOrNull { feature -> feature is FoldingFeature } as? FoldingFeature
    }
}