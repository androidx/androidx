/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.internal.compat.workaround.OutputSizesCorrector;
import androidx.camera.core.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper for accessing features in {@link StreamConfigurationMap} in a backwards compatible
 * fashion.
 */
public class StreamConfigurationMapCompat {
    private static final String TAG = "StreamConfigurationMapCompat";

    private final StreamConfigurationMapCompatImpl mImpl;
    private final OutputSizesCorrector mOutputSizesCorrector;
    private final Map<Integer, Size[]> mCachedFormatOutputSizes = new HashMap<>();
    private final Map<Integer, Size[]> mCachedFormatHighResolutionOutputSizes = new HashMap<>();
    private final Map<Class<?>, Size[]> mCachedClassOutputSizes = new HashMap<>();

    private StreamConfigurationMapCompat(@NonNull StreamConfigurationMap map,
            @NonNull OutputSizesCorrector outputSizesCorrector) {
        if (Build.VERSION.SDK_INT >= 23) {
            mImpl = new StreamConfigurationMapCompatApi23Impl(map);
        } else {
            mImpl = new StreamConfigurationMapCompatBaseImpl(map);
        }
        mOutputSizesCorrector = outputSizesCorrector;
    }

    /**
     * Provides a backward-compatible wrapper for {@link StreamConfigurationMap}.
     *
     * @param map {@link StreamConfigurationMap} class to wrap
     * @param outputSizesCorrector {@link OutputSizesCorrector} which can apply the related
     *                                                         workarounds when output sizes are
     *                                                         retrieved.
     * @return wrapped class
     */
    @NonNull
    static StreamConfigurationMapCompat toStreamConfigurationMapCompat(
            @NonNull StreamConfigurationMap map,
            @NonNull OutputSizesCorrector outputSizesCorrector) {
        return new StreamConfigurationMapCompat(map, outputSizesCorrector);
    }


    /**
     * Get the image format output formats in this stream configuration.
     *
     * <p>All image formats returned by this function will be defined in either ImageFormat or in
     * PixelFormat.
     *
     * @return an array of integer format
     * @see ImageFormat
     * @see PixelFormat
     */
    @Nullable
    public int[] getOutputFormats() {
        int[] result = mImpl.getOutputFormats();
        return result == null ? null : result.clone();
    }

    /**
     * Get a list of sizes compatible with the requested image {@code format}.
     *
     * <p>Output sizes related quirks will be applied onto the returned sizes list.
     *
     * @param format an image format from {@link ImageFormat} or {@link PixelFormat}
     * @return an array of supported sizes, or {@code null} if the {@code format} is not a
     * supported output
     * @see ImageFormat
     * @see PixelFormat
     */
    @Nullable
    public Size[] getOutputSizes(int format) {
        if (mCachedFormatOutputSizes.containsKey(format)) {
            Size[] cachedOutputSizes = mCachedFormatOutputSizes.get(format);
            return cachedOutputSizes == null ? null : mCachedFormatOutputSizes.get(format).clone();
        }

        Size[] outputSizes = mImpl.getOutputSizes(format);

        if (outputSizes == null || outputSizes.length == 0) {
            Logger.w(TAG, "Retrieved output sizes array is null or empty for format " + format);
            return outputSizes;
        }

        outputSizes = mOutputSizesCorrector.applyQuirks(outputSizes, format);
        mCachedFormatOutputSizes.put(format, outputSizes);
        return outputSizes.clone();
    }

    /**
     * Get a list of sizes compatible with {@code klass} to use as an output.
     *
     * <p>Output sizes related quirks will be applied onto the returned sizes list.
     *
     * @param klass a non-{@code null} {@link Class} object reference
     * @return an array of supported sizes for {@link ImageFormat#PRIVATE} format,
     * or {@code null} iff the {@code klass} is not a supported output.
     * @throws NullPointerException if {@code klass} was {@code null}
     */
    @Nullable
    public <T> Size[] getOutputSizes(@NonNull Class<T> klass) {
        if (mCachedClassOutputSizes.containsKey(klass)) {
            Size[] cachedOutputSizes = mCachedClassOutputSizes.get(klass);
            return cachedOutputSizes == null ? null : mCachedClassOutputSizes.get(klass).clone();
        }

        Size[] outputSizes = mImpl.getOutputSizes(klass);

        if (outputSizes == null || outputSizes.length == 0) {
            Logger.w(TAG, "Retrieved output sizes array is null or empty for class " + klass);
            return outputSizes;
        }

        outputSizes = mOutputSizesCorrector.applyQuirks(outputSizes, klass);
        mCachedClassOutputSizes.put(klass, outputSizes);
        return outputSizes.clone();
    }

    /**
     * Get a list of high resolution sizes compatible with the requested image {@code format}.
     *
     * @param format an image format from {@link ImageFormat} or {@link PixelFormat}
     * @return an array of supported sizes, or {@code null} if the {@code format} is not a
     * supported output
     * @see ImageFormat
     * @see PixelFormat
     */
    @Nullable
    public Size[] getHighResolutionOutputSizes(int format) {
        if (mCachedFormatHighResolutionOutputSizes.containsKey(format)) {
            Size[] cachedOutputSizes = mCachedFormatHighResolutionOutputSizes.get(format);
            return cachedOutputSizes == null ? null : mCachedFormatHighResolutionOutputSizes.get(
                    format).clone();
        }

        Size[] outputSizes = mImpl.getHighResolutionOutputSizes(format);

        // High resolution output sizes can be null.
        if (outputSizes != null && outputSizes.length > 0) {
            outputSizes = mOutputSizesCorrector.applyQuirks(outputSizes, format);
        }

        mCachedFormatHighResolutionOutputSizes.put(format, outputSizes);
        return outputSizes != null ? outputSizes.clone() : null;
    }

    /**
     * Returns the {@link StreamConfigurationMap} represented by this object.
     */
    @NonNull
    public StreamConfigurationMap toStreamConfigurationMap() {
        return mImpl.unwrap();
    }

    interface StreamConfigurationMapCompatImpl {

        @Nullable
        int[] getOutputFormats();

        @Nullable
        Size[] getOutputSizes(int format);

        @Nullable
        <T> Size[] getOutputSizes(@NonNull Class<T> klass);

        @Nullable
        Size[] getHighResolutionOutputSizes(int format);

        /**
         * Returns the underlying {@link StreamConfigurationMap} instance.
         */
        @NonNull
        StreamConfigurationMap unwrap();
    }
}
