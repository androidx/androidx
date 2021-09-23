/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions.internal.sessionprocessor;

import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.extensions.impl.advanced.Camera2OutputConfigImpl;
import androidx.camera.extensions.impl.advanced.ImageReaderOutputConfigImpl;
import androidx.camera.extensions.impl.advanced.MultiResolutionImageReaderOutputConfigImpl;
import androidx.camera.extensions.impl.advanced.SurfaceOutputConfigImpl;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A builder for building {@link Camera2OutputConfig}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class Camera2OutputConfigBuilder {
    private static AtomicInteger sLastId = new AtomicInteger(0);
    private OutputConfig mOutputConfig;
    private int mSurfaceGroupId;
    private String mPhysicalCameraId;
    private List<Camera2OutputConfig> mSurfaceSharingConfigs;

    private Camera2OutputConfigBuilder(OutputConfig outputConfig) {
        mOutputConfig = outputConfig;
    }

    private int getNextId() {
        return sLastId.getAndIncrement();
    }

    /**
     * Create a {@link Camera2OutputConfig} from the {@link Camera2OutputConfigImpl}.
     */
    @NonNull
    static Camera2OutputConfigBuilder fromImpl(@NonNull Camera2OutputConfigImpl impl) {
        OutputConfig outputConfig = null;
        if (impl instanceof SurfaceOutputConfigImpl) {
            SurfaceOutputConfigImpl surfaceImpl = (SurfaceOutputConfigImpl) impl;
            outputConfig = SurfaceConfig.create(surfaceImpl.getSurface());
        } else if (impl instanceof ImageReaderOutputConfigImpl) {
            ImageReaderOutputConfigImpl imageReaderImpl = (ImageReaderOutputConfigImpl) impl;
            outputConfig = ImageReaderConfig.create(imageReaderImpl.getSize(),
                    imageReaderImpl.getImageFormat(), imageReaderImpl.getMaxImages());
        } else if (impl instanceof MultiResolutionImageReaderOutputConfigImpl) {
            MultiResolutionImageReaderOutputConfigImpl multiResolutionImageReaderImpl =
                    (MultiResolutionImageReaderOutputConfigImpl) impl;
            outputConfig = MultiResolutionImageReaderConfig.create(
                    multiResolutionImageReaderImpl.getImageFormat(),
                    multiResolutionImageReaderImpl.getMaxImages());
        }

        outputConfig.setPhysicalCameraId(impl.getPhysicalCameraId());
        outputConfig.setSurfaceGroup(impl.getSurfaceGroupId());
        if (impl.getSurfaceSharingOutputConfigs() != null) {
            ArrayList<Camera2OutputConfig> surfaceSharingConfigs = new ArrayList<>();
            for (Camera2OutputConfigImpl surfaceSharingOutputConfig :
                    impl.getSurfaceSharingOutputConfigs()) {
                Camera2OutputConfigBuilder subBuilder =
                        Camera2OutputConfigBuilder.fromImpl(surfaceSharingOutputConfig);
                surfaceSharingConfigs.add(subBuilder.build());
            }
            outputConfig.setSurfaceSharingConfigs(surfaceSharingConfigs);
        }

        return new Camera2OutputConfigBuilder(outputConfig);
    }

    /**
     * Creates a {@link Camera2OutputConfig} that represents a {@link android.media.ImageReader}
     * with the given parameters.
     */
    @NonNull
    static Camera2OutputConfigBuilder newImageReaderConfig(
            @NonNull Size size, int imageFormat, int maxImages) {
        return new Camera2OutputConfigBuilder(
                 ImageReaderConfig.create(size, imageFormat, maxImages));
    }

    /**
     * Creates a {@link Camera2OutputConfig} that represents a MultiResolutionImageReader with the
     * given parameters.
     */
    @NonNull
    static Camera2OutputConfigBuilder newMultiResolutionImageReaderConfig(
            int imageFormat, int maxImages) {
        return new Camera2OutputConfigBuilder(
                MultiResolutionImageReaderConfig.create(imageFormat, maxImages));
    }

    /**
     * Creates a {@link Camera2OutputConfig} that contains the Surface directly.
     */
    @NonNull
    static Camera2OutputConfigBuilder newSurfaceConfig(@NonNull Surface surface) {
        return new Camera2OutputConfigBuilder(SurfaceConfig.create(surface));
    }

    /**
     * Adds a {@link Camera2SessionConfig} to be shared with current config.
     */
    @NonNull
    Camera2OutputConfigBuilder addSurfaceSharingOutputConfig(
            @NonNull Camera2OutputConfig camera2OutputConfig) {
        if (mSurfaceSharingConfigs == null) {
            mSurfaceSharingConfigs = new ArrayList<>();
        }

        mSurfaceSharingConfigs.add(camera2OutputConfig);
        return this;
    }

    /**
     * Sets a physical camera id.
     */
    @NonNull
    Camera2OutputConfigBuilder setPhysicalCameraId(@NonNull String physicalCameraId) {
        mPhysicalCameraId = physicalCameraId;
        return this;
    }

    /**
     * Sets surface group id.
     */
    @NonNull
    Camera2OutputConfigBuilder setSurfaceGroupId(int surfaceGroupId) {
        mSurfaceGroupId = surfaceGroupId;
        return this;
    }

    /**
     * Build a {@link Camera2OutputConfig} instance.
     */
    @NonNull
    Camera2OutputConfig build() {
        mOutputConfig.setId(getNextId());
        mOutputConfig.setPhysicalCameraId(mPhysicalCameraId);
        mOutputConfig.setSurfaceGroup(mSurfaceGroupId);
        if (mSurfaceSharingConfigs != null) {
            mOutputConfig.setSurfaceSharingConfigs(mSurfaceSharingConfigs);
        }
        return mOutputConfig;
    }

    private static class OutputConfig implements Camera2OutputConfig {
        private int mId;
        private int mSurfaceGroup;
        private String mPhysicalCameraId;
        private List<Camera2OutputConfig> mSurfaceSharingConfigs;

        OutputConfig() {
            mId = -1;
            mSurfaceGroup = 0;
            mPhysicalCameraId = null;
            mSurfaceSharingConfigs = Collections.emptyList();
        }

        @Override
        public int getId() {
            return mId;
        }

        @Override
        public int getSurfaceGroupId() {
            return mSurfaceGroup;
        }

        @Override
        @Nullable
        public String getPhysicalCameraId() {
            return mPhysicalCameraId;
        }

        @Override
        @NonNull
        public List<Camera2OutputConfig> getSurfaceSharingOutputConfigs() {
            return mSurfaceSharingConfigs;
        }

        public void setId(int id) {
            mId = id;
        }

        public void setSurfaceGroup(int surfaceGroup) {
            mSurfaceGroup = surfaceGroup;
        }

        public void setPhysicalCameraId(@Nullable String physicalCameraId) {
            mPhysicalCameraId = physicalCameraId;
        }

        public void setSurfaceSharingConfigs(
                @NonNull List<Camera2OutputConfig> surfaceSharingConfigs) {
            mSurfaceSharingConfigs = surfaceSharingConfigs;
        }
    }

    @AutoValue
    abstract static class SurfaceConfig extends OutputConfig implements SurfaceOutputConfig {
        static SurfaceConfig create(@NonNull Surface surface) {
            return new AutoValue_Camera2OutputConfigBuilder_SurfaceConfig(surface);
        }

        @Override
        @NonNull
        public abstract Surface getSurface();
    }

    @AutoValue
    abstract static class ImageReaderConfig extends OutputConfig implements
            ImageReaderOutputConfig {
        static ImageReaderConfig create(@NonNull Size size, int imageFormat,
                int maxImages) {
            return new AutoValue_Camera2OutputConfigBuilder_ImageReaderConfig(
                    size, imageFormat, maxImages);
        }

        @Override
        @NonNull
        public abstract Size getSize();

        @Override
        public abstract int getImageFormat();

        @Override
        public abstract int getMaxImages();
    }

    @AutoValue
    abstract static class MultiResolutionImageReaderConfig extends OutputConfig implements
            MultiResolutionImageReaderOutputConfig {
        static MultiResolutionImageReaderConfig create(int imageFormat, int maxImages) {
            return new AutoValue_Camera2OutputConfigBuilder_MultiResolutionImageReaderConfig(
                    imageFormat, maxImages);
        }

        @Override
        public abstract int getImageFormat();

        @Override
        public abstract int getMaxImages();
    }
}
