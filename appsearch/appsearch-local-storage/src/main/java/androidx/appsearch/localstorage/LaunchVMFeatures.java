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

package androidx.appsearch.localstorage;

import androidx.annotation.RestrictTo;

/**
 * Handles the configuration of Virtual Machine features for system launches.
 *
 * <p>This class acts as a data holder and applier for VM enablement states.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LaunchVMFeatures {
    /*** Indicates if the primary Virtual Machine feature is enabled.*/
    private boolean mIsVMEnabled1;

    /**
     * Indicates if the second iteration of the Virtual Machine integration is requested.
     *
     * Note: This is only applied if {@link #mIsVMEnabled1} is also true.
     */
    private boolean mIsVMEnabled2;

    /**
     * Returns whether the primary VM feature is enabled.
     *
     * @return {@code true} if the VM feature is enabled.
     */
    public boolean isVMEnabled1() {
        return mIsVMEnabled1;
    }

    /**
     * Returns whether the second iteration of VM integration is requested.
     *
     * @return {@code true} if both the primary and secondary VM feature is enabled.
     */
    public boolean isVMEnabled2() {
        return mIsVMEnabled1 && mIsVMEnabled2;
    }

    /**
     * Sets the enablement state of the primary VM feature.
     *
     * @param isVMEnabled1 {@code true} to enable the primary VM feature.
     */
    public void setVMEnabled1(boolean isVMEnabled1) {
        mIsVMEnabled1 = isVMEnabled1;
    }

    /**
     * Sets the requested state of the secondary VM feature.
     *
     * @param isVMEnabled2 {@code true} to request the secondary VM feature.
     */
    public void setVMEnabled2(boolean isVMEnabled2) {
        mIsVMEnabled2 = isVMEnabled2;
    }
}
