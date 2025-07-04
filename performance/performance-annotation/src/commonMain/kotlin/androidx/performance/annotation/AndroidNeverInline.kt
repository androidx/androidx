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

@file:OptIn(ExperimentalMultiplatform::class)

package androidx.performance.annotation

/**
 * Indicates that an API should never be inlined by ART on Android.
 *
 * [AndroidNeverInline] can be used to annotate methods that should not be inlined into other
 * methods. Methods that are not called frequently, are never speed-critical, or are only used for
 * debugging do not necessarily need to run quickly. Applying this annotation to prevent these
 * methods from being inlined will return some size improvements in .odex files.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION)
@OptionalExpectation
public expect annotation class AndroidNeverInline()
