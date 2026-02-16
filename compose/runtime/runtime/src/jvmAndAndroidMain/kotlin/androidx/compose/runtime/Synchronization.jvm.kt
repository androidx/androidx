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

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // b/477633861

package androidx.compose.runtime.platform

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal actual typealias Monitor = Object

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun makeMonitor(ref: Any?) = if (ref == null) Monitor() else ref as Monitor

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal actual inline fun <R> synchronized(monitor: Monitor, block: () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return kotlin.synchronized(monitor, block)
}
