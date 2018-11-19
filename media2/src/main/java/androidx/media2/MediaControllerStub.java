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

package androidx.media2;

import android.os.Binder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.media2.MediaController.PlaybackInfo;
import androidx.media2.MediaLibraryService.LibraryParams;
import androidx.media2.MediaSession.CommandButton;
import androidx.media2.SessionPlayer.BuffState;
import androidx.versionedparcelable.ParcelImpl;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class MediaControllerStub extends IMediaController.Stub {
    private static final String TAG = "MediaControllerStub";
    private static final boolean DEBUG = true; // TODO(jaewan): Change

    private final WeakReference<MediaControllerImplBase> mController;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final SequencedFutureManager mSequencedFutureManager;

    MediaControllerStub(MediaControllerImplBase controller, SequencedFutureManager manager) {
        mController = new WeakReference<>(controller);
        mSequencedFutureManager = manager;
    }

    @Override
    public void onSessionResult(final int seq, final ParcelImpl sessionResult) {
        if (sessionResult == null) {
            return;
        }
        dispatchControllerTask(new ControllerTask() {
            @Override
            public void run(MediaControllerImplBase controller) {
                SessionResult result = MediaUtils.fromParcelable(sessionResult);
                if (result == null) {
                    return;
                }
                mSequencedFutureManager.setFutureResult(seq, result);
            }
        });
    }

    @Override
    public void onLibraryResult(final int seq, final ParcelImpl libraryResult) {
        if (libraryResult == null) {
            return;
        }
        dispatchBrowserTask(new BrowserTask() {
            @Override
            public void run(MediaBrowserImplBase browser) {
                LibraryResult result = MediaUtils.fromParcelable(libraryResult);
                if (result == null) {
                    return;
                }
                mSequencedFutureManager.setFutureResult(seq, result);
            }
        });
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
                        (MediaItem) MediaUtils.fromParcelable(item), currentIdx, previousIdx,
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
                MediaItem itemObj = MediaUtils.fromParcelable(item);
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
                        (MediaMetadata) MediaUtils.fromParcelable(metadata), currentIdx,
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
                        (MediaMetadata) MediaUtils.fromParcelable(metadata));
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
                PlaybackInfo info = MediaUtils.fromParcelable(playbackInfo);
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
                    Log.d(TAG, "onConnected after MediaController2.close()");
                }
                return;
            }
            ConnectionResult result = MediaUtils.fromParcelable(connectionResult);
            List<MediaItem> itemList =
                    MediaUtils.convertParcelImplListSliceToMediaItemList(result.getPlaylistSlice());
            controller.onConnectedNotLocked(result.getSessionStub(),
                    result.getAllowedCommands(), result.getPlayerState(),
                    result.getCurrentMediaItem(), result.getPositionEventTimeMs(),
                    result.getPositionMs(), result.getPlaybackSpeed(),
                    result.getBufferedPositionMs(), result.getPlaybackInfo(),
                    result.getRepeatMode(), result.getShuffleMode(), itemList,
                    result.getSessionActivity(), result.getCurrentMediaItemIndex(),
                    result.getPreviousMediaItemIndex(), result.getNextMediaItemIndex());
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
                    Log.d(TAG, "onDisconnected after MediaController2.close()");
                }
                return;
            }
            controller.getInstance().close();
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
                List<CommandButton> layout = new ArrayList<>();
                for (int i = 0; i < commandButtonList.size(); i++) {
                    CommandButton button = MediaUtils.fromParcelable(commandButtonList.get(i));
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
                SessionCommandGroup commandGroup = MediaUtils.fromParcelable(commands);
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
                SessionCommand command = MediaUtils.fromParcelable(commandParcel);
                if (command == null) {
                    Log.w(TAG, "sendCustomCommand(): Ignoring null command");
                    return;
                }
                controller.onCustomCommand(seq, command, args);
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
                        (LibraryParams) MediaUtils.fromParcelable(libraryParams));
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
                        (LibraryParams) MediaUtils.fromParcelable(libraryParams));
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
