/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.core.operations;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SerializableToString;
import androidx.compose.remote.core.VariableProvider;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Embeds raw PCM sound data in the document. The data uses a custom lightweight SC format:
 * a small header (magic, version, bitDepth, channels, sampleRate, sampleCount) followed by
 * raw sample bytes. Loaded once during document preparation; referenced by ID in
 * SoundExpression and PlaySound operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SoundData extends Operation implements SerializableToString, Serializable,
        ComponentData, VariableProvider {
    private static final int OP_CODE = Operations.DATA_SOUND;
    private static final String CLASS_NAME = "SoundData";

    /** Maximum size of a sound sample in bytes (256 KB). */
    public static final int MAX_SOUND_DATA_SIZE = 256 * 1024;

    public int mSoundId;
    byte @NonNull [] mData;

    public SoundData(int soundId, byte @NonNull [] data) {
        this.mSoundId = soundId;
        this.mData = data;
    }

    @Override
    public int getId() {
        return mSoundId;
    }

    @Override
    public void setId(int id) {
        mSoundId = id;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mSoundId, mData);
    }

    /**
     * Write a DATA_SOUND operation to the buffer.
     *
     * @param buffer  the wire buffer
     * @param soundId the id under which this sound is stored
     * @param data    raw SC-format audio bytes
     */
    public static void apply(@NonNull WireBuffer buffer, int soundId, byte @NonNull [] data) {
        buffer.start(OP_CODE);
        buffer.writeInt(soundId);
        buffer.writeBuffer(data);
    }

    /**
     * Read a DATA_SOUND operation from the buffer and append it to the operation list.
     *
     * @param buffer     the wire buffer
     * @param operations the list to append to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int soundId = buffer.readId();
        byte[] data = buffer.readBuffer(MAX_SOUND_DATA_SIZE);
        operations.add(new SoundData(soundId, data));
    }

    /** Populate documentation for this operation. */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("Embed inline raw PCM sound data (SC format)")
                .field(DocumentedOperation.INT, "soundId", "The ID of the sound resource")
                .field(DocumentedOperation.BYTE_ARRAY, "data", "SC-format audio bytes");
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        context.loadSound(mSoundId, mData);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + "(" + mSoundId + ", " + mData.length + " bytes)";
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, "SOUND_DATA<" + mSoundId + ">");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME)
                .add("soundId", mSoundId)
                .add("dataLength", mData.length);
    }
}
