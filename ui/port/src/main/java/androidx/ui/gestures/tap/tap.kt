/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.tap

// / Signature for when a pointer that might cause a tap has contacted the
// / screen.
// /
// / The position at which the pointer contacted the screen is available in the
// / `details`.
typealias GestureTapDownCallback = (details: TapDownDetails) -> Unit

// / Signature for when a pointer that will trigger a tap has stopped contacting
// / the screen.
// /
// / The position at which the pointer stopped contacting the screen is available
// / in the `details`.
typealias GestureTapUpCallback = (details: TapUpDetails) -> Unit

// / Signature for when a tap has occurred.
typealias GestureTapCallback = () -> Unit

// / Signature for when the pointer that previously triggered a
// / [GestureTapDownCallback] will not end up causing a tap.
typealias GestureTapCancelCallback = () -> Unit