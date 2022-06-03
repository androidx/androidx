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

package androidx.graphics.surface

import android.hardware.HardwareBuffer
import android.os.Build
import android.view.AttachedSurfaceControl
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.R)
internal class SurfaceControlV31 internal constructor() : SurfaceControlImpl {

    override fun isValid(): Boolean {
        TODO("Not yet implemented")
    }

    override fun release() {
        TODO("Not yet implemented")
    }

    class Builder : SurfaceControlImpl.Builder {

        override fun setParent(surfaceView: SurfaceView): SurfaceControlImpl.Builder {
            TODO("Create from SurfaceView surface")
        }

        override fun setName(name: String): SurfaceControlImpl.Builder {
            TODO("Not yet implemented")
        }

        override fun build(): SurfaceControlImpl {
            TODO("Not yet implemented")
        }
    }

    class Transaction : SurfaceControlImpl.Transaction {

        override fun commit() {
            TODO("Not yet implemented")
        }

        override fun setVisibility(
            surfaceControl: SurfaceControlImpl,
            visible: Boolean
        ): SurfaceControlImpl.Transaction {
            TODO("Not yet implemented")
        }

        override fun reparent(
            surfaceControl: SurfaceControlImpl,
            newParent: SurfaceControlImpl?
        ): SurfaceControlImpl.Transaction {
            TODO("Not yet implemented")
        }

        override fun reparent(
            surfaceControl: SurfaceControlImpl,
            surfaceView: SurfaceView
        ): SurfaceControlImpl.Transaction {
            TODO("Not yet implemented")
        }

        override fun setBuffer(
            surfaceControl: SurfaceControlImpl,
            buffer: HardwareBuffer,
            releaseCallback: (() -> Unit)?
        ): SurfaceControlImpl.Transaction {
            TODO("Not yet implemented")
        }

        override fun setLayer(
            surfaceControl: SurfaceControlImpl,
            z: Int
        ): SurfaceControlImpl.Transaction {
            TODO("Not yet implemented")
        }

        override fun addTransactionCommittedListener(
            executor: Executor,
            listener: SurfaceControlCompat.TransactionCommittedListener
        ): SurfaceControlImpl.Transaction {
            TODO("Not yet implemented")
        }

        override fun setOpaque(
            surfaceControl: SurfaceControlImpl,
            isOpaque: Boolean
        ): SurfaceControlImpl.Transaction {
            TODO("Not yet implemented")
        }

        override fun reparent(
            surfaceControl: SurfaceControlImpl,
            attachedSurfaceControl: AttachedSurfaceControl
        ): SurfaceControlImpl.Transaction {
            throw UnsupportedOperationException("Android R does not support reparenting to an " +
                "AttachedSurfaceControl")
        }

        override fun commitTransactionOnDraw(attachedSurfaceControl: AttachedSurfaceControl) {
            throw UnsupportedOperationException("Android R does ot support committing " +
                "transactions synchronously with the draw pass of an AttachedSurfaceControl")
        }
    }
}