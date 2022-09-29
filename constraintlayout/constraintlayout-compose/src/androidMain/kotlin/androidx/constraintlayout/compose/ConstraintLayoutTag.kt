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

package androidx.constraintlayout.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density

/**
 * Alternative to [androidx.compose.ui.layout.layoutId] that enables the use of [tag].
 *
 * @param layoutId The unique Id string assigned to the Composable
 * @param tag A string to represent a group of Composables that may be affected by a
 * ConstraintLayout function. Eg: The `Variables` block in a JSON5 based [ConstraintSet]
 */
fun Modifier.layoutId(layoutId: String, tag: String? = null): Modifier {
    if (tag == null) {
        // Fallback to androidx.compose.ui.layout.layoutId
        return this.layoutId(layoutId)
    } else {
        return this.then(
            ConstraintLayoutTag(
                constraintLayoutId = layoutId,
                constraintLayoutTag = tag,
                inspectorInfo = debugInspectorInfo {
                    name = "constraintLayoutId"
                    value = layoutId
                }
            )
        )
    }
}

@Immutable
private class ConstraintLayoutTag(
    override val constraintLayoutTag: String,
    override val constraintLayoutId: String,
    inspectorInfo: InspectorInfo.() -> Unit
) : ParentDataModifier, ConstraintLayoutTagParentData, InspectorValueInfo(inspectorInfo) {
    override fun Density.modifyParentData(parentData: Any?): Any? {
        return this@ConstraintLayoutTag
    }

    override fun hashCode(): Int =
        constraintLayoutTag.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? ConstraintLayoutTag ?: return false
        return constraintLayoutTag == otherModifier.constraintLayoutTag
    }

    override fun toString(): String =
        "ConstraintLayoutTag(id=$constraintLayoutTag)"
}

interface ConstraintLayoutTagParentData {
    val constraintLayoutId: String
    val constraintLayoutTag: String
}

val Measurable.constraintLayoutTag: Any?
    get() = (parentData as? ConstraintLayoutTagParentData)?.constraintLayoutTag

val Measurable.constraintLayoutId: Any?
    get() = (parentData as? ConstraintLayoutTagParentData)?.constraintLayoutId