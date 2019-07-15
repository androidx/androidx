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

package androidx.browser.customtabs;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;
import android.support.customtabs.ICustomTabsService;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.browser.customtabs.CustomTabsService.Relation;
import androidx.browser.customtabs.CustomTabsService.Result;

import java.util.List;

/**
 * A class to be used for Custom Tabs related communication. Clients that want to launch Custom Tabs
 * can use this class exclusively to handle all related communication.
 */
public final class CustomTabsSession {
    private static final String TAG = "CustomTabsSession";
    private final Object mLock = new Object();
    private final ICustomTabsService mService;
    private final ICustomTabsCallback mCallback;
    private final ComponentName mComponentName;

    /**
     * The session ID is represented by {@link PendingIntent}. Other apps cannot
     * forge {@link PendingIntent}. The {@link PendingIntent#equals(Object)} method
     * considers two {@link PendingIntent} objects equal if their action, data, type,
     * class and category are the same (even across a process being killed).
     *
     * {@see Intent#filterEquals()}
     */
    private final PendingIntent mId;

    /**
     * Provides browsers a way to generate a mock {@link CustomTabsSession} for testing
     * purposes.
     *
     * @param componentName The component the session should be created for.
     * @return A mock session with no functionality.
     */
    @VisibleForTesting
    @NonNull
    public static CustomTabsSession createMockSessionForTesting(
            @NonNull ComponentName componentName) {
        return new CustomTabsSession(
                null, new CustomTabsSessionToken.MockCallback(), componentName, null);
    }

    /* package */ CustomTabsSession(
            ICustomTabsService service, ICustomTabsCallback callback, ComponentName componentName,
            @Nullable PendingIntent sessionId) {
        mService = service;
        mCallback = callback;
        mComponentName = componentName;
        mId = sessionId;
    }

