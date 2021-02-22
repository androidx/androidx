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

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.core.app.RemoteActionCompat;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelUtils;

import java.lang.annotation.Retention;

/**
 * Represents an action suggested by a {@link TextClassifier} on a given conversation.
 *
 * @see TextClassifier#suggestConversationActions(ConversationActions.Request)
 * @see ConversationActions
 */
public final class ConversationAction {

    private static final String EXTRA_TYPE = "type";
    private static final String EXTRA_TEXT_REPLY = "text_reply";
    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_SCORE = "score";
    private static final String EXTRA_EXTRAS = "extras";

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(SOURCE)
    @StringDef(
            value = {
                    TYPE_VIEW_CALENDAR,
                    TYPE_VIEW_MAP,
                    TYPE_TRACK_FLIGHT,
                    TYPE_OPEN_URL,
                    TYPE_SEND_SMS,
                    TYPE_CALL_PHONE,
                    TYPE_SEND_EMAIL,
                    TYPE_TEXT_REPLY,
                    TYPE_CREATE_REMINDER,
                    TYPE_SHARE_LOCATION,
                    TYPE_ADD_CONTACT,
                    TYPE_COPY
            })
    public @interface ActionType {
    }

    /**
     * Indicates an action to view a calendar at a specified time.
     */
    public static final String TYPE_VIEW_CALENDAR =
            android.view.textclassifier.ConversationAction.TYPE_VIEW_CALENDAR;
    /**
     * Indicates an action to view the map at a specified location.
     */
    public static final String TYPE_VIEW_MAP =
            android.view.textclassifier.ConversationAction.TYPE_VIEW_MAP;
    /**
     * Indicates an action to track a flight.
     */
    public static final String TYPE_TRACK_FLIGHT =
            android.view.textclassifier.ConversationAction.TYPE_TRACK_FLIGHT;
    /**
     * Indicates an action to open an URL.
     */
    public static final String TYPE_OPEN_URL =
            android.view.textclassifier.ConversationAction.TYPE_OPEN_URL;
    /**
     * Indicates an action to send a SMS.
     */
    public static final String TYPE_SEND_SMS =
            android.view.textclassifier.ConversationAction.TYPE_SEND_SMS;
    /**
     * Indicates an action to call a phone number.
     */
    public static final String TYPE_CALL_PHONE =
            android.view.textclassifier.ConversationAction.TYPE_CALL_PHONE;
    /**
     * Indicates an action to send an email.
     */
    public static final String TYPE_SEND_EMAIL =
            android.view.textclassifier.ConversationAction.TYPE_SEND_EMAIL;
    /**
     * Indicates an action to reply with a text message.
     */
    public static final String TYPE_TEXT_REPLY =
            android.view.textclassifier.ConversationAction.TYPE_TEXT_REPLY;
    /**
     * Indicates an action to create a reminder.
     */
    public static final String TYPE_CREATE_REMINDER =
            android.view.textclassifier.ConversationAction.TYPE_CREATE_REMINDER;
    /**
     * Indicates an action to reply with a location.
     */
    public static final String TYPE_SHARE_LOCATION =
            android.view.textclassifier.ConversationAction.TYPE_SHARE_LOCATION;

    /**
     * Indicates an action to add a contact.
     */
    public static final String TYPE_ADD_CONTACT = "add_contact";

    /**
     * Indicates an action to copy a code.
     */
    public static final String TYPE_COPY = "copy";

    @NonNull
    @ActionType
    private final String mType;
    @NonNull
    private final CharSequence mTextReply;
    @Nullable
    private final RemoteActionCompat mAction;
    @FloatRange(from = 0, to = 1)
    private final float mScore;
    @NonNull
    private final Bundle mExtras;

    ConversationAction(
            @NonNull String type,
            @Nullable RemoteActionCompat action,
            @Nullable CharSequence textReply,
            float score,
            @NonNull Bundle extras) {
        mType = Preconditions.checkNotNull(type);
        mAction = action;
        mTextReply = textReply;
        mScore = score;
        mExtras = Preconditions.checkNotNull(extras);
    }

    /** Returns the type of this action, for example, {@link #TYPE_VIEW_CALENDAR}. */
    @NonNull
    @ActionType
    public String getType() {
        return mType;
    }

