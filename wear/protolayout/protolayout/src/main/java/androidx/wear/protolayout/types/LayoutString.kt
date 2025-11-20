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
@file:JvmName("LayoutStringUtil")

package androidx.wear.protolayout.types

import androidx.annotation.RestrictTo
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.TextAlignment
import androidx.wear.protolayout.TypeBuilders.StringLayoutConstraint
import androidx.wear.protolayout.TypeBuilders.StringProp
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.RequiresSchemaVersion
import java.util.Objects

/**
 * Static or dynamic string value for layout fields.
 *
 * This can be used on layout string fields with data binding support.
 */
class LayoutString
private constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val prop: StringProp,
    /**
     * When [dynamicValue] is used, [layoutConstraint] ensures that the text element has a known
     * fixed size during the layout pass, independently of the actual [dynamicValue] String. If not
     * set, the text element will size itself to the content of the [dynamicValue].
     */
    val layoutConstraint: StringLayoutConstraint? = null,
) {
    /**
     * The static value. If [dynamicValue] is not null this will be used as the default value for
     * when [dynamicValue] can't be resolved.
     */
    val staticValue: String = prop.value
    /** The dynamic value. If this can't be resolved [staticValue] will be used during rendering. */
    val dynamicValue: DynamicString? = prop.dynamicValue

    /** Creates an instance for a static String value. */
    constructor(staticValue: String) : this(StringProp.Builder(staticValue).build())

    /**
     * Creates an instance for a [DynamicString] value with a static value fallback and a set of
     * layout constraints for the dynamic value.
     *
     * @param staticValue the static value that can be used when the `dynamicValue` can't be
     *   resolved.
     * @param dynamicValue the dynamic value. If this value can be resolved, the `staticValue` won't
     *   be used.
     * @param layoutConstraint used to correctly measure layout Text element size and align text to
     *   ensure that the layout is of a known size during the layout pass regardless of the
     *   `dynamicValue` String.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    constructor(
        staticValue: String,
        dynamicValue: DynamicString,
        layoutConstraint: StringLayoutConstraint,
    ) : this(
        StringProp.Builder(staticValue).setDynamicValue(dynamicValue).build(),
        layoutConstraint,
    )

    /**
     * Creates an instance for a [DynamicString] value with a static value fallback. The text
     * element will size itself to the content of the [dynamicValue], as no [layoutConstraint] is
     * specified.
     *
     * @param staticValue the static value that can be used when the `dynamicValue` can't be
     *   resolved.
     * @param dynamicValue the dynamic value. If this value can be resolved, the `staticValue` won't
     *   be used.
     */
    @RequiresSchemaVersion(major = 1, minor = 600)
    constructor(
        staticValue: String,
        dynamicValue: DynamicString,
    ) : this(StringProp.Builder(staticValue).setDynamicValue(dynamicValue).build())

    override fun equals(other: Any?) =
        this === other ||
            (other is LayoutString &&
                prop == other.prop &&
                layoutConstraint == other.layoutConstraint)

    override fun hashCode() = Objects.hash(prop, layoutConstraint)

    override fun toString() = "LayoutString(prop=$prop, layoutConstraint=$layoutConstraint)"
}

/** Extension for creating a [LayoutString] from a String. */
val String.layoutString: LayoutString
    @JvmName("createLayoutString") get() = LayoutString(this)

/**
 * Extension for creating a [LayoutString] from a [DynamicString]
 *
 * @param staticValue the static value that can be used when the `dynamicValue` can't be resolved.
 * @param layoutConstraint used to correctly measure layout Text element size and align text to
 *   ensure that the layout is of a known size during the layout pass regardless of the
 *   `dynamicValue` String.
 */
@JvmName("createLayoutString")
@RequiresSchemaVersion(major = 1, minor = 200)
fun DynamicString.asLayoutString(staticValue: String, layoutConstraint: StringLayoutConstraint) =
    LayoutString(staticValue, this, layoutConstraint)

/**
 * Extension for creating a [LayoutString] from a [DynamicString]
 *
 * @param staticValue the static value that can be used when the `dynamicValue` can't be resolved.
 */
@JvmName("createLayoutString")
@RequiresSchemaVersion(major = 1, minor = 600)
fun DynamicString.asLayoutString(staticValue: String) = LayoutString(staticValue, this)

/**
 * Specifies layout constraints for to use for layout measurement in presence of dynamic values.
 *
 * @param longestPattern the text string to use as the pattern for the (graphically) longest text
 *   that can be laid out. Used to ensure that the layout is of a known size during the layout pass.
 * @param alignment the alignment of the actual text within the space reserved by `longestPattern`
 */
@JvmOverloads
@RequiresSchemaVersion(major = 1, minor = 200)
fun stringLayoutConstraint(
    longestPattern: String,
    @TextAlignment alignment: Int = TEXT_ALIGN_CENTER,
) = StringLayoutConstraint.Builder(longestPattern).setAlignment(alignment).build()

/**
 * Extension for creating a [StringLayoutConstraint] from a String. `this` will be used as
 * `longestPattern`.
 *
 * @param alignment the alignment of the actual text within the space reserved by `longestPattern`
 */
@RequiresSchemaVersion(major = 1, minor = 200)
fun String.asLayoutConstraint(@TextAlignment alignment: Int = TEXT_ALIGN_CENTER) =
    stringLayoutConstraint(this, alignment)
