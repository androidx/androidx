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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.versionedparcelable.VersionedParcelable;

interface AudioAttributesImpl extends VersionedParcelable {
    Object getAudioAttributes();
    int getVolumeControlStream();
    int getLegacyStreamType();
    // Returns explicitly set legacy stream type.
    int getRawLegacyStreamType();
    int getContentType();
    @AudioAttributesCompat.AttributeUsage int getUsage();
    int getFlags();
    @NonNull Bundle toBundle();

    interface Builder {
        AudioAttributesImpl build();
        Builder setUsage(@AudioAttributesCompat.AttributeUsage int usage);
        Builder setContentType(@AudioAttributesCompat.AttributeContentType int contentType);
        Builder setFlags(int flags);
        Builder setLegacyStreamType(int streamType);
    }
}
