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
 * Denotes a [android.content.Context] that is tied to a [android.view.Display] and can be used to
 * obtain one via [android.content.Context.getDisplay]. Note: it is not considered to be a UI or
 * visual Context and **must not** be used to obtain UI-related services, such as
 * [android.view.WindowManager], [android.view.LayoutInflater] or [android.app.WallpaperManager] via
 * [android.content.Context.getSystemService]. If the UI services mentioned above are required,
 * instead please use Contexts which are marked as [UiContext].
 *
 * [android.app.Activity], Context instances created with
 * [android.content.Context.createWindowContext] or [android.content.Context.createDisplayContext]
 * can be used to get an associated [android.view.Display] instance.
 *
 * This is a marker annotation and has no specific attributes.
 *
 * @see android.content.Context.getDisplay
 * @see android.content.Context.getSystemService
 * @see android.content.Context.getSystemService
 * @see android.content.Context.createDisplayContext
 * @see android.content.Context.createWindowContext
 * @see UiContext
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
public annotation class DisplayContext
