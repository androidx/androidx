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
import android.os.Build
import android.view.AttachedSurfaceControl
import android.view.Surface
import android.view.SurfaceControl
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.hardware.SyncFenceCompat
import androidx.hardware.SyncFenceImpl
import androidx.graphics.surface.SurfaceControlCompat.TransactionCommittedListener
import java.util.concurrent.Executor

/**
 * Interface that abstracts the implementation of the [SurfaceControl] APIs depending on API level
 */
internal interface SurfaceControlImpl {

    /**
     * Check whether this instance points to a valid layer with the system-compositor.
     * For example this may be false if the layer was released ([release]).
     */
    fun isValid(): Boolean

    /**
     * Release the local reference to the server-side surface. The [Surface] may continue to exist
     * on-screen as long as its parent continues to exist. To explicitly remove a [Surface] from the
     * screen use [Transaction.reparent] with a null-parent. After release, [isValid] will return
     * false and other methods will throw an exception. Always call [release] when you are done with
     * a [SurfaceControlCompat] instance.
     */
    fun release()

    /**
     * Interface that abstracts the implementation of [SurfaceControl.Builder] APIs depending on
     * API level
     */
    interface Builder {

        /**
         * Set a parent [Surface] from the provided [SurfaceView] for our new
         * [SurfaceControlImpl]. Child surfaces are constrained to the onscreen region of their
         * parent. Furthermore they stack relatively in Z order, and inherit the transformation of
         * the parent.
         * @param surfaceView Target [SurfaceView] used to provide the [Surface] this
         * [SurfaceControlImpl] is associated with.
         */
        fun setParent(surfaceView: SurfaceView): Builder

        /**
         * Set a parent [SurfaceControlCompat] for the new [SurfaceControlCompat] instance.
         * Furthermore they stack relatively in Z order, and inherit the transformation of the
         * parent.
         * @param surfaceControl Target [SurfaceControlCompat] used as the parent for the newly
         * created [SurfaceControlCompat] instance
         */
        fun setParent(surfaceControl: SurfaceControlCompat): Builder

        /**
         * Set a debugging-name for the [SurfaceControlImpl].
         * @param name Debugging name configured on the [SurfaceControlCompat] instance.
         */
        fun setName(name: String): Builder

        /**
         * Construct a new [SurfaceControlImpl] with the set parameters.
         * The builder remains valid after the [SurfaceControlImpl] instance is created.
         */
        fun build(): SurfaceControlImpl
    }

