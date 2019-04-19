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

package androidx.media2.player.exoplayer;


import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;

import androidx.annotation.RestrictTo;
import androidx.media2.exoplayer.external.Format;
import androidx.media2.exoplayer.external.metadata.Metadata;
import androidx.media2.exoplayer.external.metadata.MetadataDecoder;
import androidx.media2.exoplayer.external.metadata.MetadataDecoderFactory;
import androidx.media2.exoplayer.external.metadata.MetadataInputBuffer;
import androidx.media2.exoplayer.external.util.MimeTypes;

import java.util.Arrays;

/**
 * Factory for metadata decoders that provide raw ID3 data in {@link ByteArrayFrame}s.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ final class Id3MetadataDecoderFactory implements MetadataDecoderFactory {

    @Override
    public boolean supportsFormat(Format format) {
        return MimeTypes.APPLICATION_ID3.equals(format.sampleMimeType);
    }

    @Override
    public MetadataDecoder createDecoder(Format format) {
        return new MetadataDecoder() {
            @Override
            public Metadata decode(MetadataInputBuffer inputBuffer) {
                long timestamp = inputBuffer.timeUs;
                byte[] bufferData = inputBuffer.data.array();
                Metadata.Entry entry =
                        new ByteArrayFrame(timestamp, Arrays.copyOf(bufferData, bufferData.length));
                return new Metadata(entry);
            }
        };
    }

}
