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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Surface configuration combination
 *
 * <p>{@link android.hardware.camera2.CameraDevice#createCaptureSession} defines the default
 * guaranteed stream combinations for different hardware level devices. It defines what combination
 * of surface configuration type and size pairs can be supported for different hardware level camera
 * devices. This structure is used to store a list of surface configuration as a combination.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class SurfaceCombination {

    private final List<SurfaceConfig> mSurfaceConfigList = new ArrayList<>();

    public SurfaceCombination() {
    }

    private static void generateArrangements(
            List<int[]> arrangementsResultList, int n, int[] result, int index) {
        if (index >= result.length) {
            arrangementsResultList.add(result.clone());
            return;
        }

        for (int i = 0; i < n; i++) {
            boolean included = false;

            for (int j = 0; j < index; j++) {
                if (i == result[j]) {
                    included = true;
                    break;
                }
            }

            if (!included) {
                result[index] = i;
                generateArrangements(arrangementsResultList, n, result, index + 1);
            }
        }
    }

    /** Adds a {@link SurfaceConfig} to the combination. */
    public boolean addSurfaceConfig(@NonNull SurfaceConfig surfaceConfig) {
        return mSurfaceConfigList.add(surfaceConfig);
    }

    /** Removes a {@link SurfaceConfig} from the combination. */
    public boolean removeSurfaceConfig(@NonNull SurfaceConfig surfaceConfig) {
        return mSurfaceConfigList.remove(surfaceConfig);
    }

    @NonNull
    public List<SurfaceConfig> getSurfaceConfigList() {
        return mSurfaceConfigList;
    }

    /**
     * Check whether the input surface configuration list is under the capability of the combination
     * of this object.
     *
     * @param configList the surface configuration list to be compared
     * @return the check result that whether it could be supported
     */
    public boolean isSupported(@NonNull List<SurfaceConfig> configList) {
        boolean isSupported = false;

        if (configList.isEmpty()) {
            return true;
        }

        /**
         * Sublist of this surfaceConfig may be able to support the desired configuration.
         * For example, (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (JPEG, MAXIMUM) can supported by the
         * following level3 camera device combination - (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (JPEG,
         * MAXIMUM) + (RAW, MAXIMUM).
         */
        if (configList.size() > mSurfaceConfigList.size()) {
            return false;
        }

        List<int[]> elementsArrangements = getElementsArrangements(mSurfaceConfigList.size());

        for (int[] elementsArrangement : elementsArrangements) {
            boolean checkResult = true;

            for (int index = 0; index < mSurfaceConfigList.size(); index++) {
                if (elementsArrangement[index] < configList.size()) {
                    checkResult &=
                            mSurfaceConfigList
                                    .get(index)
                                    .isSupported(configList.get(elementsArrangement[index]));

                    if (!checkResult) {
                        break;
                    }
                }
            }

            if (checkResult) {
                isSupported = true;
                break;
            }
        }

        return isSupported;
    }

    private List<int[]> getElementsArrangements(int n) {
        List<int[]> arrangementsResultList = new ArrayList<>();

        generateArrangements(arrangementsResultList, n, new int[n], 0);

        return arrangementsResultList;
    }
}
