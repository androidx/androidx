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
import android.util.Range;
import android.util.Size;

import androidx.camera.camera2.internal.compat.workaround.OutputSizesCorrector;
import androidx.camera.core.Logger;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    static @NonNull StreamConfigurationMapCompat toStreamConfigurationMapCompat(
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
    public int @Nullable [] getOutputFormats() {
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
    public Size @Nullable [] getOutputSizes(int format) {
        if (mCachedFormatOutputSizes.containsKey(format)) {
            Size[] cachedOutputSizes = mCachedFormatOutputSizes.get(format);
            return cachedOutputSizes == null ? null : mCachedFormatOutputSizes.get(format).clone();
        }

        Size[] outputSizes = null;
        try {
            // b/378508360: try-catch to workaround the exception when using
            // StreamConfigurationMap provided by Robolectric.
            outputSizes = mImpl.getOutputSizes(format);
        } catch (Throwable t) {
            Logger.w(TAG, "Failed to get output sizes for " + format, t);
        }

        if (outputSizes == null || outputSizes.length == 0) {
            Logger.w(TAG, "Retrieved output sizes array is null or empty for format " + format);
            return outputSizes;
        }

        outputSizes = mOutputSizesCorrector.applyQuirks(outputSizes, format);
        mCachedFormatOutputSizes.put(format, outputSizes);
        return outputSizes.clone();
    }

    /**
     * Get the minimum frame duration for the format/size combination (in nanoseconds).
     *
     * @return a minimum frame duration > 0 in nanoseconds, or 0 if the minimum frame duration is
     * not available.
     * @see StreamConfigurationMap#getOutputMinFrameDuration(int, Size)
     */
    public long getOutputMinFrameDuration(int format, @NonNull Size size) {
        try {
            return mImpl.getOutputMinFrameDuration(format, size);
        } catch (RuntimeException e) {
            Logger.w(TAG, "Failed to get min frame duration for format = " + format
                    + " and size = " + size, e);
        }
        return 0;
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
    public <T> Size @Nullable [] getOutputSizes(@NonNull Class<T> klass) {
        if (mCachedClassOutputSizes.containsKey(klass)) {
            Size[] cachedOutputSizes = mCachedClassOutputSizes.get(klass);
            return cachedOutputSizes == null ? null : mCachedClassOutputSizes.get(klass).clone();
        }

        Size[] outputSizes = null;
        try {
            // b/378508360: try-catch to workaround the exception when using
            // StreamConfigurationMap provided by Robolectric.
            outputSizes = mImpl.getOutputSizes(klass);
        } catch (Throwable t) {
            Logger.w(TAG, "Fail to get output sizes for " + klass, t);
        }

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
    public Size @Nullable [] getHighResolutionOutputSizes(int format) {
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

    /** Get a list of supported high speed video recording FPS ranges. */
    @Nullable
    public Range<Integer>[] getHighSpeedVideoFpsRanges() {
        return mImpl.getHighSpeedVideoFpsRanges();
    }

    /** Get the frame per second ranges (fpsMin, fpsMax) for input high speed video size. */
    @Nullable
    public Range<Integer>[] getHighSpeedVideoFpsRangesFor(@NonNull Size size)
            throws IllegalArgumentException {
        return mImpl.getHighSpeedVideoFpsRangesFor(size);
    }

    /** Get a list of supported high speed video recording sizes. */
    @Nullable
    public Size[] getHighSpeedVideoSizes() {
        return mImpl.getHighSpeedVideoSizes();
    }

    /** Get the supported video sizes for an input high speed FPS range. */
    @Nullable
    public Size[] getHighSpeedVideoSizesFor(@NonNull Range<Integer> fpsRange)
            throws IllegalArgumentException {
        return mImpl.getHighSpeedVideoSizesFor(fpsRange);
    }

    /**
     * Returns the {@link StreamConfigurationMap} represented by this object.
     */
    public @NonNull StreamConfigurationMap toStreamConfigurationMap() {
        return mImpl.unwrap();
    }

    interface StreamConfigurationMapCompatImpl {

        int @Nullable [] getOutputFormats();

        Size @Nullable [] getOutputSizes(int format);

        <T> Size @Nullable [] getOutputSizes(@NonNull Class<T> klass);

        long getOutputMinFrameDuration(int format, @NonNull Size size);

        Size @Nullable [] getHighResolutionOutputSizes(int format);

        @Nullable
        Range<Integer>[] getHighSpeedVideoFpsRanges();

        @Nullable
        Range<Integer>[] getHighSpeedVideoFpsRangesFor(@NonNull Size size)
                throws IllegalArgumentException;

        @Nullable
        Size[] getHighSpeedVideoSizes();

        @Nullable
        Size[] getHighSpeedVideoSizesFor(@NonNull Range<Integer> fpsRange)
                throws IllegalArgumentException;

        /**
         * Returns the underlying {@link StreamConfigurationMap} instance.
         */
        @NonNull StreamConfigurationMap unwrap();
    }
}
