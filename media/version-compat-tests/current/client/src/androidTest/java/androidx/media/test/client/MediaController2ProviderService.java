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

import static androidx.media.test.lib.CommonConstants.ACTION_MEDIA_CONTROLLER2;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.mediacompat.testlib.IRemoteMediaController2;
import android.util.Log;

import androidx.media.test.lib.TestUtils;
import androidx.media.test.lib.TestUtils.SyncHandler;
import androidx.media2.MediaBrowser2;
import androidx.media2.MediaController2;
import androidx.media2.MediaLibraryService2.LibraryParams;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaUtils2;
import androidx.media2.Rating2;
import androidx.media2.SessionCommand2;
import androidx.media2.SessionCommandGroup2;
import androidx.media2.SessionToken2;
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
 * A Service that creates {@link MediaController2} and calls its methods
 * according to the service app's requests.
 */
public class MediaController2ProviderService extends Service {
    private static final String TAG = "MediaController2ProviderService";

    Map<String, MediaController2> mMediaController2Map = new HashMap<>();
    RemoteMediaController2Stub mBinder;

    SyncHandler mHandler;
    Executor mExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new RemoteMediaController2Stub();

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
        if (ACTION_MEDIA_CONTROLLER2.equals(intent.getAction())) {
            return mBinder;
        }
        return null;
    }

    @Override
    public void onDestroy() {
        for (MediaController2 controller2 : mMediaController2Map.values()) {
            controller2.close();
        }
    }

    private class RemoteMediaController2Stub extends IRemoteMediaController2.Stub {
        @Override
        public void create(final boolean isBrowser, final String controllerId,
                ParcelImpl tokenParcelable, boolean waitForConnection) throws RemoteException {
            final SessionToken2 token = MediaUtils2.fromParcelable(tokenParcelable);
            final TestControllerCallback callback = new TestControllerCallback();

            try {
                mHandler.postAndSync(new Runnable() {
                    @Override
                    public void run() {
                        MediaController2 controller2;
                        if (isBrowser) {
                            controller2 = new MediaBrowser2(MediaController2ProviderService.this,
                                    token, mExecutor, callback);
                        } else {
                            controller2 = new MediaController2(MediaController2ProviderService.this,
                                    token, mExecutor, callback);
                        }
                        mMediaController2Map.put(controllerId, controller2);
                    }
                });
            } catch (Exception ex) {
                Log.e(TAG, "Exception occurred while creating MediaController2.", ex);
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
                Log.e(TAG, "Could not connect to the given session2.");
            }
        }

        ////////////////////////////////////////////////////////////////////////////////
        // MediaController2 methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public ParcelImpl getConnectedSessionToken(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            return MediaUtils2.toParcelable(controller2.getConnectedSessionToken());
        }

        @Override
        public void play(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.play();
        }

        @Override
        public void pause(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.pause();
        }

        @Override
        public void prepare(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.prepare();
        }

        @Override
        public void seekTo(String controllerId, long pos) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.seekTo(pos);
        }

        @Override
        public void setPlaybackSpeed(String controllerId, float speed) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.setPlaybackSpeed(speed);
        }

        @Override
        public void setPlaylist(String controllerId, List<String> list, ParcelImpl metadata)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.setPlaylist(list, (MediaMetadata2) MediaUtils2.fromParcelable(metadata));
        }

        @Override
        public void createAndSetDummyPlaylist(String controllerId, int size, ParcelImpl metadata)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                // Make media ID of each item same with its index.
                list.add(TestUtils.getMediaIdInDummyList(i));
            }
            controller2.setPlaylist(list, (MediaMetadata2) MediaUtils2.fromParcelable(metadata));
        }

        @Override
        public void setMediaItem(String controllerId, String mediaId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.setMediaItem(mediaId);
        }

        @Override
        public void updatePlaylistMetadata(String controllerId, ParcelImpl metadata)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.updatePlaylistMetadata(
                    (MediaMetadata2) MediaUtils2.fromParcelable(metadata));
        }

        @Override
        public void addPlaylistItem(String controllerId, int index, String mediaId)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.addPlaylistItem(index, mediaId);
        }

        @Override
        public void removePlaylistItem(String controllerId, int index) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.removePlaylistItem(index);
        }

        @Override
        public void replacePlaylistItem(String controllerId, int index, String mediaId)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.replacePlaylistItem(index, mediaId);
        }

        @Override
        public void skipToPreviousItem(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.skipToPreviousPlaylistItem();
        }

        @Override
        public void skipToNextItem(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.skipToNextPlaylistItem();
        }

        @Override
        public void skipToPlaylistItem(String controllerId, int index) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.skipToPlaylistItem(index);
        }

        @Override
        public void setShuffleMode(String controllerId, int shuffleMode) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.setShuffleMode(shuffleMode);
        }

        @Override
        public void setRepeatMode(String controllerId, int repeatMode) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.setRepeatMode(repeatMode);
        }

        @Override
        public void setVolumeTo(String controllerId, int value, int flags) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.setVolumeTo(value, flags);
        }

        @Override
        public void adjustVolume(String controllerId, int direction, int flags)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.adjustVolume(direction, flags);
        }

        @Override
        public void sendCustomCommand(String controllerId, Bundle command, Bundle args)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.sendCustomCommand(SessionCommand2.fromBundle(command), args);
        }

        @Override
        public void fastForward(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.fastForward();
        }

        @Override
        public void rewind(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.rewind();
        }

        @Override
        public void skipForward(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.skipForward();
        }

        @Override
        public void skipBackward(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.skipBackward();
        }

        @Override
        public void playFromMediaId(String controllerId, String mediaId, Bundle extras)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.playFromMediaId(mediaId, extras);
        }

        @Override
        public void playFromSearch(String controllerId, String query, Bundle extras)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.playFromSearch(query, extras);
        }

        @Override
        public void playFromUri(String controllerId, Uri uri, Bundle extras)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.playFromUri(uri, extras);
        }

        @Override
        public void prepareFromMediaId(String controllerId, String mediaId, Bundle extras)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.prepareFromMediaId(mediaId, extras);
        }

        @Override
        public void prepareFromSearch(String controllerId, String query, Bundle extras)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.prepareFromSearch(query, extras);
        }

        @Override
        public void prepareFromUri(String controllerId, Uri uri, Bundle extras)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.prepareFromUri(uri, extras);
        }

        @Override
        public void setRating(String controllerId, String mediaId, ParcelImpl rating)
                throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.setRating(mediaId, ParcelUtils.<Rating2>fromParcelable(rating));
        }

        @Override
        public void subscribeRoutesInfo(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.subscribeRoutesInfo();
        }

        @Override
        public void unsubscribeRoutesInfo(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.unsubscribeRoutesInfo();
        }

        @Override
        public void selectRoute(String controllerId, Bundle route) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.selectRoute(route);
        }

        @Override
        public void close(String controllerId) throws RemoteException {
            MediaController2 controller2 = mMediaController2Map.get(controllerId);
            controller2.close();
        }

        ////////////////////////////////////////////////////////////////////////////////
        // MediaBrowser2 methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public void getLibraryRoot(String controllerId, ParcelImpl libraryParams)
                throws RemoteException {
            MediaBrowser2 browser2 = (MediaBrowser2) mMediaController2Map.get(controllerId);
            browser2.getLibraryRoot((LibraryParams) MediaUtils2.fromParcelable(libraryParams));
        }

        @Override
        public void subscribe(String controllerId, String parentId, ParcelImpl libraryParams)
                throws RemoteException {
            MediaBrowser2 browser2 = (MediaBrowser2) mMediaController2Map.get(controllerId);
            browser2.subscribe(parentId, (LibraryParams) MediaUtils2.fromParcelable(libraryParams));
        }

        @Override
        public void unsubscribe(String controllerId, String parentId) throws RemoteException {
            MediaBrowser2 browser2 = (MediaBrowser2) mMediaController2Map.get(controllerId);
            browser2.unsubscribe(parentId);
        }

        @Override
        public void getChildren(String controllerId, String parentId, int page, int pageSize,
                ParcelImpl libraryParams) throws RemoteException {
            MediaBrowser2 browser2 = (MediaBrowser2) mMediaController2Map.get(controllerId);
            browser2.getChildren(parentId, page, pageSize,
                    (LibraryParams) MediaUtils2.fromParcelable(libraryParams));
        }

        @Override
        public void getItem(String controllerId, String mediaId) throws RemoteException {
            MediaBrowser2 browser2 = (MediaBrowser2) mMediaController2Map.get(controllerId);
            browser2.getItem(mediaId);
        }

        @Override
        public void search(String controllerId, String query, ParcelImpl libraryParams)
                throws RemoteException {
            MediaBrowser2 browser2 = (MediaBrowser2) mMediaController2Map.get(controllerId);
            browser2.search(query, (LibraryParams) MediaUtils2.fromParcelable(libraryParams));
        }

        @Override
        public void getSearchResult(String controllerId, String query, int page, int pageSize,
                ParcelImpl libraryParams) throws RemoteException {
            MediaBrowser2 browser2 = (MediaBrowser2) mMediaController2Map.get(controllerId);
            browser2.getSearchResult(query, page, pageSize,
                    (LibraryParams) MediaUtils2.fromParcelable(libraryParams));
        }

        private class TestControllerCallback extends MediaBrowser2.BrowserCallback {
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
