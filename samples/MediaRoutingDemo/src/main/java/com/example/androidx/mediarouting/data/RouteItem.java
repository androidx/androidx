/*
 * Copyright 2022 The Android Open Source Project
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

package com.example.androidx.mediarouting.data;

import android.media.AudioManager;

import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** RouteItem helps keep track of the current app controlled routes. */
public final class RouteItem {

    private String mId;
    private String mName;
    private String mDescription;
    private ControlFilter mControlFilter;
    private PlaybackStream mPlaybackStream;
    private PlaybackType mPlaybackType;
    private boolean mCanDisconnect;
    private VolumeHandling mVolumeHandling;
    private int mVolume;
    private int mVolumeMax;
    private DeviceType mDeviceType;
    private List<String> mGroupMemberIds;
    private boolean mIsSenderDriven;

    public RouteItem() {
        this.mId = UUID.randomUUID().toString();
        this.mName = "";
        this.mDescription = "";
        this.mControlFilter = ControlFilter.BASIC;
        this.mPlaybackStream = PlaybackStream.MUSIC;
        this.mPlaybackType = PlaybackType.REMOTE;
        this.mVolumeHandling = VolumeHandling.FIXED;
        this.mVolume = 5;
        this.mVolumeMax = 25;
        this.mDeviceType = DeviceType.UNKNOWN;
        this.mCanDisconnect = false;
        this.mGroupMemberIds = new ArrayList<>();
        this.mIsSenderDriven = false;
    }

    public RouteItem(
            @NonNull String id,
            @NonNull String name,
            @NonNull String description,
            @NonNull ControlFilter controlFilter,
            @NonNull PlaybackStream playbackStream,
            @NonNull PlaybackType playbackType,
            boolean canDisconnect,
            @NonNull VolumeHandling volumeHandling,
            int volume,
            int volumeMax,
            @NonNull DeviceType deviceType,
            @NonNull List<String> groupMemberIds,
            boolean isSenderDriven) {
        mId = id;
        mName = name;
        mDescription = description;
        mControlFilter = controlFilter;
        mPlaybackStream = playbackStream;
        mPlaybackType = playbackType;
        mCanDisconnect = canDisconnect;
        mVolumeHandling = volumeHandling;
        mVolume = volume;
        mVolumeMax = volumeMax;
        mDeviceType = deviceType;
        mGroupMemberIds = groupMemberIds;
        mIsSenderDriven = isSenderDriven;
    }

    /** Returns a deep copy of an existing {@link RouteItem}. */
    @NonNull
    public static RouteItem copyOf(@NonNull RouteItem routeItem) {
        return new RouteItem(
                routeItem.getId(),
                routeItem.getName(),
                routeItem.getDescription(),
                routeItem.getControlFilter(),
                routeItem.getPlaybackStream(),
                routeItem.getPlaybackType(),
                routeItem.isCanDisconnect(),
                routeItem.getVolumeHandling(),
                routeItem.getVolume(),
                routeItem.getVolumeMax(),
                routeItem.getDeviceType(),
                routeItem.getGroupMemberIds(),
                routeItem.isSenderDriven());
    }

    public enum ControlFilter {
        BASIC,
        QUEUE,
        SESSION;
    }

    public enum PlaybackStream {
        ACCESSIBILITY(AudioManager.STREAM_ACCESSIBILITY),
        ALARM(AudioManager.STREAM_ALARM),
        DTMF(AudioManager.STREAM_DTMF),
        MUSIC(AudioManager.STREAM_MUSIC),
        NOTIFICATION(AudioManager.STREAM_NOTIFICATION),
        RING(AudioManager.STREAM_RING),
        SYSTEM(AudioManager.STREAM_SYSTEM),
        VOICE_CALL(AudioManager.STREAM_VOICE_CALL);

        public final int mIntConstant;

        PlaybackStream(int intConstant) {
            mIntConstant = intConstant;
        }
    }

    public enum PlaybackType {
        LOCAL(MediaRouter.RouteInfo.PLAYBACK_TYPE_LOCAL),
        REMOTE(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE);

