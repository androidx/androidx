/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.testing

import android.util.Range
import android.util.Rational
import androidx.camera.camera2.pipe.integration.compat.EvCompCompat
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import kotlinx.coroutines.Deferred

class FakeEvCompCompat
constructor(
    override val supported: Boolean = false,
    override val range: Range<Int> = Range(0, 0),
    override val step: Rational = Rational.ZERO,
) : EvCompCompat {
    override fun stopRunningTask(throwable: Throwable) {
        TODO("Not yet implemented")
    }

    override fun applyAsync(
        evCompIndex: Int,
        requestControl: UseCaseCameraRequestControl,
        cancelPreviousTask: Boolean,
    ): Deferred<Int> {
        TODO("Not yet implemented")
    }
}
