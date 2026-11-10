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

import androidx.annotation.OptIn;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.media.MediaPlaybackManager;
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
    @RequiresCarApi(9)
    private final @Nullable MediaPlaybackStyle mStyle;
    @ExperimentalCarApi
    @RequiresCarApi(10)
    private final boolean mIsVideoEnabled;
    @ExperimentalCarApi
    @RequiresCarApi(10)
    private final boolean mIsAutoHideControlsEnabled;

    /**
     * Returns the {@link Header} to display in this template or not to display one if it is {@code
     * null}.
     */
    public @Nullable Header getHeader() {
        return mHeader;
    }

    /**
     * Returns the {@link MediaPlaybackStyle} for this template or {@code null} if none was set.
     */
    @RequiresCarApi(9)
    public @Nullable MediaPlaybackStyle getStyle() {
        return mStyle;
    }

    /**
     * Returns whether video playback is enabled for this template.
     *
     * @see Builder#setVideoEnabled(boolean)
     */
    @ExperimentalCarApi
    @RequiresCarApi(10)
    public boolean isVideoEnabled() {
        return mIsVideoEnabled;
    }

    /**
     * Returns whether the playback controls should auto-hide after inactivity when video is
     * playing.
     *
     * <p>This setting only applies while video is playing and has no effect when video is disabled.
     *
     * @see Builder#setAutoHideControlsEnabled(boolean)
     */
    @ExperimentalCarApi
    @RequiresCarApi(10)
    public boolean isAutoHideControlsEnabled() {
        return mIsAutoHideControlsEnabled;
    }

    @Override
    public @NonNull String toString() {
        return "MediaPlaybackTemplate";
    }

    @OptIn(markerClass = ExperimentalCarApi.class)
    @Override
    public int hashCode() {
        return Objects.hash(
                mHeader,
                mStyle,
                mIsVideoEnabled,
                mIsAutoHideControlsEnabled);
    }

    @OptIn(markerClass = ExperimentalCarApi.class)
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
                && Objects.equals(mStyle, otherTemplate.mStyle)
                && mIsVideoEnabled == otherTemplate.mIsVideoEnabled
                && mIsAutoHideControlsEnabled == otherTemplate.mIsAutoHideControlsEnabled;
    }

    /** Constructs an empty instance, used by serialization code. */
    @OptIn(markerClass = ExperimentalCarApi.class)
    private MediaPlaybackTemplate() {
        mHeader = null;
        mStyle = null;
        mIsVideoEnabled = false;
        mIsAutoHideControlsEnabled = false;
    }

    @OptIn(markerClass = ExperimentalCarApi.class)
    MediaPlaybackTemplate(Builder builder) {
        mHeader = builder.mHeader;
        mStyle = builder.mStyle;
        mIsVideoEnabled = builder.mIsVideoEnabled;
        mIsAutoHideControlsEnabled = builder.mIsAutoHideControlsEnabled;
    }

    /** Builder for the {@link MediaPlaybackTemplate} */
    @RequiresCarApi(8)
    public static final class Builder {
        @Nullable Header mHeader;
        @Nullable MediaPlaybackStyle mStyle;
        @ExperimentalCarApi
        private boolean mIsVideoEnabled = false;
        @ExperimentalCarApi
        private boolean mIsAutoHideControlsEnabled = false;

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
         * Sets the {@link MediaPlaybackStyle} for this template or {@code null} to use default
         * host styling.
         *
         * <p>Defaults to {@code null}, which means default host styling is used.
         */
        @RequiresCarApi(9)
        public MediaPlaybackTemplate.@NonNull Builder setStyle(
                @Nullable MediaPlaybackStyle style) {
            this.mStyle = style;
            return this;
        }

        /**
         * Sets whether video playback is enabled for this template.
         *
         * <p>When video playback is enabled, the video is rendered on the non-interactive
         * surface provided by the host via
         * {@link androidx.car.app.AppManager#setSurfaceCallback(androidx.car.app.SurfaceCallback)},
         * which requires the {@code androidx.car.app.ACCESS_SURFACE} permission to be declared.
         *
         * <p>The host will create and show the {@link android.view.Surface} when it's safe to show
         * videos based on UX restrictions, and will destroy the {@link android.view.Surface}
         * when it's not, showing the regular media screen instead.
         *
         * <p>On hosts that do not support video playback, this setting is ignored and standard
         * media artwork is displayed instead.
         *
         * <p>Defaults to {@code false}.
         *
         * @see androidx.car.app.AppManager#setSurfaceCallback(androidx.car.app.SurfaceCallback)
         */
        @ExperimentalCarApi
        @RequiresCarApi(10)
        public MediaPlaybackTemplate.@NonNull Builder setVideoEnabled(boolean isVideoEnabled) {
            mIsVideoEnabled = isVideoEnabled;
            return this;
        }

        /**
         * Sets whether the playback controls should auto-hide after inactivity when video is
         * playing.
         *
         * <p>Defaults to {@code false}.
         *
         * <p>This setting only applies while video is playing and has no effect when video is
         * disabled.
         */
        @ExperimentalCarApi
        @RequiresCarApi(10)
        public MediaPlaybackTemplate.@NonNull Builder setAutoHideControlsEnabled(
                boolean isAutoHideControlsEnabled) {
            mIsAutoHideControlsEnabled = isAutoHideControlsEnabled;
            return this;
        }

        /** Constructs the template defined by this builder. */
        public @NonNull MediaPlaybackTemplate build() {
            return new MediaPlaybackTemplate(this);
        }

        /** Creates a default {@link Builder}. */
        public Builder() {};

        /** Creates a new {@link Builder}, populated from the input {@link MediaPlaybackTemplate} */
        @OptIn(markerClass = ExperimentalCarApi.class)
        public Builder(@NonNull MediaPlaybackTemplate template) {
            mHeader = template.getHeader();
            mStyle = template.getStyle();
            mIsVideoEnabled = template.isVideoEnabled();
            mIsAutoHideControlsEnabled = template.isAutoHideControlsEnabled();
        }
    }
}
