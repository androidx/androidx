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

import android.app.PendingIntent;
import android.app.RemoteAction;
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
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @ParcelField(1)
    public IconCompat mIcon;
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @ParcelField(2)
    public CharSequence mTitle;
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @ParcelField(3)
    public CharSequence mContentDescription;
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @ParcelField(4)
    public PendingIntent mActionIntent;
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @ParcelField(5)
    public boolean mEnabled;
    /**
     * @hide
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
     * @hide
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
        RemoteActionCompat action = new RemoteActionCompat(
                IconCompat.createFromIcon(remoteAction.getIcon()), remoteAction.getTitle(),
                remoteAction.getContentDescription(), remoteAction.getActionIntent());
        action.setEnabled(remoteAction.isEnabled());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            action.setShouldShowIcon(remoteAction.shouldShowIcon());
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
     */
    @RequiresApi(26)
    @NonNull
    public RemoteAction toRemoteAction() {
        RemoteAction action = new RemoteAction(mIcon.toIcon(), mTitle, mContentDescription,
                mActionIntent);
        action.setEnabled(isEnabled());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            action.setShouldShowIcon(shouldShowIcon());
        }
        return action;
    }
}
