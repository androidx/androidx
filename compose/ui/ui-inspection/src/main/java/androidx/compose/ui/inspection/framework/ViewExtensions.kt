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

package androidx.compose.ui.inspection.framework

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.ui.R
import androidx.compose.ui.inspection.util.ThreadUtils
import java.util.Collections
import java.util.WeakHashMap

fun ViewGroup.getChildren(): List<View> {
    ThreadUtils.assertOnMainThread()
    return (0 until childCount).map { i -> getChildAt(i) }
}

fun View.ancestors(): Sequence<View> =
    generateSequence(this) { it.parent as? View }

fun View.isRoot(): Boolean =
    parent as? View == null

/**
 * Return a list of this view and all its children in depth-first order
 */
fun View.flatten(): Sequence<View> {
    ThreadUtils.assertOnMainThread()

    val remaining = mutableListOf(this)
    return generateSequence {
        val next = remaining.removeLastOrNull()
        if (next is ViewGroup) {
            remaining.addAll(next.getChildren())
        }
        next
    }
}

/**
 * Returns true if this view represents a special type that bridges between the legacy UI
 * framework and Jetpack Compose.
 *
 * Note: AndroidComposeView lives in compose.ui but is internal, which is why we need to check
 * indirectly like this. TODO(b/177998085): Expose this class to our library.
 */
fun View.isAndroidComposeView(): Boolean {
    return javaClass.canonicalName == "androidx.compose.ui.platform.AndroidComposeView"
}

/**
 * Return true if this view already has a slot table.
 */
val View.hasSlotTable: Boolean
    get() = getTag(R.id.inspection_slot_table_set) is Set<*>

/**
 * Adds a slot table to this view. Return 1 if added, 0 if not.
 */
fun View.addSlotTable(): Int {
    if (hasSlotTable) {
        return 0
    }
    setTag(
        R.id.inspection_slot_table_set,
        Collections.newSetFromMap(WeakHashMap<CompositionData, Boolean>())
    )
    return 1
}
