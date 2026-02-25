/*
 * Copyright 2026 The Android Open Source Project
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
@file:JvmName("ContainersKt")

package androidx.wear.protolayout.layout

import android.annotation.SuppressLint
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension
import androidx.wear.protolayout.DimensionBuilders.HorizontalLayoutConstraint
import androidx.wear.protolayout.DimensionBuilders.SpacerDimension
import androidx.wear.protolayout.DimensionBuilders.VerticalLayoutConstraint
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_UNDEFINED
import androidx.wear.protolayout.LayoutElementBuilders.HorizontalAlignment
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_UNDEFINED
import androidx.wear.protolayout.LayoutElementBuilders.VerticalAlignment
import androidx.wear.protolayout.expression.RequiresSchemaVersion
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers

/**
 * Creates a container which stacks all of its children on top of one another. This also allows to
 * add a background color, or to have a border around them with some padding.
 *
 * @param width The width of the box. Defaults to wrap.
 * @param height The height of the box. Defaults to wrap.
 * @param modifier Modifiers to set to this element
 * @param horizontalAlignment The horizontal alignment of the element inside this box. Defaults to
 *   center aligned.
 * @param verticalAlignment The vertical alignment of the element inside this box. Defaults to
 *   center aligned.
 * @param contents The list of child elements to place inside this container.
 */
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
public fun box(
    vararg contents: LayoutElement,
    width: ContainerDimension? = null,
    height: ContainerDimension? = null,
    modifier: LayoutModifier? = null,
    @HorizontalAlignment horizontalAlignment: Int = HORIZONTAL_ALIGN_UNDEFINED,
    @VerticalAlignment verticalAlignment: Int = VERTICAL_ALIGN_UNDEFINED,
): Box =
    Box.Builder()
        .apply {
            width?.let { setWidth(it) }
            height?.let { setHeight(it) }
            modifier?.let { setModifiers(it.toProtoLayoutModifiers()) }
            if (horizontalAlignment != HORIZONTAL_ALIGN_UNDEFINED) {
                setHorizontalAlignment(horizontalAlignment)
            }
            if (verticalAlignment != VERTICAL_ALIGN_UNDEFINED) {
                setVerticalAlignment(verticalAlignment)
            }
            contents.forEach { addContent(it) }
        }
        .build()

/**
 * Creates a column of elements. Each child element will be laid out vertically, one after another
 * (i.e. stacking down). This element will size itself to the smallest size required to hold all of
 * its children (e.g. if it contains three elements sized 10x10, 20x20 and 30x30, the resulting
 * column will be 30x60).
 *
 * <p>If specified, horizontal_alignment can be used to control the gravity inside the container,
 * affecting the horizontal placement of children whose width are smaller than the resulting column
 * width.
 *
 * @param width The width of the box. Defaults to wrap.
 * @param height The height of the box. Defaults to wrap.
 * @param modifier Modifiers to set to this element
 * @param horizontalAlignment The horizontal alignment of the element inside this box. Defaults to
 *   center aligned.
 * @param contents The list of child elements to place inside this container.
 */
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
public fun column(
    vararg contents: LayoutElement,
    width: ContainerDimension? = null,
    height: ContainerDimension? = null,
    modifier: LayoutModifier? = null,
    @HorizontalAlignment horizontalAlignment: Int = HORIZONTAL_ALIGN_UNDEFINED,
): Column =
    Column.Builder()
        .apply {
            width?.let { setWidth(it) }
            height?.let { setHeight(it) }
            modifier?.let { setModifiers(it.toProtoLayoutModifiers()) }
            if (horizontalAlignment != HORIZONTAL_ALIGN_UNDEFINED) {
                setHorizontalAlignment(horizontalAlignment)
            }
            contents.forEach { addContent(it) }
        }
        .build()

/**
 * Creates a row of elements. Each child will be laid out horizontally, one after another (i.e.
 * stacking to the right). This element will size itself to the smallest size required to hold all
 * of its children (e.g. if it contains three elements sized 10x10, 20x20 and 30x30, the resulting
 * row will be 60x30).
 *
 * If specified, [verticalAlignment] can be used to control the gravity inside the container,
 * affecting the vertical placement of children whose width are smaller than the resulting row
 * height.
 *
 * @param width The width of the box. Defaults to wrap.
 * @param height The height of the box. Defaults to wrap.
 * @param modifier Modifiers to set to this element
 * @param verticalAlignment The vertical alignment of the element inside this box. Defaults to
 *   center aligned.
 * @param contents The list of child elements to place inside this container.
 */
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
public fun row(
    vararg contents: LayoutElement,
    width: ContainerDimension? = null,
    height: ContainerDimension? = null,
    modifier: LayoutModifier? = null,
    @VerticalAlignment verticalAlignment: Int = VERTICAL_ALIGN_UNDEFINED,
): Row =
    Row.Builder()
        .apply {
            width?.let { setWidth(it) }
            height?.let { setHeight(it) }
            modifier?.let { setModifiers(it.toProtoLayoutModifiers()) }
            if (verticalAlignment != VERTICAL_ALIGN_UNDEFINED) {
                setVerticalAlignment(verticalAlignment)
            }
            contents.forEach { addContent(it) }
        }
        .build()

/**
 * Creates a simple spacer, typically used to provide padding between adjacent elements.
 *
 * @param width The width of the box. Defaults to wrap.
 * @param height The height of the box. Defaults to wrap.
 * @param modifier Modifiers to set to this element
 * @param horizontalLayoutConstraint The bounding constraints for the layout affected by the dynamic
 *   value from [width]. If the [SpacerDimension] for [width] does not have a dynamic value, this
 *   will be ignored.
 * @param verticalLayoutConstraint The bounding constraints for the layout affected by the dynamic
 *   value from [height]. If the [SpacerDimension] for [height] does not have a dynamic value, this
 *   will be ignored.
 */
@SuppressLint("ProtoLayoutMinSchema")
@Suppress("MissingJvmstatic") // Kotlin-friendly version of already available Java Apis
public fun spacer(
    width: SpacerDimension? = null,
    height: SpacerDimension? = null,
    modifier: LayoutModifier? = null,
    @RequiresSchemaVersion(major = 1, minor = 200)
    horizontalLayoutConstraint: HorizontalLayoutConstraint? = null,
    @RequiresSchemaVersion(major = 1, minor = 200)
    verticalLayoutConstraint: VerticalLayoutConstraint? = null,
): Spacer =
    Spacer.Builder()
        .apply {
            width?.let { setWidth(it) }
            height?.let { setHeight(it) }
            modifier?.let { setModifiers(it.toProtoLayoutModifiers()) }
            horizontalLayoutConstraint?.let { setLayoutConstraintsForDynamicWidth(it) }
            verticalLayoutConstraint?.let { setLayoutConstraintsForDynamicHeight(it) }
        }
        .build()
