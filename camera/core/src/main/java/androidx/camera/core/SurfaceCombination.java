/*
 * Copyright (C) 2019 The Android Open Source Project
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

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.ArrayList;
import java.util.List;

/**
 * Surface configuration combination
 *
 * <p>{@link android.hardware.camera2.CameraDevice#createCaptureSession} defines the default
 * guaranteed stream combinations for different hardware level devices. It defines what combination
 * of surface configuration type and size pairs can be supported for different hardware level camera
 * devices. This structure is used to store a list of surface configuration as a combination.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class SurfaceCombination {

    private final List<SurfaceConfiguration> mSurfaceConfigurationList = new ArrayList<>();

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

    /** Adds a {@link SurfaceConfiguration} to the combination. */
    public boolean addSurfaceConfiguration(SurfaceConfiguration surfaceConfiguration) {
        if (surfaceConfiguration == null) {
            return false;
        }

        return mSurfaceConfigurationList.add(surfaceConfiguration);
    }

    /** Removes a {@link SurfaceConfiguration} from the combination. */
    public boolean removeSurfaceConfiguration(SurfaceConfiguration surfaceConfiguration) {
        if (surfaceConfiguration == null) {
            return false;
        }

        return mSurfaceConfigurationList.remove(surfaceConfiguration);
    }

    public List<SurfaceConfiguration> getSurfaceConfigurationList() {
        return mSurfaceConfigurationList;
    }

    /**
     * Check whether the input surface configuration list is under the capability of the combination
     * of this object.
     *
     * @param configurationList the surface configuration list to be compared
     * @return the check result that whether it could be supported
     */
    public boolean isSupported(List<SurfaceConfiguration> configurationList) {
        boolean isSupported = false;

        if (configurationList == null || configurationList.isEmpty()) {
            return true;
        }

        /**
         * Sublist of this surfaceConfiguration may be able to support the desired configuration.
         * For example, (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (JPEG, MAXIMUM) can supported by the
         * following level3 camera device combination - (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (JPEG,
         * MAXIMUM) + (RAW, MAXIMUM).
         */
        if (configurationList.size() > mSurfaceConfigurationList.size()) {
            return false;
        }

        List<int[]> elementsArrangements = getElementsArrangements(
                mSurfaceConfigurationList.size());

        for (int[] elementsArrangement : elementsArrangements) {
            boolean checkResult = true;

            for (int index = 0; index < mSurfaceConfigurationList.size(); index++) {
                if (elementsArrangement[index] < configurationList.size()) {
                    checkResult &=
                            mSurfaceConfigurationList
                                    .get(index)
                                    .isSupported(configurationList.get(elementsArrangement[index]));

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