    /**
     * Returns a RemoteActionCompat object, which contains the icon, label and a PendingIntent, for
     * the specified action type.
     */
    @Nullable
    public RemoteActionCompat getAction() {
        return mAction;
    }

    /**
     * Returns the confidence score for the specified action. The value ranges from 0 (low
     * confidence) to 1 (high confidence).
     */
    @FloatRange(from = 0, to = 1)
    public float getConfidenceScore() {
        return mScore;
    }

    /**
     * Returns the text reply that could be sent as a reply to the given conversation.
     * <p>
     * This is only available when the type of the action is {@link #TYPE_TEXT_REPLY}.
     */
    @Nullable
    public CharSequence getTextReply() {
        return mTextReply;
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
        bundle.putString(EXTRA_TYPE, mType);
        bundle.putCharSequence(EXTRA_TEXT_REPLY, mTextReply);
        ParcelUtils.putVersionedParcelable(bundle, EXTRA_ACTION, mAction);
        bundle.putFloat(EXTRA_SCORE, mScore);
        bundle.putBundle(EXTRA_EXTRAS, mExtras);
        return bundle;
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(29)
    android.view.textclassifier.ConversationAction toPlatform() {
        return new android.view.textclassifier.ConversationAction.Builder(getType())
                .setAction(
                        getAction() == null
                                ? null
                                : getAction().toRemoteAction())
                .setConfidenceScore(getConfidenceScore())
                .setTextReply(getTextReply())
                .setExtras(getExtras())
                .build();
    }

    /**
     * Converts a bundle that was created using {@link #toBundle()} to a {@link ConversationAction}.
     */
    @NonNull
    public static ConversationAction createFromBundle(@NonNull Bundle bundle) {
        return new ConversationAction(
                bundle.getString(EXTRA_TYPE),
                (RemoteActionCompat) ParcelUtils.getVersionedParcelable(bundle, EXTRA_ACTION),
                bundle.getCharSequence(EXTRA_TEXT_REPLY),
                bundle.getFloat(EXTRA_SCORE),
                bundle.getBundle(EXTRA_EXTRAS));
    }

    @Nullable
    @RequiresApi(29)
    static ConversationAction fromPlatform(
            @Nullable android.view.textclassifier.ConversationAction conversationAction) {
        if (conversationAction == null) {
            return null;
        }
        return new ConversationAction.Builder(conversationAction.getType())
                .setAction(
                        conversationAction.getAction() == null
                                ? null
                                : RemoteActionCompat.createFromRemoteAction(
                                        conversationAction.getAction()))
                .setConfidenceScore(conversationAction.getConfidenceScore())
                .setTextReply(conversationAction.getTextReply())
                .setExtras(conversationAction.getExtras())
                .build();
    }

    /** Builder class to construct {@link ConversationAction}. */
    public static final class Builder {
        @Nullable
        @ActionType
        private String mType;
        @Nullable
        private RemoteActionCompat mAction;
        @Nullable
        private CharSequence mTextReply;
        private float mScore;
        @Nullable
        private Bundle mExtras;

        public Builder(@NonNull @ActionType String actionType) {
            mType = Preconditions.checkNotNull(actionType);
        }

        /**
         * Sets an action that may be performed on the given conversation.
         */
        @NonNull
        public Builder setAction(@Nullable RemoteActionCompat action) {
            mAction = action;
            return this;
        }

        /**
         * Sets a text reply that may be performed on the given conversation.
         */
        @NonNull
        public Builder setTextReply(@Nullable CharSequence textReply) {
            mTextReply = textReply;
            return this;
        }

        /** Sets the confident score. */
        @NonNull
        public Builder setConfidenceScore(@FloatRange(from = 0, to = 1) float score) {
            mScore = score;
            return this;
        }

        /**
         * Sets the extended data for the conversation action object.
         */
        @NonNull
        public Builder setExtras(@Nullable Bundle extras) {
            mExtras = extras;
            return this;
        }

        /** Builds the {@link ConversationAction} object. */
        @NonNull
        public ConversationAction build() {
            return new ConversationAction(
                    mType,
                    mAction,
                    mTextReply,
                    mScore,
                    mExtras == null ? Bundle.EMPTY : mExtras);
        }
    }
}
