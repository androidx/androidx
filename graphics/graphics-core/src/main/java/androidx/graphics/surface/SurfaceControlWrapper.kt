/*
 * Copyright 2021 The Android Open Source Project
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
import android.os.Build
import android.view.Surface
import android.view.SurfaceControl
import androidx.annotation.RequiresApi
import androidx.hardware.SyncFenceV19
import java.util.concurrent.Executor

internal class JniBindings {
    companion object {
        @JvmStatic
        external fun nCreate(surfaceControl: Long, debugName: String): Long
        @JvmStatic
        external fun nCreateFromSurface(surface: Surface, debugName: String): Long
        @JvmStatic
        external fun nRelease(surfaceControl: Long)
        @JvmStatic

        external fun nTransactionCreate(): Long
        @JvmStatic
        external fun nTransactionDelete(surfaceTransaction: Long)
        @JvmStatic
        external fun nTransactionApply(surfaceTransaction: Long)
        @JvmStatic
        external fun nTransactionReparent(
            surfaceTransaction: Long,
            surfaceControl: Long,
            newParent: Long
        )

        @JvmStatic
        external fun nTransactionSetOnComplete(
            surfaceTransaction: Long,
            listener: SurfaceControlCompat.TransactionCompletedListener
        )

        @JvmStatic
        external fun nTransactionSetOnCommit(
            surfaceTransaction: Long,
            listener: SurfaceControlCompat.TransactionCommittedListener
        )

        @JvmStatic
        external fun nDupFenceFd(
            syncFence: SyncFenceV19
        ): Int

        @JvmStatic
        external fun nSetBuffer(
            surfaceTransaction: Long,
            surfaceControl: Long,
            hardwareBuffer: HardwareBuffer?,
            acquireFieldFd: SyncFenceV19
        )

        @JvmStatic
        external fun nSetGeometry(
            surfaceTransaction: Long,
            surfaceControl: Long,
            bufferWidth: Int,
            bufferHeight: Int,
            dstWidth: Int,
            dstHeight: Int,
            transformation: Int
        )

        @JvmStatic
        external fun nSetVisibility(
            surfaceTransaction: Long,
            surfaceControl: Long,
            visibility: Byte
        )

        @JvmStatic
        external fun nSetZOrder(surfaceTransaction: Long, surfaceControl: Long, zOrder: Int)
        @JvmStatic
        external fun nSetDamageRegion(
            surfaceTransaction: Long,
            surfaceControl: Long,
            rect: Rect?
        )

        @JvmStatic
        external fun nSetDesiredPresentTime(
            surfaceTransaction: Long,
            desiredPresentTime: Long
        )

        @JvmStatic
        external fun nSetBufferTransparency(
            surfaceTransaction: Long,
            surfaceControl: Long,
            transparency: Byte,
        )

        @JvmStatic
        external fun nSetBufferAlpha(
            surfaceTransaction: Long,
            surfaceControl: Long,
            alpha: Float
        )

        @JvmStatic
        external fun nSetCrop(
            surfaceTransaction: Long,
            surfaceControl: Long,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int
        )

        @JvmStatic
        external fun nSetPosition(
            surfaceTransaction: Long,
            surfaceControl: Long,
            x: Float,
            y: Float
        )

        @JvmStatic
        external fun nSetScale(
            surfaceTransaction: Long,
            surfaceControl: Long,
            scaleX: Float,
            scaleY: Float
        )

        @JvmStatic
        external fun nSetBufferTransform(
            surfaceTransaction: Long,
            surfaceControl: Long,
            transformation: Int
        )

        init {
            System.loadLibrary("graphics-core")
        }
    }
}

/**
 * Handle to an on-screen Surface managed by the system compositor. By constructing
 * a [Surface] from this [SurfaceControlWrapper] you can submit buffers to be composited. Using
 * [SurfaceControlWrapper.Transaction] you can manipulate various properties of how the buffer will be
 * displayed on-screen. SurfaceControls are arranged into a scene-graph like hierarchy, and
 * as such any SurfaceControl may have a parent. Geometric properties like transform, crop, and
 * Z-ordering will be inherited from the parent, as if the child were content in the parents
 * buffer stream.
 *
 * Compatibility class for [SurfaceControl]. This differs by being built upon the equivalent API
 * within the Android NDK. It introduces some APIs into earlier platform releases than what was
 * initially exposed for SurfaceControl.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class SurfaceControlWrapper {

    constructor(surfaceControl: SurfaceControlWrapper, debugName: String) {
        mNativeSurfaceControl = JniBindings.nCreate(surfaceControl.mNativeSurfaceControl, debugName)
        if (mNativeSurfaceControl == 0L) {
            throw IllegalArgumentException()
        }
    }

    constructor(surface: Surface, debugName: String) {
        mNativeSurfaceControl = JniBindings.nCreateFromSurface(surface, debugName)
        if (mNativeSurfaceControl == 0L) {
            throw IllegalArgumentException()
        }
    }

    private var mNativeSurfaceControl: Long = 0

    /**
     * Compatibility class for ASurfaceTransaction.
     */
    class Transaction() {
        private var mNativeSurfaceTransaction: Long

        init {
            mNativeSurfaceTransaction = JniBindings.nTransactionCreate()
            if (mNativeSurfaceTransaction == 0L) {
                throw java.lang.IllegalArgumentException()
            }
        }

        /**
         * Commits the updates accumulated in this transaction.
         *
         * This is the equivalent of ASurfaceTransaction_apply.
         *
         * Note that the transaction is guaranteed to be applied atomically. The
         * transactions which are applied on the same thread are als guaranteed to be applied
         * in order.
         */
        fun commit() {
            JniBindings.nTransactionApply(mNativeSurfaceTransaction)
        }

        // Suppression of PairedRegistration below is in order to match existing
        // framework implementation of no remove method for listeners

        /**
         * Sets the callback that is invoked once the updates from this transaction are
         * presented.
         *
         * @param listener The callback that will be invoked when the transaction has
         * been completed. This value cannot be null.
         */
        @Suppress("PairedRegistration")
        internal fun addTransactionCompletedListener(
            listener: SurfaceControlCompat.TransactionCompletedListener
        ): Transaction {
            JniBindings.nTransactionSetOnComplete(mNativeSurfaceTransaction, listener)
            return this
        }

        /**
         * Sets the callback that is invoked once the updates from this transaction are
         * applied and ready to be presented. This callback is invoked before the
         * setOnCompleteListener callback.
         *
         * @param executor The executor that the callback should be invoked on.
         * This value can be null, where it will run on the default thread. Callback and
         * listener events are dispatched through this Executor, providing an easy way
         * to control which thread is used.
         *
         * @param listener The callback that will be invoked when the transaction has
         * been completed. This value cannot be null.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        @Suppress("PairedRegistration")
        fun addTransactionCommittedListener(
            executor: Executor?,
            listener: SurfaceControlCompat.TransactionCommittedListener
        ): Transaction {
            var listenerWrapper = listener
            if (executor != null) {
                listenerWrapper = object : SurfaceControlCompat.TransactionCommittedListener {
                    override fun onTransactionCommitted() {
                        executor.execute { (listener::onTransactionCommitted)() }
                    }
                }
            }
            JniBindings.nTransactionSetOnCommit(mNativeSurfaceTransaction, listenerWrapper)
            return this
        }

        /**
         * Updates the [HardwareBuffer] displayed for the provided surfaceControl. Takes an
         * optional [SyncFenceV19] that is signalled when all pending work for the buffer
         * is complete and the buffer can be safely read.
         *
         * The frameworks takes ownership of the syncFence passed and is responsible for closing
         * it.
         *
         * Note that the buffer must be allocated with [HardwareBuffer.USAGE_COMPOSER_OVERLAY] and
         * [HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE] as the surface control might be
         * composited using an overlay or the GPU.
         *
         * @param surfaceControl The surfaceControl to update. Can not be null.
         *
         * @param hardwareBuffer The buffer to be displayed. This can not be null.
         *
         * @param syncFence The presentation fence. If null or invalid, this is equivalent to not
         * including it.
         */
        @JvmOverloads
        fun setBuffer(
            surfaceControl: SurfaceControlWrapper,
            hardwareBuffer: HardwareBuffer?,
            syncFence: SyncFenceV19 = SyncFenceV19(-1)
        ): Transaction {
            JniBindings.nSetBuffer(
                mNativeSurfaceTransaction,
                surfaceControl.mNativeSurfaceControl,
                hardwareBuffer,
                syncFence
            )
            return this
        }

        /**
         * Updates the visibility of the given [SurfaceControlWrapper]. If visibility is set to
         * false, the [SurfaceControlWrapper] and all surfaces in the subtree will be hidden.
         * By default SurfaceControls are visible.
         *
         * @param surfaceControl The SurfaceControl for which to set the visibility
         * This value cannot be null.
         *
         * @param visibility the new visibility. A value of true means the surface and its children
         * will be visible.
         */
        fun setVisibility(
            surfaceControl: SurfaceControlWrapper,
            visibility: Boolean
        ): Transaction {
            JniBindings.nSetVisibility(
                mNativeSurfaceTransaction,
                surfaceControl.mNativeSurfaceControl,
                if (visibility) 1 else 0
            )
            return this
        }

        /**
         * Updates z order index for [SurfaceControlWrapper]. Note that the z order for a
         * surface is relative to other surfaces that are siblings of this surface.
         * Behavior of siblings with the same z order is undefined.
         *
         * Z orders can range from Integer.MIN_VALUE to Integer.MAX_VALUE. Default z order
         * index is 0. [SurfaceControlWrapper] instances are positioned back-to-front. That is
         * lower z order values are rendered below other [SurfaceControlWrapper] instances with
         * higher z order values.
         *
         * @param surfaceControl surface control to set the z order of.
         *
         * @param zOrder desired layer z order to set the surfaceControl.
         */
        fun setLayer(surfaceControl: SurfaceControlWrapper, zOrder: Int): Transaction {
            JniBindings.nSetZOrder(
                mNativeSurfaceTransaction,
                surfaceControl.mNativeSurfaceControl,
                zOrder
            )
            return this
        }

        /**
         * Updates the region for content on this surface updated in this transaction. If
         * unspecified, the complete surface will be assumed to be damaged. The damage region is
         * the area of the buffer that has changed since the previously sent buffer. This can be
         * used to reduce the amount of recomposition that needs to happen when only a small
         * region of the buffer is being updated, such as for a small blinking cursor or
         * a loading indicator.
         *
         * @param surfaceControl The surface control for which we want to set the damage region of.
         *
         * @param region The region to set. If null, the entire buffer is assumed dirty. This
         * is equivalent to not setting a damage region at all.
         */
        fun setDamageRegion(
            surfaceControl: SurfaceControlWrapper,
            region: Region?
        ): Transaction {
            JniBindings.nSetDamageRegion(
                mNativeSurfaceTransaction,
                surfaceControl.mNativeSurfaceControl,
                region?.bounds
            )
            return this
        }

        /**
         * Re-parents a given layer to a new parent. Children inherit transform
         * (position, scaling) crop, visibility, and Z-ordering from their parents, as
         * if the children were pixels within the parent Surface.
         *
         * Any children of the reparented surfaceControl will remain children of
         * the surfaceControl.
         *
         * The newParent can be null. Surface controls with a null parent do not
         * appear on the display.
         *
         * @param surfaceControl The surface control to reparent
         *
         * @param newParent the new parent we want to set the surface control to. Can be null.
         */
        fun reparent(
            surfaceControl: SurfaceControlWrapper,
            newParent: SurfaceControlWrapper?
        ): Transaction {
            JniBindings.nTransactionReparent(
                mNativeSurfaceTransaction,
                surfaceControl.mNativeSurfaceControl,
                newParent?.mNativeSurfaceControl ?: 0L
            )
            return this
        }

        /**
         * Specifies a desiredPresentTime for the transaction. The framework will try to present
         * the transaction at or after the time specified.
         *
         * Transactions will not be presented until all acquire fences have signaled even if the
         * app requests an earlier present time.
         *
         * If an earlier transaction has a desired present time of x, and a later transaction
         * has a desired present time that is before x, the later transaction will not preempt the
         * earlier transaction.
         *
         * @param desiredPresentTimeNano The present time in nanoseconds to try to present the
         * Transaction at.
         */
        fun setDesiredPresentTime(desiredPresentTimeNano: Long): Transaction {
            JniBindings.nSetDesiredPresentTime(mNativeSurfaceTransaction, desiredPresentTimeNano)
            return this
        }

        /**
         * Update whether the content in the buffer associated with this surface is completely
         * opaque. If true, every pixel of content in the buffer must be opaque or visual errors
         * can occur.
         *
         * @param surfaceControl surface control to set the transparency of.
         *
         * @param isOpaque true if buffers alpha should be ignored
         */
        fun setOpaque(
            surfaceControl: SurfaceControlWrapper,
            isOpaque: Boolean
        ): Transaction {
            JniBindings.nSetBufferTransparency(
                mNativeSurfaceTransaction,
                surfaceControl.mNativeSurfaceControl,
                if (isOpaque) 2 else 0
            )
            return this
        }

        /**
         * Sets the alpha for the buffer. It uses a premultiplied blending.
         *
         * The passsed in alpha must be inclusively between 0.0 and 1.0.
         *
         * @paaram surfaceControl The surface control that we want to set the alpha of.
         *
         * @param alpha alpha value within the range [0, 1].
         *
         * @throws IllegalArgumentException if alpha is out of range.
         */
        fun setAlpha(
            surfaceControl: SurfaceControlWrapper,
            alpha: Float
        ): Transaction {
            if (alpha < 0.0f || alpha > 1.0f) {
                throw IllegalArgumentException("Alpha value must be between 0.0 and 1.0.")
            } else {
                JniBindings.nSetBufferAlpha(
                    mNativeSurfaceTransaction,
                    surfaceControl.mNativeSurfaceControl,
                    alpha
                )
            }
            return this
        }

        /**
         * Bounds the surface and its children to the bounds specified. Size of the surface
         * will be ignored and only the crop and buffer size will be used to determine the
         * bounds of the surface. If no crop is specified and the surface has no buffer,
         * the surface bounds is only constrained by the size of its parent bounds.
         *
         * @param surfaceControl The [SurfaceControlWrapper] to apply the crop to. This value
         * cannot be null.
         *
         * @param crop Bounds of the crop to apply. This value can be null. A null value will remove
         * the crop and bounds are determined via bounds of the parent surface.
         *
         * @throws IllegalArgumentException if crop is not a valid rectangle.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        fun setCrop(surfaceControl: SurfaceControlWrapper, crop: Rect?): Transaction {
            require((crop == null) || (crop.width() >= 0 && crop.height() >= 0)) {
                throw IllegalArgumentException("width and height must be non-negative")
            }
            if (crop == null) {
                JniBindings.nSetCrop(
                    mNativeSurfaceTransaction,
                    surfaceControl.mNativeSurfaceControl,
                    0,
                    0,
                    0,
                    0
                )
            } else {
                JniBindings.nSetCrop(
                    mNativeSurfaceTransaction,
                    surfaceControl.mNativeSurfaceControl,
                    crop.left,
                    crop.top,
                    crop.right,
                    crop.bottom
                )
            }

            return this
        }

        /**
         * Sets the SurfaceControl to the specified position relative to the parent SurfaceControl
         *
         * @param surfaceControl The [SurfaceControlWrapper] to change position. This value cannot
         * be null
         *
         * @param x the X position
         *
         * @param y the Y position
         */
        @RequiresApi(Build.VERSION_CODES.S)
        fun setPosition(surfaceControl: SurfaceControlWrapper, x: Float, y: Float): Transaction {
            JniBindings.nSetPosition(
                mNativeSurfaceTransaction,
                surfaceControl.mNativeSurfaceControl,
                x,
                y
            )
            return this
        }

        /**
         * Sets the SurfaceControl to the specified scale with (0, 0) as the
         * center point of the scale.
         *
         * @param surfaceControl The [SurfaceControlWrapper] to change scale. This value cannot
         * be null.
         *
         * @param scaleX the X scale
         *
         * @param scaleY the Y scale
         */
        @RequiresApi(Build.VERSION_CODES.S)
        fun setScale(
            surfaceControl: SurfaceControlWrapper,
            scaleX: Float,
            scaleY: Float
        ): Transaction {
            JniBindings.nSetScale(
                mNativeSurfaceTransaction,
                surfaceControl.mNativeSurfaceControl,
                scaleX,
                scaleY
            )
            return this
        }

        /**
         * Sets the buffer transform that should be applied to the current buffer
         *
         * @param surfaceControl the [SurfaceControlWrapper] to update. This value cannot be null.
         *
         * @param transformation The transform to apply to the buffer. Value is
         * [SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY],
         * [SurfaceControlCompat.BUFFER_TRANSFORM_MIRROR_HORIZONTAL],
         * [SurfaceControlCompat.BUFFER_TRANSFORM_MIRROR_VERTICAL],
         * [SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90],
         * [SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180],
         * [SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270],
         * [SurfaceControlCompat.BUFFER_TRANSFORM_MIRROR_HORIZONTAL] |
         * [SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90], or
         * [SurfaceControlCompat.BUFFER_TRANSFORM_MIRROR_VERTICAL] |
         * [SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90]
         */
        @RequiresApi(Build.VERSION_CODES.S)
        fun setBufferTransform(
            surfaceControl: SurfaceControlWrapper,
            transformation: Int
        ): Transaction {
            JniBindings.nSetBufferTransform(
                mNativeSurfaceTransaction,
                surfaceControl.mNativeSurfaceControl,
                transformation
            )
            return this
        }

        fun setGeometry(
            surfaceControl: SurfaceControlWrapper,
            width: Int,
            height: Int,
            dstWidth: Int,
            dstHeight: Int,
            transformation: Int
        ): Transaction {
            JniBindings.nSetGeometry(
                mNativeSurfaceTransaction,
                surfaceControl.mNativeSurfaceControl,
                width,
                height,
                dstWidth,
                dstHeight,
                transformation
            )
            return this
        }

        /**
         * Destroys the transaction object.
         */
        fun close() {
            if (mNativeSurfaceTransaction != 0L) {
                JniBindings.nTransactionDelete(mNativeSurfaceTransaction)
                mNativeSurfaceTransaction = 0L
            }
        }

        fun finalize() {
            close()
        }
    }

    /**
     * Check whether this instance points to a valid layer with the system-compositor.
     */
    fun isValid(): Boolean = mNativeSurfaceControl != 0L

    override fun equals(other: Any?): Boolean {
        if (other == this) {
            return true
        }
        if ((other == null) or
            (other?.javaClass != SurfaceControlWrapper::class.java)
        ) {
            return false
        }

        other as SurfaceControlWrapper
        if (other.mNativeSurfaceControl == this.mNativeSurfaceControl) {
            return true
        }

        return false
    }

    override fun hashCode(): Int {
        return mNativeSurfaceControl.hashCode()
    }

    /**
     * Release the local reference to the server-side surface. The surface may continue to exist
     * on-screen as long as its parent continues to exist. To explicitly remove a surface from the
     * screen use [Transaction.reparent] with a null-parent. After release, [isValid] will return
     * false and other methods will throw an exception. Always call release() when you're done with
     * a SurfaceControl.
     */
    fun release() {
        if (mNativeSurfaceControl != 0L) {
            JniBindings.nRelease(mNativeSurfaceControl)
            mNativeSurfaceControl = 0
        }
    }

    protected fun finalize() {
        release()
    }

    /**
     * Builder class for [SurfaceControlWrapper].
     *
     * Requires a debug name.
     */
    class Builder {
        private var mSurface: Surface? = null
        private var mSurfaceControl: SurfaceControlWrapper? = null
        private lateinit var mDebugName: String

        fun setParent(surface: Surface): Builder {
            mSurface = surface
            mSurfaceControl = null
            return this
        }

        fun setParent(surfaceControlWrapper: SurfaceControlWrapper): Builder {
            mSurface = null
            mSurfaceControl = surfaceControlWrapper
            return this
        }

        @Suppress("MissingGetterMatchingBuilder")
        fun setDebugName(debugName: String): Builder {
            mDebugName = debugName
            return this
        }

        /**
         * Builds the [SurfaceControlWrapper] object
         */
        fun build(): SurfaceControlWrapper {
            val surface = mSurface
            val surfaceControl = mSurfaceControl
            return if (surface != null) {
                SurfaceControlWrapper(surface, mDebugName)
            } else if (surfaceControl != null) {
                SurfaceControlWrapper(surfaceControl, mDebugName)
            } else {
                throw IllegalStateException("")
            }
        }
    }
}