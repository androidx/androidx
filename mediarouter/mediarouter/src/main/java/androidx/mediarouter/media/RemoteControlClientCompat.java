/*
 * Copyright (C) 2013 The Android Open Source Project
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
package androidx.mediarouter.media;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.ref.WeakReference;

/**
 * Provides access to features of the remote control client.
 *
 * Hidden for now but we might want to make this available to applications
 * in the future.
 */
abstract class RemoteControlClientCompat {
    protected final Context mContext;
    protected final android.media.RemoteControlClient mRcc;
    protected VolumeCallback mVolumeCallback;

    protected RemoteControlClientCompat(Context context, android.media.RemoteControlClient rcc) {
        mContext = context;
        mRcc = rcc;
    }

    public static RemoteControlClientCompat obtain(
            Context context, android.media.RemoteControlClient rcc) {
        if (Build.VERSION.SDK_INT >= 16) {
            return new JellybeanImpl(context, rcc);
        }
        return new LegacyImpl(context, rcc);
    }

    public android.media.RemoteControlClient getRemoteControlClient() {
        return mRcc;
    }

    /**
     * Sets the current playback information.
     * Must be called at least once to attach to the remote control client.
     *
     * @param info The playback information.  Must not be null.
     */
    public void setPlaybackInfo(PlaybackInfo info) {
    }

    /**
     * Sets a callback to receive volume change requests from the remote control client.
     *
     * @param callback The volume callback to use or null if none.
     */
    public void setVolumeCallback(VolumeCallback callback) {
        mVolumeCallback = callback;
    }

    /**
     * Specifies information about the playback.
     */
    public static final class PlaybackInfo {
        public int volume;
        public int volumeMax;
        public int volumeHandling = MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED;
        public int playbackStream = AudioManager.STREAM_MUSIC;
        public int playbackType = MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE;
        @Nullable
        public String volumeControlId;
    }

    /**
     * Called when volume updates are requested by the remote control client.
     */
    public interface VolumeCallback {
        /**
         * Called when the volume should be increased or decreased.
         *
         * @param direction An integer indicating whether the volume is to be increased
         *                  (positive value) or decreased (negative value).
         *                  For bundled changes, the absolute value indicates the number of changes
         *                  in the same direction, e.g. +3 corresponds to three "volume up" changes.
         */
        void onVolumeUpdateRequest(int direction);

        /**
         * Called when the volume for the route should be set to the given value.
         *
         * @param volume An integer indicating the new volume value that should be used,
         *               always between 0 and the value set by {@link PlaybackInfo#volumeMax}.
         */
        void onVolumeSetRequest(int volume);
    }

    /**
     * Legacy implementation for platform versions prior to Jellybean.
     * Does nothing.
     */
    static class LegacyImpl extends RemoteControlClientCompat {
        LegacyImpl(Context context, android.media.RemoteControlClient rcc) {
            super(context, rcc);
        }
    }

    /**
     * Implementation for Jellybean.
     *
     * The basic idea of this implementation is to attach the RCC to a UserRouteInfo
     * in order to hook up stream metadata and volume callbacks because there is no
     * other API available to do so in this platform version.  The UserRouteInfo itself
     * is not attached to the MediaRouter so it is transparent to the user.
     */
    @RequiresApi(16)
    static class JellybeanImpl extends RemoteControlClientCompat {
        private final android.media.MediaRouter mRouter;
        private final android.media.MediaRouter.RouteCategory mUserRouteCategory;
        private final android.media.MediaRouter.UserRouteInfo mUserRoute;
        private boolean mRegistered;

        JellybeanImpl(Context context, android.media.RemoteControlClient rcc) {
            super(context, rcc);

            mRouter = MediaRouterApi16Impl.getMediaRouter(context);
            mUserRouteCategory = MediaRouterApi16Impl.createRouteCategory(mRouter, "", false);
            mUserRoute = MediaRouterApi16Impl.createUserRoute(mRouter, mUserRouteCategory);
        }

        @Override
        public void setPlaybackInfo(PlaybackInfo info) {
            MediaRouterApi16Impl.UserRouteInfo.setVolume(mUserRoute, info.volume);
            MediaRouterApi16Impl.UserRouteInfo.setVolumeMax(mUserRoute, info.volumeMax);
            MediaRouterApi16Impl.UserRouteInfo.setVolumeHandling(mUserRoute, info.volumeHandling);
            MediaRouterApi16Impl.UserRouteInfo.setPlaybackStream(mUserRoute, info.playbackStream);
            MediaRouterApi16Impl.UserRouteInfo.setPlaybackType(mUserRoute, info.playbackType);

            if (!mRegistered) {
                mRegistered = true;
                MediaRouterApi16Impl.UserRouteInfo.setVolumeCallback(
                        mUserRoute,
                        MediaRouterApi16Impl.createVolumeCallback(new VolumeCallbackWrapper(this)));
                MediaRouterApi16Impl.UserRouteInfo.setRemoteControlClient(mUserRoute, mRcc);
            }
        }

        private static final class VolumeCallbackWrapper
                implements MediaRouterApi16Impl.VolumeCallback {
            // Unfortunately, the framework never unregisters its volume observer from
            // the audio service so the UserRouteInfo object may leak along with
            // any callbacks that we attach to it.  Use a weak reference to prevent
            // the volume callback from holding strong references to anything important.
            private final WeakReference<JellybeanImpl> mImplWeak;

            public VolumeCallbackWrapper(JellybeanImpl impl) {
                mImplWeak = new WeakReference<>(impl);
            }

            @Override
            public void onVolumeUpdateRequest(@NonNull android.media.MediaRouter.RouteInfo route,
                    int direction) {
                JellybeanImpl impl = mImplWeak.get();
                if (impl != null && impl.mVolumeCallback != null) {
                    impl.mVolumeCallback.onVolumeUpdateRequest(direction);
                }
            }

            @Override
            public void onVolumeSetRequest(@NonNull android.media.MediaRouter.RouteInfo route,
                    int volume) {
                JellybeanImpl impl = mImplWeak.get();
                if (impl != null && impl.mVolumeCallback != null) {
                    impl.mVolumeCallback.onVolumeSetRequest(volume);
                }
            }
        }
    }
}
