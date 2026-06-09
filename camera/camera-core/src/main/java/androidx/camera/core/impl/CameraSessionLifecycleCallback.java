/*
 * Copyright 2026 The Android Open Source Project
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

/** A callback for tracking logical camera capture session lifecycle events. */
public abstract class CameraSessionLifecycleCallback {

    /** Called when the camera capture session is successfully configured and started. */
    public void onSessionStarted() {}

    /** Called when the camera capture session encounters a configuration error. */
    public void onSessionError() {}

    /** Called when the camera capture session is stopped. */
    public void onSessionStopped() {}
}
