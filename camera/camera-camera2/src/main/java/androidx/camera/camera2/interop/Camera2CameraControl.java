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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.Camera2CameraControlImpl;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.core.CameraControl;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * An class that provides ability to interoperate with the {@link android.hardware.camera2} APIs.
 *
 * <p>Camera2 specific controls, like capture request options, can be applied through this class.
 * A Camera2CameraControl can be created from a general {@link CameraControl} which is associated
 * to a camera. Then the controls will affect all use cases that are using that camera.
 *
 * <p>If any option applied by Camera2CameraControl conflicts with the options required by
 * CameraX internally. The options from Camera2CameraControl will override, which may result in
 * unexpected behavior depends on the options being applied.
 */
@ExperimentalCamera2Interop
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Camera2CameraControl {

    @RestrictTo(Scope.LIBRARY)
    public static final String TAG_KEY = "Camera2CameraControl";

    private boolean mIsActive = false;
    private boolean mPendingUpdate = false;
    private final Camera2CameraControlImpl mCamera2CameraControlImpl;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @CameraExecutor
    final Executor mExecutor;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Object mLock = new Object();
    @GuardedBy("mLock")
    private Camera2ImplConfig.Builder mBuilder = new Camera2ImplConfig.Builder();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CallbackToFutureAdapter.Completer<Void> mCompleter;
    private final Camera2CameraControlImpl.CaptureResultListener mCaptureResultListener =
            (captureResult) -> {
                CallbackToFutureAdapter.Completer<Void> completerToSet = null;
                if (mCompleter != null) {
                    Object tag = captureResult.getRequest().getTag();
                    if (tag instanceof TagBundle) {
                        TagBundle tagBundle = (TagBundle) tag;
                        Integer tagInteger = (Integer) tagBundle.getTag(TAG_KEY);
                        if (tagInteger != null && tagInteger.equals(mCompleter.hashCode())) {
                            completerToSet = mCompleter;
                            mCompleter = null;
                        }
                    }
                }
                if (completerToSet != null) {
                    completerToSet.set(null);
                }
                // Return false to keep getting captureResult.
                return false;
            };

    /**
     * Creates a new camera control with Camera2 implementation.
     *
     * @param camera2CameraControlImpl the camera control this Camera2CameraControl belongs.
     * @param executor                 the camera executor used to run camera task.
     */
    @RestrictTo(Scope.LIBRARY)
    public Camera2CameraControl(@NonNull Camera2CameraControlImpl camera2CameraControlImpl,
            @NonNull @CameraExecutor Executor executor) {
        mCamera2CameraControlImpl = camera2CameraControlImpl;
        mExecutor = executor;
    }

    @RestrictTo(Scope.LIBRARY)
    @NonNull
    public Camera2CameraControlImpl.CaptureResultListener getCaptureRequestListener() {
        return mCaptureResultListener;
    }

    /**
     * Gets the {@link Camera2CameraControl} from a {@link CameraControl}.
     *
     * <p>The {@link CameraControl} is still usable after a {@link Camera2CameraControl} is
     * obtained from it. Note that the {@link Camera2CameraControl} has higher priority than the
     * {@link CameraControl}. For example, if
     * {@link android.hardware.camera2.CaptureRequest#FLASH_MODE} is set through the
     * {@link Camera2CameraControl}. All {@link CameraControl} features that required
     * {@link android.hardware.camera2.CaptureRequest#FLASH_MODE} internally like torch may not
     * work properly.
     *
     * @param cameraControl The {@link CameraControl} to get from.
     * @return The camera control with Camera2 implementation.
     * @throws IllegalArgumentException if the camera control does not contain the camera2
     *                                  information (e.g., if CameraX was not initialized with a
     *                                  {@link androidx.camera.camera2.Camera2Config}).
     */
    @NonNull
    public static Camera2CameraControl from(@NonNull CameraControl cameraControl) {
        CameraControlInternal cameraControlImpl =
                ((CameraControlInternal) cameraControl).getImplementation();
        Preconditions.checkArgument(cameraControlImpl instanceof Camera2CameraControlImpl,
                "CameraControl doesn't contain Camera2 implementation.");
        return ((Camera2CameraControlImpl) cameraControlImpl).getCamera2CameraControl();
    }

    /**
     * Sets a {@link CaptureRequestOptions} and updates the session with the options it
     * contains.
     *
     * <p>This will first clear all options that have already been set, then apply the new options.
     *
     * <p>Any values which are in conflict with values already set by CameraX, such as by
     * {@link androidx.camera.core.CameraControl}, will overwrite the existing values. The
     * values will be submitted with every repeating and single capture requests issued by
     * CameraX, which may result in unexpected behavior depending on the values being applied.
     *
     * @param bundle The {@link CaptureRequestOptions} which will be set.
     * @return a {@link ListenableFuture} which completes when the repeating
     * {@link android.hardware.camera2.CaptureResult} shows the options have be submitted
     * completely. The future fails with {@link CameraControl.OperationCanceledException} if newer
     * options are set or camera is closed before the current request completes.
     * Cancelling the ListenableFuture is a no-op.
     */
    @SuppressWarnings("AsyncSuffixFuture")
    @NonNull
    public ListenableFuture<Void> setCaptureRequestOptions(
            @NonNull CaptureRequestOptions bundle) {
        clearCaptureRequestOptionsInternal();
        addCaptureRequestOptionsInternal(bundle);

        return Futures.nonCancellationPropagating(CallbackToFutureAdapter.getFuture(completer -> {
            mExecutor.execute(() -> {
                updateConfig(completer);
            });
            return "setCaptureRequestOptions";
        }));
    }

    /**
     * Adds a {@link CaptureRequestOptions} updates the session with the options it
     * contains.
     *
     * <p>The options will be merged with the existing options. If one option is set with a
     * different value, it will overwrite the existing value.
     *
     * <p>Any values which are in conflict with values already set by CameraX, such as by
     * {@link androidx.camera.core.CameraControl}, will overwrite the existing values. The
     * values will be submitted with every repeating and single capture requests issued by
     * CameraX, which may result in unexpected behavior depends on the values being applied.
     *
     * @param bundle The {@link CaptureRequestOptions} which will be set.
     * @return a {@link ListenableFuture} which completes when the repeating
     * {@link android.hardware.camera2.CaptureResult} shows the options have be submitted
     * completely. The future fails with {@link CameraControl.OperationCanceledException} if newer
     * options are set or camera is closed before the current request completes.
     */
    @SuppressWarnings("AsyncSuffixFuture")
    @NonNull
    public ListenableFuture<Void> addCaptureRequestOptions(
            @NonNull CaptureRequestOptions bundle) {
        addCaptureRequestOptionsInternal(bundle);

        return Futures.nonCancellationPropagating(CallbackToFutureAdapter.getFuture(completer -> {
            mExecutor.execute(() -> {
                updateConfig(completer);
            });
            return "addCaptureRequestOptions";
        }));
    }

    /**
     * Gets all the capture request options that is currently applied by the
     * {@link Camera2CameraControl}.
     *
     * <p>It doesn't include the capture request options applied by
     * the {@link android.hardware.camera2.CameraDevice} templates or by CameraX.
     *
     * @return The {@link CaptureRequestOptions}.
     */
    @NonNull
    public CaptureRequestOptions getCaptureRequestOptions() {
        synchronized (mLock) {
            return CaptureRequestOptions.Builder.from(mBuilder.build()).build();
        }
    }

    /**
     * Clears all capture request options that is currently applied by the
     * {@link Camera2CameraControl}.
     *
     * @return a {@link ListenableFuture} which completes when the repeating
     * {@link android.hardware.camera2.CaptureResult} shows the options have be submitted
     * completely. The future fails with {@link CameraControl.OperationCanceledException} if newer
     * options are set or camera is closed before the current request completes.
     */
    @SuppressWarnings("AsyncSuffixFuture")
    @NonNull
    public ListenableFuture<Void> clearCaptureRequestOptions() {
        clearCaptureRequestOptionsInternal();

        return Futures.nonCancellationPropagating(CallbackToFutureAdapter.getFuture(completer -> {
            mExecutor.execute(() -> {
                updateConfig(completer);
            });
            return "clearCaptureRequestOptions";
        }));
    }

    /**
     * Gets the {@link Camera2ImplConfig} that is currently applied by the
     * {@link Camera2CameraControl}.
     *
     */
    @RestrictTo(Scope.LIBRARY)
    @NonNull
    public Camera2ImplConfig getCamera2ImplConfig() {
        synchronized (mLock) {
            if (mCompleter != null) {
                mBuilder.getMutableConfig().insertOption(
                        Camera2ImplConfig.CAPTURE_REQUEST_TAG_OPTION,
                        mCompleter.hashCode());
            }
            return mBuilder.build();
        }
    }

    private void addCaptureRequestOptionsInternal(@NonNull CaptureRequestOptions bundle) {
        synchronized (mLock) {
            for (Config.Option<?> option : bundle.listOptions()) {
                @SuppressWarnings("unchecked")
                Config.Option<Object> objectOpt = (Config.Option<Object>) option;
                mBuilder.getMutableConfig().insertOption(objectOpt,
                        bundle.retrieveOption(objectOpt));
            }
        }
    }

    private void clearCaptureRequestOptionsInternal() {
        synchronized (mLock) {
            mBuilder = new Camera2ImplConfig.Builder();
        }
    }

    @ExecutedBy("mExecutor")
    private void updateConfig(CallbackToFutureAdapter.Completer<Void> completer) {
        mPendingUpdate = true;
        // Complete the future if CaptureResult shows it's submitted successfully.
        CallbackToFutureAdapter.Completer<Void> completerToCancel = null;
        if (mCompleter != null) {
            completerToCancel = mCompleter;
        }
        mCompleter = completer;
        if (mIsActive) {
            updateSession();
        }
        if (completerToCancel != null) {
            completerToCancel.setException(new CameraControl.OperationCanceledException(
                    "Camera2CameraControl was updated with new options."));
        }
    }

    @ExecutedBy("mExecutor")
    private void updateSession() {
        mCamera2CameraControlImpl.updateSessionConfig();
        mPendingUpdate = false;
    }

    /**
     * Set current active state.
     *
     * <p>When the state changes from active to inactive, the Camera2 options will be cleared.
     * When the state changes from inactive to active, a session update will be issued if there's
     * Camera2 options set while inactive.
     *
     */
    @RestrictTo(Scope.LIBRARY)
    public void setActive(boolean isActive) {
        mExecutor.execute(() -> setActiveInternal(isActive));
    }

    @ExecutedBy("mExecutor")
    private void setActiveInternal(boolean isActive) {
        if (mIsActive == isActive) {
            return;
        }

        mIsActive = isActive;

        if (mIsActive) {
            if (mPendingUpdate) {
                updateSession();
            }
        } else {
            if (mCompleter != null) {
                mCompleter.setException(new CameraControl.OperationCanceledException(
                        "The camera control has became inactive."));
                mCompleter = null;
            }
        }
    }
}
