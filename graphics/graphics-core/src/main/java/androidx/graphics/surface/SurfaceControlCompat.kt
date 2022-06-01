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
import androidx.hardware.SyncFenceCompat
import java.util.concurrent.Executor

internal class JniBindings {
    companion object {
        external fun nCreate(surfaceControl: Long, debugName: String): Long
        external fun nCreateFromSurface(surface: Surface, debugName: String): Long
        external fun nRelease(surfaceControl: Long)

        external fun nTransactionCreate(): Long
        external fun nTransactionDelete(surfaceTransaction: Long)
        external fun nTransactionApply(surfaceTransaction: Long)
        external fun nTransactionSetOnComplete(
            surfaceTransaction: Long,
            listener: SurfaceControlCompat.TransactionCompletedListener
        )

        external fun nTransactionSetOnCommit(
            surfaceTransaction: Long,
            listener: SurfaceControlCompat.TransactionCommittedListener
        )

        external fun nExtractFenceFd(
            syncFence: SyncFenceCompat
        ): Int

        external fun nSetBuffer(
            surfaceTransaction: Long,
            surfaceControl: Long,
            hardwareBuffer: HardwareBuffer,
            acquireFieldFd: SyncFenceCompat
        )

        external fun nSetVisibility(
            surfaceTransaction: Long,
            surfaceControl: Long,
            visibility: Byte
        )

        external fun nSetZOrder(surfaceTransaction: Long, surfaceControl: Long, zOrder: Int)
        external fun nSetDamageRegion(
            surfaceTransaction: Long,
            surfaceControl: Long,
            rect: Rect?
        )

        external fun nSetDesiredPresentTime(
            surfaceTransaction: Long,
            desiredPresentTime: Long
        )

        init {
            System.loadLibrary("graphics-core")
        }
    }
}

