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

package androidx.navigationevent.internal

// https://github.com/JetBrains/compose-multiplatform-core/blob/aedff98279a9e778d13c67fc5c7bb1797e2f8a99/lifecycle/lifecycle-viewmodel/src/webMain/kotlin/androidx/lifecycle/viewmodel/internal/SynchronizedObject.web.kt#L18
internal actual class SynchronizedObject actual constructor()

// https://github.com/JetBrains/compose-multiplatform-core/blob/aedff98279a9e778d13c67fc5c7bb1797e2f8a99/lifecycle/lifecycle-viewmodel/src/webMain/kotlin/androidx/lifecycle/viewmodel/internal/SynchronizedObject.web.kt#L20
internal actual inline fun <T> synchronizedImpl(
    lock: SynchronizedObject,
    crossinline action: () -> T,
): T = action()
