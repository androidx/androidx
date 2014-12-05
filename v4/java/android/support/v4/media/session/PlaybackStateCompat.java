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

import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.TextUtils;

/**
 * Playback state for a {@link MediaSessionCompat}. This includes a state like
 * {@link PlaybackStateCompat#STATE_PLAYING}, the current playback position,
 * and the current control capabilities.
 */
public final class PlaybackStateCompat implements Parcelable {

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
     * message should also be set when entering this state.
     *
     * @see Builder#setState
     */
    public final static int STATE_ERROR = 7;

    /**
     * State indicating the class doing playback is currently connecting to a
     * route. Depending on the implementation you may return to the previous
     * state when the connection finishes or enter {@link #STATE_NONE}. If
     * the connection failed {@link #STATE_ERROR} should be used.
     * @hide
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
     * Use this value for the position to indicate the position is not known.
     */
    public final static long PLAYBACK_POSITION_UNKNOWN = -1;

    private final int mState;
    private final long mPosition;
    private final long mBufferedPosition;
    private final float mSpeed;
    private final long mActions;
    private final CharSequence mErrorMessage;
    private final long mUpdateTime;

    private Object mStateObj;

    private PlaybackStateCompat(int state, long position, long bufferedPosition,
            float rate, long actions, CharSequence errorMessage, long updateTime) {
        mState = state;
        mPosition = position;
        mBufferedPosition = bufferedPosition;
        mSpeed = rate;
        mActions = actions;
        mErrorMessage = errorMessage;
        mUpdateTime = updateTime;
    }

    private PlaybackStateCompat(Parcel in) {
        mState = in.readInt();
        mPosition = in.readLong();
        mSpeed = in.readFloat();
        mUpdateTime = in.readLong();
        mBufferedPosition = in.readLong();
        mActions = in.readLong();
        mErrorMessage = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
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
        bob.append(", error=").append(mErrorMessage);
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
     */
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
     * <li> {@link PlaybackStateCompat#ACTION_PAUSE}</li>
     * <li> {@link PlaybackStateCompat#ACTION_STOP}</li>
     * <li> {@link PlaybackStateCompat#ACTION_FAST_FORWARD}</li>
     * <li> {@link PlaybackStateCompat#ACTION_SKIP_TO_NEXT}</li>
     * <li> {@link PlaybackStateCompat#ACTION_SEEK_TO}</li>
     * <li> {@link PlaybackStateCompat#ACTION_SET_RATING}</li>
     * </ul>
     */
    public long getActions() {
        return mActions;
    }

    /**
     * Get a user readable error message. This should be set when the state is
     * {@link PlaybackStateCompat#STATE_ERROR}.
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
     * Creates an instance from a framework {@link android.media.session.PlaybackState} object.
     * <p>
     * This method is only supported on API 21+.
     * </p>
     *
     * @param stateObj A {@link android.media.session.PlaybackState} object, or null if none.
     * @return An equivalent {@link PlaybackStateCompat} object, or null if none.
     */
    public static PlaybackStateCompat fromPlaybackState(Object stateObj) {
        if (stateObj == null || Build.VERSION.SDK_INT < 21) {
            return null;
        }

        PlaybackStateCompat state = new PlaybackStateCompat(
                PlaybackStateCompatApi21.getState(stateObj),
                PlaybackStateCompatApi21.getPosition(stateObj),
                PlaybackStateCompatApi21.getBufferedPosition(stateObj),
                PlaybackStateCompatApi21.getPlaybackSpeed(stateObj),
                PlaybackStateCompatApi21.getActions(stateObj),
                PlaybackStateCompatApi21.getErrorMessage(stateObj),
                PlaybackStateCompatApi21.getLastPositionUpdateTime(stateObj));
        state.mStateObj = stateObj;
        return state;
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
        if (mStateObj != null || Build.VERSION.SDK_INT < 21) {
            return mStateObj;
        }

        mStateObj = PlaybackStateCompatApi21.newInstance(mState, mPosition, mBufferedPosition,
                mSpeed, mActions, mErrorMessage, mUpdateTime);
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

        /**
         * Use {@link PlaybackStateCompat.CustomAction.Builder#build()}.
         */
        private CustomAction(String action, CharSequence name, int icon, Bundle extras) {
            mAction = action;
            mName = name;
            mIcon = icon;
            mExtras = extras;
        }

        private CustomAction(Parcel in) {
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
        private int mState;
        private long mPosition;
        private long mBufferedPosition;
        private float mRate;
        private long mActions;
        private CharSequence mErrorMessage;
        private long mUpdateTime;

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
            mErrorMessage = source.mErrorMessage;
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
         * </ul>
         *
         * @param state The current state of playback.
         * @param position The position in the current track in ms.
         * @param playbackSpeed The current rate of playback as a multiple of
         *            normal playback.
         */
        public Builder setState(int state, long position, float playbackSpeed) {
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
        public Builder setState(int state, long position, float playbackSpeed, long updateTime) {
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
         * <li> {@link PlaybackStateCompat#ACTION_PAUSE}</li>
         * <li> {@link PlaybackStateCompat#ACTION_STOP}</li>
         * <li> {@link PlaybackStateCompat#ACTION_FAST_FORWARD}</li>
         * <li> {@link PlaybackStateCompat#ACTION_SKIP_TO_NEXT}</li>
         * <li> {@link PlaybackStateCompat#ACTION_SEEK_TO}</li>
         * <li> {@link PlaybackStateCompat#ACTION_SET_RATING}</li>
         * </ul>
         *
         * @return this
         */
        public Builder setActions(long capabilities) {
            mActions = capabilities;
            return this;
        }

        /**
         * Set a user readable error message. This should be set when the state
         * is {@link PlaybackStateCompat#STATE_ERROR}.
         *
         * @return this
         */
        public Builder setErrorMessage(CharSequence errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        /**
         * Creates the playback state object.
         */
        public PlaybackStateCompat build() {
            return new PlaybackStateCompat(mState, mPosition, mBufferedPosition,
                    mRate, mActions, mErrorMessage, mUpdateTime);
        }
    }
}
