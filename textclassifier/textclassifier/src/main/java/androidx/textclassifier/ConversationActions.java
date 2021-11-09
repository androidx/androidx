/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.textclassifier;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.SpannedString;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.core.app.Person;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a list of actions suggested by a {@link TextClassifier} on a given conversation.
 * <p>
 * This is an object to store the result of {@link TextClassifier#suggestConversationActions(Request)}.
 *
 * @see TextClassifier#suggestConversationActions(Request)
 */
public final class ConversationActions {
    private static final String EXTRA_CONVERSATION_ACTIONS = "conversation_actions";
    private static final String EXTRA_ID = "id";

    private final List<ConversationAction> mConversationActions;
    private final String mId;

    /** Constructs a {@link ConversationActions} object. */
    public ConversationActions(
            @NonNull List<ConversationAction> conversationActions, @Nullable String id) {
        mConversationActions =
                Collections.unmodifiableList(Preconditions.checkNotNull(conversationActions));
        mId = id;
    }

    /**
     * Returns an immutable list of {@link ConversationAction} objects, which are ordered from high
     * confidence to low confidence.
     */
    @NonNull
    public List<ConversationAction> getConversationActions() {
        return mConversationActions;
    }

    /**
     * Returns the id, if one exists, for this object.
     */
    @Nullable
    public String getId() {
        return mId;
    }

    /**
     * Adds this object to a Bundle that can be read back with the same parameters
     * to {@link #createFromBundle(Bundle)}.
     */
    @NonNull
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        BundleUtils.putConversationActionsList(
                bundle, EXTRA_CONVERSATION_ACTIONS, mConversationActions);
        bundle.putString(EXTRA_ID, mId);
        return bundle;
    }

    /**
     * Converts a bundle that was created using {@link #toBundle()} to a
     * {@link ConversationActions}.
     */
    @NonNull
    public static ConversationActions createFromBundle(@NonNull Bundle bundle) {
        return new ConversationActions(
                BundleUtils.getConversationActionsList(bundle, EXTRA_CONVERSATION_ACTIONS),
                bundle.getString(EXTRA_ID));
    }

    /**
     * Converts a {@link android.view.textclassifier.ConversationActions} to {@link
     * androidx.textclassifier.ConversationActions}.
     */
    @Nullable
    @RequiresApi(29)
    static ConversationActions fromPlatform(
            @Nullable android.view.textclassifier.ConversationActions conversationActions) {
        if (conversationActions == null) {
            return null;
        }
        return new ConversationActions(
                conversationActions.getConversationActions().stream()
                        .map(ConversationAction::fromPlatform)
                        .collect(Collectors.toList()),
                conversationActions.getId());
    }

    @RequiresApi(29)
    android.view.textclassifier.ConversationActions toPlatform() {
        return new android.view.textclassifier.ConversationActions(
                    getConversationActions().stream()
                        .map(ConversationAction::toPlatform)
                        .collect(Collectors.toList()),
                getId());
    }

    /** Represents a message in the conversation. */
    public static final class Message {
        private static final String EXTRA_AUTHOR = "author";
        private static final String EXTRA_REFERENCE_TIME = "reference_time";
        private static final String EXTRA_TEXT = "text";
        private static final String EXTRA_EXTRAS = "extras";

        /**
         * Represents the local user.
         */
        @NonNull
        public static final Person PERSON_USER_SELF =
                new Person.Builder()
                        .setKey("text-classifier-conversation-actions-user-self")
                        .build();

        /**
         * Represents the remote user.
         * <p>
         * If possible, you are suggested to create a {@link Person} object that can identify
         * the remote user better, so that the underlying model could differentiate between
         * different remote users.
         */
        @NonNull
        public static final Person PERSON_USER_OTHERS =
                new Person.Builder()
                        .setKey("text-classifier-conversation-actions-user-others")
                        .build();

        @NonNull
        private final Person mAuthor;
        @Nullable
        private final Long mReferenceTime;
        @Nullable
        private final CharSequence mText;
        @NonNull
        private final Bundle mExtras;

        Message(
                @Nullable Person author,
                @Nullable Long referenceTime,
                @Nullable CharSequence text,
                @NonNull Bundle bundle) {
            mAuthor = author;
            mReferenceTime = referenceTime;
            mText = text;
            mExtras = Preconditions.checkNotNull(bundle);
        }

        /** Returns the person that composed the message. */
        @NonNull
        public Person getAuthor() {
            return mAuthor;
        }

        /**
         * Returns the reference time of the message, for example it could be the compose or send
         * time of this message. This should be milliseconds from the epoch of
         * 1970-01-01T00:00:00Z(UTC timezone). If no reference time or {@code null} is set,
         * now is used.
         */
        @Nullable
        public Long getReferenceTime() {
            return mReferenceTime;
        }

        /** Returns the text of the message. */
        @Nullable
        public CharSequence getText() {
            return mText;
        }

        /**
         * Returns the extended data related to this conversation action.
         *
         * <p><b>NOTE: </b>Each call to this method returns a new bundle copy so clients should
         * prefer to hold a reference to the returned bundle rather than frequently calling this
         * method.
         */
        @NonNull
        public Bundle getExtras() {
            return mExtras;
        }

        /**
         * Adds this object to a Bundle that can be read back with the same parameters
         * to {@link #createFromBundle(Bundle)}.
         */
        @NonNull
        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putBundle(EXTRA_AUTHOR, mAuthor.toBundle());
            BundleUtils.putLong(bundle, EXTRA_REFERENCE_TIME, mReferenceTime);
            bundle.putCharSequence(EXTRA_TEXT, mText);
            bundle.putBundle(EXTRA_EXTRAS, mExtras);
            return bundle;
        }

        /**
         * Converts a bundle that was created using {@link #toBundle()} to a
         * {@link ConversationActions.Message}.
         */
        @NonNull
        public static Message createFromBundle(@NonNull Bundle bundle) {
            return new ConversationActions.Message(
                    Person.fromBundle(bundle.getBundle(EXTRA_AUTHOR)),
                    BundleUtils.getLong(bundle, EXTRA_REFERENCE_TIME),
                    bundle.getCharSequence(EXTRA_TEXT),
                    bundle.getBundle(EXTRA_EXTRAS));
        }

        @RequiresApi(29)
        android.view.textclassifier.ConversationActions.Message toPlatform() {
            return new android.view.textclassifier.ConversationActions.Message.Builder(
                    getAuthor().toAndroidPerson())
                    .setText(getText())
                    .setReferenceTime(ConvertUtils.createZonedDateTimeFromUtc(getReferenceTime()))
                    .setExtras(getExtras())
                    .build();
        }

        @RequiresApi(29)
        @Nullable
        static Message fromPlatform(
                @Nullable android.view.textclassifier.ConversationActions.Message message) {
            if (message == null) {
                return null;
            }
            return new Message.Builder(
                    Person.fromAndroidPerson(message.getAuthor()))
                    .setText(message.getText())
                    .setReferenceTime(ConvertUtils.zonedDateTimeToUtcMs(message.getReferenceTime()))
                    .setExtras(message.getExtras())
                    .build();
        }

        /** Builder class to construct a {@link Message} */
        public static final class Builder {
            @Nullable
            Person mAuthor;
            @Nullable
            private Long mReferenceTime;
            @Nullable
            private CharSequence mText;
            @Nullable
            private Bundle mExtras;

            /**
             * Constructs a builder.
             *
             * @param author the person that composed the message, use {@link #PERSON_USER_SELF}
             *               to represent the local user. If it is not possible to identify the
             *               remote user that the local user is conversing with, use
             *               {@link #PERSON_USER_OTHERS} to represent a remote user.
             */
            public Builder(@NonNull Person author) {
                mAuthor = Preconditions.checkNotNull(author);
            }

            /** Sets the text of this message. */
            @NonNull
            public Builder setText(@Nullable CharSequence text) {
                mText = text;
                return this;
            }

            /**
             * Sets the reference time of this message, for example it could be the compose or send
             * time of this message. This should be milliseconds from the epoch of
             * 1970-01-01T00:00:00Z(UTC timezone). If no reference time or {@code null} is set,
             * now is used.
             */
            @NonNull
            public Builder setReferenceTime(@Nullable Long referenceTime) {
                mReferenceTime = referenceTime;
                return this;
            }

            /** Sets a set of extended data to the message. */
            @NonNull
            public Builder setExtras(@Nullable Bundle bundle) {
                this.mExtras = bundle;
                return this;
            }

            /** Builds the {@link Message} object. */
            @NonNull
            public Message build() {
                return new Message(
                        mAuthor,
                        mReferenceTime,
                        mText == null ? null : new SpannedString(mText),
                        mExtras == null ? Bundle.EMPTY : mExtras);
            }
        }
    }

    /**
     * A request object for generating conversation action suggestions.
     *
     * @see TextClassifier#suggestConversationActions(Request)
     */
    public static final class Request {
        private static final String EXTRA_MESSAGES = "messages";
        private static final String EXTRA_ENTITY_CONFIG = "entity_config";
        private static final String EXTRA_MAX_SUGGESTION = "max_suggestion";
        private static final String EXTRA_HINTS = "hints";
        private static final String EXTRA_EXTRAS = "extras";


        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(SOURCE)
        @StringDef(
                value = {
                        HINT_FOR_NOTIFICATION,
                        HINT_FOR_IN_APP,
                })
        public @interface Hint {}

        /**
         * To indicate the generated actions will be used within the app.
         */
        public static final String HINT_FOR_IN_APP = "in_app";
        /**
         * To indicate the generated actions will be used for notification.
         */
        public static final String HINT_FOR_NOTIFICATION = "notification";

        @NonNull
        private final List<Message> mConversation;
        @NonNull
        private final TextClassifier.EntityConfig mTypeConfig;
        private final int mMaxSuggestions;
        @NonNull
        @Hint
        private final List<String> mHints;
        @NonNull
        private final Bundle mExtras;

        Request(
                @NonNull List<Message> conversation,
                @NonNull TextClassifier.EntityConfig typeConfig,
                int maxSuggestions,
                @NonNull @Hint List<String> hints,
                @NonNull Bundle extras) {
            mConversation = Preconditions.checkNotNull(conversation);
            mTypeConfig = Preconditions.checkNotNull(typeConfig);
            mMaxSuggestions = maxSuggestions;
            mHints = Preconditions.checkNotNull(hints);
            mExtras = Preconditions.checkNotNull(extras);
        }

        /** Returns the type config. */
        @NonNull
        public TextClassifier.EntityConfig getTypeConfig() {
            return mTypeConfig;
        }

        /** Returns an immutable list of messages that make up the conversation. */
        @NonNull
        public List<Message> getConversation() {
            return mConversation;
        }

        /**
         * Return the maximal number of suggestions the caller wants, value -1 means no restriction
         * and this is the default.
         */
        @IntRange(from = -1)
        public int getMaxSuggestions() {
            return mMaxSuggestions;
        }

        /** Returns an immutable list of hints */
        @NonNull
        @Hint
        public List<String> getHints() {
            return mHints;
        }

        /**
         * Returns the extended data related to this request.
         *
         * <p><b>NOTE: </b>Do not modify this bundle.
         */
        @NonNull
        public Bundle getExtras() {
            return mExtras;
        }

        /**
         * Adds this object to a Bundle that can be read back with the same parameters
         * to {@link #createFromBundle(Bundle)}.
         */
        @NonNull
        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            BundleUtils.putConversationActionsMessageList(bundle, EXTRA_MESSAGES, mConversation);
            bundle.putBundle(EXTRA_ENTITY_CONFIG, mTypeConfig.toBundle());
            bundle.putInt(EXTRA_MAX_SUGGESTION, mMaxSuggestions);
            bundle.putStringArrayList(EXTRA_HINTS, new ArrayList<>(mHints));
            bundle.putBundle(EXTRA_EXTRAS, mExtras);
            return bundle;
        }

        /**
         * Converts a bundle that was created using {@link #toBundle()} to a
         * {@link ConversationActions.Request}.
         */
        @NonNull
        public static Request createFromBundle(@NonNull Bundle bundle) {
            return new Request(
                    BundleUtils.getConversationActionsMessageList(bundle, EXTRA_MESSAGES),
                    TextClassifier.EntityConfig.createFromBundle(
                            bundle.getBundle(EXTRA_ENTITY_CONFIG)),
                    bundle.getInt(EXTRA_MAX_SUGGESTION),
                    bundle.getStringArrayList(EXTRA_HINTS),
                    bundle.getBundle(EXTRA_EXTRAS));
        }

        @SuppressLint("WrongConstant")
        @RequiresApi(29)
        @NonNull
        android.view.textclassifier.ConversationActions.Request toPlatform() {
            android.view.textclassifier.ConversationActions.Request.Builder builder =
                    new android.view.textclassifier.ConversationActions.Request.Builder(
                    getConversation().stream()
                            .map(msg -> msg.toPlatform())
                            .collect(Collectors.toList()))
                    .setHints(getHints())
                    .setExtras(getExtras())
                    .setTypeConfig(getTypeConfig().toPlatform());

            // To workaround a bug in platform that setMaxSuggestions does not accept -1 as input,
            // which is actually valid and the default value.
            if (getMaxSuggestions() >= 0) {
                builder.setMaxSuggestions(getMaxSuggestions());
            }

            return builder.build();
        }

        @RequiresApi(29)
        @Nullable
        static ConversationActions.Request fromPlatform(
                @Nullable android.view.textclassifier.ConversationActions.Request request) {
            if (request == null) {
                return null;
            }
            return new ConversationActions.Request.Builder(
                    request.getConversation().stream()
                            .map(Message::fromPlatform)
                            .collect(Collectors.toList()))
                    .setHints(request.getHints())
                    .setMaxSuggestions(request.getMaxSuggestions())
                    .setExtras(request.getExtras())
                    .setTypeConfig(
                            TextClassifier.EntityConfig.fromPlatform(request.getTypeConfig()))
                    .build();
        }

        /** Builder object to construct the {@link Request} object. */
        public static final class Builder {
            @NonNull
            private List<Message> mConversation;
            @Nullable
            private TextClassifier.EntityConfig mTypeConfig;
            private int mMaxSuggestions = -1;
            @Nullable
            @Hint
            private List<String> mHints;
            @Nullable
            private Bundle mExtras;

            /**
             * Constructs a builder.
             *
             * @param conversation the conversation that the text classifier is going to generate
             *     actions for.
             */
            public Builder(@NonNull List<Message> conversation) {
                mConversation = Preconditions.checkNotNull(conversation);
            }

            /**
             * Sets the hints to help text classifier to generate actions. It could be used to help
             * text classifier to infer what types of actions the caller may be interested in.
             */
            @NonNull
            public Builder setHints(@Nullable @Hint List<String> hints) {
                mHints = hints;
                return this;
            }

            /** Sets the type config. */
            @NonNull
            public Builder setTypeConfig(@Nullable TextClassifier.EntityConfig typeConfig) {
                mTypeConfig = typeConfig;
                return this;
            }

            /**
             * Sets the maximum number of suggestions you want. Value -1 means no restriction and
             * this is the default.
             */
            @NonNull
            public Builder setMaxSuggestions(@IntRange(from = -1) int maxSuggestions) {
                if (maxSuggestions < -1) {
                    throw new IllegalArgumentException("maxSuggestions has to be greater than or "
                            + "equal to -1.");
                }
                mMaxSuggestions = maxSuggestions;
                return this;
            }

            /** Sets a set of extended data to the request. */
            @NonNull
            public Builder setExtras(@Nullable Bundle bundle) {
                mExtras = bundle;
                return this;
            }

            /** Builds the {@link Request} object. */
            @NonNull
            public Request build() {
                return new Request(
                        Collections.unmodifiableList(mConversation),
                        mTypeConfig == null
                                ? new TextClassifier.EntityConfig.Builder().build()
                                : mTypeConfig,
                        mMaxSuggestions,
                        mHints == null
                                ? Collections.<String>emptyList()
                                : Collections.unmodifiableList(mHints),
                        mExtras == null ? Bundle.EMPTY : mExtras);
            }
        }
    }
}
