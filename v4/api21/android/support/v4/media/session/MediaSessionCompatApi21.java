/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.VolumeProvider;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;

import java.util.ArrayList;
import java.util.List;

class MediaSessionCompatApi21 {
    public static Object createSession(Context context, String tag) {
        return new MediaSession(context, tag);
    }

    public static Object verifySession(Object mediaSession) {
        if (mediaSession instanceof MediaSession) {
            return mediaSession;
        }
        throw new IllegalArgumentException("mediaSession is not a valid MediaSession object");
    }

    public static Object verifyToken(Object token) {
        if (token instanceof MediaSession.Token) {
            return token;
        }
        throw new IllegalArgumentException("token is not a valid MediaSession.Token object");
    }

    public static Object createCallback(Callback callback) {
        return new CallbackProxy<Callback>(callback);
    }

    public static void setCallback(Object sessionObj, Object callbackObj, Handler handler) {
        ((MediaSession) sessionObj).setCallback((MediaSession.Callback) callbackObj, handler);
    }

    public static void setFlags(Object sessionObj, int flags) {
        ((MediaSession)sessionObj).setFlags(flags);
    }

    public static void setPlaybackToLocal(Object sessionObj, int stream) {
        // TODO update APIs to use support version of AudioAttributes
        AudioAttributes.Builder bob = new AudioAttributes.Builder();
        bob.setLegacyStreamType(stream);
        ((MediaSession) sessionObj).setPlaybackToLocal(bob.build());
    }

    public static void setPlaybackToRemote(Object sessionObj, Object volumeProviderObj) {
        ((MediaSession)sessionObj).setPlaybackToRemote((VolumeProvider)volumeProviderObj);
    }

    public static void setActive(Object sessionObj, boolean active) {
        ((MediaSession)sessionObj).setActive(active);
    }

    public static boolean isActive(Object sessionObj) {
        return ((MediaSession)sessionObj).isActive();
    }

    public static void sendSessionEvent(Object sessionObj, String event, Bundle extras) {
        ((MediaSession)sessionObj).sendSessionEvent(event, extras);
    }

    public static void release(Object sessionObj) {
        ((MediaSession)sessionObj).release();
    }

    public static Parcelable getSessionToken(Object sessionObj) {
        return ((MediaSession)sessionObj).getSessionToken();
    }

    public static void setPlaybackState(Object sessionObj, Object stateObj) {
        ((MediaSession)sessionObj).setPlaybackState((PlaybackState)stateObj);
    }

    public static void setMetadata(Object sessionObj, Object metadataObj) {
        ((MediaSession)sessionObj).setMetadata((MediaMetadata)metadataObj);
    }

    public static void setSessionActivity(Object sessionObj, PendingIntent pi) {
        ((MediaSession) sessionObj).setSessionActivity(pi);
    }

    public static void setMediaButtonReceiver(Object sessionObj, PendingIntent pi) {
        ((MediaSession) sessionObj).setMediaButtonReceiver(pi);
    }

    public static void setQueue(Object sessionObj, List<Object> queueObjs) {
        if (queueObjs == null) {
            ((MediaSession) sessionObj).setQueue(null);
            return;
        }
        ArrayList<MediaSession.QueueItem> queue = new ArrayList<MediaSession.QueueItem>();
        for (Object itemObj : queueObjs) {
            queue.add((MediaSession.QueueItem) itemObj);
        }
        ((MediaSession) sessionObj).setQueue(queue);
    }

    public static void setQueueTitle(Object sessionObj, CharSequence title) {
        ((MediaSession) sessionObj).setQueueTitle(title);
    }

    public static void setExtras(Object sessionObj, Bundle extras) {
        ((MediaSession) sessionObj).setExtras(extras);
    }

    public static interface Callback {
        public void onCommand(String command, Bundle extras, ResultReceiver cb);
        public boolean onMediaButtonEvent(Intent mediaButtonIntent);
        public void onPlay();
        public void onPlayFromMediaId(String mediaId, Bundle extras);
        public void onPlayFromSearch(String search, Bundle extras);
        public void onSkipToQueueItem(long id);
        public void onPause();
        public void onSkipToNext();
        public void onSkipToPrevious();
        public void onFastForward();
        public void onRewind();
        public void onStop();
        public void onSeekTo(long pos);
        public void onSetRating(Object ratingObj);
        public void onCustomAction(String action, Bundle extras);
    }

    static class CallbackProxy<T extends Callback> extends MediaSession.Callback {
        protected final T mCallback;

        public CallbackProxy(T callback) {
            mCallback = callback;
        }

        @Override
        public void onCommand(String command, Bundle args, ResultReceiver cb) {
            mCallback.onCommand(command, args, cb);
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            return mCallback.onMediaButtonEvent(mediaButtonIntent);
        }

        @Override
        public void onPlay() {
            mCallback.onPlay();
        }

        @Override
        public void onPause() {
            mCallback.onPause();
        }

        @Override
        public void onSkipToNext() {
            mCallback.onSkipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            mCallback.onSkipToPrevious();
        }

        @Override
        public void onFastForward() {
            mCallback.onFastForward();
        }

        @Override
        public void onRewind() {
            mCallback.onRewind();
        }

        @Override
        public void onStop() {
            mCallback.onStop();
        }

        @Override
        public void onSeekTo(long pos) {
            mCallback.onSeekTo(pos);
        }

        @Override
        public void onSetRating(Rating rating) {
            mCallback.onSetRating(rating);
        }
    }

    static class QueueItem {

        public static Object createItem(Object mediaDescription, long id) {
            return new MediaSession.QueueItem((MediaDescription) mediaDescription, id);
        }

        public static Object getDescription(Object queueItem) {
            return ((MediaSession.QueueItem) queueItem).getDescription();
        }

        public static long getQueueId(Object queueItem) {
            return ((MediaSession.QueueItem) queueItem).getQueueId();
        }
    }
}
