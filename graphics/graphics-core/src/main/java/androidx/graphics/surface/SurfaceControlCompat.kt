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
import android.hardware.DataSpace
import android.hardware.HardwareBuffer
import android.os.Build
import android.view.AttachedSurfaceControl
import android.view.Surface
import android.view.SurfaceControl
import android.view.SurfaceView
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.hardware.SyncFenceCompat
import java.util.concurrent.Executor

/**
 * Handle to an on-screen Surface managed by the system compositor. [SurfaceControlCompat] is a
 * combination of a buffer source, and metadata about how to display the buffers. By constructing a
 * [Surface] from this [SurfaceControl] you can submit buffers to be composited. Using
 * [SurfaceControlCompat.Transaction] you can manipulate various properties of how the buffer will
 * be displayed on-screen. [SurfaceControlCompat]s are arranged into a scene-graph like hierarchy,
 * and as such any [SurfaceControlCompat] may have a parent. Geometric properties like transform,
 * crop, and Z-ordering will be inherited from the parent, as if the child were content in the
 * parents buffer stream.
 *
 * This class differs slightly than [SurfaceControl] in that it backports some functionality
 * to Android R and above by delegating to the related APIs available in the NDK. For newer Android
 * versions, this leverages the equivalent [SurfaceControl] API available in the SDK
 */
