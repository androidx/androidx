/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.annotation.SuppressLint
import android.util.Range
import android.util.Rational
import androidx.camera.core.ExposureState

/** Immutable adaptor to the ExposureState interface. */
@SuppressLint("UnsafeOptInUsageError")
public data class EvCompValue(
    private val supported: Boolean,
    private val index: Int,
    private val range: Range<Int>,
    private val step: Rational
) : ExposureState {
    override fun getExposureCompensationIndex(): Int = index

    override fun getExposureCompensationRange(): Range<Int> = range

    override fun getExposureCompensationStep(): Rational = step

    override fun isExposureCompensationSupported(): Boolean = supported

    internal fun updateIndex(newIndex: Int): EvCompValue {
        return copy(index = newIndex)
    }
}
