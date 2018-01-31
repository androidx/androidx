/*
 * Copyright 2017 The Android Open Source Project
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

package android.support.mediacompat.service;


import static android.support.mediacompat.testlib.MediaBrowserConstants.CUSTOM_ACTION_SEND_ERROR;
import static android.support.mediacompat.testlib.MediaBrowserConstants
        .CUSTOM_ACTION_SEND_PROGRESS_UPDATE;
import static android.support.mediacompat.testlib.MediaBrowserConstants.CUSTOM_ACTION_SEND_RESULT;
import static android.support.mediacompat.testlib.MediaBrowserConstants.NOTIFY_CHILDREN_CHANGED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SEND_DELAYED_ITEM_LOADED;
import static android.support.mediacompat.testlib.MediaBrowserConstants
        .SEND_DELAYED_NOTIFY_CHILDREN_CHANGED;
import static android.support.mediacompat.testlib.MediaBrowserConstants.SET_SESSION_TOKEN;
import static android.support.mediacompat.testlib.MediaSessionConstants.RELEASE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SEND_SESSION_EVENT;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_ACTIVE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_CAPTIONING_ENABLED;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_EXTRAS;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_FLAGS;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_METADATA;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_PLAYBACK_STATE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_PLAYBACK_TO_LOCAL;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_PLAYBACK_TO_REMOTE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_QUEUE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_QUEUE_TITLE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_RATING_TYPE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_REPEAT_MODE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_SESSION_ACTIVITY;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_SHUFFLE_MODE;
import static android.support.mediacompat.testlib.util.IntentUtil
        .ACTION_CALL_MEDIA_BROWSER_SERVICE_METHOD;
import static android.support.mediacompat.testlib.util.IntentUtil.ACTION_CALL_MEDIA_SESSION_METHOD;
import static android.support.mediacompat.testlib.util.IntentUtil.KEY_ARGUMENT;
import static android.support.mediacompat.testlib.util.IntentUtil.KEY_METHOD_ID;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.ParcelableVolumeInfo;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.List;

public class ServiceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (ACTION_CALL_MEDIA_BROWSER_SERVICE_METHOD.equals(intent.getAction()) && extras != null) {
            StubMediaBrowserServiceCompat service = StubMediaBrowserServiceCompat.sInstance;
            int method = extras.getInt(KEY_METHOD_ID, 0);

            switch (method) {
                case NOTIFY_CHILDREN_CHANGED:
                    service.notifyChildrenChanged(extras.getString(KEY_ARGUMENT));
                    break;
                case SEND_DELAYED_NOTIFY_CHILDREN_CHANGED:
                    service.sendDelayedNotifyChildrenChanged();
                    break;
                case SEND_DELAYED_ITEM_LOADED:
                    service.sendDelayedItemLoaded();
                    break;
                case CUSTOM_ACTION_SEND_PROGRESS_UPDATE:
                    service.mCustomActionResult.sendProgressUpdate(extras.getBundle(KEY_ARGUMENT));
                    break;
                case CUSTOM_ACTION_SEND_ERROR:
                    service.mCustomActionResult.sendError(extras.getBundle(KEY_ARGUMENT));
                    break;
                case CUSTOM_ACTION_SEND_RESULT:
                    service.mCustomActionResult.sendResult(extras.getBundle(KEY_ARGUMENT));
                    break;
                case SET_SESSION_TOKEN:
                    StubMediaBrowserServiceCompatWithDelayedMediaSession.sInstance
                            .callSetSessionToken();
                    break;
            }
        } else if (ACTION_CALL_MEDIA_SESSION_METHOD.equals(intent.getAction()) && extras != null) {
            MediaSessionCompat session = StubMediaBrowserServiceCompat.sSession;
            int method = extras.getInt(KEY_METHOD_ID, 0);

            switch (method) {
                case SET_EXTRAS:
                    session.setExtras(extras.getBundle(KEY_ARGUMENT));
                    break;
                case SET_FLAGS:
                    session.setFlags(extras.getInt(KEY_ARGUMENT));
                    break;
                case SET_METADATA:
                    session.setMetadata((MediaMetadataCompat) extras.getParcelable(KEY_ARGUMENT));
                    break;
                case SET_PLAYBACK_STATE:
                    session.setPlaybackState(
                            (PlaybackStateCompat) extras.getParcelable(KEY_ARGUMENT));
                    break;
                case SET_QUEUE:
                    List<QueueItem> items = extras.getParcelableArrayList(KEY_ARGUMENT);
                    session.setQueue(items);
                    break;
                case SET_QUEUE_TITLE:
                    session.setQueueTitle(extras.getCharSequence(KEY_ARGUMENT));
                    break;
                case SET_SESSION_ACTIVITY:
                    session.setSessionActivity((PendingIntent) extras.getParcelable(KEY_ARGUMENT));
                    break;
                case SET_CAPTIONING_ENABLED:
                    session.setCaptioningEnabled(extras.getBoolean(KEY_ARGUMENT));
                    break;
                case SET_REPEAT_MODE:
                    session.setRepeatMode(extras.getInt(KEY_ARGUMENT));
                    break;
                case SET_SHUFFLE_MODE:
                    session.setShuffleMode(extras.getInt(KEY_ARGUMENT));
                    break;
                case SEND_SESSION_EVENT:
                    Bundle arguments = extras.getBundle(KEY_ARGUMENT);
                    session.sendSessionEvent(
                            arguments.getString("event"), arguments.getBundle("extras"));
                    break;
                case SET_ACTIVE:
                    session.setActive(extras.getBoolean(KEY_ARGUMENT));
                    break;
                case RELEASE:
                    session.release();
                    break;
                case SET_PLAYBACK_TO_LOCAL:
                    session.setPlaybackToLocal(extras.getInt(KEY_ARGUMENT));
                    break;
                case SET_PLAYBACK_TO_REMOTE:
                    ParcelableVolumeInfo volumeInfo = extras.getParcelable(KEY_ARGUMENT);
                    session.setPlaybackToRemote(new VolumeProviderCompat(
                            volumeInfo.controlType,
                            volumeInfo.maxVolume,
                            volumeInfo.currentVolume) {});
                    break;
                case SET_RATING_TYPE:
                    session.setRatingType(RatingCompat.RATING_5_STARS);
                    break;
            }
        }
    }
}
