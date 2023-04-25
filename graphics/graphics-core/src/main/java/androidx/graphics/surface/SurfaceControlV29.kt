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

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.Region
import android.hardware.HardwareBuffer
import android.os.Build
import android.view.AttachedSurfaceControl
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.graphics.lowlatency.BufferTransformHintResolver.Companion.UNKNOWN_TRANSFORM
import androidx.graphics.lowlatency.FrontBufferUtils
import androidx.hardware.SyncFenceImpl
import androidx.graphics.surface.SurfaceControlCompat.Companion.BUFFER_TRANSFORM_ROTATE_270
import androidx.graphics.surface.SurfaceControlCompat.Companion.BUFFER_TRANSFORM_ROTATE_90
import androidx.hardware.SyncFenceV19
import java.util.concurrent.Executor

/**
 * Implementation of [SurfaceControlImpl] that wraps the [SurfaceControlWrapper] API.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class SurfaceControlV29 internal constructor(
    internal val surfaceControl: SurfaceControlWrapper
) : SurfaceControlImpl {

    private var currActiveBufferReleaseCallback: (() -> Unit)? = null

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
         * See [SurfaceControlWrapper.Builder.setParent]
         */
        override fun setParent(surfaceControl: SurfaceControlCompat): SurfaceControlImpl.Builder {
            builder.setParent(surfaceControl.scImpl.asWrapperSurfaceControl())
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
        private val uncommittedBufferCallbackMap = HashMap<SurfaceControlImpl, BufferData?>()
        private val pendingSetTransformCalls = HashMap<SurfaceControlImpl, Int>()

        /**
         * Class to wrap metadata around setBuffer calls. This is used to appropriately call
         * the release callbacks as well as configure the buffer transform for older API levels
         */
        private class BufferData(
            val width: Int,
            val height: Int,
            val releaseCallback: (() -> Unit)?
        )

        /**
         * See [SurfaceControlWrapper.Transaction.commit]
         */
        override fun commit() {
            setPendingBufferTransform()
            updateReleaseCallbacks()
            uncommittedBufferCallbackMap.clear()
            pendingSetTransformCalls.clear()
            transaction.commit()
        }

        private fun updateReleaseCallbacks() {
            // store prev committed callbacks so we only need 1 onComplete callback
            val callbackInvokeList = mutableListOf<(() -> Unit)>()

            for (surfaceControl in uncommittedBufferCallbackMap.keys) {
                (surfaceControl as? SurfaceControlV29)?.apply {
                    // add active buffers callback to list if we have a new buffer about to overwrite
                    currActiveBufferReleaseCallback?.let { callbackInvokeList.add(it) }

                    // add as new active buffer callback
                    currActiveBufferReleaseCallback =
                        uncommittedBufferCallbackMap[surfaceControl]?.releaseCallback
                }
            }

            if (callbackInvokeList.size > 0) {
                val callbackListener = object : SurfaceControlCompat.TransactionCompletedListener {
                    override fun onTransactionCompleted() {
                        callbackInvokeList.forEach { it.invoke() }
                        callbackInvokeList.clear()
                    }
                }

                this.addTransactionCompletedListener(callbackListener)
            }
        }

        private fun setPendingBufferTransform() {
            for (surfaceControl in pendingSetTransformCalls.keys) {
                uncommittedBufferCallbackMap[surfaceControl]?.let {
                    val transformation = pendingSetTransformCalls.getOrDefault(
                        surfaceControl,
                        UNKNOWN_TRANSFORM
                    )
                    if (transformation != UNKNOWN_TRANSFORM) {
                        val dstWidth: Int
                        val dstHeight: Int
                        if (transformation == BUFFER_TRANSFORM_ROTATE_90 ||
                            transformation == BUFFER_TRANSFORM_ROTATE_270
                        ) {
                            dstWidth = it.height
                            dstHeight = it.width
                        } else {
                            dstWidth = it.width
                            dstHeight = it.height
                        }
                        transaction.setGeometry(
                            surfaceControl.asWrapperSurfaceControl(),
                            it.width,
                            it.height,
                            dstWidth,
                            dstHeight,
                            transformation
                        )
                    }
                }
            }
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
         * See [SurfaceControlWrapper.Transaction.setBuffer]
         */
        override fun setBuffer(
            surfaceControl: SurfaceControlImpl,
            buffer: HardwareBuffer?,
            fence: SyncFenceImpl?,
            releaseCallback: (() -> Unit)?
        ): SurfaceControlImpl.Transaction {
            if (buffer != null) {
                // we have a previous mapping in the same transaction, invoke callback
                val data = BufferData(
                    width = buffer.width,
                    height = buffer.height,
                    releaseCallback = releaseCallback
                )
                uncommittedBufferCallbackMap.put(surfaceControl, data)?.releaseCallback?.invoke()
            }

            val targetBuffer = buffer ?: PlaceholderBuffer
            // Ensure if we have a null value, we default to the default value for SyncFence
            // argument to prevent null pointer dereference
            if (fence == null) {
                transaction.setBuffer(surfaceControl.asWrapperSurfaceControl(), targetBuffer)
            } else {
                transaction.setBuffer(
                    surfaceControl.asWrapperSurfaceControl(),
                    targetBuffer,
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
         * See [SurfaceControlWrapper.Transaction.setCrop]
         */
        @RequiresApi(Build.VERSION_CODES.S)
        override fun setCrop(
            surfaceControl: SurfaceControlImpl,
            crop: Rect?
        ): SurfaceControlImpl.Transaction {
            transaction.setCrop(surfaceControl.asWrapperSurfaceControl(), crop)
            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.setPosition]
         */
        @RequiresApi(Build.VERSION_CODES.S)
        override fun setPosition(
            surfaceControl: SurfaceControlImpl,
            x: Float,
            y: Float
        ): SurfaceControlImpl.Transaction {
            transaction.setPosition(surfaceControl.asWrapperSurfaceControl(), x, y)
            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.setScale]
         */
        @RequiresApi(Build.VERSION_CODES.S)
        override fun setScale(
            surfaceControl: SurfaceControlImpl,
            scaleX: Float,
            scaleY: Float
        ): SurfaceControlImpl.Transaction {
            transaction.setScale(surfaceControl.asWrapperSurfaceControl(), scaleX, scaleY)
            return this
        }

        /**
         * See [SurfaceControlWrapper.Transaction.setBufferTransform]
         */
        override fun setBufferTransform(
            surfaceControl: SurfaceControlImpl,
            @SurfaceControlCompat.Companion.BufferTransform transformation: Int
        ): Transaction {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                transaction.setBufferTransform(
                    surfaceControl.asWrapperSurfaceControl(),
                    transformation
                )
            } else {
                pendingSetTransformCalls[surfaceControl] = transformation
            }
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

        private fun SyncFenceImpl.asSyncFenceCompat(): SyncFenceV19 =
            if (this is SyncFenceV19) {
                this
            } else {
                throw IllegalArgumentException(
                    "Expected SyncFenceCompat implementation " +
                        "for API level 19"
                )
            }
    }

    private companion object {

        // Certain Android platform versions have inconsistent behavior when it comes to
        // configuring a null HardwareBuffer. More specifically Android Q appears to crash
        // and restart emulator instances.
        // Additionally the SDK setBuffer API hides the buffer from the display if it is
        // null but the NDK API does not and persists the buffer contents on screen.
        // So instead change the buffer to a 1 x 1 placeholder to achieve a similar effect
        // with more consistent behavior.
        @SuppressLint("WrongConstant")
        val PlaceholderBuffer = HardwareBuffer.create(
            1,
            1,
            HardwareBuffer.RGBA_8888,
            1,
            FrontBufferUtils.BaseFlags
        )

        fun SurfaceControlImpl.asWrapperSurfaceControl(): SurfaceControlWrapper =
            if (this is SurfaceControlV29) {
                surfaceControl
            } else {
                throw IllegalArgumentException("Parent implementation is only for Android T+.")
            }
    }
}