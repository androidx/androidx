/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.featurecombinationquery;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.OutputConfiguration;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A compat class to store the same minimal information as Camera2's
 * {@link android.hardware.camera2.params.SessionConfiguration}.
 * <p>
 * This class allows querying on devices that do not support Camera2's
 * {@link android.hardware.camera2.CameraDevice.CameraDeviceSetup} and need to query the camera
 * capabilities before calling {@link android.hardware.camera2.CameraManager#openCamera}.
 * <p>
 * However, there are downsides to using the {@code *Legacy} classes.
 * <ol>
 *     <li>When {@code *Legacy} classes are used, only the non-Camera2 databases (if present) will
 *     be queried.</li>
 *     <li>The output of {@link CameraDevice#createCaptureRequest} will often set defaults based on
 *     the Template passed to it. Without the capture request provided by the camera2 API, there is
 *     no reasonable way for this API to guarantee that the result returned is completely
 *     reliable.</li>
 * </ol>
 * <p>
 * With those downsides, the usage of the {@code *Legacy} classes is restricted to devices
 * that do not support {@link android.hardware.camera2.CameraDevice.CameraDeviceSetup}. On devices
 * that do not support {@link android.hardware.camera2.CameraDevice.CameraDeviceSetup}, it is
 * strongly recommended that this class is used if and only if calling
 * {@link android.hardware.camera2.CameraManager#openCamera} is not possible before the query.
 *
 * @see CameraDeviceSetupCompat#isSessionConfigurationSupportedLegacy(SessionConfigurationLegacy)
 */
public class SessionConfigurationLegacy {
    private final List<OutputConfiguration> mOutputConfigs;
    private final SessionParametersLegacy mSessionParams;

    private SessionConfigurationLegacy(List<OutputConfiguration> outputConfigs,
            SessionParametersLegacy sessionParams) {
        mOutputConfigs = outputConfigs;
        mSessionParams = sessionParams;
    }

    /**
     * @return the configured output configurations.
     */
    @NonNull
    public List<OutputConfiguration> getOutputConfigurations() {
        return mOutputConfigs;
    }

    /**
     * @return the associated session parameters.
     */
    @NonNull
    public SessionParametersLegacy getSessionParameters() {
        return mSessionParams;
    }

    /**
     * Simple builder class for {@link SessionConfigurationLegacy}.
     */
    public static final class Builder {
        @NonNull
        private final ArrayList<OutputConfiguration> mOutputConfigs = new ArrayList<>();
        @NonNull
        private SessionParametersLegacy mSessionParams =
                new SessionParametersLegacy.Builder().build();

        public Builder() {
        }

        /**
         * Add an {@link OutputConfiguration} to the session configuration.
         *
         * @param outputConfig {@link OutputConfiguration} to add to the session configuration.
         * @return the current builder
         */
        @NonNull
        public Builder addOutputConfiguration(@NonNull OutputConfiguration outputConfig) {
            mOutputConfigs.add(outputConfig);
            return this;
        }

        /**
         * Add a collection of {@link OutputConfiguration}s to the session configuration.
         *
         * @param outputConfigs {@link Collection} of {@link OutputConfiguration}s to add.
         * @return the current builder
         */
        @NonNull
        public Builder addOutputConfigurations(
                @NonNull Collection<@NonNull OutputConfiguration> outputConfigs) {
            mOutputConfigs.addAll(outputConfigs);
            return this;
        }

        /**
         * Sets the session parameters for the session configuration. Overwrites any previously set
         * session parameters.
         *
         * @param sessionParams session parameters to be associated with the session configuration
         * @return the current builder
         */
        @NonNull
        public Builder setSessionParameters(@NonNull SessionParametersLegacy sessionParams) {
            mSessionParams = sessionParams;
            return this;
        }

        /**
         * Builds a {@link SessionConfigurationLegacy}.
         * <p>
         * Note that the created {@link SessionConfigurationLegacy} makes a shallow copy of the
         * {@link OutputConfiguration}s added via {@link #addOutputConfiguration}, and so
         * any mutations to the added {@link OutputConfiguration} objects will be reflected in
         * the created {@link SessionConfigurationLegacy}.
         * <p>
         * This is quirk of implementation, and it is generally recommended to treat
         * {@link OutputConfiguration} objects as immutable once they have been added to a
         * {@link SessionConfigurationLegacy.Builder}.
         *
         * @return a new {@link SessionConfigurationLegacy} object.
         */
        @NonNull
        public SessionConfigurationLegacy build() {
            return new SessionConfigurationLegacy(List.copyOf(mOutputConfigs), mSessionParams);
        }

    }
}
