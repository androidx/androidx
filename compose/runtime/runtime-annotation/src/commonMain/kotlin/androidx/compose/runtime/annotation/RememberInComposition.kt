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

package androidx.compose.runtime.annotation

/**
 * RememberInComposition is used by an associated lint check to enforce that a marked constructor,
 * function, or property getter is not called directly within composition. This should be used to
 * mark APIs that:
 * - Return stateful / mutable objects that should be reused across compositions
 * - Return objects whose identity is important to maintain across compositions, for example
 *   lightweight objects used as 'keys' for other APIs
 * - Return objects that are expensive to instantiate, and should be remembered for performance
 *   reasons
 *
 * Note that the lint check will recommend that users either remember this call, or move it outside
 * composition. As a result this should not be used to mark APIs that are intended to be used inside
 * a side effect, as these should not be called inside a remember block.
 */
@MustBeDocumented
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
public annotation class RememberInComposition
