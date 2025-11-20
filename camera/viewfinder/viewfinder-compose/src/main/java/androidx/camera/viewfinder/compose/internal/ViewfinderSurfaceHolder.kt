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

package androidx.camera.viewfinder.compose.internal

import android.view.Surface
import androidx.camera.viewfinder.core.impl.RefCounted

/**
 * Manages ownership and lifecycle of a [Surface] used by the Viewfinder.
 *
 * This interface facilitates the sharing and management of the underlying [Surface]. It provides
 * access to a [RefCounted] [Surface], ensuring that the surface is released only when all clients
 * have finished using it.
 *
 * It also allows the [Surface] to be explicitly detached from the view that originally created or
 * provided it, which is important for proper resource cleanup and lifecycle management, especially
 * when the view might be destroyed before the surface is no longer needed by other components.
 */
internal interface ViewfinderSurfaceHolder {
    /**
     * Provides the reference-counted [Surface].
     *
     * Clients should increment the reference count when they start using the surface and decrement
     * it when they are done. The surface will be released when its reference count drops to zero.
     */
    val refCountedSurface: RefCounted<Surface>

    /**
     * Detaches the [Surface] from its originating view or source.
     *
     * This typically involves operations like reparenting a [android.view.SurfaceControl] or
     * signaling that the surface is no longer tied to the view's lifecycle. This method should be
     * called to ensure proper cleanup when the view destroys the surface.
     */
    fun detach()
}
