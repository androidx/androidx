/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")// // ///
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

package androidx.ui.gestures.events

// /// The bit of [PointerEvent.buttons] that corresponds to the primary mouse button.
// ///
// /// The primary mouse button is typically the left button on the top of the
// /// mouse but can be reconfigured to be a different physical button.
const val PRIMARY_MOUSE_BUTTON = 0x01

// /// The bit of [PointerEvent.buttons] that corresponds to the secondary mouse button.
// ///
// /// The secondary mouse button is typically the right button on the top of the
// /// mouse but can be reconfigured to be a different physical button.
const val SECONDARY_MOUSE_BUTTON = 0x02

// /// The bit of [PointerEvent.buttons] that corresponds to the primary stylus button.
// ///
// /// The primary stylus button is typically the top of the stylus and near the
// /// tip but can be reconfigured to be a different physical button.
const val PRIMARY_STYLUS_BUTTON = 0x02

// /// The bit of [PointerEvent.buttons] that corresponds to the middle mouse button.
// ///
// /// The middle mouse button is typically between the left and right buttons on
// /// the top of the mouse but can be reconfigured to be a different physical
// /// button.
const val MIDDLE_MOUSE_BUTTON = 0x04

// /// The bit of [PointerEvent.buttons] that corresponds to the secondary stylus button.
// ///
// /// The secondary stylus button is typically on the end of the stylus farthest
// /// from the tip but can be reconfigured to be a different physical button.
const val SECONDARY_STYLUS_BUTTON = 0x04

// /// The bit of [PointerEvent.buttons] that corresponds to the back mouse button.
// ///
// /// The back mouse button is typically on the left side of the mouse but can be
// /// reconfigured to be a different physical button.
const val BACK_MOUSE_BUTTON = 0x08

// /// The bit of [PointerEvent.buttons] that corresponds to the forward mouse button.
// ///
// /// The forward mouse button is typically on the right side of the mouse but can
// /// be reconfigured to be a different physical button.
const val FORWARD_MOUSE_BUTTON = 0x10

// /// The bit of [PointerEvent.buttons] that corresponds to the nth mouse button.
// ///
// /// The number argument can be at most 32.
// ///
// /// See [PRIMARY_MOUSE_BUTTON], [SECONDARY_MOUSE_BUTTON], [MIDDLE_MOUSE_BUTTON],
// /// [BACK_MOUSE_BUTTON], and [FORWARD_MOUSE_BUTTON] for semantic names for some
// /// mouse buttons.
fun nthMouseButton(number: Int): Int = PRIMARY_MOUSE_BUTTON shl (number - 1)

// /// The bit of [PointerEvent.buttons] that corresponds to the nth stylus button.
// ///
// /// The number argument can be at most 32.
// ///
// /// See [PRIMARY_STYLUS_BUTTON] and [SECONDARY_STYLUS_BUTTON] for semantic names
// /// for some stylus buttons.
fun nthStylusButton(number: Int): Int = PRIMARY_STYLUS_BUTTON shl (number - 1)