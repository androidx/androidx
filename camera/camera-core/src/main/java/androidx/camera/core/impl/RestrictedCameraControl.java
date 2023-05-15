/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A {@link CameraControlInternal} whose capabilities can be restricted via
 * {@link #enableRestrictedOperations(boolean, Set)}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class RestrictedCameraControl extends ForwardingCameraControl {
    /**
     * Defines the list of supported camera operations.
     */
    public static final int ZOOM = 0;
    public static final int AUTO_FOCUS = 1;
    public static final int AF_REGION = 2;
    public static final int AE_REGION = 3;
    public static final int AWB_REGION = 4;
    public static final int FLASH = 5;
    public static final int TORCH = 6;
    public static final int EXPOSURE_COMPENSATION = 7;

    public @interface CameraOperation {
    }

    private final CameraControlInternal mCameraControl;
    private volatile boolean mUseRestrictedCameraOperations = false;
    @Nullable
    private volatile @CameraOperation Set<Integer> mRestrictedCameraOperations;

    /**
     * Creates the restricted version of the given {@link CameraControlInternal}.
     */
    public RestrictedCameraControl(@NonNull CameraControlInternal cameraControl) {
        super(cameraControl);
        mCameraControl = cameraControl;
    }

    /**
     * Enable or disable the restricted operations. If disabled, it works just like the origin
     * CameraControlInternal instance.
     */
    public void enableRestrictedOperations(boolean enable,
            @Nullable @CameraOperation Set<Integer> restrictedOperations) {
        mUseRestrictedCameraOperations = enable;
        mRestrictedCameraOperations = restrictedOperations;
    }

    /**
     * Returns implementation instance.
     */
    @NonNull
    @Override
    public CameraControlInternal getImplementation() {
        return mCameraControl;
    }

    boolean isOperationSupported(
            @NonNull @CameraOperation int... operations) {
        if (!mUseRestrictedCameraOperations || mRestrictedCameraOperations == null) {
            return true;
        }

        // Arrays.asList doesn't work for int array.
        List<Integer> operationList = new ArrayList<>(operations.length);
        for (int operation : operations) {
            operationList.add(operation);
        }

        return mRestrictedCameraOperations.containsAll(operationList);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> enableTorch(boolean torch) {
        if (!isOperationSupported(TORCH)) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("Torch is not supported"));
        }
        return mCameraControl.enableTorch(torch);
    }

    @NonNull
    @Override
    public ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action) {
        FocusMeteringAction modifiedAction = getModifiedFocusMeteringAction(action);
        if (modifiedAction == null) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("FocusMetering is not supported"));
        }

        return mCameraControl.startFocusAndMetering(modifiedAction);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> cancelFocusAndMetering() {
        return mCameraControl.cancelFocusAndMetering();
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setZoomRatio(float ratio) {
        if (!isOperationSupported(ZOOM)) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("Zoom is not supported"));
        }
        return mCameraControl.setZoomRatio(ratio);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setLinearZoom(float linearZoom) {
        if (!isOperationSupported(ZOOM)) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("Zoom is not supported"));
        }
        return mCameraControl.setLinearZoom(linearZoom);
    }

    @NonNull
    @Override
    public ListenableFuture<Integer> setExposureCompensationIndex(int value) {
        if (!isOperationSupported(EXPOSURE_COMPENSATION)) {
            return Futures.immediateFailedFuture(
                    new IllegalStateException("ExposureCompensation is not supported"));
        }
        return mCameraControl.setExposureCompensationIndex(value);
    }

    /**
     * Returns the modified {@link FocusMeteringAction} that filters out unsupported AE/AF/AWB
     * regions. Returns null if none of AF/AE/AWB regions can be supported after the filtering.
     */
    @Nullable
    FocusMeteringAction getModifiedFocusMeteringAction(@NonNull FocusMeteringAction action) {
        boolean shouldModify = false;
        FocusMeteringAction.Builder builder = new FocusMeteringAction.Builder(action);
        if (!action.getMeteringPointsAf().isEmpty()
                && !isOperationSupported(AUTO_FOCUS, AF_REGION)) {
            shouldModify = true;
            builder.removePoints(FocusMeteringAction.FLAG_AF);
        }

        if (!action.getMeteringPointsAe().isEmpty()
                && !isOperationSupported(AE_REGION)) {
            shouldModify = true;
            builder.removePoints(FocusMeteringAction.FLAG_AE);
        }

        if (!action.getMeteringPointsAwb().isEmpty()
                && !isOperationSupported(AWB_REGION)) {
            shouldModify = true;
            builder.removePoints(FocusMeteringAction.FLAG_AWB);
        }

        // Returns origin action if no need to modify.
        if (!shouldModify) {
            return action;
        }

        FocusMeteringAction modifyAction = builder.build();
        if (modifyAction.getMeteringPointsAf().isEmpty()
                && modifyAction.getMeteringPointsAe().isEmpty()
                && modifyAction.getMeteringPointsAwb().isEmpty()) {
            // All regions are not allowed, return null.
            return null;
        }
        return builder.build();
    }
}
