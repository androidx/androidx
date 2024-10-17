/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Represents a remote action that can be called from another process.  The action can have an
 * associated visualization including metadata like an icon or title.
 * <p>
 * This is a backward-compatible version of {@link RemoteAction}.
 */
@VersionedParcelize(jetifyAs = "android.support.v4.app.RemoteActionCompat")
public final class RemoteActionCompat implements VersionedParcelable {
    /**
     */
    @SuppressWarnings("NotNullFieldNotInitialized") // VersionedParceleble inits this field.
    @NonNull
    @RestrictTo(LIBRARY_GROUP)
    @ParcelField(1)
    public IconCompat mIcon;
    /**
     */
    @SuppressWarnings("NotNullFieldNotInitialized") // VersionedParceleble inits this field.
    @NonNull
    @RestrictTo(LIBRARY_GROUP)
    @ParcelField(2)
    public CharSequence mTitle;
    /**
     */
    @SuppressWarnings("NotNullFieldNotInitialized") // VersionedParceleble inits this field.
    @NonNull
    @RestrictTo(LIBRARY_GROUP)
    @ParcelField(3)
    public CharSequence mContentDescription;
    /**
     */
    @SuppressWarnings("NotNullFieldNotInitialized") // VersionedParceleble inits this field.
    @NonNull
    @RestrictTo(LIBRARY_GROUP)
    @ParcelField(4)
    public PendingIntent mActionIntent;
    /**
     */
    @RestrictTo(LIBRARY_GROUP)
    @ParcelField(5)
    public boolean mEnabled;
    /**
     */
    @RestrictTo(LIBRARY_GROUP)
    @ParcelField(6)
    public boolean mShouldShowIcon;

    public RemoteActionCompat(@NonNull IconCompat icon, @NonNull CharSequence title,
            @NonNull CharSequence contentDescription, @NonNull PendingIntent intent) {
        mIcon = Preconditions.checkNotNull(icon);
        mTitle = Preconditions.checkNotNull(title);
        mContentDescription = Preconditions.checkNotNull(contentDescription);
        mActionIntent = Preconditions.checkNotNull(intent);
        mEnabled = true;
        mShouldShowIcon = true;
    }

    /**
     * Used for VersionedParcelable.
     */
    @RestrictTo(LIBRARY_GROUP)
    public RemoteActionCompat() {}

    /**
     * Constructs a {@link RemoteActionCompat} using data from {@code other}.
     */
    public RemoteActionCompat(@NonNull RemoteActionCompat other) {
        Preconditions.checkNotNull(other);
        mIcon = other.mIcon;
        mTitle = other.mTitle;
        mContentDescription = other.mContentDescription;
        mActionIntent = other.mActionIntent;
        mEnabled = other.mEnabled;
        mShouldShowIcon = other.mShouldShowIcon;
    }

    /**
     * Creates an RemoteActionCompat from a RemoteAction.
     */
    @RequiresApi(26)
    @NonNull
    public static RemoteActionCompat createFromRemoteAction(@NonNull RemoteAction remoteAction) {
        Preconditions.checkNotNull(remoteAction);
        RemoteActionCompat action = new RemoteActionCompat(IconCompat.createFromIcon(
                Api26Impl.getIcon(remoteAction)),
                Api26Impl.getTitle(remoteAction),
                Api26Impl.getContentDescription(remoteAction),
                Api26Impl.getActionIntent(remoteAction));
        action.setEnabled(Api26Impl.isEnabled(remoteAction));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            action.setShouldShowIcon(Api28Impl.shouldShowIcon(remoteAction));
        }
        return action;
    }

    /**
     * Sets whether this action is enabled.
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * Return whether this action is enabled.
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Sets whether the icon should be shown.
     */
    public void setShouldShowIcon(boolean shouldShowIcon) {
        mShouldShowIcon = shouldShowIcon;
    }

    /**
     * Return whether the icon should be shown.
     */
    @SuppressLint("KotlinPropertyAccess")
    public boolean shouldShowIcon() {
        return mShouldShowIcon;
    }

    /**
     * Return an icon representing the action.
     */
    public @NonNull IconCompat getIcon() {
        return mIcon;
    }

    /**
     * Return an title representing the action.
     */
    public @NonNull CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Return a content description representing the action.
     */
    public @NonNull CharSequence getContentDescription() {
        return mContentDescription;
    }

    /**
     * Return the action intent.
     */
    public @NonNull PendingIntent getActionIntent() {
        return mActionIntent;
    }

    /**
     * Convert this compat object to {@link RemoteAction} object.
     *
     * @return {@link RemoteAction} object
     *
     * @deprecated Use {@link #toRemoteAction(Context)} instead.
     */
    @RequiresApi(26)
    @NonNull
    @Deprecated
    public RemoteAction toRemoteAction() {
        //noinspection DataFlowIssue
        return toRemoteAction(null);
    }

    /**
     * Convert this compat object to {@link RemoteAction} object.
     *
     * @param context A {@link Context} that will be used to get icon from mIcon.
     * @return {@link RemoteAction} object
     */
    @RequiresApi(26)
    @NonNull
    public RemoteAction toRemoteAction(@NonNull Context context) {
        RemoteAction action = Api26Impl.createRemoteAction(mIcon.toIcon(context), mTitle,
                mContentDescription, mActionIntent);
        Api26Impl.setEnabled(action, isEnabled());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Api28Impl.setShouldShowIcon(action, shouldShowIcon());
        }
        return action;
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        static boolean shouldShowIcon(RemoteAction remoteAction) {
            return remoteAction.shouldShowIcon();
        }

        static void setShouldShowIcon(RemoteAction remoteAction, boolean shouldShowIcon) {
            remoteAction.setShouldShowIcon(shouldShowIcon);
        }
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }

        static CharSequence getContentDescription(RemoteAction remoteAction) {
            return remoteAction.getContentDescription();
        }

        static PendingIntent getActionIntent(RemoteAction remoteAction) {
            return remoteAction.getActionIntent();
        }

        static CharSequence getTitle(RemoteAction remoteAction) {
            return remoteAction.getTitle();
        }

        static Icon getIcon(RemoteAction remoteAction) {
            return remoteAction.getIcon();
        }

        static boolean isEnabled(RemoteAction remoteAction) {
            return remoteAction.isEnabled();
        }

        static RemoteAction createRemoteAction(Icon icon, CharSequence title,
                CharSequence contentDescription, PendingIntent intent) {
            return new RemoteAction(icon, title, contentDescription, intent);
        }

        static void setEnabled(RemoteAction remoteAction, boolean enabled) {
            remoteAction.setEnabled(enabled);
        }
    }
}
