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

package androidx.core.view.accessibility

/**
 * Denotes that the annotated API is part of a preview accessibility feature surface that is subject
 * to change or removal in future releases.
 *
 * <p>These APIs are introduced to validate new features and gather developer feedback. Developers
 * opting in should expect potential breaking changes or API promotion paths in subsequent library
 * versions.
 */
@MustBeDocumented
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    message =
        "This is a preview accessibility API. It may be changed or removed in future releases."
)
public annotation class PreviewAccessibilityApi
