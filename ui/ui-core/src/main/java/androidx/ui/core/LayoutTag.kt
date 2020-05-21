/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import androidx.compose.Immutable
import androidx.compose.Stable
import androidx.ui.unit.Density

/**
 * Tag the element with [tag] to identify the element within its parent.
 */
@Stable
fun Modifier.tag(tag: Any) = this + LayoutTag(tag)

/**
 * A [ParentDataModifier] which tags the target with the given [tag]. The provided tag
 * will act as parent data, and can be used for example by parent layouts to associate
 * composable children to [Measurable]s when doing layout, as shown below.
 *
 * Example usage:
 * @sample androidx.ui.core.samples.LayoutTagChildrenUsage
 */
@Immutable
private data class LayoutTag(override val tag: Any) : ParentDataModifier,
LayoutTagParentData {
    override fun Density.modifyParentData(parentData: Any?): Any? {
        return this@LayoutTag
    }
}

/**
 * Can be implemented by values used as parent data to make them usable as tags.
 * If a parent data value implements this interface, it can then be returned when querying
 * [Measurable.tag] for the corresponding child.
 */
interface LayoutTagParentData {
    val tag: Any
}

/**
 * Retrieves the tag associated to a composable with the [LayoutTag] modifier.
 * For a parent data value to be returned by this property when not using the [LayoutTag]
 * modifier, the parent data value should implement the [LayoutTagParentData] interface.
 */
val Measurable.tag: Any?
    get() = (parentData as? LayoutTagParentData)?.tag
