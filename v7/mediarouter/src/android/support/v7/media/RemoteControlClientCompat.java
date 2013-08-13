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
package android.support.v7.media;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

import java.lang.ref.WeakReference;

/**
 * Provides access to features of the remote control client.
 *
 * Hidden for now but we might want to make this available to applications
 * in the future.
 */
abstract class RemoteControlClientCompat {
    protected final Context mContext;
    protected final Object mRcc;
    protected VolumeCallback mVolumeCallback;

    protected RemoteControlClientCompat(Context context, Object rcc) {
        mContext = context;
        mRcc = rcc;
    }

    public static RemoteControlClientCompat obtain(Context context, Object rcc) {
        if (Build.VERSION.SDK_INT >= 16) {
            return new JellybeanImpl(context, rcc);
        }
        return new LegacyImpl(context, rcc);
    }

    public Object getRemoteControlClient() {
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
    }

    /**
     * Called when volume updates are requested by the remote control client.
     */
    public interface VolumeCallback {
        /**
         * Called when the volume should be increased or decreased.
         *
         * @param direction An integer indicating whether the volume is to be increased
         * (positive value) or decreased (negative value).
         * For bundled changes, the absolute value indicates the number of changes
         * in the same direction, e.g. +3 corresponds to three "volume up" changes.
         */
        public void onVolumeUpdateRequest(int direction);

        /**
         * Called when the volume for the route should be set to the given value.
         *
         * @param volume An integer indicating the new volume value that should be used,
         * always between 0 and the value set by {@link PlaybackInfo#volumeMax}.
         */
        public void onVolumeSetRequest(int volume);
    }

    /**
     * Legacy implementation for platform versions prior to Jellybean.
     * Does nothing.
     */
    static class LegacyImpl extends RemoteControlClientCompat {
        public LegacyImpl(Context context, Object rcc) {
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
    static class JellybeanImpl extends RemoteControlClientCompat {
        private final Object mRouterObj;
        private final Object mUserRouteCategoryObj;
        private final Object mUserRouteObj;
        private boolean mRegistered;

        public JellybeanImpl(Context context, Object rcc) {
            super(context, rcc);

            mRouterObj = MediaRouterJellybean.getMediaRouter(context);
            mUserRouteCategoryObj = MediaRouterJellybean.createRouteCategory(
                    mRouterObj, "", false);
            mUserRouteObj = MediaRouterJellybean.createUserRoute(
                    mRouterObj, mUserRouteCategoryObj);
        }

        @Override
        public void setPlaybackInfo(PlaybackInfo info) {
            MediaRouterJellybean.UserRouteInfo.setVolume(
                    mUserRouteObj, info.volume);
            MediaRouterJellybean.UserRouteInfo.setVolumeMax(
                    mUserRouteObj, info.volumeMax);
            MediaRouterJellybean.UserRouteInfo.setVolumeHandling(
                    mUserRouteObj, info.volumeHandling);
            MediaRouterJellybean.UserRouteInfo.setPlaybackStream(
                    mUserRouteObj, info.playbackStream);
            MediaRouterJellybean.UserRouteInfo.setPlaybackType(
                    mUserRouteObj, info.playbackType);

            if (!mRegistered) {
                mRegistered = true;
                MediaRouterJellybean.UserRouteInfo.setVolumeCallback(mUserRouteObj,
                        MediaRouterJellybean.createVolumeCallback(
                                new VolumeCallbackWrapper(this)));
                MediaRouterJellybean.UserRouteInfo.setRemoteControlClient(mUserRouteObj, mRcc);
            }
        }

        private static final class VolumeCallbackWrapper
                implements MediaRouterJellybean.VolumeCallback {
            // Unfortunately, the framework never unregisters its volume observer from
            // the audio service so the UserRouteInfo object may leak along with
            // any callbacks that we attach to it.  Use a weak reference to prevent
            // the volume callback from holding strong references to anything important.
            private final WeakReference<JellybeanImpl> mImplWeak;

            public VolumeCallbackWrapper(JellybeanImpl impl) {
                mImplWeak = new WeakReference<JellybeanImpl>(impl);
            }

            @Override
            public void onVolumeUpdateRequest(Object routeObj, int direction) {
                JellybeanImpl impl = mImplWeak.get();
                if (impl != null && impl.mVolumeCallback != null) {
                    impl.mVolumeCallback.onVolumeUpdateRequest(direction);
                }
            }

            @Override
            public void onVolumeSetRequest(Object routeObj, int volume) {
                JellybeanImpl impl = mImplWeak.get();
                if (impl != null && impl.mVolumeCallback != null) {
                    impl.mVolumeCallback.onVolumeSetRequest(volume);
                }
            }
        }
    }
}
