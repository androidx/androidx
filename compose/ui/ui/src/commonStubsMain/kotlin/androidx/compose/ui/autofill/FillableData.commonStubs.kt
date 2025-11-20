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

package androidx.compose.ui.autofill

import androidx.compose.ui.implementedInJetBrainsFork

/**
 * Creates a [FillableData] object from a [CharSequence].
 *
 * This is a stub implementation and will throw an exception if called when Autofill is not
 * supported on this platform.
 *
 * @param textValue The value to store in the [FillableData].
 */
actual fun FillableData.Companion.createFromText(textValue: CharSequence): FillableData? {
    implementedInJetBrainsFork()
}

/**
 * Creates a [FillableData] object from a [Boolean].
 *
 * This is a stub implementation and will throw an exception if called when Autofill is not
 * supported on this platform.
 *
 * @param booleanValue The value to store in the [FillableData].
 */
actual fun FillableData.Companion.createFromBoolean(booleanValue: Boolean): FillableData? {
    implementedInJetBrainsFork()
}

/**
 * Creates a [FillableData] object from an [Int].
 *
 * This is a stub implementation and will throw an exception if called when Autofill is not
 * supported on this platform.
 *
 * @param listIndexValue The value to store in the [FillableData].
 */
actual fun FillableData.Companion.createFromListIndex(listIndexValue: Int): FillableData? {
    implementedInJetBrainsFork()
}

/**
 * Creates a [FillableData] object from a [Long].
 *
 * This is a stub implementation and will throw an exception if called when Autofill is not
 * supported on this platform.
 *
 * @param dateMillisValue The value to store in the [FillableData].
 */
actual fun FillableData.Companion.createFromDateMillis(dateMillisValue: Long): FillableData? {
    implementedInJetBrainsFork()
}
