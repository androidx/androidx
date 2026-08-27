/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.car.app.media.model;

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.StrokeCap;
import androidx.car.app.model.constraints.CarColorConstraints;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Defines the visual style for a {@link MediaPlaybackTemplate}.
 *
 * <p>This style can be used to customize media playback interactive controls and progress
 * indicators, such as their accent color and stroke cap shape.
 */
@CarProtocol
@KeepFields
@RequiresCarApi(9)
@ExperimentalCarApi
public final class MediaPlaybackStyle {
    private final @Nullable CarColor mMediaAccentColor;
    @StrokeCap.StrokeCapType
    private final int mProgressBarStrokeCap;

    /**
     * Returns the custom media accent {@link CarColor} for interactive controls, or {@code null}
     * if none was set.
     */
    public @Nullable CarColor getMediaAccentColor() {
        return mMediaAccentColor;
    }

    /**
     * Returns the {@link StrokeCap} shape for the playback progress bar, or
     * {@link StrokeCap#DEFAULT} if not set.
     */
    @StrokeCap.StrokeCapType
    public int getProgressBarStrokeCap() {
        return mProgressBarStrokeCap;
    }

    @Override
    public @NonNull String toString() {
        return "MediaPlaybackStyle{"
                + "mediaAccentColor="
                + mMediaAccentColor
                + ", progressBarStrokeCap="
                + mProgressBarStrokeCap
                + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mMediaAccentColor, mProgressBarStrokeCap);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MediaPlaybackStyle)) {
            return false;
        }
        MediaPlaybackStyle otherStyle = (MediaPlaybackStyle) other;
        return Objects.equals(mMediaAccentColor, otherStyle.mMediaAccentColor)
                && mProgressBarStrokeCap == otherStyle.mProgressBarStrokeCap;
    }

    private MediaPlaybackStyle(Builder builder) {
        mMediaAccentColor = builder.mMediaAccentColor;
        mProgressBarStrokeCap = builder.mProgressBarStrokeCap;
    }

    /** Constructs an empty instance, used by serialization code. */
    private MediaPlaybackStyle() {
        mMediaAccentColor = null;
        mProgressBarStrokeCap = StrokeCap.DEFAULT;
    }

    /** A builder of {@link MediaPlaybackStyle}. */
    public static final class Builder {
        @Nullable CarColor mMediaAccentColor;
        @StrokeCap.StrokeCapType
        int mProgressBarStrokeCap = StrokeCap.DEFAULT;

        /**
         * Sets the custom accent {@link CarColor} for media interactive controls (such as the
         * play/pause button container and seekbar track) or {@code null} to use default host
         * styling.
         *
         * <p>Defaults to {@code null}, which means default host styling or album art palette
         * color extraction is used.
         */
        public @NonNull Builder setMediaAccentColor(@Nullable CarColor mediaAccentColor) {
            if (mediaAccentColor != null) {
                CarColorConstraints.UNCONSTRAINED.validateOrThrow(mediaAccentColor);
            }
            this.mMediaAccentColor = mediaAccentColor;
            return this;
        }

        /**
         * Sets the {@link StrokeCap} shape for the playback progress bar displayed in the media
         * template.
         *
         * <p>The playback progress bar reflects the playback state and progress of the active
         * {@link android.media.session.MediaSession} /
         * {@link android.support.v4.media.session.MediaSessionCompat}.
         *
         * <p>Defaults to {@link StrokeCap#DEFAULT}, which means the host will use default system
         * shape styling.
         */
        public @NonNull Builder setProgressBarStrokeCap(
                @StrokeCap.StrokeCapType int progressBarStrokeCap) {
            this.mProgressBarStrokeCap = progressBarStrokeCap;
            return this;
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }

        /** Creates a new {@link Builder}, populated from the input {@link MediaPlaybackStyle}. */
        public Builder(@NonNull MediaPlaybackStyle style) {
            requireNonNull(style);
            mMediaAccentColor = style.mMediaAccentColor;
            mProgressBarStrokeCap = style.mProgressBarStrokeCap;
        }

        /** Constructs the {@link MediaPlaybackStyle} defined by this builder. */
        public @NonNull MediaPlaybackStyle build() {
            return new MediaPlaybackStyle(this);
        }
    }
}
