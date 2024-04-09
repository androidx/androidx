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

package androidx.kruth

/**
 * Platform-specific propositions for [Comparable] typed subjects.
 *
 * @param T the type of the object being tested by this [ComparableSubject]
 */
internal actual interface PlatformComparableSubject<T : Comparable<T>>

internal actual class PlatformComparableSubjectImpl<T : Comparable<T>> actual constructor(
    actual: T?,
    metadata: FailureMetadata,
) : Subject<T>(actual, metadata, typeDescriptionOverride = null), PlatformComparableSubject<T>