    @JvmDefaultWithCompatibility
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    interface Transaction : AutoCloseable {

        /**
         * Indicates whether the surface must be considered opaque, even if its pixel format is
         * set to translucent. This can be useful if an application needs full RGBA 8888 support for
         * instance but will still draw every pixel opaque.
         * This flag only determines whether opacity will be sampled from the alpha channel.
         * Plane-alpha from calls to setAlpha() can still result in blended composition regardless
         * of the opaque setting. Combined effects are (assuming a buffer format with an alpha
         * channel):
         *
         * OPAQUE + alpha(1.0) == opaque composition
         * OPAQUE + alpha(0.x) == blended composition
         * OPAQUE + alpha(0.0) == no composition
         * !OPAQUE + alpha(1.0) == blended composition
         * !OPAQUE + alpha(0.x) == blended composition
         * !OPAQUE + alpha(0.0) == no composition
         * If the underlying buffer lacks an alpha channel, it is as if setOpaque(true) were set
         * automatically.
         *
         * @param surfaceControl Target [SurfaceControlCompat] to change the opaque flag for
         * @param isOpaque Flag indicating if the [SurfaceControlCompat] should be fully opaque or
         * transparent
         */
        fun setOpaque(surfaceControl: SurfaceControlImpl, isOpaque: Boolean): Transaction

        /**
         * Sets the visibility of a given Layer and it's sub-tree.
         * @param surfaceControl Target [SurfaceControlImpl]
         */
        fun setVisibility(surfaceControl: SurfaceControlImpl, visible: Boolean): Transaction

        /**
         * Re-parents a given [SurfaceControlImpl] to a new parent. Children inherit transform
         * (position, scaling) crop, visibility, and Z-ordering from their parents, as if the
         * children were pixels within the parent [Surface].
         * @param surfaceControl Target [SurfaceControlImpl] instance to reparent
         * @param newParent Parent [SurfaceControlImpl] that the target [SurfaceControlCompat]
         * instance is added to. This can be null indicating that the target [SurfaceControlCompat]
         * should be removed from the scene.
         */
        fun reparent(
            surfaceControl: SurfaceControlImpl,
            newParent: SurfaceControlImpl?
        ): Transaction

        /**
         * Re-parents a given [SurfaceControlImpl] to be a child of the [AttachedSurfaceControl].
         * Children inherit transform (position, scaling) crop, visibility, and Z-ordering from
         * their parents, as if the children were pixels within the parent [Surface].
         * @param surfaceControl Target [SurfaceControlImpl] instance to reparent
         * @param attachedSurfaceControl [AttachedSurfaceControl] instance that acts as the new
         * parent of the provided [SurfaceControlImpl] instance.
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun reparent(
            surfaceControl: SurfaceControlImpl,
            attachedSurfaceControl: AttachedSurfaceControl
        ): Transaction

        /**
         * Updates the [HardwareBuffer] displayed for the [SurfaceControlImpl]. Note that the
         * buffer must be allocated with [HardwareBuffer.USAGE_COMPOSER_OVERLAY] as well as
         * [HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE] as the surface control might be composited using
         * either an overlay or using the GPU. A presentation fence may be passed to improve
         * performance by allowing the buffer to complete rendering while it is waiting for the
         * transaction to be applied. For example, if the buffer is being produced by rendering with
         * OpenGL ES then a fence created with the eglDupNativeFenceFDANDROID EGL extension API
         * can be used to allow the GPU rendering to be concurrent with the transaction.
         * The compositor will wait for the fence to be signaled before the buffer is displayed.
         * If multiple buffers are set as part of the same transaction, the presentation fences of
         * all of them must signal before any buffer is displayed. That is, the entire transaction
         * is delayed until all presentation fences have signaled, ensuring the transaction remains
         * consistent.
         *
         * @param surfaceControl Target [SurfaceControlImpl] to configure the provided buffer.
         * @param buffer [HardwareBuffer] instance to be rendered by the [SurfaceControlImpl]
         * instance.
         * @param fence Optional [SyncFenceCompat] that serves as the presentation fence. If set,
         * the [SurfaceControlCompat.Transaction] will not apply until the fence signals.
         * @param releaseCallback Optional callback invoked when the buffer is ready for re-use
         * after being presented to the display.
         */
        fun setBuffer(
            surfaceControl: SurfaceControlImpl,
            buffer: HardwareBuffer,
            fence: SyncFenceImpl? = null,
            releaseCallback: (() -> Unit)? = null
        ): Transaction

        /**
         * Set the Z-order for a given [SurfaceControlImpl], relative to it's siblings.
         * If two siblings share the same Z order the ordering is undefined.
         * [Surface]s with a negative Z will be placed below the parent [Surface].
         */
        fun setLayer(
            surfaceControl: SurfaceControlImpl,
            z: Int
        ): Transaction

        /**
         * Request to add a [SurfaceControlCompat.TransactionCommittedListener]. The callback is
         * invoked when transaction is applied and the updates are ready to be presented. Once
         * applied, any callbacks added before the commit will be cleared from the Transaction.
         * This callback does not mean buffers have been released! It simply means that any new
         * transactions applied will not overwrite the transaction for which we are receiving a
         * callback and instead will be included in the next frame.
         * If you are trying to avoid dropping frames (overwriting transactions), and unable to
         * use timestamps (Which provide a more efficient solution), then this method provides a
         * method to pace your transaction application.
         * @param executor [Executor] to provide the thread the callback is invoked on.
         * @param listener [TransactionCommittedListener] instance that is invoked when the
         * transaction has been committed.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        fun addTransactionCommittedListener(
            executor: Executor,
            listener: TransactionCommittedListener
        ): Transaction

        /**
         * Updates the region for the content on this surface updated in this transaction. The
         * damage region is the area of the buffer that has changed since the previously
         * sent buffer. This can be used to reduce the amount of recomposition that needs to
         * happen when only a small region of the buffer is being updated, such as for a small
         * blinking cursor or a loading indicator.
         * @param surfaceControl Target [SurfaceControlImpl] to set damage region of.
         * @param region The region to be set. If null, the entire buffer is assumed dirty. This is
         * equivalent to not setting a damage region at all.
         */
        fun setDamageRegion(
            surfaceControl: SurfaceControlImpl,
            region: Region?
        ): Transaction

        /**
         * Set the alpha for a given surface. If the alpha is non-zero the SurfaceControl will
         * be blended with the Surfaces under it according to the specified ratio.
         * @param surfaceControl Target [SurfaceControlImpl] to set the alpha of.
         * @param alpha The alpha to set. Value is between 0.0 and 1.0 inclusive.
         */
        fun setAlpha(
            surfaceControl: SurfaceControlImpl,
            alpha: Float
        ): Transaction

        /**
         * Bounds the surface and its children to the bounds specified. Size of the surface
         * will be ignored and only the crop and buffer size will be used to determine the
         * bounds of the surface. If no crop is specified and the surface has no buffer,
         * the surface bounds is only constrained by the size of its parent bounds.
         *
         * @param surfaceControl The [SurfaceControlImpl] to apply the crop to. This value
         * cannot be null.
         *
         * @param crop Bounds of the crop to apply. This value can be null.
         *
         * @throws IllegalArgumentException if crop is not a valid rectangle.
         */
        fun setCrop(
            surfaceControl: SurfaceControlImpl,
            crop: Rect?
        ): Transaction

        /**
         * Sets the SurfaceControl to the specified position relative to the parent SurfaceControl
         *
         * @param surfaceControl The [SurfaceControlImpl] to change position. This value cannot
         * be null
         *
         * @param x the X position
         *
         * @param y the Y position
         */
        fun setPosition(
            surfaceControl: SurfaceControlImpl,
            x: Float,
            y: Float
        ): Transaction

        /**
         * Sets the SurfaceControl to the specified scale with (0, 0) as the
         * center point of the scale.
         *
         * @param surfaceControl The [SurfaceControlImpl] to change scale. This value cannot
         * be null.
         *
         * @param scaleX the X scale
         *
         * @param scaleY the Y scale
         */
        fun setScale(
            surfaceControl: SurfaceControlImpl,
            scaleX: Float,
            scaleY: Float
        ): Transaction

        /**
         * Sets the buffer transform that should be applied to the current buffer
         *
         * @param surfaceControl the [SurfaceControlImpl] to update. This value cannot be null.
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
        fun setBufferTransform(
            surfaceControl: SurfaceControlImpl,
            @SurfaceControlCompat.Companion.BufferTransform transformation: Int
        ): Transaction

        /**
         * See [SurfaceControlCompat.Transaction.setExtendedRangeBrightness]
         */
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun setExtendedRangeBrightness(
            surfaceControl: SurfaceControlImpl,
            currentBufferRatio: Float,
            desiredRatio: Float
        ): Transaction

        /**
         * See [SurfaceControlCompat.Transaction.setDataSpace]
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun setDataSpace(
            surfaceControl: SurfaceControlImpl,
            dataSpace: Int
        ): Transaction

        /**
         * Commit the transaction, clearing it's state, and making it usable as a new transaction.
         * This will not release any resources and [SurfaceControlImpl.Transaction.close] must be
         * called to release the transaction.
         */
        fun commit()

        /**
         * Release the native transaction object, without committing it.
         */
        override fun close()

        /**
         * Consume the passed in transaction, and request the View hierarchy to apply it atomically
         * with the next draw. This transaction will be merged with the buffer transaction from the
         * ViewRoot and they will show up on-screen atomically synced. This will not cause a draw to
         * be scheduled, and if there are no other changes to the View hierarchy you may need to
         * call View.invalidate()
         * @param attachedSurfaceControl [AttachedSurfaceControl] associated with the ViewRoot that
         * will apply the provided transaction on the next draw pass
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun commitTransactionOnDraw(attachedSurfaceControl: AttachedSurfaceControl)
    }
}