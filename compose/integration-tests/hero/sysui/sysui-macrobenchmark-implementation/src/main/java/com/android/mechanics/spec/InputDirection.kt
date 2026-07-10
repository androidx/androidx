/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.mechanics.spec

/**
 * The intrinsic direction of the input value.
 *
 * It reflects the user's intent, that is its meant to be derived from a gesture. If the input is
 * driven by an animation, the direction is expected to not change.
 *
 * The directions are labelled [Min] and [Max] to reflect descending and ascending input values
 * respectively, but it does not imply an spatial direction.
 */
enum class InputDirection(val sign: Int) {
    Min(sign = -1),
    Max(sign = +1),
}
