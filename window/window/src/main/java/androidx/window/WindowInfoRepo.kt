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

import android.app.Activity
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

/**
 * An interface to provide all the relevant info about a [android.view.Window].
 */
public interface WindowInfoRepo {

    /**
     * Returns the [WindowMetrics] according to the current system state.
     *
     *
     * The metrics describe the size of the area the window would occupy with
     * [MATCH_PARENT][android.view.WindowManager.LayoutParams.MATCH_PARENT] width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     *
     *
     * The value of this is based on the **current** windowing state of the system. For
     * example, for activities in multi-window mode, the metrics returned are based on the
     * current bounds that the user has selected for the [Activity][android.app.Activity]'s
     * window.
     *
     * @see maximumWindowMetrics
     * @see android.view.WindowManager.getCurrentWindowMetrics
     */
    public val currentWindowMetrics: WindowMetrics

    /**
     * Returns the largest [WindowMetrics] an app may expect in the current system state.
     *
     *
     * The metrics describe the size of the largest potential area the window might occupy with
     * [MATCH_PARENT][android.view.WindowManager.LayoutParams.MATCH_PARENT] width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     *
     *
     * The value of this is based on the largest **potential** windowing state of the system.
     * For example, for activities in multi-window mode the metrics returned are based on what the
     * bounds would be if the user expanded the window to cover the entire screen.
     *
     *
     * Note that this might still be smaller than the size of the physical display if certain
     * areas of the display are not available to windows created for the associated [Context].
     * For example, devices with foldable displays that wrap around the enclosure may split the
     * physical display into different regions, one for the front and one for the back, each acting
     * as different logical displays. In this case [maximumWindowMetrics] would return
     * the region describing the side of the device the associated [context's][Context]
     * window is placed.
     *
     * @see currentWindowMetrics
     * @see android.view.WindowManager.getMaximumWindowMetrics
     */
    public val maximumWindowMetrics: WindowMetrics

    /**
     * A [Flow] of [WindowLayoutInfo] that contains all the available features.
     */
    public val windowLayoutInfo: Flow<WindowLayoutInfo>

    public companion object {

        private var decorator: WindowInfoRepoDecorator = EmptyDecorator

        @JvmStatic
        @ExperimentalCoroutinesApi
        public fun create(activity: Activity): WindowInfoRepo {
            val taggedRepo = activity.getTag(R.id.androidx_window_activity_scope) as? WindowInfoRepo
            val repo = taggedRepo ?: activity.getOrCreateTag(R.id.androidx_window_activity_scope) {
                WindowInfoRepoImp(
                    activity,
                    WindowBoundsHelper(),
                    ExtensionWindowBackend.getInstance(activity)
                )
            }
            return decorator.decorate(repo)
        }

        @JvmStatic
        @RestrictTo(LIBRARY_GROUP)
        public fun overrideDecorator(overridingDecorator: WindowInfoRepoDecorator) {
            decorator = overridingDecorator
        }

        @JvmStatic
        @RestrictTo(LIBRARY_GROUP)
        public fun reset() {
            decorator = EmptyDecorator
        }
    }
}

@RestrictTo(LIBRARY_GROUP)
public interface WindowInfoRepoDecorator {
    /**
     * Returns an instance of [WindowInfoRepo] associated to the [Activity]
     */
    @RestrictTo(LIBRARY_GROUP)
    public fun decorate(repo: WindowInfoRepo): WindowInfoRepo
}

private object EmptyDecorator : WindowInfoRepoDecorator {
    override fun decorate(repo: WindowInfoRepo): WindowInfoRepo {
        return repo
    }
}