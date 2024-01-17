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
package androidx.annotation

/**
 * Denotes a [android.content.Context] that **can not** be used to obtain a [android.view.Display]
 * via [android.content.Context.getDisplay] nor to obtain an instance of a visual service, such a
 * [android.view.WindowManager], [android.view.LayoutInflater] or [android.app.WallpaperManager] via
 * [android.content.Context.getSystemService].
 *
 * This is a marker annotation and has no specific attributes.
 *
 * @see android.content.Context.getDisplay
 * @see android.content.Context.getSystemService
 * @see android.content.Context.getSystemService
 * @see UiContext
 * @see DisplayContext
 */
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD
)
public annotation class NonUiContext
