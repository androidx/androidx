/*
 * Copyright (C) 2018 The Android Open Source Project
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
package androidx.constraintlayout.utils.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.R;

/**
 * An AppCompatImageButton that can display, combine and filter images. <b>Added in 2.0</b>
 * <p>
 * Subclass of AppCompatImageButton to handle various common filtering operations.
 * </p>
 * <h2>ImageFilterButton attributes</h2>
 * <table summary="KeyTrigger attributes">
 * <tr>
 * <td>altSrc</td>
 * <td>Provide and alternative image to the src image to allow cross fading</td>
 * </tr>
 * <tr>
 * <td>saturation</td>
 * <td>Sets the saturation of the image.<br>  0 = grayscale, 1 = original, 2 = hyper saturated</td>
 * </tr
 * <tr>
 * <td>brightness</td>
 * <td>Sets the brightness of the image.<br>  0 = black, 1 = original, 2 = twice as bright
 * </td>
 * </tr>
 * <tr>
 * <td>warmth</td>
 * <td>This adjust the apparent color temperature of the image.<br> 1=neutral, 2=warm, .5=cold</td>
 * </tr>
 * <tr>
 * <td>contrast</td>
 * <td>This sets the contrast. 1 = unchanged, 0 = gray, 2 = high contrast</td>
 * </tr>
 * <tr>
 * <td>crossfade</td>
 * <td>Set the current mix between the two images. <br>  0=src 1= altSrc image</td>
 * </tr>
 * <tr>
 * <td>round</td>
 * <td>(id) call the TransitionListener with this trigger id</td>
 * </tr>
 * <tr>
 * <td>roundPercent</td>
 * <td>Set the corner radius of curvature  as a fraction of the smaller side.
 *    For squares 1 will result in a circle</td>
 * </tr>
 * <tr>
 * <td>overlay</td>
 * <td>Defines whether the alt image will be faded in on top of the original image or if it will be
 * crossfaded with it. Default is true. Set to false for semitransparent objects</td>
 * </tr>
 * </table>
 */
public class ImageFilterButton extends androidx.appcompat.widget.AppCompatImageButton {
    private ImageFilterView.ImageMatrix mImageMatrix = new ImageFilterView.ImageMatrix();
    private float mCrossfade = 0;
    private float mRoundPercent = 0; // rounds the corners as a percent
    private float mRound = Float.NaN; // rounds the corners in dp if NaN RoundPercent is in effect
    private Path mPath;
    ViewOutlineProvider mViewOutlineProvider;
    RectF mRect;

    Drawable[] mLayers = new Drawable[2];
    LayerDrawable mLayer;
    private boolean mOverlay = true;
    private Drawable mAltDrawable = null;
    private Drawable mDrawable = null;

    public ImageFilterButton(Context context) {
        super(context);
        init(context, null);
    }

