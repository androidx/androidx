/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.javascriptengine;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Represents a message sent between two message ports.
 * <p>
 * The type of the message can be checked using {@link #getType()}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Message {
    /**
     * The type of the message.
     */
    @IntDef({Message.Type.STRING, Message.Type.ARRAY_BUFFER})
    @Retention(RetentionPolicy.SOURCE)
    @interface Type {
        /**
         * The message is a {@link String}.
         */
        int STRING = 0;
        /**
         * The message is a {@code byte[]} (representing an ArrayBuffer).
         */
        int ARRAY_BUFFER = 1;
    }

    @Type private final int mType;
    @Nullable
    private final String mString;
    private final byte @Nullable [] mArrayBuffer;

    /**
     * Creates a new message with a {@link String} payload.
     *
     * @param string the string payload.
     * @return a new Message instance with the string payload.
     */
    @NonNull
    public static Message createString(@NonNull String string) {
        return new Message(string);
    }

    /**
     * Creates a new message with a {@code byte[]} payload.
     * <p>
     * This method does not create a copy of the byte array; the message object will only hold a
     * reference to the original byte array. Data is only copied during message posting.
     *
     * @param bytes the byte array payload.
     * @return a new Message instance with the byte array payload.
     */
    @NonNull
    public static Message createArrayBuffer(byte @NonNull [] bytes) {
        return new Message(bytes);
    }

    private Message(@NonNull String string) {
        Objects.requireNonNull(string);
        mString = string;
        mArrayBuffer = null;
        mType = Type.STRING;
    }

    private Message(byte @NonNull [] bytes) {
        Objects.requireNonNull(bytes);
        mString = null;
        mArrayBuffer = bytes;
        mType = Type.ARRAY_BUFFER;
    }

    /**
     * Returns the type of the data stored in the message.
     *
     * Note on Forward Compatibility:
     * <p>
     * Support for new types may be added over time. An application should generally
     * be prepared to handle messages of previously undefined types, particularly when
     * it processes external scripts that may be updated separately from the application.
     * <p>
     * A new message type may be introduced in future versions of the JavaScript engine.
     * If a generic else block is used, code like message.getArrayBuffer() could be called
     * on new, unsupported type, leading to a MessageTypeMismatchException at runtime.
     *
     * @return the type of the message.
     */
    public @Type int getType() {
        return mType;
    }

    /**
     * Returns the message's data as a String.
     * <p>
     * Does not perform any implicit conversions.
     * If the internal representation of the message is not a String, a
     * {@link MessageTypeMismatchException} will be thrown.
     *
     * @return the message's data as a String.
     * @throws MessageTypeMismatchException if the message type is not a string.
     */
    @NonNull
    public String getString() {
        if (mType != Type.STRING) {
            throw new MessageTypeMismatchException("Message type is not a string");
        }
        return Objects.requireNonNull(mString);
    }

    /**
     * Returns the message's data as a byte array.
     * <p>
     * Does not perform any implicit conversions.
     * If the internal representation of the message is not a byte array, a
     * {@link MessageTypeMismatchException} will be thrown.
     * <p>
     * This method only obtains a reference to the backing array and does not create a copy.
     * Modifications to the returned byte array will modify the content of this message instance.
     *
     * @return the message's data as a byte array.
     * @throws MessageTypeMismatchException if the message type is not an array buffer.
     */
    public byte @NonNull [] getArrayBuffer() {
        if (mType != Type.ARRAY_BUFFER) {
            throw new MessageTypeMismatchException("Message type is not an array buffer");
        }
        return Objects.requireNonNull(mArrayBuffer);
    }
}
