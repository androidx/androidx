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

package androidx.camera.extensions.impl.advanced;

import android.hardware.camera2.params.DynamicRangeProfiles;
import android.hardware.camera2.params.OutputConfiguration;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A builder implementation to help OEM build the {@link Camera2OutputConfigImpl} instance.
 */
public class Camera2OutputConfigImplBuilder {
    static AtomicInteger sLastId = new AtomicInteger(0);
    private OutputConfigImplImpl mOutputConfig;
    private int mSurfaceGroupId = OutputConfiguration.SURFACE_GROUP_ID_NONE;
    private int mOutputConfigId = -1;
    private String mPhysicalCameraId;
    private List<Camera2OutputConfigImpl> mSurfaceSharingConfigs;
    private long mDynamicRangeProfile = DynamicRangeProfiles.STANDARD;
    private Camera2OutputConfigImplBuilder(@NonNull OutputConfigImplImpl outputConfig) {
        mOutputConfig = outputConfig;
    }

    private int getNextId() {
        return sLastId.getAndIncrement();
    }

    /**
     * Creates a {@link Camera2OutputConfigImpl} that represents a {@link android.media.ImageReader}
     * with the given parameters.
     */
    @NonNull
    public static Camera2OutputConfigImplBuilder newImageReaderConfig(
            @NonNull Size size, int imageFormat, int maxImages) {
        return new Camera2OutputConfigImplBuilder(
                new ImageReaderOutputConfigImplImpl(size, imageFormat, maxImages));
    }


    /**
     * Creates a {@link Camera2OutputConfigImpl} that represents a {@link android.media.ImageReader}
     * with the given parameters.
     */
    @NonNull
    public static Camera2OutputConfigImplBuilder newImageReaderConfig(
            @NonNull Size size, int imageFormat, int maxImages, long usage) {
        return new Camera2OutputConfigImplBuilder(
                new ImageReaderOutputConfigImplImpl(size, imageFormat, maxImages, usage));
    }

    /**
     * Creates a {@link Camera2OutputConfigImpl} that represents a MultiResolutionImageReader with
     * the given parameters.
     */
    @NonNull
    public static Camera2OutputConfigImplBuilder newMultiResolutionImageReaderConfig(
            int imageFormat, int maxImages) {
        return new Camera2OutputConfigImplBuilder(
                new MultiResolutionImageReaderOutputConfigImplImpl(imageFormat, maxImages));
    }

    /**
     * Creates a {@link Camera2OutputConfigImpl} that contains the Surface directly.
     */
    @NonNull
    public static Camera2OutputConfigImplBuilder newSurfaceConfig(@NonNull Surface surface) {
        return new Camera2OutputConfigImplBuilder(new SurfaceOutputConfigImplImpl(surface));
    }

