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

package androidx.wear.compose.material3

import kotlin.math.PI

/** Converts the angle measured in degrees to an equivalent angle measured in radians. */
internal fun Float.toRadians() = this * PI.toFloat() / 180f

/** Converts the angle measured in radians to an equivalent angle measured in degrees. */
internal fun Float.toDegrees() = this * 180f / PI.toFloat()
