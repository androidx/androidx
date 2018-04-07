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
package androidx.core.content.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.drawable.IconCompat;

import java.util.Arrays;

/**
 * Helper for accessing features in {@link ShortcutInfo}.
 */
public class ShortcutInfoCompat {

    Context mContext;
    String mId;

    Intent[] mIntents;
    ComponentName mActivity;

    CharSequence mLabel;
    CharSequence mLongLabel;
    CharSequence mDisabledMessage;

    IconCompat mIcon;
    boolean mIsAlwaysBadged;

    private ShortcutInfoCompat() { }

    /**
     * @return {@link ShortcutInfo} object from this compat object.
     */
    @RequiresApi(25)
    public ShortcutInfo toShortcutInfo() {
        ShortcutInfo.Builder builder = new ShortcutInfo.Builder(mContext, mId)
                .setShortLabel(mLabel)
                .setIntents(mIntents);
        if (mIcon != null) {
            builder.setIcon(mIcon.toIcon());
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
        if (mIcon != null) {
            Drawable badge = null;
            if (mIsAlwaysBadged) {
                PackageManager pm = mContext.getPackageManager();
                if (mActivity != null) {
                    try {
                        badge = pm.getActivityIcon(mActivity);
                    } catch (PackageManager.NameNotFoundException e) {
                        // Ignore
                    }
                }
                if (badge == null) {
                    badge = mContext.getApplicationInfo().loadIcon(pm);
                }
            }
            mIcon.addToShortcutIntent(outIntent, badge, mContext);
        }
        return outIntent;
    }

    /**
     * Returns the ID of a shortcut.
     *
     * <p>Shortcut IDs are unique within each publisher app and must be stable across
     * devices so that shortcuts will still be valid when restored on a different device.
     * See {@link android.content.pm.ShortcutManager} for details.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Return the target activity.
     *
     * <p>This has nothing to do with the activity that this shortcut will launch.
     * Launcher apps should show the launcher icon for the returned activity alongside
     * this shortcut.
     *
     * @see Builder#setActivity(ComponentName)
     */
    @Nullable
    public ComponentName getActivity() {
        return mActivity;
    }

    /**
     * Return the short description of a shortcut.
     *
     * @see Builder#setShortLabel(CharSequence)
     */
    @NonNull
    public CharSequence getShortLabel() {
        return mLabel;
    }

    /**
     * Return the long description of a shortcut.
     *
     * @see Builder#setLongLabel(CharSequence)
     */
    @Nullable
    public CharSequence getLongLabel() {
        return mLongLabel;
    }

    /**
     * Return the message that should be shown when the user attempts to start a shortcut
     * that is disabled.
     *
     * @see Builder#setDisabledMessage(CharSequence)
     */
    @Nullable
    public CharSequence getDisabledMessage() {
        return mDisabledMessage;
    }

    /**
     * Returns the intent that is executed when the user selects this shortcut.
     * If setIntents() was used, then return the last intent in the array.
     *
     * @see Builder#setIntent(Intent)
     */
    @NonNull
    public Intent getIntent() {
        return mIntents[mIntents.length - 1];
    }

    /**
     * Return the intent set with {@link Builder#setIntents(Intent[])}.
     *
     * @see Builder#setIntents(Intent[])
     */
    @NonNull
    public Intent[] getIntents() {
        return Arrays.copyOf(mIntents, mIntents.length);
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
        public Builder setIcon(IconCompat icon) {
            mInfo.mIcon = icon;
            return this;
        }

        /**
         * Sets the target activity. A shortcut will be shown along with this activity's icon
         * on the launcher.
         *
         * @see ShortcutInfo#getActivity()
         * @see ShortcutInfo.Builder#setActivity(ComponentName)
         */
        @NonNull
        public Builder setActivity(@NonNull ComponentName activity) {
            mInfo.mActivity = activity;
            return this;
        }

        /**
         * Badges the icon before passing it over to the Launcher.
         * <p>
         * Launcher automatically badges {@link ShortcutInfo}, so only the legacy shortcut icon,
         * {@link Intent.ShortcutIconResource} is badged. This field is ignored when using
         * {@link ShortcutInfo} on API 25 and above.
         * <p>
         * If the shortcut is associated with an activity, the activity icon is used as the badge,
         * otherwise application icon is used.
         *
         * @see #setActivity(ComponentName)
         */
        public Builder setAlwaysBadged() {
            mInfo.mIsAlwaysBadged = true;
            return this;
        }

        /**
         * Creates a {@link ShortcutInfoCompat} instance.
         */
        @NonNull
        public ShortcutInfoCompat build() {
            // Verify the arguments
            if (TextUtils.isEmpty(mInfo.mLabel)) {
                throw new IllegalArgumentException("Shortcut must have a non-empty label");
            }
            if (mInfo.mIntents == null || mInfo.mIntents.length == 0) {
                throw new IllegalArgumentException("Shortcut must have an intent");
            }
            return mInfo;
        }
    }
}
