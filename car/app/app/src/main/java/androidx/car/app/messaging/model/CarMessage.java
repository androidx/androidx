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

import static java.util.Objects.requireNonNull;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.CarText;
import androidx.car.app.annotations.KeepFields;
import androidx.core.app.Person;

/** Represents a single message in a {@link ConversationItem} */
@ExperimentalCarApi
@CarProtocol
@RequiresCarApi(6)
@KeepFields
public class CarMessage {
    @NonNull
    private final Bundle mSender;
    @NonNull
    private final CarText mBody;
    private final long mReceivedTimeEpochMillis;
    private final boolean mIsRead;

    CarMessage(@NonNull Builder builder) {
        this.mSender = requireNonNull(builder.mSender).toBundle();
        this.mBody = requireNonNull(builder.mBody);
        this.mReceivedTimeEpochMillis = builder.mReceivedTimeEpochMillis;
        this.mIsRead = builder.mIsRead;
    }

    /** Default constructor for serialization. */
    private CarMessage() {
        this.mSender = new Person.Builder().setName("").build().toBundle();
        this.mBody = new CarText.Builder("").build();
        this.mReceivedTimeEpochMillis = 0;
        this.mIsRead = false;
    }


    /** Returns a {@link Person} representing the message sender */
    @NonNull
    public Person getSender() {
        return Person.fromBundle(mSender);
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

    /** Returns a {@link boolean}, indicating whether the message has been read */
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

        /** Sets a {@link Person} representing the message sender */
        public @NonNull Builder setSender(@NonNull Person sender) {
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

        /** Sets a {@link boolean}, indicating whether the message has been read */
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
