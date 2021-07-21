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
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import kotlinx.coroutines.flow.Flow

/**
 * An interface to provide all the relevant info about a [android.view.Window].
 */
public interface WindowInfoRepo {

    /**
     * Returns a [Flow] for consuming the current [WindowMetrics] according to the current
     * system state.
     *
     * The metrics describe the size of the area the window would occupy with
     * [MATCH_PARENT][android.view.WindowManager.LayoutParams.MATCH_PARENT] width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     *
     * The value of this is based on the **current** windowing state of the system. For
     * example, for activities in multi-window mode, the metrics returned are based on the
     * current bounds that the user has selected for the [Activity][android.app.Activity]'s
     * window.
     *
     * @see android.view.WindowManager.getCurrentWindowMetrics
     */
    public val currentWindowMetrics: Flow<WindowMetrics>

    /**
     * A [Flow] of [WindowLayoutInfo] that contains all the available features.
     */
    public val windowLayoutInfo: Flow<WindowLayoutInfo>

    public companion object {

        private var decorator: WindowInfoRepoDecorator = EmptyDecorator

        @JvmStatic
        public fun create(activity: Activity): WindowInfoRepo {
            val taggedRepo = activity.getTag(R.id.androidx_window_activity_scope) as? WindowInfoRepo
            val repo = taggedRepo ?: activity.getOrCreateTag(R.id.androidx_window_activity_scope) {
                WindowInfoRepoImpl(
                    activity,
                    WindowMetricsCalculatorCompat,
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