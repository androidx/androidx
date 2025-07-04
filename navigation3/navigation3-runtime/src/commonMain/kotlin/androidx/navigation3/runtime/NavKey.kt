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

package androidx.navigation3.runtime

import kotlinx.serialization.Serializable

/**
 * Marker interface for keys.
 *
 * Objects and classes that extend this class must be marked with the [Serializable] annotation in
 * order to be saved with by the [rememberNavBackStack] function.
 *
 * This class is required because [Serializable] is only an annotation and does not provide a way to
 * link classes marked with the annotation together and provide a serializable that works with all
 * of them, resulting it making it impossible to properly save and restore.
 */
public interface NavKey
