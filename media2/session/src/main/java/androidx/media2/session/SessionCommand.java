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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Surface;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.core.util.ObjectsCompat;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.Rating;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.media2.session.MediaSession.SessionCallback;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Define a command that a {@link MediaController} can send to a {@link MediaSession}.
 * <p>
 * If {@link #getCommandCode()} isn't {@link #COMMAND_CODE_CUSTOM}), it's predefined command.
 * If {@link #getCommandCode()} is {@link #COMMAND_CODE_CUSTOM}), it's custom command and
 * {@link #getCustomAction()} shouldn't be {@code null}.
 */
@VersionedParcelize
public final class SessionCommand implements VersionedParcelable {
    /**
     * The first version of session commands. This version is for commands introduced in
     * AndroidX 1.0.0.
     * <p>
     * This would be used to specify which commands should be added by
     * {@link SessionCommandGroup.Builder#addAllPredefinedCommands(int)}
     *
     * @see SessionCommandGroup.Builder#addAllPredefinedCommands(int)
     */
    public static final int COMMAND_VERSION_1 = 1;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final int COMMAND_VERSION_CURRENT = COMMAND_VERSION_1;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({COMMAND_VERSION_1})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CommandVersion {}

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({COMMAND_CODE_CUSTOM,
            COMMAND_CODE_PLAYER_PLAY,
            COMMAND_CODE_PLAYER_PAUSE,
            COMMAND_CODE_PLAYER_PREPARE,
            COMMAND_CODE_PLAYER_SEEK_TO,
            COMMAND_CODE_PLAYER_SET_SPEED,
            COMMAND_CODE_PLAYER_GET_PLAYLIST,
            COMMAND_CODE_PLAYER_SET_PLAYLIST,
            COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM,
            COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM,
            COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM,
            COMMAND_CODE_PLAYER_SET_SHUFFLE_MODE,
            COMMAND_CODE_PLAYER_SET_REPEAT_MODE,
            COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA,
            COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM,
            COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM,
            COMMAND_CODE_PLAYER_REPLACE_PLAYLIST_ITEM,
            COMMAND_CODE_PLAYER_GET_CURRENT_MEDIA_ITEM,
            COMMAND_CODE_PLAYER_UPDATE_LIST_METADATA,
            COMMAND_CODE_PLAYER_SET_MEDIA_ITEM,
            COMMAND_CODE_PLAYER_SET_SURFACE,
            COMMAND_CODE_PLAYER_SELECT_TRACK,
            COMMAND_CODE_PLAYER_DESELECT_TRACK,
            COMMAND_CODE_VOLUME_SET_VOLUME,
            COMMAND_CODE_VOLUME_ADJUST_VOLUME,
            COMMAND_CODE_SESSION_FAST_FORWARD,
            COMMAND_CODE_SESSION_REWIND,
            COMMAND_CODE_SESSION_SKIP_FORWARD,
            COMMAND_CODE_SESSION_SKIP_BACKWARD,
            COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID,
            COMMAND_CODE_SESSION_PLAY_FROM_SEARCH,
            COMMAND_CODE_SESSION_PLAY_FROM_URI,
            COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID,
            COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH,
            COMMAND_CODE_SESSION_PREPARE_FROM_URI,
            COMMAND_CODE_SESSION_SET_RATING,
            COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT,
            COMMAND_CODE_LIBRARY_SUBSCRIBE,
            COMMAND_CODE_LIBRARY_UNSUBSCRIBE,
            COMMAND_CODE_LIBRARY_GET_CHILDREN,
            COMMAND_CODE_LIBRARY_GET_ITEM,
            COMMAND_CODE_LIBRARY_SEARCH,
            COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CommandCode {}

    /**
     * Command code for the custom command which can be defined by string action in the
     * {@link SessionCommand}.
     */
    public static final int COMMAND_CODE_CUSTOM = 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Player commands (i.e. commands to {@link SessionPlayer})
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static final ArrayMap<Integer, Range> VERSION_PLAYER_BASIC_COMMANDS_MAP = new ArrayMap<>();
    static final ArrayMap<Integer, Range> VERSION_PLAYER_PLAYLIST_COMMANDS_MAP = new ArrayMap<>();
    static final ArrayMap<Integer, Range> VERSION_PLAYER_HIDDEN_COMMANDS_MAP = new ArrayMap<>();

    /**
     * Command code for {@link MediaController#play()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo,
     * SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_PLAY = 10000;

    /**
     * Command code for {@link MediaController#pause()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo,
     * SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_PAUSE = 10001;

    /**
     * Command code for {@link MediaController#prepare()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo,
     * SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_PREPARE = 10002;

    /**
     * Command code for {@link MediaController#seekTo(long)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo,
     * SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_SEEK_TO = 10003;

    /**
     * Command code for {@link MediaController#setPlaybackSpeed(float)}}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo,
     * SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_SET_SPEED = 10004;

    /**
     * Command code for {@link MediaController#getPlaylist()}. This will expose metadata
     * information to the controller.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_GET_PLAYLIST = 10005;

    /**
     * Command code for {@link MediaController#setPlaylist(List, MediaMetadata)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_SET_PLAYLIST = 10006;

    /**
     * Command code for {@link MediaController#skipToPlaylistItem(int)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM = 10007;

    /**
     * Command code for {@link MediaController#skipToPreviousPlaylistItem()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(
     * MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM = 10008;

    /**
     * Command code for {@link MediaController#skipToNextPlaylistItem()}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the {@link SessionCallback#onCommandRequest(
     * MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */

    public static final int COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM = 10009;

    /**
     * Command code for {@link MediaController#setShuffleMode(int)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_SET_SHUFFLE_MODE = 10010;

    /**
     * Command code for {@link MediaController#setRepeatMode(int)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_SET_REPEAT_MODE = 10011;

    /**
     * Command code for {@link MediaController#getPlaylistMetadata()}. This will expose metadata
     * information to the controller.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA = 10012;

    /**
     * Command code for {@link MediaController#addPlaylistItem(int, String)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM = 10013;

    /**
     * Command code for {@link MediaController#removePlaylistItem(int)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM = 10014;

    /**
     * Command code for {@link MediaController#replacePlaylistItem(int, String)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_REPLACE_PLAYLIST_ITEM = 10015;

    /**
     * Command code for {@link MediaController#getCurrentMediaItem()}. This will expose metadata
     * information to the controller.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_GET_CURRENT_MEDIA_ITEM = 10016;

    /**
     * Command code for {@link MediaController#updatePlaylistMetadata(MediaMetadata)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_UPDATE_LIST_METADATA = 10017;

    /**
     * Command code for {@link MediaController#setMediaItem(String)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_PLAYER_SET_MEDIA_ITEM = 10018;

    /**
     * Command code for {@link MediaController#setSurface(Surface)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int COMMAND_CODE_PLAYER_SET_SURFACE = 11000;

    /**
     * Command code for {@link MediaController#selectTrack(SessionPlayer.TrackInfo)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int COMMAND_CODE_PLAYER_SELECT_TRACK = 11001;

    /**
     * Command code for {@link MediaController#deselectTrack(SessionPlayer.TrackInfo)}.
     * <p>
     * Command would be sent directly to the player if the session doesn't reject the request
     * through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int COMMAND_CODE_PLAYER_DESELECT_TRACK = 11002;

    static {
        VERSION_PLAYER_BASIC_COMMANDS_MAP.put(COMMAND_VERSION_1,
                new Range(COMMAND_CODE_PLAYER_PLAY, COMMAND_CODE_PLAYER_SET_SPEED));
    }

    static {
        VERSION_PLAYER_PLAYLIST_COMMANDS_MAP.put(COMMAND_VERSION_1,
                new Range(COMMAND_CODE_PLAYER_GET_PLAYLIST,
                        COMMAND_CODE_PLAYER_SET_MEDIA_ITEM));
    }

    static {
        VERSION_PLAYER_HIDDEN_COMMANDS_MAP.put(COMMAND_VERSION_1,
                new Range(COMMAND_CODE_PLAYER_SET_SURFACE, COMMAND_CODE_PLAYER_DESELECT_TRACK));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Volume commands (i.e. commands to {@link AudioManager} or {@link RouteMediaPlayer})
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static final ArrayMap<Integer, Range> VERSION_VOLUME_COMMANDS_MAP = new ArrayMap<>();

    /**
     * Command code for {@link MediaController#setVolumeTo(int, int)}.
     * <p>
     * <p>
     * If the session doesn't reject the request through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)},
     * command would adjust the device volume. It would send to the player directly only if it's
     * remote player. See RouteMediaPlayer for a remote player.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_VOLUME_SET_VOLUME = 30000;

    /**
     * Command code for {@link MediaController#adjustVolume(int, int)}.
     * <p>
     * If the session doesn't reject the request through the
     * {@link SessionCallback#onCommandRequest(MediaSession, ControllerInfo, SessionCommand)},
     * command would adjust the device volume. It would send to the player directly only if it's
     * remote player. See RouteMediaPlayer for a remote player.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_VOLUME_ADJUST_VOLUME = 30001;

    static {
        VERSION_VOLUME_COMMANDS_MAP.put(COMMAND_VERSION_1,
                new Range(COMMAND_CODE_VOLUME_SET_VOLUME,
                        COMMAND_CODE_VOLUME_ADJUST_VOLUME));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Session commands (i.e. commands to {@link MediaSession#SessionCallback})
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static final ArrayMap<Integer, Range> VERSION_SESSION_COMMANDS_MAP = new ArrayMap<>();

    /**
     * Command code for {@link MediaController#fastForward()}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_SESSION_FAST_FORWARD = 40000;

    /**
     * Command code for {@link MediaController#rewind()}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_SESSION_REWIND = 40001;

    /**
     * Command code for {@link MediaController#skipForward()}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_SESSION_SKIP_FORWARD = 40002;

    /**
     * Command code for {@link MediaController#skipBackward()}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_SESSION_SKIP_BACKWARD = 40003;

    /**
     * Command code for {@link MediaController#playFromMediaId(String, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final int COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID = 40004;

    /**
     * Command code for {@link MediaController#playFromSearch(String, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final int COMMAND_CODE_SESSION_PLAY_FROM_SEARCH = 40005;

    /**
     * Command code for {@link MediaController#playFromUri(Uri, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final int COMMAND_CODE_SESSION_PLAY_FROM_URI = 40006;

    /**
     * Command code for {@link MediaController#prepareFromMediaId(String, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final int COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID = 40007;

    /**
     * Command code for {@link MediaController#prepareFromSearch(String, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final int COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH = 40008;

    /**
     * Command code for {@link MediaController#prepareFromUri(Uri, Bundle)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final int COMMAND_CODE_SESSION_PREPARE_FROM_URI = 40009;

    /**
     * Command code for {@link MediaController#setRating(String, Rating)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_SESSION_SET_RATING = 40010;

    static {
        VERSION_SESSION_COMMANDS_MAP.put(COMMAND_VERSION_1,
                new Range(COMMAND_CODE_SESSION_FAST_FORWARD, COMMAND_CODE_SESSION_SET_RATING));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Session commands (i.e. commands to {@link MediaLibrarySession#MediaLibrarySessionCallback})
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static final ArrayMap<Integer, Range> VERSION_LIBRARY_COMMANDS_MAP = new ArrayMap<>();

    /**
     * Command code for {@link MediaBrowser#getLibraryRoot(LibraryParams)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT = 50000;

    /**
     * Command code for {@link MediaBrowser#subscribe(String, LibraryParams)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_LIBRARY_SUBSCRIBE = 50001;

    /**
     * Command code for {@link MediaBrowser#unsubscribe(String)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_LIBRARY_UNSUBSCRIBE = 50002;

    /**
     * Command code for {@link MediaBrowser#getChildren(String, int, int, LibraryParams)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_LIBRARY_GET_CHILDREN = 50003;

    /**
     * Command code for {@link MediaBrowser#getItem(String)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_LIBRARY_GET_ITEM = 50004;

    /**
     * Command code for {@link MediaBrowser#search(String, LibraryParams)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_LIBRARY_SEARCH = 50005;

    /**
     * Command code for {@link MediaBrowser#getSearchResult(String, int, int, LibraryParams)}.
     * <p>
     * Code version is {@link #COMMAND_VERSION_1}.
     */
    public static final int COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT = 50006;

    static {
        VERSION_LIBRARY_COMMANDS_MAP.put(COMMAND_VERSION_1,
                new Range(COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT,
                        COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT));
    }

    @ParcelField(1)
    @CommandCode int mCommandCode;
    // Nonnull if it's custom command
    @ParcelField(2)
    String mCustomAction;
    @ParcelField(3)
    Bundle mCustomExtras;

    /**
     * Used for VersionedParcelable.
     */
    SessionCommand() {
    }

    /**
     * Constructor for creating a predefined command.
     *
     * @param commandCode A command code for predefined command.
     */
    public SessionCommand(@CommandCode int commandCode) {
        if (commandCode == COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("commandCode shouldn't be COMMAND_CODE_CUSTOM");
        }
        mCommandCode = commandCode;
        mCustomAction = null;
        mCustomExtras = null;
    }

    /**
     * Constructor for creating a custom command.
     *
     * @param action The action of this custom command.
     * @param extras An extra bundle for this custom command.
     */
    public SessionCommand(@NonNull String action, @Nullable Bundle extras) {
        if (action == null) {
            throw new NullPointerException("action shouldn't be null");
        }
        mCommandCode = COMMAND_CODE_CUSTOM;
        mCustomAction = action;
        mCustomExtras = extras;
    }

    /**
     * Gets the command code of a predefined command.
     * This will return {@link #COMMAND_CODE_CUSTOM} for a custom command.
     */
    @CommandCode
    public int getCommandCode() {
        return mCommandCode;
    }

    /**
     * Gets the action of a custom command.
     * This will return {@code null} for a predefined command.
     */
    @Nullable
    public String getCustomAction() {
        return mCustomAction;
    }

    /**
     * Gets the extra bundle of a custom command.
     * This will return {@code null} for a predefined command.
     */
    @Nullable
    public Bundle getCustomExtras() {
        return mCustomExtras;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SessionCommand)) {
            return false;
        }
        SessionCommand other = (SessionCommand) obj;
        return mCommandCode == other.mCommandCode
                && TextUtils.equals(mCustomAction, other.mCustomAction);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mCustomAction, mCommandCode);
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
