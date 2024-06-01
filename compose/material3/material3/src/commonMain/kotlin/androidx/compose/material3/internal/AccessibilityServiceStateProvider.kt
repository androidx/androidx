/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Returns the state of whether any accessibility services are enabled.
 *
 * @param listenToTouchExplorationState whether to track the enabled/disabled state of touch
 *   exploration (i.e. TalkBack)
 * @param listenToSwitchAccessState whether to track the enabled/disabled state of Switch Access
 */
@Composable
internal expect fun rememberAccessibilityServiceState(
    listenToTouchExplorationState: Boolean = true,
    listenToSwitchAccessState: Boolean = true,
): State<Boolean>
