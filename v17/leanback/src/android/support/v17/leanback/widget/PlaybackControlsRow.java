/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.support.v17.leanback.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * A row of playback controls to be displayed by a {@link PlaybackControlsRowPresenter}.
 *
 * This row consists of some optional item detail, a series of primary actions,
 * and an optional series of secondary actions.
 *
 * Controls are specified via an {@link ObjectAdapter} containing one or more
 * {@link Action}s.
 *
 * Adapters should have their {@link PresenterSelector} set to an instance of
 * {@link ControlButtonPresenterSelector}.
 *
 */
public class PlaybackControlsRow extends Row {

    /**
     * An action displaying icons for play and pause.
     */
    public static class PlayPauseAction extends Action {
        Drawable mPlayIcon;
        Drawable mPauseIcon;

        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public PlayPauseAction(Context context) {
            super(R.id.lb_control_play_pause);
            mPlayIcon = context.getResources().getDrawable(R.drawable.lb_ic_play);
            mPauseIcon = context.getResources().getDrawable(R.drawable.lb_ic_pause);
            play();
        }

        /**
         * Display the play icon.
         */
        public void play() {
            setIcon(mPlayIcon);
        }

        /**
         * Display the pause icon.
         */
        public void pause() {
            setIcon(mPauseIcon);
        }

        /**
         * Toggle between the play and pause icon.
         */
        public void toggle() {
            setIcon(getIcon() == mPlayIcon ? mPauseIcon : mPlayIcon);
        }

        /**
         * Returns true if the current icon is play.
         */
        public boolean isPlayIconShown() {
            return getIcon() == mPlayIcon;
        }
    }

    /**
     * An action displaying an icon for fast forward.
     */
    public static class FastForwardAction extends Action {
        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public FastForwardAction(Context context) {
            super(R.id.lb_control_fast_forward);
            setIcon(context.getResources().getDrawable(R.drawable.lb_ic_fast_forward));
        }
    }

    /**
     * An action displaying an icon for rewind.
     */
    public static class RewindAction extends Action {
        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public RewindAction(Context context) {
            super(R.id.lb_control_fast_rewind);
            setIcon(context.getResources().getDrawable(R.drawable.lb_ic_fast_rewind));
        }
    }

    /**
     * An action displaying an icon for skip next.
     */
    public static class SkipNextAction extends Action {
        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public SkipNextAction(Context context) {
            super(R.id.lb_control_skip_next);
            setIcon(context.getResources().getDrawable(R.drawable.lb_ic_skip_next));
        }
    }

    /**
     * An action displaying an icon for skip previous.
     */
    public static class SkipPreviousAction extends Action {
        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public SkipPreviousAction(Context context) {
            super(R.id.lb_control_skip_previous);
            setIcon(context.getResources().getDrawable(R.drawable.lb_ic_skip_previous));
        }
    }

    /**
     * An action displaying an icon for "more actions".
     */
    public static class MoreActions extends Action {
        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public MoreActions(Context context) {
            super(R.id.lb_control_more_actions);
            setIcon(context.getResources().getDrawable(R.drawable.lb_ic_more));
        }
    }

    /**
     * An action displaying an icon for thumbs up.
     */
    public static class ThumbsUpAction extends Action {
        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public ThumbsUpAction(Context context) {
            super(R.id.lb_control_thumbs_up);
            setIcon(context.getResources().getDrawable(R.drawable.lb_ic_thumb_up));
        }
    }

    /**
     * An action displaying an icon for thumbs down.
     */
    public static class ThumbsDownAction extends Action {
        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public ThumbsDownAction(Context context) {
            super(R.id.lb_control_thumbs_down);
            setIcon(context.getResources().getDrawable(R.drawable.lb_ic_thumb_down));
        }
    }

    /**
     * An action for displaying three repeat states: none, one, or all.
     */
    public static class RepeatAction extends Action {
        public static int NONE = 0;
        public static int ONE = 1;
        public static int ALL = 2;
        private Drawable[] mRepeatIcon = new Drawable[3];
        private int mState;

        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public RepeatAction(Context context) {
            super(R.id.lb_control_repeat);
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.lb_ic_loop);
            mRepeatIcon[NONE] = new BitmapDrawable(context.getResources(),
                    bitmap);
            mRepeatIcon[ONE] = new BitmapDrawable(context.getResources(),
                    createBitmap(bitmap, Color.CYAN));
            mRepeatIcon[ALL] = new BitmapDrawable(context.getResources(),
                    createBitmap(bitmap, Color.GREEN));
            repeatNone();
        }

        /**
         * Display the icon for repeat-none.
         */
        public void repeatNone() {
            setIcon(mRepeatIcon[mState = NONE]);
        }

        /**
         * Display the icon for repeat-one.
         */
        public void repeatOne() {
            setIcon(mRepeatIcon[mState = ONE]);
        }

        /**
         * Display the icon for repeat-all.
         */
        public void repeatAll() {
            setIcon(mRepeatIcon[mState = ALL]);
        }

        /**
         * Display the next icon in the series.
         */
        public void next() {
            mState = mState == ALL ? NONE : mState + 1;
            setIcon(mRepeatIcon[mState]);
        }

