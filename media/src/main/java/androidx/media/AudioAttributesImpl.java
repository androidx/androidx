/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.VersionedParcelable;

/** @hide */
// It should be public to allow Parcelizers which never be de/jetified can access the interface.
@RestrictTo(LIBRARY)
public interface AudioAttributesImpl extends VersionedParcelable {
    /** Gets framework {@link android.media.AudioAttributes}. */
    @Nullable
    Object getAudioAttributes();
    int getVolumeControlStream();
    int getLegacyStreamType();
    // Returns explicitly set legacy stream type.
    int getRawLegacyStreamType();
    int getContentType();
    @AudioAttributesCompat.AttributeUsage int getUsage();
    int getFlags();

    interface Builder {
        @NonNull
        AudioAttributesImpl build();
        @NonNull
        Builder setUsage(@AudioAttributesCompat.AttributeUsage int usage);
        @NonNull
        Builder setContentType(@AudioAttributesCompat.AttributeContentType int contentType);
        @NonNull
        Builder setFlags(int flags);
        @NonNull
        Builder setLegacyStreamType(int streamType);
    }
}
