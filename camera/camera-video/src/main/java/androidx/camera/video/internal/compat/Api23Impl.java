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

package androidx.camera.video.internal.compat;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

/**
 * Helper class to avoid verification errors for methods introduced in Android 6.0 (API 23).
 */
@RequiresApi(23)
public final class Api23Impl {

    private Api23Impl() {
    }

    /** Creates an {@link AudioRecord.Builder}. */
    @DoNotInline
    @NonNull
    public static AudioRecord.Builder createAudioRecordBuilder() {
        return new AudioRecord.Builder();
    }

    /**
     * Sets the {@linkplain AudioRecord.Builder#setAudioSource(int) audio source} on an
     * {@link AudioRecord.Builder}.
     */
    @DoNotInline
    public static void setAudioSource(@NonNull AudioRecord.Builder audioRecordBuilder,
            int audioSource) {
        audioRecordBuilder.setAudioSource(audioSource);
    }

    /**
     * Sets the {@linkplain AudioRecord.Builder#setAudioFormat(AudioFormat) audio format} on an
     * {@link AudioRecord.Builder}.
     */
    @DoNotInline
    public static void setAudioFormat(@NonNull AudioRecord.Builder audioRecordBuilder,
            @NonNull AudioFormat audioFormat) {
        audioRecordBuilder.setAudioFormat(audioFormat);
    }

    /**
     * Sets the {@linkplain AudioRecord.Builder#setBufferSizeInBytes(int) buffer size} on an
     * {@link AudioRecord.Builder}.
     */
    @DoNotInline
    public static void setBufferSizeInBytes(@NonNull AudioRecord.Builder audioRecordBuilder,
            int bufferSizeInBytes) {
        audioRecordBuilder.setBufferSizeInBytes(bufferSizeInBytes);
    }


    /**
     * Builds an {@link AudioRecord} from an {@link AudioRecord.Builder}.
     */
    @DoNotInline
    @NonNull
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public static AudioRecord build(@NonNull AudioRecord.Builder audioRecordBuilder) {
        return audioRecordBuilder.build();
    }
}
