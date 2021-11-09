/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import android.os.Binder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.MediaParcelUtils;
import androidx.media2.common.ParcelImplListSlice;
import androidx.media2.common.SessionPlayer.BuffState;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.versionedparcelable.ParcelImpl;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class MediaControllerStub extends IMediaController.Stub {
    private static final String TAG = "MediaControllerStub";
    private static final boolean DEBUG = true; // TODO(jaewan): Change

    private final WeakReference<MediaControllerImplBase> mController;

    MediaControllerStub(MediaControllerImplBase controller) {
        mController = new WeakReference<>(controller);
    }

    @Override
    public void onSessionResult(final int seq, final ParcelImpl sessionResult) {
        if (sessionResult == null) {
            return;
        }
        dispatchControllerTask(controller ->
                controller.setFutureResult(seq, MediaParcelUtils.fromParcelable(sessionResult)));
    }

    @Override
    public void onLibraryResult(final int seq, final ParcelImpl libraryResult) {
        if (libraryResult == null) {
            return;
        }
        dispatchBrowserTask(browser ->
                browser.setFutureResult(seq, MediaParcelUtils.fromParcelable(libraryResult)));
    }

    @Override
    public void onCurrentMediaItemChanged(int seq, final ParcelImpl item, final int currentIdx,
            final int previousIdx, final int nextIdx) {
        if (item == null) {
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                controller.notifyCurrentMediaItemChanged(
                        (MediaItem) MediaParcelUtils.fromParcelable(item), currentIdx, previousIdx,
                        nextIdx);
            }
        });
    }

    @Override
    public void onPlayerStateChanged(int seq, final long eventTimeMs, final long positionMs,
            final int state) {
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                controller.notifyPlayerStateChanges(eventTimeMs, positionMs, state);
            }
        });
    }

    @Override
    public void onPlaybackSpeedChanged(int seq, final long eventTimeMs, final long positionMs,
            final float speed) {
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                controller.notifyPlaybackSpeedChanges(eventTimeMs, positionMs, speed);
            }
        });
    }

    @Override
    public void onBufferingStateChanged(int seq, final ParcelImpl item, @BuffState final int state,
            final long bufferedPositionMs, final long eventTimeMs, final long positionMs) {
        if (item == null) {
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                MediaItem itemObj = MediaParcelUtils.fromParcelable(item);
                if (itemObj == null) {
                    Log.w(TAG, "onBufferingStateChanged(): Ignoring null item");
                    return;
                }
                controller.notifyBufferingStateChanged(itemObj, state, bufferedPositionMs,
                        eventTimeMs, positionMs);
            }
        });
    }

    @Override
    public void onPlaylistChanged(int seq, final ParcelImplListSlice listSlice,
            final ParcelImpl metadata, final int currentIdx, final int previousIdx,
            final int nextIdx) {
        if (metadata == null) {
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                List<MediaItem> playlist =
                        MediaUtils.convertParcelImplListSliceToMediaItemList(listSlice);
                controller.notifyPlaylistChanges(playlist,
                        (MediaMetadata) MediaParcelUtils.fromParcelable(metadata), currentIdx,
                        previousIdx, nextIdx);
            }
        });
    }

    @Override
    public void onPlaylistMetadataChanged(int seq, final ParcelImpl metadata)
            throws RuntimeException {
        if (metadata == null) {
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                controller.notifyPlaylistMetadataChanges(
                        (MediaMetadata) MediaParcelUtils.fromParcelable(metadata));
            }
        });
    }

    @Override
    public void onRepeatModeChanged(int seq, final int repeatMode, final int currentIdx,
            final int previousIdx, final int nextIdx) {
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                controller.notifyRepeatModeChanges(repeatMode, currentIdx, previousIdx, nextIdx);
            }
        });
    }

    @Override
    public void onShuffleModeChanged(int seq, final int shuffleMode, final int currentIdx,
            final int previousIdx, final int nextIdx) {
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                controller.notifyShuffleModeChanges(shuffleMode, currentIdx, previousIdx, nextIdx);
            }
        });
    }

    @Override
    public void onPlaybackCompleted(int seq) {
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                controller.notifyPlaybackCompleted();
            }
        });
    }

    @Override
    public void onPlaybackInfoChanged(int seq, final ParcelImpl playbackInfo)
            throws RuntimeException {
        if (playbackInfo == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "onPlaybackInfoChanged");
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                MediaController.PlaybackInfo info = MediaParcelUtils.fromParcelable(playbackInfo);
                if (info == null) {
                    Log.w(TAG, "onPlaybackInfoChanged(): Ignoring null playbackInfo");
                    return;
                }
                controller.notifyPlaybackInfoChanges(info);
            }
        });
    }

    @Override
    public void onSeekCompleted(int seq, final long eventTimeMs, final long positionMs,
            final long seekPositionMs) {
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                controller.notifySeekCompleted(eventTimeMs, positionMs, seekPositionMs);
            }
        });
    }

    @Override
    public void onVideoSizeChanged(int seq, final ParcelImpl item, final ParcelImpl videoSize) {
        if (videoSize == null) {
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                VideoSize size = MediaParcelUtils.fromParcelable(videoSize);
                if (size == null) {
                    Log.w(TAG, "onVideoSizeChanged(): Ignoring null VideoSize");
                    return;
                }
                controller.notifyVideoSizeChanged(size);
            }
        });
    }

    @Override
    public void onSubtitleData(int seq, final ParcelImpl item, final ParcelImpl track,
            final ParcelImpl data) {
        if (item == null || track == null || data == null) {
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                MediaItem itemObj = MediaParcelUtils.fromParcelable(item);
                if (itemObj == null) {
                    Log.w(TAG, "onSubtitleData(): Ignoring null MediaItem");
                    return;
                }
                TrackInfo trackObj = MediaParcelUtils.fromParcelable(track);
                if (trackObj == null) {
                    Log.w(TAG, "onSubtitleData(): Ignoring null TrackInfo");
                    return;
                }
                SubtitleData dataObj = MediaParcelUtils.fromParcelable(data);
                if (dataObj == null) {
                    Log.w(TAG, "onSubtitleData(): Ignoring null SubtitleData");
                    return;
                }
                controller.notifySubtitleData(itemObj, trackObj, dataObj);
            }
        });
    }
    @Override
    public void onConnected(int seq, ParcelImpl connectionResult) {
        if (connectionResult == null) {
            // disconnected
            onDisconnected(seq);
            return;
        }
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaControllerImplBase controller = mController.get();
            if (controller == null) {
                if (DEBUG) {
                    Log.d(TAG, "onConnected after MediaController.close()");
                }
                return;
            }
            ConnectionResult result = MediaParcelUtils.fromParcelable(connectionResult);
            List<MediaItem> itemList =
                    MediaUtils.convertParcelImplListSliceToMediaItemList(result.getPlaylistSlice());
            controller.onConnectedNotLocked(result.getVersion(), result.getSessionStub(),
                    result.getAllowedCommands(), result.getPlayerState(),
                    result.getCurrentMediaItem(), result.getPositionEventTimeMs(),
                    result.getPositionMs(), result.getPlaybackSpeed(),
                    result.getBufferedPositionMs(), result.getPlaybackInfo(),
                    result.getRepeatMode(), result.getShuffleMode(), itemList,
                    result.getSessionActivity(), result.getCurrentMediaItemIndex(),
                    result.getPreviousMediaItemIndex(), result.getNextMediaItemIndex(),
                    result.getTokenExtras(), result.getVideoSize(), result.getTracks(),
                    result.getSelectedVideoTrack(), result.getSelectedAudioTrack(),
                    result.getSelectedSubtitleTrack(), result.getSelectedMetadataTrack(),
                    result.getPlaylistMetadata(), result.getBufferingState());
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onDisconnected(int seq) {
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaControllerImplBase controller = mController.get();
            if (controller == null) {
                if (DEBUG) {
                    Log.d(TAG, "onDisconnected after MediaController.close()");
                }
                return;
            }
            controller.mInstance.close();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onSetCustomLayout(final int seq, final List<ParcelImpl> commandButtonList) {
        if (commandButtonList == null) {
            Log.w(TAG, "setCustomLayout(): Ignoring null commandButtonList");
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                List<MediaSession.CommandButton> layout = new ArrayList<>();
                for (int i = 0; i < commandButtonList.size(); i++) {
                    MediaSession.CommandButton button =
                            MediaParcelUtils.fromParcelable(commandButtonList.get(i));
                    if (button != null) {
                        layout.add(button);
                    }
                }
                controller.onSetCustomLayout(seq, layout);
            }
        });
    }

    @Override
    public void onAllowedCommandsChanged(int seq, final ParcelImpl commands) {
        if (commands == null) {
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                SessionCommandGroup commandGroup = MediaParcelUtils.fromParcelable(commands);
                if (commandGroup == null) {
                    Log.w(TAG, "onAllowedCommandsChanged(): Ignoring null commands");
                    return;
                }
                controller.onAllowedCommandsChanged(commandGroup);
            }
        });
    }

    @Override
    public void onCustomCommand(final int seq, final ParcelImpl commandParcel, final Bundle args) {
        if (commandParcel == null) {
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                SessionCommand command = MediaParcelUtils.fromParcelable(commandParcel);
                if (command == null) {
                    Log.w(TAG, "sendCustomCommand(): Ignoring null command");
                    return;
                }
                controller.onCustomCommand(seq, command, args);
            }
        });
    }

    @Override
    public void onTrackInfoChanged(final int seq, final List<ParcelImpl> trackInfoList,
            final ParcelImpl selectedVideoParcel, final ParcelImpl selectedAudioParcel,
            final ParcelImpl selectedSubtitleParcel, final ParcelImpl selectedMetadataParcel) {
        if (trackInfoList == null) {
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                List<TrackInfo> trackInfos = MediaParcelUtils.fromParcelableList(trackInfoList);
                TrackInfo selectedVideoTrack = MediaParcelUtils.fromParcelable(selectedVideoParcel);
                TrackInfo selectedAudioTrack = MediaParcelUtils.fromParcelable(selectedAudioParcel);
                TrackInfo selectedSubtitleTrack =
                        MediaParcelUtils.fromParcelable(selectedSubtitleParcel);
                TrackInfo selectedMetadataTrack =
                        MediaParcelUtils.fromParcelable(selectedMetadataParcel);
                controller.notifyTracksChanged(seq, trackInfos, selectedVideoTrack,
                        selectedAudioTrack, selectedSubtitleTrack, selectedMetadataTrack);
            }
        });
    }

    @Override
    public void onTrackSelected(final int seq, final ParcelImpl trackInfoParcel) {
        if (trackInfoParcel == null) {
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                TrackInfo trackInfo = MediaParcelUtils.fromParcelable(trackInfoParcel);
                if (trackInfo == null) {
                    Log.w(TAG, "onTrackSelected(): Ignoring null track info");
                    return;
                }
                controller.notifyTrackSelected(seq, trackInfo);
            }
        });
    }

    @Override
    public void onTrackDeselected(final int seq, final ParcelImpl trackInfoParcel) {
        if (trackInfoParcel == null) {
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                TrackInfo trackInfo = MediaParcelUtils.fromParcelable(trackInfoParcel);
                if (trackInfo == null) {
                    Log.w(TAG, "onTrackSelected(): Ignoring null track info");
                    return;
                }
                controller.notifyTrackDeselected(seq, trackInfo);
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // MediaBrowser specific
    ////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onSearchResultChanged(int seq, final String query, final int itemCount,
            final ParcelImpl libraryParams) throws RuntimeException {
        if (libraryParams == null) {
            return;
        }
        if (TextUtils.isEmpty(query)) {
            Log.w(TAG, "onSearchResultChanged(): Ignoring empty query");
            return;
        }
        if (itemCount < 0) {
            Log.w(TAG, "onSearchResultChanged(): Ignoring negative itemCount: " + itemCount);
            return;
        }
        dispatchBrowserTask(new BrowserTask() {
            @Override
            public void run(MediaBrowserImplBase browser) {
                browser.notifySearchResultChanged(query, itemCount,
                        (LibraryParams) MediaParcelUtils.fromParcelable(libraryParams));
            }
        });
    }

    @Override
    public void onChildrenChanged(int seq, final String parentId, final int itemCount,
            final ParcelImpl libraryParams) {
        if (libraryParams == null) {
            return;
        }
        if (TextUtils.isEmpty(parentId)) {
            Log.w(TAG, "onChildrenChanged(): Ignoring empty parentId");
            return;
        }
        if (itemCount < 0) {
            Log.w(TAG, "onChildrenChanged(): Ignoring negative itemCount: " + itemCount);
            return;
        }
        dispatchBrowserTask(new BrowserTask() {
            @Override
            public void run(MediaBrowserImplBase browser) {
                browser.notifyChildrenChanged(parentId, itemCount,
                        (LibraryParams) MediaParcelUtils.fromParcelable(libraryParams));
            }
        });
    }

    public void destroy() {
        mController.clear();
    }

    private void dispatchControllerTask(ControllerTask task) {
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaControllerImplBase controller = mController.get();
            if (controller == null || !controller.isConnected()) {
                return;
            }
            task.run(controller);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void dispatchBrowserTask(BrowserTask task) {
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaControllerImplBase browser = mController.get();
            if (!(browser instanceof MediaBrowserImplBase) || !browser.isConnected()) {
                return;
            }
            task.run((MediaBrowserImplBase) browser);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @FunctionalInterface
    private interface ControllerTask {
        void run(MediaControllerImplBase controller);
    }

    @FunctionalInterface
    private interface BrowserTask {
        void run(MediaBrowserImplBase browser);
    }
}
