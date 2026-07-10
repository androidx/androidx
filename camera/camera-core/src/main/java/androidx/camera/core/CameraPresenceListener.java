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

package androidx.camera.core;


import org.jspecify.annotations.NonNull;

import java.util.Set;

/**
 * A listener for changes in camera presence, indicating when cameras are added to or removed
 * from the set of devices that are recognized by the system and usable by CameraX.
 *
 * <p>This listener reports on changes to the set of cameras that CameraX determines can be bound
 * and used. This "usable" set is derived by filtering the list of all available cameras
 * based on several criteria:
 * <ul>
 * <li>The cameras reported as available by the device's framework.</li>
 * <li>The camera meeting the minimum requirements for use with the CameraX library.</li>
 * <li>The application of any {@link CameraSelector} limiters configured to CameraX during
 * initialization.</li>
 * </ul>
 *
 * <p><b>Important:</b> This listener focuses on the <b>presence</b> of usable cameras, not their
 * temporary <b>availability</b> due to other apps using them. It does <b>not</b> report temporary
 * changes in availability, such as when a camera becomes temporarily in-use by another
 * application. For observing those states on an open camera, see {@link CameraState}.
 *
 * <p>Register this listener via
 * {@link androidx.camera.lifecycle.ProcessCameraProvider#addCameraPresenceListener} or
 * {@link androidx.camera.lifecycle.LifecycleCameraProvider#addCameraPresenceListener}.
 */
public interface CameraPresenceListener {
    /**
     * Called when one or more new cameras have been added to the list of usable cameras.
     *
     * <p>On initial registration of the listener, **if there are any cameras currently present
     * and usable**, this method will be called once with a set containing all of them.
     * Subsequent calls will only contain cameras that have been newly added since the last
     * update.
     *
     * <p>Upon receiving this callback, an application can iterate through the provided set and may
     * choose to switch to one of the new cameras. A specific {@link CameraIdentifier} can then be
     * used with {@link CameraSelector#of(CameraIdentifier...)} to create a precise selector for
     * binding.
     *
     * @param cameraIdentifiers The non-empty set of identifiers for the cameras that were added.
     */
    void onCamerasAdded(@NonNull Set<CameraIdentifier> cameraIdentifiers);

    /**
     * Called when one or more previously usable cameras have been removed from the system.
     *
     * <p><b>Important:</b> When this callback is invoked, the cameras it refers to are no longer
     * operational. Their corresponding {@link Camera}, {@link CameraInfo}, and
     * {@link CameraControl} objects should be considered invalid. The provided identifiers
     * should be used as keys to identify which cameras were removed.
     *
     * <p><b>Action:</b> If any of the removed cameras were in use, the application should unbind
     * all {@link UseCase}s that were bound to the removed camera. This is necessary to release
     * resources and allow the {@code UseCase}s to be bound to another camera. Afterward, the
     * application can attempt to switch to another available camera or update its UI to reflect
     * that the camera is no longer present.
     *
     * <p><b>Event Order Guarantee:</b> If any of the removed cameras were currently open, their
     * {@link CameraState} will have already transitioned to {@link CameraState.Type#CLOSED} with an
     * error of {@link CameraState#ERROR_CAMERA_REMOVED} <em>before</em> this callback is invoked.
     *
     * @param cameraIdentifiers The non-empty set of identifiers for the cameras that were removed.
     */
    void onCamerasRemoved(@NonNull Set<CameraIdentifier> cameraIdentifiers);
}
