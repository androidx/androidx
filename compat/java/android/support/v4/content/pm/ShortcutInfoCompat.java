/**
 * Copyright (C) 2017 The Android Open Source Project
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
package android.support.v4.content.pm;

import android.support.annotation.RequiresApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * Helper for accessing features in {@link android.content.pm.ShortcutInfo}
 * introduced after API level 25 in a backwards compatible fashion.
 */
public class ShortcutInfoCompat {

    private Context mContext;
    private String mId;

    private Intent[] mIntents;
    private ComponentName mActivity;

    private CharSequence mLabel;
    private CharSequence mLongLabel;
    private CharSequence mDisabledMessage;

    private Bitmap mIconBitmap;
    private int mIconId;

    private ShortcutInfoCompat() { }

    @RequiresApi(25)
    ShortcutInfo toShortcutInfo() {
        ShortcutInfo.Builder builder = new ShortcutInfo.Builder(mContext, mId)
                .setShortLabel(mLabel)
                .setIntents(mIntents);
        if (mIconId != 0) {
            builder.setIcon(Icon.createWithResource(mContext, mIconId));
        } else if (mIconBitmap != null) {
            builder.setIcon(Icon.createWithBitmap(mIconBitmap));
        }
        if (!TextUtils.isEmpty(mLongLabel)) {
            builder.setLongLabel(mLongLabel);
        }
        if (!TextUtils.isEmpty(mDisabledMessage)) {
            builder.setDisabledMessage(mDisabledMessage);
        }
        if (mActivity != null) {
            builder.setActivity(mActivity);
        }
        return builder.build();
    }

    Intent addToIntent(Intent outIntent) {
        outIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, mIntents[mIntents.length - 1])
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, mLabel.toString());
        if (mIconId != 0) {
            outIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(mContext, mIconId));
        }
        if (mIconBitmap != null) {
            outIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, mIconBitmap);
        }
        return outIntent;
    }

    /**
     * Builder class for {@link ShortcutInfoCompat} objects.
     */
    public static class Builder {

        private final ShortcutInfoCompat mInfo;

        public Builder(@NonNull Context context, @NonNull String id) {
            mInfo = new ShortcutInfoCompat();
            mInfo.mContext = context;
            mInfo.mId = id;
        }

        /**
         * Sets the short title of a shortcut.
         *
         * <p>This is a mandatory field when publishing a new shortcut.
         *
         * <p>This field is intended to be a concise description of a shortcut.
         *
         * <p>The recommended maximum length is 10 characters.
         */
        @NonNull
        public Builder setShortLabel(@NonNull CharSequence shortLabel) {
            mInfo.mLabel = shortLabel;
            return this;
        }

        /**
         * Sets the text of a shortcut.
         *
         * <p>This field is intended to be more descriptive than the shortcut title. The launcher
         * shows this instead of the short title when it has enough space.
         *
         * <p>The recommend maximum length is 25 characters.
         */
        @NonNull
        public Builder setLongLabel(@NonNull CharSequence longLabel) {
            mInfo.mLongLabel = longLabel;
            return this;
        }

        /**
         * Sets the message that should be shown when the user attempts to start a shortcut that
         * is disabled.
         *
         * @see ShortcutInfo#getDisabledMessage()
         */
        @NonNull
        public Builder setDisabledMessage(@NonNull CharSequence disabledMessage) {
            mInfo.mDisabledMessage = disabledMessage;
            return this;
        }

        /**
         * Sets the intent of a shortcut.  Alternatively, {@link #setIntents(Intent[])} can be used
         * to launch an activity with other activities in the back stack.
         *
         * <p>This is a mandatory field when publishing a new shortcut.
         *
         * <p>The given {@code intent} can contain extras, but these extras must contain values
         * of primitive types in order for the system to persist these values.
         */
        @NonNull
        public Builder setIntent(@NonNull Intent intent) {
            return setIntents(new Intent[]{intent});
        }

        /**
         * Sets multiple intents instead of a single intent, in order to launch an activity with
         * other activities in back stack.  Use {@link android.app.TaskStackBuilder} to build
         * intents. The last element in the list represents the only intent that doesn't place
         * an activity on the back stack.
         */
        @NonNull
        public Builder setIntents(@NonNull Intent[] intents) {
            mInfo.mIntents = intents;
            return this;
        }

        /**
         * Sets an icon of a shortcut.
         */
        @NonNull
        public Builder setIcon(@NonNull Bitmap icon) {
            mInfo.mIconBitmap = icon;
            return this;
        }

        /**
         * Sets an icon of a shortcut.
         */
        @NonNull
        public Builder setIcon(@DrawableRes int icon) {
            mInfo.mIconId = icon;
            return this;
        }

        /**
         * Sets the target activity. A shortcut will be shown along with this activity's icon
         * on the launcher.
         *
         * @see ShortcutInfo#getActivity()
         * @see android.content.pm.ShortcutInfo.Builder#setActivity(ComponentName)
         */
        @NonNull
        public Builder setActivity(@NonNull ComponentName activity) {
            mInfo.mActivity = activity;
            return this;
        }

        /**
         * Sets an icon of a shortcut.
         */
        @NonNull
        public ShortcutInfoCompat build() {
            // Verify the arguments
            if (TextUtils.isEmpty(mInfo.mLabel)) {
                throw new IllegalArgumentException("Shortcut much have a non-empty label");
            }
            if (mInfo.mIntents == null || mInfo.mIntents.length == 0) {
                throw new IllegalArgumentException("Shortcut much have an intent");
            }
            return mInfo;
        }
    }
}
