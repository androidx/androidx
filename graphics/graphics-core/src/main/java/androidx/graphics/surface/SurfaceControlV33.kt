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

import android.graphics.Rect
import android.graphics.Region
import android.hardware.HardwareBuffer
import android.hardware.SyncFence
import android.os.Build
import android.view.AttachedSurfaceControl
import android.view.SurfaceControl
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.hardware.SyncFenceImpl
import androidx.hardware.SyncFenceV33
import java.util.concurrent.Executor

/**
 * Implementation of [SurfaceControlImpl] that wraps the SDK's [SurfaceControl] API.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class SurfaceControlV33 internal constructor(
    internal val surfaceControl: SurfaceControl
) : SurfaceControlImpl {

    /**
     * See [SurfaceControlImpl.isValid]
     */
    override fun isValid(): Boolean = surfaceControl.isValid

    /**
     * See [SurfaceControlImpl.release]
     */
    override fun release() {
        surfaceControl.release()
    }

    /**
     * See [SurfaceControlImpl.Builder]
     */
    class Builder : SurfaceControlImpl.Builder {

        private val builder = SurfaceControl.Builder()

        /**
         * See [SurfaceControlImpl.Builder.setParent]
         */
        override fun setParent(surfaceView: SurfaceView): SurfaceControlImpl.Builder {
            builder.setParent(surfaceView.surfaceControl)
            return this
        }

        /**
         * See [SurfaceControlImpl.Builder.setParent]
         */
        override fun setParent(surfaceControl: SurfaceControlCompat): SurfaceControlImpl.Builder {
            builder.setParent(surfaceControl.scImpl.asFrameworkSurfaceControl())
            return this
        }

        /**
         * See [SurfaceControlImpl.Builder.setName]
         */
        override fun setName(name: String): Builder {
            builder.setName(name)
            return this
        }

        /**
         * See [SurfaceControlImpl.Builder.build]
         */
        override fun build(): SurfaceControlImpl = SurfaceControlV33(builder.build())
    }

    /**
     * See [SurfaceControlImpl.Transaction]
     */
    class Transaction : SurfaceControlImpl.Transaction {

        private val mTransaction = SurfaceControl.Transaction()

        /**
         * See [SurfaceControlImpl.Transaction.setOpaque]
         */
        override fun setOpaque(
            surfaceControl: SurfaceControlImpl,
            isOpaque: Boolean
        ): SurfaceControlImpl.Transaction {
            mTransaction.setOpaque(surfaceControl.asFrameworkSurfaceControl(), isOpaque)
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.setVisibility]
         */
        override fun setVisibility(
            surfaceControl: SurfaceControlImpl,
            visible: Boolean
        ): SurfaceControlImpl.Transaction {
            mTransaction.setVisibility(surfaceControl.asFrameworkSurfaceControl(), visible)
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.setBuffer]
         */
        override fun setBuffer(
            surfaceControl: SurfaceControlImpl,
            buffer: HardwareBuffer?,
            fence: SyncFenceImpl?,
            releaseCallback: (() -> Unit)?
        ): Transaction {
            mTransaction.setBuffer(
                surfaceControl.asFrameworkSurfaceControl(),
                buffer,
                fence?.asSyncFence()
            ) {
                releaseCallback?.invoke()
            }
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.setLayer]
         */
        override fun setLayer(
            surfaceControl: SurfaceControlImpl,
            z: Int
        ): SurfaceControlImpl.Transaction {
            mTransaction.setLayer(surfaceControl.asFrameworkSurfaceControl(), z)
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.reparent]
         */
        override fun reparent(
            surfaceControl: SurfaceControlImpl,
            newParent: SurfaceControlImpl?
        ): Transaction {
            mTransaction.reparent(
                surfaceControl.asFrameworkSurfaceControl(),
                newParent?.asFrameworkSurfaceControl()
            )
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.reparent]
         */
        override fun reparent(
            surfaceControl: SurfaceControlImpl,
            attachedSurfaceControl: AttachedSurfaceControl
        ): SurfaceControlImpl.Transaction {
            val reparentTransaction = attachedSurfaceControl
                .buildReparentTransaction(surfaceControl.asFrameworkSurfaceControl())
            if (reparentTransaction != null) {
                mTransaction.merge(reparentTransaction)
            }
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.addTransactionCommittedListener]
         */
        override fun addTransactionCommittedListener(
            executor: Executor,
            listener: SurfaceControlCompat.TransactionCommittedListener
        ): SurfaceControlImpl.Transaction {
            mTransaction.addTransactionCommittedListener(executor) {
                listener.onTransactionCommitted()
            }
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.setDamageRegion]
         */
        override fun setDamageRegion(
            surfaceControl: SurfaceControlImpl,
            region: Region?
        ): SurfaceControlImpl.Transaction {
            mTransaction.setDamageRegion(surfaceControl.asFrameworkSurfaceControl(), region)
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.setAlpha]
         */
        override fun setAlpha(
            surfaceControl: SurfaceControlImpl,
            alpha: Float
        ): SurfaceControlImpl.Transaction {
            mTransaction.setAlpha(surfaceControl.asFrameworkSurfaceControl(), alpha)
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.setCrop]
         */
        override fun setCrop(
            surfaceControl: SurfaceControlImpl,
            crop: Rect?
        ): SurfaceControlImpl.Transaction {
            mTransaction.setCrop(surfaceControl.asFrameworkSurfaceControl(), crop)
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.setPosition]
         */
        override fun setPosition(
            surfaceControl: SurfaceControlImpl,
            x: Float,
            y: Float
        ): SurfaceControlImpl.Transaction {
            mTransaction.setPosition(surfaceControl.asFrameworkSurfaceControl(), x, y)
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.setScale]
         */
        override fun setScale(
            surfaceControl: SurfaceControlImpl,
            scaleX: Float,
            scaleY: Float
        ): SurfaceControlImpl.Transaction {
            mTransaction.setScale(surfaceControl.asFrameworkSurfaceControl(), scaleX, scaleY)
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.setBufferTransform]
         */
        override fun setBufferTransform(
            surfaceControl: SurfaceControlImpl,
            @SurfaceControlCompat.Companion.BufferTransform transformation: Int
        ): SurfaceControlImpl.Transaction {
            mTransaction.setBufferTransform(
                surfaceControl.asFrameworkSurfaceControl(),
                transformation
            )
            return this
        }

        /**
         * See [SurfaceControlImpl.Transaction.commit]
         */
        override fun commit() {
            mTransaction.apply()
        }

        /**
         * See [SurfaceControlImpl.Transaction.close]
         */
        override fun close() {
            mTransaction.close()
        }

        /**
         * See [SurfaceControlImpl.Transaction.commitTransactionOnDraw]
         */
        override fun commitTransactionOnDraw(attachedSurfaceControl: AttachedSurfaceControl) {
            attachedSurfaceControl.applyTransactionOnDraw(mTransaction)
        }

        private fun SyncFenceImpl.asSyncFence(): SyncFence =
            if (this is SyncFenceV33) {
                mSyncFence
            } else {
                throw
                IllegalArgumentException("Expected SyncFenceCompat implementation for API level 33")
            }
    }

    private companion object {
        fun SurfaceControlImpl.asFrameworkSurfaceControl(): SurfaceControl =
            if (this is SurfaceControlV33) {
                surfaceControl
            } else {
                throw IllegalArgumentException("Parent implementation is not for Android T")
            }
    }
}