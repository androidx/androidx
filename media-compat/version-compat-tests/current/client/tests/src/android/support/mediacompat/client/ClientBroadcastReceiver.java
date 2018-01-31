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

package android.support.mediacompat.client;

import static android.support.mediacompat.testlib.MediaControllerConstants.ADD_QUEUE_ITEM;
import static android.support.mediacompat.testlib.MediaControllerConstants
        .ADD_QUEUE_ITEM_WITH_INDEX;
import static android.support.mediacompat.testlib.MediaControllerConstants.ADJUST_VOLUME;
import static android.support.mediacompat.testlib.MediaControllerConstants.FAST_FORWARD;
import static android.support.mediacompat.testlib.MediaControllerConstants.PAUSE;
import static android.support.mediacompat.testlib.MediaControllerConstants.PLAY;
import static android.support.mediacompat.testlib.MediaControllerConstants.PLAY_FROM_MEDIA_ID;
import static android.support.mediacompat.testlib.MediaControllerConstants.PLAY_FROM_SEARCH;
import static android.support.mediacompat.testlib.MediaControllerConstants.PLAY_FROM_URI;
import static android.support.mediacompat.testlib.MediaControllerConstants.PREPARE;
import static android.support.mediacompat.testlib.MediaControllerConstants.PREPARE_FROM_MEDIA_ID;
import static android.support.mediacompat.testlib.MediaControllerConstants.PREPARE_FROM_SEARCH;
import static android.support.mediacompat.testlib.MediaControllerConstants.PREPARE_FROM_URI;
import static android.support.mediacompat.testlib.MediaControllerConstants.REMOVE_QUEUE_ITEM;
import static android.support.mediacompat.testlib.MediaControllerConstants.REWIND;
import static android.support.mediacompat.testlib.MediaControllerConstants.SEEK_TO;
import static android.support.mediacompat.testlib.MediaControllerConstants.SEND_COMMAND;
import static android.support.mediacompat.testlib.MediaControllerConstants.SEND_CUSTOM_ACTION;
import static android.support.mediacompat.testlib.MediaControllerConstants
        .SEND_CUSTOM_ACTION_PARCELABLE;
import static android.support.mediacompat.testlib.MediaControllerConstants.SET_CAPTIONING_ENABLED;
import static android.support.mediacompat.testlib.MediaControllerConstants.SET_RATING;
import static android.support.mediacompat.testlib.MediaControllerConstants.SET_REPEAT_MODE;
import static android.support.mediacompat.testlib.MediaControllerConstants.SET_SHUFFLE_MODE;
import static android.support.mediacompat.testlib.MediaControllerConstants.SET_VOLUME_TO;
import static android.support.mediacompat.testlib.MediaControllerConstants.SKIP_TO_NEXT;
import static android.support.mediacompat.testlib.MediaControllerConstants.SKIP_TO_PREVIOUS;
import static android.support.mediacompat.testlib.MediaControllerConstants.SKIP_TO_QUEUE_ITEM;
import static android.support.mediacompat.testlib.MediaControllerConstants.STOP;
import static android.support.mediacompat.testlib.util.IntentUtil
        .ACTION_CALL_MEDIA_CONTROLLER_METHOD;
import static android.support.mediacompat.testlib.util.IntentUtil
        .ACTION_CALL_TRANSPORT_CONTROLS_METHOD;
