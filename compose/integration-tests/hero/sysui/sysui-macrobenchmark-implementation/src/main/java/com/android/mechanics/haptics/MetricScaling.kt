/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.haptics

import androidx.compose.ui.unit.Density
import kotlin.jvm.JvmInline
import kotlin.math.abs

private const val PIXEL_INCH_CONVERSION = 25.4f / (160f * 1000)

fun Density.pxToMeters(pxValue: Float): Meters = Meters(pxValue * (PIXEL_INCH_CONVERSION / density))

fun Density.pxPerSecToMetersPerSec(pxValue: Float): MetersPerSec =
    MetersPerSec(pxValue * (PIXEL_INCH_CONVERSION / density))

@JvmInline
value class Meters(val value: Float) {
    fun absoluteValue(): MetersPerSec = MetersPerSec(abs(value))

    operator fun minus(other: Meters) = Meters(value - other.value)
}

@JvmInline
value class MetersPerSec(val value: Float) {
    fun absoluteValue(): MetersPerSec = MetersPerSec(abs(value))

    operator fun div(other: MetersPerSec): MetersPerSec = MetersPerSec(value / other.value)
}
