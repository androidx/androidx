/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.viewfinder.core.impl

import android.graphics.PixelFormat
import android.os.Build
import android.view.Surface
import android.view.SurfaceControl
import android.view.SurfaceView
import android.view.View
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting

/**
 * Compat class to avoid [VerifyError] when using [SurfaceControl] on API < 29.
 *
 * This wraps [SurfaceControl] on API >= 29 and is a no-op stub on API < 29.
 */
public sealed interface SurfaceControlCompat {

    /** Create a new Surface from this SurfaceControl, or return null if this is a stub. */
    public fun newSurface(): Surface?

    /** Sets the buffer size of this surface control. */
    public fun setBufferSize(width: Int, height: Int)

    /** Release this surface control. */
    public fun release()

    /** Reparent the surface control to null. */
    public fun detach()

    /**
     * Reparents this surface control to a new parent [SurfaceControlCompat].
     *
     * On older API levels, this is a no-op and will return `false`.
     *
     * @param newParent The new parent [SurfaceControlCompat].
     * @return `true` if the reparent operation was performed, `false` otherwise.
     */
    public fun reparent(newParent: SurfaceControlCompat): Boolean

    /**
     * Rotates the content of this surface control clockwise by [rotationDegrees] and scales the
     * rotated content to fit the original `width x height` bounds in the parent's coordinate space.
     *
     * For 90 and 270 degree rotations this squeezes the content along each axis. Callers are
     * expected to undo the squeeze with a matching non-uniform scale on an ancestor, the same way
     * [Transformations.getTextureViewCorrectionMatrix] is used for a `TextureView`.
     *
     * When [syncView] is provided on API 31+, the change is applied with the next frame drawn by
     * the root of [syncView], so that it lands on the same frame as any View-level transformation
     * changes. Otherwise, the change is applied immediately. On API 29 to 30, this means a change
     * may become visible up to a frame before or after a View-level transformation made in the same
     * frame.
     *
     * On older API levels, this is a no-op and will return `false`.
     *
     * @param rotationDegrees The clockwise rotation in degrees. Must be a multiple of 90.
     * @param width The width of the content of this surface control.
     * @param height The height of the content of this surface control.
     * @param syncView An optional view to synchronize the change with.
     * @return `true` if the rotation was applied, `false` otherwise.
     */
    public fun setContentRotation(
        rotationDegrees: Int,
        width: Int,
        height: Int,
        syncView: View?,
    ): Boolean

    public companion object {
        /**
         * Creates a SurfaceControl or a stub implementation.
         *
         * This method creates a *new* SurfaceControl that is parented to the provided
         * [SurfaceView].
         *
         * @param parent The SurfaceView to use as a parent.
         * @param format The format to use for the SurfaceControl (on newer APIs).
         * @param width The width to set on the SurfaceControl or the Surface.
         * @param height The height to set on the SurfaceControl or the Surface.
         * @param name The name of the SurfaceControl to create.
         * @return a compat implementation of [SurfaceControlCompat].
         */
        @JvmStatic
        public fun create(
            parent: SurfaceView,
            format: Int,
            width: Int,
            height: Int,
            name: String,
        ): SurfaceControlCompat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SurfaceControlApi29Impl(parent, format, width, height, name)
            } else {
                SurfaceControlStub
            }

