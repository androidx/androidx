/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * MediaSpec communicates the encoding type and encoder-specific options for both the
 * video and audio inputs to the VideoOutput.
 */
@AutoValue
public abstract class MediaSpec {

    /** MPEG4 media file format, it should the default format in {@link VideoCapture} */
    public static final int VIDEO_FORMAT_MPEG_4 = 0;
    /** VP8, VP9 media file format */
    public static final int VIDEO_FORMAT_WEBM = 1;

    // Doesn't allow inheritance outside of package
    MediaSpec(){
    }

    /**
     * Returns a VideoSpec instance.
     */
    @NonNull
    public abstract VideoSpec getVideoSpec();

    /**
     * Returns an AudioSpec instance.
     */
    @Nullable
    public abstract AudioSpec getAudioSpec();

    /** @hide */
    @IntDef({VIDEO_FORMAT_MPEG_4, VIDEO_FORMAT_WEBM})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface VideoFormat {
    }

    /**
     * Returns the output file format.
     *
     * @return the file format
     */
    @VideoFormat
    public abstract int getOutputFormat();

    /** Creates a {@link Builder}. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_MediaSpec.Builder();
    }

    /**
     * The builder for {@link MediaSpec}.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        Builder() {
        }

        /**
         * Sets audio related configuration.
         */
        @NonNull
        public abstract Builder setAudioSpec(@NonNull AudioSpec audioSpec);

        /**
         * Sets video related configuration.
         */
        @NonNull
        public abstract Builder setVideoSpec(@NonNull VideoSpec videoSpec);

        /**
         * Sets the video recording output format.
         *
         * @param videoFormat The requested video format. Possible values are
         * {@link MediaSpec#VIDEO_FORMAT_MPEG_4} or {@link MediaSpec#VIDEO_FORMAT_WEBM}.
         */
        @NonNull
        public abstract Builder setOutputFormat(@VideoFormat int videoFormat);

        /** Build the {@link MediaSpec} from this builder. */
        @NonNull
        public abstract MediaSpec build();
    }
}
