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

import android.annotation.SuppressLint;
import android.hardware.camera2.params.OutputConfiguration;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A builder implementation to help OEM build the {@link Camera2OutputConfigImpl} instance.
 */
@SuppressLint("UnknownNullness")
public class Camera2OutputConfigImplBuilder {
    static AtomicInteger sLastId = new AtomicInteger(0);
    private OutputConfigImplImpl mOutputConfig;
    private int mSurfaceGroupId = OutputConfiguration.SURFACE_GROUP_ID_NONE;
    private String mPhysicalCameraId;
    private List<Camera2OutputConfigImpl> mSurfaceSharingConfigs;

    private Camera2OutputConfigImplBuilder(OutputConfigImplImpl outputConfig) {
        mOutputConfig = outputConfig;
    }

    private int getNextId() {
        return sLastId.getAndIncrement();
    }

    /**
     * Creates a {@link Camera2OutputConfigImpl} that represents a {@link android.media.ImageReader}
     * with the given parameters.
     */
    public static Camera2OutputConfigImplBuilder newImageReaderConfig(
            Size size, int imageFormat, int maxImages) {
        return new Camera2OutputConfigImplBuilder(
                new ImageReaderOutputConfigImplImpl(size, imageFormat, maxImages));
    }

    /**
     * Creates a {@link Camera2OutputConfigImpl} that represents a MultiResolutionImageReader with
     * the given parameters.
     */
    public static Camera2OutputConfigImplBuilder newMultiResolutionImageReaderConfig(
            int imageFormat, int maxImages) {
        return new Camera2OutputConfigImplBuilder(
                new MultiResolutionImageReaderOutputConfigImplImpl(imageFormat, maxImages));
    }

    /**
     * Creates a {@link Camera2OutputConfigImpl} that contains the Surface directly.
     */
    public static Camera2OutputConfigImplBuilder newSurfaceConfig(Surface surface) {
        return new Camera2OutputConfigImplBuilder(new SurfaceOutputConfigImplImpl(surface));
    }

    /**
     * Adds a {@link Camera2SessionConfigImpl} to be shared with current config.
     */
    public Camera2OutputConfigImplBuilder addSurfaceSharingOutputConfig(
            Camera2OutputConfigImpl camera2OutputConfig) {
        if (mSurfaceSharingConfigs == null) {
            mSurfaceSharingConfigs = new ArrayList<>();
        }

        mSurfaceSharingConfigs.add(camera2OutputConfig);
        return this;
    }

    /**
     * Sets a physical camera id.
     */
    public Camera2OutputConfigImplBuilder setPhysicalCameraId(String physicalCameraId) {
        mPhysicalCameraId = physicalCameraId;
        return this;
    }

    /**
     * Sets surface group id.
     */
    public Camera2OutputConfigImplBuilder setSurfaceGroupId(int surfaceGroupId) {
        mSurfaceGroupId = surfaceGroupId;
        return this;
    }

    /**
     * Build a {@link Camera2OutputConfigImpl} instance.
     */
    public Camera2OutputConfigImpl build() {
        mOutputConfig.setId(getNextId());
        mOutputConfig.setPhysicalCameraId(mPhysicalCameraId);
        mOutputConfig.setSurfaceGroup(mSurfaceGroupId);
        mOutputConfig.setSurfaceSharingConfigs(mSurfaceSharingConfigs);
        return mOutputConfig;
    }

    private static class OutputConfigImplImpl implements Camera2OutputConfigImpl {
        private int mId;
        private int mSurfaceGroup;
        private String mPhysicalCameraId;
        private List<Camera2OutputConfigImpl> mSurfaceSharingConfigs;

        OutputConfigImplImpl() {
            mId = -1;
            mSurfaceGroup = 0;
            mPhysicalCameraId = null;
            mSurfaceSharingConfigs = null;
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
        public String getPhysicalCameraId() {
            return mPhysicalCameraId;
        }

        @Override
        public List<Camera2OutputConfigImpl> getSurfaceSharingOutputConfigs() {
            return mSurfaceSharingConfigs;
        }

        public void setId(int id) {
            mId = id;
        }

        public void setSurfaceGroup(int surfaceGroup) {
            mSurfaceGroup = surfaceGroup;
        }

        public void setPhysicalCameraId(String physicalCameraId) {
            mPhysicalCameraId = physicalCameraId;
        }

        public void setSurfaceSharingConfigs(
                List<Camera2OutputConfigImpl> surfaceSharingConfigs) {
            mSurfaceSharingConfigs = surfaceSharingConfigs;
        }
    }

    private static class SurfaceOutputConfigImplImpl extends OutputConfigImplImpl
            implements SurfaceOutputConfigImpl {
        private Surface mSurface;

        SurfaceOutputConfigImplImpl(Surface surface) {
            mSurface = surface;
        }

        @Override
        public Surface getSurface() {
            return mSurface;
        }
    }

    private static class ImageReaderOutputConfigImplImpl extends OutputConfigImplImpl
            implements ImageReaderOutputConfigImpl {
        private Size mSize;
        private int mImageFormat;
        private int mMaxImages;

        ImageReaderOutputConfigImplImpl(Size size, int imageFormat, int maxImages) {
            mSize = size;
            mImageFormat = imageFormat;
            mMaxImages = maxImages;
        }

        @Override
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
    }

    private static class MultiResolutionImageReaderOutputConfigImplImpl extends OutputConfigImplImpl
            implements MultiResolutionImageReaderOutputConfigImpl {
        private int mImageFormat;
        private int mMaxImages;

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
