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

package androidx.compose.remote.creation.compose.test.util

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import kotlin.reflect.full.declaredMemberProperties

fun RemoteArrangement.Horizontal.propertyName(): String {
    return RemoteArrangement::class
        .declaredMemberProperties
        .firstOrNull { it.get(RemoteArrangement) == this }
        ?.name ?: "Unknown"
}

fun RemoteArrangement.Vertical.propertyName(): String {
    return RemoteArrangement::class
        .declaredMemberProperties
        .firstOrNull { it.get(RemoteArrangement) == this }
        ?.name ?: "Unknown"
}

fun RemoteAlignment.Horizontal.propertyName(): String {
    return RemoteAlignment::class
        .declaredMemberProperties
        .firstOrNull { it.get(RemoteAlignment) == this }
        ?.name ?: "Unknown"
}

fun RemoteAlignment.Vertical.propertyName(): String {
    return RemoteAlignment::class
        .declaredMemberProperties
        .firstOrNull { it.get(RemoteAlignment) == this }
        ?.name ?: "Unknown"
}
