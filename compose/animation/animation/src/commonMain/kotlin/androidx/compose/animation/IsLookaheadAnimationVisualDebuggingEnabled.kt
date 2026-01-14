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

package androidx.compose.animation

/**
 * True by default, but does not turn on animation visual debugging unless content is wrapped with
 * AnimationVisualDebugScope and isEnabled == true in AnimationVisualDebugGlobalConfig. When
 * compiling with R8, this is automatically set to false and all relevant code is stripped out.
 */
internal val isLookaheadAnimationVisualDebuggingEnabled: Boolean = true
