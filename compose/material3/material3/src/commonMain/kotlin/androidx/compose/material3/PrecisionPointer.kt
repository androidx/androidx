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

@file:JvmName("PrecisionPointer")

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlin.jvm.JvmName

/**
 * Whether components should have a denser spacing and sizing due to a precision pointer being
 * present.
 *
 * Note that this will always return `false` if the
 * [ComposeMaterial3Flags.isPrecisionPointerComponentSizingEnabled] flag is not enabled.
 *
 * @see ComposeMaterial3Flags.isPrecisionPointerComponentSizingEnabled
 */
fun shouldUsePrecisionPointerComponentSizing(): Boolean {
    return shouldUsePrecisionPointerComponentSizing.value
}

/**
 * A flag that represents whether a precision pointer is present, and thus whether components should
 * have denser spacing and sizing.
 *
 * Note that this value will always be `false` if the
 * [ComposeMaterial3Flags.isPrecisionPointerComponentSizingEnabled] flag is not enabled.
 */
internal val shouldUsePrecisionPointerComponentSizing: MutableState<Boolean> = mutableStateOf(false)

/**
 * Provides support for querying the device state for the presence of precision pointers via
 * [shouldUsePrecisionPointerComponentSizing].
 *
 * This function is re-entrant; if it is called multiple times in the same hierarchy, nothing
 * happens on subsequent invocations.
 */
@Composable
internal expect fun EnsurePrecisionPointerListenersRegistered(content: @Composable () -> Unit)
