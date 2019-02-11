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

import android.graphics.ImageFormat;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.util.Size;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Different implementations of {@link ImageReaderProxy}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class ImageReaderProxys {
    private static final String TAG = ImageReaderProxys.class.getSimpleName();
    private static final int SHARED_IMAGE_FORMAT = ImageFormat.YUV_420_888;
    private static final int SHARED_MAX_IMAGES = 8;
    private static final List<QueuedImageReaderProxy> sharedImageReaderProxys = new ArrayList<>();
    private static Set<DeviceProperties> sharedReaderWhitelist;
    private static ImageReader sharedImageReader;

    private ImageReaderProxys() {
    }

    /**
     * Creates an {@link ImageReaderProxy} which chooses a device-compatible implementation.
     *
     * @param cameraId  of the target camera
     * @param width     of the reader
     * @param height    of the reader
     * @param format    of the reader
     * @param maxImages of the reader
     * @param handler   for on-image-available callbacks
     * @return new {@link ImageReaderProxy} instance
     */
    static ImageReaderProxy createCompatibleReader(
            String cameraId, int width, int height, int format, int maxImages, Handler handler) {
        if (inSharedReaderWhitelist(DeviceProperties.create())) {
            return createSharedReader(cameraId, width, height, format, maxImages, handler);
        } else {
            return createIsolatedReader(width, height, format, maxImages, handler);
        }
    }

    /**
     * Creates an {@link ImageReaderProxy} which uses its own isolated {@link ImageReader}.
     *
     * @param width     of the reader
     * @param height    of the reader
     * @param format    of the reader
     * @param maxImages of the reader
     * @param handler   for on-image-available callbacks
     * @return new {@link ImageReaderProxy} instance
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static ImageReaderProxy createIsolatedReader(
            int width, int height, int format, int maxImages, Handler handler) {
        ImageReader imageReader = ImageReader.newInstance(width, height, format, maxImages);
        return new AndroidImageReaderProxy(imageReader);
    }

    /**
     * Creates an {@link ImageReaderProxy} which shares an underlying {@link ImageReader}.
     *
     * @param cameraId  of the target camera
     * @param width     of the reader
     * @param height    of the reader
     * @param format    of the reader
     * @param maxImages of the reader
     * @param handler   for on-image-available callbacks
     * @return new {@link ImageReaderProxy} instance
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static ImageReaderProxy createSharedReader(
            String cameraId, int width, int height, int format, int maxImages, Handler handler) {
        if (sharedImageReader == null) {
            Size resolution =
                    CameraX.getSurfaceManager().getMaxOutputSize(cameraId, SHARED_IMAGE_FORMAT);
            Log.d(TAG, "Resolution of base ImageReader: " + resolution);
            sharedImageReader =
                    ImageReader.newInstance(
                            resolution.getWidth(),
                            resolution.getHeight(),
                            SHARED_IMAGE_FORMAT,
                            SHARED_MAX_IMAGES);
        }
        Log.d(TAG, "Resolution of forked ImageReader: " + new Size(width, height));
        QueuedImageReaderProxy imageReaderProxy =
                new QueuedImageReaderProxy(
                        width, height, format, maxImages, sharedImageReader.getSurface());
        sharedImageReaderProxys.add(imageReaderProxy);
        sharedImageReader.setOnImageAvailableListener(
                new ForwardingImageReaderListener(sharedImageReaderProxys), handler);
        imageReaderProxy.addOnReaderCloseListener(
                reader -> {
                    sharedImageReaderProxys.remove(reader);
                    if (sharedImageReaderProxys.isEmpty()) {
                        clearSharedReaders();
                    }
                });
        return imageReaderProxy;
    }

    /**
     * Returns true if the device is in the shared reader whitelist.
     *
     * <p>Devices in the whitelist are known to work with shared readers. Devices outside the
     * whitelist may also work with shared readers, but they have not been tested yet.
     *
     * @param device to check
     * @return true if device is in whitelist
     */
    static boolean inSharedReaderWhitelist(DeviceProperties device) {
        if (sharedReaderWhitelist == null) {
            sharedReaderWhitelist = new HashSet<>();
            for (int sdkVersion = 21; sdkVersion <= 27; ++sdkVersion) {
                sharedReaderWhitelist.add(DeviceProperties.create("Google", "Pixel", sdkVersion));
                sharedReaderWhitelist.add(
                        DeviceProperties.create("Google", "Pixel XL", sdkVersion));
                sharedReaderWhitelist.add(
                        DeviceProperties.create("HMD Global", "Nokia 8.1", sdkVersion));
            }
        }
        return sharedReaderWhitelist.contains(device);
    }

    private static void clearSharedReaders() {
        sharedImageReaderProxys.clear();
        sharedImageReader.setOnImageAvailableListener(null, null);
        sharedImageReader.close();
        sharedImageReader = null;
    }
}
