/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.messaging.model;

import static androidx.car.app.messaging.model.ConversationItem.validateSender;

import android.net.Uri;
import android.os.Bundle;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.CarText;
import androidx.core.app.Person;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Represents a single message in a {@link ConversationItem} */
@CarProtocol
@RequiresCarApi(7)
@KeepFields
public class CarMessage {
    private final @Nullable Bundle mSender;
    private final @Nullable CarText mBody;
    private final @Nullable String mMultimediaMimeType;
    private final @Nullable Uri mMultimediaUri;
    private final long mReceivedTimeEpochMillis;
    private final boolean mIsRead;

    @Override
    public int hashCode() {
        return Objects.hash(
                PersonsEqualityHelper.getPersonHashCode(getSender()),
                mBody,
                mReceivedTimeEpochMillis,
                mIsRead
        );
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CarMessage)) {
            return false;
        }
        CarMessage otherCarMessage = (CarMessage) other;

        return
                PersonsEqualityHelper.arePersonsEqual(getSender(), otherCarMessage.getSender())
                        && Objects.equals(mBody, otherCarMessage.mBody)
                        && mReceivedTimeEpochMillis == otherCarMessage.mReceivedTimeEpochMillis
                        && mIsRead == otherCarMessage.mIsRead;
    }

    CarMessage(@NonNull Builder builder) {
        this.mSender = builder.mSender == null ? null : validateSender(builder.mSender).toBundle();
        this.mBody = builder.mBody;
        this.mMultimediaMimeType = builder.mMultimediaMimeType;
        this.mMultimediaUri = builder.mMultimediaUri;
        this.mReceivedTimeEpochMillis = builder.mReceivedTimeEpochMillis;
        this.mIsRead = builder.mIsRead;
    }

    /** Default constructor for serialization. */
    private CarMessage() {
        this.mSender = null;
        this.mBody = null;
        this.mMultimediaMimeType = null;
        this.mMultimediaUri = null;
        this.mReceivedTimeEpochMillis = 0;
        this.mIsRead = false;
    }


    /**
     * Returns a {@link Person} representing the message sender.
     *
     * <p> For self-sent messages, this method will return {@code null} or
     * {@link ConversationItem#getSelf()}.
     */
    public @Nullable Person getSender() {
        return mSender == null ? null : Person.fromBundle(mSender);
    }

    /**
     * Returns a {@link CarText} representing the message body
     *
     * <p> Messages must have one or both of the following:
     * <ul>
     *     <li> A message body (text)
     *     <li> A MIME type + URI (image, audio, etc.)
     * </ul>
     *
     * @see #getMultimediaMimeType()
     * @see #getMultimediaUri()
     */
    public @Nullable CarText getBody() {
        return mBody;
    }

    /**
     * Returns a {@link String} representing the MIME type of a multimedia message
     *
     * <p> Messages must have one or both of the following:
     * <ul>
     *     <li> A message body (text)
     *     <li> A MIME type + URI (image, audio, etc.)
     * </ul>
     *
     * @see #getBody()
     * @see #getMultimediaUri()
     */
    public @Nullable String getMultimediaMimeType() {
        return mMultimediaMimeType;
    }

    /**
     * Returns a {@link Uri} pointing to the contents of a multimedia message.
     *
     * <p> Messages must have one or both of the following:
     * <ul>
     *     <li> A message body (text)
     *     <li> A MIME type + URI (image, audio, etc.)
     * </ul>
     *
     * @see #getBody()
     * @see #getMultimediaMimeType()
     */
    public @Nullable Uri getMultimediaUri() {
        return mMultimediaUri;
    }

    /** Returns a {@code long} representing the message timestamp (in epoch millis) */
    public long getReceivedTimeEpochMillis() {
        return mReceivedTimeEpochMillis;
    }

    /** Returns a {@code boolean}, indicating whether the message has been read */
    public boolean isRead() {
        return mIsRead;
    }

    /** A builder for {@link CarMessage} */
    public static final class Builder {
        @Nullable Person mSender;
        @Nullable CarText mBody;
        @Nullable String mMultimediaMimeType;
        @Nullable Uri mMultimediaUri;
        long mReceivedTimeEpochMillis;
        boolean mIsRead;

        /**
         * Sets a {@link Person} representing the message sender
         *
         * <p> The {@link Person} must specify a non-null
         * {@link Person.Builder#setName(CharSequence)} and
         * {@link Person.Builder#setKey(String)}.
         */
        public @NonNull Builder setSender(@Nullable Person sender) {
            mSender = sender;
            return this;
        }

        /**
         * Sets a {@link CarText} representing the message body
         *
         * <p> Messages must have one or both of the following:
         * <ul>
         *     <li> A message body (text)
         *     <li> A MIME type + URI (image, audio, etc.)
         * </ul>
         *
         * @see #setMultimediaMimeType(String)
         * @see #setMultimediaUri(Uri)
         */
        public @NonNull Builder setBody(@Nullable CarText body) {
            mBody = body;
            return this;
        }

        /**
         * Sets a {@link String} representing the MIME type of a multimedia message
         *
         * <p> Messages must have one or both of the following:
         * <ul>
         *     <li> A message body (text)
         *     <li> A MIME type + URI (image, audio, etc.)
         * </ul>
         *
         * @see #setBody(CarText)
         * @see #setMultimediaUri(Uri)
         */
        public @NonNull Builder setMultimediaMimeType(@Nullable String multimediaMimeType) {
            this.mMultimediaMimeType = multimediaMimeType;
            return this;
        }

        /**
         * Sets a {@link Uri} pointing to the contents of a multimedia message.
         *
         * <p> Messages must have one or both of the following:
         * <ul>
         *     <li> A message body (text)
         *     <li> A MIME type + URI (image, audio, etc.)
         * </ul>
         *
         * @see #setBody(CarText)
         * @see #setMultimediaMimeType(String)
         */
        public @NonNull Builder setMultimediaUri(@Nullable Uri multimediaUri) {
            this.mMultimediaUri = multimediaUri;
            return this;
        }

        /** Sets a {@code long} representing the message timestamp (in epoch millis) */
        public @NonNull Builder setReceivedTimeEpochMillis(long receivedTimeEpochMillis) {
            mReceivedTimeEpochMillis = receivedTimeEpochMillis;
            return this;
        }

        /** Sets a {@code boolean}, indicating whether the message has been read */
        public @NonNull Builder setRead(boolean isRead) {
            mIsRead = isRead;
            return this;
        }

        /** Returns a new {@link CarMessage} instance defined by this builder */
        public @NonNull CarMessage build() {
            if (mMultimediaMimeType == null ^ mMultimediaUri == null) {
                throw new IllegalStateException("Incomplete multimedia data detected in "
                        + "CarMessage. Please be sure to provide both MIME type and URI for "
                        + "multimedia messages.");
            }

            // Conceptually, we're checking that body text and multimedia data (mime type or URI)
            // are null.
            // The compiler complains if I check both mime type and URI, due to previous validation.
            if (mBody == null && mMultimediaMimeType == null) {
                throw new IllegalStateException("Message must have content. Please provide body "
                        + "text, multimedia data (URI + MIME type), or both.");
            }

            return new CarMessage(this);
        }
    }
}
