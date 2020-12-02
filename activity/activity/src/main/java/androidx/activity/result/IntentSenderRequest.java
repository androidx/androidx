/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.activity.result;

import static android.content.Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
import static android.content.Intent.FLAG_ACTIVITY_FORWARD_RESULT;
import static android.content.Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY;
import static android.content.Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;
import static android.content.Intent.FLAG_ACTIVITY_MATCH_EXTERNAL;
import static android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;
import static android.content.Intent.FLAG_ACTIVITY_NO_HISTORY;
import static android.content.Intent.FLAG_ACTIVITY_NO_USER_ACTION;
import static android.content.Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP;
import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
import static android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;
import static android.content.Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static android.content.Intent.FLAG_ACTIVITY_TASK_ON_HOME;
import static android.content.Intent.FLAG_DEBUG_LOG_RESOLUTION;
import static android.content.Intent.FLAG_EXCLUDE_STOPPED_PACKAGES;
import static android.content.Intent.FLAG_FROM_BACKGROUND;
import static android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_PREFIX_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
import static android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A request for a
 * {@link androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult}
 * Activity Contract.
 */
@SuppressLint("BanParcelableUsage")
public final class IntentSenderRequest implements Parcelable {
    @NonNull
    private final IntentSender mIntentSender;
    @Nullable
    private final Intent mFillInIntent;
    private final int mFlagsMask;
    private final int mFlagsValues;

    IntentSenderRequest(@NonNull IntentSender intentSender, @Nullable Intent intent, int flagsMask,
            int flagsValues) {
        mIntentSender = intentSender;
        mFillInIntent = intent;
        mFlagsMask = flagsMask;
        mFlagsValues = flagsValues;
    }

    /**
     * Get the intentSender from this IntentSenderRequest.
     *
     * @return the IntentSender to launch.
     */
    @NonNull
    public IntentSender getIntentSender() {
        return mIntentSender;
    }

    /**
     * Get the intent from this IntentSender request.  If non-null, this will be provided as the
     * intent parameter to IntentSender#sendIntent.
     *
     * @return the fill in intent.
     */
    @Nullable
    public Intent getFillInIntent() {
        return mFillInIntent;
    }

    /**
     * Get the flag mask from this IntentSender request.
     *
     * @return intent flags in the original IntentSender that you would like to change.
     */
    public int getFlagsMask() {
        return mFlagsMask;
    }

    /**
     * Get the flag values from this IntentSender request.
     *
     * @return desired values for any bits set in flagsMask
     */
    public int getFlagsValues() {
        return mFlagsValues;
    }

    @SuppressWarnings("ConstantConditions")
    IntentSenderRequest(@NonNull Parcel in) {
        mIntentSender = in.readParcelable(IntentSender.class.getClassLoader());
        mFillInIntent = in.readParcelable(Intent.class.getClassLoader());
        mFlagsMask = in.readInt();
        mFlagsValues = in.readInt();
    }

    @NonNull
    public static final Creator<IntentSenderRequest> CREATOR = new Creator<IntentSenderRequest>() {
        @Override
        public IntentSenderRequest createFromParcel(Parcel in) {
            return new IntentSenderRequest(in);
        }

        @Override
        public IntentSenderRequest[] newArray(int size) {
            return new IntentSenderRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mIntentSender, flags);
        dest.writeParcelable(mFillInIntent, flags);
        dest.writeInt(mFlagsMask);
        dest.writeInt(mFlagsValues);
    }

    /**
     * A builder for constructing {@link IntentSenderRequest} instances.
     */
    public static final class Builder {
        private IntentSender mIntentSender;
        private Intent mFillInIntent;
        private int mFlagsMask;
        private int mFlagsValues;

        @IntDef({FLAG_GRANT_READ_URI_PERMISSION, FLAG_GRANT_WRITE_URI_PERMISSION,
                FLAG_FROM_BACKGROUND, FLAG_DEBUG_LOG_RESOLUTION, FLAG_EXCLUDE_STOPPED_PACKAGES,
                FLAG_INCLUDE_STOPPED_PACKAGES, FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                FLAG_GRANT_PREFIX_URI_PERMISSION, FLAG_ACTIVITY_MATCH_EXTERNAL,
                FLAG_ACTIVITY_NO_HISTORY, FLAG_ACTIVITY_SINGLE_TOP, FLAG_ACTIVITY_NEW_TASK,
                FLAG_ACTIVITY_MULTIPLE_TASK, FLAG_ACTIVITY_CLEAR_TOP,
                FLAG_ACTIVITY_FORWARD_RESULT, FLAG_ACTIVITY_PREVIOUS_IS_TOP,
                FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS, FLAG_ACTIVITY_BROUGHT_TO_FRONT,
                FLAG_ACTIVITY_RESET_TASK_IF_NEEDED, FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
                FLAG_ACTIVITY_NEW_DOCUMENT, FLAG_ACTIVITY_NO_USER_ACTION,
                FLAG_ACTIVITY_REORDER_TO_FRONT, FLAG_ACTIVITY_NO_ANIMATION,
                FLAG_ACTIVITY_CLEAR_TASK, FLAG_ACTIVITY_TASK_ON_HOME,
                FLAG_ACTIVITY_RETAIN_IN_RECENTS, FLAG_ACTIVITY_LAUNCH_ADJACENT})
        @Retention(RetentionPolicy.SOURCE)
        private @interface Flag {}

        /**
         * Constructor that takes an {@link IntentSender} and sets it for the builder.
         *
         * @param intentSender IntentSender to go in the IntentSenderRequest.
         */
        public Builder(@NonNull IntentSender intentSender) {
            mIntentSender = intentSender;
        }

        /**
         * Convenience constructor that takes an {@link PendingIntent} and uses
         * its {@link IntentSender}.
         *
         * @param pendingIntent the pendingIntent containing with the intentSender to go in the
         *                      IntentSenderRequest.
         */
        public Builder(@NonNull PendingIntent pendingIntent) {
            this(pendingIntent.getIntentSender());
        }

        /**
         * Set the intent for the {@link IntentSenderRequest}.
         *
         * @param fillInIntent intent to go in the IntentSenderRequest. If non-null, this
         *                     will be provided as the intent parameter to IntentSender#sendIntent.
         * @return This builder.
         */
        @NonNull
        public Builder setFillInIntent(@Nullable Intent fillInIntent) {
            mFillInIntent = fillInIntent;
            return this;
        }

        /**
         * Set the flag mask and flag values for the {@link IntentSenderRequest}.
         *
         * @param values flagValues to go in the IntentSenderRequest. Desired values for any bits
         *             set in flagsMask
         * @param mask mask to go in the IntentSenderRequest. Intent flags in the original
         *             IntentSender that you would like to change.
         *
         * @return This builder.
         */
        @NonNull
        public Builder setFlags(@Flag int values, int mask) {
            mFlagsValues = values;
            mFlagsMask = mask;
            return this;
        }

        /**
         * Build the IntentSenderRequest specified by this builder.
         *
         * @return the newly constructed IntentSenderRequest.
         */
        @NonNull
        public IntentSenderRequest build() {
            return new IntentSenderRequest(mIntentSender, mFillInIntent, mFlagsMask, mFlagsValues);
        }
    }
}