    /**
     * Tells the browser of a likely future navigation to a URL.
     * The most likely URL has to be specified first. Optionally, a list of
     * other likely URLs can be provided. They are treated as less likely than
     * the first one, and have to be sorted in decreasing priority order. These
     * additional URLs may be ignored.
     * All previous calls to this method will be deprioritized.
     *
     * @param url                Most likely URL.
     * @param extras             Reserved for future use.
     * @param otherLikelyBundles Other likely destinations, sorted in decreasing
     *                           likelihood order. Inside each Bundle, the client should provide a
     *                           {@link Uri} using {@link CustomTabsService#KEY_URL} with
     *                           {@link Bundle#putParcelable(String, android.os.Parcelable)}.
     * @return                   true for success.
     */
    public boolean mayLaunchUrl(Uri url, Bundle extras, List<Bundle> otherLikelyBundles) {
        addIdToBundle(extras);
        try {
            return mService.mayLaunchUrl(mCallback, url, extras, otherLikelyBundles);
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * This sets the action button on the toolbar with ID
     * {@link CustomTabsIntent#TOOLBAR_ACTION_BUTTON_ID}.
     *
     * @param icon          The new icon of the action button.
     * @param description   Content description of the action button.
     *
     * @see CustomTabsSession#setToolbarItem(int, Bitmap, String)
     */
    public boolean setActionButton(@NonNull Bitmap icon, @NonNull String description) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(CustomTabsIntent.KEY_ICON, icon);
        bundle.putString(CustomTabsIntent.KEY_DESCRIPTION, description);

        Bundle metaBundle = new Bundle();
        metaBundle.putBundle(CustomTabsIntent.EXTRA_ACTION_BUTTON_BUNDLE, bundle);
        addIdToBundle(bundle);
        try {
            return mService.updateVisuals(mCallback, metaBundle);
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Updates the {@link RemoteViews} of the secondary toolbar in an existing custom tab session.
     * @param remoteViews   The updated {@link RemoteViews} that will be shown in secondary toolbar.
     *                      If null, the current secondary toolbar will be dismissed.
     * @param clickableIDs  The ids of clickable views. The onClick event of these views will be
     *                      handled by custom tabs.
     * @param pendingIntent The {@link PendingIntent} that will be sent when the user clicks on one
     *                      of the {@link View}s in clickableIDs.
     */
    public boolean setSecondaryToolbarViews(@Nullable RemoteViews remoteViews,
            @Nullable int[] clickableIDs, @Nullable PendingIntent pendingIntent) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(CustomTabsIntent.EXTRA_REMOTEVIEWS, remoteViews);
        bundle.putIntArray(CustomTabsIntent.EXTRA_REMOTEVIEWS_VIEW_IDS, clickableIDs);
        bundle.putParcelable(CustomTabsIntent.EXTRA_REMOTEVIEWS_PENDINGINTENT, pendingIntent);
        addIdToBundle(bundle);
        try {
            return mService.updateVisuals(mCallback, bundle);
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Updates the visuals for toolbar items. Will only succeed if a custom tab created using this
     * session is in the foreground in browser and the given id is valid.
     * @param id            The id for the item to update.
     * @param icon          The new icon of the toolbar item.
     * @param description   Content description of the toolbar item.
     * @return              Whether the update succeeded.
     * @deprecated Use
     * CustomTabsSession#setSecondaryToolbarViews(RemoteViews, int[], PendingIntent)
     */
    @Deprecated
    public boolean setToolbarItem(int id, @NonNull Bitmap icon, @NonNull String description) {
        Bundle bundle = new Bundle();
        bundle.putInt(CustomTabsIntent.KEY_ID, id);
        bundle.putParcelable(CustomTabsIntent.KEY_ICON, icon);
        bundle.putString(CustomTabsIntent.KEY_DESCRIPTION, description);

        Bundle metaBundle = new Bundle();
        metaBundle.putBundle(CustomTabsIntent.EXTRA_ACTION_BUTTON_BUNDLE, bundle);
        addIdToBundle(metaBundle);
        try {
            return mService.updateVisuals(mCallback, metaBundle);
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Sends a request to create a two way postMessage channel between the client and the browser.
     *
     * @param postMessageOrigin      A origin that the client is requesting to be identified as
     *                               during the postMessage communication.
     * @return Whether the implementation accepted the request. Note that returning true
     *         here doesn't mean an origin has already been assigned as the validation is
     *         asynchronous.
     */
    public boolean requestPostMessageChannel(Uri postMessageOrigin) {
        Bundle extras = new Bundle();
        addIdToBundle(extras);
        try {
            return mService.requestPostMessageChannelWithExtras(
                    mCallback, postMessageOrigin, extras);
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Sends a postMessage request using the origin communicated via
     * {@link CustomTabsService#requestPostMessageChannel(
     * CustomTabsSessionToken, Uri)}. Fails when called before
     * {@link PostMessageServiceConnection#notifyMessageChannelReady(Bundle)} is received on
     * the client side.
     *
     * @param message The message that is being sent.
     * @param extras Reserved for future use.
     * @return An integer constant about the postMessage request result. Will return
     *        {@link CustomTabsService#RESULT_SUCCESS} if successful.
     */
    @Result
    public int postMessage(String message, Bundle extras) {
        addIdToBundle(extras);
        synchronized (mLock) {
            try {
                return mService.postMessage(mCallback, message, extras);
            } catch (RemoteException e) {
                return CustomTabsService.RESULT_FAILURE_REMOTE_ERROR;
            }
        }
    }

    /**
     * Requests to validate a relationship between the application and an origin.
     *
     * <p>
     * See <a href="https://developers.google.com/digital-asset-links/v1/getting-started">here</a>
     * for documentation about Digital Asset Links. This methods requests the browser to verify
     * a relation with the calling application, to grant the associated rights.
     *
     * <p>
     * If this method returns {@code true}, the validation result will be provided through
     * {@link CustomTabsCallback#onRelationshipValidationResult(int, Uri, boolean, Bundle)}.
     * Otherwise the request didn't succeed. The client must call
     * {@link CustomTabsClient#warmup(long)} before this.
     *
     * @param relation Relation to check, must be one of the {@code CustomTabsService#RELATION_* }
     *                 constants.
     * @param origin Origin.
     * @param extras Reserved for future use.
     * @return {@code true} if the request has been submitted successfully.
     */
    public boolean validateRelationship(@Relation int relation, @NonNull Uri origin,
            @Nullable Bundle extras) {
        if (relation < CustomTabsService.RELATION_USE_AS_ORIGIN
                || relation > CustomTabsService.RELATION_HANDLE_ALL_URLS) {
            return false;
        }
        if (extras == null) {
            extras = new Bundle();
        }
        addIdToBundle(extras);
        try {
            return mService.validateRelationship(mCallback, relation, origin, extras);
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Passes an URI of a file, e.g. in order to pass a large bitmap to be displayed in the
     * Custom Tabs provider.
     *
     * Prior to calling this method, the client needs to grant a read permission to the target
     * Custom Tabs provider via {@link android.content.Context#grantUriPermission}.
     *
     * The file is read and processed (where applicable) synchronously, therefore it's recommended
     * to call this method on a background thread.
     *
     * @param uri {@link Uri} of the file.
     * @param purpose Purpose of transferring this file, one of the constants enumerated in
     *                {@code CustomTabsService#FilePurpose}.
     * @param extras Reserved for future use.
     * @return {@code true} if the file was received successfully.
     */
    public boolean receiveFile(@NonNull Uri uri, @CustomTabsService.FilePurpose int purpose,
            @Nullable Bundle extras) {
        if (extras == null) {
            extras = new Bundle();
        }
        addIdToBundle(extras);
        try {
            return mService.receiveFile(mCallback, uri, purpose, extras);
        } catch (RemoteException e) {
            return false;
        }
    }

    private void addIdToBundle(Bundle bundle) {
        if (mId != null) bundle.putParcelable(CustomTabsIntent.EXTRA_SESSION_ID, mId);
    }

    /* package */ IBinder getBinder() {
        return mCallback.asBinder();
    }

    /* package */ ComponentName getComponentName() {
        return mComponentName;
    }

    /* package */ PendingIntent getId() {
        return mId;
    }

    /**
     * A class to be used instead of {@link CustomTabsSession} before we are connected
     * {@link CustomTabsService}.
     *
     * Use {@link CustomTabsClient#attachSession(PendingSession)} to get {@link CustomTabsSession}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class PendingSession {
        private final CustomTabsCallback mCallback;
        private final PendingIntent mId;

        /* package */ PendingSession(
                CustomTabsCallback callback, PendingIntent sessionId) {
            mCallback = callback;
            mId = sessionId;
        }

        /* package */ PendingIntent getId() {
            return mId;
        }

        /* package */ CustomTabsCallback getCallback() {
            return mCallback;
        }
    }
}
