/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.fragment.app

import android.view.View
import androidx.collection.ArrayMap

/** Contains the Fragment Transition functionality. */
internal object FragmentTransition {
    @JvmField val PLATFORM_IMPL: FragmentTransitionImpl? = FragmentTransitionCompat21()

    @JvmField val SUPPORT_IMPL = resolveSupportImpl()

    @Suppress("UNCHECKED_CAST")
    private fun resolveSupportImpl(): FragmentTransitionImpl? =
        try {
            val impl =
                Class.forName("androidx.transition.FragmentTransitionSupport")
                    as Class<FragmentTransitionImpl>
            impl.getDeclaredConstructor().newInstance()
        } catch (ignored: Exception) {
            // support-transition is not loaded; ignore
            null
        }

    /** Utility to find the String key in [map] that maps to [value]. */
    @JvmStatic
    fun ArrayMap<String, String>.findKeyForValue(value: String): String? =
        filter { entry ->
                // Find the entries with the given value
                entry.value == value
            }
            .map { entry ->
                // And get the key associated with that value
                entry.key
            }
            .firstOrNull()

    /**
     * A utility to retain only the mappings in the map that have a value that has a key in
     * [namedViews]. This is a useful equivalent to [ArrayMap.retainAll] for values.
     */
    @JvmStatic
    fun ArrayMap<String, String>.retainValues(namedViews: ArrayMap<String, View>) {
        for (i in (size - 1) downTo 0) {
            val targetName = valueAt(i)
            if (!namedViews.containsKey(targetName)) {
                removeAt(i)
            }
        }
    }

    /**
     * Calls the [android.app.SharedElementCallback.onSharedElementStart] or
     * [android.app.SharedElementCallback.onSharedElementEnd] on the appropriate incoming or
     * outgoing fragment.
     *
     * @param inFragment The incoming fragment
     * @param outFragment The outgoing fragment
     * @param isPop Is the incoming fragment part of a pop transaction?
     * @param sharedElements The shared element Views
     * @param isStart Call the start or end call on the SharedElementCallback
     */
    @JvmStatic
    fun callSharedElementStartEnd(
        inFragment: Fragment,
        outFragment: Fragment,
        isPop: Boolean,
        sharedElements: ArrayMap<String, View>,
        isStart: Boolean,
    ) {
        val sharedElementCallback =
            if (isPop) {
                outFragment.enterTransitionCallback
            } else {
                inFragment.enterTransitionCallback
            }
        if (sharedElementCallback != null) {
            val views = sharedElements.map { it.value }
            val names = sharedElements.map { it.key }
            if (isStart) {
                sharedElementCallback.onSharedElementStart(names, views, null)
            } else {
                sharedElementCallback.onSharedElementEnd(names, views, null)
            }
        }
    }

    /** Sets the visibility of all Views in [views] to [visibility]. */
    @JvmStatic
    fun setViewVisibility(views: List<View>, visibility: Int) {
        views.forEach { view -> view.visibility = visibility }
    }

    @JvmStatic
    fun supportsTransition(): Boolean {
        return PLATFORM_IMPL != null || SUPPORT_IMPL != null
    }
}
