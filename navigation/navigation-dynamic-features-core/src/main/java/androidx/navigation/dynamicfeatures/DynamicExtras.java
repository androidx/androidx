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

package androidx.navigation.dynamicfeatures;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigator;

/**
 * Extras for dynamic navigators.
 *
 * You can set a {@link DynamicInstallMonitor} here to be notified of install state changes.
 */
public final class DynamicExtras implements Navigator.Extras {
    @Nullable
    private final DynamicInstallMonitor mInstallMonitor;
    @Nullable
    private final Navigator.Extras mDestinationExtras;

    /**
     * Creates DynamicExtras.
     *
     * @param installMonitor    Enable monitoring changes in installation state.
     * @param destinationExtras Extras that should be passed on to the final navigation destination.
     */
    DynamicExtras(
            @Nullable DynamicInstallMonitor installMonitor,
            @Nullable Navigator.Extras destinationExtras) {
        mInstallMonitor = installMonitor;
        mDestinationExtras = destinationExtras;
    }

    /**
     * @return The {@link DynamicInstallMonitor} used.
     */
    @Nullable
    public DynamicInstallMonitor getInstallMonitor() {
        return mInstallMonitor;
    }

    /**
     * @return {@link Navigator.Extras} associated with these {@link DynamicExtras}.
     */
    @Nullable
    public Navigator.Extras getDestinationExtras() {
        return mDestinationExtras;
    }

    /**
     * Builder that enables creation of {@link DynamicExtras}.
     */
    public static final class Builder {

        private DynamicInstallMonitor mMonitor;
        private Navigator.Extras mDestinationExtras;

        /**
         * Set the {@link DynamicInstallMonitor}.
         *
         * @param monitor The {@link DynamicInstallMonitor} to set.
         * @return This {@link Builder}.
         */
        @NonNull
        public Builder setInstallMonitor(@NonNull DynamicInstallMonitor monitor) {
            mMonitor = monitor;
            return this;
        }

        /**
         * Set the {@link Navigator.Extras}.
         *
         * @param destinationExtras The {@link Navigator.Extras} to set.
         * @return This {@link Builder}.
         */
        @NonNull
        public Builder setDestinationExtras(@NonNull Navigator.Extras destinationExtras) {
            mDestinationExtras = destinationExtras;
            return this;
        }

        /**
         * Build {@link DynamicExtras}.
         *
         * @return A new instance of {@link DynamicExtras} with all attributes set in the builder.
         */
        @NonNull
        public DynamicExtras build() {
            return new DynamicExtras(mMonitor, mDestinationExtras);
        }
    }
}
