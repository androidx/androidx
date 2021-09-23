/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.view.video;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

/** Holder class for metadata that should be saved alongside captured video. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@ExperimentalVideo
@AutoValue
public abstract class Metadata {
    /**
     * Returns a {@link Location} object representing the geographic location where the video was
     * taken.
     *
     * @return The location object or {@code null} if no location was set.
     */
    @Nullable
    public abstract Location getLocation();

    /** Creates a {@link Builder}. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_Metadata.Builder();
    }

    // Don't allow inheritance outside of package
    Metadata() {
    }

    /** The builder for {@link Metadata}. */
    @SuppressWarnings("StaticFinalBuilder")
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Sets a {@link Location} object representing a geographic location where the video was
         * taken.
         *
         * <p>If {@code null}, no location information will be saved with the video. Default
         * value is {@code null}.
         */
        @NonNull
        public abstract Builder setLocation(@Nullable Location location);

        /** Build the {@link Metadata} from this builder. */
        @NonNull
        public abstract Metadata build();

        Builder() {
        }
    }
}
