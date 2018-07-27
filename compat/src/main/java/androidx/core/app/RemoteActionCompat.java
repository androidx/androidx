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

import android.app.PendingIntent;
import android.app.RemoteAction;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Preconditions;

/**
 * Helper for accessing features in {@link android.app.RemoteAction}.
 */
public final class RemoteActionCompat {

    private static final String EXTRA_ICON = "icon";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_CONTENT_DESCRIPTION = "desc";
    private static final String EXTRA_ACTION_INTENT = "action";
    private static final String EXTRA_ENABLED = "enabled";
    private static final String EXTRA_SHOULD_SHOW_ICON = "showicon";

    private final IconCompat mIcon;
    private final CharSequence mTitle;
    private final CharSequence mContentDescription;
    private final PendingIntent mActionIntent;
    private boolean mEnabled;
    private boolean mShouldShowIcon;

    public RemoteActionCompat(@NonNull IconCompat icon, @NonNull CharSequence title,
            @NonNull CharSequence contentDescription, @NonNull PendingIntent intent) {
        if (icon == null || title == null || contentDescription == null || intent == null) {
            throw new IllegalArgumentException(
                    "Expected icon, title, content description and action callback");
        }
        mIcon = icon;
        mTitle = title;
        mContentDescription = contentDescription;
        mActionIntent = intent;
        mEnabled = true;
        mShouldShowIcon = true;
    }

    /**
     * Constructs a Foo builder using data from {@code other}.
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
        if (Build.VERSION.SDK_INT >= 28) {
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
        if (Build.VERSION.SDK_INT >= 28) {
            action.setShouldShowIcon(shouldShowIcon());
        }
        return action;
    }

    /**
     * Adds this Icon to a Bundle that can be read back with the same parameters
     * to {@link #createFromBundle(Bundle)}.
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putBundle(EXTRA_ICON, mIcon.toBundle());
        bundle.putCharSequence(EXTRA_TITLE, mTitle);
        bundle.putCharSequence(EXTRA_CONTENT_DESCRIPTION, mContentDescription);
        bundle.putParcelable(EXTRA_ACTION_INTENT, mActionIntent);
        bundle.putBoolean(EXTRA_ENABLED, mEnabled);
        bundle.putBoolean(EXTRA_SHOULD_SHOW_ICON, mShouldShowIcon);
        return bundle;
    }

    /**
     * Extracts an icon from a bundle that was added using {@link #toBundle()}.
     */
    @Nullable
    public static RemoteActionCompat createFromBundle(@NonNull Bundle bundle) {
        RemoteActionCompat action = new RemoteActionCompat(
                IconCompat.createFromBundle(bundle.getBundle(EXTRA_ICON)),
                bundle.getCharSequence(EXTRA_TITLE),
                bundle.getCharSequence(EXTRA_CONTENT_DESCRIPTION),
                bundle.<PendingIntent>getParcelable(EXTRA_ACTION_INTENT));
        action.setEnabled(bundle.getBoolean(EXTRA_ENABLED));
        action.setShouldShowIcon(bundle.getBoolean(EXTRA_SHOULD_SHOW_ICON));
        return action;
    }
}
