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

package androidx.compose.ui.test

import kotlin.jvm.JvmInline

// TODO: https://youtrack.jetbrains.com/issue/CMP-9904/Implement-TrackpadButton-actual
@JvmInline
actual value class TrackpadButton actual constructor(actual val buttonId: Int) {
    actual companion object {
        actual val Primary: TrackpadButton
            get() = TODO("Not yet implemented")
        actual val Secondary: TrackpadButton
            get() = TODO("Not yet implemented")
        actual val Tertiary: TrackpadButton
            get() = TODO("Not yet implemented")
    }
}