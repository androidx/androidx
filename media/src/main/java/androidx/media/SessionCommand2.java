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

package androidx.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.media.MediaSession2.ControllerInfo;
import androidx.media.MediaSession2.SessionCallback;

import java.util.List;

/**
 * Define a command that a {@link MediaController2} can send to a {@link MediaSession2}.
 * <p>
 * If {@link #getCommandCode()} isn't {@link #COMMAND_CODE_CUSTOM}), it's predefined command.
 * If {@link #getCommandCode()} is {@link #COMMAND_CODE_CUSTOM}), it's custom command and
 * {@link #getCustomCommand()} shouldn't be {@code null}.
 */
public final class SessionCommand2 {
    /**
     * Command code for the custom command which can be defined by string action in the
     * {@link SessionCommand2}.
     */
    public static final int COMMAND_CODE_CUSTOM = 0;

    /**
     * Command code for {@link MediaController2#play()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo,
     * SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYBACK_PLAY = 1;

    /**
     * Command code for {@link MediaController2#pause()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo,
     * SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYBACK_PAUSE = 2;

    /**
     * Command code for {@link MediaController2#reset()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo,
     * SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYBACK_RESET = 3;

    /**
     * Command code for {@link MediaController2#skipToNextItem()}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the {@link SessionCallback#onCommandRequest(
     * MediaSession2, ControllerInfo, SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM = 4;

    /**
     * Command code for {@link MediaController2#skipToPreviousItem()}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the {@link SessionCallback#onCommandRequest(
     * MediaSession2, ControllerInfo, SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM = 5;

    /**
     * Command code for {@link MediaController2#prepare()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo,
     * SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYBACK_PREPARE = 6;

    /**
     * Command code for {@link MediaController2#fastForward()}.
     */
    public static final int COMMAND_CODE_SESSION_FAST_FORWARD = 7;

    /**
     * Command code for {@link MediaController2#rewind()}.
     */
    public static final int COMMAND_CODE_SESSION_REWIND = 8;

    /**
     * Command code for {@link MediaController2#seekTo(long)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo,
     * SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYBACK_SEEK_TO = 9;

    /**
     * Command code for both {@link MediaController2#setVolumeTo(int, int)}.
     * <p>
     * Command would set the device volume or send to the volume provider directly if the session
     * doesn't reject the request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     */
    public static final int COMMAND_CODE_VOLUME_SET_VOLUME = 10;

    /**
     * Command code for both {@link MediaController2#adjustVolume(int, int)}.
     * <p>
     * Command would adjust the device volume or send to the volume provider directly if the session
     * doesn't reject the request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     */
    public static final int COMMAND_CODE_VOLUME_ADJUST_VOLUME = 11;

    /**
     * Command code for {@link MediaController2#skipToPlaylistItem(MediaItem2)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM = 12;

    /**
     * Command code for {@link MediaController2#setShuffleMode(int)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE = 13;

    /**
     * Command code for {@link MediaController2#setRepeatMode(int)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE = 14;

    /**
     * Command code for {@link MediaController2#addPlaylistItem(int, MediaItem2)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYLIST_ADD_ITEM = 15;

    /**
     * Command code for {@link MediaController2#addPlaylistItem(int, MediaItem2)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYLIST_REMOVE_ITEM = 16;

    /**
     * Command code for {@link MediaController2#replacePlaylistItem(int, MediaItem2)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYLIST_REPLACE_ITEM = 17;

    /**
     * Command code for {@link MediaController2#getPlaylist()}. This will expose metadata
     * information to the controller.
     */
    public static final int COMMAND_CODE_PLAYLIST_GET_LIST = 18;

    /**
     * Command code for {@link MediaController2#setPlaylist(List, MediaMetadata2)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYLIST_SET_LIST = 19;

    /**
     * Command code for {@link MediaController2#getPlaylistMetadata()}. This will expose
     * metadata information to the controller.
     */
    public static final int COMMAND_CODE_PLAYLIST_GET_LIST_METADATA = 20;

    /**
     * Command code for {@link MediaController2#updatePlaylistMetadata(MediaMetadata2)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYLIST_SET_LIST_METADATA = 21;

    /**
     * Command code for {@link MediaController2#getCurrentMediaItem()}. This will expose
     * metadata information to the controller.
     */
    public static final int COMMAND_CODE_PLAYLIST_GET_CURRENT_MEDIA_ITEM = 20;

    /**
     * Command code for {@link MediaController2#playFromMediaId(String, Bundle)}.
     */
    public static final int COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID = 22;

    /**
     * Command code for {@link MediaController2#playFromUri(Uri, Bundle)}.
     */
    public static final int COMMAND_CODE_SESSION_PLAY_FROM_URI = 23;

    /**
     * Command code for {@link MediaController2#playFromSearch(String, Bundle)}.
     */
    public static final int COMMAND_CODE_SESSION_PLAY_FROM_SEARCH = 24;

    /**
     * Command code for {@link MediaController2#prepareFromMediaId(String, Bundle)}.
     */
    public static final int COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID = 25;

    /**
     * Command code for {@link MediaController2#prepareFromUri(Uri, Bundle)}.
     */
    public static final int COMMAND_CODE_SESSION_PREPARE_FROM_URI = 26;

