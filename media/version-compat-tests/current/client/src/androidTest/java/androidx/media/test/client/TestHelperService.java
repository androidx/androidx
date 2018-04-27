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

package androidx.media.test.client;

import static androidx.media.test.lib.CommonConstants.ACTION_TEST_HELPER;
import static androidx.media.test.lib.CommonConstants.KEY_ARGUMENTS;
import static androidx.media.test.lib.CommonConstants.KEY_COMMAND;
import static androidx.media.test.lib.CommonConstants.KEY_EXTRAS;
import static androidx.media.test.lib.CommonConstants.KEY_FLAGS;
import static androidx.media.test.lib.CommonConstants.KEY_ITEM_INDEX;
import static androidx.media.test.lib.CommonConstants.KEY_MEDIA_ID;
import static androidx.media.test.lib.CommonConstants.KEY_MEDIA_ITEM;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYLIST;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYLIST_METADATA;
import static androidx.media.test.lib.CommonConstants.KEY_QUERY;
import static androidx.media.test.lib.CommonConstants.KEY_RATING;
import static androidx.media.test.lib.CommonConstants.KEY_REPEAT_MODE;
import static androidx.media.test.lib.CommonConstants.KEY_RESULT_RECEIVER;
import static androidx.media.test.lib.CommonConstants.KEY_ROUTE;
import static androidx.media.test.lib.CommonConstants.KEY_SEEK_POSITION;
import static androidx.media.test.lib.CommonConstants.KEY_SHUFFLE_MODE;
import static androidx.media.test.lib.CommonConstants.KEY_SPEED;
import static androidx.media.test.lib.CommonConstants.KEY_URI;
import static androidx.media.test.lib.CommonConstants.KEY_VOLUME_DIRECTION;
import static androidx.media.test.lib.CommonConstants.KEY_VOLUME_VALUE;
import static androidx.media.test.lib.MediaController2Constants.ADD_PLAYLIST_ITEM;
import static androidx.media.test.lib.MediaController2Constants.ADJUST_VOLUME;
import static androidx.media.test.lib.MediaController2Constants.FAST_FORWARD;
import static androidx.media.test.lib.MediaController2Constants.PAUSE;
import static androidx.media.test.lib.MediaController2Constants.PLAY;
import static androidx.media.test.lib.MediaController2Constants.PLAY_FROM_MEDIA_ID;
import static androidx.media.test.lib.MediaController2Constants.PLAY_FROM_SEARCH;
import static androidx.media.test.lib.MediaController2Constants.PLAY_FROM_URI;
import static androidx.media.test.lib.MediaController2Constants.PREPARE;
import static androidx.media.test.lib.MediaController2Constants.PREPARE_FROM_MEDIA_ID;
import static androidx.media.test.lib.MediaController2Constants.PREPARE_FROM_SEARCH;
import static androidx.media.test.lib.MediaController2Constants.PREPARE_FROM_URI;
import static androidx.media.test.lib.MediaController2Constants.REMOVE_PLAYLIST_ITEM;
import static androidx.media.test.lib.MediaController2Constants.REPLACE_PLAYLIST_ITEM;
import static androidx.media.test.lib.MediaController2Constants.RESET;
import static androidx.media.test.lib.MediaController2Constants.REWIND;
import static androidx.media.test.lib.MediaController2Constants.SEEK_TO;
import static androidx.media.test.lib.MediaController2Constants.SELECT_ROUTE;
import static androidx.media.test.lib.MediaController2Constants.SEND_CUSTOM_COMMAND;
import static androidx.media.test.lib.MediaController2Constants.SET_PLAYBACK_SPEED;
import static androidx.media.test.lib.MediaController2Constants.SET_PLAYLIST;
import static androidx.media.test.lib.MediaController2Constants.SET_RATING;
import static androidx.media.test.lib.MediaController2Constants.SET_REPEAT_MODE;
import static androidx.media.test.lib.MediaController2Constants.SET_SHUFFLE_MODE;
import static androidx.media.test.lib.MediaController2Constants.SET_VOLUME_TO;
import static androidx.media.test.lib.MediaController2Constants.SKIP_TO_NEXT_ITEM;
import static androidx.media.test.lib.MediaController2Constants.SKIP_TO_PLAYLIST_ITEM;
import static androidx.media.test.lib.MediaController2Constants.SKIP_TO_PREVIOUS_ITEM;
import static androidx.media.test.lib.MediaController2Constants.SUBSCRIBE_ROUTES_INFO;
import static androidx.media.test.lib.MediaController2Constants.UNSUBSCRIBE_ROUTES_INFO;
import static androidx.media.test.lib.MediaController2Constants.UPDATE_PLAYLIST_METADATA;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.mediacompat.testlib.IClientAppTestHelperService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media.MediaController2;
import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;
import androidx.media.MediaSession2;
import androidx.media.Rating2;
import androidx.media.SessionCommand2;
import androidx.media.SessionCommandGroup2;
import androidx.media.SessionToken2;
import androidx.media.test.lib.TestUtils.SyncHandler;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * A Service that creates {@link MediaController2} and calls its methods
 * according to the service app's requests.
 */
