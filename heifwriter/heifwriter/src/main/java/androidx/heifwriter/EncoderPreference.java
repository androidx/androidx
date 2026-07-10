/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.heifwriter;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines the configuration for an encoder, including hardware/software preference
 * and constant quality (CQ) bitrate mode preference.
 */
public class EncoderPreference {
    /**
     * This is the default mode. System will use its default behavior.
     */
    public static final int NO_ENCODER_PREFERENCE = 0;

    /**
     * Only use a hardware encoder.
     */
    public static final int HARDWARE_ENCODER_ONLY = 1;

    /**
     * Prefer a hardware encoder, but fall back to a software encoder.
     */
    public static final int HARDWARE_ENCODER_PREFERRED = 2;
    /**
     * Only use a software encoder.
     */
    public static final int SOFTWARE_ENCODER_ONLY = 3;
    /**
     * Prefer a software encoder, but fall back to a hardware encoder.
     */
    public static final int SOFTWARE_ENCODER_PREFERRED = 4;

    /**
     * This is the default mode. Constant quality is not enforced,
     * but encoders with constant quality support are prioritized.
     */
    public static final int CONSTANT_QUALITY_MODE_PREFERRED = 0;

    /**
     * Only choose the encoder that supports constant quality mode.
     */
    public static final int CONSTANT_QUALITY_MODE_ONLY = 1;

    /**
     * Representing the hardware or software preference for the encoder.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        NO_ENCODER_PREFERENCE,
        HARDWARE_ENCODER_ONLY,
        HARDWARE_ENCODER_PREFERRED,
        SOFTWARE_ENCODER_ONLY,
        SOFTWARE_ENCODER_PREFERRED,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EncoderType {}

    /**
     * Representing the constant quality (CQ) bitrate mode preference.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        CONSTANT_QUALITY_MODE_PREFERRED, CONSTANT_QUALITY_MODE_ONLY,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface BitrateMode {}

    private final @EncoderType int mEncodertype;
    private final @BitrateMode int mBitrateMode;

    /**
     * Builder class for constructing a {@link EncoderPreference} object from specified parameters.
     */
    public static final class Builder {
        private @EncoderType int mEncoderType = NO_ENCODER_PREFERENCE;
        private @BitrateMode int mBitrateMode = CONSTANT_QUALITY_MODE_PREFERRED;

        /**
         * Creates a new Builder with default preference settings.
         */
        public Builder() {}

        /**
         * Sets the preferred encoding type.
         *
         * @param encoderType The preferred encoding type (HARDWARE or SOFTWARE).
         * @return The Builder object to chain calls.
         */
        public @NonNull Builder setEncoderType(@EncoderType int encoderType) {
            this.mEncoderType = encoderType;
            return this;
        }

        /**
         * Sets the preferred bitrate mode.
         *
         * @param bitrateMode The preferred bitrate mode.
         * @return The Builder object to chain calls.
         */
        public @NonNull Builder setBitrateMode(@BitrateMode int bitrateMode) {
            this.mBitrateMode = bitrateMode;
            return this;
        }

        /**
         * Creates an {@link EncoderPreference} object from the current settings.
         *
         * @return The immutable EncoderPreference object.
         */
        public @NonNull EncoderPreference build() {
            return new EncoderPreference(mEncoderType, mBitrateMode);
        }
    }

    /**
     * Constructs an EncoderPreference object with the specified preferences.
     *
     * @param encodertype The preferred encoding type (HARDWARE or SOFTWARE).
     * @param bitrateMode The preferred bitrate mode (CONSTANT_QUALITY, VARIABLE_BITRATE,
     *                    or CONSTANT_BITRATE).
     */
    private EncoderPreference(@EncoderType int encodertype, @BitrateMode int bitrateMode) {
        this.mEncodertype = encodertype;
        this.mBitrateMode = bitrateMode;
    }

    /**
     * Gets the default encoder preference.
     *
     * @return The EncoderPreference object.
     */
    public static @NonNull EncoderPreference getDefaultEncoderPreference() {
        return new Builder().build();
    }

    /**
     * Gets the preferred encoding type.
     *
     * @return The EncoderType.
     */
    public @EncoderType int getEncoderType() {
        return mEncodertype;
    }

    /**
     * Gets the preferred bitrate mode.
     *
     * @return The BitrateMode.
     */
    public @BitrateMode int getBitrateMode() {
        return mBitrateMode;
    }

    @Override
    public String toString() {
      return "EncoderPreference: encodertype=" + getEncoderType() +
              ", bitrateMode=" + getBitrateMode();
  }
}
