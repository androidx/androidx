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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;

import androidx.annotation.IntDef;
import androidx.annotation.LongDef;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Playback state for a {@link MediaSessionCompat}. This includes a state like
 * {@link PlaybackStateCompat#STATE_PLAYING}, the current playback position,
 * and the current control capabilities.
 */
public final class PlaybackStateCompat implements Parcelable {

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @LongDef(flag=true, value={ACTION_STOP, ACTION_PAUSE, ACTION_PLAY, ACTION_REWIND,
            ACTION_SKIP_TO_PREVIOUS, ACTION_SKIP_TO_NEXT, ACTION_FAST_FORWARD, ACTION_SET_RATING,
            ACTION_SEEK_TO, ACTION_PLAY_PAUSE, ACTION_PLAY_FROM_MEDIA_ID, ACTION_PLAY_FROM_SEARCH,
            ACTION_SKIP_TO_QUEUE_ITEM, ACTION_PLAY_FROM_URI, ACTION_PREPARE,
            ACTION_PREPARE_FROM_MEDIA_ID, ACTION_PREPARE_FROM_SEARCH, ACTION_PREPARE_FROM_URI,
            ACTION_SET_REPEAT_MODE, ACTION_SET_SHUFFLE_MODE, ACTION_SET_CAPTIONING_ENABLED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Actions {}

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @LongDef({ACTION_STOP, ACTION_PAUSE, ACTION_PLAY, ACTION_REWIND, ACTION_SKIP_TO_PREVIOUS,
            ACTION_SKIP_TO_NEXT, ACTION_FAST_FORWARD, ACTION_PLAY_PAUSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaKeyAction {}

    /**
     * Indicates this session supports the stop command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_STOP = 1 << 0;

    /**
     * Indicates this session supports the pause command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_PAUSE = 1 << 1;

    /**
     * Indicates this session supports the play command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_PLAY = 1 << 2;

    /**
     * Indicates this session supports the rewind command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_REWIND = 1 << 3;

    /**
     * Indicates this session supports the previous command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_SKIP_TO_PREVIOUS = 1 << 4;

    /**
     * Indicates this session supports the next command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_SKIP_TO_NEXT = 1 << 5;

    /**
     * Indicates this session supports the fast forward command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_FAST_FORWARD = 1 << 6;

    /**
     * Indicates this session supports the set rating command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_SET_RATING = 1 << 7;

    /**
     * Indicates this session supports the seek to command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_SEEK_TO = 1 << 8;

    /**
     * Indicates this session supports the play/pause toggle command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_PLAY_PAUSE = 1 << 9;

    /**
     * Indicates this session supports the play from media id command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_PLAY_FROM_MEDIA_ID = 1 << 10;

    /**
     * Indicates this session supports the play from search command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_PLAY_FROM_SEARCH = 1 << 11;

    /**
     * Indicates this session supports the skip to queue item command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_SKIP_TO_QUEUE_ITEM = 1 << 12;

    /**
     * Indicates this session supports the play from URI command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_PLAY_FROM_URI = 1 << 13;

    /**
     * Indicates this session supports the prepare command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_PREPARE = 1 << 14;

    /**
     * Indicates this session supports the prepare from media id command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_PREPARE_FROM_MEDIA_ID = 1 << 15;

    /**
     * Indicates this session supports the prepare from search command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_PREPARE_FROM_SEARCH = 1 << 16;

    /**
     * Indicates this session supports the prepare from URI command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_PREPARE_FROM_URI = 1 << 17;

    /**
     * Indicates this session supports the set repeat mode command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_SET_REPEAT_MODE = 1 << 18;

    /**
     * Indicates this session supports the set shuffle mode enabled command.
     *
     * @see Builder#setActions(long)
     * @deprecated Use {@link #ACTION_SET_SHUFFLE_MODE} instead.
     */
    @Deprecated
    public static final long ACTION_SET_SHUFFLE_MODE_ENABLED = 1 << 19;

    /**
     * Indicates this session supports the set captioning enabled command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_SET_CAPTIONING_ENABLED = 1 << 20;

    /**
     * Indicates this session supports the set shuffle mode command.
     *
     * @see Builder#setActions(long)
     */
    public static final long ACTION_SET_SHUFFLE_MODE = 1 << 21;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({STATE_NONE, STATE_STOPPED, STATE_PAUSED, STATE_PLAYING, STATE_FAST_FORWARDING,
            STATE_REWINDING, STATE_BUFFERING, STATE_ERROR, STATE_CONNECTING,
            STATE_SKIPPING_TO_PREVIOUS, STATE_SKIPPING_TO_NEXT, STATE_SKIPPING_TO_QUEUE_ITEM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {}

    /**
     * This is the default playback state and indicates that no media has been
     * added yet, or the performer has been reset and has no content to play.
     *
     * @see Builder#setState
     */
    public final static int STATE_NONE = 0;

    /**
     * State indicating this item is currently stopped.
     *
     * @see Builder#setState
     */
    public final static int STATE_STOPPED = 1;

    /**
     * State indicating this item is currently paused.
     *
     * @see Builder#setState
     */
    public final static int STATE_PAUSED = 2;

    /**
     * State indicating this item is currently playing.
     *
     * @see Builder#setState
     */
    public final static int STATE_PLAYING = 3;

    /**
     * State indicating this item is currently fast forwarding.
     *
     * @see Builder#setState
     */
    public final static int STATE_FAST_FORWARDING = 4;

    /**
     * State indicating this item is currently rewinding.
     *
     * @see Builder#setState
     */
    public final static int STATE_REWINDING = 5;

    /**
     * State indicating this item is currently buffering and will begin playing
     * when enough data has buffered.
     *
     * @see Builder#setState
     */
    public final static int STATE_BUFFERING = 6;

    /**
     * State indicating this item is currently in an error state. The error
     * code should also be set when entering this state.
     *
     * @see Builder#setState
     * @see Builder#setErrorMessage(int, CharSequence)
     */
    public final static int STATE_ERROR = 7;

    /**
     * State indicating the class doing playback is currently connecting to a
     * route. Depending on the implementation you may return to the previous
     * state when the connection finishes or enter {@link #STATE_NONE}. If
     * the connection failed {@link #STATE_ERROR} should be used.
     * <p>
     * On devices earlier than API 21, this will appear as {@link #STATE_BUFFERING}
     * </p>
     *
     * @see Builder#setState
     */
    public final static int STATE_CONNECTING = 8;

    /**
     * State indicating the player is currently skipping to the previous item.
     *
     * @see Builder#setState
     */
    public final static int STATE_SKIPPING_TO_PREVIOUS = 9;

    /**
     * State indicating the player is currently skipping to the next item.
     *
     * @see Builder#setState
     */
    public final static int STATE_SKIPPING_TO_NEXT = 10;

    /**
     * State indicating the player is currently skipping to a specific item in
     * the queue.
     * <p>
     * On devices earlier than API 21, this will appear as {@link #STATE_SKIPPING_TO_NEXT}
     * </p>
     *
     * @see Builder#setState
     */
    public final static int STATE_SKIPPING_TO_QUEUE_ITEM = 11;

    /**
     * Use this value for the position to indicate the position is not known.
     */
    public final static long PLAYBACK_POSITION_UNKNOWN = -1;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({REPEAT_MODE_INVALID, REPEAT_MODE_NONE, REPEAT_MODE_ONE, REPEAT_MODE_ALL,
            REPEAT_MODE_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RepeatMode {}

    /**
     * {@link MediaControllerCompat.TransportControls#getRepeatMode} returns this value
     * when the session is not ready for providing its repeat mode.
     */
    public static final int REPEAT_MODE_INVALID = -1;

    /**
     * Use this value with {@link MediaControllerCompat.TransportControls#setRepeatMode}
     * to indicate that the playback will be stopped at the end of the playing media list.
     */
    public static final int REPEAT_MODE_NONE = 0;

    /**
     * Use this value with {@link MediaControllerCompat.TransportControls#setRepeatMode}
     * to indicate that the playback of the current playing media item will be repeated.
     */
    public static final int REPEAT_MODE_ONE = 1;

    /**
     * Use this value with {@link MediaControllerCompat.TransportControls#setRepeatMode}
     * to indicate that the playback of the playing media list will be repeated.
     */
    public static final int REPEAT_MODE_ALL = 2;

    /**
     * Use this value with {@link MediaControllerCompat.TransportControls#setRepeatMode}
     * to indicate that the playback of the playing media group will be repeated.
     * A group is a logical block of media items which is specified in the section 5.7 of the
     * Bluetooth AVRCP 1.6.
     */
    public static final int REPEAT_MODE_GROUP = 3;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({SHUFFLE_MODE_INVALID, SHUFFLE_MODE_NONE, SHUFFLE_MODE_ALL, SHUFFLE_MODE_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ShuffleMode {}

    /**
     * {@link MediaControllerCompat.TransportControls#getShuffleMode} returns this value
     * when the session is not ready for providing its shuffle mode.
     */
    public static final int SHUFFLE_MODE_INVALID = -1;

    /**
     * Use this value with {@link MediaControllerCompat.TransportControls#setShuffleMode}
     * to indicate that the media list will be played in order.
     */
    public static final int SHUFFLE_MODE_NONE = 0;

    /**
     * Use this value with {@link MediaControllerCompat.TransportControls#setShuffleMode}
     * to indicate that the media list will be played in shuffled order.
     */
    public static final int SHUFFLE_MODE_ALL = 1;

    /**
     * Use this value with {@link MediaControllerCompat.TransportControls#setShuffleMode}
     * to indicate that the media group will be played in shuffled order.
     * A group is a logical block of media items which is specified in the section 5.7 of the
     * Bluetooth AVRCP 1.6.
     */
    public static final int SHUFFLE_MODE_GROUP = 2;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({ERROR_CODE_UNKNOWN_ERROR, ERROR_CODE_APP_ERROR, ERROR_CODE_NOT_SUPPORTED,
            ERROR_CODE_AUTHENTICATION_EXPIRED, ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED,
            ERROR_CODE_CONCURRENT_STREAM_LIMIT, ERROR_CODE_PARENTAL_CONTROL_RESTRICTED,
            ERROR_CODE_NOT_AVAILABLE_IN_REGION, ERROR_CODE_CONTENT_ALREADY_PLAYING,
            ERROR_CODE_SKIP_LIMIT_REACHED, ERROR_CODE_ACTION_ABORTED, ERROR_CODE_END_OF_QUEUE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorCode {}

    /**
     * This is the default error code and indicates that none of the other error codes applies.
     * The error code should be set when entering {@link #STATE_ERROR}.
     */
    public static final int ERROR_CODE_UNKNOWN_ERROR = 0;

    /**
     * Error code when the application state is invalid to fulfill the request.
     * The error code should be set when entering {@link #STATE_ERROR}.
     */
    public static final int ERROR_CODE_APP_ERROR = 1;

    /**
     * Error code when the request is not supported by the application.
     * The error code should be set when entering {@link #STATE_ERROR}.
     */
    public static final int ERROR_CODE_NOT_SUPPORTED = 2;

    /**
     * Error code when the request cannot be performed because authentication has expired.
     * The error code should be set when entering {@link #STATE_ERROR}.
     */
    public static final int ERROR_CODE_AUTHENTICATION_EXPIRED = 3;

    /**
     * Error code when a premium account is required for the request to succeed.
     * The error code should be set when entering {@link #STATE_ERROR}.
     */
    public static final int ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED = 4;

    /**
     * Error code when too many concurrent streams are detected.
     * The error code should be set when entering {@link #STATE_ERROR}.
     */
    public static final int ERROR_CODE_CONCURRENT_STREAM_LIMIT = 5;

    /**
     * Error code when the content is blocked due to parental controls.
     * The error code should be set when entering {@link #STATE_ERROR}.
     */
    public static final int ERROR_CODE_PARENTAL_CONTROL_RESTRICTED = 6;

    /**
     * Error code when the content is blocked due to being regionally unavailable.
     * The error code should be set when entering {@link #STATE_ERROR}.
     */
    public static final int ERROR_CODE_NOT_AVAILABLE_IN_REGION = 7;

    /**
     * Error code when the requested content is already playing.
     * The error code should be set when entering {@link #STATE_ERROR}.
     */
    public static final int ERROR_CODE_CONTENT_ALREADY_PLAYING = 8;

    /**
     * Error code when the application cannot skip any more songs because skip limit is reached.
     * The error code should be set when entering {@link #STATE_ERROR}.
     */
    public static final int ERROR_CODE_SKIP_LIMIT_REACHED = 9;

    /**
     * Error code when the action is interrupted due to some external event.
     * The error code should be set when entering {@link #STATE_ERROR}.
     */
    public static final int ERROR_CODE_ACTION_ABORTED = 10;

    /**
     * Error code when the playback navigation (previous, next) is not possible because the queue
     * was exhausted.
     * The error code should be set when entering {@link #STATE_ERROR}.
     */
    public static final int ERROR_CODE_END_OF_QUEUE = 11;

    // KeyEvent constants only available on API 11+
    private static final int KEYCODE_MEDIA_PAUSE = 127;
    private static final int KEYCODE_MEDIA_PLAY = 126;

    /**
     * Translates a given action into a matched key code defined in {@link KeyEvent}. The given
     * action should be one of the following:
     * <ul>
     * <li>{@link PlaybackStateCompat#ACTION_PLAY}</li>
     * <li>{@link PlaybackStateCompat#ACTION_PAUSE}</li>
     * <li>{@link PlaybackStateCompat#ACTION_SKIP_TO_NEXT}</li>
     * <li>{@link PlaybackStateCompat#ACTION_SKIP_TO_PREVIOUS}</li>
     * <li>{@link PlaybackStateCompat#ACTION_STOP}</li>
     * <li>{@link PlaybackStateCompat#ACTION_FAST_FORWARD}</li>
     * <li>{@link PlaybackStateCompat#ACTION_REWIND}</li>
     * <li>{@link PlaybackStateCompat#ACTION_PLAY_PAUSE}</li>
     * </ul>
     *
     * @param action The action to be translated.
     *
     * @return the key code matched to the given action.
     */
    public static int toKeyCode(@MediaKeyAction long action) {
        if (action == ACTION_PLAY) {
            return KEYCODE_MEDIA_PLAY;
        } else if (action == ACTION_PAUSE) {
            return KEYCODE_MEDIA_PAUSE;
        } else if (action == ACTION_SKIP_TO_NEXT) {
            return KeyEvent.KEYCODE_MEDIA_NEXT;
        } else if (action == ACTION_SKIP_TO_PREVIOUS) {
            return KeyEvent.KEYCODE_MEDIA_PREVIOUS;
        } else if (action == ACTION_STOP) {
            return KeyEvent.KEYCODE_MEDIA_STOP;
        } else if (action == ACTION_FAST_FORWARD) {
            return KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
        } else if (action == ACTION_REWIND) {
            return KeyEvent.KEYCODE_MEDIA_REWIND;
        } else if (action == ACTION_PLAY_PAUSE) {
            return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
        }
        return KeyEvent.KEYCODE_UNKNOWN;
    }

    final int mState;
    final long mPosition;
    final long mBufferedPosition;
    final float mSpeed;
    final long mActions;
    final int mErrorCode;
    final CharSequence mErrorMessage;
    final long mUpdateTime;
    List<PlaybackStateCompat.CustomAction> mCustomActions;
    final long mActiveItemId;
    final Bundle mExtras;

    private Object mStateObj;

    PlaybackStateCompat(int state, long position, long bufferedPosition,
            float rate, long actions, int errorCode, CharSequence errorMessage, long updateTime,
            List<PlaybackStateCompat.CustomAction> customActions,
            long activeItemId, Bundle extras) {
        mState = state;
        mPosition = position;
        mBufferedPosition = bufferedPosition;
        mSpeed = rate;
        mActions = actions;
        mErrorCode = errorCode;
        mErrorMessage = errorMessage;
        mUpdateTime = updateTime;
        mCustomActions = new ArrayList<>(customActions);
        mActiveItemId = activeItemId;
        mExtras = extras;
    }

    PlaybackStateCompat(Parcel in) {
        mState = in.readInt();
        mPosition = in.readLong();
        mSpeed = in.readFloat();
        mUpdateTime = in.readLong();
        mBufferedPosition = in.readLong();
        mActions = in.readLong();
        mErrorMessage = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mCustomActions = in.createTypedArrayList(CustomAction.CREATOR);
        mActiveItemId = in.readLong();
        mExtras = in.readBundle();
        // New attributes should be added at the end for backward compatibility.
        mErrorCode = in.readInt();
    }

    @Override
    public String toString() {
        StringBuilder bob = new StringBuilder("PlaybackState {");
        bob.append("state=").append(mState);
        bob.append(", position=").append(mPosition);
        bob.append(", buffered position=").append(mBufferedPosition);
        bob.append(", speed=").append(mSpeed);
        bob.append(", updated=").append(mUpdateTime);
        bob.append(", actions=").append(mActions);
        bob.append(", error code=").append(mErrorCode);
        bob.append(", error message=").append(mErrorMessage);
        bob.append(", custom actions=").append(mCustomActions);
        bob.append(", active item id=").append(mActiveItemId);
        bob.append("}");
        return bob.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mState);
        dest.writeLong(mPosition);
        dest.writeFloat(mSpeed);
        dest.writeLong(mUpdateTime);
        dest.writeLong(mBufferedPosition);
        dest.writeLong(mActions);
        TextUtils.writeToParcel(mErrorMessage, dest, flags);
        dest.writeTypedList(mCustomActions);
        dest.writeLong(mActiveItemId);
        dest.writeBundle(mExtras);
        // New attributes should be added at the end for backward compatibility.
        dest.writeInt(mErrorCode);
    }

    /**
     * Get the current state of playback. One of the following:
     * <ul>
     * <li> {@link PlaybackStateCompat#STATE_NONE}</li>
     * <li> {@link PlaybackStateCompat#STATE_STOPPED}</li>
     * <li> {@link PlaybackStateCompat#STATE_PLAYING}</li>
     * <li> {@link PlaybackStateCompat#STATE_PAUSED}</li>
     * <li> {@link PlaybackStateCompat#STATE_FAST_FORWARDING}</li>
     * <li> {@link PlaybackStateCompat#STATE_REWINDING}</li>
     * <li> {@link PlaybackStateCompat#STATE_BUFFERING}</li>
     * <li> {@link PlaybackStateCompat#STATE_ERROR}</li>
     * <li> {@link PlaybackStateCompat#STATE_CONNECTING}</li>
     * <li> {@link PlaybackStateCompat#STATE_SKIPPING_TO_PREVIOUS}</li>
     * <li> {@link PlaybackStateCompat#STATE_SKIPPING_TO_NEXT}</li>
     * <li> {@link PlaybackStateCompat#STATE_SKIPPING_TO_QUEUE_ITEM}</li>
     */
    @State
    public int getState() {
        return mState;
    }

    /**
     * Get the current playback position in ms.
     */
    public long getPosition() {
        return mPosition;
    }

    /**
     * Get the current buffered position in ms. This is the farthest playback
     * point that can be reached from the current position using only buffered
     * content.
     */
    public long getBufferedPosition() {
        return mBufferedPosition;
    }

    /**
     * Get the current playback speed as a multiple of normal playback. This
     * should be negative when rewinding. A value of 1 means normal playback and
     * 0 means paused.
     *
     * @return The current speed of playback.
     */
    public float getPlaybackSpeed() {
        return mSpeed;
    }

    /**
     * Get the current actions available on this session. This should use a
     * bitmask of the available actions.
     * <ul>
     * <li> {@link PlaybackStateCompat#ACTION_SKIP_TO_PREVIOUS}</li>
     * <li> {@link PlaybackStateCompat#ACTION_REWIND}</li>
     * <li> {@link PlaybackStateCompat#ACTION_PLAY}</li>
     * <li> {@link PlaybackStateCompat#ACTION_PLAY_PAUSE}</li>
     * <li> {@link PlaybackStateCompat#ACTION_PAUSE}</li>
     * <li> {@link PlaybackStateCompat#ACTION_STOP}</li>
     * <li> {@link PlaybackStateCompat#ACTION_FAST_FORWARD}</li>
     * <li> {@link PlaybackStateCompat#ACTION_SKIP_TO_NEXT}</li>
     * <li> {@link PlaybackStateCompat#ACTION_SEEK_TO}</li>
     * <li> {@link PlaybackStateCompat#ACTION_SET_RATING}</li>
     * <li> {@link PlaybackStateCompat#ACTION_PLAY_FROM_MEDIA_ID}</li>
     * <li> {@link PlaybackStateCompat#ACTION_PLAY_FROM_SEARCH}</li>
     * <li> {@link PlaybackStateCompat#ACTION_SKIP_TO_QUEUE_ITEM}</li>
     * <li> {@link PlaybackStateCompat#ACTION_PLAY_FROM_URI}</li>
     * <li> {@link PlaybackStateCompat#ACTION_PREPARE}</li>
     * <li> {@link PlaybackStateCompat#ACTION_PREPARE_FROM_MEDIA_ID}</li>
     * <li> {@link PlaybackStateCompat#ACTION_PREPARE_FROM_SEARCH}</li>
     * <li> {@link PlaybackStateCompat#ACTION_PREPARE_FROM_URI}</li>
     * <li> {@link PlaybackStateCompat#ACTION_SET_REPEAT_MODE}</li>
     * <li> {@link PlaybackStateCompat#ACTION_SET_SHUFFLE_MODE}</li>
     * <li> {@link PlaybackStateCompat#ACTION_SET_CAPTIONING_ENABLED}</li>
     * </ul>
     */
    @Actions
    public long getActions() {
        return mActions;
    }

    /**
     * Get the list of custom actions.
     */
    public List<PlaybackStateCompat.CustomAction> getCustomActions() {
        return mCustomActions;
    }

    /**
     * Get the error code. This should be set when the state is
     * {@link PlaybackStateCompat#STATE_ERROR}.
     *
     * @see #ERROR_CODE_UNKNOWN_ERROR
     * @see #ERROR_CODE_APP_ERROR
     * @see #ERROR_CODE_NOT_SUPPORTED
     * @see #ERROR_CODE_AUTHENTICATION_EXPIRED
     * @see #ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED
     * @see #ERROR_CODE_CONCURRENT_STREAM_LIMIT
     * @see #ERROR_CODE_PARENTAL_CONTROL_RESTRICTED
     * @see #ERROR_CODE_NOT_AVAILABLE_IN_REGION
     * @see #ERROR_CODE_CONTENT_ALREADY_PLAYING
     * @see #ERROR_CODE_SKIP_LIMIT_REACHED
     * @see #ERROR_CODE_ACTION_ABORTED
     * @see #ERROR_CODE_END_OF_QUEUE
     * @see #getErrorMessage()
     */
    @ErrorCode
    public int getErrorCode() {
        return mErrorCode;
    }

    /**
     * Get the user readable optional error message. This may be set when the state is
     * {@link PlaybackStateCompat#STATE_ERROR}.
     *
     * @see #getErrorCode()
     */
    public CharSequence getErrorMessage() {
        return mErrorMessage;
    }

    /**
     * Get the elapsed real time at which position was last updated. If the
     * position has never been set this will return 0;
     *
     * @return The last time the position was updated.
     */
    public long getLastPositionUpdateTime() {
        return mUpdateTime;
    }

    /**
     * Get the id of the currently active item in the queue. If there is no
     * queue or a queue is not supported by the session this will be
     * {@link MediaSessionCompat.QueueItem#UNKNOWN_ID}.
     *
     * @return The id of the currently active item in the queue or
     *         {@link MediaSessionCompat.QueueItem#UNKNOWN_ID}.
     */
    public long getActiveQueueItemId() {
        return mActiveItemId;
    }

    /**
     * Get any custom extras that were set on this playback state.
     *
     * @return The extras for this state or null.
     */
    public @Nullable Bundle getExtras() {
        return mExtras;
    }

    /**
     * Creates an instance from a framework {@link android.media.session.PlaybackState} object.
     * <p>
     * This method is only supported on API 21+.
     * </p>
     *
     * @param stateObj A {@link android.media.session.PlaybackState} object, or null if none.
     * @return An equivalent {@link PlaybackStateCompat} object, or null if none.
     */
    public static PlaybackStateCompat fromPlaybackState(Object stateObj) {
        if (stateObj != null && Build.VERSION.SDK_INT >= 21) {
            List<Object> customActionObjs = PlaybackStateCompatApi21.getCustomActions(stateObj);
            List<PlaybackStateCompat.CustomAction> customActions = null;
            if (customActionObjs != null) {
                customActions = new ArrayList<>(customActionObjs.size());
                for (Object customActionObj : customActionObjs) {
                    customActions.add(CustomAction.fromCustomAction(customActionObj));
                }
            }
            Bundle extras;
            if (Build.VERSION.SDK_INT >= 22) {
                extras = PlaybackStateCompatApi22.getExtras(stateObj);
            } else {
                extras = null;
            }
            PlaybackStateCompat state = new PlaybackStateCompat(
                    PlaybackStateCompatApi21.getState(stateObj),
                    PlaybackStateCompatApi21.getPosition(stateObj),
                    PlaybackStateCompatApi21.getBufferedPosition(stateObj),
                    PlaybackStateCompatApi21.getPlaybackSpeed(stateObj),
                    PlaybackStateCompatApi21.getActions(stateObj),
                    ERROR_CODE_UNKNOWN_ERROR,
                    PlaybackStateCompatApi21.getErrorMessage(stateObj),
                    PlaybackStateCompatApi21.getLastPositionUpdateTime(stateObj),
                    customActions,
                    PlaybackStateCompatApi21.getActiveQueueItemId(stateObj),
                    extras);
            state.mStateObj = stateObj;
            return state;
        } else {
            return null;
        }
    }

    /**
     * Gets the underlying framework {@link android.media.session.PlaybackState} object.
     * <p>
     * This method is only supported on API 21+.
     * </p>
     *
     * @return An equivalent {@link android.media.session.PlaybackState} object, or null if none.
     */
    public Object getPlaybackState() {
        if (mStateObj == null && Build.VERSION.SDK_INT >= 21) {
            List<Object> customActions = null;
            if (mCustomActions != null) {
                customActions = new ArrayList<>(mCustomActions.size());
                for (PlaybackStateCompat.CustomAction customAction : mCustomActions) {
                    customActions.add(customAction.getCustomAction());
                }
            }
            if (Build.VERSION.SDK_INT >= 22) {
                mStateObj = PlaybackStateCompatApi22.newInstance(mState, mPosition,
                        mBufferedPosition,
                        mSpeed, mActions, mErrorMessage, mUpdateTime,
                        customActions, mActiveItemId, mExtras);
            } else {
                //noinspection AndroidLintNewApi - NewApi lint fails to handle nested checks.
                mStateObj = PlaybackStateCompatApi21.newInstance(mState, mPosition,
                        mBufferedPosition, mSpeed, mActions, mErrorMessage, mUpdateTime,
                        customActions, mActiveItemId);
            }
        }
        return mStateObj;
    }

    public static final Parcelable.Creator<PlaybackStateCompat> CREATOR =
            new Parcelable.Creator<PlaybackStateCompat>() {
        @Override
        public PlaybackStateCompat createFromParcel(Parcel in) {
            return new PlaybackStateCompat(in);
        }

        @Override
        public PlaybackStateCompat[] newArray(int size) {
            return new PlaybackStateCompat[size];
        }
    };

    /**
     * {@link PlaybackStateCompat.CustomAction CustomActions} can be used to
     * extend the capabilities of the standard transport controls by exposing
     * app specific actions to {@link MediaControllerCompat Controllers}.
     */
    public static final class CustomAction implements Parcelable {
        private final String mAction;
        private final CharSequence mName;
        private final int mIcon;
        private final Bundle mExtras;

        private Object mCustomActionObj;

        /**
         * Use {@link PlaybackStateCompat.CustomAction.Builder#build()}.
         */
        CustomAction(String action, CharSequence name, int icon, Bundle extras) {
            mAction = action;
            mName = name;
            mIcon = icon;
            mExtras = extras;
        }

        CustomAction(Parcel in) {
            mAction = in.readString();
            mName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            mIcon = in.readInt();
            mExtras = in.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mAction);
            TextUtils.writeToParcel(mName, dest, flags);
            dest.writeInt(mIcon);
            dest.writeBundle(mExtras);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        /**
         * Creates an instance from a framework
         * {@link android.media.session.PlaybackState.CustomAction} object.
         * <p>
         * This method is only supported on API 21+.
         * </p>
         *
         * @param customActionObj A {@link android.media.session.PlaybackState.CustomAction} object,
         * or null if none.
         * @return An equivalent {@link PlaybackStateCompat.CustomAction} object, or null if none.
         */
        public static PlaybackStateCompat.CustomAction fromCustomAction(Object customActionObj) {
            if (customActionObj == null || Build.VERSION.SDK_INT < 21) {
                return null;
            }

            PlaybackStateCompat.CustomAction customAction = new PlaybackStateCompat.CustomAction(
                    PlaybackStateCompatApi21.CustomAction.getAction(customActionObj),
                    PlaybackStateCompatApi21.CustomAction.getName(customActionObj),
                    PlaybackStateCompatApi21.CustomAction.getIcon(customActionObj),
                    PlaybackStateCompatApi21.CustomAction.getExtras(customActionObj));
            customAction.mCustomActionObj = customActionObj;
            return customAction;
        }

        /**
         * Gets the underlying framework {@link android.media.session.PlaybackState.CustomAction}
         * object.
         * <p>
         * This method is only supported on API 21+.
         * </p>
         *
         * @return An equivalent {@link android.media.session.PlaybackState.CustomAction} object,
         * or null if none.
         */
        public Object getCustomAction() {
            if (mCustomActionObj != null || Build.VERSION.SDK_INT < 21) {
                return mCustomActionObj;
            }

            mCustomActionObj = PlaybackStateCompatApi21.CustomAction.newInstance(mAction,
                    mName, mIcon, mExtras);
            return mCustomActionObj;
        }

        public static final Parcelable.Creator<PlaybackStateCompat.CustomAction> CREATOR
                = new Parcelable.Creator<PlaybackStateCompat.CustomAction>() {

                    @Override
                    public PlaybackStateCompat.CustomAction createFromParcel(Parcel p) {
                        return new PlaybackStateCompat.CustomAction(p);
                    }

                    @Override
                    public PlaybackStateCompat.CustomAction[] newArray(int size) {
                        return new PlaybackStateCompat.CustomAction[size];
                    }
                };

        /**
         * Returns the action of the {@link CustomAction}.
         *
         * @return The action of the {@link CustomAction}.
         */
        public String getAction() {
            return mAction;
        }

        /**
         * Returns the display name of this action. e.g. "Favorite"
         *
         * @return The display name of this {@link CustomAction}.
         */
        public CharSequence getName() {
            return mName;
        }

        /**
         * Returns the resource id of the icon in the {@link MediaSessionCompat
         * Session's} package.
         *
         * @return The resource id of the icon in the {@link MediaSessionCompat
         *         Session's} package.
         */
        public int getIcon() {
            return mIcon;
        }

        /**
         * Returns extras which provide additional application-specific
         * information about the action, or null if none. These arguments are
         * meant to be consumed by a {@link MediaControllerCompat} if it knows
         * how to handle them.
         *
         * @return Optional arguments for the {@link CustomAction}.
         */
        public Bundle getExtras() {
            return mExtras;
        }

        @Override
        public String toString() {
            return "Action:" +
                    "mName='" + mName +
                    ", mIcon=" + mIcon +
                    ", mExtras=" + mExtras;
        }

        /**
         * Builder for {@link CustomAction} objects.
         */
        public static final class Builder {
            private final String mAction;
            private final CharSequence mName;
            private final int mIcon;
            private Bundle mExtras;

            /**
             * Creates a {@link CustomAction} builder with the id, name, and
             * icon set.
             *
             * @param action The action of the {@link CustomAction}.
             * @param name The display name of the {@link CustomAction}. This
             *            name will be displayed along side the action if the UI
             *            supports it.
             * @param icon The icon resource id of the {@link CustomAction}.
             *            This resource id must be in the same package as the
             *            {@link MediaSessionCompat}. It will be displayed with
             *            the custom action if the UI supports it.
             */
            public Builder(String action, CharSequence name, int icon) {
                if (TextUtils.isEmpty(action)) {
                    throw new IllegalArgumentException(
                            "You must specify an action to build a CustomAction.");
                }
                if (TextUtils.isEmpty(name)) {
                    throw new IllegalArgumentException(
                            "You must specify a name to build a CustomAction.");
                }
                if (icon == 0) {
                    throw new IllegalArgumentException(
                            "You must specify an icon resource id to build a CustomAction.");
                }
                mAction = action;
                mName = name;
                mIcon = icon;
            }

            /**
             * Set optional extras for the {@link CustomAction}. These extras
             * are meant to be consumed by a {@link MediaControllerCompat} if it
             * knows how to handle them. Keys should be fully qualified (e.g.
             * "com.example.MY_ARG") to avoid collisions.
             *
             * @param extras Optional extras for the {@link CustomAction}.
             * @return this.
             */
            public Builder setExtras(Bundle extras) {
                mExtras = extras;
                return this;
            }

            /**
             * Build and return the {@link CustomAction} instance with the
             * specified values.
             *
             * @return A new {@link CustomAction} instance.
             */
            public CustomAction build() {
                return new CustomAction(mAction, mName, mIcon, mExtras);
            }
        }
    }

    /**
     * Builder for {@link PlaybackStateCompat} objects.
     */
    public static final class Builder {
        private final List<PlaybackStateCompat.CustomAction> mCustomActions = new ArrayList<>();

        private int mState;
        private long mPosition;
        private long mBufferedPosition;
        private float mRate;
        private long mActions;
        private int mErrorCode;
        private CharSequence mErrorMessage;
        private long mUpdateTime;
        private long mActiveItemId = MediaSessionCompat.QueueItem.UNKNOWN_ID;
        private Bundle mExtras;

        /**
         * Create an empty Builder.
         */
        public Builder() {
        }

        /**
         * Create a Builder using a {@link PlaybackStateCompat} instance to set the
         * initial values.
         *
         * @param source The playback state to copy.
         */
        public Builder(PlaybackStateCompat source) {
            mState = source.mState;
            mPosition = source.mPosition;
            mRate = source.mSpeed;
            mUpdateTime = source.mUpdateTime;
            mBufferedPosition = source.mBufferedPosition;
            mActions = source.mActions;
            mErrorCode = source.mErrorCode;
            mErrorMessage = source.mErrorMessage;
            if (source.mCustomActions != null) {
                mCustomActions.addAll(source.mCustomActions);
            }
            mActiveItemId = source.mActiveItemId;
            mExtras = source.mExtras;
        }

        /**
         * Set the current state of playback.
         * <p>
         * The position must be in ms and indicates the current playback
         * position within the track. If the position is unknown use
         * {@link #PLAYBACK_POSITION_UNKNOWN}.
         * <p>
         * The rate is a multiple of normal playback and should be 0 when paused
         * and negative when rewinding. Normal playback rate is 1.0.
         * <p>
         * The state must be one of the following:
         * <ul>
         * <li> {@link PlaybackStateCompat#STATE_NONE}</li>
         * <li> {@link PlaybackStateCompat#STATE_STOPPED}</li>
         * <li> {@link PlaybackStateCompat#STATE_PLAYING}</li>
         * <li> {@link PlaybackStateCompat#STATE_PAUSED}</li>
         * <li> {@link PlaybackStateCompat#STATE_FAST_FORWARDING}</li>
         * <li> {@link PlaybackStateCompat#STATE_REWINDING}</li>
         * <li> {@link PlaybackStateCompat#STATE_BUFFERING}</li>
         * <li> {@link PlaybackStateCompat#STATE_ERROR}</li>
         * <li> {@link PlaybackStateCompat#STATE_CONNECTING}</li>
         * <li> {@link PlaybackStateCompat#STATE_SKIPPING_TO_PREVIOUS}</li>
         * <li> {@link PlaybackStateCompat#STATE_SKIPPING_TO_NEXT}</li>
         * <li> {@link PlaybackStateCompat#STATE_SKIPPING_TO_QUEUE_ITEM}</li>
         * </ul>
         *
         * @param state The current state of playback.
         * @param position The position in the current track in ms.
         * @param playbackSpeed The current rate of playback as a multiple of
         *            normal playback.
         */
        public Builder setState(@State int state, long position, float playbackSpeed) {
            return setState(state, position, playbackSpeed, SystemClock.elapsedRealtime());
        }

        /**
         * Set the current state of playback.
         * <p>
         * The position must be in ms and indicates the current playback
         * position within the track. If the position is unknown use
         * {@link #PLAYBACK_POSITION_UNKNOWN}.
         * <p>
         * The rate is a multiple of normal playback and should be 0 when paused
         * and negative when rewinding. Normal playback rate is 1.0.
         * <p>
         * The state must be one of the following:
         * <ul>
         * <li> {@link PlaybackStateCompat#STATE_NONE}</li>
         * <li> {@link PlaybackStateCompat#STATE_STOPPED}</li>
         * <li> {@link PlaybackStateCompat#STATE_PLAYING}</li>
         * <li> {@link PlaybackStateCompat#STATE_PAUSED}</li>
         * <li> {@link PlaybackStateCompat#STATE_FAST_FORWARDING}</li>
         * <li> {@link PlaybackStateCompat#STATE_REWINDING}</li>
         * <li> {@link PlaybackStateCompat#STATE_BUFFERING}</li>
         * <li> {@link PlaybackStateCompat#STATE_ERROR}</li>
         * <li> {@link PlaybackStateCompat#STATE_CONNECTING}</li>
         * <li> {@link PlaybackStateCompat#STATE_SKIPPING_TO_PREVIOUS}</li>
         * <li> {@link PlaybackStateCompat#STATE_SKIPPING_TO_NEXT}</li>
         * <li> {@link PlaybackStateCompat#STATE_SKIPPING_TO_QUEUE_ITEM}</li>
         * </ul>
         *
         * @param state The current state of playback.
         * @param position The position in the current item in ms.
         * @param playbackSpeed The current speed of playback as a multiple of
         *            normal playback.
         * @param updateTime The time in the {@link SystemClock#elapsedRealtime}
         *            timebase that the position was updated at.
         * @return this
         */
        public Builder setState(@State int state, long position, float playbackSpeed,
                long updateTime) {
            mState = state;
            mPosition = position;
            mUpdateTime = updateTime;
            mRate = playbackSpeed;
            return this;
        }

        /**
         * Set the current buffered position in ms. This is the farthest
         * playback point that can be reached from the current position using
         * only buffered content.
         *
         * @return this
         */
        public Builder setBufferedPosition(long bufferPosition) {
            mBufferedPosition = bufferPosition;
            return this;
        }

        /**
         * Set the current capabilities available on this session. This should
         * use a bitmask of the available capabilities.
         * <ul>
         * <li> {@link PlaybackStateCompat#ACTION_SKIP_TO_PREVIOUS}</li>
         * <li> {@link PlaybackStateCompat#ACTION_REWIND}</li>
         * <li> {@link PlaybackStateCompat#ACTION_PLAY}</li>
         * <li> {@link PlaybackStateCompat#ACTION_PLAY_PAUSE}</li>
         * <li> {@link PlaybackStateCompat#ACTION_PAUSE}</li>
         * <li> {@link PlaybackStateCompat#ACTION_STOP}</li>
         * <li> {@link PlaybackStateCompat#ACTION_FAST_FORWARD}</li>
         * <li> {@link PlaybackStateCompat#ACTION_SKIP_TO_NEXT}</li>
         * <li> {@link PlaybackStateCompat#ACTION_SEEK_TO}</li>
         * <li> {@link PlaybackStateCompat#ACTION_SET_RATING}</li>
         * <li> {@link PlaybackStateCompat#ACTION_PLAY_FROM_MEDIA_ID}</li>
         * <li> {@link PlaybackStateCompat#ACTION_PLAY_FROM_SEARCH}</li>
         * <li> {@link PlaybackStateCompat#ACTION_SKIP_TO_QUEUE_ITEM}</li>
         * <li> {@link PlaybackStateCompat#ACTION_PLAY_FROM_URI}</li>
         * <li> {@link PlaybackStateCompat#ACTION_PREPARE}</li>
         * <li> {@link PlaybackStateCompat#ACTION_PREPARE_FROM_MEDIA_ID}</li>
         * <li> {@link PlaybackStateCompat#ACTION_PREPARE_FROM_SEARCH}</li>
         * <li> {@link PlaybackStateCompat#ACTION_PREPARE_FROM_URI}</li>
         * <li> {@link PlaybackStateCompat#ACTION_SET_REPEAT_MODE}</li>
         * <li> {@link PlaybackStateCompat#ACTION_SET_SHUFFLE_MODE}</li>
         * <li> {@link PlaybackStateCompat#ACTION_SET_CAPTIONING_ENABLED}</li>
         * </ul>
         *
         * @return this
         */
        public Builder setActions(@Actions long capabilities) {
            mActions = capabilities;
            return this;
        }

        /**
         * Add a custom action to the playback state. Actions can be used to
         * expose additional functionality to {@link MediaControllerCompat
         * Controllers} beyond what is offered by the standard transport
         * controls.
         * <p>
         * e.g. start a radio station based on the current item or skip ahead by
         * 30 seconds.
         *
         * @param action An identifier for this action. It can be sent back to
         *            the {@link MediaSessionCompat} through
         *            {@link MediaControllerCompat.TransportControls#sendCustomAction(String, Bundle)}.
         * @param name The display name for the action. If text is shown with
         *            the action or used for accessibility, this is what should
         *            be used.
         * @param icon The resource action of the icon that should be displayed
         *            for the action. The resource should be in the package of
         *            the {@link MediaSessionCompat}.
         * @return this
         */
        public Builder addCustomAction(String action, String name, int icon) {
            return addCustomAction(new PlaybackStateCompat.CustomAction(action, name, icon, null));
        }

        /**
         * Add a custom action to the playback state. Actions can be used to expose additional
         * functionality to {@link MediaControllerCompat Controllers} beyond what is offered
         * by the standard transport controls.
         * <p>
         * An example of an action would be to start a radio station based on the current item
         * or to skip ahead by 30 seconds.
         *
         * @param customAction The custom action to add to the {@link PlaybackStateCompat}.
         * @return this
         */
        public Builder addCustomAction(PlaybackStateCompat.CustomAction customAction) {
            if (customAction == null) {
                throw new IllegalArgumentException(
                        "You may not add a null CustomAction to PlaybackStateCompat.");
            }
            mCustomActions.add(customAction);
            return this;
        }

        /**
         * Set the active item in the play queue by specifying its id. The
         * default value is {@link MediaSessionCompat.QueueItem#UNKNOWN_ID}
         *
         * @param id The id of the active item.
         * @return this
         */
        public Builder setActiveQueueItemId(long id) {
            mActiveItemId = id;
            return this;
        }

        /**
         * Set a user readable error message. This should be set when the state
         * is {@link PlaybackStateCompat#STATE_ERROR}.
         *
         * @return this
         * @deprecated Use {@link #setErrorMessage(int, CharSequence)} instead.
         */
        public Builder setErrorMessage(CharSequence errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        /**
         * Set the error code with an optional user readable error message. This should be set when
         * the state is {@link PlaybackStateCompat#STATE_ERROR}.
         *
         * @param errorCode The errorCode to set.
         * @param errorMessage The user readable error message. Can be null.
         * @return this
         */
        public Builder setErrorMessage(@ErrorCode int errorCode, CharSequence errorMessage) {
            mErrorCode = errorCode;
            mErrorMessage = errorMessage;
            return this;
        }

        /**
         * Set any custom extras to be included with the playback state.
         *
         * @param extras The extras to include.
         * @return this
         */
        public Builder setExtras(Bundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * Creates the playback state object.
         */
        public PlaybackStateCompat build() {
            return new PlaybackStateCompat(mState, mPosition, mBufferedPosition,
                    mRate, mActions, mErrorCode, mErrorMessage, mUpdateTime,
                    mCustomActions, mActiveItemId, mExtras);
        }
    }
}
