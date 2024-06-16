/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.camera.core.imagecapture;

import static androidx.core.util.Preconditions.checkState;

import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MetadataImageReader;
import androidx.camera.core.SettableImageProxy;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.internal.CameraCaptureResultImageInfo;
import androidx.camera.core.streamsharing.StreamSharing;
import androidx.camera.core.streamsharing.VirtualCameraCaptureResult;

import java.util.concurrent.Executor;

/**
 * A {@link ImageReaderProxy} wrapper that does not rely on {@link CameraCaptureResult} to
 * deliver {@link ImageProxy}.
 *
 * <p>As opposed to {@link MetadataImageReader} which merges incoming {@link ImageProxy} with
 * {@link CameraCaptureResult}, this class delivers incoming {@link ImageProxy} with a
 * virtual {@link CameraCaptureResult}. This is useful when the {@link CameraCaptureResult}
 * cannot be merged. For example, for Extensions and {@link StreamSharing}, the incoming
 * {@link CameraCaptureResult} does not have matching timestamps with {@link ImageProxy}.
 */
public class NoMetadataImageReader implements ImageReaderProxy {
    @NonNull
    private final ImageReaderProxy mWrappedImageReader;
    @Nullable
    private ProcessingRequest mPendingRequest;

    /**
     * Creates a new instance of {@link NoMetadataImageReader} by wrapping an existing
     * {@link ImageReaderProxy}.
     */
    NoMetadataImageReader(@NonNull ImageReaderProxy imageReaderProxy) {
        mWrappedImageReader = imageReaderProxy;
    }

    /**
     * Sets the {@link ProcessingRequest} that is pending to be processed.
     */
    void acceptProcessingRequest(@NonNull ProcessingRequest request) {
        checkState(mPendingRequest == null, "Pending request should be null");
        mPendingRequest = request;
    }

    void clearProcessingRequest() {
        mPendingRequest = null;
    }

    @Nullable
    @Override
    public ImageProxy acquireLatestImage() {
        return createImageProxyWithEmptyMetadata(mWrappedImageReader.acquireLatestImage());
    }

    @Nullable
    @Override
    public ImageProxy acquireNextImage() {
        return createImageProxyWithEmptyMetadata(mWrappedImageReader.acquireNextImage());
    }

    @Override
    public void close() {
        mWrappedImageReader.close();
    }

    @Override
    public int getHeight() {
        return mWrappedImageReader.getHeight();
    }

    @Override
    public int getWidth() {
        return mWrappedImageReader.getWidth();
    }

    @Override
    public int getImageFormat() {
        return mWrappedImageReader.getImageFormat();
    }

    @Override
    public int getMaxImages() {
        return mWrappedImageReader.getMaxImages();
    }

    @Nullable
    @Override
    public Surface getSurface() {
        return mWrappedImageReader.getSurface();
    }

    @Override
    public void setOnImageAvailableListener(@NonNull OnImageAvailableListener listener,
            @NonNull Executor executor) {
        mWrappedImageReader.setOnImageAvailableListener(
                imageReader -> listener.onImageAvailable(NoMetadataImageReader.this), executor);
    }

    @Override
    public void clearOnImageAvailableListener() {
        mWrappedImageReader.clearOnImageAvailableListener();
    }

    @Nullable
    private ImageProxy createImageProxyWithEmptyMetadata(@Nullable ImageProxy originalImage) {
        if (originalImage == null) {
            return null;
        }
        TagBundle tagBundle =
                (mPendingRequest == null) ? TagBundle.emptyBundle() :
                        TagBundle.create(new Pair<>(mPendingRequest.getTagBundleKey(),
                                mPendingRequest.getStageIds().get(0)));
        mPendingRequest = null;
        return new SettableImageProxy(originalImage,
                new Size(originalImage.getWidth(), originalImage.getHeight()),
                new CameraCaptureResultImageInfo(new VirtualCameraCaptureResult(
                        tagBundle, originalImage.getImageInfo().getTimestamp())));
    }
}
