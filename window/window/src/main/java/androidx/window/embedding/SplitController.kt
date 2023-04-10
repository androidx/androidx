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

package androidx.window.embedding

import android.app.Activity
import android.content.Context
import androidx.core.util.Consumer
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A singleton controller class that gets information about the currently active activity
 * splits and provides interaction points to customize the splits and form new
 * splits.
 *
 * A split is a pair of containers that host activities in the same or different
 * processes, combined under the same parent window of the hosting task.
 *
 * A pair of activities can be put into a split by providing a static or runtime
 * split rule and then launching the activities in the same task using
 * [Activity.startActivity()][android.app.Activity.startActivity].
 */
class SplitController private constructor(applicationContext: Context) {
    private val embeddingBackend: EmbeddingBackend = ExtensionEmbeddingBackend
        .getInstance(applicationContext)

    // TODO(b/258356512): Make this method a flow API
    /**
     * Registers a listener for updates about the active split state(s) that this
     * activity is part of. An activity can be in zero, one or more active splits.
     * More than one active split is possible if an activity created multiple
     * containers to side, stacked on top of each other. Or it can be in two
     * different splits at the same time - in a secondary container for one (it was
     * launched to the side) and in the primary for another (it launched another
     * activity to the side). The reported splits in the list are ordered from
     * bottom to top by their z-order, more recent splits appearing later.
     * Guaranteed to be called at least once to report the most recent state.
     *
     * @param activity only split that this [Activity] is part of will be reported.
     * @param executor when there is an update to the active split state(s), the [consumer] will be
     * invoked on this [Executor].
     * @param consumer [Consumer] that will be invoked on the [executor] when there is an update to
     * the active split state(s).
     */
    fun addSplitListener(
        activity: Activity,
        executor: Executor,
        consumer: Consumer<List<SplitInfo>>
    ) {
        embeddingBackend.addSplitListenerForActivity(activity, executor, consumer)
    }

    /**
     * Unregisters a listener that was previously registered via [addSplitListener].
     *
     * @param consumer the previously registered [Consumer] to unregister.
     */
    fun removeSplitListener(
        consumer: Consumer<List<SplitInfo>>
    ) {
        embeddingBackend.removeSplitListenerForActivity(consumer)
    }

    /**
     * Indicates whether split functionality is supported on the device. Note
     * that devices might not enable splits in all states or conditions. For
     * example, a foldable device with multiple screens can choose to collapse
     * splits when apps run on the device's small display, but enable splits
     * when apps run on the device's large display. In cases like this,
     * `isSplitSupported` always returns `true`, and if the split is collapsed,
     * activities are launched on top, following the non-activity embedding
     * model.
     */
    fun isSplitSupported(): Boolean {
        return embeddingBackend.isSplitSupported()
    }

    companion object {
        @Volatile
        private var globalInstance: SplitController? = null
        private val globalLock = ReentrantLock()

        internal const val sDebug = false

        /**
         * Obtains the singleton instance of [SplitController].
         *
         * @param context the [Context] to initialize the controller with
         */
        @JvmStatic
        fun getInstance(context: Context): SplitController {
            if (globalInstance == null) {
                globalLock.withLock {
                    if (globalInstance == null) {
                        globalInstance = SplitController(context.applicationContext)
                    }
                }
            }
            return globalInstance!!
        }
    }
}