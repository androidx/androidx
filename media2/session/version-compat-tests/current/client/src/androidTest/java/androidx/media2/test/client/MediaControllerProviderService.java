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

package androidx.media2.test.client;

import static androidx.media2.test.common.CommonConstants.ACTION_MEDIA2_CONTROLLER;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.media2.common.MediaMetadata;
import androidx.media2.common.MediaParcelUtils;
import androidx.media2.common.Rating;
import androidx.media2.session.MediaBrowser;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.session.SessionToken;
import androidx.media2.test.common.IRemoteMediaController;
import androidx.media2.test.common.TestUtils;
import androidx.media2.test.common.TestUtils.SyncHandler;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * A Service that creates {@link MediaController} and calls its methods
 * according to the service app's requests.
 */
public class MediaControllerProviderService extends Service {
    private static final String TAG = "MediaControllerProviderService";

    Map<String, MediaController> mMediaControllerMap = new HashMap<>();
    RemoteMediaControllerStub mBinder;

    SyncHandler mHandler;
    Executor mExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new RemoteMediaControllerStub();

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
        if (ACTION_MEDIA2_CONTROLLER.equals(intent.getAction())) {
            return mBinder;
        }
        return null;
    }

    @Override
    public void onDestroy() {
        for (MediaController controller : mMediaControllerMap.values()) {
            controller.close();
        }
    }

    private class RemoteMediaControllerStub extends IRemoteMediaController.Stub {
        @Override
        public void create(final boolean isBrowser, final String controllerId,
                ParcelImpl tokenParcelable, final Bundle connectionHints, boolean waitForConnection)
                throws RemoteException {
            final SessionToken token = MediaParcelUtils.fromParcelable(tokenParcelable);
            final TestControllerCallback callback = new TestControllerCallback();

            try {
                mHandler.postAndSync(new Runnable() {
                    @Override
                    public void run() {
                        Context context = MediaControllerProviderService.this;
                        MediaController controller;
                        if (isBrowser) {
                            MediaBrowser.Builder builder = new MediaBrowser.Builder(context)
                                    .setSessionToken(token)
                                    .setControllerCallback(mExecutor, callback);
                            if (connectionHints != null) {
                                builder.setConnectionHints(connectionHints);
                            }
                            controller = builder.build();
                        } else {
                            MediaController.Builder builder = new MediaController.Builder(context)
                                    .setSessionToken(token)
                                    .setControllerCallback(mExecutor, callback);
                            if (connectionHints != null) {
                                builder.setConnectionHints(connectionHints);
                            }
                            controller = builder.build();

                        }
                        mMediaControllerMap.put(controllerId, controller);
                    }
                });
            } catch (Exception ex) {
                Log.e(TAG, "Exception occurred while creating MediaController.", ex);
            }

            if (!waitForConnection) {
                return;
            }

            boolean connected = false;
            try {
                connected = callback.mConnectionLatch.await(3000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException occurred while waiting for connection", ex);
            }

            if (!connected) {
                Log.e(TAG, "Could not connect to the given session.");
            }
        }

        ////////////////////////////////////////////////////////////////////////////////
        // MediaController methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public ParcelImpl getConnectedSessionToken(String controllerId) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            return MediaParcelUtils.toParcelable(controller.getConnectedToken());
        }

        @Override
        public void play(String controllerId) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.play();
        }

        @Override
        public void pause(String controllerId) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.pause();
        }

        @Override
        public void prepare(String controllerId) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.prepare();
        }

        @Override
        public void seekTo(String controllerId, long pos) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.seekTo(pos);
        }

        @Override
        public void setPlaybackSpeed(String controllerId, float speed) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.setPlaybackSpeed(speed);
        }

        @Override
        public void setPlaylist(String controllerId, List<String> list, ParcelImpl metadata)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.setPlaylist(list, (MediaMetadata) MediaParcelUtils.fromParcelable(metadata));
        }

        @Override
        public void createAndSetDummyPlaylist(String controllerId, int size, ParcelImpl metadata)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                // Make media ID of each item same with its index.
                list.add(TestUtils.getMediaIdInDummyList(i));
            }
            controller.setPlaylist(list, (MediaMetadata) MediaParcelUtils.fromParcelable(metadata));
        }

        @Override
        public void setMediaItem(String controllerId, String mediaId) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.setMediaItem(mediaId);
        }

        @Override
        public void updatePlaylistMetadata(String controllerId, ParcelImpl metadata)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.updatePlaylistMetadata(
                    (MediaMetadata) MediaParcelUtils.fromParcelable(metadata));
        }

        @Override
        public void addPlaylistItem(String controllerId, int index, String mediaId)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.addPlaylistItem(index, mediaId);
        }

        @Override
        public void removePlaylistItem(String controllerId, int index) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.removePlaylistItem(index);
        }

        @Override
        public void replacePlaylistItem(String controllerId, int index, String mediaId)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.replacePlaylistItem(index, mediaId);
        }

        @Override
        public void skipToPreviousItem(String controllerId) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.skipToPreviousPlaylistItem();
        }

        @Override
        public void skipToNextItem(String controllerId) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.skipToNextPlaylistItem();
        }

        @Override
        public void skipToPlaylistItem(String controllerId, int index) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.skipToPlaylistItem(index);
        }

        @Override
        public void setShuffleMode(String controllerId, int shuffleMode) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.setShuffleMode(shuffleMode);
        }

        @Override
        public void setRepeatMode(String controllerId, int repeatMode) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.setRepeatMode(repeatMode);
        }

        @Override
        public void setVolumeTo(String controllerId, int value, int flags) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.setVolumeTo(value, flags);
        }

        @Override
        public void adjustVolume(String controllerId, int direction, int flags)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.adjustVolume(direction, flags);
        }

        @Override
        public void sendCustomCommand(String controllerId, ParcelImpl command, Bundle args)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.sendCustomCommand((SessionCommand) MediaParcelUtils.fromParcelable(command),
                    args);
        }

        @Override
        public void fastForward(String controllerId) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.fastForward();
        }

        @Override
        public void rewind(String controllerId) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.rewind();
        }

        @Override
        public void skipForward(String controllerId) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.skipForward();
        }

        @Override
        public void skipBackward(String controllerId) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.skipBackward();
        }

        @Override
        public void playFromMediaId(String controllerId, String mediaId, Bundle extras)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.playFromMediaId(mediaId, extras);
        }

        @Override
        public void playFromSearch(String controllerId, String query, Bundle extras)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.playFromSearch(query, extras);
        }

        @Override
        public void playFromUri(String controllerId, Uri uri, Bundle extras)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.playFromUri(uri, extras);
        }

        @Override
        public void prepareFromMediaId(String controllerId, String mediaId, Bundle extras)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.prepareFromMediaId(mediaId, extras);
        }

        @Override
        public void prepareFromSearch(String controllerId, String query, Bundle extras)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.prepareFromSearch(query, extras);
        }

        @Override
        public void prepareFromUri(String controllerId, Uri uri, Bundle extras)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.prepareFromUri(uri, extras);
        }

        @Override
        public void setRating(String controllerId, String mediaId, ParcelImpl rating)
                throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.setRating(mediaId, ParcelUtils.<Rating>fromParcelable(rating));
        }

        @Override
        public void close(String controllerId) throws RemoteException {
            MediaController controller = mMediaControllerMap.get(controllerId);
            controller.close();
        }

        ////////////////////////////////////////////////////////////////////////////////
        // MediaBrowser methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public void getLibraryRoot(String controllerId, ParcelImpl libraryParams)
                throws RemoteException {
            MediaBrowser browser = (MediaBrowser) mMediaControllerMap.get(controllerId);
            browser.getLibraryRoot((LibraryParams) MediaParcelUtils.fromParcelable(libraryParams));
        }

        @Override
        public void subscribe(String controllerId, String parentId, ParcelImpl libraryParams)
                throws RemoteException {
            MediaBrowser browser = (MediaBrowser) mMediaControllerMap.get(controllerId);
            browser.subscribe(parentId,
                    (LibraryParams) MediaParcelUtils.fromParcelable(libraryParams));
        }

        @Override
        public void unsubscribe(String controllerId, String parentId) throws RemoteException {
            MediaBrowser browser = (MediaBrowser) mMediaControllerMap.get(controllerId);
            browser.unsubscribe(parentId);
        }

        @Override
        public void getChildren(String controllerId, String parentId, int page, int pageSize,
                ParcelImpl libraryParams) throws RemoteException {
            MediaBrowser browser = (MediaBrowser) mMediaControllerMap.get(controllerId);
            browser.getChildren(parentId, page, pageSize,
                    (LibraryParams) MediaParcelUtils.fromParcelable(libraryParams));
        }

        @Override
        public void getItem(String controllerId, String mediaId) throws RemoteException {
            MediaBrowser browser = (MediaBrowser) mMediaControllerMap.get(controllerId);
            browser.getItem(mediaId);
        }

        @Override
        public void search(String controllerId, String query, ParcelImpl libraryParams)
                throws RemoteException {
            MediaBrowser browser = (MediaBrowser) mMediaControllerMap.get(controllerId);
            browser.search(query, (LibraryParams) MediaParcelUtils.fromParcelable(libraryParams));
        }

        @Override
        public void getSearchResult(String controllerId, String query, int page, int pageSize,
                ParcelImpl libraryParams) throws RemoteException {
            MediaBrowser browser = (MediaBrowser) mMediaControllerMap.get(controllerId);
            browser.getSearchResult(query, page, pageSize,
                    (LibraryParams) MediaParcelUtils.fromParcelable(libraryParams));
        }

        private class TestControllerCallback extends MediaBrowser.BrowserCallback {
            private CountDownLatch mConnectionLatch = new CountDownLatch(1);

            @Override
            public void onConnected(MediaController controller,
                    SessionCommandGroup allowedCommands) {
                super.onConnected(controller, allowedCommands);
                mConnectionLatch.countDown();
            }
        }
    }
}
