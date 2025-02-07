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
import androidx.wear.protolayout.ModifiersBuilders.SEMANTICS_ROLE_NONE
import androidx.wear.protolayout.ModifiersBuilders.Semantics
import androidx.wear.protolayout.ModifiersBuilders.SemanticsRole
import androidx.wear.protolayout.TypeBuilders.StringProp
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.RequiresSchemaVersion
import java.util.Objects

/**
 * Adds content description to be read by accessibility services.
 *
 * @param staticValue The static content description. This value will be used if [dynamicValue] is
 *   null, or if can't be resolved.
 * @param dynamicValue The dynamic content description. This is useful when content of the element
 *   itself is dynamic.
 */
@SuppressLint("ProtoLayoutMinSchema") // 1.2 schema only used when dynamicValue is non-null
fun LayoutModifier.contentDescription(
    staticValue: String,
    @RequiresSchemaVersion(major = 1, minor = 200) dynamicValue: DynamicString? = null
): LayoutModifier =
    this then
        BaseSemanticElement(
            contentDescription =
                StringProp.Builder(staticValue)
                    .apply { dynamicValue?.let { setDynamicValue(it) } }
                    .build()
        )

/**
 * Adds the semantic role of user interface element. Accessibility services might use this to
 * describe the element or do customizations.
 */
fun LayoutModifier.semanticsRole(@SemanticsRole semanticsRole: Int): LayoutModifier =
    this then BaseSemanticElement(semanticsRole = semanticsRole)

internal class BaseSemanticElement(
    val contentDescription: StringProp? = null,
    @SemanticsRole val semanticsRole: Int = SEMANTICS_ROLE_NONE
) : LayoutModifier.Element {
    @SuppressLint("ProtoLayoutMinSchema")
    fun mergeTo(initial: Semantics.Builder?): Semantics.Builder =
        (initial ?: Semantics.Builder()).apply {
            contentDescription?.let { setContentDescription(it) }
            if (semanticsRole != SEMANTICS_ROLE_NONE) {
                setRole(semanticsRole)
            }
        }

    override fun equals(other: Any?): Boolean =
        other is BaseSemanticElement &&
            contentDescription == other.contentDescription &&
            semanticsRole == other.semanticsRole

    override fun hashCode(): Int = Objects.hash(contentDescription, semanticsRole)

    override fun toString(): String =
        "BaseSemanticElement[contentDescription=$contentDescription, semanticRole=$semanticsRole"
}
