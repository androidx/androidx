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

package androidx.window.layout

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.window.core.ConsumerAdapter
import kotlinx.coroutines.flow.Flow

/**
 * An interface to provide all the relevant info about a [android.view.Window].
 */
public interface WindowInfoTracker {

    /**
     * A [Flow] of [WindowLayoutInfo] that contains all the available features. A [WindowLayoutInfo]
     * contains a [List] of [DisplayFeature] that intersect the associated [android.view.Window].
     *
     * The first [WindowLayoutInfo] will not be emitted until [Activity.onStart] has been called.
     * which values you receive and when is device dependent. It is recommended to test scenarios
     * where there is a long delay between subscribing and receiving the first value or never
     * receiving a value, since it may differ between hardware implementations.
     *
     * Since the information is associated to the [Activity] you should not retain the [Flow] across
     * [Activity] recreations. Doing so can result in unpredictable behavior such as a memory leak
     * or incorrect values for [WindowLayoutInfo].
     *
     * @param activity an [Activity] that has not been destroyed.
     * @see WindowLayoutInfo
     * @see DisplayFeature
     */
    public fun windowLayoutInfo(activity: Activity): Flow<WindowLayoutInfo>

    public companion object {

        private val DEBUG = false
        private val TAG = WindowInfoTracker::class.simpleName

        @Suppress("MemberVisibilityCanBePrivate") // Avoid synthetic accessor
        internal val extensionBackend: WindowBackend? by lazy {
            try {
                val loader = WindowInfoTracker::class.java.classLoader
                val provider = loader?.let {
                    SafeWindowLayoutComponentProvider(loader, ConsumerAdapter(loader))
                }
                provider?.windowLayoutComponent?.let { component ->
                    ExtensionWindowLayoutInfoBackend(component, ConsumerAdapter(loader))
                }
            } catch (t: Throwable) {
                if (DEBUG) {
                    Log.d(TAG, "Failed to load WindowExtensions")
                }
                null
            }
        }

        private var decorator: WindowInfoTrackerDecorator = EmptyDecorator

        /**
         * Provide an instance of [WindowInfoTracker] that is associated to the given [Context].
         * The instance created should be safe to retain globally. The actual data is provided by
         * [WindowInfoTracker.windowLayoutInfo] and requires an [Activity].
         * @param context Any valid [Context].
         * @see WindowInfoTracker.windowLayoutInfo
         */
        @JvmName("getOrCreate")
        @JvmStatic
        public fun getOrCreate(context: Context): WindowInfoTracker {
            val backend = extensionBackend ?: SidecarWindowBackend.getInstance(context)
            val repo = WindowInfoTrackerImpl(WindowMetricsCalculatorCompat, backend)
            return decorator.decorate(repo)
        }

        @JvmStatic
        @RestrictTo(LIBRARY_GROUP)
        public fun overrideDecorator(overridingDecorator: WindowInfoTrackerDecorator) {
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
public interface WindowInfoTrackerDecorator {
    /**
     * Returns an instance of [WindowInfoTracker] associated to the [Activity]
     */
    @RestrictTo(LIBRARY_GROUP)
    public fun decorate(tracker: WindowInfoTracker): WindowInfoTracker
}

private object EmptyDecorator : WindowInfoTrackerDecorator {
    override fun decorate(tracker: WindowInfoTracker): WindowInfoTracker {
        return tracker
    }
}