public class TestHelperService extends Service {

    private static final String TAG = "TestHelperService_clientApp";

    MediaController2 mController2;
    ServiceBinder mBinder;
    SyncHandler mHandler;
    Executor mExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new ServiceBinder();

        mHandler = new SyncHandler(getMainLooper());
        mExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                mHandler.post(command);
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (ACTION_TEST_HELPER.equals(intent.getAction())) {
            return mBinder;
        }
        return null;
    }

    private class ServiceBinder extends IClientAppTestHelperService.Stub {
        @Override
        public void createMediaController2(String testName, Bundle tokenBundle)
                throws RemoteException {
            tokenBundle.setClassLoader(MediaSession2.class.getClassLoader());
            SessionToken2 token = SessionToken2.fromBundle(tokenBundle);

            // TODO: Use different callback according to the test name.
            createController2(token, new TestControllerCallback());
        }

        @Override
        public void callMediaController2Method(int method, @NonNull Bundle args)
                throws RemoteException {
            args.setClassLoader(MediaSession2.class.getClassLoader());

            switch (method) {
                case PLAY: {
                    mController2.play();
                    break;
                } case PAUSE: {
                    mController2.pause();
                    break;
                } case RESET: {
                    mController2.reset();
                    break;
                } case PREPARE: {
                    mController2.prepare();
                    break;
                } case SEEK_TO: {
                    long seekPosition = args.getLong(KEY_SEEK_POSITION);
                    mController2.seekTo(seekPosition);
                    break;
                } case SET_PLAYBACK_SPEED: {
                    float speed = args.getFloat(KEY_SPEED);
                    mController2.setPlaybackSpeed(speed);
                    break;
                } case SET_PLAYLIST: {
                    List<MediaItem2> playlist = MediaTestUtils.fromParcelableList(
                            args.getParcelableArrayList(KEY_PLAYLIST));
                    MediaMetadata2 metadata = MediaMetadata2.fromBundle(
                            args.getBundle(KEY_PLAYLIST_METADATA));
                    mController2.setPlaylist(playlist, metadata);
                    break;
                } case UPDATE_PLAYLIST_METADATA: {
                    MediaMetadata2 metadata = MediaMetadata2.fromBundle(
                            args.getBundle(KEY_PLAYLIST_METADATA));
                    mController2.updatePlaylistMetadata(metadata);
                    break;
                } case ADD_PLAYLIST_ITEM: {
                    int index = args.getInt(KEY_ITEM_INDEX);
                    MediaItem2 item = MediaItem2.fromBundle(args.getBundle(KEY_MEDIA_ITEM));
                    mController2.addPlaylistItem(index, item);
                    break;
                } case REMOVE_PLAYLIST_ITEM: {
                    MediaItem2 item = MediaItem2.fromBundle(args.getBundle(KEY_MEDIA_ITEM));
                    mController2.removePlaylistItem(item);
                    break;
                }  case REPLACE_PLAYLIST_ITEM: {
                    int index = args.getInt(KEY_ITEM_INDEX);
                    MediaItem2 item = MediaItem2.fromBundle(args.getBundle(KEY_MEDIA_ITEM));
                    mController2.replacePlaylistItem(index, item);
                    break;
                } case SKIP_TO_PREVIOUS_ITEM: {
                    mController2.skipToPreviousItem();
                    break;
                } case SKIP_TO_NEXT_ITEM: {
                    mController2.skipToNextItem();
                    break;
                } case SKIP_TO_PLAYLIST_ITEM: {
                    MediaItem2 item = MediaItem2.fromBundle(args.getBundle(KEY_MEDIA_ITEM));
                    mController2.skipToPlaylistItem(item);
                    break;
                } case SET_SHUFFLE_MODE: {
                    int shuffleMode = args.getInt(KEY_SHUFFLE_MODE);
                    mController2.setShuffleMode(shuffleMode);
                    break;
                } case SET_REPEAT_MODE: {
                    int repeatMode = args.getInt(KEY_REPEAT_MODE);
                    mController2.setRepeatMode(repeatMode);
                    break;
                } case SET_VOLUME_TO: {
                    int value = args.getInt(KEY_VOLUME_VALUE);
                    int flags = args.getInt(KEY_FLAGS);
                    mController2.setVolumeTo(value, flags);
                    break;
                } case ADJUST_VOLUME: {
                    int direction = args.getInt(KEY_VOLUME_DIRECTION);
                    int flags = args.getInt(KEY_FLAGS);
                    mController2.adjustVolume(direction, flags);
                    break;
                } case SEND_CUSTOM_COMMAND: {
                    SessionCommand2 command = SessionCommand2.fromBundle(
                            args.getBundle(KEY_COMMAND));
                    Bundle commandArgs = args.getBundle(KEY_ARGUMENTS);
                    ResultReceiver resultReceiver = args.getParcelable(KEY_RESULT_RECEIVER);
                    mController2.sendCustomCommand(command, commandArgs, resultReceiver);
                    break;
                } case FAST_FORWARD: {
                    mController2.fastForward();
                    break;
                } case REWIND: {
                    mController2.rewind();
                    break;
                } case PLAY_FROM_SEARCH: {
                    String query = args.getString(KEY_QUERY);
                    Bundle extras = args.getBundle(KEY_EXTRAS);
                    mController2.playFromSearch(query, extras);
                    break;
                } case PLAY_FROM_URI: {
                    Uri uri = args.getParcelable(KEY_URI);
                    Bundle extras = args.getBundle(KEY_EXTRAS);
                    mController2.playFromUri(uri, extras);
                    break;
                } case PLAY_FROM_MEDIA_ID: {
                    String mediaId = args.getString(KEY_MEDIA_ID);
                    Bundle extras = args.getBundle(KEY_EXTRAS);
                    mController2.playFromMediaId(mediaId, extras);
                    break;
                } case PREPARE_FROM_SEARCH: {
                    String query = args.getString(KEY_QUERY);
                    Bundle extras = args.getBundle(KEY_EXTRAS);
                    mController2.prepareFromSearch(query, extras);
                    break;
                } case PREPARE_FROM_URI: {
                    Uri uri = args.getParcelable(KEY_URI);
                    Bundle extras = args.getBundle(KEY_EXTRAS);
                    mController2.prepareFromUri(uri, extras);
                    break;
                } case PREPARE_FROM_MEDIA_ID: {
                    String mediaId = args.getString(KEY_MEDIA_ID);
                    Bundle extras = args.getBundle(KEY_EXTRAS);
                    mController2.prepareFromMediaId(mediaId, extras);
                    break;
                } case SET_RATING: {
                    String mediaId = args.getString(KEY_MEDIA_ID);
                    Rating2 rating = Rating2.fromBundle(args.getBundle(KEY_RATING));
                    mController2.setRating(mediaId, rating);
                    break;
                } case SUBSCRIBE_ROUTES_INFO: {
                    mController2.subscribeRoutesInfo();
                    break;
                } case UNSUBSCRIBE_ROUTES_INFO: {
                    mController2.unsubscribeRoutesInfo();
                    break;
                } case SELECT_ROUTE: {
                    Bundle route = args.getBundle(KEY_ROUTE);
                    mController2.selectRoute(route);
                    break;
                }
            }
        }

        private void createController2(final SessionToken2 token,
                final TestControllerCallback callback) {
            try {
                mHandler.postAndSync(new Runnable() {
                    @Override
                    public void run() {
                        mController2 = new MediaController2(
                                TestHelperService.this, token, mExecutor, callback);
                    }
                });
            } catch (Exception ex) {
                Log.e(TAG, "Exception occurred while waiting for connection", ex);
            }

            boolean connected = false;
            try {
                connected = callback.mConnectionLatch.await(3000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException occurred while waiting for connection", ex);
            }

            if (!connected) {
                Log.e(TAG, "Could not connect to the given session2.");
            }
        }

        private class TestControllerCallback extends MediaController2.ControllerCallback {
            private CountDownLatch mConnectionLatch = new CountDownLatch(1);

            @Override
            public void onConnected(MediaController2 controller,
                    SessionCommandGroup2 allowedCommands) {
                super.onConnected(controller, allowedCommands);
                mConnectionLatch.countDown();
            }
        }
    }
}
