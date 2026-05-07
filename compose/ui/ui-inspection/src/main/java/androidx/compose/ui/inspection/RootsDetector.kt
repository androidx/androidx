/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.inspection

import android.view.View
import android.view.inspector.WindowInspector
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.ui.R
import androidx.compose.ui.inspection.framework.flatten
import androidx.compose.ui.inspection.framework.isAndroidComposeView
import androidx.compose.ui.inspection.util.ThreadUtils
import androidx.inspection.InspectorEnvironment

class RootsDetector(environment: InspectorEnvironment) {
    private val xrHelper = XrHelper(environment)

    fun getRoots(): List<View> {
        val xrViews = xrHelper.getXrViews()
        return xrViews.ifEmpty { getAndroidViews() }
    }

    fun getAllRoots(): List<View> {
        return xrHelper.getXrViews() + getAndroidViews()
    }

    fun getAllCompositionRoots(): Set<CompositionData> {
        val views = getAllRoots()
        val composeViews = mutableSetOf<View>()
        views.forEach { view ->
            view.flatten().filter { it.isAndroidComposeView() }.forEach { composeViews.add(it) }
        }
        val roots = mutableSetOf<CompositionData>()
        composeViews.forEach { view -> roots.addAll(view.compositionRoots) }
        return roots
    }

    val View.compositionRoots: Set<CompositionData>
        get() {
            @Suppress("UNCHECKED_CAST")
            return getTag(R.id.inspection_slot_table_set) as? Set<CompositionData> ?: emptySet()
        }

    private fun getAndroidViews(): List<View> {
        ThreadUtils.assertOnMainThread()
        val views = WindowInspector.getGlobalWindowViews()
        return views
    }
}
