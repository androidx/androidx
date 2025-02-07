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

package androidx.wear.protolayout.modifiers

import android.annotation.SuppressLint
import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.wear.protolayout.ActionBuilders.Action
import androidx.wear.protolayout.ActionBuilders.LoadAction
import androidx.wear.protolayout.ActionBuilders.actionFromProto
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.StateBuilders.State
import androidx.wear.protolayout.expression.DynamicDataMap
import androidx.wear.protolayout.expression.RequiresSchemaVersion
import androidx.wear.protolayout.types.dp

/**
 * Adds the clickable property of the modified element. It allows the modified element to have
 * actions associated with it, which will be executed when the element is tapped.
 *
 * @param action is triggered whenever the element is tapped. By default adds an empty [LoadAction].
 * @param id is the associated identifier for this clickable. This will be passed to the action
 *   handler.
 */
fun LayoutModifier.clickable(action: Action = loadAction(), id: String? = null): LayoutModifier =
    this then BaseClickableElement(action = action, id = id)

/**
 * Creates a [Clickable] that allows the modified element to have actions associated with it, which
 * will be executed when the element is tapped.
 *
 * @param action is triggered whenever the element is tapped. By default adds an empty [LoadAction].
 * @param id is the associated identifier for this clickable. This will be passed to the action
 *   handler.
 * @param minClickableWidth of the clickable area. The default value is 48dp, following the Material
 *   design accessibility guideline. Note that this value does not affect the layout, so the minimum
 *   clickable width is not guaranteed unless there is enough space around the element within its
 *   parent bounds.
 * @param minClickableHeight of the clickable area. The default value is 48dp, following the
 *   Material design accessibility guideline. Note that this value does not affect the layout, so
 *   the minimum clickable height is not guaranteed unless there is enough space around the element
 *   within its parent bounds.
 */
@SuppressLint("ProtoLayoutMinSchema")
@JvmOverloads
fun clickable(
    action: Action = loadAction(),
    id: String? = null,
    @RequiresSchemaVersion(major = 1, minor = 300)
    @Dimension(DP)
    minClickableWidth: Float = Float.NaN,
    @RequiresSchemaVersion(major = 1, minor = 300)
    @Dimension(DP)
    minClickableHeight: Float = Float.NaN
): Clickable =
    Clickable.Builder()
        .setOnClick(action)
        .apply {
            id?.let { setId(it) }
            if (!minClickableWidth.isNaN()) setMinimumClickableWidth(minClickableWidth.dp)
            if (!minClickableHeight.isNaN()) setMinimumClickableHeight(minClickableHeight.dp)
        }
        .build()

/**
 * Adds the clickable property of the modified element. It allows the modified element to have
 * actions associated with it, which will be executed when the element is tapped.
 */
fun LayoutModifier.clickable(clickable: Clickable): LayoutModifier =
    this then
        BaseClickableElement(
            action =
                clickable.onClick?.let {
                    actionFromProto(it.toActionProto(), checkNotNull(clickable.fingerprint))
                },
            id = clickable.id,
            minClickableWidth = clickable.minimumClickableWidth.value,
            minClickableHeight = clickable.minimumClickableHeight.value
        )

/**
 * Creates an action used to load (or reload) the layout contents.
 *
 * @param requestedStateMap is the state associated with this action. This state will be passed to
 *   the action handler.
 */
@SuppressLint("ProtoLayoutMinSchema")
fun loadAction(
    @RequiresSchemaVersion(major = 1, minor = 200) requestedStateMap: DynamicDataMap? = null
): LoadAction =
    LoadAction.Builder()
        .apply {
            requestedStateMap?.let { setRequestState(State.Builder().setStateMap(it).build()) }
        }
        .build()

/**
 * Sets the minimum width and height of the clickable area. The default value is 48dp, following the
 * Material design accessibility guideline. Note that this value does not affect the layout, so the
 * minimum clickable width/height is not guaranteed unless there is enough space around the element
 * within its parent bounds.
 */
@RequiresSchemaVersion(major = 1, minor = 300)
fun LayoutModifier.minimumTouchTargetSize(
    @Dimension(DP) minWidth: Float,
    @Dimension(DP) minHeight: Float
): LayoutModifier =
    this then BaseClickableElement(minClickableWidth = minWidth, minClickableHeight = minHeight)

internal class BaseClickableElement(
    val action: Action? = null,
    val id: String? = null,
    @Dimension(DP) val minClickableWidth: Float = Float.NaN,
    @Dimension(DP) val minClickableHeight: Float = Float.NaN,
) : LayoutModifier.Element {
    @SuppressLint("ProtoLayoutMinSchema")
    fun mergeTo(initial: Clickable.Builder?): Clickable.Builder =
        (initial ?: Clickable.Builder()).apply {
            if (!id.isNullOrEmpty()) setId(id)
            action?.let { setOnClick(it) }
            if (!minClickableWidth.isNaN()) setMinimumClickableWidth(minClickableWidth.dp)
            if (!minClickableHeight.isNaN()) setMinimumClickableHeight(minClickableHeight.dp)
        }
}
