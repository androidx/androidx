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

package androidx.compose.material3.adaptive

import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

/**
 * Calculates and returns [WindowAdaptiveInfo] of the provided context. It's a convenient function
 * that uses the default [WindowSizeClass] constructor and the default [Posture] calculation
 * functions to retrieve [WindowSizeClass] and [Posture].
 *
 * @return [WindowAdaptiveInfo] of the provided context
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
expect fun currentWindowAdaptiveInfo(): WindowAdaptiveInfo
