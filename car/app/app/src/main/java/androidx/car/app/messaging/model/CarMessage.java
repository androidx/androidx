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

import static java.util.Objects.requireNonNull;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.CarText;
import androidx.core.app.Person;

import java.util.Objects;

/** Represents a single message in a {@link ConversationItem} */
@ExperimentalCarApi
@CarProtocol
@RequiresCarApi(7)
@KeepFields
public class CarMessage {
    @Nullable
    private final Bundle mSender;
    @NonNull
    private final CarText mBody;
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
        this.mBody = requireNonNull(builder.mBody);
        this.mReceivedTimeEpochMillis = builder.mReceivedTimeEpochMillis;
        this.mIsRead = builder.mIsRead;
    }

    /** Default constructor for serialization. */
    private CarMessage() {
        this.mSender = null;
        this.mBody = new CarText.Builder("").build();
        this.mReceivedTimeEpochMillis = 0;
        this.mIsRead = false;
    }


    /**
     * Returns a {@link Person} representing the message sender.
     *
     * <p> For self-sent messages, this method will return {@code null} or
     * {@link ConversationItem#getSelf()}.
     */
    @Nullable
    public Person getSender() {
        return mSender == null ? null : Person.fromBundle(mSender);
    }

    /** Returns a {@link CarText} representing the message body */
    @NonNull
    public CarText getBody() {
        return mBody;
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
        @Nullable
        Person mSender;
        @Nullable
        CarText mBody;
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

        /** Sets a {@link CarText} representing the message body */
        public @NonNull Builder setBody(@NonNull CarText body) {
            mBody = body;
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
            return new CarMessage(this);
        }
    }
}
