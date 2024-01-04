/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.tiles.tooling.preview

import android.content.Context
import androidx.annotation.FloatRange
import androidx.wear.tooling.preview.devices.WearDevice
import androidx.wear.tooling.preview.devices.WearDevices

/**
 * The annotation that marks Tile preview components that should have a visual preview in the
 * Android Studio preview panel. Tile preview components are methods that take an optional [Context]
 * parameter and return a [TilePreviewData]. Methods annotated with [TilePreview] must be top level
 * declarations or in a top level class with a default constructor.
 *
 * For example:
 * ```kotlin
 * @TilePreview
 * fun myTilePreview(): TilePreviewData {
 *     return TilePreviewData { request -> myTile(request) }
 * }
 * ```
 * or:
 * ```kotlin
 * @TilePreview
 * fun myTilePreview(context: Context): TilePreviewData {
 *     return TilePreviewData { request -> myTile(request, context) }
 * }
 * ```
 *
 * Because of the way previews are rendered within Android Studio, they are lightweight and don't
 * require the whole Android framework to render them. However, this comes with the following
 * limitations:
 * * No network access
 * * No file access
 * * Some [Context] APIs may not be fully available, such as launching activities or retrieving
 * services
 *
 * For more information, see
 * https://developer.android.com/jetpack/compose/tooling/previews#preview-limitations
 *
 * The annotation contains a number of parameters that allow to define the way the Tile will be
 * rendered within the preview. The passed parameters are only read by Studio when rendering the
 * preview.
 *
 * @param name Display name of this preview allowing to identify it in the panel.
 * @param group Group name for this @[TilePreview]. This allows grouping them in the UI and
 * displaying only one or more of them.
 * @param locale Current user preference for the locale, corresponding to
 * [locale](https://d.android.com/guide/topics/resources/providing-resources.html#LocaleQualifier)
 * resource qualifier. By default, the `default` folder will be used.
 * @param device Device identifier indicating the device to use in the preview. For example
 * "id:wearos_small_round".See the available devices in [WearDevices].
 * @param fontScale User preference for the linear scaling factor for fonts, relative to the base
 * density scaling.
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@Repeatable
@MustBeDocumented
annotation class TilePreview(
    val name: String = "",
    val group: String = "",
    val locale: String = "",
    @WearDevice val device: String = WearDevices.SMALL_ROUND,
    @FloatRange(from = 0.01) val fontScale: Float = 1f,
)
