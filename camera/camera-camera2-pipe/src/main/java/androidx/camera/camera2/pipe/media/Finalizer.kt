/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.media

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Interface for objects that know how to finalize arbitrary objects.
 *
 * This is primarily used by classes that may need to intercept and do something with an
 * intermediate object before it is fully closed.
 */

interface Finalizer<in T> {
    fun finalize(value: T?)
}

/** Simple [Finalizer] that can be used with [AutoCloseable] objects to close and release them. */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
object ClosingFinalizer : Finalizer<AutoCloseable> {
    override fun finalize(value: AutoCloseable?) {
        value?.close()
    }
}

/** Simple [Finalizer] that does nothing. Often used for objects that do not need to be closed. */
object NoOpFinalizer : Finalizer<Any?> {
    override fun finalize(value: Any?) {}
}
