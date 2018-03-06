/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v4.media.session;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.PlaybackStateCompat.MediaKeyAction;
import android.util.Log;
import android.view.KeyEvent;

import java.util.List;

/**
 * A media button receiver receives and helps translate hardware media playback buttons, such as
 * those found on wired and wireless headsets, into the appropriate callbacks in your app.
 * <p />
 * You can add this MediaButtonReceiver to your app by adding it directly to your
 * AndroidManifest.xml:
 * <pre>
 * &lt;receiver android:name="android.support.v4.media.session.MediaButtonReceiver" &gt;
 *   &lt;intent-filter&gt;
 *     &lt;action android:name="android.intent.action.MEDIA_BUTTON" /&gt;
 *   &lt;/intent-filter&gt;
 * &lt;/receiver&gt;
 * </pre>
 *
 * This class assumes you have a {@link Service} in your app that controls media playback via a
 * {@link MediaSessionCompat}. Once a key event is received by MediaButtonReceiver, this class tries
 * to find a {@link Service} that can handle {@link Intent#ACTION_MEDIA_BUTTON}, and a
 * {@link MediaBrowserServiceCompat} in turn. If an appropriate service is found, this class
 * forwards the key event to the service. If neither is available or more than one valid
 * service/media browser service is found, an {@link IllegalStateException} will be thrown. Thus,
 * your app should have one of the following services to get a key event properly.
 * <p />
 *
 * <h4>Service Handling ACTION_MEDIA_BUTTON</h4>
 * A service can receive a key event by including an intent filter that handles
 * {@link Intent#ACTION_MEDIA_BUTTON}:
 * <pre>
 * &lt;service android:name="com.example.android.MediaPlaybackService" &gt;
 *   &lt;intent-filter&gt;
 *     &lt;action android:name="android.intent.action.MEDIA_BUTTON" /&gt;
 *   &lt;/intent-filter&gt;
 * &lt;/service&gt;
 * </pre>
 *
 * Events can then be handled in {@link Service#onStartCommand(Intent, int, int)} by calling
 * {@link MediaButtonReceiver#handleIntent(MediaSessionCompat, Intent)}, passing in your current
 * {@link MediaSessionCompat}:
 * <pre>
 * private MediaSessionCompat mMediaSessionCompat = ...;
 *
 * public int onStartCommand(Intent intent, int flags, int startId) {
 *   MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
 *   return super.onStartCommand(intent, flags, startId);
 * }
 * </pre>
 *
 * This ensures that the correct callbacks to {@link MediaSessionCompat.Callback} will be triggered
 * based on the incoming {@link KeyEvent}.
 * <p class="note"><strong>Note:</strong> Once the service is started, it must start to run in the
 * foreground.</p>
 *
 * <h4>MediaBrowserService</h4>
 * If you already have a {@link MediaBrowserServiceCompat} in your app, MediaButtonReceiver will
 * deliver the received key events to the {@link MediaBrowserServiceCompat} by default. You can
 * handle them in your {@link MediaSessionCompat.Callback}.
 */
