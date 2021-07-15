/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.slice.builders

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.core.graphics.drawable.IconCompat
import androidx.slice.builders.ListBuilder.ICON_IMAGE

/**
 * @hide
 */
@RestrictTo(LIBRARY)
@DslMarker
annotation class SliceMarker

/**
 * Helper class annotated with @SliceMarker, which is annotated with @DslMarker.
 * Two implicit receivers that are annotated with @SliceMarker are not accessible in the same scope,
 * ensuring a type-safe DSL.
 */
@SliceMarker
class ListBuilderDsl(context: Context, uri: Uri, ttl: Long) : ListBuilder(context, uri, ttl)

/**
 * Helper class annotated with @SliceMarker, which is annotated with @DslMarker.
 * Two implicit receivers that are annotated with @SliceMarker are not accessible in the same scope,
 * ensuring a type-safe DSL.
 */
@SliceMarker
class RowBuilderDsl : ListBuilder.RowBuilder()

/**
 * Helper class annotated with @SliceMarker, which is annotated with @DslMarker.
 * Two implicit receivers that are annotated with @SliceMarker are not accessible in the same scope,
 * ensuring a type-safe DSL.
 */
@SliceMarker
class InputRangeBuilderDsl : ListBuilder.InputRangeBuilder()

/**
 * Helper class annotated with @SliceMarker, which is annotated with @DslMarker.
 * Two implicit receivers that are annotated with @SliceMarker are not accessible in the same scope,
 * ensuring a type-safe DSL.
 */
@SliceMarker
class RangeBuilderDsl : ListBuilder.RangeBuilder()

/**
 * Helper class annotated with @SliceMarker, which is annotated with @DslMarker.
 * Two implicit receivers that are annotated with @SliceMarker are not accessible in the same scope,
 * ensuring a type-safe DSL.
 */
@SliceMarker
class HeaderBuilderDsl : ListBuilder.HeaderBuilder()

/**
 * Reduces verbosity required to build a Slice in Kotlin.
 * Usage example:
 * <pre>
 * val slice = list(context = context, uri = testUri, ttl = ListBuilder.INFINITY) {
 *     gridRow {
 *         setPrimaryAction(action)
 *         cell {
 *             addTitleText("Title 1")
 *             addImage(createIcon(R.drawable.ic_android_black_24dp), ListBuilder.SMALL_IMAGE)
 *             addText("Text 1")
 *         }
 *         cell {
 *             addTitleText("Title 2")
 *             addImage(createIcon(R.drawable.ic_android_black_24dp), ListBuilder.SMALL_IMAGE)
 *             addText("Text 2")
 *         }
 *     }
 *     row {
 *         setTitle("Title")
 *         setSubtitle("Subtitle")
 *         setPrimaryAction(sliceAction)
 *     }
 * }
 * </pre>
 * @see ListBuilder.build
 */
inline fun list(
    context: Context,
    uri: Uri,
    ttl: Long,
    addRows: ListBuilderDsl.() -> Unit
) = ListBuilderDsl(context, uri, ttl).apply { addRows() }.build()

/**
 * @see ListBuilder.setHeader
 */
inline fun ListBuilderDsl.header(buildHeader: HeaderBuilderDsl.() -> Unit) =
    setHeader(HeaderBuilderDsl().apply { buildHeader() })

/**
 * @see ListBuilder.addGridRow
 */
inline fun ListBuilderDsl.gridRow(buildGrid: GridRowBuilderDsl.() -> Unit) =
    addGridRow(GridRowBuilderDsl().apply { buildGrid() })

/**
 * @see ListBuilder.addRow
 */
inline fun ListBuilderDsl.row(buildRow: RowBuilderDsl.() -> Unit) =
    addRow(RowBuilderDsl().apply { buildRow() })

/**
 * @see ListBuilder.setSeeMoreRow
 */
inline fun ListBuilderDsl.seeMoreRow(buildRow: RowBuilderDsl.() -> Unit) =
    setSeeMoreRow(RowBuilderDsl().apply { buildRow() })

/**
 * @see ListBuilder.addInputRange
 */
inline fun ListBuilderDsl.inputRange(buildInputRange: InputRangeBuilderDsl.() -> Unit) =
    addInputRange(InputRangeBuilderDsl().apply { buildInputRange() })

/**
 * @see ListBuilder.addRange
 */
inline fun ListBuilderDsl.range(buildRange: RangeBuilderDsl.() -> Unit) =
    addRange(RangeBuilderDsl().apply { buildRange() })

/**
 * Factory method to build a tappable [SliceAction].
 */
fun tapSliceAction(
    pendingIntent: PendingIntent,
    icon: IconCompat,
    @ListBuilder.ImageMode imageMode: Int = ICON_IMAGE,
    title: CharSequence
) = SliceAction(pendingIntent, icon, imageMode, title)

/**
 * Factory method to build a toggleable [SliceAction].
 */
fun toggleSliceAction(
    pendingIntent: PendingIntent,
    icon: IconCompat? = null,
    title: CharSequence,
    isChecked: Boolean
) = icon?.let { SliceAction(pendingIntent, it, title, isChecked) }
    ?: SliceAction(pendingIntent, title, isChecked)
