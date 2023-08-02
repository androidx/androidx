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

import android.graphics.Region
import android.hardware.HardwareBuffer
import android.os.Build
import android.view.AttachedSurfaceControl
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.graphics.lowlatency.SyncFenceImpl
import androidx.graphics.lowlatency.SyncFenceV19
import androidx.hardware.SyncFence
import java.util.concurrent.Executor

/**
 * Implementation of [SurfaceControlImpl] that wraps the [SurfaceControlWrapper] API.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class SurfaceControlV29 internal constructor(
    internal val surfaceControl: SurfaceControlWrapper
) : SurfaceControlImpl {

    /**
     * See [SurfaceControlWrapper.isValid]
     */
    override fun isValid(): Boolean = surfaceControl.isValid()

    /**
     * See [SurfaceControlWrapper.release]
     */
    override fun release() {
        surfaceControl.release()
    }

    /**
     * See [SurfaceControlWrapper.Builder]
     */
    class Builder : SurfaceControlImpl.Builder {
        private var builder = SurfaceControlWrapper.Builder()

        /**
         * See [SurfaceControlWrapper.Builder.setParent]
         */
        override fun setParent(surfaceView: SurfaceView): SurfaceControlImpl.Builder {
            builder.setParent(surfaceView.holder.surface)
            return this
        }

        /**
         * See [SurfaceControlWrapper.Builder.setDebugName]
         */
        override fun setName(name: String): SurfaceControlImpl.Builder {
            builder.setDebugName(name)
            return this
        }

        /**
         * See [SurfaceControlWrapper.Builder.build]
         */
        override fun build(): SurfaceControlImpl = SurfaceControlV29(builder.build())
    }

    /**
     * See [SurfaceControlWrapper.Transaction]
     */
    class Transaction : SurfaceControlImpl.Transaction {
        private val transaction = SurfaceControlWrapper.Transaction()
        private val bufferCallbacks = ArrayList<(() -> Unit)>()

        /**
         * See [SurfaceControlWrapper.Transaction.commit]
         */
        override fun commit() {
            if (bufferCallbacks.size > 0) {
                val callbackListener = object : SurfaceControlCompat.TransactionCompletedListener {
                    override fun onTransactionCompleted() {
                        for (callback in bufferCallbacks) {
                            callback.invoke()
                        }

                        bufferCallbacks.clear()
                    }
                }

                this.addTransactionCompletedListener(callbackListener)
            }
            transaction.commit()
        }

        /**
         * See [SurfaceControlWrapper.Transaction.setVisibility]
         */
        override fun setVisibility(
            surfaceControl: SurfaceControlImpl,
            visible: Boolean
        ): SurfaceControlImpl.Transaction {
            transaction.setVisibility(surfaceControl.asWrapperSurfaceControl(), visible)
            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.reparent]
         */
        override fun reparent(
            surfaceControl: SurfaceControlImpl,
            newParent: SurfaceControlImpl?
        ): SurfaceControlImpl.Transaction {
            transaction.reparent(
                surfaceControl.asWrapperSurfaceControl(),
                newParent?.asWrapperSurfaceControl()
            )
            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.reparent]
         */
        override fun reparent(
            surfaceControl: SurfaceControlImpl,
            surfaceView: SurfaceView
        ): SurfaceControlImpl.Transaction {
            transaction.reparent(
                surfaceControl.asWrapperSurfaceControl(),
                SurfaceControlWrapper.Builder()
                    .setParent(surfaceView.holder.surface)
                    .setDebugName(surfaceView.toString())
                    .build()
            )
            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.setBuffer]
         */
        override fun setBuffer(
            surfaceControl: SurfaceControlImpl,
            buffer: HardwareBuffer,
            fence: SyncFenceImpl?,
            releaseCallback: (() -> Unit)?
        ): SurfaceControlImpl.Transaction {
            if (releaseCallback != null) {
                bufferCallbacks.add { releaseCallback() }
            }

            // Ensure if we have a null value, we default to the default value for SyncFence
            // argument to prevent null pointer dereference
            if (fence == null) {
                transaction.setBuffer(surfaceControl.asWrapperSurfaceControl(), buffer)
            } else {
                transaction.setBuffer(
                    surfaceControl.asWrapperSurfaceControl(),
                    buffer,
                    fence.asSyncFenceCompat()
                )
            }

            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.setLayer]
         */
        override fun setLayer(
            surfaceControl: SurfaceControlImpl,
            z: Int
        ): SurfaceControlImpl.Transaction {
            transaction.setLayer(surfaceControl.asWrapperSurfaceControl(), z)
            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.addTransactionCommittedListener]
         */
        @RequiresApi(Build.VERSION_CODES.S)
        override fun addTransactionCommittedListener(
            executor: Executor,
            listener: SurfaceControlCompat.TransactionCommittedListener
        ): SurfaceControlImpl.Transaction {
            transaction.addTransactionCommittedListener(executor, listener)
            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.addTransactionCompletedListener]
         */
        fun addTransactionCompletedListener(
            listener: SurfaceControlCompat.TransactionCompletedListener
        ): SurfaceControlImpl.Transaction {
            transaction.addTransactionCompletedListener(listener)
            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.setDamageRegion]
         */
        override fun setDamageRegion(
            surfaceControl: SurfaceControlImpl,
            region: Region?
        ): SurfaceControlImpl.Transaction {
            transaction.setDamageRegion(surfaceControl.asWrapperSurfaceControl(), region)
            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.setOpaque]
         */
        override fun setOpaque(
            surfaceControl: SurfaceControlImpl,
            isOpaque: Boolean
        ): SurfaceControlImpl.Transaction {
            transaction.setOpaque(surfaceControl.asWrapperSurfaceControl(), isOpaque)
            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.setAlpha]
         */
        override fun setAlpha(
            surfaceControl: SurfaceControlImpl,
            alpha: Float
        ): SurfaceControlImpl.Transaction {
            transaction.setAlpha(surfaceControl.asWrapperSurfaceControl(), alpha)
            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.close]
         */
        override fun close() {
            transaction.close()
        }

        override fun reparent(
            surfaceControl: SurfaceControlImpl,
            attachedSurfaceControl: AttachedSurfaceControl
        ): SurfaceControlImpl.Transaction {
            throw UnsupportedOperationException(
                "Reparenting to an AttachedSurfaceControl is only available on Android T+."
            )
        }

        override fun commitTransactionOnDraw(attachedSurfaceControl: AttachedSurfaceControl) {
            throw UnsupportedOperationException(
                "Committing transactions synchronously with the draw pass of an " +
                    "AttachedSurfaceControl is only available on Android T+."
            )
        }

        private fun SurfaceControlImpl.asWrapperSurfaceControl(): SurfaceControlWrapper =
            if (this is SurfaceControlV29) {
                surfaceControl
            } else {
                throw IllegalArgumentException("Parent implementation is only for Android T+.")
            }

        private fun SyncFenceImpl.asSyncFenceCompat(): SyncFence =
            if (this is SyncFenceV19) {
                mSyncFence
            } else {
                throw IllegalArgumentException("Expected SyncFenceCompat implementation " +
                    "for API level 19")
            }
    }
}