@RequiresApi(Build.VERSION_CODES.Q)
class SurfaceControlCompat internal constructor(
    internal val scImpl: SurfaceControlImpl
) {

    /**
     * Constants for [Transaction.setBufferTransform].
     *
     * Various transformations that can be applied to a buffer.
     */
    companion object {
        @Suppress("AcronymName")
        @IntDef(
            value = [BUFFER_TRANSFORM_IDENTITY, BUFFER_TRANSFORM_MIRROR_HORIZONTAL,
                BUFFER_TRANSFORM_MIRROR_VERTICAL, BUFFER_TRANSFORM_ROTATE_180,
                BUFFER_TRANSFORM_ROTATE_90, BUFFER_TRANSFORM_ROTATE_270]
        )
        internal annotation class BufferTransform

        /**
         * The identity transformation. Maps a coordinate (x, y) onto itself.
         */
        const val BUFFER_TRANSFORM_IDENTITY = 0

        /**
         * Mirrors the buffer horizontally. Maps a point (x, y) to (-x, y)
         */
        const val BUFFER_TRANSFORM_MIRROR_HORIZONTAL = 1

        /**
         * Mirrors the buffer vertically. Maps a point (x, y) to (x, -y)
         */
        const val BUFFER_TRANSFORM_MIRROR_VERTICAL = 2

        /**
         * Rotates the buffer 180 degrees clockwise. Maps a point (x, y) to (-x, -y)
         */
        const val BUFFER_TRANSFORM_ROTATE_180 = 3

        /**
         * Rotates the buffer 90 degrees clockwise. Maps a point (x, y) to (-y, x)
         */
        const val BUFFER_TRANSFORM_ROTATE_90 = 4

        /**
         * Rotates the buffer 270 degrees clockwise. Maps a point (x, y) to (y, -x)
         */
        const val BUFFER_TRANSFORM_ROTATE_270 = 7
    }

    /**
     * Check whether this instance points to a valid layer with the system-compositor.
     * For example this may be false if the layer was released ([release]).
     */
    fun isValid(): Boolean = scImpl.isValid()

    /**
     * Release the local reference to the server-side surface. The [Surface] may continue to exist
     * on-screen as long as its parent continues to exist. To explicitly remove a [Surface] from the
     * screen use [Transaction.reparent] with a null-parent. After release, [isValid] will return
     * false and other methods will throw an exception. Always call [release] when you are done with
     * a [SurfaceControlCompat] instance.
     */
    fun release() {
        scImpl.release()
    }

    /**
     * Builder class for [SurfaceControlCompat] objects. By default the [Surface] will be hidden,
     * and have "unset" bounds, meaning it can be as large as the bounds of its parent if a buffer
     * or child so requires. It is necessary to set at least a name via [Builder.setName]
     */
    class Builder {

        private val mBuilderImpl = createImpl()

        /**
         * Set a parent [Surface] from the provided [SurfaceView] for our new
         * [SurfaceControlCompat]. Child surfaces are constrained to the onscreen region of their
         * parent. Furthermore they stack relatively in Z order, and inherit the transformation of
         * the parent.
         * @param surfaceView Target [SurfaceView] used to provide the [Surface] this
         * [SurfaceControlCompat] is associated with.
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun setParent(surfaceView: SurfaceView): Builder {
            mBuilderImpl.setParent(surfaceView)
            return this
        }

        /**
         * Set a parent [SurfaceControlCompat] for the new [SurfaceControlCompat] instance.
         * Furthermore they stack relatively in Z order, and inherit the transformation of the
         * parent.
         * @param surfaceControl Target [SurfaceControlCompat] used as the parent for the newly
         * created [SurfaceControlCompat] instance
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun setParent(surfaceControl: SurfaceControlCompat): Builder {
            mBuilderImpl.setParent(surfaceControl)
            return this
        }

        /**
         * Set a debugging-name for the [SurfaceControlCompat].
         * @param name Debugging name configured on the [SurfaceControlCompat] instance.
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun setName(name: String): Builder {
            mBuilderImpl.setName(name)
            return this
        }

        /**
         * Construct a new [SurfaceControlCompat] with the set parameters.
         * The builder remains valid after the [SurfaceControlCompat] instance is created.
         */
        fun build(): SurfaceControlCompat = SurfaceControlCompat(mBuilderImpl.build())

        internal companion object {
            @RequiresApi(Build.VERSION_CODES.Q)
            fun createImpl(): SurfaceControlImpl.Builder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    SurfaceControlVerificationHelper.createBuilderV33()
                } else {
                    SurfaceControlVerificationHelper.createBuilderV29()
                }
        }
    }

    /**
     * Interface to handle request to
     * [SurfaceControlV29.Transaction.addTransactionCompletedListener]
     */
    internal interface TransactionCompletedListener {
        /**
         * Invoked when a frame including the updates in a transaction was presented.
         *
         * Buffers which are replaced or removed from the scene in the transaction invoking
         * this callback may be reused after this point.
         */
        fun onTransactionCompleted()
    }

    /**
     * Interface to handle request to
     * [SurfaceControlCompat.Transaction.addTransactionCommittedListener]
     */
    interface TransactionCommittedListener {
        /**
         * Invoked when the transaction has been committed in SurfaceFlinger
         */
        fun onTransactionCommitted()
    }

    /**
     * An atomic set of changes to a set of [SurfaceControlCompat].
     */
    class Transaction : AutoCloseable {
        /**
         * internal mapping of buffer transforms used for testing purposes
         */
        internal val mBufferTransforms = HashMap<SurfaceControlCompat, Int>()

        private val mImpl = createImpl()

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
        fun setOpaque(surfaceControl: SurfaceControlCompat, isOpaque: Boolean): Transaction {
            mImpl.setOpaque(surfaceControl.scImpl, isOpaque)
            return this
        }

        /**
         * Sets the visibility of a given Layer and it's sub-tree.
         * @param surfaceControl Target [SurfaceControlCompat] to change the visibility
         * @param visible `true` to indicate the [SurfaceControlCompat] should be visible, `false`
         * otherwise
         */
        fun setVisibility(surfaceControl: SurfaceControlCompat, visible: Boolean): Transaction {
            mImpl.setVisibility(surfaceControl.scImpl, visible)
            return this
        }

        /**
         * Re-parents a given [SurfaceControlCompat] to a new parent. Children inherit transform
         * (position, scaling) crop, visibility, and Z-ordering from their parents, as if the
         * children were pixels within the parent [Surface].
         * @param surfaceControl Target [SurfaceControlCompat] instance to reparent
         * @param newParent Parent [SurfaceControlCompat] that the target [SurfaceControlCompat]
         * instance is added to. This can be null indicating that the target [SurfaceControlCompat]
         * should be removed from the scene.
         */
        fun reparent(
            surfaceControl: SurfaceControlCompat,
            newParent: SurfaceControlCompat?
        ): Transaction {
            mImpl.reparent(surfaceControl.scImpl, newParent?.scImpl)
            return this
        }

        /**
         * Re-parents a given [SurfaceControlCompat] to be a child of the [AttachedSurfaceControl].
         * Children inherit transform (position, scaling) crop, visibility, and Z-ordering from
         * their parents, as if the children were pixels within the parent [Surface].
         * @param surfaceControl Target [SurfaceControlCompat] instance to reparent
         * @param attachedSurfaceControl [AttachedSurfaceControl] instance that acts as the new
         * parent of the provided [SurfaceControlCompat] instance.
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun reparent(
            surfaceControl: SurfaceControlCompat,
            attachedSurfaceControl: AttachedSurfaceControl
        ): Transaction {
            mImpl.reparent(surfaceControl.scImpl, attachedSurfaceControl)
            return this
        }

        /**
         * Updates the [HardwareBuffer] displayed for the [SurfaceControlCompat]. Note that the
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
         * @param surfaceControl Target [SurfaceControlCompat] to configure the provided buffer.
         * @param buffer [HardwareBuffer] instance to be rendered by the [SurfaceControlCompat]
         * instance.
         * @param fence Optional [SyncFenceCompat] that serves as the presentation fence. If set,
         * the [SurfaceControlCompat.Transaction] will not apply until the fence signals.
         * @param releaseCallback Optional callback invoked when the buffer is ready for re-use
         * after being presented to the display.
         */
        @JvmOverloads
        fun setBuffer(
            surfaceControl: SurfaceControlCompat,
            buffer: HardwareBuffer,
            fence: SyncFenceCompat? = null,
            releaseCallback: (() -> Unit)? = null
        ): Transaction {
            mImpl.setBuffer(surfaceControl.scImpl, buffer, fence?.mImpl, releaseCallback)
            return this
        }

        /**
         * Set the Z-order for a given [SurfaceControlCompat], relative to it's siblings.
         * If two siblings share the same Z order the ordering is undefined.
         * [Surface]s with a negative Z will be placed below the parent [Surface].
         */
        fun setLayer(
            surfaceControl: SurfaceControlCompat,
            z: Int
        ): Transaction {
            mImpl.setLayer(surfaceControl.scImpl, z)
            return this
        }

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
        @Suppress("PairedRegistration")
        @RequiresApi(Build.VERSION_CODES.S)
        fun addTransactionCommittedListener(
            executor: Executor,
            listener: TransactionCommittedListener
        ): Transaction {
            mImpl.addTransactionCommittedListener(executor, listener)
            return this
        }

        /**
         * Updates the region for the content on this surface updated in this transaction. The
         * damage region is the area of the buffer that has changed since the previously
         * sent buffer. This can be used to reduce the amount of recomposition that needs to
         * happen when only a small region of the buffer is being updated, such as for a small
         * blinking cursor or a loading indicator.
         * @param surfaceControl Target [SurfaceControlCompat] to set damage region of.
         * @param region The region to be set. If null, the entire buffer is assumed dirty. This is
         * equivalent to not setting a damage region at all.
         */
        fun setDamageRegion(
            surfaceControl: SurfaceControlCompat,
            region: Region?
        ): Transaction {
            mImpl.setDamageRegion(surfaceControl.scImpl, region)
            return this
        }

        /**
         * Set the alpha for a given surface. If the alpha is non-zero the SurfaceControl will
         * be blended with the Surfaces under it according to the specified ratio.
         * @param surfaceControl Target [SurfaceControlCompat] to set the alpha of.
         * @param alpha The alpha to set. Value is between 0.0 and 1.0 inclusive.
         */
        fun setAlpha(
            surfaceControl: SurfaceControlCompat,
            alpha: Float
        ): Transaction {
            mImpl.setAlpha(surfaceControl.scImpl, alpha)
            return this
        }

        /**
         * Bounds the surface and its children to the bounds specified. Size of the surface
         * will be ignored and only the crop and buffer size will be used to determine the
         * bounds of the surface. If no crop is specified and the surface has no buffer,
         * the surface bounds is only constrained by the size of its parent bounds.
         *
         * @param surfaceControl The [SurfaceControlCompat] to apply the crop to. This value
         * cannot be null.
         *
         * @param crop Bounds of the crop to apply. This value can be null.
         *
         * @throws IllegalArgumentException if crop is not a valid rectangle.
         */
        fun setCrop(
            surfaceControl: SurfaceControlCompat,
            crop: Rect?
        ): Transaction {
            mImpl.setCrop(surfaceControl.scImpl, crop)
            return this
        }

        /**
         * Sets the SurfaceControl to the specified position relative to the parent SurfaceControl
         *
         * @param surfaceControl The [SurfaceControlCompat] to change position. This value cannot
         * be null
         *
         * @param x the X position
         *
         * @param y the Y position
         */
        fun setPosition(
            surfaceControl: SurfaceControlCompat,
            x: Float,
            y: Float
        ): Transaction {
            mImpl.setPosition(surfaceControl.scImpl, x, y)
            return this
        }

        /**
         * Sets the SurfaceControl to the specified scale with (0, 0) as the
         * center point of the scale.
         *
         * @param surfaceControl The [SurfaceControlCompat] to change scale. This value cannot
         * be null.
         *
         * @param scaleX the X scale
         *
         * @param scaleY the Y scale
         */
        fun setScale(
            surfaceControl: SurfaceControlCompat,
            scaleX: Float,
            scaleY: Float
        ): Transaction {
            mImpl.setScale(surfaceControl.scImpl, scaleX, scaleY)
            return this
        }

        /**
         * Sets the buffer transform that should be applied to the current buffer
         *
         * @param surfaceControl the [SurfaceControlCompat] to update. This value cannot be null.
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
            surfaceControl: SurfaceControlCompat,
            @BufferTransform transformation: Int
        ): Transaction {
            mBufferTransforms[surfaceControl] = transformation
            mImpl.setBufferTransform(surfaceControl.scImpl, transformation)
            return this
        }

        /**
         * Sets the desired extended range brightness for the layer. This only applies for layers
         * that are displaying [HardwareBuffer] instances with a DataSpace of
         * [DataSpace.RANGE_EXTENDED].
         *
         * @param surfaceControl The layer whose extended range brightness is being specified
         * @param currentBufferRatio The current hdr/sdr ratio of the current buffer. For example
         * if the buffer was rendered with a target SDR whitepoint of 100 nits and a max display
         * brightness of 200 nits, this should be set to 2.0f.
         *
         * Default value is 1.0f.
         *
         * Transfer functions that encode their own brightness ranges,
         * such as HLG or PQ, should also set this to 1.0f and instead
         * communicate extended content brightness information via
         * metadata such as CTA861_3 or SMPTE2086.
         *
         * Must be finite && >= 1.0f
         *
         * @param desiredRatio The desired hdr/sdr ratio. This can be used to communicate the max
         * desired brightness range. This is similar to the "max luminance" value in other HDR
         * metadata formats, but represented as a ratio of the target SDR whitepoint to the max
         * display brightness. The system may not be able to, or may choose not to, deliver the
         * requested range.
         *
         * While requesting a large desired ratio will result in the most
         * dynamic range, voluntarily reducing the requested range can help
         * improve battery life as well as can improve quality by ensuring
         * greater bit depth is allocated to the luminance range in use.
         *
         * Default value is 1.0f and indicates that extended range brightness
         * is not being used, so the resulting SDR or HDR behavior will be
         * determined entirely by the dataspace being used (ie, typically SDR
         * however PQ or HLG transfer functions will still result in HDR)
         *
         * Must be finite && >= 1.0f
         * @return this
         **/
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun setExtendedRangeBrightness(
            surfaceControl: SurfaceControlCompat,
            @FloatRange(from = 1.0, fromInclusive = true) currentBufferRatio: Float,
            @FloatRange(from = 1.0, fromInclusive = true) desiredRatio: Float
        ): Transaction {
            mImpl.setExtendedRangeBrightness(
                surfaceControl.scImpl,
                currentBufferRatio,
                desiredRatio
            )
            return this
        }

        /**
         * Set the dataspace for the SurfaceControl. This will control how the buffer
         * set with [setBuffer] is displayed.
         *
         * @param surfaceControl The SurfaceControl to update
         * @param dataSpace The dataspace to set it to
         * @return this
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun setDataSpace(
            surfaceControl: SurfaceControlCompat,
            dataSpace: Int
        ): Transaction {
            mImpl.setDataSpace(surfaceControl.scImpl, dataSpace)
            return this
        }

        /**
         * Commit the transaction, clearing it's state, and making it usable as a new transaction.
         * This will not release any resources and [SurfaceControlCompat.Transaction.close] must be
         * called to release the transaction.
         */
        fun commit() {
            mBufferTransforms.clear()
            mImpl.commit()
        }

        /**
         * Release the native transaction object, without committing it.
         */
        override fun close() {
            mImpl.close()
        }

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
        fun commitTransactionOnDraw(attachedSurfaceControl: AttachedSurfaceControl) {
            mImpl.commitTransactionOnDraw(attachedSurfaceControl)
        }

        internal companion object {
            @RequiresApi(Build.VERSION_CODES.Q)
            fun createImpl(): SurfaceControlImpl.Transaction =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    SurfaceControlVerificationHelper.createTransactionV33()
                } else {
                    SurfaceControlVerificationHelper.createTransactionV29()
                }
        }
    }
}

/**
 * Helper class to avoid class verification failures
 */
internal class SurfaceControlVerificationHelper private constructor() {

    companion object {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @androidx.annotation.DoNotInline
        fun createBuilderV33(): SurfaceControlImpl.Builder = SurfaceControlV33.Builder()

        @RequiresApi(Build.VERSION_CODES.Q)
        @androidx.annotation.DoNotInline
        fun createBuilderV29(): SurfaceControlImpl.Builder = SurfaceControlV29.Builder()

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @androidx.annotation.DoNotInline
        fun createTransactionV33(): SurfaceControlImpl.Transaction = SurfaceControlV33.Transaction()

        @RequiresApi(Build.VERSION_CODES.Q)
        @androidx.annotation.DoNotInline
        fun createTransactionV29(): SurfaceControlImpl.Transaction = SurfaceControlV29.Transaction()
    }
}