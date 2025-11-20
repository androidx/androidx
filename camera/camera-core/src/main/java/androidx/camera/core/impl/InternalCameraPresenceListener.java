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

package androidx.camera.core.impl;


import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * A listener for being notified of the final, filtered set of available cameras.
 */
public interface InternalCameraPresenceListener {

    /**
     * Called when the list of available and filtered camera IDs has been updated.
     *
     * <p>Implementations should reconcile their internal state with this new, complete list of IDs.
     * If the update cannot be completed successfully, an exception should be thrown to trigger
     * a rollback.
     *
     * @param cameraIds The complete, ordered list of camera IDs that are currently available and
     *                  compatible.
     * @throws CameraUpdateException if the listener fails to update its internal state.
     */
    void onCamerasUpdated(@NonNull List<String> cameraIds) throws CameraUpdateException;
}