        public final int mIntConstant;

        PlaybackType(int intConstant) {
            mIntConstant = intConstant;
        }
    }

    public enum VolumeHandling {
        FIXED(MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED),
        VARIABLE(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE);

        public final int mIntConstant;

        VolumeHandling(int intConstant) {
            mIntConstant = intConstant;
        }
    }

    public enum DeviceType {
        TV(MediaRouter.RouteInfo.DEVICE_TYPE_TV),
        SPEAKER(MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER),
        BLUETOOTH(MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH),
        AUDIO_VIDEO_RECEIVER(MediaRouter.RouteInfo.DEVICE_TYPE_AUDIO_VIDEO_RECEIVER),
        TABLET(MediaRouter.RouteInfo.DEVICE_TYPE_TABLET),
        TABLET_DOCKED(MediaRouter.RouteInfo.DEVICE_TYPE_TABLET_DOCKED),
        COMPUTER(MediaRouter.RouteInfo.DEVICE_TYPE_COMPUTER),
        GAME_CONSOLE(MediaRouter.RouteInfo.DEVICE_TYPE_GAME_CONSOLE),
        CAR(MediaRouter.RouteInfo.DEVICE_TYPE_CAR),
        SMARTWATCH(MediaRouter.RouteInfo.DEVICE_TYPE_SMARTWATCH),
        SMARTPHONE(MediaRouter.RouteInfo.DEVICE_TYPE_SMARTPHONE),
        GROUP(MediaRouter.RouteInfo.DEVICE_TYPE_GROUP),
        UNKNOWN(MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN);

        public final int mIntConstant;

        DeviceType(int intConstant) {
            mIntConstant = intConstant;
        }
    }

    @NonNull
    public String getId() {
        return mId;
    }

    public void setId(@NonNull String id) {
        mId = id;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public void setName(@NonNull String name) {
        mName = name;
    }

    @NonNull
    public String getDescription() {
        return mDescription;
    }

    public void setDescription(@NonNull String description) {
        mDescription = description;
    }

    @NonNull
    public ControlFilter getControlFilter() {
        return mControlFilter;
    }

    public void setControlFilter(@NonNull ControlFilter controlFilter) {
        mControlFilter = controlFilter;
    }

    @NonNull
    public PlaybackStream getPlaybackStream() {
        return mPlaybackStream;
    }

    public void setPlaybackStream(@NonNull PlaybackStream playbackStream) {
        mPlaybackStream = playbackStream;
    }

    @NonNull
    public PlaybackType getPlaybackType() {
        return mPlaybackType;
    }

    public void setPlaybackType(@NonNull PlaybackType playbackType) {
        mPlaybackType = playbackType;
    }

    public boolean isCanDisconnect() {
        return mCanDisconnect;
    }

    public void setCanDisconnect(boolean canDisconnect) {
        mCanDisconnect = canDisconnect;
    }

    @NonNull
    public VolumeHandling getVolumeHandling() {
        return mVolumeHandling;
    }

    public void setVolumeHandling(@NonNull VolumeHandling volumeHandling) {
        mVolumeHandling = volumeHandling;
    }

    public int getVolume() {
        return mVolume;
    }

    public void setVolume(int volume) {
        mVolume = volume;
    }

    public int getVolumeMax() {
        return mVolumeMax;
    }

    public void setVolumeMax(int volumeMax) {
        mVolumeMax = volumeMax;
    }

    @NonNull
    public DeviceType getDeviceType() {
        return mDeviceType;
    }

    public void setDeviceType(@NonNull DeviceType deviceType) {
        mDeviceType = deviceType;
    }

    @NonNull
    public List<String> getGroupMemberIds() {
        return mGroupMemberIds;
    }

    public void setGroupMemberIds(@NonNull List<String> groupMemberIds) {
        mGroupMemberIds = groupMemberIds;
    }

    public boolean isSenderDriven() {
        return mIsSenderDriven;
    }

    public void setSenderDriven(boolean isSenderDriven) {
        mIsSenderDriven = isSenderDriven;
    }
}
