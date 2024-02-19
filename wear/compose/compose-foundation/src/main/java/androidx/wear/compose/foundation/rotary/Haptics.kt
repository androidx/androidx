/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.rotary

/**
 * Handles haptics for rotary usage
 */
internal interface RotaryHapticHandler {

    /**
     * Handles haptics when scroll is used
     */
    fun handleScrollHaptic(event: UnifiedRotaryEvent)

    /**
     * Handles haptics when scroll with snap is used
     */
    fun handleSnapHaptic(event: UnifiedRotaryEvent)

    /**
     * Handles haptics when edge of the list is reached
     */
    fun handleLimitHaptic(event: UnifiedRotaryEvent, isStart: Boolean)
}
