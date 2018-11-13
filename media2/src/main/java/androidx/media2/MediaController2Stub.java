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

import android.app.PendingIntent;
import android.os.Binder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.media2.MediaBrowser2.BrowserResult;
import androidx.media2.MediaController2.ControllerResult;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaLibraryService2.LibraryParams;
import androidx.media2.MediaLibraryService2.LibraryResult;
import androidx.media2.MediaSession2.CommandButton;
import androidx.media2.MediaSession2.SessionResult;
import androidx.media2.SessionPlayer2.BuffState;
import androidx.versionedparcelable.ParcelImpl;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class MediaController2Stub extends IMediaController2.Stub {
    private static final String TAG = "MediaController2Stub";
    private static final boolean DEBUG = true; // TODO(jaewan): Change

    private final WeakReference<MediaController2ImplBase> mController;
    private final SequencedFutureManager mSequencedFutureManager;

    MediaController2Stub(MediaController2ImplBase controller, SequencedFutureManager manager) {
        mController = new WeakReference<>(controller);
        mSequencedFutureManager = manager;
    }

    @Override
    public void onSessionResult(int seq, ParcelImpl sessionResult) {
        if (sessionResult == null) {
            return;
        }
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            SessionResult result = MediaUtils2.fromParcelable(sessionResult);
            if (result == null) {
                return;
            }
            mSequencedFutureManager.setFutureResult(seq, ControllerResult.from(result));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onLibraryResult(int seq, ParcelImpl libraryResult) {
        if (libraryResult == null) {
            return;
        }
        final long token = Binder.clearCallingIdentity();
        try {
            try {
                final MediaBrowser2 browser = getBrowser();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            LibraryResult result = MediaUtils2.fromParcelable(libraryResult);
            if (result == null) {
                return;
            }
            mSequencedFutureManager.setFutureResult(seq, BrowserResult.from(result));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onCurrentMediaItemChanged(ParcelImpl item) {
        if (item == null) {
            return;
        }
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            controller.notifyCurrentMediaItemChanged((MediaItem2) MediaUtils2.fromParcelable(item));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onPlayerStateChanged(long eventTimeMs, long positionMs, int state) {
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            controller.notifyPlayerStateChanges(eventTimeMs, positionMs, state);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed) {
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            controller.notifyPlaybackSpeedChanges(eventTimeMs, positionMs, speed);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onBufferingStateChanged(ParcelImpl item, @BuffState int state,
            long bufferedPositionMs) {
        if (item == null) {
            return;
        }
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            MediaItem2 item2 = MediaUtils2.fromParcelable(item);
            if (item2 == null) {
                Log.w(TAG, "onBufferingStateChanged(): Ignoring null item");
                return;
            }
            controller.notifyBufferingStateChanged(item2, state, bufferedPositionMs);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onPlaylistChanged(ParcelImplListSlice listSlice, ParcelImpl metadata) {
        if (metadata == null) {
            return;
        }
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            List<MediaItem2> playlist =
                    MediaUtils2.convertParcelImplListSliceToMediaItem2List(listSlice);
            controller.notifyPlaylistChanges(playlist,
                    (MediaMetadata2) MediaUtils2.fromParcelable(metadata));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onPlaylistMetadataChanged(ParcelImpl metadata) throws RuntimeException {
        if (metadata == null) {
            return;
        }
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            controller.notifyPlaylistMetadataChanges(
                    (MediaMetadata2) MediaUtils2.fromParcelable(metadata));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            controller.notifyRepeatModeChanges(repeatMode);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onShuffleModeChanged(int shuffleMode) {
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            controller.notifyShuffleModeChanges(shuffleMode);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onPlaybackCompleted() {
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            controller.notifyPlaybackCompleted();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onPlaybackInfoChanged(ParcelImpl playbackInfo) throws RuntimeException {
        if (playbackInfo == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "onPlaybackInfoChanged");
        }
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            PlaybackInfo info = MediaUtils2.fromParcelable(playbackInfo);
            if (info == null) {
                Log.w(TAG, "onPlaybackInfoChanged(): Ignoring null playbackInfo");
                return;
            }
            controller.notifyPlaybackInfoChanges(info);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onSeekCompleted(long eventTimeMs, long positionMs, long seekPositionMs) {
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            controller.notifySeekCompleted(eventTimeMs, positionMs, seekPositionMs);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onConnected(IMediaSession2 sessionBinder, ParcelImpl commandGroup, int playerState,
            ParcelImpl currentItem, long positionEventTimeMs, long positionMs, float playbackSpeed,
            long bufferedPositionMs, ParcelImpl playbackInfo, int shuffleMode, int repeatMode,
            ParcelImplListSlice listSlice, PendingIntent sessionActivity) {
        if (sessionBinder == null || commandGroup == null || currentItem == null
                || playbackInfo == null) {
            return;
        }
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller = mController.get();
            if (controller == null) {
                if (DEBUG) {
                    Log.d(TAG, "onConnected after MediaController2.close()");
                }
                return;
            }
            List<MediaItem2> itemList =
                    MediaUtils2.convertParcelImplListSliceToMediaItem2List(listSlice);
            controller.onConnectedNotLocked(sessionBinder,
                    (SessionCommandGroup2) MediaUtils2.fromParcelable(commandGroup), playerState,
                    (MediaItem2) MediaUtils2.fromParcelable(currentItem),
                    positionEventTimeMs, positionMs, playbackSpeed, bufferedPositionMs,
                    (PlaybackInfo) MediaUtils2.fromParcelable(playbackInfo), repeatMode,
                    shuffleMode,
                    itemList, sessionActivity);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onDisconnected() {
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller = mController.get();
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
    public void onSetCustomLayout(int seq, List<ParcelImpl> commandButtonList) {
        if (commandButtonList == null) {
            Log.w(TAG, "setCustomLayout(): Ignoring null commandButtonList");
            return;
        }
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            if (controller == null) {
                // TODO(jaewan): Revisit here. Could be a bug
                return;
            }
            List<CommandButton> layout = new ArrayList<>();
            for (int i = 0; i < commandButtonList.size(); i++) {
                CommandButton button = MediaUtils2.fromParcelable(commandButtonList.get(i));
                if (button != null) {
                    layout.add(button);
                }
            }
            controller.onSetCustomLayout(seq, layout);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onAllowedCommandsChanged(ParcelImpl commands) {
        if (commands == null) {
            return;
        }
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            if (controller == null) {
                // TODO(jaewan): Revisit here. Could be a bug
                return;
            }
            SessionCommandGroup2 commandGroup = MediaUtils2.fromParcelable(commands);
            if (commandGroup == null) {
                Log.w(TAG, "onAllowedCommandsChanged(): Ignoring null commands");
                return;
            }
            controller.onAllowedCommandsChanged(commandGroup);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onCustomCommand(int seq, ParcelImpl commandParcel, Bundle args) {
        if (commandParcel == null) {
            return;
        }
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaController2ImplBase controller;
            try {
                controller = getController();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            SessionCommand2 command = MediaUtils2.fromParcelable(commandParcel);
            if (command == null) {
                Log.w(TAG, "sendCustomCommand(): Ignoring null command");
                return;
            }
            controller.onCustomCommand(seq, command, args);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // MediaBrowser specific
    ////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onSearchResultChanged(final String query, final int itemCount,
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
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaBrowser2 browser;
            try {
                browser = getBrowser();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            if (browser == null) {
                return;
            }
            browser.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    browser.getCallback().onSearchResultChanged(browser, query, itemCount,
                            (LibraryParams) MediaUtils2.fromParcelable(libraryParams));
                }
            });
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onChildrenChanged(final String parentId, final int itemCount,
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
        final long token = Binder.clearCallingIdentity();
        try {
            final MediaBrowser2 browser;
            try {
                browser = getBrowser();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Don't fail silently here. Highly likely a bug");
                return;
            }
            if (browser == null) {
                return;
            }
            browser.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    // TODO (b/118472216): Find all ParcelUtils.fromParcelable usages,
                    // and null check before calling it.
                    browser.getCallback().onChildrenChanged(browser, parentId, itemCount,
                            (LibraryParams) MediaUtils2.fromParcelable(libraryParams));
                }
            });
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void destroy() {
        mController.clear();
    }

    private MediaController2ImplBase getController() throws IllegalStateException {
        final MediaController2ImplBase controller = mController.get();
        if (controller == null) {
            throw new IllegalStateException("Controller is released");
        }
        return controller;
    }

    private MediaBrowser2 getBrowser() throws IllegalStateException {
        final MediaController2ImplBase controller = getController();
        if (controller.getInstance() instanceof MediaBrowser2) {
            return (MediaBrowser2) controller.getInstance();
        }
        return null;
    }
}
