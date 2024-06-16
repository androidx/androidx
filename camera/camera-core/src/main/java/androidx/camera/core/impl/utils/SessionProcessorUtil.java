/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.impl.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.impl.RestrictedCameraInfo;
import androidx.camera.core.impl.SessionProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for session processor related operations.
 */
public final class SessionProcessorUtil {
    private SessionProcessorUtil() {

    }

    /**
     * Returns whether the camera operations are supported by the SessionProcessor.
     *
     * @param operations the camera operations to check.
     * @return {@code true} if the operations can be supported, otherwise {@code false}.
     */
    public static boolean isOperationSupported(@Nullable SessionProcessor sessionProcessor,
            @NonNull @RestrictedCameraInfo.CameraOperation int... operations) {
        if (sessionProcessor == null) {
            return true;
        }
        // Arrays.asList doesn't work for int array.
        List<Integer> operationList = new ArrayList<>(operations.length);
        for (int operation : operations) {
            operationList.add(operation);
        }

        return sessionProcessor.getSupportedCameraOperations().containsAll(operationList);
    }

    /**
     * Returns the modified {@link FocusMeteringAction} that filters out unsupported AE/AF/AWB
     * regions. Returns {@code null} if none of AF/AE/AWB regions can be supported after the
     * filtering.
     */
    @Nullable
    public static FocusMeteringAction getModifiedFocusMeteringAction(
            @Nullable SessionProcessor sessionProcessor, @NonNull FocusMeteringAction action) {
        if (sessionProcessor == null) {
            return action;
        }
        boolean shouldModify = false;
        FocusMeteringAction.Builder builder = new FocusMeteringAction.Builder(action);
        if (!action.getMeteringPointsAf().isEmpty()
                && !isOperationSupported(sessionProcessor,
                RestrictedCameraInfo.CAMERA_OPERATION_AUTO_FOCUS,
                RestrictedCameraInfo.CAMERA_OPERATION_AF_REGION)) {
            shouldModify = true;
            builder.removePoints(FocusMeteringAction.FLAG_AF);
        }

        if (!action.getMeteringPointsAe().isEmpty()
                && !isOperationSupported(sessionProcessor,
                RestrictedCameraInfo.CAMERA_OPERATION_AE_REGION)) {
            shouldModify = true;
            builder.removePoints(FocusMeteringAction.FLAG_AE);
        }

        if (!action.getMeteringPointsAwb().isEmpty()
                && !isOperationSupported(sessionProcessor,
                RestrictedCameraInfo.CAMERA_OPERATION_AWB_REGION)) {
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
