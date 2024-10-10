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
 */
@ExperimentalCarApi
@RequiresCarApi(8)
@CarProtocol
@KeepFields
public class MediaPlaybackTemplate implements Template {
    private final @Nullable Header mHeader;

    /**
     * Returns the {@link Header} to display in this template or not to display one if it is {@code
     * null}.
     */
    public @Nullable Header getHeader() {
        return mHeader;
    }

    @Override
    public @NonNull String toString() {
        return "MediaPlaybackTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mHeader);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MediaPlaybackTemplate)) {
            return false;
        }
        MediaPlaybackTemplate otherTemplate = (MediaPlaybackTemplate) other;

        return Objects.equals(mHeader, otherTemplate.mHeader);
    }

    /** Constructs an empty instance, used by serialization code. */
    private MediaPlaybackTemplate() {
        mHeader = null;
    }

    MediaPlaybackTemplate(Builder builder) {
        mHeader = builder.mHeader;
    }

    /** Builder for the {@link MediaPlaybackTemplate} */
    @ExperimentalCarApi
    public static final class Builder {
        @Nullable Header mHeader;

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

        /** Constructs the template defined by this builder. */
        public @NonNull MediaPlaybackTemplate build() {
            return new MediaPlaybackTemplate(this);
        }

        /** Creates a default {@link Builder}. */
        public Builder() {};

        /** Creates a new {@link Builder}, populated from the input {@link MediaPlaybackTemplate} */
        public Builder(@NonNull MediaPlaybackTemplate template) {
            mHeader = template.getHeader();
        }
    }
}