        /**
         * Creates a SurfaceControl or a stub implementation.
         *
         * This method creates a *new* SurfaceControl that is parented to another
         * [SurfaceControlCompat].
         *
         * @param parent The SurfaceControlCompat to use as a parent.
         * @param width The width to set on the SurfaceControl or the Surface.
         * @param height The height to set on the SurfaceControl or the Surface.
         * @param name The name of the SurfaceControl to create.
         * @param format The format to use for the SurfaceControl (on newer APIs).
         * @return a compat implementation of [SurfaceControlCompat].
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            parent: SurfaceControlCompat,
            width: Int,
            height: Int,
            name: String,
            format: Int = PixelFormat.OPAQUE,
        ): SurfaceControlCompat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SurfaceControlApi29Impl(parent, width, height, name, format)
            } else {
                SurfaceControlStub
            }

        /**
         * Wraps a SurfaceView's SurfaceControl if available.
         *
         * This method wraps an *existing* SurfaceControl obtained from the provided [SurfaceView].
         * It does not create a new SurfaceControl.
         *
         * @param surfaceView The SurfaceView to wrap the SurfaceControl from.
         * @return a compat implementation of [SurfaceControlCompat] or a stub if the SurfaceView
         *   does not have a SurfaceControl.
         */
        @JvmStatic
        public fun wrap(surfaceView: SurfaceView): SurfaceControlCompat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SurfaceControlApi29Impl(surfaceControl = surfaceView.surfaceControl)
            } else {
                SurfaceControlStub
            }

        @RequiresApi(Build.VERSION_CODES.Q)
        @VisibleForTesting
        @JvmSynthetic
        internal fun wrap(
            surfaceView: SurfaceView,
            newTransaction: () -> SurfaceControl.Transaction,
        ): SurfaceControlCompat =
            SurfaceControlApi29Impl(
                surfaceControl = surfaceView.surfaceControl,
                newTransaction = newTransaction,
            )
    }

    /** API 29+ implementation of [SurfaceControlCompat]. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private class SurfaceControlApi29Impl(
        // Primary constructor now wraps an existing SurfaceControl
        private val surfaceControl: SurfaceControl,
        private val newTransaction: () -> SurfaceControl.Transaction = {
            SurfaceControl.Transaction()
        },
    ) : SurfaceControlCompat {
        // Secondary constructor for creating a new SurfaceControl with a parent SurfaceView
        constructor(
            parent: SurfaceView,
            format: Int,
            width: Int,
            height: Int,
            name: String,
        ) : this( // Calls the primary constructor with the newly built SurfaceControl
            SurfaceControl.Builder()
                .setName(name)
                .setFormat(format)
                .setBufferSize(width, height)
                .setParent(parent.surfaceControl)
                .build()
        ) {
            initializeNewSurfaceControl()
        }

        // Secondary constructor for creating a new SurfaceControl with a parent
        // SurfaceControlCompat
        constructor(
            parent: SurfaceControlCompat,
            width: Int,
            height: Int,
            name: String,
            format: Int,
        ) : this( // Calls the primary constructor with the newly built SurfaceControl
            SurfaceControl.Builder()
                .setName(name)
                .setFormat(format)
                .setBufferSize(width, height)
                .setParent((parent as SurfaceControlApi29Impl).surfaceControl)
                .build()
        ) {
            // Call the common initialization logic here
            initializeNewSurfaceControl()
        }

        /**
         * Contains initialization logic that should only run when a new SurfaceControl is created.
         */
        private fun initializeNewSurfaceControl() {
            newTransaction().use { transaction ->
                transaction.setVisibility(this.surfaceControl, true).apply()
            }
        }

        override fun newSurface(): Surface? {
            return Surface(surfaceControl)
        }

        override fun setBufferSize(width: Int, height: Int) {
            newTransaction().use { transaction ->
                transaction.setBufferSize(surfaceControl, width, height).apply()
            }
        }

        override fun release() {
            surfaceControl.release()
        }

        override fun detach() {
            newTransaction().use { transaction ->
                transaction.reparent(surfaceControl, null).apply()
            }
        }

        override fun reparent(newParent: SurfaceControlCompat): Boolean {
            if (surfaceControl.isValid) {
                newTransaction().use { transaction ->
                    transaction
                        .reparent(
                            surfaceControl,
                            (newParent as SurfaceControlApi29Impl).surfaceControl,
                        )
                        .apply()
                }
                return true
            }
            return false
        }

        override fun setContentRotation(
            rotationDegrees: Int,
            width: Int,
            height: Int,
            syncView: View?,
        ): Boolean {
            if (!surfaceControl.isValid) {
                return false
            }
            val geometry = ContentRotationGeometry.create(rotationDegrees, width, height)
            val transaction = newTransaction()
            // setGeometry() is used because setBufferTransform() only transforms the buffer of the
            // layer itself, and this layer has no buffer. The geometry of a layer applies to its
            // children and is preserved across frames.
            @Suppress("DEPRECATION")
            transaction.setGeometry(
                surfaceControl,
                geometry.sourceCrop,
                geometry.destinationFrame,
                geometry.transform,
            )
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    syncView != null &&
                    Api31Impl.applyTransactionOnDraw(syncView, transaction)
            ) {
                // On API 31-33, ViewRootImpl can retain the Transaction and merge it later on
                // RenderThread, so it must not be closed here; it is freed through a
                // NativeAllocationRegistry once garbage collected. On API 34+, it is merged
                // synchronously.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    transaction.close()
                }
                syncView.invalidate()
            } else {
                transaction.apply()
                transaction.close()
            }
            return true
        }
    }

    /** API 31+ helper to avoid class verification failures on API 29-30. */
    @RequiresApi(Build.VERSION_CODES.S)
    private object Api31Impl {
        @JvmStatic
        fun applyTransactionOnDraw(view: View, transaction: SurfaceControl.Transaction): Boolean =
            view.rootSurfaceControl?.applyTransactionOnDraw(transaction) == true
    }

    /** Stub implementation of [SurfaceControlCompat] for older APIs. */
    private object SurfaceControlStub : SurfaceControlCompat {
        override fun newSurface(): Surface? = null

        override fun setBufferSize(width: Int, height: Int) {
            // No-op for older APIs
        }

        override fun release() {
            // No-op for older APIs
        }

        override fun detach() {
            // No-op for older APIs
        }

        override fun reparent(newParent: SurfaceControlCompat) = false // No-op for older APIs

        override fun setContentRotation(
            rotationDegrees: Int,
            width: Int,
            height: Int,
            syncView: View?,
        ) = false // No-op for older APIs
    }
}
