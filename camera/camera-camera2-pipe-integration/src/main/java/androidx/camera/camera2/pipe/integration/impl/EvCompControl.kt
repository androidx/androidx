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

package androidx.camera.camera2.pipe.integration.impl

import android.util.Range
import android.util.Rational
import androidx.camera.camera2.pipe.integration.compat.EvCompCompat
import androidx.camera.camera2.pipe.integration.config.CameraScope
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Inject

@CameraScope
class EvCompControl @Inject constructor(private val compat: EvCompCompat) : UseCaseCameraControl {
    private var _evCompIndex = 0
    var evCompIndex: Int
        get() = _evCompIndex
        set(value) {
            _evCompIndex = value
            update()
        }

    val supported: Boolean
        get() = compat.supported
    val range: Range<Int>
        get() = compat.range
    val step: Rational
        get() = compat.step

    private var _useCaseCamera: UseCaseCamera? = null
    override var useCaseCamera: UseCaseCamera?
        get() = _useCaseCamera
        set(value) {
            _useCaseCamera = value
            update()
        }

    override fun reset() {
        _evCompIndex = 0
    }

    private fun update() {
        _useCaseCamera?.let {
            compat.apply(_evCompIndex, it)
        }
    }

    @Module
    abstract class Bindings {
        @Binds
        @IntoSet
        abstract fun provideControls(evCompControl: EvCompControl): UseCaseCameraControl
    }
}