/**
 * Handle to an on-screen Surface managed by the system compositor. By constructing
 * a [Surface] from this SurfaceControlCompat you can submit buffers to be composited. Using
 * [SurfaceControlCompat.Transaction] you can manipulate various properties of how the buffer will be
 * displayed on-screen. SurfaceControl's are arranged into a scene-graph like hierarchy, and
 * as such any SurfaceControl may have a parent. Geometric properties like transform, crop, and
 * Z-ordering will be inherited from the parent, as if the child were content in the parents
 * buffer stream.
 *
 * Compatibility class for [SurfaceControl]. This differs by being built upon the equivalent API
 * within the Android NDK. It introduces some APIs into earlier platform releases than what was
 * initially exposed for SurfaceControl.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class SurfaceControlCompat internal constructor(
    surface: Surface? = null,
    surfaceControl: SurfaceControlCompat? = null,
    debugName: String
) {
    private var mNativeSurfaceControl: Long = 0

    init {
        if (surface != null) {
            mNativeSurfaceControl = JniBindings.nCreateFromSurface(surface, debugName)
        } else if (surfaceControl != null) {
            mNativeSurfaceControl =
                JniBindings.nCreate(surfaceControl.mNativeSurfaceControl, debugName)
        }
        if (mNativeSurfaceControl == 0L) {
            throw IllegalArgumentException()
        }
    }

    /**
     * Callback interface for usage with [Transaction.addTransactionCompletedListener]
     */
    interface TransactionCompletedListener {
        /**
         * Invoked when a frame including the updates in a transaction was presented.
         *
         * Buffers which are replaced or removed from the scene in the transaction invoking
         * this callback may be reused after this point.
         *
         * @param latchTimeNanos Timestamp in nano seconds of when frame was latched by
         * the framework.
         *
         * @param presentTimeNanos  System time in nano seconds of when callback is called.
         */
        fun onComplete(latchTimeNanos: Long, presentTimeNanos: Long)
    }

    /**
     * Callback interface for usage with [Transaction.addTransactionCommittedListener]
     */
    interface TransactionCommittedListener {
        /**
         * Invoked when the transaction has been committed in SurfaceFlinger. This is when
         * the transaction is applied and the updates are ready to be presented.
         * This will be invoked before the Transaction.onComplete callback.
         *
         * This callback does not mean buffers have been released! It simply means that any new
         * transactions applied will not overwrite the transaction for which we are receiving
         * a callback and instead will be included in the next frame.
         *
         * @param latchTimeNanos Timestamp in nano seconds of when frame was latched by
         * the framework.
         *
         * @param presentTimeNanos System time in nano seconds of when callback is called.
         */
        fun onCommit(latchTimeNanos: Long, presentTimeNanos: Long)
    }

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
         * @param executor The executor that the callback should be invoked on.
         * This value cannot be null. Callback and listener events are dispatched
         * through this Executor, providing an easy way to control which thread is used.
         *
         * @param listener The callback that will be invoked when the transaction has
         * been completed. This value cannot be null.
         */
        @Suppress("PairedRegistration")
        fun addTransactionCompletedListener(
            executor: Executor,
            listener: TransactionCompletedListener
        ): Transaction {
            val listenerWrapper = object : TransactionCompletedListener {
                override fun onComplete(latchTimeNanos: Long, presentTimeNanos: Long) {
                    executor.execute { (listener::onComplete)(latchTimeNanos, presentTimeNanos) }
                }
            }

            JniBindings.nTransactionSetOnComplete(mNativeSurfaceTransaction, listenerWrapper)
            return this
        }

        /**
         * Sets the callback that is invoked once the updates from this transaction are
         * applied and ready to be presented. This callback is invoked before the
         * setOnCompleteListener callback.
         *
         * @param executor The executor that the callback should be invoked on.
         * This value cannot be null. Callback and listener events are dispatched
         * through this Executor, providing an easy way to control which thread is used.
         *
         * @param listener The callback that will be invoked when the transaction has
         * been completed. This value cannot be null.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        @Suppress("PairedRegistration")
        fun addTransactionCommittedListener(
            executor: Executor,
            listener: TransactionCommittedListener
        ): Transaction {
            val listenerWrapper = object : TransactionCommittedListener {
                override fun onCommit(latchTimeNanos: Long, presentTimeNanos: Long) {
                    executor.execute { (listener::onCommit)(latchTimeNanos, presentTimeNanos) }
                }
            }

            JniBindings.nTransactionSetOnCommit(mNativeSurfaceTransaction, listenerWrapper)
            return this
        }

        /**
         * Updates the [HardwareBuffer] displayed for the provided surfaceControl. Takes an
         * optional [SyncFenceCompat] that is signalled when all pending work for the buffer
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
            surfaceControl: SurfaceControlCompat,
            hardwareBuffer: HardwareBuffer,
            syncFence: SyncFenceCompat = SyncFenceCompat(-1)
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
         * Updates the visibility of the given [SurfaceControlCompat]. If visibility is set to
         * false, the [SurfaceControlCompat] and all surfaces in the subtree will be hidden.
         *
         * @param surfaceControl The SurfaceControl for which to set the visibility
         * This value cannot be null.
         *
         * @param visibility the new visibility. A value of true means the surface and its children
         * will be visible.
         */
        fun setVisibility(
            surfaceControl: SurfaceControlCompat,
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
         * Updates z order index for [SurfaceControlCompat]. Note that the z order for a
         * surface is relative to other surfaces that are siblings of this surface.
         * Behavior of siblings with the same z order is undefined.
         *
         * Z orders can range from Integer.MIN_VALUE to Integer.MAX_VALUE. Default z order
         * index is 0.
         *
         * @param surfaceControl surface control to set the z order of.
         *
         * @param zOrder desired layer z order to set the surfaceControl.
         */
        fun setLayer(surfaceControl: SurfaceControlCompat, zOrder: Int): Transaction {
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
            surfaceControl: SurfaceControlCompat,
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
         * Destroys the transaction object.
         */
        fun close() {
            if (mNativeSurfaceTransaction != 0L) {
                JniBindings.nTransactionDelete(mNativeSurfaceTransaction)
            }
            mNativeSurfaceTransaction = 0L
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
            (other?.javaClass != SurfaceControlCompat::class.java)
        ) {
            return false
        }

        other as SurfaceControlCompat
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
        JniBindings.nRelease(mNativeSurfaceControl)
        mNativeSurfaceControl = 0
    }

    protected fun finalize() {
        release()
    }

    /**
     * Builder class for [SurfaceControlCompat].
     *
     * Requires a parent surface. Debug name is default empty string.
     */
    class Builder private constructor(surface: Surface?, surfaceControl: SurfaceControlCompat?) {
        private var mSurface: Surface? = surface
        private var mSurfaceControl: SurfaceControlCompat? = surfaceControl
        private var mDebugName: String = ""

        constructor(surface: Surface) : this(surface, null) {}

        constructor(surfaceControl: SurfaceControlCompat) : this(null, surfaceControl) {}

        @Suppress("MissingGetterMatchingBuilder")
        fun setDebugName(debugName: String): Builder {
            mDebugName = debugName
            return this
        }

        /**
         * Builds the [SurfaceControlCompat] object
         */
        fun build(): SurfaceControlCompat {
            return SurfaceControlCompat(mSurface, mSurfaceControl, mDebugName)
        }
    }
}