public class MediaButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaButtonReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || !Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())
                || !intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
            Log.d(TAG, "Ignore unsupported intent: " + intent);
            return;
        }
        ComponentName mediaButtonServiceComponentName =
                getServiceComponentByAction(context, Intent.ACTION_MEDIA_BUTTON);
        if (mediaButtonServiceComponentName != null) {
            intent.setComponent(mediaButtonServiceComponentName);
            startForegroundService(context, intent);
            return;
        }
        ComponentName mediaBrowserServiceComponentName = getServiceComponentByAction(context,
                MediaBrowserServiceCompat.SERVICE_INTERFACE);
        if (mediaBrowserServiceComponentName != null) {
            PendingResult pendingResult = goAsync();
            Context applicationContext = context.getApplicationContext();
            MediaButtonConnectionCallback connectionCallback =
                    new MediaButtonConnectionCallback(applicationContext, intent, pendingResult);
            MediaBrowserCompat mediaBrowser = new MediaBrowserCompat(applicationContext,
                    mediaBrowserServiceComponentName, connectionCallback, null);
            connectionCallback.setMediaBrowser(mediaBrowser);
            mediaBrowser.connect();
            return;
        }
        throw new IllegalStateException("Could not find any Service that handles "
                + Intent.ACTION_MEDIA_BUTTON + " or implements a media browser service.");
    }

    private static class MediaButtonConnectionCallback extends
            MediaBrowserCompat.ConnectionCallback {
        private final Context mContext;
        private final Intent mIntent;
        private final PendingResult mPendingResult;

        private MediaBrowserCompat mMediaBrowser;

        MediaButtonConnectionCallback(Context context, Intent intent, PendingResult pendingResult) {
            mContext = context;
            mIntent = intent;
            mPendingResult = pendingResult;
        }

        void setMediaBrowser(MediaBrowserCompat mediaBrowser) {
            mMediaBrowser = mediaBrowser;
        }

        @Override
        public void onConnected() {
            try {
                MediaControllerCompat mediaController = new MediaControllerCompat(mContext,
                        mMediaBrowser.getSessionToken());
                KeyEvent ke = mIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                mediaController.dispatchMediaButtonEvent(ke);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to create a media controller", e);
            }
            finish();
        }

        @Override
        public void onConnectionSuspended() {
            finish();
        }

        @Override
        public void onConnectionFailed() {
            finish();
        }

        private void finish() {
            mMediaBrowser.disconnect();
            mPendingResult.finish();
        }
    };

    /**
     * Extracts any available {@link KeyEvent} from an {@link Intent#ACTION_MEDIA_BUTTON}
     * intent, passing it onto the {@link MediaSessionCompat} using
     * {@link MediaControllerCompat#dispatchMediaButtonEvent(KeyEvent)}, which in turn
     * will trigger callbacks to the {@link MediaSessionCompat.Callback} registered via
     * {@link MediaSessionCompat#setCallback(MediaSessionCompat.Callback)}.
     * @param mediaSessionCompat A {@link MediaSessionCompat} that has a
     *            {@link MediaSessionCompat.Callback} set.
     * @param intent The intent to parse.
     * @return The extracted {@link KeyEvent} if found, or null.
     */
    public static KeyEvent handleIntent(MediaSessionCompat mediaSessionCompat, Intent intent) {
        if (mediaSessionCompat == null || intent == null
                || !Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())
                || !intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
            return null;
        }
        KeyEvent ke = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        MediaControllerCompat mediaController = mediaSessionCompat.getController();
        mediaController.dispatchMediaButtonEvent(ke);
        return ke;
    }

    /**
     * Creates a broadcast pending intent that will send a media button event. The {@code action}
     * will be translated to the appropriate {@link KeyEvent}, and it will be sent to the
     * registered media button receiver in the given context. The {@code action} should be one of
     * the following:
     * <ul>
     * <li>{@link PlaybackStateCompat#ACTION_PLAY}</li>
     * <li>{@link PlaybackStateCompat#ACTION_PAUSE}</li>
     * <li>{@link PlaybackStateCompat#ACTION_SKIP_TO_NEXT}</li>
     * <li>{@link PlaybackStateCompat#ACTION_SKIP_TO_PREVIOUS}</li>
     * <li>{@link PlaybackStateCompat#ACTION_STOP}</li>
     * <li>{@link PlaybackStateCompat#ACTION_FAST_FORWARD}</li>
     * <li>{@link PlaybackStateCompat#ACTION_REWIND}</li>
     * <li>{@link PlaybackStateCompat#ACTION_PLAY_PAUSE}</li>
     * </ul>
     *
     * @param context The context of the application.
     * @param action The action to be sent via the pending intent.
     * @return Created pending intent, or null if cannot find a unique registered media button
     *         receiver or if the {@code action} is unsupported/invalid.
     */
    public static PendingIntent buildMediaButtonPendingIntent(Context context,
            @MediaKeyAction long action) {
        ComponentName mbrComponent = getMediaButtonReceiverComponent(context);
        if (mbrComponent == null) {
            Log.w(TAG, "A unique media button receiver could not be found in the given context, so "
                    + "couldn't build a pending intent.");
            return null;
        }
        return buildMediaButtonPendingIntent(context, mbrComponent, action);
    }

    /**
     * Creates a broadcast pending intent that will send a media button event. The {@code action}
     * will be translated to the appropriate {@link KeyEvent}, and sent to the provided media
     * button receiver via the pending intent. The {@code action} should be one of the following:
     * <ul>
     * <li>{@link PlaybackStateCompat#ACTION_PLAY}</li>
     * <li>{@link PlaybackStateCompat#ACTION_PAUSE}</li>
     * <li>{@link PlaybackStateCompat#ACTION_SKIP_TO_NEXT}</li>
     * <li>{@link PlaybackStateCompat#ACTION_SKIP_TO_PREVIOUS}</li>
     * <li>{@link PlaybackStateCompat#ACTION_STOP}</li>
     * <li>{@link PlaybackStateCompat#ACTION_FAST_FORWARD}</li>
     * <li>{@link PlaybackStateCompat#ACTION_REWIND}</li>
     * <li>{@link PlaybackStateCompat#ACTION_PLAY_PAUSE}</li>
     * </ul>
     *
     * @param context The context of the application.
     * @param mbrComponent The full component name of a media button receiver where you want to send
     *            this intent.
     * @param action The action to be sent via the pending intent.
     * @return Created pending intent, or null if the given component name is null or the
     *         {@code action} is unsupported/invalid.
     */
    public static PendingIntent buildMediaButtonPendingIntent(Context context,
            ComponentName mbrComponent, @MediaKeyAction long action) {
        if (mbrComponent == null) {
            Log.w(TAG, "The component name of media button receiver should be provided.");
            return null;
        }
        int keyCode = PlaybackStateCompat.toKeyCode(action);
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            Log.w(TAG,
                    "Cannot build a media button pending intent with the given action: " + action);
            return null;
        }
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setComponent(mbrComponent);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        return PendingIntent.getBroadcast(context, keyCode, intent, 0);
    }

    static ComponentName getMediaButtonReceiverComponent(Context context) {
        Intent queryIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        queryIntent.setPackage(context.getPackageName());
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryBroadcastReceivers(queryIntent, 0);
        if (resolveInfos.size() == 1) {
            ResolveInfo resolveInfo = resolveInfos.get(0);
            return new ComponentName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);
        } else if (resolveInfos.size() > 1) {
            Log.w(TAG, "More than one BroadcastReceiver that handles "
                    + Intent.ACTION_MEDIA_BUTTON + " was found, returning null.");
        }
        return null;
    }

    private static void startForegroundService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private static ComponentName getServiceComponentByAction(Context context, String action) {
        PackageManager pm = context.getPackageManager();
        Intent queryIntent = new Intent(action);
        queryIntent.setPackage(context.getPackageName());
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(queryIntent, 0 /* flags */);
        if (resolveInfos.size() == 1) {
            ResolveInfo resolveInfo = resolveInfos.get(0);
            return new ComponentName(resolveInfo.serviceInfo.packageName,
                    resolveInfo.serviceInfo.name);
        } else if (resolveInfos.isEmpty()) {
            return null;
        } else {
            throw new IllegalStateException("Expected 1 service that handles " + action + ", found "
                    + resolveInfos.size());
        }
    }
}
