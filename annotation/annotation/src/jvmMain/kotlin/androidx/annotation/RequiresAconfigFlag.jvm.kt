/*
 * Copyright 2025 The Android Open Source Project
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
 * Indicates an API is part of a feature that is guarded by an aconfig flag, and only available if
 * the flag is enabled.
 *
 * Unless the API has been finalized and has become part of the SDK, callers of the annotated API
 * must check that the flag is enabled before making any assumptions about the existence of the API.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FIELD,
    AnnotationTarget.FILE,
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // Flags are only supported internally for now.
public annotation class RequiresAconfigFlag(
    /**
     * The string value for the aconfig flag used to guard the feature this API is part of, for
     * example `"android.os.flags.my_feature"`.
     */
    val value: String
)
