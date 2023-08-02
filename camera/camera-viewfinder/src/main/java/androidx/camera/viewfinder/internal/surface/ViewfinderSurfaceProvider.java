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

package androidx.camera.viewfinder.internal.surface;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.viewfinder.ViewfinderSurfaceRequest;

/**
 * A interface implemented by the application to provide a {@link Surface} for viewfinder.
 *
 * <p> This interface is implemented by the application to provide a {@link Surface}. This
 * will be called by application when it needs a Surface for viewfinder. It also signals when the
 * Surface is no longer in use.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ViewfinderSurfaceProvider {
    /**
     * Called when a new {@link Surface} has been requested by the camera.
     *
     * <p>This is called every time a new surface is required to keep the viewfinder running.
     * The camera may repeatedly request surfaces, but only a single request will be active at a
     * time.
     *
     * @param request the request for a surface which contains the requirements of the
     *                surface and methods for completing the request.
     */
    void onSurfaceRequested(@NonNull ViewfinderSurfaceRequest request);
}
