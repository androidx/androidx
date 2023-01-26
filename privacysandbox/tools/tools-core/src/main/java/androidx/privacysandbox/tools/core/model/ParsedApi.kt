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

package androidx.privacysandbox.tools.core.model

/** Result of parsing a full developer-defined API for an SDK. */
data class ParsedApi(
    val services: Set<AnnotatedInterface>,
    val values: Set<AnnotatedValue> = emptySet(),
    val callbacks: Set<AnnotatedInterface> = emptySet(),
    val interfaces: Set<AnnotatedInterface> = emptySet(),
) {
    val valueMap = values.associateBy { it.type }
    val callbackMap = callbacks.associateBy { it.type }
    val interfaceMap = interfaces.associateBy { it.type }
}