    /**
     * Adds a {@link Camera2SessionConfigImpl} to be shared with current config.
     */
    @NonNull
    public Camera2OutputConfigImplBuilder addSurfaceSharingOutputConfig(
            @NonNull Camera2OutputConfigImpl camera2OutputConfig) {
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
    public Camera2OutputConfigImplBuilder setPhysicalCameraId(@Nullable String physicalCameraId) {
        mPhysicalCameraId = physicalCameraId;
        return this;
    }

    /**
     * Sets surface group id.
     */
    @NonNull
    public Camera2OutputConfigImplBuilder setSurfaceGroupId(int surfaceGroupId) {
        mSurfaceGroupId = surfaceGroupId;
        return this;
    }

    /**
     * Sets Output Config id (Optional: Atomic Integer will be used if this function is not called)
     */
    @NonNull
    public Camera2OutputConfigImplBuilder setOutputConfigId(int outputConfigId) {
        mOutputConfigId = outputConfigId;
        return this;
    }

    /**
     * Set dynamic range profile.
     */
    @NonNull
    public Camera2OutputConfigImplBuilder setDynamicRangeProfile(long dynamicRangeProfile) {
        mDynamicRangeProfile = dynamicRangeProfile;
        return this;
    }

    /**
     * Build a {@link Camera2OutputConfigImpl} instance.
     */
    @NonNull
    public Camera2OutputConfigImpl build() {
        // Sets an output config id otherwise an output config id will be generated
        if (mOutputConfigId == -1) {
            mOutputConfig.setId(getNextId());
        } else {
            mOutputConfig.setId(mOutputConfigId);
        }
        mOutputConfig.setPhysicalCameraId(mPhysicalCameraId);
        mOutputConfig.setSurfaceGroup(mSurfaceGroupId);
        mOutputConfig.setSurfaceSharingConfigs(mSurfaceSharingConfigs);
        mOutputConfig.setDynamicRangeProfile(mDynamicRangeProfile);
        return mOutputConfig;
    }

    private static class OutputConfigImplImpl implements Camera2OutputConfigImpl {
        private int mId;
        private int mSurfaceGroup;
        private String mPhysicalCameraId;
        private long mDynamicRangeProfile;
        private List<Camera2OutputConfigImpl> mSurfaceSharingConfigs;

        OutputConfigImplImpl() {
            mId = -1;
            mSurfaceGroup = 0;
            mPhysicalCameraId = null;
            mSurfaceSharingConfigs = null;
            mDynamicRangeProfile = DynamicRangeProfiles.STANDARD;
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
        public long getDynamicRangeProfile() {
            return mDynamicRangeProfile;
        }

        @Override
        @Nullable
        public List<Camera2OutputConfigImpl> getSurfaceSharingOutputConfigs() {
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

        public void setDynamicRangeProfile(long dynamicRangeProfile) {
            mDynamicRangeProfile = dynamicRangeProfile;
        }

        public void setSurfaceSharingConfigs(
                @Nullable List<Camera2OutputConfigImpl> surfaceSharingConfigs) {
            if (surfaceSharingConfigs != null) {
                surfaceSharingConfigs = new ArrayList<>(surfaceSharingConfigs);
            }
            mSurfaceSharingConfigs = surfaceSharingConfigs;
        }
    }

    private static class SurfaceOutputConfigImplImpl extends OutputConfigImplImpl
            implements SurfaceOutputConfigImpl {
        private final Surface mSurface;

        SurfaceOutputConfigImplImpl(@NonNull Surface surface) {
            mSurface = surface;
        }

        @Override
        @NonNull
        public Surface getSurface() {
            return mSurface;
        }
    }

    private static class ImageReaderOutputConfigImplImpl extends OutputConfigImplImpl
            implements ImageReaderOutputConfigImpl {
        private static final long USAGE_UNSPECIFIED = -1;
        private final Size mSize;
        private final int mImageFormat;
        private final int mMaxImages;
        private final long mUsage;

        ImageReaderOutputConfigImplImpl(@NonNull Size size, int imageFormat, int maxImages) {
            this(size, imageFormat, maxImages, USAGE_UNSPECIFIED);
        }

        ImageReaderOutputConfigImplImpl(@NonNull Size size, int imageFormat, int maxImages,
                long usage) {
            mSize = size;
            mImageFormat = imageFormat;
            mMaxImages = maxImages;
            mUsage = usage;
        }

        @Override
        @NonNull
        public Size getSize() {
            return mSize;
        }

        @Override
        public int getImageFormat() {
            return mImageFormat;
        }

        @Override
        public int getMaxImages() {
            return mMaxImages;
        }

        @Override
        public long getUsage() {
            if (mUsage == USAGE_UNSPECIFIED) {
                return ImageReaderOutputConfigImpl.super.getUsage();
            }
            return mUsage;
        }
    }

    private static class MultiResolutionImageReaderOutputConfigImplImpl extends OutputConfigImplImpl
            implements MultiResolutionImageReaderOutputConfigImpl {
        private final int mImageFormat;
        private final int mMaxImages;

        MultiResolutionImageReaderOutputConfigImplImpl(int imageFormat, int maxImages) {
            mImageFormat = imageFormat;
            mMaxImages = maxImages;
        }

        @Override
        public int getImageFormat() {
            return mImageFormat;
        }

        @Override
        public int getMaxImages() {
            return mMaxImages;
        }
    }
}
