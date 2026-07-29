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

package androidx.camera.camera2.interop

import androidx.camera.core.CameraXDsl
import androidx.camera.core.impl.MutableConfig

/**
 * Composite target interop interfaces for specific CameraX components.
 *
 * **Note:** Using Camera2 interop options can override internal CameraX configurations. If an
 * option configured via interop conflicts with options required by CameraX internally, the option
 * from Camera2Interop will override, which may result in unexpected behavior or interfere with
 * CameraX functionality.
 */

// =========================================================================================
// Inherited Target Interop Interfaces
// =========================================================================================

/**
 * Configures Camera2 options on a [androidx.camera.core.UseCase].
 *
 * Provides setters for output stream configuration options, such as physical camera ID, stream use
 * case, mirror mode, timestamp base, dynamic range profile, and surface group ID.
 */
@CameraXDsl
public interface UseCaseCamera2Interop : OutputConfigurationInterop<UseCaseCamera2Interop>

/**
 * Configures Camera2 options on an [androidx.camera.core.ImageCapture] use case.
 *
 * Provides setters for output stream options and still capture request settings (such as still
 * capture request keys, template types, and capture callbacks). The capture request keys for
 * one-shot still captures (such as [androidx.camera.core.ImageCapture.takePicture]) are determined
 * by copying all repeating request keys (which may include keys added via
 * [androidx.camera.core.SessionConfig.Builder.setInterop] or
 * [androidx.camera.core.CameraControl.applyInteropAsync]) and then overriding them with the still
 * capture request keys configured here.
 *
 * **Warning:** Callbacks configured on this target (such as
 * [android.hardware.camera2.CameraCaptureSession.CaptureCallback]) receive raw
 * [android.hardware.camera2.CameraCaptureSession] instances. Directly invoking state-altering
 * methods on these raw objects (such as [android.hardware.camera2.CameraCaptureSession.close] or
 * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
 * management and may cause state desynchronization, stream interruption, or application crashes.
 */
@CameraXDsl
public interface ImageCaptureCamera2Interop :
    OutputConfigurationInterop<ImageCaptureCamera2Interop>,
    StillCaptureInterop<ImageCaptureCamera2Interop>

/**
 * Configures Camera2 options on a [androidx.camera.core.SessionConfig].
 *
 * Provides setters for session parameters, operating mode session type, session color space, device
 * and session state callbacks, capture request keys, and repeating capture callbacks.
 *
 * **Warning:** Callbacks configured on this target receive raw
 * [android.hardware.camera2.CameraDevice] and [android.hardware.camera2.CameraCaptureSession]
 * instances. Directly invoking state-altering methods on these raw objects (such as
 * [android.hardware.camera2.CameraCaptureSession.close],
 * [android.hardware.camera2.CameraCaptureSession.abortCaptures], or
 * [android.hardware.camera2.CameraDevice.close]) bypasses CameraX pipeline management and may cause
 * state desynchronization, stream interruption, or application crashes.
 */
@CameraXDsl
public interface SessionConfigCamera2Interop :
    CameraDeviceInterop<SessionConfigCamera2Interop>,
    SessionConfigurationInterop<SessionConfigCamera2Interop>,
    CameraCaptureSessionInterop<SessionConfigCamera2Interop>

/**
 * Configures Camera2 options on a [androidx.camera.core.CameraControl].
 *
 * Provides setters for dynamic capture request key-value pairs, clearing request keys, template
 * types, and repeating capture callbacks on active camera sessions.
 *
 * **Warning:** Callbacks configured on this target (such as
 * [android.hardware.camera2.CameraCaptureSession.CaptureCallback]) receive raw
 * [android.hardware.camera2.CameraCaptureSession] instances. Directly invoking state-altering
 * methods on these raw objects (such as [android.hardware.camera2.CameraCaptureSession.close] or
 * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
 * management and may cause state desynchronization, stream interruption, or application crashes.
 */
@CameraXDsl
public interface CameraControlCamera2Interop :
    CameraCaptureSessionInterop<CameraControlCamera2Interop>

// =========================================================================================
// Concrete Implementation Classes
// =========================================================================================

/** Default implementation of [UseCaseCamera2Interop] storing options in [mutableConfig]. */
internal open class UseCaseCamera2InteropImpl(override val mutableConfig: MutableConfig) :
    UseCaseCamera2Interop, OutputConfigurationInteropDelegate<UseCaseCamera2Interop>

/** Default implementation of [ImageCaptureCamera2Interop] storing options in [mutableConfig]. */
internal class ImageCaptureCamera2InteropImpl(override val mutableConfig: MutableConfig) :
    ImageCaptureCamera2Interop,
    OutputConfigurationInteropDelegate<ImageCaptureCamera2Interop>,
    StillCaptureInteropDelegate<ImageCaptureCamera2Interop>

/** Default implementation of [SessionConfigCamera2Interop] storing options in [mutableConfig]. */
internal class SessionConfigCamera2InteropImpl(override val mutableConfig: MutableConfig) :
    SessionConfigCamera2Interop,
    CameraDeviceInteropDelegate<SessionConfigCamera2Interop>,
    SessionConfigurationInteropDelegate<SessionConfigCamera2Interop>,
    CameraCaptureSessionInteropDelegate<SessionConfigCamera2Interop>

/** Default implementation of [CameraControlCamera2Interop] storing options in [mutableConfig]. */
internal class CameraControlCamera2InteropImpl(override val mutableConfig: MutableConfig) :
    CameraControlCamera2Interop, CameraCaptureSessionInteropDelegate<CameraControlCamera2Interop>
