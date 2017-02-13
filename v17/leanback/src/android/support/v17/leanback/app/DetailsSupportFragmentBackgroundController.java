// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from VideoDetailsFragmentBackgroundController.java.  DO NOT MODIFY. */

/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.support.v17.leanback.app;

import android.animation.PropertyValuesHolder;
import android.support.v4.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v17.leanback.R;
import android.support.v17.leanback.graphics.FitWidthBitmapDrawable;
import android.support.v17.leanback.media.PlaybackGlue;
import android.support.v17.leanback.media.PlaybackGlueHost;
import android.support.v17.leanback.widget.DetailsParallaxDrawable;
import android.support.v17.leanback.widget.ParallaxTarget;

/**
 * Controller for DetailsSupportFragment parallax background and embedded video play.
 * <p>
 * The parallax background drawable is made of two parts: cover drawable (by default
 * {@link FitWidthBitmapDrawable}) above the details overview row and bottom drawable (by default
 * {@link ColorDrawable}) below the details overview row. While vertically scrolling rows, the size
 * of cover drawable and bottom drawable will be updated and the cover drawable will by default
 * perform a parallax shift using {@link FitWidthBitmapDrawable#PROPERTY_VERTICAL_OFFSET}.
 * </p>
 * <pre>
 *        ***************************
 *        *      Cover Drawable     *
 *        * (FitWidthBitmapDrawable)*
 *        *                         *
 *        ***************************
 *        *    DetailsOverviewRow   *
 *        *                         *
 *        ***************************
 *        *     Bottom Drawable     *
 *        *      (ColorDrawable)    *
 *        *         Related         *
 *        *         Content         *
 *        ***************************
 * </pre>
 * Both parallax background drawable and embedded video play are optional. App must call
 * {@link #enableParallax()} and/or {@link #setupVideoPlayback(PlaybackGlue)} explicitly.
 * The PlaybackGlue is automatically {@link PlaybackGlue#play()} when fragment starts and
 * {@link PlaybackGlue#pause()} when fragment stops. When video is ready to play, cover drawable
 * will be faded out.
 * Example:
 * <pre>
 * DetailsSupportFragmentBackgroundController mController = new DetailsSupportFragmentBackgroundController(this);
 *
 * public void onCreate(Bundle savedInstance) {
 *     super.onCreate(savedInstance);
 *     MediaPlayerGlue player = new MediaPlayerGlue(..);
 *     player.setUrl(...);
 *     mController.enableParallax();
 *     mController.setupVideoPlayback(player);
 * }
 *
 * static class MyLoadBitmapTask extends ... {
 *     WeakReference<MyFragment> mFragmentRef;
 *     MyLoadBitmapTask(MyFragment fragment) {
 *         mFragmentRef = new WeakReference(fragment);
 *     }
 *     protected void onPostExecute(Bitmap bitmap) {
 *         MyFragment fragment = mFragmentRef.get();
 *         if (fragment != null) {
 *             fragment.mController.setCoverBitmap(bitmap);
 *         }
 *     }
 * }
 *
 * public void onStart() {
 *     new MyLoadBitmapTask(this).execute(url);
 * }
 *
 * public void onStop() {
 *     mController.setCoverBitmap(null);
 * }
 * </pre>
 * <p>
 * To customize cover drawable and/or bottom drawable, app should call
 * {@link #enableParallax(Drawable, Drawable, ParallaxTarget.PropertyValuesHolderTarget)}.
 * If app supplies a custom cover Drawable, it should not call {@link #setCoverBitmap(Bitmap)}.
 * If app supplies a custom bottom Drawable, it should not call {@link #setSolidColor(int)}.
 * </p>
 * <p>
 * To customize playback fragment, app should override {@link #onCreateVideoSupportFragment()} and
 * {@link #onCreateGlueHost()}.
 * </p>
 *
 */
public class DetailsSupportFragmentBackgroundController {

