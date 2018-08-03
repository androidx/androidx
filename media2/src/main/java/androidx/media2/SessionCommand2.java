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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.core.util.ObjectsCompat;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.MediaSession2.SessionCallback;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.List;

/**
 * Define a command that a {@link MediaController2} can send to a {@link MediaSession2}.
 * <p>
 * If {@link #getCommandCode()} isn't {@link #COMMAND_CODE_CUSTOM}), it's predefined command.
 * If {@link #getCommandCode()} is {@link #COMMAND_CODE_CUSTOM}), it's custom command and
 * {@link #getCustomCommand()} shouldn't be {@code null}.
 */
@VersionedParcelize
public final class SessionCommand2 implements VersionedParcelable {
    /**
     * The first version of session commands. This version is for commands introduced in
     * AndroidX 1.0.0.
     * <p>
     * This would be used to specify which commands should be added by
     * {@link SessionCommandGroup2.Builder#addAllPredefinedCommands(int)}
     *
     * @see SessionCommandGroup2.Builder#addAllPredefinedCommands(int)
     */
    public static final int COMMAND_VERSION_1 = 1;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final int COMMAND_VERSION_CURRENT = COMMAND_VERSION_1;

    /**
     * Command code for the custom command which can be defined by string action in the
     * {@link SessionCommand2}.
     */
    public static final int COMMAND_CODE_CUSTOM = 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Playback commands (i.e. commands to {@link MediaPlayerConnector})
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static final ArrayMap<Integer, Range> VERSION_PLAYBACK_COMMANDS_MAP = new ArrayMap<>();

    /**
     * Command code for {@link MediaController2#play()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo,
     * SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYBACK_PLAY = 10000;

    /**
     * Command code for {@link MediaController2#pause()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo,
     * SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYBACK_PAUSE = 10001;

    /**
     * Command code for {@link MediaController2#reset()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo,
     * SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYBACK_RESET = 10002;

    /**
     * Command code for {@link MediaController2#prepare()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo,
     * SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYBACK_PREPARE = 10003;

    /**
     * Command code for {@link MediaController2#seekTo(long)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo,
     * SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYBACK_SEEK_TO = 10004;

    /**
     * Command code for {@link MediaController2#setPlaybackSpeed(float)}}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo,
     * SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYBACK_SET_SPEED = 10005;

    static {
        VERSION_PLAYBACK_COMMANDS_MAP.put(COMMAND_VERSION_1,
                new Range(COMMAND_CODE_PLAYBACK_PLAY, COMMAND_CODE_PLAYBACK_SET_SPEED));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Playlist commands (i.e. commands to {@link MediaPlaylistAgent})
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static final ArrayMap<Integer, Range> VERSION_PLAYLIST_COMMANDS_MAP = new ArrayMap<>();

    /**
     * Command code for {@link MediaController2#getPlaylist()}. This will expose metadata
     * information to the controller.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_GET_LIST = 20000;

    /**
     * Command code for {@link MediaController2#setPlaylist(List, MediaMetadata2)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_SET_LIST = 20001;

    /**
     * Command code for {@link MediaController2#skipToPlaylistItem(MediaItem2)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM = 20002;

    /**
     * Command code for {@link MediaController2#skipToPreviousItem()}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the {@link SessionCallback#onCommandRequest(
     * MediaSession2, ControllerInfo, SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM = 20003;

    /**
     * Command code for {@link MediaController2#skipToNextItem()}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the {@link SessionCallback#onCommandRequest(
     * MediaSession2, ControllerInfo, SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM = 20004;

    /**
     * Command code for {@link MediaController2#setShuffleMode(int)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE = 20005;

    /**
     * Command code for {@link MediaController2#setRepeatMode(int)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE = 20006;

    /**
     * Command code for {@link MediaController2#getPlaylistMetadata()}. This will expose
     * metadata information to the controller.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_GET_LIST_METADATA = 20007;

    /**
     * Command code for {@link MediaController2#addPlaylistItem(int, MediaItem2)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_ADD_ITEM = 20008;

    /**
     * Command code for {@link MediaController2#addPlaylistItem(int, MediaItem2)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_REMOVE_ITEM = 20009;

    /**
     * Command code for {@link MediaController2#replacePlaylistItem(int, MediaItem2)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_REPLACE_ITEM = 20010;

    /**
     * Command code for {@link MediaController2#getCurrentMediaItem()}. This will expose
     * metadata information to the controller.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_GET_CURRENT_MEDIA_ITEM = 20011;

    /**
     * Command code for {@link MediaController2#updatePlaylistMetadata(MediaMetadata2)}.
     * <p>
     * Command would be sent directly to the playlist agent if the session doesn't reject the
     * request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYLIST_UPDATE_LIST_METADATA = 20012;

    static {
        VERSION_PLAYLIST_COMMANDS_MAP.put(COMMAND_VERSION_1,
                new Range(COMMAND_CODE_PLAYLIST_GET_LIST,
                        COMMAND_CODE_PLAYLIST_UPDATE_LIST_METADATA));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Volume commands (i.e. commands to {@link AudioManager} or {@link RouteMediaPlayer})
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static final ArrayMap<Integer, Range> VERSION_VOLUME_COMMANDS_MAP = new ArrayMap<>();

    /**
     * Command code for {@link MediaController2#setVolumeTo(int, int)}.
     * <p>
     * <p>
     * If the session doesn't reject the request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)},
     * command would adjust the device volume. It would send to the player directly only if it's
     * remote player. See RouteMediaPlayer for a remote player.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     *
     * @see androidx.mediarouter.media.RouteMediaPlayer#setPlayerVolume()
     */

