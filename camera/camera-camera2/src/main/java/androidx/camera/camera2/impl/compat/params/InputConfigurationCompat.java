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

package androidx.camera.camera2.impl.compat.params;

import android.annotation.SuppressLint;
import android.hardware.camera2.params.InputConfiguration;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import java.util.Objects;

/**
 * Helper for accessing features in InputConfiguration in a backwards compatible fashion.
 */
@RequiresApi(21)
public final class InputConfigurationCompat {

    private final InputConfigurationCompatImpl mImpl;

    /**
     * Create an input configuration with the width, height, and user-defined format.
     *
     * <p>Images of an user-defined format are accessible by applications. Use
     * {@link android.hardware.camera2.CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP}
     * to query supported input formats</p>
     *
     * @param width  Width of the input buffers.
     * @param height Height of the input buffers.
     * @param format Format of the input buffers. One of ImageFormat or PixelFormat constants.
     * @see android.graphics.ImageFormat
     * @see android.graphics.PixelFormat
     * @see android.hardware.camera2.CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP
     */
    public InputConfigurationCompat(int width, int height, int format) {
        if (Build.VERSION.SDK_INT >= 23) {
            mImpl = new InputConfigurationCompatApi23Impl(width, height, format);
        } else {
            mImpl = new InputConfigurationCompatBaseImpl(width, height, format);
        }
    }

    private InputConfigurationCompat(@NonNull InputConfigurationCompatImpl impl) {
        mImpl = impl;
    }

    /**
     * Creates an instance from a framework android.hardware.camera2.params.InputConfiguration
     * object.
     *
     * <p>This method always returns {@code null} on API &lt;= 22.</p>
     *
     * @param inputConfiguration an android.hardware.camera2.params.InputConfiguration object, or
     *                           {@code null} if none.
     * @return an equivalent {@link InputConfigurationCompat} object, or {@code null} if not
     * supported.
     */
    @Nullable
    public static InputConfigurationCompat wrap(@Nullable Object inputConfiguration) {
        if (inputConfiguration == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT < 23) {
            return null;
        }
        return new InputConfigurationCompat(
                new InputConfigurationCompatApi23Impl(inputConfiguration));
    }

    /**
     * Get the width of this input configuration.
     *
     * @return width of this input configuration.
     */
    public int getWidth() {
        return mImpl.getWidth();
    }

    /**
     * Get the height of this input configuration.
     *
     * @return height of this input configuration.
     */
    public int getHeight() {
        return mImpl.getHeight();
    }

    /**
     * Get the format of this input configuration.
     *
     * @return format of this input configuration.
     */
    public int getFormat() {
        return mImpl.getFormat();
    }

    /**
     * Check if this InputConfiguration is equal to another InputConfiguration.
     *
     * <p>Two input configurations are equal if and only if they have the same widths, heights, and
     * formats.</p>
     *
     * @param obj the object to compare this instance with.
     * @return {@code true} if the objects were equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof InputConfigurationCompat)) {
            return false;
        }

        return mImpl.equals(((InputConfigurationCompat) obj).mImpl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return mImpl.hashCode();
    }

    /**
     * Return this {@link InputConfigurationCompat} as a string representation.
     *
     * <p> {@code "InputConfiguration(w:%d, h:%d, format:%d)"}, where {@code %d} represents
     * the width, height, and format, respectively.</p>
     *
     * @return string representation of {@link InputConfigurationCompat}
     */
    @Override
    public String toString() {
        return mImpl.toString();
    }

    /**
     * Gets the underlying framework android.hardware.camera2.params.InputConfiguration object.
     *
     * <p>This method always returns {@code null} on API &lt;= 22.</p>
     *
     * @return an equivalent android.hardware.camera2.params.InputConfiguration object, or {@code
     * null} if
     * not supported.
     */
    @Nullable
    public Object unwrap() {
        return mImpl.getInputConfiguration();
    }

    private interface InputConfigurationCompatImpl {
        int getWidth();

        int getHeight();

        int getFormat();

        @Nullable
        Object getInputConfiguration();
    }

    @VisibleForTesting
    static final class InputConfigurationCompatBaseImpl implements
            InputConfigurationCompatImpl {

        private final int mWidth;
        private final int mHeight;
        private final int mFormat;

        InputConfigurationCompatBaseImpl(int width, int height, int format) {
            mWidth = width;
            mHeight = height;
            mFormat = format;
        }

        @Override
        public int getWidth() {
            return mWidth;
        }

        @Override
        public int getHeight() {
            return mHeight;
        }

        @Override
        public int getFormat() {
            return mFormat;
        }

        @Override
        public Object getInputConfiguration() {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof InputConfigurationCompatBaseImpl)) {
                return false;
            }

            InputConfigurationCompatBaseImpl otherInputConfig =
                    (InputConfigurationCompatBaseImpl) obj;

            return otherInputConfig.getWidth() == mWidth
                    && otherInputConfig.getHeight() == mHeight
                    && otherInputConfig.getFormat() == mFormat;
        }

        @Override
        public int hashCode() {
            int h = 1;
            // Strength reduction; in case the compiler has illusions about divisions being faster
            h = ((h << 5) - h) ^ mWidth; // (h * 31) XOR mWidth
            h = ((h << 5) - h) ^ mHeight; // (h * 31) XOR mHeight
            h = ((h << 5) - h) ^ mFormat; // (h * 31) XOR mFormat

            return h;
        }

        @SuppressLint("DefaultLocale") // Implementation matches framework
        @Override
        public String toString() {
            return String.format("InputConfiguration(w:%d, h:%d, format:%d)", mWidth, mHeight,
                    mFormat);
        }
    }

    @RequiresApi(23)
    private static final class InputConfigurationCompatApi23Impl implements
            InputConfigurationCompatImpl {

        private final InputConfiguration mObject;

        InputConfigurationCompatApi23Impl(@NonNull Object inputConfiguration) {
            mObject = (InputConfiguration) inputConfiguration;
        }

        InputConfigurationCompatApi23Impl(int width, int height, int format) {
            this(new InputConfiguration(width, height, format));
        }

        @Override
        public int getWidth() {
            return mObject.getWidth();
        }

        @Override
        public int getHeight() {
            return mObject.getHeight();
        }

        @Override
        public int getFormat() {
            return mObject.getFormat();
        }

        @Nullable
        @Override
        public Object getInputConfiguration() {
            return mObject;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof InputConfigurationCompatImpl)) {
                return false;
            }
            return Objects.equals(mObject,
                    ((InputConfigurationCompatImpl) obj).getInputConfiguration());
        }

        @Override
        public int hashCode() {
            return mObject.hashCode();
        }

        @Override
        public String toString() {
            return mObject.toString();
        }
    }

}
