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

package androidx.browser.customtabs;

import android.app.PendingIntent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.IntRange;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a custom action that can be invoked on specific content types (image or link)
 * within a Custom Tab.
 * Instances of this class are created using {@link CustomContentAction.Builder} and added to
 * a {@link CustomTabsIntent} using
 * {@link CustomTabsIntent.Builder#addCustomContentAction(CustomContentAction)}.
 */
@ExperimentalCustomContentAction
public final class CustomContentAction {

    /**
     * Key for the action ID in the Bundle.
     */
    static final String KEY_ID = "androidx.browser.customtabs.customcontentaction.ID";

    /**
     * Key for the action label in the Bundle.
     */
    static final String KEY_LABEL = "androidx.browser.customtabs.customcontentaction.LABEL";

    /**
     * Key for the action PendingIntent in the Bundle.
     */
    static final String KEY_PENDING_INTENT =
            "androidx.browser.customtabs.customcontentaction.PENDING_INTENT";

    /**
     * Key for the action target type in the Bundle.
     */
    static final String KEY_TARGET_TYPE =
            "androidx.browser.customtabs.customcontentaction.TARGET_TYPE";

    private final int mId;
    private final @NonNull String mLabel;
    private final @NonNull PendingIntent mPendingIntent;
    private final @CustomTabsIntent.ContentTargetType int mTargetType;

    /**
     * Builder class for {@link CustomContentAction} objects.
     */
    public static final class Builder {
        private final int mId;
        private final @NonNull String mLabel;
        private final @NonNull PendingIntent mPendingIntent;
        private final @CustomTabsIntent.ContentTargetType int mTargetType;

        /**
         * Creates a new Builder for {@link CustomContentAction}.
         *
         * @param id            A unique integer ID for this action. This ID will be sent back to
         *                      your
         *                      application when the action is triggered. It should be unique
         *                      among all
         *                      custom content actions added to a single {@link CustomTabsIntent}.
         * @param label         The user-visible label for this action (e.g., "Pin Image",
         *                      "Share Link with App X"). The browser will display this text, but
         *                      may truncate or otherwise alter it to fit its own UI limitations,
         *                      which can vary between browsers.
         * @param pendingIntent The {@link PendingIntent} to be sent when this action is triggered.
         * @param targetType    The type of content this action applies to
         *                      (e.g., {@link CustomTabsIntent#CONTENT_TARGET_TYPE_IMAGE} or
         *                      {@link CustomTabsIntent#CONTENT_TARGET_TYPE_LINK}).
         */
        public Builder(@IntRange(from = 0) int id, @NonNull String label,
                @NonNull PendingIntent pendingIntent,
                @CustomTabsIntent.ContentTargetType int targetType) {
            if (label.isEmpty()) {
                throw new IllegalArgumentException("Label cannot be empty.");
            }
            if (id < 0) {
                throw new IllegalArgumentException("Id cannot be set to negative numbers.");
            }
            if (targetType != CustomTabsIntent.CONTENT_TARGET_TYPE_IMAGE
                    && targetType != CustomTabsIntent.CONTENT_TARGET_TYPE_LINK) {
                throw new IllegalArgumentException("Invalid target type: " + targetType);
            }
            mId = id;
            mLabel = label;
            mPendingIntent = pendingIntent;
            mTargetType = targetType;
        }

        /**
         * Builds the {@link CustomContentAction} instance.
         *
         * @return The built {@link CustomContentAction}.
         */
        public @NonNull CustomContentAction build() {
            return new CustomContentAction(mId, mLabel, mPendingIntent, mTargetType);
        }
    }

    private CustomContentAction(int id, @NonNull String label, @NonNull PendingIntent pendingIntent,
            @CustomTabsIntent.ContentTargetType int targetType) {
        mId = id;
        mLabel = label;
        mPendingIntent = pendingIntent;
        mTargetType = targetType;
    }

    /**
     * @return The unique ID for this action.
     */
    public int getId() {
        return mId;
    }

    /**
     * @return The user-visible label for this action.
     */
    public @NonNull String getLabel() {
        return mLabel;
    }

    /**
     * @return The {@link PendingIntent} to be sent when this action is triggered.
     */
    public @NonNull PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    /**
     * @return The type of content this action applies to.
     */
    public @CustomTabsIntent.ContentTargetType int getTargetType() {
        return mTargetType;
    }

    /**
     * Converts this {@link CustomContentAction} to a {@link Bundle} for serialization.
     */
    @NonNull
    Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_ID, mId);
        bundle.putString(KEY_LABEL, mLabel);
        bundle.putParcelable(KEY_PENDING_INTENT, mPendingIntent);
        bundle.putInt(KEY_TARGET_TYPE, mTargetType);

        return bundle;
    }

    /**
     * Creates a {@link CustomContentAction} from a {@link Bundle}.
     * Returns {@code null} if the bundle is malformed or missing required fields.
     */
    @SuppressWarnings("deprecation")
    static @Nullable CustomContentAction fromBundle(@NonNull Bundle bundle) {
        if (!bundle.containsKey(KEY_ID)) return null;
        int id = bundle.getInt(KEY_ID);

        String label = bundle.getString(KEY_LABEL);
        if (TextUtils.isEmpty(label)) return null;

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingIntent = bundle.getParcelable(KEY_PENDING_INTENT, PendingIntent.class);
        } else {
            pendingIntent = bundle.getParcelable(KEY_PENDING_INTENT);
        }
        if (pendingIntent == null) return null;

        int targetType = bundle.getInt(KEY_TARGET_TYPE, 0); // Default to 0 to catch invalid
        if (targetType != CustomTabsIntent.CONTENT_TARGET_TYPE_IMAGE
                && targetType != CustomTabsIntent.CONTENT_TARGET_TYPE_LINK) {
            return null;
        }

        return new CustomContentAction(id, label, pendingIntent, targetType);
    }
}
