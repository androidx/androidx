/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.media.MediaPlaybackManager;
import androidx.car.app.model.Banner;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.Header;
import androidx.car.app.model.Template;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A template representing content to display for media playback.
 *
 * <h4>Template Requirement</h4>
 *
 * A pre requisite for using this template is the usage of {@link
 * MediaPlaybackManager#registerMediaPlaybackToken}.
 *
 * <p><b>Note:</b> Starting in Car API 9, all media apps will render with a persistent
 * entrypoint to the full screen {@link MediaPlaybackTemplate}. The CAL Host may render that
 * entrypoint as an action (on smaller screens) to a mini-controller bar (on larger screens).
 * When a user clicks on this, the host will send a callback with the
 * {@link androidx.car.app.media.MediaConstants#ACTION_SHOW_MEDIA_PLAYBACK} intent. All 3P Media
 * Apps MUST handle this intent callback.
 */
@RequiresCarApi(8)
@CarProtocol
@KeepFields
public class MediaPlaybackTemplate implements Template {
    private final @Nullable Header mHeader;
    @ExperimentalCarApi
    @RequiresCarApi(9)
    private final @Nullable Banner mBanner;
    @ExperimentalCarApi
    @RequiresCarApi(9)
    private final @Nullable CarColor mMediaAccentColor;

    /**
     * Returns the {@link Header} to display in this template or not to display one if it is {@code
     * null}.
     */
    public @Nullable Header getHeader() {
        return mHeader;
    }

    /**
     * Returns the {@link Banner} to display in this template or not to display one if it is {@code
     * null}.
     */
    @ExperimentalCarApi
    @RequiresCarApi(9)
    public @Nullable Banner getBanner() {
        return mBanner;
    }

    /**
     * Returns the custom media accent {@link CarColor} for this template or {@code null} if none
     * was set.
     */
    @ExperimentalCarApi
    @RequiresCarApi(9)
    public @Nullable CarColor getMediaAccentColor() {
        return mMediaAccentColor;
    }

    @Override
    public @NonNull String toString() {
        return "MediaPlaybackTemplate";
    }

    @androidx.annotation.OptIn(markerClass = androidx.car.app.annotations.ExperimentalCarApi.class)
    @Override
    public int hashCode() {
        return Objects.hash(mHeader, mBanner, mMediaAccentColor);
    }

    @androidx.annotation.OptIn(markerClass = androidx.car.app.annotations.ExperimentalCarApi.class)
    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MediaPlaybackTemplate)) {
            return false;
        }
        MediaPlaybackTemplate otherTemplate = (MediaPlaybackTemplate) other;

        return Objects.equals(mHeader, otherTemplate.mHeader)
                && Objects.equals(mBanner, otherTemplate.mBanner)
                && Objects.equals(mMediaAccentColor, otherTemplate.mMediaAccentColor);
    }

    /** Constructs an empty instance, used by serialization code. */
    @androidx.annotation.OptIn(markerClass = androidx.car.app.annotations.ExperimentalCarApi.class)
    private MediaPlaybackTemplate() {
        mHeader = null;
        mBanner = null;
        mMediaAccentColor = null;
    }

    @androidx.annotation.OptIn(markerClass = androidx.car.app.annotations.ExperimentalCarApi.class)
    MediaPlaybackTemplate(Builder builder) {
        mHeader = builder.mHeader;
        mBanner = builder.mBanner;
        mMediaAccentColor = builder.mMediaAccentColor;
    }

    /** Builder for the {@link MediaPlaybackTemplate} */
    @RequiresCarApi(8)
    public static final class Builder {
        @Nullable Header mHeader;
        @ExperimentalCarApi
        @Nullable Banner mBanner;
        @ExperimentalCarApi
        @Nullable CarColor mMediaAccentColor;

        /**
         * Sets the {@link Header} for this template or {code null} to not display a {@link
         * Header}.
         *
         * <p>Defaults to {@code null}, which means header is not displayed.
         */
        public MediaPlaybackTemplate.@NonNull Builder setHeader(@Nullable Header header) {
            this.mHeader = header;
            return this;
        }

        /**
         * Sets the {@link Banner} for this template or {code null} to not display a {@link
         * Banner}.
         *
         * <p>Defaults to {@code null}, which means banner is not displayed.
         */
        @ExperimentalCarApi
        @RequiresCarApi(9)
        public MediaPlaybackTemplate.@NonNull Builder setBanner(@Nullable Banner banner) {
            this.mBanner = banner;
            return this;
        }

        /**
         * Sets the custom accent {@link CarColor} for media interactive controls in this template
         * or {@code null} to use default host styling.
         *
         * <p>Defaults to {@code null}, which means default host styling is used.
         */
        @ExperimentalCarApi
        @RequiresCarApi(9)
        public MediaPlaybackTemplate.@NonNull Builder setMediaAccentColor(
                @Nullable CarColor mediaAccentColor) {
            this.mMediaAccentColor = mediaAccentColor;
            return this;
        }

        /** Constructs the template defined by this builder. */
        public @NonNull MediaPlaybackTemplate build() {
            return new MediaPlaybackTemplate(this);
        }

        /** Creates a default {@link Builder}. */
        public Builder() {};

        /** Creates a new {@link Builder}, populated from the input {@link MediaPlaybackTemplate} */
        @androidx.annotation.OptIn(markerClass =
                androidx.car.app.annotations.ExperimentalCarApi.class)
        public Builder(@NonNull MediaPlaybackTemplate template) {
            mHeader = template.getHeader();
            mBanner = template.getBanner();
            mMediaAccentColor = template.getMediaAccentColor();
        }
    }
}
