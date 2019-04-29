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

package androidx.media2.test.service;

import static androidx.media2.test.common.CommonConstants.ACTION_MEDIA_SESSION_COMPAT;
import static androidx.media2.test.common.CommonConstants.KEY_METADATA_COMPAT;
import static androidx.media2.test.common.CommonConstants.KEY_PLAYBACK_STATE_COMPAT;
import static androidx.media2.test.common.CommonConstants.KEY_QUEUE;
import static androidx.media2.test.common.CommonConstants.KEY_SESSION_COMPAT_TOKEN;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.media.VolumeProviderCompat;
import androidx.media2.test.common.IRemoteMediaSessionCompat;
import androidx.media2.test.common.TestUtils.SyncHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A Service that creates {@link MediaSessionCompat} and calls its methods according to the
 * client app's requests.
 */
public class MediaSessionCompatProviderService extends Service {
    private static final String TAG = "MediaSessionCompatProviderService";

    Map<String, MediaSessionCompat> mSessionMap = new HashMap<>();
    RemoteMediaSessionCompatStub mSessionBinder;

    SyncHandler mHandler;
    Executor mExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        mSessionBinder = new RemoteMediaSessionCompatStub();
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
        if (ACTION_MEDIA_SESSION_COMPAT.equals(intent.getAction())) {
            return mSessionBinder;
        }
        return null;
    }

    @Override
    public void onDestroy() {
        for (MediaSessionCompat session : mSessionMap.values()) {
            session.release();
        }
    }

    private class RemoteMediaSessionCompatStub extends IRemoteMediaSessionCompat.Stub {
        @Override
        public void create(final String sessionTag) throws RemoteException {
            try {
                mHandler.postAndSync(new Runnable() {
                    @Override
                    public void run() {
                        final MediaSessionCompat session = new MediaSessionCompat(
                                MediaSessionCompatProviderService.this, sessionTag);
                        mSessionMap.put(sessionTag, session);
                    }
                });
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException occurred while creating MediaSessionCompat", ex);
            }
        }

        ////////////////////////////////////////////////////////////////////////////////
        // MediaSessionCompat methods
        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public Bundle getSessionToken(String sessionTag) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            Bundle result = new Bundle();
            result.putParcelable(KEY_SESSION_COMPAT_TOKEN, session.getSessionToken());
            return result;
        }

        @Override
        public void setPlaybackToLocal(String sessionTag, int stream) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            session.setPlaybackToLocal(stream);
        }

        @Override
        public void setPlaybackToRemote(String sessionTag, int volumeControl, int maxVolume,
                int currentVolume) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            session.setPlaybackToRemote(new VolumeProviderCompat(
                    volumeControl, maxVolume, currentVolume) {
                @Override
                public void onSetVolumeTo(int volume) {
                    setCurrentVolume(volume);
                }

                @Override
                public void onAdjustVolume(int direction) {
                    setCurrentVolume(getCurrentVolume() + direction);
                }
            });
        }

        @Override
        public void release(String sessionTag) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            session.release();
        }

        @Override
        public void setPlaybackState(String sessionTag, Bundle stateBundle) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            stateBundle.setClassLoader(MediaSessionCompat.class.getClassLoader());
            PlaybackStateCompat state = stateBundle.getParcelable(KEY_PLAYBACK_STATE_COMPAT);
            session.setPlaybackState(state);
        }

        @Override
        public void setMetadata(String sessionTag, Bundle metadataBundle) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            metadataBundle.setClassLoader(MediaSessionCompat.class.getClassLoader());
            MediaMetadataCompat metadata = metadataBundle.getParcelable(KEY_METADATA_COMPAT);
            session.setMetadata(metadata);
        }

        @Override
        public void setQueue(String sessionTag, Bundle queueBundle) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            queueBundle.setClassLoader(MediaSessionCompat.class.getClassLoader());
            List<QueueItem> queue = queueBundle.getParcelableArrayList(KEY_QUEUE);
            session.setQueue(queue);
        }

        @Override
        public void setQueueTitle(String sessionTag, CharSequence title) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            session.setQueueTitle(title);
        }

        @Override
        public void setRepeatMode(String sessionTag, int repeatMode) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            session.setRepeatMode(repeatMode);
        }

        @Override
        public void setShuffleMode(String sessionTag, int shuffleMode) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            session.setShuffleMode(shuffleMode);
        }

        @Override
        public void setSessionActivity(String sessionTag, PendingIntent pi) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            session.setSessionActivity(pi);
        }

        @Override
        public void setFlags(String sessionTag, int flags) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            session.setFlags(flags);
        }

        @Override
        public void setRatingType(String sessionTag, int type) throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            session.setRatingType(type);
        }

        @Override
        public void sendSessionEvent(String sessionTag, String event, Bundle extras)
                throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            session.sendSessionEvent(event, extras);
        }

        @Override
        public void setCaptioningEnabled(String sessionTag, boolean enabled)
                throws RemoteException {
            MediaSessionCompat session = mSessionMap.get(sessionTag);
            session.setCaptioningEnabled(enabled);
        }
    }
}