    private final DetailsSupportFragment mFragment;
    private DetailsParallaxDrawable mParallaxDrawable;
    private int mParallaxDrawableMaxOffset;
    private PlaybackGlue mPlaybackGlue;
    private DetailsBackgroundVideoHelper mVideoHelper;
    private Bitmap mCoverBitmap;
    private int mSolidColor;
    private boolean mCanUseHost = false;

    /**
     * Creates a DetailsSupportFragmentBackgroundController for a DetailsSupportFragment. Note that
     * each DetailsSupportFragment can only associate with one DetailsSupportFragmentBackgroundController.
     *
     * @param fragment The DetailsSupportFragment to control background and embedded video playing.
     * @throws IllegalStateException If fragment was already associated with another controller.
     */
    public DetailsSupportFragmentBackgroundController(DetailsSupportFragment fragment) {
        if (fragment.mDetailsBackgroundController != null) {
            throw new IllegalStateException("Each DetailsSupportFragment is allowed to initialize "
                    + "DetailsSupportFragmentBackgroundController once");
        }
        fragment.mDetailsBackgroundController = this;
        mFragment = fragment;
    }

    /**
     * Enables default parallax background using a {@link FitWidthBitmapDrawable} as cover drawable
     * and {@link ColorDrawable} as bottom drawable. A vertical parallax movement will be applied
     * to the FitWidthBitmapDrawable. App may use {@link #setSolidColor(int)} and
     * {@link #setCoverBitmap(Bitmap)} to change the content of bottom drawable and cover drawable.
     * This method must be called before {@link #setupVideoPlayback(PlaybackGlue)}.
     *
     * @see #setCoverBitmap(Bitmap)
     * @see #setSolidColor(int)
     * @throws IllegalStateException If {@link #setupVideoPlayback(PlaybackGlue)} was called.
     */
    public void enableParallax() {
        int offset = mParallaxDrawableMaxOffset;
        if (offset == 0) {
            offset = mFragment.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.lb_details_cover_drawable_parallax_movement);
        }
        Drawable coverDrawable = new FitWidthBitmapDrawable();
        ColorDrawable colorDrawable = new ColorDrawable();
        enableParallax(coverDrawable, colorDrawable,
                new ParallaxTarget.PropertyValuesHolderTarget(
                        coverDrawable,
                        PropertyValuesHolder.ofInt(FitWidthBitmapDrawable.PROPERTY_VERTICAL_OFFSET,
                                0, -offset)
                ));
    }

    /**
     * Enables parallax background using a custom cover drawable at top and a custom bottom
     * drawable. This method must be called before {@link #setupVideoPlayback(PlaybackGlue)}.
     *
     * @param coverDrawable Custom cover drawable shown at top. {@link #setCoverBitmap(Bitmap)}
     *                      will not work if coverDrawable is not {@link FitWidthBitmapDrawable};
     *                      in that case it's app's responsibility to set content into
     *                      coverDrawable.
     * @param bottomDrawable Drawable shown at bottom. {@link #setSolidColor(int)} will not work
     *                       if bottomDrawable is not {@link ColorDrawable}; in that case it's app's
     *                       responsibility to set content of bottomDrawable.
     * @param coverDrawableParallaxTarget Target to perform parallax effect within coverDrawable.
     *                                    Use null for no parallax movement effect.
     *                                    Example to move bitmap within FitWidthBitmapDrawable:
     *                                    new ParallaxTarget.PropertyValuesHolderTarget(
     *                                        coverDrawable, PropertyValuesHolder.ofInt(
     *                                            FitWidthBitmapDrawable.PROPERTY_VERTICAL_OFFSET,
     *                                            0, -120))
     * @throws IllegalStateException If {@link #setupVideoPlayback(PlaybackGlue)} was called.
     */
    public void enableParallax(@NonNull Drawable coverDrawable, @NonNull Drawable bottomDrawable,
                               @Nullable ParallaxTarget.PropertyValuesHolderTarget
                                       coverDrawableParallaxTarget) {
        if (mParallaxDrawable != null) {
            return;
        }
        // if bitmap is set before enableParallax, use it as initial value.
        if (mCoverBitmap != null && coverDrawable instanceof FitWidthBitmapDrawable) {
            ((FitWidthBitmapDrawable) coverDrawable).setBitmap(mCoverBitmap);
        }
        // if solid color is set before enableParallax, use it as initial value.
        if (mSolidColor != Color.TRANSPARENT && bottomDrawable instanceof ColorDrawable) {
            ((ColorDrawable) bottomDrawable).setColor(mSolidColor);
        }
        if (mPlaybackGlue != null) {
            throw new IllegalStateException("enableParallaxDrawable must be called before "
                    + "enableVideoPlayback");
        }
        mParallaxDrawable = new DetailsParallaxDrawable(
                mFragment.getContext(),
                mFragment.getParallax(),
                coverDrawable,
                bottomDrawable,
                coverDrawableParallaxTarget);
        mFragment.setBackgroundDrawable(mParallaxDrawable);
    }

    /**
     * Enable video playback and set proper {@link PlaybackGlueHost}. This method by default
     * creates a VideoSupportFragment and VideoSupportFragmentGlueHost to host the PlaybackGlue.
     * This method must be called after calling details Fragment super.onCreate().
     *
     * @param playbackGlue
     * @see #onCreateVideoSupportFragment()
     * @see #onCreateGlueHost().
     */
    public void setupVideoPlayback(@NonNull PlaybackGlue playbackGlue) {
        if (mPlaybackGlue == playbackGlue) {
            return;
        }
        mPlaybackGlue = playbackGlue;
        mVideoHelper = new DetailsBackgroundVideoHelper(mPlaybackGlue,
                mFragment.getParallax(), mParallaxDrawable.getCoverDrawable());
        if (mCanUseHost) {
            mPlaybackGlue.setHost(onCreateGlueHost());
        }
    }

    /**
     * Enable Host for PlaybackGlue. This is delayed until: onStart() is called,
     * activity transitions are finished.
     */
    void enablePlaybackHost() {
        if (!mCanUseHost) {
            mCanUseHost = true;
            if (mPlaybackGlue != null) {
                mPlaybackGlue.setHost(onCreateGlueHost());
            }
        }
        if (mPlaybackGlue != null && mPlaybackGlue.isReadyForPlayback()) {
            mPlaybackGlue.play();
        }
    }

    void onStop() {
        if (mPlaybackGlue != null) {
            mPlaybackGlue.pause();
        }
    }

    /**
     * Disable parallax that would auto-start video playback
     * @return true if video fragment is visible or false otherwise.
     */
    boolean disableVideoParallax() {
        if (mVideoHelper != null) {
            mVideoHelper.stopParallax();
            return mVideoHelper.isVideoVisible();
        }
        return false;
    }

    /**
     * Returns the cover drawable at top. Returns null if {@link #enableParallax()} is not called.
     * By default it's a {@link FitWidthBitmapDrawable}.
     *
     * @return The cover drawable at top.
     */
    public final Drawable getCoverDrawable() {
        if (mParallaxDrawable == null) {
            return null;
        }
        return mParallaxDrawable.getCoverDrawable();
    }

    /**
     * Returns the drawable at bottom. Returns null if {@link #enableParallax()} is not called.
     * By default it's a {@link ColorDrawable}.
     *
     * @return The bottom drawable.
     */
    public final Drawable getBottomDrawable() {
        if (mParallaxDrawable == null) {
            return null;
        }
        return mParallaxDrawable.getBottomDrawable();
    }

    /**
     * Creates a Fragment to host {@link PlaybackGlue}. Returns a new {@link VideoSupportFragment} by
     * default. App may override and return a different fragment and it also must override
     * {@link #onCreateGlueHost()}.
     *
     * @return A new fragment used in {@link #onCreateGlueHost()}.
     * @see #onCreateGlueHost()
     * @see #setupVideoPlayback(PlaybackGlue)
     */
    public Fragment onCreateVideoSupportFragment() {
        return new VideoSupportFragment();
    }

    /**
     * Creates a PlaybackGlueHost to host PlaybackGlue. App may override this if it overrides
     * {@link #onCreateVideoSupportFragment()}. This method must be called after calling Fragment
     * super.onCreate(). When override this method, app may call
     * {@link #findOrCreateVideoSupportFragment()} to get or create a fragment.
     *
     * @return A new PlaybackGlueHost to host PlaybackGlue.
     * @see #onCreateVideoSupportFragment()
     * @see #findOrCreateVideoSupportFragment()
     * @see #setupVideoPlayback(PlaybackGlue)
     */
    public PlaybackGlueHost onCreateGlueHost() {
        return new VideoSupportFragmentGlueHost((VideoSupportFragment) findOrCreateVideoSupportFragment());
    }

    /**
     * Adds or gets fragment for rendering video in DetailsSupportFragment. A subclass that
     * overrides {@link #onCreateGlueHost()} should call this method to get a fragment for creating
     * a {@link PlaybackGlueHost}.
     *
     * @return Fragment the added or restored fragment responsible for rendering video.
     * @see #onCreateGlueHost()
     */
    public final Fragment findOrCreateVideoSupportFragment() {
        return mFragment.findOrCreateVideoSupportFragment();
    }

    /**
     * Convenient method to set Bitmap in cover drawable. If app is not using default
     * {@link FitWidthBitmapDrawable}, app should not use this method  It's safe to call
     * setCoverBitmap() before calling {@link #enableParallax()}.
     *
     * @param bitmap bitmap to set as cover.
     */
    public final void setCoverBitmap(Bitmap bitmap) {
        mCoverBitmap = bitmap;
        Drawable drawable = getCoverDrawable();
        if (drawable instanceof FitWidthBitmapDrawable) {
            ((FitWidthBitmapDrawable) drawable).setBitmap(mCoverBitmap);
        }
    }

    /**
     * Returns Bitmap set by {@link #setCoverBitmap(Bitmap)}.
     *
     * @return Bitmap for cover drawable.
     */
    public final Bitmap getCoverBitmap() {
        return mCoverBitmap;
    }

    /**
     * Returns color set by {@link #setSolidColor(int)}.
     *
     * @return Solid color used for bottom drawable.
     */
    public final @ColorInt int getSolidColor() {
        return mSolidColor;
    }

    /**
     * Convenient method to set color in bottom drawable. If app is not using default
     * {@link ColorDrawable}, app should not use this method. It's safe to call setSolidColor()
     * before calling {@link #enableParallax()}.
     *
     * @param color color for bottom drawable.
     */
    public final void setSolidColor(@ColorInt int color) {
        mSolidColor = color;
        Drawable bottomDrawable = getBottomDrawable();
        if (bottomDrawable instanceof ColorDrawable) {
            ((ColorDrawable) bottomDrawable).setColor(color);
        }
    }

    /**
     * Sets default parallax offset in pixels for bitmap moving vertically. This method must
     * be called before {@link #enableParallax()}.
     *
     * @param offset Offset in pixels (e.g. 120).
     * @see #enableParallax()
     */
    public final void setParallaxDrawableMaxOffset(int offset) {
        if (mParallaxDrawable != null) {
            throw new IllegalStateException("enableParallax already called");
        }
        mParallaxDrawableMaxOffset = offset;
    }

    /**
     * Returns Default parallax offset in pixels for bitmap moving vertically.
     * When 0, a default value would be used.
     *
     * @return Default parallax offset in pixels for bitmap moving vertically.
     * @see #enableParallax()
     */
    public final int getParallaxDrawableMaxOffset() {
        return mParallaxDrawableMaxOffset;
    }

}