    public static final int COMMAND_CODE_VOLUME_SET_VOLUME = 30000;

    /**
     * Command code for {@link MediaController2#adjustVolume(int, int)}.
     * <p>
     * If the session doesn't reject the request through the
     * {@link SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)},
     * command would adjust the device volume. It would send to the player directly only if it's
     * remote player. See RouteMediaPlayer for a remote player.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     *
     * @see androidx.mediarouter.media.RouteMediaPlayer#adjustPlayerVolume()
     */

    public static final int COMMAND_CODE_VOLUME_ADJUST_VOLUME = 30001;

    static {
        VERSION_VOLUME_COMMANDS_MAP.put(COMMAND_VERSION_1,
                new Range(COMMAND_CODE_VOLUME_SET_VOLUME,
                        COMMAND_CODE_VOLUME_ADJUST_VOLUME));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Session commands (i.e. commands to {@link MediaSession2#SessionCallback})
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static final ArrayMap<Integer, Range> VERSION_SESSION_COMMANDS_MAP = new ArrayMap<>();

    /**
     * Command code for {@link MediaController2#fastForward()}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_SESSION_FAST_FORWARD = 40000;

    /**
     * Command code for {@link MediaController2#rewind()}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_SESSION_REWIND = 40001;

    /**
     * Command code for {@link MediaController2#playFromMediaId(String, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID = 40002;

    /**
     * Command code for {@link MediaController2#playFromSearch(String, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_SESSION_PLAY_FROM_SEARCH = 40003;

    /**
     * Command code for {@link MediaController2#playFromUri(Uri, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_SESSION_PLAY_FROM_URI = 40004;

    /**
     * Command code for {@link MediaController2#prepareFromMediaId(String, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID = 40005;

    /**
     * Command code for {@link MediaController2#prepareFromSearch(String, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH = 40006;

    /**
     * Command code for {@link MediaController2#prepareFromUri(Uri, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_SESSION_PREPARE_FROM_URI = 40007;

    /**
     * Command code for {@link MediaController2#setRating(String, Rating2)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_SESSION_SET_RATING = 40008;

    /**
     * Command code for {@link MediaController2#subscribeRoutesInfo()}
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO = 40009;

    /**
     * Command code for {@link MediaController2#unsubscribeRoutesInfo()}
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO = 40010;

    /**
     * Command code for {@link MediaController2#selectRoute(Bundle)}}
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_SESSION_SELECT_ROUTE = 40011;

    static {
        VERSION_SESSION_COMMANDS_MAP.put(COMMAND_VERSION_1,
                new Range(COMMAND_CODE_SESSION_FAST_FORWARD,
                        COMMAND_CODE_SESSION_SELECT_ROUTE));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Session commands (i.e. commands to {@link MediaLibrarySession#MediaLibrarySessionCallback})
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static final ArrayMap<Integer, Range> VERSION_LIBRARY_COMMANDS_MAP = new ArrayMap<>();

    /**
     * Command code for {@link MediaBrowser2#getLibraryRoot(Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT = 50000;

    /**
     * Command code for {@link MediaBrowser2#subscribe(String, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_LIBRARY_SUBSCRIBE = 50001;

    /**
     * Command code for {@link MediaBrowser2#unsubscribe(String)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_LIBRARY_UNSUBSCRIBE = 50002;

    /**
     * Command code for {@link MediaBrowser2#getChildren(String, int, int, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_LIBRARY_GET_CHILDREN = 50003;

    /**
     * Command code for {@link MediaBrowser2#getItem(String)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_LIBRARY_GET_ITEM = 50004;

    /**
     * Command code for {@link MediaBrowser2#search(String, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_LIBRARY_SEARCH = 50005;

    /**
     * Command code for {@link MediaBrowser2#getSearchResult(String, int, int, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT = 50006;

    static {
        VERSION_LIBRARY_COMMANDS_MAP.put(COMMAND_VERSION_1,
                new Range(COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT,
                        COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT));
    }

    private static final String KEY_COMMAND_CODE = "android.media.session2.command.command_code";
    private static final String KEY_COMMAND_CUSTOM_COMMAND =
            "android.media.session2.command.custom_command";
    private static final String KEY_COMMAND_EXTRAS = "android.media.session2.command.extras";

    @ParcelField(1)
    int mCommandCode;
    // Nonnull if it's custom command
    @ParcelField(2)
    String mCustomCommand;
    @ParcelField(3)
    Bundle mExtras;

    /**
     * Used for VersionedParcelable.
     */
    SessionCommand2() {
    }

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
        return ObjectsCompat.hash(mCustomCommand, mCommandCode);
    }

    static final class Range {
        public final int lower;
        public final int upper;

        Range(int lower, int upper) {
            this.lower = lower;
            this.upper = upper;
        }
    }
}