        /**
         * Returns the current state (NONE, ALL, or ONE).
         */
        public int getState() {
            return mState;
        }
    }

    /**
     * An action for displaying a shuffle icon.
     */
    public static class ShuffleAction extends Action {
        /**
         * Constructor
         * @param context Context used for loading resources.
         */
        public ShuffleAction(Context context) {
            super(R.id.lb_control_shuffle);
            setIcon(context.getResources().getDrawable(R.drawable.lb_ic_shuffle));
        }
    }

    private static Bitmap createBitmap(Bitmap bitmap, int color) {
        Bitmap dst = bitmap.copy(bitmap.getConfig(), true);
        Canvas canvas = new Canvas(dst);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return dst;
    }

    private Object mItem;
    private Drawable mImageDrawable;
    private ObjectAdapter mPrimaryActionsAdapter;
    private ObjectAdapter mSecondaryActionsAdapter;
    private int mTotalTimeMs;
    private int mCurrentTimeMs;
    private int mBufferedProgressMs;
    private OnPlaybackStateChangedListener mListener;

    /**
     * Constructor for a PlaybackControlsRow that displays some details from
     * the given item.
     *
     * @param item The main item for the row.
     */
    public PlaybackControlsRow(Object item) {
        mItem = item;
    }

    /**
     * Constructor for a PlaybackControlsRow that has no item details.
     */
    public PlaybackControlsRow() {
    }

    /**
     * Gets the main item for the details page.
     */
    public final Object getItem() {
        return mItem;
    }

    /**
     * Sets a {link @Drawable} image for this row.
     *
     * @param drawable The drawable to set.
     */
    public final void setImageDrawable(Drawable drawable) {
        mImageDrawable = drawable;
    }

    /**
     * Sets a {@link Bitmap} for this row.
     *
     * @param context The context to retrieve display metrics from.
     * @param bm The bitmap to set.
     */
    public final void setImageBitmap(Context context, Bitmap bm) {
        mImageDrawable = new BitmapDrawable(context.getResources(), bm);
    }

    /**
     * Gets the image {@link Drawable} of this row.
     *
     * @return The overview's image drawable, or null if no drawable has been
     *         assigned.
     */
    public final Drawable getImageDrawable() {
        return mImageDrawable;
    }

    /**
     * Sets the primary actions {@link ObjectAdapter}.
     */
    public final void setPrimaryActionsAdapter(ObjectAdapter adapter) {
        mPrimaryActionsAdapter = adapter;
    }

    /**
     * Sets the secondary actions {@link ObjectAdapter}.
     */
    public final void setSecondaryActionsAdapter(ObjectAdapter adapter) {
        mSecondaryActionsAdapter = adapter;
    }

    /**
     * Returns the primary actions {@link ObjectAdapter}.
     */
    public final ObjectAdapter getPrimaryActionsAdapter() {
        return mPrimaryActionsAdapter;
    }

    /**
     * Returns the secondary actions {@link ObjectAdapter}.
     */
    public final ObjectAdapter getSecondaryActionsAdapter() {
        return mSecondaryActionsAdapter;
    }

    /**
     * Sets the total time in milliseconds for the playback controls row.
     */
    public void setTotalTime(int ms) {
        mTotalTimeMs = ms;
    }

    /**
     * Returns the total time in milliseconds for the playback controls row.
     */
    public int getTotalTime() {
        return mTotalTimeMs;
    }

    /**
     * Sets the current time in milliseconds for the playback controls row.
     */
    public void setCurrentTime(int ms) {
        if (mCurrentTimeMs != ms) {
            mCurrentTimeMs = ms;
            currentTimeChanged();
        }
    }

    /**
     * Returns the current time in milliseconds for the playback controls row.
     */
    public int getCurrentTime() {
        return mCurrentTimeMs;
    }

    /**
     * Sets the buffered progress for the playback controls row.
     */
    public void setBufferedProgress(int ms) {
        if (mBufferedProgressMs != ms) {
            mBufferedProgressMs = ms;
            bufferedProgressChanged();
        }
    }

    /**
     * Returns the buffered progress for the playback controls row.
     */
    public int getBufferedProgress() {
        return mBufferedProgressMs;
    }

    interface OnPlaybackStateChangedListener {
        public void onCurrentTimeChanged(int currentTimeMs);
        public void onBufferedProgressChanged(int bufferedProgressMs);
    }

    /**
     * Sets a listener to be called when the playback state changes.
     */
    public void setOnPlaybackStateChangedListener(OnPlaybackStateChangedListener listener) {
        mListener = listener;
    }

    /**
     * Returns the playback state listener.
     */
    public OnPlaybackStateChangedListener getOnPlaybackStateChangedListener() {
        return mListener;
    }

    private void currentTimeChanged() {
        if (mListener != null) {
            mListener.onCurrentTimeChanged(mCurrentTimeMs);
        }
    }

    private void bufferedProgressChanged() {
        if (mListener != null) {
            mListener.onBufferedProgressChanged(mBufferedProgressMs);
        }
    }
}
