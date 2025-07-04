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

import android.os.Build
import android.view.Surface
import android.view.SurfaceControl
import android.view.SurfaceView
import androidx.annotation.RequiresApi

/**
 * Compat class to avoid [VerifyError] when using [SurfaceControl] on API < 29.
 *
 * This wraps [SurfaceControl] on API >= 29 and is a no-op stub on API < 29.
 */
sealed interface SurfaceControlCompat {

    /** Create a new Surface from this SurfaceControl, or return null if this is a stub. */
    fun newSurface(): Surface?

    /** Sets the buffer size of this surface control. */
    fun setBufferSize(width: Int, height: Int)

    /** Release this surface control. */
    fun release()

    /** Reparent the surface control to null. */
    fun detach()

    /**
     * Reparents this surface control to a new parent [SurfaceControlCompat].
     *
     * On older API levels, this is a no-op and will return `false`.
     *
     * @param newParent The new parent [SurfaceControlCompat].
     * @return `true` if the reparent operation was performed, `false` otherwise.
     */
    fun reparent(newParent: SurfaceControlCompat): Boolean

    companion object {
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
        fun create(
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
         * @return a compat implementation of [SurfaceControlCompat].
         */
        @JvmStatic
        fun create(
            parent: SurfaceControlCompat,
            width: Int,
            height: Int,
            name: String,
        ): SurfaceControlCompat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SurfaceControlApi29Impl(parent, width, height, name)
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
        fun wrap(surfaceView: SurfaceView): SurfaceControlCompat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SurfaceControlApi29Impl(surfaceControl = surfaceView.surfaceControl)
            } else {
                SurfaceControlStub
            }
    }

    /** API 29+ implementation of [SurfaceControlCompat]. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private class SurfaceControlApi29Impl(
        // Primary constructor now wraps an existing SurfaceControl
        private val surfaceControl: SurfaceControl
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
        ) : this( // Calls the primary constructor with the newly built SurfaceControl
            SurfaceControl.Builder()
                .setName(name)
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
            SurfaceControl.Transaction().use { transaction ->
                transaction.setVisibility(this.surfaceControl, true).apply()
            }
        }

        override fun newSurface(): Surface? {
            return Surface(surfaceControl)
        }

        override fun setBufferSize(width: Int, height: Int) {
            SurfaceControl.Transaction().use { transaction ->
                transaction.setBufferSize(surfaceControl, width, height).apply()
            }
        }

        override fun release() {
            surfaceControl.release()
        }

        override fun detach() {
            SurfaceControl.Transaction().use { transaction ->
                transaction.reparent(surfaceControl, null).apply()
            }
        }

        override fun reparent(newParent: SurfaceControlCompat): Boolean {
            if (surfaceControl.isValid) {
                SurfaceControl.Transaction().use { transaction ->
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
    }
}
