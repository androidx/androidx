/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.preview

/**
 * The annotation to be used on a Glance composable for displaying visual previews in Android
 * Studio.
 *
 * The [widthDp] and [heightDp] parameters correspond to the size of the widget, i.e. the size
 * available in the LocalSize composition local. When [widthDp] and [heightDp] aren't specified, the
 * visual preview wraps its content. In this case, LocalSize should not be read within the
 * composable.
 *
 * @param widthDp width in DP that will be used when rendering the annotated Glance @[Composable]
 *   and that will be set as the widget's LocalSize width.
 * @param heightDp height in DP that will be used when rendering the annotated Glance @[Composable]
 *   and that will be set as the widget's LocalSize height.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@ExperimentalGlancePreviewApi
@Repeatable
annotation class Preview(val widthDp: Int = -1, val heightDp: Int = -1)
