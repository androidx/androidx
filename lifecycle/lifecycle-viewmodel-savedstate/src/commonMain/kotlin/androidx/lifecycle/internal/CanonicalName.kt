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

package androidx.lifecycle.internal

import kotlin.reflect.KClass

/**
 * Multiplatform replacement for [KClass.qualifiedName] reflection API.
 *
 * This extension property provides the canonical name for a [KClass] instance across different
 * platforms, addressing the limitation where [KClass.qualifiedName] is not supported on all
 * platforms (e.g., Kotlin/JS).
 *
 * For platforms where [KClass.qualifiedName] is available, it will return that value. On platforms
 * like Kotlin/JS where `qualifiedName` is not yet supported, it falls back to [KClass.simpleName].
 *
 * @return The canonical name of the [KClass], or `null` if the [KClass] itself is `null` or its
 *   name cannot be determined.
 */
internal expect val KClass<*>?.canonicalName: String?
