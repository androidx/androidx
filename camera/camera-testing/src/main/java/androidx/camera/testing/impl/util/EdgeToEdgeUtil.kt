/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.testing.impl.util

import android.R
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** A utility class for enabling edge-to-edge display in an activity. */
public object EdgeToEdgeUtil {
    /**
     * Enables edge-to-edge display for the given activity.
     *
     * This function sets the activity to display its content behind the system bars (status bar and
     * navigation bar). It then applies window insets as padding or margins to specified views to
     * prevent them from being obscured by the system bars.
     *
     * Note: This is only effective on API 35 (Android V) and above.
     *
     * @param activity The [androidx.appcompat.app.AppCompatActivity] to enable edge-to-edge display
     *   for.
     * @param applyWindowInsetsListenerViewId The ID of the view to which the
     *   [android.view.View.OnApplyWindowInsetsListener] will be attached. This view will typically
     *   have its padding adjusted to the system bar insets. Defaults to `android.R.id.content`.
     * @param viewIdsTopPaddingRequired A list of view IDs that require top margin to be set. The
     *   margin will be the height of the action bar.
     */
    @JvmStatic
    public fun enableEdgeToEdge(
        activity: AppCompatActivity,
        applyWindowInsetsListenerViewId: Int = R.id.content,
        viewIdsTopPaddingRequired: List<Int> = emptyList(),
    ) {
        // Skips the edge to edge setup on older devices.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return
        }

        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        ViewCompat.setOnApplyWindowInsetsListener(
            activity.findViewById(applyWindowInsetsListenerViewId)
        ) { v, insets ->
            v.post {
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

                val actionBarHeight = activity.supportActionBar?.height ?: 0

                viewIdsTopPaddingRequired.forEach { id ->
                    activity.findViewById<View>(id).let { view ->
                        val params = view.layoutParams
                        if (params is ViewGroup.MarginLayoutParams) {
                            params.topMargin = actionBarHeight
                            view.layoutParams = params
                        }
                    }
                }
            }
            insets
        }
    }
}
