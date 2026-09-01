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

package androidx.savedstate

import androidx.savedstate.serialization.platformEncodeDecode

/**
 * Runs all of [SavedStateTest] with every state under test additionally routed through the
 * platform's parceling and unparceling logic, to simulate real-world behavior.
 */
internal class ParceledSavedStateTest : SavedStateTest() {
    override fun postProcessCreated(savedState: SavedState): SavedState =
        platformEncodeDecode(savedState)
}
