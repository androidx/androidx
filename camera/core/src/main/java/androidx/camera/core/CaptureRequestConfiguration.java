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

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Key;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.Configuration.Option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configurations needed for a capture request.
 *
 * <p>The CaptureRequestConfiguration contains all the {@link android.hardware.camera2} parameters
 * that are required to issue a {@link CaptureRequest}.
 *
 * @hide
 */
public final class CaptureRequestConfiguration {

    /** The set of {@link Surface} that data from the camera will be put into. */
    private final List<DeferrableSurface> surfaces;

    /** The parameters used to configure the {@link CaptureRequest}. */
    private final Map<Key<?>, CaptureRequestParameter<?>> captureRequestParameters;

    private final Configuration implementationOptions;

    /**
     * The templates used for configuring a {@link CaptureRequest}. This must match the constants
     * defined by {@link CameraDevice}
     */
    private final int templateType;

    /** The camera capture callback for a {@link CameraCaptureSession}. */
    private final CameraCaptureCallback cameraCaptureCallback;

    /** True if this capture request needs a repeating surface */
    private final boolean useRepeatingSurface;

    /**
     * Private constructor for a CaptureRequestConfiguration.
     *
     * <p>In practice, the {@link CaptureRequestConfiguration.Builder} will be used to construct a
     * CaptureRequestConfiguration.
     *
     * @param surfaces                 The set of {@link Surface} where data will be put into.
     * @param captureRequestParameters The parameters used to configure the {@link CaptureRequest}.
     * @param implementationOptions    The generic parameters to be passed to the {@link BaseCamera}
     *                                 class.
     * @param templateType             The template for parameters of the CaptureRequest. This
     *                                 must match the
     *                                 constants defined by {@link CameraDevice}.
     * @param cameraCaptureCallback    The camera capture callback.
     */
    private CaptureRequestConfiguration(
            List<DeferrableSurface> surfaces,
            Map<Key<?>, CaptureRequestParameter<?>> captureRequestParameters,
            Configuration implementationOptions,
            int templateType,
            CameraCaptureCallback cameraCaptureCallback,
            boolean useRepeatingSurface) {
        this.surfaces = surfaces;
        this.captureRequestParameters = captureRequestParameters;
        this.implementationOptions = implementationOptions;
        this.templateType = templateType;
        this.cameraCaptureCallback = cameraCaptureCallback;
        this.useRepeatingSurface = useRepeatingSurface;
    }

    public void addSurface(DeferrableSurface surface) {
        surfaces.add(surface);
    }

    public List<DeferrableSurface> getSurfaces() {
        return Collections.unmodifiableList(surfaces);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public Map<Key<?>, CaptureRequestParameter<?>> getCameraCharacteristics() {
        return Collections.unmodifiableMap(captureRequestParameters);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public Configuration getImplementationOptions() {
        return implementationOptions;
    }

    int getTemplateType() {
        return templateType;
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public boolean isUseRepeatingSurface() {
        return useRepeatingSurface;
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraCaptureCallback getCameraCaptureCallback() {
        return cameraCaptureCallback;
    }

    /**
     * Return the builder of a {@link CaptureRequest} which can be issued.
     *
     * <p>Returns {@code null} if a valid {@link CaptureRequest} can not be constructed.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public CaptureRequest.Builder buildCaptureRequest(@Nullable CameraDevice device)
            throws CameraAccessException {
        if (device == null) {
            return null;
        }
        CaptureRequest.Builder builder = device.createCaptureRequest(templateType);

        for (CaptureRequestParameter<?> captureRequestParameter :
                captureRequestParameters.values()) {
            captureRequestParameter.apply(builder);
        }

        List<Surface> surfaceList = DeferrableSurfaces.surfaceList(surfaces);

        if (surfaceList.isEmpty()) {
            return null;
        }

        for (Surface surface : surfaceList) {
            builder.addTarget(surface);
        }

        return builder;
    }

    /** Builder for easy modification/rebuilding of a {@link CaptureRequestConfiguration}. */
    public static final class Builder {
        private final Set<DeferrableSurface> surfaces = new HashSet<>();
        private final Map<Key<?>, CaptureRequestParameter<?>> captureRequestParameters =
                new HashMap<>();
        private MutableConfiguration implementationOptions = MutableOptionsBundle.create();
        private int templateType = -1;
        private CameraCaptureCallback cameraCaptureCallback =
                CameraCaptureCallbacks.createNoOpCallback();
        private boolean useRepeatingSurface = false;

        public Builder() {
        }

        private Builder(CaptureRequestConfiguration base) {
            surfaces.addAll(base.surfaces);
            captureRequestParameters.putAll(base.captureRequestParameters);
            implementationOptions = MutableOptionsBundle.from(base.implementationOptions);
            templateType = base.templateType;
            cameraCaptureCallback = base.cameraCaptureCallback;
            useRepeatingSurface = base.isUseRepeatingSurface();
        }

        /** Create a {@link Builder} from a {@link CaptureRequestConfiguration} */
        public static Builder from(CaptureRequestConfiguration base) {
            return new Builder(base);
        }

        int getTemplateType() {
            return templateType;
        }

        /**
         * Set the template characteristics of the CaptureRequestConfiguration.
         *
         * @param templateType Template constant that must match those defined by {@link
         *                     CameraDevice}
         */
        public void setTemplateType(int templateType) {
            this.templateType = templateType;
        }

        CameraCaptureCallback getCameraCaptureCallback() {
            return cameraCaptureCallback;
        }

        public void setCameraCaptureCallback(CameraCaptureCallback cameraCaptureCallback) {
            this.cameraCaptureCallback = cameraCaptureCallback;
        }

        public void addSurface(DeferrableSurface surface) {
            surfaces.add(surface);
        }

        public void removeSurface(DeferrableSurface surface) {
            surfaces.remove(surface);
        }

        public void clearSurfaces() {
            surfaces.clear();
        }

        Set<DeferrableSurface> getSurfaces() {
            return surfaces;
        }

        public <T> void addCharacteristic(Key<T> key, T value) {
            captureRequestParameters.put(key, CaptureRequestParameter.create(key, value));
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        public void addCharacteristics(Map<Key<?>, CaptureRequestParameter<?>> characteristics) {
            captureRequestParameters.putAll(characteristics);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        public void setImplementationOptions(Configuration config) {
            implementationOptions = MutableOptionsBundle.from(config);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        public void addImplementationOptions(Configuration config) {
            for (Option<?> option : config.listOptions()) {
                @SuppressWarnings("unchecked") // Options/values are being copied directly
                        Option<Object> objectOpt = (Option<Object>) option;
                implementationOptions.insertOption(objectOpt, config.retrieveOption(objectOpt));
            }
        }

        Map<Key<?>, CaptureRequestParameter<?>> getCharacteristic() {
            return captureRequestParameters;
        }

        boolean isUseRepeatingSurface() {
            return useRepeatingSurface;
        }

        public void setUseRepeatingSurface(boolean useRepeatingSurface) {
            this.useRepeatingSurface = useRepeatingSurface;
        }

        /**
         * Builds an instance of a CaptureRequestConfiguration that has all the combined parameters
         * of the CaptureRequestConfiguration that have been added to the Builder.
         */
        public CaptureRequestConfiguration build() {
            return new CaptureRequestConfiguration(
                    new ArrayList<>(surfaces),
                    new HashMap<>(captureRequestParameters),
                    OptionsBundle.from(implementationOptions),
                    templateType,
                    cameraCaptureCallback,
                    useRepeatingSurface);
        }
    }
}
