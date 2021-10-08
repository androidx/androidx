/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.InitializationException;
import androidx.camera.core.impl.CameraInfoInternal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class CameraSelectionOptimizer {
    private CameraSelectionOptimizer() {
    }

    static List<String> getSelectedAvailableCameraIds(
            @NonNull Camera2CameraFactory cameraFactory,
            @Nullable CameraSelector availableCamerasSelector)
            throws InitializationException {
        try {
            List<String> availableCameraIds = new ArrayList<>();
            List<String> cameraIdList =
                    Arrays.asList(cameraFactory.getCameraManager().getCameraIdList());
            if (availableCamerasSelector == null) {
                for (String id : cameraIdList) {
                    availableCameraIds.add(id);
                }
                return availableCameraIds;
            }

            // Skip camera ID by heuristic: 0 is back lens facing, 1 is front lens facing.
            String skippedCameraId;
            try {
                Integer lensFacingInteger = availableCamerasSelector.getLensFacing();
                skippedCameraId = decideSkippedCameraIdByHeuristic(
                        cameraFactory.getCameraManager(), lensFacingInteger, cameraIdList);
            } catch (IllegalStateException e) {
                // Don't skip camera if there is any conflict in camera lens facing.
                skippedCameraId = null;
            }

            List<CameraInfo> cameraInfos = new ArrayList<>();

            for (String id : cameraIdList) {
                if (id.equals(skippedCameraId)) {
                    continue;
                }
                Camera2CameraInfoImpl cameraInfo = cameraFactory.getCameraInfo(id);
                cameraInfos.add(cameraInfo);
            }

            try {
                List<CameraInfo> filteredCameraInfos =
                        availableCamerasSelector.filter(cameraInfos);

                for (CameraInfo cameraInfo : filteredCameraInfos) {
                    String cameraId = ((CameraInfoInternal) cameraInfo).getCameraId();
                    availableCameraIds.add(cameraId);
                }
            } catch (IllegalArgumentException e) {
                // Return empty available id list if no camera is found.
                return availableCameraIds;
            }

            return availableCameraIds;
        } catch (CameraAccessExceptionCompat e) {
            throw new InitializationException(CameraUnavailableExceptionHelper.createFrom(e));
        } catch (CameraUnavailableException e) {
            throw new InitializationException(e);
        }
    }

    // Returns the camera id that can be safely skipped.
    // Returns null if no camera ids can be skipped.
    private static String decideSkippedCameraIdByHeuristic(CameraManagerCompat cameraManager,
            Integer lensFacingInteger, List<String> cameraIdList)
            throws CameraAccessExceptionCompat {
        String skippedCameraId = null;
        if (lensFacingInteger == null) { // Not specifying lens facing,  cannot skip any camera id.
            return null;
        }

        // No skipping camera if camera id "0" or "1" does not exist. Also avoid querying unexisting
        // camera ids.
        if (!(cameraIdList.contains("0") && cameraIdList.contains("1"))) {
            return null;
        }

        if (lensFacingInteger.intValue() == CameraSelector.LENS_FACING_BACK) {
            if (cameraManager.getCameraCharacteristicsCompat("0").get(
                    CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK) {
                // If apps requires back lens facing,  and "0" is confirmed to be back
                // We can safely ignore "1" as a optimization for initialization latency
                skippedCameraId = "1";
            }
        } else if (lensFacingInteger.intValue() == CameraSelector.LENS_FACING_FRONT) {
            if (cameraManager.getCameraCharacteristicsCompat("1").get(
                    CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT) {
                // If apps requires front lens facing,  and "1" is confirmed to be back
                // We can safely ignore "0" as a optimization for initialization latency
                skippedCameraId = "0";
            }
        }

        return skippedCameraId;
    }
}