    /**
     * Command code for {@link MediaController2#prepareFromSearch(String, Bundle)}.
     */
    public static final int COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH = 27;

    /**
     * Command code for {@link MediaController2#setRating(String, Rating2)}.
     */
    public static final int COMMAND_CODE_SESSION_SET_RATING = 28;

    /**
     * Command code for {@link MediaController2#subscribeRoutesInfo()}
     */
    public static final int COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO = 36;

    /**
     * Command code for {@link MediaController2#unsubscribeRoutesInfo()}
     */
    public static final int COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO = 37;

    /**
     * Command code for {@link MediaController2#selectRoute(Bundle)}}
     */
    public static final int COMMAND_CODE_SESSION_SELECT_ROUTE = 38;

    /**
     * Command code for {@link MediaBrowser2#getChildren(String, int, int, Bundle)}.
     */
    public static final int COMMAND_CODE_LIBRARY_GET_CHILDREN = 29;

    /**
     * Command code for {@link MediaBrowser2#getItem(String)}.
     */
    public static final int COMMAND_CODE_LIBRARY_GET_ITEM = 30;

    /**
     * Command code for {@link MediaBrowser2#getLibraryRoot(Bundle)}.
     */
    public static final int COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT = 31;

    /**
     * Command code for {@link MediaBrowser2#getSearchResult(String, int, int, Bundle)}.
     */
    public static final int COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT = 32;

    /**
     * Command code for {@link MediaBrowser2#search(String, Bundle)}.
     */
    public static final int COMMAND_CODE_LIBRARY_SEARCH = 33;

    /**
     * Command code for {@link MediaBrowser2#subscribe(String, Bundle)}.
     */
    public static final int COMMAND_CODE_LIBRARY_SUBSCRIBE = 34;

    /**
     * Command code for {@link MediaBrowser2#unsubscribe(String)}.
     */
    public static final int COMMAND_CODE_LIBRARY_UNSUBSCRIBE = 35;

    /**
     * Command code for {@link MediaController2#setPlaybackSpeed(float)}}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo,
     * SessionCommand2)}.
     */
    public static final int COMMAND_CODE_PLAYBACK_SET_SPEED = 39;

    private static final String KEY_COMMAND_CODE =
            "android.media.media_session2.command.command_code";
    private static final String KEY_COMMAND_CUSTOM_COMMAND =
            "android.media.media_session2.command.custom_command";
    private static final String KEY_COMMAND_EXTRAS =
            "android.media.media_session2.command.extras";

    private final int mCommandCode;
    // Nonnull if it's custom command
    private final String mCustomCommand;
    private final Bundle mExtras;

    /**
     * Constructor for creating a predefined command.
     *
     * @param commandCode A command code for predefined command.
     */
    public SessionCommand2(int commandCode) {
        if (commandCode == COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("commandCode shouldn't be COMMAND_CODE_CUSTOM");
        }
        mCommandCode = commandCode;
        mCustomCommand = null;
        mExtras = null;
    }

    /**
     * Constructor for creating a custom command.
     *
     * @param action The action of this custom command.
     * @param extras An extra bundle for this custom command.
     */
    public SessionCommand2(@NonNull String action, @Nullable Bundle extras) {
        if (action == null) {
            throw new IllegalArgumentException("action shouldn't be null");
        }
        mCommandCode = COMMAND_CODE_CUSTOM;
        mCustomCommand = action;
        mExtras = extras;
    }

    /**
     * Gets the command code of a predefined command.
     * This will return {@link #COMMAND_CODE_CUSTOM} for a custom command.
     */
    public int getCommandCode() {
        return mCommandCode;
    }

    /**
     * Gets the action of a custom command.
     * This will return {@code null} for a predefined command.
     */
    public @Nullable String getCustomCommand() {
        return mCustomCommand;
    }

    /**
     * Gets the extra bundle of a custom command.
     * This will return {@code null} for a predefined command.
     */
    public @Nullable Bundle getExtras() {
        return mExtras;
    }

    /**
     * @return a new {@link Bundle} instance from the command
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_COMMAND_CODE, mCommandCode);
        bundle.putString(KEY_COMMAND_CUSTOM_COMMAND, mCustomCommand);
        bundle.putBundle(KEY_COMMAND_EXTRAS, mExtras);
        return bundle;
    }

    /**
     * @return a new {@link SessionCommand2} instance from the Bundle
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static SessionCommand2 fromBundle(@NonNull Bundle command) {
        if (command == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        int code = command.getInt(KEY_COMMAND_CODE);
        if (code != COMMAND_CODE_CUSTOM) {
            return new SessionCommand2(code);
        } else {
            String customCommand = command.getString(KEY_COMMAND_CUSTOM_COMMAND);
            if (customCommand == null) {
                return null;
            }
            return new SessionCommand2(customCommand, command.getBundle(KEY_COMMAND_EXTRAS));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SessionCommand2)) {
            return false;
        }
        SessionCommand2 other = (SessionCommand2) obj;
        return mCommandCode == other.mCommandCode
                && TextUtils.equals(mCustomCommand, other.mCustomCommand);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        return ((mCustomCommand != null) ? mCustomCommand.hashCode() : 0) * prime + mCommandCode;
    }
}
