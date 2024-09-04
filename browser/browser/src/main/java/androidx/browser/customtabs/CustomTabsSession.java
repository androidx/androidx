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
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;
import android.support.customtabs.ICustomTabsService;
import android.support.customtabs.IEngagementSignalsCallback;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.VisibleForTesting;
import androidx.browser.customtabs.CustomTabsService.Relation;
import androidx.browser.customtabs.CustomTabsService.Result;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * A class to be used for Custom Tabs related communication. Clients that want to launch Custom Tabs
 * can use this class exclusively to handle all related communication.
 */
public final class CustomTabsSession {
    private static final String TAG = "CustomTabsSession";
    static final String TARGET_ORIGIN_KEY = "target_origin";
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
    @Nullable
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
                new MockSession(), new CustomTabsSessionToken.MockCallback(), componentName, null);
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
     * @param url                Most likely URL, may be {@code null} if {@code otherLikelyBundles}
     *                           is provided.
     * @param extras             Reserved for future use.
     * @param otherLikelyBundles Other likely destinations, sorted in decreasing
     *                           likelihood order. Inside each Bundle, the client should provide a
     *                           {@link Uri} using {@link CustomTabsService#KEY_URL} with
     *                           {@link Bundle#putParcelable(String, android.os.Parcelable)}.
     * @return true for success.
     */
    @SuppressWarnings("NullAway")  // TODO: b/142938599
    public boolean mayLaunchUrl(@Nullable Uri url, @Nullable Bundle extras,
            @Nullable List<Bundle> otherLikelyBundles) {
        extras = createBundleWithId(extras);
        try {
            return mService.mayLaunchUrl(mCallback, url, extras, otherLikelyBundles);
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Request the browser to start navigational prefetch to the page that will be used for future
     * navigations.
     *
     * @param url     The url to be prefetched for upcoming navigations.
     * @param options The option used for prefetch request. Please see
     *                {@link PrefetchOptions}.
     */
    @ExperimentalPrefetch
    @SuppressWarnings("NullAway")  // TODO: b/142938599
    public void prefetch(@NonNull Uri url, @NonNull PrefetchOptions options) {
        Bundle optionsWithId = createBundleWithId(options.toBundle());
        try {
            mService.prefetch(mCallback, url, optionsWithId);
        } catch (RemoteException e) {
            return;
        }
    }

    /**
     * Request the browser to start navigational prefetch to the pages that will be used for future
     * navigations.
     *
     * @param urls     The urls to be prefetched for upcoming navigations.
     * @param options The option used for prefetch request. Please see
     *                {@link PrefetchOptions}.
     */
    @ExperimentalPrefetch
    @SuppressWarnings("NullAway")  // TODO: b/142938599
    public void prefetch(@NonNull List<Uri> urls, @NonNull PrefetchOptions options) {
        Bundle optionsWithId = createBundleWithId(options.toBundle());
        try {
            for (Uri uri : urls) {
                mService.prefetch(mCallback, uri, optionsWithId);
            }
        } catch (RemoteException e) {
            return;
        }
    }

    /**
     * This sets the action button on the toolbar with ID
     * {@link CustomTabsIntent#TOOLBAR_ACTION_BUTTON_ID}.
     *
     * @param icon        The new icon of the action button.
     * @param description Content description of the action button.
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
     *
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
     * Sets a {@link PendingIntent} object to be sent when the user swipes up from the secondary
     * (bottom) toolbar.
     *
     * @param pendingIntent {@link PendingIntent} to send.
     * @return Whether the update succeeded.
     */
    public boolean setSecondaryToolbarSwipeUpGesture(@Nullable PendingIntent pendingIntent) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(CustomTabsIntent.EXTRA_SECONDARY_TOOLBAR_SWIPE_UP_GESTURE,
                pendingIntent);
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
     *
     * @param id          The id for the item to update.
     * @param icon        The new icon of the toolbar item.
     * @param description Content description of the toolbar item.
     * @return Whether the update succeeded.
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
     * @param postMessageOrigin A origin that the client is requesting to be identified as
     *                          during the postMessage communication.
     * @return Whether the implementation accepted the request. Note that returning true
     * here doesn't mean an origin has already been assigned as the validation is
     * asynchronous.
     */
    public boolean requestPostMessageChannel(@NonNull Uri postMessageOrigin) {
        return requestPostMessageChannel(postMessageOrigin, null, new Bundle());
    }

    /**
     * Sends a request to create a two way postMessage channel between the client and the browser
     * with specifying the target origin to communicate with.
     *
     * @param postMessageOrigin       A origin that the client is requesting to be identified as
     *                                during the postMessage communication.
     * @param postMessageTargetOrigin The target Origin to establish the postMessage communication
     *                                with.
     * @param extras  Reserved for future use.
     * @return Whether the implementation accepted the request. Note that returning true
     * here doesn't mean an origin has already been assigned as the validation is
     * asynchronous.
     */
    public boolean requestPostMessageChannel(@NonNull Uri postMessageOrigin,
            @Nullable Uri postMessageTargetOrigin, @NonNull Bundle extras) {
        try {
            Bundle targetOriginWithIdBundle =
                    createPostMessageExtraBundle(postMessageTargetOrigin);
            if (targetOriginWithIdBundle != null) {
                extras.putAll(targetOriginWithIdBundle);
                return mService.requestPostMessageChannelWithExtras(
                        mCallback, postMessageOrigin, extras);
            } else {
                return mService.requestPostMessageChannel(mCallback, postMessageOrigin);
            }
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Sends a postMessage request using the origin communicated via
     * {@link CustomTabsService#requestPostMessageChannel(
     *CustomTabsSessionToken, Uri)}. Fails when called before
     * {@link PostMessageServiceConnection#notifyMessageChannelReady(Bundle)} is received on
     * the client side.
     *
     * @param message The message that is being sent.
     * @param extras  Reserved for future use.
     * @return An integer constant about the postMessage request result. Will return
     * {@link CustomTabsService#RESULT_SUCCESS} if successful.
     */
    @Result
    public int postMessage(@NonNull String message, @Nullable Bundle extras) {
        extras = createBundleWithId(extras);
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
     * @param origin   Origin.
     * @param extras   Reserved for future use.
     * @return {@code true} if the request has been submitted successfully.
     */
    public boolean validateRelationship(@Relation int relation, @NonNull Uri origin,
            @Nullable Bundle extras) {
        if (relation < CustomTabsService.RELATION_USE_AS_ORIGIN
                || relation > CustomTabsService.RELATION_HANDLE_ALL_URLS) {
            return false;
        }
        extras = createBundleWithId(extras);
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
     * @param uri     {@link Uri} of the file.
     * @param purpose Purpose of transferring this file, one of the constants enumerated in
     *                {@code CustomTabsService#FilePurpose}.
     * @param extras  Reserved for future use.
     * @return {@code true} if the file was received successfully.
     */
    public boolean receiveFile(@NonNull Uri uri, @CustomTabsService.FilePurpose int purpose,
            @Nullable Bundle extras) {
        extras = createBundleWithId(extras);
        try {
            return mService.receiveFile(mCallback, uri, purpose, extras);
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Returns whether the Engagement Signals API is available. The availability of the Engagement
     * Signals API may change at runtime. If an {@link EngagementSignalsCallback} has been set, an
     * {@link EngagementSignalsCallback#onSessionEnded} signal will be sent if the API becomes
     * unavailable later.
     *
     * @param extras Reserved for future use.
     * @return Whether the Engagement Signals API is available. A false value means
     *         {@link #setEngagementSignalsCallback} will return false and not set the callback.
     * @throws RemoteException If the Service dies while responding to the request.
     * @throws UnsupportedOperationException If this method isn't supported by the Custom Tabs
     *                                       implementation.
     */
    public boolean isEngagementSignalsApiAvailable(@NonNull Bundle extras) throws RemoteException {
        Bundle extrasWithId = createBundleWithId(extras);
        try {
            return mService.isEngagementSignalsApiAvailable(mCallback, extrasWithId);
        } catch (SecurityException e) {
            throw new UnsupportedOperationException("This method isn't supported by the "
                    + "Custom Tabs implementation.", e);
        }
    }

    /**
     * Sets an {@link EngagementSignalsCallback} to receive callbacks for events related to the
     * user's engagement with webpage within the tab.
     *
     * Note that the callback will be executed on the main thread using
     * {@link Looper#getMainLooper()}. To specify the execution thread, use
     * {@link #setEngagementSignalsCallback(Executor, EngagementSignalsCallback, Bundle)}.
     *
     * @param callback The {@link EngagementSignalsCallback} to receive the user engagement signals.
     * @param extras   Reserved for future use.
     * @return Whether the callback connection is allowed. If false, no callbacks will be called for
     * this session.
     * @throws RemoteException               If the Service dies while responding to the request.
     * @throws UnsupportedOperationException If this method isn't supported by the Custom Tabs
     *                                       implementation.
     */
    @RequiresFeature(name = CustomTabsFeatures.ENGAGEMENT_SIGNALS, enforcement =
            "androidx.browser.customtabs.CustomTabsSession#isEngagementSignalsApiAvailable")
    public boolean setEngagementSignalsCallback(@NonNull EngagementSignalsCallback callback,
            @NonNull Bundle extras) throws RemoteException {
        Bundle extrasWithId = createBundleWithId(extras);
        IEngagementSignalsCallback wrapper = createEngagementSignalsCallbackWrapper(callback);
        try {
            return mService.setEngagementSignalsCallback(mCallback, wrapper.asBinder(),
                    extrasWithId);
        } catch (SecurityException e) {
            throw new UnsupportedOperationException("This method isn't supported by the "
                    + "Custom Tabs implementation.", e);
        }
    }

    private IEngagementSignalsCallback.Stub createEngagementSignalsCallbackWrapper(
            @NonNull final EngagementSignalsCallback callback) {
        return new IEngagementSignalsCallback.Stub() {
            private final Handler mHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onVerticalScrollEvent(boolean isDirectionUp, Bundle extras) {
                mHandler.post(() -> callback.onVerticalScrollEvent(isDirectionUp, extras));
            }

            @Override
            public void onGreatestScrollPercentageIncreased(int scrollPercentage, Bundle extras) {
                mHandler.post(() -> callback.onGreatestScrollPercentageIncreased(
                        scrollPercentage, extras));
            }

            @Override
            public void onSessionEnded(boolean didUserInteract, Bundle extras) {
                mHandler.post(() -> callback.onSessionEnded(didUserInteract, extras));
            }
        };
    }

    /**
     * Sets an {@link EngagementSignalsCallback} to receive callbacks for events related to the
     * user's engagement with webpage within the tab.
     *
     * @param executor The {@link Executor} to be used to execute the callbacks.
     * @param callback The {@link EngagementSignalsCallback} to receive the user engagement signals.
     * @param extras   Reserved for future use.
     * @return Whether the callback connection is allowed. If false, no callbacks will be called for
     * this session.
     * @throws RemoteException               If the Service dies while responding to the request.
     * @throws UnsupportedOperationException If this method isn't supported by the Custom Tabs
     *                                       implementation.
     */
    @RequiresFeature(name = CustomTabsFeatures.ENGAGEMENT_SIGNALS, enforcement =
            "androidx.browser.customtabs.CustomTabsSession#isEngagementSignalsApiAvailable")
    public boolean setEngagementSignalsCallback(@NonNull Executor executor,
            @NonNull EngagementSignalsCallback callback,
            @NonNull Bundle extras) throws RemoteException {
        Bundle extrasWithId = createBundleWithId(extras);
        IEngagementSignalsCallback wrapper =
                createEngagementSignalsCallbackWrapper(callback, executor);
        try {
            return mService.setEngagementSignalsCallback(mCallback, wrapper.asBinder(),
                    extrasWithId);
        } catch (SecurityException e) {
            throw new UnsupportedOperationException("This method isn't supported by the "
                    + "Custom Tabs implementation.", e);
        }
    }

    private IEngagementSignalsCallback.Stub createEngagementSignalsCallbackWrapper(
            @NonNull final EngagementSignalsCallback callback, @NonNull Executor executor) {
        return new IEngagementSignalsCallback.Stub() {
            private final Executor mExecutor = executor;

            @Override
            public void onVerticalScrollEvent(boolean isDirectionUp, Bundle extras) {
                long identity = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() -> callback.onVerticalScrollEvent(isDirectionUp, extras));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            @Override
            public void onGreatestScrollPercentageIncreased(int scrollPercentage, Bundle extras) {
                long identity = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() -> callback.onGreatestScrollPercentageIncreased(
                            scrollPercentage, extras));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            @Override
            public void onSessionEnded(boolean didUserInteract, Bundle extras) {
                long identity = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() -> callback.onSessionEnded(didUserInteract, extras));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        };
    }

    private @Nullable Bundle createPostMessageExtraBundle(@Nullable Uri targetOrigin) {
        Bundle toReturn = new Bundle();
        if (targetOrigin != null) {
            toReturn.putParcelable(CustomTabsSession.TARGET_ORIGIN_KEY, targetOrigin);
        }
        // If mId is not null we know that the CustomTabsService supports
        // requestPostMessageChannelWithExtras. That is because non-null mId means that
        // CustomTabsSession was created with CustomTabsClient#newSession(Callback int), which
        // can succeed only when browsers supporting CustomTabsService#newSessionWithExtras.
        // This was added at the same time as requestPostMessageChannelWithExtras.
        if (mId != null) {
            addIdToBundle(toReturn);
        }
        return toReturn.isEmpty() ? null : toReturn;
    }

    private Bundle createBundleWithId(@Nullable Bundle bundle) {
        Bundle bundleWithId = new Bundle();
        if (bundle != null) bundleWithId.putAll(bundle);
        addIdToBundle(bundleWithId);
        return bundleWithId;
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

    @Nullable
        /* package */ PendingIntent getId() {
        return mId;
    }

    /**
     * A class to be used instead of {@link CustomTabsSession} when a Custom Tab is launched before
     * a Service connection is established.
     *
     * Use {@link CustomTabsClient#attachSession(PendingSession)} to get {@link CustomTabsSession}.
     */
    @ExperimentalPendingSession
    public static class PendingSession {
        @Nullable
        private final CustomTabsCallback mCallback;
        @Nullable
        private final PendingIntent mId;

        /* package */ PendingSession(
                @Nullable CustomTabsCallback callback, @Nullable PendingIntent sessionId) {
            mCallback = callback;
            mId = sessionId;
        }

        @Nullable
            /* package */ PendingIntent getId() {
            return mId;
        }

        @Nullable
            /* package */ CustomTabsCallback getCallback() {
            return mCallback;
        }
    }

    // For use in testing only.
    static class MockSession extends ICustomTabsService.Stub {
        @Override
        public boolean warmup(long flags) throws RemoteException {
            return false;
        }

        @Override
        public boolean newSession(ICustomTabsCallback callback) throws RemoteException {
            return false;
        }

        @Override
        public boolean newSessionWithExtras(ICustomTabsCallback callback, Bundle extras)
                throws RemoteException {
            return false;
        }

        @Override
        public boolean mayLaunchUrl(ICustomTabsCallback callback, Uri url, Bundle extras,
                List<Bundle> otherLikelyBundles) throws RemoteException {
            return false;
        }

        @Override
        @ExperimentalPrefetch
        public void prefetch(ICustomTabsCallback callback, Uri url, Bundle options)
                throws RemoteException {
        }

        @SuppressWarnings("NullAway")  // TODO: b/142938599
        @Override
        public Bundle extraCommand(String commandName, Bundle args) throws RemoteException {
            return null;
        }

        @Override
        public boolean updateVisuals(ICustomTabsCallback callback, Bundle bundle)
                throws RemoteException {
            return false;
        }

        @Override
        public boolean requestPostMessageChannel(ICustomTabsCallback callback,
                Uri postMessageOrigin) throws RemoteException {
            return false;
        }

        @Override
        public boolean requestPostMessageChannelWithExtras(ICustomTabsCallback callback,
                Uri postMessageOrigin, Bundle extras) throws RemoteException {
            return false;
        }

        @Override
        public int postMessage(ICustomTabsCallback callback, String message, Bundle extras)
                throws RemoteException {
            return 0;
        }

        @Override
        public boolean validateRelationship(ICustomTabsCallback callback, int relation, Uri origin,
                Bundle extras) throws RemoteException {
            return false;
        }

        @Override
        public boolean receiveFile(ICustomTabsCallback callback, Uri uri, int purpose,
                Bundle extras) throws RemoteException {
            return false;
        }

        @Override
        public boolean isEngagementSignalsApiAvailable(ICustomTabsCallback customTabsCallback,
                Bundle extras) throws RemoteException {
            return false;
        }

        @Override
        public boolean setEngagementSignalsCallback(ICustomTabsCallback customTabsCallback,
                IBinder callback, Bundle extras) throws RemoteException {
            return false;
        }
    }
}
