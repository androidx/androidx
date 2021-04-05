/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.interop;

import static java.lang.annotation.RetentionPolicy.CLASS;

import androidx.annotation.RequiresOptIn;

import java.lang.annotation.Retention;

/**
 * Denotes that the annotated method uses the experimental methods which allow direct access to
 * camera2 classes.
 *
 * <p>The Camera2Interop and Camera2Interop.Extender exposes the underlying instances of camera2
 * classes such CameraDevice.StateCallback, CameraCaptureSession.StateCallback and
 * CameraCaptureSession.CaptureCallback. In addition the configs allow setting of camera2
 * CaptureRequest parameters. However, CameraX does not provide any guarantee on how it operates
 * on these parameters. The ordering and number of times these objects might in order to best
 * optimize the top level behavior.
 *
 * <p>The values from the callbacks should only be read. Methods that modify the CameraDevice or
 * CameraCaptureSession will likely move CameraX into an inconsistent internal state.
 *
 * <p>These will be changed in future release possibly, hence add @Experimental annotation.
 */
@Retention(CLASS)
@RequiresOptIn
public @interface ExperimentalCamera2Interop {
}
