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

package androidx.wear.protolayout.testing

import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Spannable
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TypeBuilders.StringProp
import androidx.wear.protolayout.expression.DynamicDataMap

// TODO b/372916396 - Dealing with Arc container and ArcLayoutElements
internal val LayoutElement.modifiers: ModifiersBuilders.Modifiers?
    get() =
        when (this) {
            is Box -> modifiers
            is Row -> modifiers
            is Column -> modifiers
            is Spacer -> modifiers
            is Text -> modifiers
            is Image -> modifiers
            is Arc -> modifiers
            is Spannable -> modifiers
            else -> null
        }

internal val LayoutElement.color: ColorBuilders.ColorProp?
    get() =
        when (this) {
            is Text -> fontStyle?.color
            is Image -> colorFilter?.tint
            else -> modifiers?.background?.color
        }

internal val LayoutElement.contentDescription: StringProp?
    get() = modifiers?.semantics?.contentDescription

internal val LayoutElement.tag: ByteArray?
    get() = modifiers?.metadata?.tagData

internal fun LayoutElement.getText(injectedData: DynamicDataMap? = null): String? {
    val dynamicValue = (this as? Text)?.text?.dynamicValue
    val staticValue = (this as? Text)?.text?.value
    // Falls back to  use the static text when the dynamic data evaluation fails due to lack of
    // data in the pipeline.
    return dynamicValue?.evaluate(injectedData) ?: staticValue
}

internal fun LayoutElement.getContentDescription(injectedData: DynamicDataMap? = null): String? {
    val dynamicValue = this.contentDescription?.dynamicValue
    val staticValue = this.contentDescription?.value
    // Falls back to  use the static text when the dynamic data evaluation fails due to lack of
    // data in the pipeline.
    return dynamicValue?.evaluate(injectedData) ?: staticValue
}

internal val LayoutElement.children: List<LayoutElement>
    get() =
        when (this) {
            is Box -> contents
            is Row -> contents
            is Column -> contents
            else -> emptyList()
        }
