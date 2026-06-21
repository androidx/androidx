/*
 * Copyright (C) 2026 The Android Open Source Project
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
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Triggers playback of a previously defined {@link SoundExpression} or {@link SoundData}
 * resource. Usable in any scope where {@link HapticFeedback} is usable (action handlers,
 * onClick, onTouch, etc.).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PlaySound extends Operation implements SerializableToString, Serializable {
    private static final int OP_CODE = Operations.PLAY_SOUND;
    private static final String CLASS_NAME = "PlaySound";

    private int mSoundExpressionId;

    public PlaySound(int soundExpressionId) {
        this.mSoundExpressionId = soundExpressionId;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mSoundExpressionId);
    }

    /**
     * Write a PLAY_SOUND operation to the buffer.
     *
     * @param buffer            the wire buffer
     * @param soundExpressionId the id of the SoundExpression to play
     */
    public static void apply(@NonNull WireBuffer buffer, int soundExpressionId) {
        buffer.start(OP_CODE);
        buffer.writeInt(soundExpressionId);
    }

    /**
     * Read a PLAY_SOUND operation from the buffer and append it to the list.
     *
     * @param buffer     the wire buffer
     * @param operations the list to append to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int soundExpressionId = buffer.readId();
        operations.add(new PlaySound(soundExpressionId));
    }

    /** Populate documentation for this operation. */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Miscellaneous Operations", OP_CODE, CLASS_NAME)
                .description("Trigger playback of a sound expression")
                .field(DocumentedOperation.INT, "soundExpressionId",
                        "ID of the SoundExpression to play");
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        context.playSound(mSoundExpressionId);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + "(" + mSoundExpressionId + ")";
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, "PLAY_SOUND<" + mSoundExpressionId + ">");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("soundExpressionId", mSoundExpressionId);
    }
}
