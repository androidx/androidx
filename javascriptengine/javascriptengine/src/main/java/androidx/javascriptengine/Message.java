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
import java.util.Base64;
import java.util.Objects;

/**
 * Represents a message that can be sent or received between two message ports.
 */
public final class Message {
    /**
     * The data type of the message.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        TYPE_STRING,
        TYPE_ARRAY_BUFFER,
    })
    public @interface Type {}

    /**
     * The message is a {@link String}.
     */
    public static final int TYPE_STRING = 0;
    /**
     * The message is a {@code byte[]} representing a JavaScript ArrayBuffer.
     */
    public static final int TYPE_ARRAY_BUFFER = 1;

    private final @Type int mType;
    @Nullable
    private final String mString;
    private final byte @Nullable [] mArrayBuffer;

    /**
     * Creates a new message with a {@link String} payload.
     * <p>
     * String messages must contain valid Unicode. Invalid Unicode, such as unpaired surrogates may
     * result in message corruption. To send binary data, use
     * {@link #createArrayBufferMessage(byte[])}.
     *
     * @param string the string payload.
     * @return a new Message instance with the string payload.
     */
    @NonNull
    public static Message createStringMessage(@NonNull String string) {
        return new Message(string);
    }

    /**
     * Creates a new message with a JavaScript ArrayBuffer ({@code byte[]}) payload.
     * <p>
     * This method does not create a copy of the byte array; the message object will only hold a
     * reference to the original byte array. Data is only copied during message posting. As such,
     * you should generally avoid modifying the original supplied array after creating the message.
     *
     * @param bytes the contents of the ArrayBuffer.
     * @return a new Message instance with the ArrayBuffer payload.
     */
    @NonNull
    public static Message createArrayBufferMessage(byte @NonNull [] bytes) {
        return new Message(bytes);
    }

    // We generally don't want to provide public constructors as there isn't a clean 1:1 mapping
    // between Java and JavaScript types. For example, Java has ArrayBuffer, Uint8Array, and
    // Int8Array, all of which could correspond to a Java byte[].
    //
    // Therefore, we instead expose factory methods to ensure that we can provide support for such
    // data types in the future.
    private Message(@NonNull String string) {
        Objects.requireNonNull(string);
        mString = string;
        mArrayBuffer = null;
        mType = TYPE_STRING;
    }

    private Message(byte @NonNull [] bytes) {
        Objects.requireNonNull(bytes);
        mString = null;
        mArrayBuffer = bytes;
        mType = TYPE_ARRAY_BUFFER;
    }

    /**
     * Returns the type of the data stored in the message.
     * <p>
     * Note on Forward Compatibility:
     * <p>
     * Support for new types may be added over time. An application should generally be prepared to
     * accept messages of previously undefined types, particularly when it processes external
     * scripts that may be updated separately from the application. This may necessitate else blocks
     * or default case labels to handle unknown types, or simply ignoring messages of unmatched
     * types if appropriate.
     * <p>
     * As such, you should avoid assuming the data type unless you have positively confirmed it
     * specifically, else you may encounter a MessageTypeMismatchException due to using an
     * inappropriate getter.
     *
     * @return the data type of the message.
     */
    public @Type int getType() {
        return mType;
    }

    /**
     * Returns the encapsulated String data.
     * <p>
     * No implicit conversions are performed. If the data type of the message is not a String,
     * a {@link MessageTypeMismatchException} will be thrown. You should only call this method if
     * {@link #getType()} returns {@link #TYPE_STRING}.
     *
     * @return the encapsulated String data.
     * @throws MessageTypeMismatchException if the message type is not a string.
     */
    @NonNull
    public String getString() {
        if (mType != TYPE_STRING) {
            throw new MessageTypeMismatchException("Message type is not a string");
        }
        return Objects.requireNonNull(mString);
    }

    /**
     * Returns the encapsulated JavaScript ArrayBuffer data as a <code>byte[]</code>.
     * <p>
     * No implicit conversions are performed. If the data type of the message is not an ArrayBuffer,
     * a {@link MessageTypeMismatchException} will be thrown. You should only call this method if
     * {@link #getType()} returns {@link #TYPE_ARRAY_BUFFER}.
     * <p>
     * This method only obtains a reference to the backing array and does not create a copy.
     * Modifications to the returned byte array will modify the content of this message instance.
     *
     * @return the encapsulated ArrayBuffer data as a <code>byte[]</code>.
     * @throws MessageTypeMismatchException if the message type is not an ArrayBuffer.
     */
    public byte @NonNull [] getArrayBuffer() {
        if (mType != TYPE_ARRAY_BUFFER) {
            throw new MessageTypeMismatchException("Message type is not an array buffer");
        }
        return Objects.requireNonNull(mArrayBuffer);
    }

    /**
     * Produce a string representation of the message for debugging purposes.
     * <p>
     * The output of toString may not be stable across API versions. Do not attempt to parse its
     * output or rely on any property about it in main application logic.
     *
     * @return a string representation of the message.
     */
    @Override
    @NonNull
    public String toString() {
        switch (mType) {
            case TYPE_STRING:
                return super.toString() + "(string=" + mString + ")";
            case TYPE_ARRAY_BUFFER:
                return super.toString() + "(arrayBuffer="
                        + Base64.getEncoder().encodeToString(mArrayBuffer) + ")";
            default:
                // This case should be unreachable if all Message.Type enum values are handled.
                // Reaching here indicates a bug in the JavaScriptEngine library.
                throw new UnsupportedOperationException(
                        "Unsupported message type: " + getType());
        }
    }
}
