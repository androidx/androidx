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

import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Item;
import androidx.car.app.utils.CollectionUtils;
import androidx.core.app.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Represents a conversation */
@ExperimentalCarApi
@CarProtocol
@KeepFields
@RequiresCarApi(6)
public class ConversationItem implements Item {
    @NonNull
    private final String mId;
    @NonNull
    private final CarText mTitle;
    @NonNull
    private final Bundle mSelf;
    @Nullable
    private final CarIcon mIcon;
    private final boolean mIsGroupConversation;
    @NonNull
    private final List<CarMessage> mMessages;
    @NonNull
    private final ConversationCallbackDelegate mConversationCallbackDelegate;

    @Override
    public int hashCode() {
        return Objects.hash(
                PersonsEqualityHelper.getPersonHashCode(getSelf()),
                mId,
                mTitle,
                mIcon,
                mIsGroupConversation,
                mMessages
        );
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConversationItem)) {
            return false;
        }
        ConversationItem otherConversationItem = (ConversationItem) other;

        return
                Objects.equals(mId, otherConversationItem.mId)
                        && Objects.equals(mTitle, otherConversationItem.mTitle)
                        && Objects.equals(mIcon, otherConversationItem.mIcon)
                        && PersonsEqualityHelper
                            .arePersonsEqual(getSelf(), otherConversationItem.getSelf())
                        && mIsGroupConversation == otherConversationItem.mIsGroupConversation
                        && Objects.equals(mMessages, otherConversationItem.mMessages)
                ;
    }

    ConversationItem(@NonNull Builder builder) {
        this.mId = requireNonNull(builder.mId);
        this.mTitle = requireNonNull(builder.mTitle);
        this.mSelf = validateSender(builder.mSelf).toBundle();
        this.mIcon = builder.mIcon;
        this.mIsGroupConversation = builder.mIsGroupConversation;
        this.mMessages = requireNonNull(CollectionUtils.unmodifiableCopy(builder.mMessages));
        checkState(!mMessages.isEmpty(), "Message list cannot be empty.");
        this.mConversationCallbackDelegate = new ConversationCallbackDelegateImpl(
                requireNonNull(builder.mConversationCallback));
    }

    /** Default constructor for serialization. */
    private ConversationItem() {
        mId = "";
        mTitle = new CarText.Builder("").build();
        mSelf = new Person.Builder().setName("").build().toBundle();
        mIcon = null;
        mIsGroupConversation = false;
        mMessages = new ArrayList<>();
        mConversationCallbackDelegate = new ConversationCallbackDelegateImpl(
                new ConversationCallback() {
                    @Override
                    public void onMarkAsRead() {
                        // Do nothing
                    }

                    @Override
                    public void onTextReply(@NonNull String replyText) {
                        // Do nothing
                    }
                });
    }

    /**
     * Returns a unique identifier for the conversation
     *
     * @see Builder#setId
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /** Returns the title of the conversation */
    @NonNull
    public CarText getTitle() {
        return mTitle;
    }

    /** Returns a {@link Person} for the conversation */
    @NonNull
    public Person getSelf() {
        return Person.fromBundle(mSelf);
    }

    /** Returns a {@link CarIcon} for the conversation, or {@code null} if not set */
    @Nullable
    public CarIcon getIcon() {
        return mIcon;
    }

    /**
     * Returns whether this conversation involves 3+ participants (a "group" conversation)
     *
     * @see Builder#setGroupConversation(boolean)
     */
    public boolean isGroupConversation() {
        return mIsGroupConversation;
    }

    /** Returns a list of messages for this {@link ConversationItem} */
    @NonNull
    public List<CarMessage> getMessages() {
        return mMessages;
    }

    /** Returns host->client callbacks for this conversation */
    @NonNull
    public ConversationCallbackDelegate getConversationCallbackDelegate() {
        return mConversationCallbackDelegate;
    }

    /**
     * Verifies that a given {@link Person} has the required fields to be a message sender. Returns
     * the input {@link Person} if valid, or throws an exception if invalid.
     *
     * <p> See also {@link ConversationItem#getSelf()} and {@link CarMessage#getSender()}.
     */
    static Person validateSender(@Nullable Person person) {
        requireNonNull(person);
        requireNonNull(person.getName());
        requireNonNull(person.getKey());
        return person;
    }

    /** A builder for {@link ConversationItem} */
    public static final class Builder {
        @Nullable
        String mId;
        @Nullable
        CarText mTitle;
        @Nullable
        Person mSelf;
        @Nullable
        CarIcon mIcon;
        boolean mIsGroupConversation;
        @Nullable
        List<CarMessage> mMessages;
        @Nullable
        ConversationCallback mConversationCallback;

        /**
         * Specifies a unique identifier for the conversation
         *
         * <p> IDs may be used for a variety of purposes, including...
         * <ul>
         *     <li> Distinguishing new {@link ConversationItem}s from updated
         *     {@link ConversationItem}s in the UI, when data is refreshed
         *     <li> Identifying {@link ConversationItem}s in "mark as read" / "reply" callbacks
         * </ul>
         */
        @NonNull
        public Builder setId(@NonNull String id) {
            mId = id;
            return this;
        }

        /** Sets the title of the conversation */
        @NonNull
        public Builder setTitle(@NonNull CarText title) {
            mTitle = title;
            return this;
        }

        /** Sets a {@link CarIcon} for the conversation */
        @NonNull
        public Builder setIcon(@NonNull CarIcon icon) {
            mIcon = icon;
            return this;
        }

        /**
         * Sets a {@link Person} for the conversation
         *
         * <p> The {@link Person} must specify a non-null
         * {@link Person.Builder#setName(CharSequence)} and
         * {@link Person.Builder#setKey(String)}.
         */
        @NonNull
        public Builder setSelf(@NonNull Person self) {
            mSelf = self;
            return this;
        }

        /**
         * Specifies whether this conversation involves 3+ participants (a "group" conversation)
         *
         * <p> If unspecified, conversations are assumed to have exactly two participants (a "1:1"
         * conversation)
         *
         * <p> UX presentation may differ slightly between group and 1:1 conversations. As a
         * historical example, message readout may include sender names for group conversations, but
         * omit them for 1:1 conversations.
         */
        @NonNull
        public Builder setGroupConversation(boolean isGroupConversation) {
            mIsGroupConversation = isGroupConversation;
            return this;
        }

        /** Specifies a list of messages for the conversation */
        @NonNull
        public Builder setMessages(@NonNull List<CarMessage> messages) {
            mMessages = messages;
            return this;
        }

        /** Sets a {@link ConversationCallback} for the conversation */
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        @NonNull
        public Builder setConversationCallback(
                @NonNull ConversationCallback conversationCallback) {
            mConversationCallback = conversationCallback;
            return this;
        }

        /** Returns a new {@link ConversationItem} instance defined by this builder */
        @NonNull
        public ConversationItem build() {
            return new ConversationItem(this);
        }
    }
}