import static android.support.mediacompat.testlib.util.IntentUtil.KEY_ARGUMENT;
import static android.support.mediacompat.testlib.util.IntentUtil.KEY_METHOD_ID;
import static android.support.mediacompat.testlib.util.IntentUtil.KEY_SESSION_TOKEN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.TransportControls;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public class ClientBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        MediaControllerCompat controller;
        try {
            controller = new MediaControllerCompat(context,
                    (MediaSessionCompat.Token) extras.getParcelable(KEY_SESSION_TOKEN));
        } catch (RemoteException ex) {
            // Do nothing.
            return;
        }
        int method = extras.getInt(KEY_METHOD_ID, 0);

        if (ACTION_CALL_MEDIA_CONTROLLER_METHOD.equals(intent.getAction()) && extras != null) {
            Bundle arguments;
            switch (method) {
                case SEND_COMMAND:
                    arguments = extras.getBundle(KEY_ARGUMENT);
                    controller.sendCommand(
                            arguments.getString("command"),
                            arguments.getBundle("extras"),
                            new ResultReceiver(null));
                    break;
                case ADD_QUEUE_ITEM:
                    controller.addQueueItem(
                            (MediaDescriptionCompat) extras.getParcelable(KEY_ARGUMENT));
                    break;
                case ADD_QUEUE_ITEM_WITH_INDEX:
                    arguments = extras.getBundle(KEY_ARGUMENT);
                    controller.addQueueItem(
                            (MediaDescriptionCompat) arguments.getParcelable("description"),
                            arguments.getInt("index"));
                    break;
                case REMOVE_QUEUE_ITEM:
                    controller.removeQueueItem(
                            (MediaDescriptionCompat) extras.getParcelable(KEY_ARGUMENT));
                    break;
                case SET_VOLUME_TO:
                    controller.setVolumeTo(extras.getInt(KEY_ARGUMENT), 0);
                    break;
                case ADJUST_VOLUME:
                    controller.adjustVolume(extras.getInt(KEY_ARGUMENT), 0);
                    break;
            }
        } else if (ACTION_CALL_TRANSPORT_CONTROLS_METHOD.equals(intent.getAction())
                && extras != null) {
            TransportControls controls = controller.getTransportControls();
            Bundle arguments;
            switch (method) {
                case PLAY:
                    controls.play();
                    break;
                case PAUSE:
                    controls.pause();
                    break;
                case STOP:
                    controls.stop();
                    break;
                case FAST_FORWARD:
                    controls.fastForward();
                    break;
                case REWIND:
                    controls.rewind();
                    break;
                case SKIP_TO_PREVIOUS:
                    controls.skipToPrevious();
                    break;
                case SKIP_TO_NEXT:
                    controls.skipToNext();
                    break;
                case SEEK_TO:
                    controls.seekTo(extras.getLong(KEY_ARGUMENT));
                    break;
                case SET_RATING:
                    controls.setRating((RatingCompat) extras.getParcelable(KEY_ARGUMENT));
                    break;
                case PLAY_FROM_MEDIA_ID:
                    arguments = extras.getBundle(KEY_ARGUMENT);
                    controls.playFromMediaId(
                            arguments.getString("mediaId"),
                            arguments.getBundle("extras"));
                    break;
                case PLAY_FROM_SEARCH:
                    arguments = extras.getBundle(KEY_ARGUMENT);
                    controls.playFromSearch(
                            arguments.getString("query"),
                            arguments.getBundle("extras"));
                    break;
                case PLAY_FROM_URI:
                    arguments = extras.getBundle(KEY_ARGUMENT);
                    controls.playFromUri(
                            (Uri) arguments.getParcelable("uri"),
                            arguments.getBundle("extras"));
                    break;
                case SEND_CUSTOM_ACTION:
                    arguments = extras.getBundle(KEY_ARGUMENT);
                    controls.sendCustomAction(
                            arguments.getString("action"),
                            arguments.getBundle("extras"));
                    break;
                case SEND_CUSTOM_ACTION_PARCELABLE:
                    arguments = extras.getBundle(KEY_ARGUMENT);
                    controls.sendCustomAction(
                            (PlaybackStateCompat.CustomAction)
                                    arguments.getParcelable("action"),
                            arguments.getBundle("extras"));
                    break;
                case SKIP_TO_QUEUE_ITEM:
                    controls.skipToQueueItem(extras.getLong(KEY_ARGUMENT));
                    break;
                case PREPARE:
                    controls.prepare();
                    break;
                case PREPARE_FROM_MEDIA_ID:
                    arguments = extras.getBundle(KEY_ARGUMENT);
                    controls.prepareFromMediaId(
                            arguments.getString("mediaId"),
                            arguments.getBundle("extras"));
                    break;
                case PREPARE_FROM_SEARCH:
                    arguments = extras.getBundle(KEY_ARGUMENT);
                    controls.prepareFromSearch(
                            arguments.getString("query"),
                            arguments.getBundle("extras"));
                    break;
                case PREPARE_FROM_URI:
                    arguments = extras.getBundle(KEY_ARGUMENT);
                    controls.prepareFromUri(
                            (Uri) arguments.getParcelable("uri"),
                            arguments.getBundle("extras"));
                    break;
                case SET_CAPTIONING_ENABLED:
                    controls.setCaptioningEnabled(extras.getBoolean(KEY_ARGUMENT));
                    break;
                case SET_REPEAT_MODE:
                    controls.setRepeatMode(extras.getInt(KEY_ARGUMENT));
                    break;
                case SET_SHUFFLE_MODE:
                    controls.setShuffleMode(extras.getInt(KEY_ARGUMENT));
                    break;
            }
        }
    }
}