    public ImageFilterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ImageFilterButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setPadding(0, 0, 0, 0);
        if (attrs != null) {
            TypedArray a = context
                    .obtainStyledAttributes(attrs, R.styleable.ImageFilterView);
            final int count = a.getIndexCount();
            mAltDrawable = a.getDrawable(R.styleable.ImageFilterView_altSrc);

            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ImageFilterView_crossfade) {
                    mCrossfade = a.getFloat(attr, 0);
                } else if (attr == R.styleable.ImageFilterView_warmth) {
                    setWarmth(a.getFloat(attr, 0));
                } else if (attr == R.styleable.ImageFilterView_saturation) {
                    setSaturation(a.getFloat(attr, 0));
                } else if (attr == R.styleable.ImageFilterView_contrast) {
                    setContrast(a.getFloat(attr, 0));
                } else if (attr == R.styleable.ImageFilterView_round) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setRound(a.getDimension(attr, 0));
                    }
                } else if (attr == R.styleable.ImageFilterView_roundPercent) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setRoundPercent(a.getFloat(attr, 0));
                    }
                } else if (attr == R.styleable.ImageFilterView_overlay) {
                    setOverlay(a.getBoolean(attr, mOverlay));
                } else if (attr == R.styleable.ImageFilterView_imagePanX) {
                    setImagePanX(a.getFloat(attr, mPanX));
                } else if (attr == R.styleable.ImageFilterView_imagePanY) {
                    setImagePanY(a.getFloat(attr, mPanY));
                } else if (attr == R.styleable.ImageFilterView_imageRotate) {
                    setImageRotate(a.getFloat(attr, mRotate));
                } else if (attr == R.styleable.ImageFilterView_imageZoom) {
                    setImageZoom(a.getFloat(attr, mZoom));
                }
            }
            a.recycle();

            mDrawable = getDrawable();
            if (mAltDrawable != null && mDrawable != null) {

                mLayers[0] = mDrawable = getDrawable().mutate();
                mLayers[1] = mAltDrawable.mutate();

                mLayer = new LayerDrawable(mLayers);
                mLayer.getDrawable(1).setAlpha((int) (255 * mCrossfade));
                if (!mOverlay) {
                    mLayer.getDrawable(0).setAlpha((int) (255 * (1 - mCrossfade)));
                }
                super.setImageDrawable(mLayer);
            } else {
                mDrawable = getDrawable();
                if (mDrawable != null) {
                    mLayers[0] = mDrawable = mDrawable.mutate();
                }
            }
        }
    }
    // ======================== support for pan/zoom/rotate =================
    // defined as 0 = center of screen
    // if with < scree with,  1 is the right edge lines up with screen
    // if width > screen width, 1 is thee left edge lines up
    // -1 works similarly
    // zoom 1 = the image fits such that the view is filed

    private float mPanX = Float.NaN;
    private float mPanY = Float.NaN;
    private float mZoom = Float.NaN;
    private float mRotate = Float.NaN;

    /**
     * gts the pan from the center
     * pan of 1 the image is "all the way to the right"
     * if the images width is greater than the screen width,
     *  pan = 1 results in the left edge lining up
     * if the images width is less than the screen width,
     *  pan = 1 results in the right edges lining up
     * if image width == screen width it does nothing
     *
     * @return the pan in X. Where 0 is centered = Float. NaN if not set
     */
    public float getImagePanX() {
        return mPanX;
    }

    /**
     * gets the pan from the center
     * pan of 1 the image is "all the way to the bottom"
     * if the images width is greater than the screen height,
     *  pan = 1 results in the bottom edge lining up
     * if the images width is less than the screen height,
     *  pan = 1 results in the top edges lining up
     * if image height == screen height it does nothing
     *
     * @return pan in Y. Where 0 is centered
     */
    public float getImagePanY() {
        return mPanY;
    }

    /**
     * sets the zoom where 1 scales the image just enough to fill the view
     *
     * @return the zoom
     */
    public float getImageZoom() {
        return mZoom;
    }

    /**
     * gets the rotation
     *
     * @return the rotation in degrees
     */
    public float getImageRotate() {
        return mRotate;
    }

    /**
     * sets the pan from the center
     * pan of 1 the image is "all the way to the right"
     * if the images width is greater than the screen width,
     * pan = 1 results in the left edge lining up
     * if the images width is less than the screen width,
     * pan = 1 results in the right edges lining up
     * if image width == screen width it does nothing
     *
     * @param pan sets the pan in X. Where 0 is centered
     */
    public void setImagePanX(float pan) {
        mPanX = pan;
        updateViewMatrix();
    }

    /**
     * sets the pan from the center
     * pan of 1 the image is "all the way to the bottom"
     * if the images width is greater than the screen height,
     * pan = 1 results in the bottom edge lining up
     * if the images width is less than the screen height,
     * pan = 1 results in the top edges lining up
     * if image height == screen height it does nothing
     *
     * @param pan sets the pan in Y. Where 0 is centered
     */
    public void setImagePanY(float pan) {
        mPanY = pan;
        updateViewMatrix();
    }

    /**
     * sets the zoom where 1 scales the image just enough to fill the view
     *
     * @param zoom the zoom factor
     */
    public void setImageZoom(float zoom) {
        mZoom = zoom;
        updateViewMatrix();
    }

    /**
     * sets the rotation angle of the image in degrees
     *
     * @param rotation the rotation in degrees
     */
    public void setImageRotate(float rotation) {
        mRotate = rotation;
        updateViewMatrix();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (mAltDrawable != null && drawable != null) {
            mDrawable = drawable.mutate();
            mLayers[0] = mDrawable;
            mLayers[1] = mAltDrawable;
            mLayer = new LayerDrawable(mLayers);
            super.setImageDrawable(mLayer);
            setCrossfade(mCrossfade);
        } else {
            super.setImageDrawable(drawable);
        }
    }

    @Override
    public void setImageResource(int resId) {
        if (mAltDrawable != null) {
            mDrawable = AppCompatResources.getDrawable(getContext(), resId).mutate();
            mLayers[0] = mDrawable;
            mLayers[1] = mAltDrawable;
            mLayer = new LayerDrawable(mLayers);
            super.setImageDrawable(mLayer);
            setCrossfade(mCrossfade);
        } else {
            super.setImageResource(resId);
        }
    }

    /**
     * Set the alternative image used to cross fade to.
     *
     * @param resId
     */
    public void setAltImageResource(int resId) {
        mAltDrawable = AppCompatResources.getDrawable(getContext(), resId).mutate();
        mLayers[0] = mDrawable;
        mLayers[1] = mAltDrawable;
        mLayer = new LayerDrawable(mLayers);
        super.setImageDrawable(mLayer);
        setCrossfade(mCrossfade);
    }

    private void updateViewMatrix() {
        if (Float.isNaN(mPanX)
                && Float.isNaN(mPanY)
                && Float.isNaN(mZoom)
                && Float.isNaN(mRotate)
        ) {
            setScaleType(ScaleType.FIT_CENTER);
            return;
        }
        setMatrix();
    }

    private void setMatrix() {
        if (Float.isNaN(mPanX)
                && Float.isNaN(mPanY)
                && Float.isNaN(mZoom)
                && Float.isNaN(mRotate)
        ) {
            return;
        }
        float panX = Float.isNaN(mPanX) ? 0 : mPanX;
        float panY = Float.isNaN(mPanY) ? 0 : mPanY;
        float zoom = Float.isNaN(mZoom) ? 1 : mZoom;
        float rota = Float.isNaN(mRotate) ? 0 : mRotate;
        Matrix imageMatrix = new Matrix();
        imageMatrix.reset();
        float iw = getDrawable().getIntrinsicWidth();
        float ih = getDrawable().getIntrinsicHeight();
        float sw = getWidth();
        float sh = getHeight();
        float scale = zoom * ((iw * sh < ih * sw) ? sw / iw : sh / ih);
        imageMatrix.postScale(scale, scale);
        float tx = 0.5f * (panX * (sw - scale * iw) + sw - (scale * iw));
        float ty = 0.5f * (panY * (sh - scale * ih) + sh - (scale * ih));
        imageMatrix.postTranslate(tx, ty);
        imageMatrix.postRotate(rota, sw / 2, sh / 2);
        setImageMatrix(imageMatrix);
        setScaleType(ScaleType.MATRIX);
    }
    // ================================================================

    /**
     * Defines whether the alt image will be faded in on top of the original image or if it will be
     * crossfaded with it. Default is true.
     *
     * @param overlay
     */
    private void setOverlay(boolean overlay) {
        mOverlay = overlay;
    }

    /**
     * sets the saturation of the image;
     * 0 = grayscale, 1 = original, 2 = hyper saturated
     *
     * @param saturation
     */

    public void setSaturation(float saturation) {
        mImageMatrix.mSaturation = saturation;
        mImageMatrix.updateMatrix(this);
    }

    /**
     * Returns the currently applied saturation
     *
     * @return 0 = grayscale, 1 = original, 2 = hyper saturated
     */
    public float getSaturation() {
        return mImageMatrix.mSaturation;
    }

    /**
     * This sets the contrast. 1 = unchanged, 0 = gray, 2 = high contrast
     *
     * @param contrast
     */
    public void setContrast(float contrast) {
        mImageMatrix.mContrast = contrast;
        mImageMatrix.updateMatrix(this);
    }

    /**
     * Returns the currently applied contrast
     *
     * @return 1 = unchanged, 0 = gray, 2 = high contrast
     */
    public float getContrast() {
        return mImageMatrix.mContrast;
    }

    /**
     * This makes the apparent color temperature of the image warmer or colder.
     *
     * @param warmth 1 is neutral, 2 is warm, .5 is cold
     */
    public void setWarmth(float warmth) {
        mImageMatrix.mWarmth = warmth;
        mImageMatrix.updateMatrix(this);
    }

    /**
     * Returns the currently applied warmth
     *
     * @return warmth 1 is neutral, 2 is warm, .5 is cold
     */
    public float getWarmth() {
        return mImageMatrix.mWarmth;
    }

    /**
     * Set the current mix between the two images that can be set on this view.
     *
     * @param crossfade a number from 0 to 1
     */
    public void setCrossfade(float crossfade) {

        mCrossfade = crossfade;
        if (mLayers != null) {
            if (!mOverlay) {
                mLayer.getDrawable(0).setAlpha((int) (255 * (1 - mCrossfade)));
            }
            mLayer.getDrawable(1).setAlpha((int) (255 * mCrossfade));
            super.setImageDrawable(mLayer);
        }
    }

    /**
     * Returns the currently applied crossfade.
     *
     * @return a number from 0 to 1
     */
    public float getCrossfade() {
        return mCrossfade;
    }

    /**
     * sets the brightness of the image;
     * 0 = black, 1 = original, 2 = twice as bright
     *
     * @param brightness
     */

    public void setBrightness(float brightness) {
        mImageMatrix.mBrightness = brightness;
        mImageMatrix.updateMatrix(this);
    }

    /**
     * Set the corner radius of curvature  as a fraction of the smaller side.
     * For squares 1 will result in a circle
     *
     * @param round the radius of curvature as a fraction of the smaller width
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public void setRoundPercent(float round) {
        boolean change = (mRoundPercent != round);
        mRoundPercent = round;
        if (mRoundPercent != 0.0f) {
            if (mPath == null) {
                mPath = new Path();
            }
            if (mRect == null) {
                mRect = new RectF();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mViewOutlineProvider == null) {
                    mViewOutlineProvider = new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, Outline outline) {
                            int w = getWidth();
                            int h = getHeight();
                            float r = Math.min(w, h) * mRoundPercent / 2;
                            outline.setRoundRect(0, 0, w, h, r);
                        }
                    };
                    setOutlineProvider(mViewOutlineProvider);
                }
                setClipToOutline(true);
            }
            int w = getWidth();
            int h = getHeight();
            float r = Math.min(w, h) * mRoundPercent / 2;
            mRect.set(0, 0, w, h);
            mPath.reset();
            mPath.addRoundRect(mRect, r, r, Path.Direction.CW);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setClipToOutline(false);
            }
        }
        if (change) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                invalidateOutline();
            }
        }

    }

    /**
     * Set the corner radius of curvature
     *
     * @param round the radius of curvature  NaN = default meaning roundPercent in effect
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public void setRound(float round) {
        if (Float.isNaN(round)) {
            mRound = round;
            float tmp = mRoundPercent;
            mRoundPercent = -1;
            setRoundPercent(tmp); // force eval of roundPercent
            return;
        }
        boolean change = (mRound != round);
        mRound = round;

        if (mRound != 0.0f) {
            if (mPath == null) {
                mPath = new Path();
            }
            if (mRect == null) {
                mRect = new RectF();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mViewOutlineProvider == null) {
                    mViewOutlineProvider = new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, Outline outline) {
                            int w = getWidth();
                            int h = getHeight();
                            outline.setRoundRect(0, 0, w, h, mRound);
                        }
                    };
                    setOutlineProvider(mViewOutlineProvider);
                }
                setClipToOutline(true);

            }
            int w = getWidth();
            int h = getHeight();
            mRect.set(0, 0, w, h);
            mPath.reset();
            mPath.addRoundRect(mRect, mRound, mRound, Path.Direction.CW);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setClipToOutline(false);
            }
        }
        if (change) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                invalidateOutline();
            }
        }

    }

    /**
     * Get the fractional corner radius of curvature.
     *
     * @return Fractional radius of curvature with respect to smallest size
     */
    public float getRoundPercent() {
        return mRoundPercent;
    }

    /**
     * Get the corner radius of curvature NaN = RoundPercent in effect.
     *
     * @return Radius of curvature
     */
    public float getRound() {
        return mRound;
    }

    @Override
    public void draw(Canvas canvas) {
        boolean clip = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (mRound != 0.0f && mPath != null) {
                clip = true;
                canvas.save();
                canvas.clipPath(mPath);
            }
        }
        super.draw(canvas);
        if (clip) {
            canvas.restore();
        }
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        setMatrix();
    }
}
