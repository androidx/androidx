/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.values;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;
import androidx.appactions.interaction.capabilities.core.values.properties.Recipient;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Represents an message object. */
@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
public abstract class Message extends Thing {

    /** Create a new Message.Builder instance. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_Message.Builder();
    }

    /** Returns the recipients of the message. */
    @NonNull
    public abstract List<Recipient> getRecipientList();

    /** Returns the message text. */
    @NonNull
    public abstract Optional<String> getMessageText();

    /** Builder class for building an Message. */
    @AutoValue.Builder
    public abstract static class Builder extends Thing.Builder<Builder>
            implements BuilderOf<Message> {

        private final List<Recipient> mRecipientList = new ArrayList<>();

        /** Adds a {@link Person}. */
        @NonNull
        public final Builder addRecipient(@NonNull Person person) {
            mRecipientList.add(new Recipient(person));
            return this;
        }

        /** Adds a {@link Recipient}. */
        @NonNull
        public final Builder addRecipient(@NonNull Recipient recipient) {
            mRecipientList.add(recipient);
            return this;
        }

        /** Adds a list of {@link Recipient}s. */
        @NonNull
        public final Builder addAllRecipient(@NonNull Iterable<Recipient> recipients) {
            for (Recipient recipient : recipients) {
                mRecipientList.add(recipient);
            }
            return this;
        }

        /** Sets the message text. */
        @NonNull
        public abstract Builder setMessageText(@NonNull String messageText);

        /** Builds and returns the Message instance. */
        @Override
        @NonNull
        public final Message build() {
            setRecipientList(mRecipientList);
            return autoBuild();
        }

        /** Sets the recipients of the message. */
        @NonNull
        abstract Builder setRecipientList(@NonNull List<Recipient> recipientList);

        @NonNull
        abstract Message autoBuild();
    }
}
