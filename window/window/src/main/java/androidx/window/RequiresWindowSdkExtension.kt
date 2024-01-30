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

package androidx.window

import androidx.annotation.IntRange

// TODO(b/292738295): Provide lint checks for RequiresWindowSdkExtension
/**
 * Denotes that the annotated element must only be used if
 * [WindowSdkExtensions.extensionVersion] is greater than or equal to the given [version].
 * Please see code sample linked below for usages.
 *
 * Calling the API that requires a higher level than the device's current level may lead to
 * exceptions or unexpected results.
 *
 * @param version the minimum required [WindowSdkExtensions] version of the denoted target
 *
 * @sample androidx.window.samples.annotateRequiresWindowSdkExtension
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY,
)
annotation class RequiresWindowSdkExtension(
    /** The minimum required [WindowSdkExtensions] version of the denoted target */
    @IntRange(from = 1)
    val version: Int
)
