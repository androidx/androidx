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
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.R;

import org.jspecify.annotations.NonNull;

/**
 * An ImageView that can display, combine and filter images. <b>Added in 2.0</b>
 * <p>
 * Subclass of ImageView to handle various common filtering operations
 * </p>
 *
 * <h2>ImageFilterView attributes</h2>
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
 *     For squares 1 will result in a circle</td>
 * </tr>
 * <tr>
 * <td>overlay</td>
 * <td>Defines whether the alt image will be faded in on top of the original image or if it will be
 *     crossfaded with it. Default is true. Set to false for semitransparent objects</td>
 * </tr>
 * </table>
 */
public class ImageFilterView extends androidx.appcompat.widget.AppCompatImageView {
    static class ImageMatrix {
        float[] mMatrix = new float[4 * 5];
        ColorMatrix mColorMatrix = new ColorMatrix();
        ColorMatrix mTmpColorMatrix = new ColorMatrix();
        float mBrightness = 1;
        float mSaturation = 1;
        float mContrast = 1;
        float mWarmth = 1;

        private void saturation(float saturationStrength) {
            float Rf = 0.2999f;
            float Gf = 0.587f;
            float Bf = 0.114f;
            float s = saturationStrength;

            float ms = 1.0f - s;
            float Rt = Rf * ms;
            float Gt = Gf * ms;
            float Bt = Bf * ms;

            mMatrix[0] = (Rt + s);
            mMatrix[1] = Gt;
            mMatrix[2] = Bt;
            mMatrix[3] = 0;
            mMatrix[4] = 0;

            mMatrix[5] = Rt;
            mMatrix[6] = (Gt + s);
            mMatrix[7] = Bt;
            mMatrix[8] = 0;
            mMatrix[9] = 0;

            mMatrix[10] = Rt;
            mMatrix[11] = Gt;
            mMatrix[12] = (Bt + s);
            mMatrix[13] = 0;
            mMatrix[14] = 0;

            mMatrix[15] = 0;
            mMatrix[16] = 0;
            mMatrix[17] = 0;
            mMatrix[18] = 1;
            mMatrix[19] = 0;
        }

        private void warmth(float warmth) {
            float baseTemperature = 5000;
            if (warmth <= 0) warmth = .01f;
            float tmpColor_r;
            float tmpColor_g;
            float tmpColor_b;

            float kelvin = baseTemperature / warmth;
            { // simulate a black body radiation
                float centiKelvin = kelvin / 100;
                float colorR, colorG, colorB;
                if (centiKelvin > 66) {
                    float tmp = centiKelvin - 60.f;
                    // Original statements (all decimal values)
                    // colorR = (329.698727446f * (float) Math.pow(tmp, -0.1332047592f))
                    // colorG = (288.1221695283f * (float) Math.pow(tmp, 0.0755148492f))
                    colorR = (329.69873f * (float) Math.pow(tmp, -0.13320476f));
                    colorG = (288.12216f * (float) Math.pow(tmp, 0.07551485f));
                } else {
                    // Original statements (all decimal values)
                    // colorG = (99.4708025861f * (float) Math.log(centiKelvin) - 161.1195681661f);
                    colorG = (99.4708f * (float) Math.log(centiKelvin) - 161.11957f);
                    colorR = 255;
                }
                if (centiKelvin < 66) {
                    if (centiKelvin > 19) {
                        // Original statements (all decimal values)
                        // 138.5177312231f * (float) Math.log(centiKelvin - 10) - 305.0447927307f);
                        colorB = (138.51773f
                                * (float) Math.log(centiKelvin - 10) - 305.0448f);
                    } else {
                        colorB = 0;
                    }
                } else {
                    colorB = 255;
                }
                tmpColor_r = Math.min(255, Math.max(colorR, 0));
                tmpColor_g = Math.min(255, Math.max(colorG, 0));
                tmpColor_b = Math.min(255, Math.max(colorB, 0));
            }

            float color_r = tmpColor_r;
            float color_g = tmpColor_g;
            float color_b = tmpColor_b;
            kelvin = baseTemperature;
            { // simulate a black body radiation
                float centiKelvin = kelvin / 100;
                float colorR, colorG, colorB;
                if (centiKelvin > 66) {
                    float tmp = centiKelvin - 60.f;
                    // Original statements (all decimal values)
                    //  colorR = (329.698727446f * (float) Math.pow(tmp, -0.1332047592f));
                    //  colorG = (288.1221695283f * (float) Math.pow(tmp, 0.0755148492f));
                    colorR = (329.69873f * (float) Math.pow(tmp, -0.13320476f));
                    colorG = (288.12216f * (float) Math.pow(tmp, 0.07551485f));

                } else {
                    // Original statements (all decimal values)
                    //float of (99.4708025861f * (float) Math.log(centiKelvin) - 161.1195681661f);
                    colorG = (99.4708f * (float) Math.log(centiKelvin) - 161.11957f);
                    colorR = 255;
                }
                if (centiKelvin < 66) {
                    if (centiKelvin > 19) {
                        // Original statements (all decimal values)
                        //float of (138.5177312231 * Math.log(centiKelvin - 10) - 305.0447927307);
                        colorB = (138.51773f * (float) Math.log(centiKelvin - 10) - 305.0448f);
                    } else {
                        colorB = 0;
                    }
                } else {
                    colorB = 255;
                }
                tmpColor_r = Math.min(255, Math.max(colorR, 0));
                tmpColor_g = Math.min(255, Math.max(colorG, 0));
                tmpColor_b = Math.min(255, Math.max(colorB, 0));
            }

            color_r /= tmpColor_r;
            color_g /= tmpColor_g;
            color_b /= tmpColor_b;
            mMatrix[0] = color_r;
            mMatrix[1] = 0;
            mMatrix[2] = 0;
            mMatrix[3] = 0;
            mMatrix[4] = 0;

            mMatrix[5] = 0;
            mMatrix[6] = color_g;
            mMatrix[7] = 0;
            mMatrix[8] = 0;
            mMatrix[9] = 0;

            mMatrix[10] = 0;
            mMatrix[11] = 0;
            mMatrix[12] = color_b;
            mMatrix[13] = 0;
            mMatrix[14] = 0;

            mMatrix[15] = 0;
            mMatrix[16] = 0;
            mMatrix[17] = 0;
            mMatrix[18] = 1;
            mMatrix[19] = 0;
        }

        private void brightness(float brightness) {

            mMatrix[0] = brightness;
            mMatrix[1] = 0;
            mMatrix[2] = 0;
            mMatrix[3] = 0;
            mMatrix[4] = 0;

            mMatrix[5] = 0;
            mMatrix[6] = brightness;
            mMatrix[7] = 0;
            mMatrix[8] = 0;
            mMatrix[9] = 0;

            mMatrix[10] = 0;
            mMatrix[11] = 0;
            mMatrix[12] = brightness;
            mMatrix[13] = 0;
            mMatrix[14] = 0;

            mMatrix[15] = 0;
            mMatrix[16] = 0;
            mMatrix[17] = 0;
            mMatrix[18] = 1;
            mMatrix[19] = 0;
        }

        void updateMatrix(ImageView view) {
            mColorMatrix.reset();
            boolean filter = false;
            if (mSaturation != 1.0f) {
                saturation(mSaturation);
                mColorMatrix.set(mMatrix);
                filter = true;
            }
            if (mContrast != 1.0f) {
                mTmpColorMatrix.setScale(mContrast, mContrast, mContrast, 1);
                mColorMatrix.postConcat(mTmpColorMatrix);
                filter = true;
            }
            if (mWarmth != 1.0f) {
                warmth(mWarmth);
                mTmpColorMatrix.set(mMatrix);
                mColorMatrix.postConcat(mTmpColorMatrix);
                filter = true;
            }
            if (mBrightness != 1.0f) {
                brightness(mBrightness);
                mTmpColorMatrix.set(mMatrix);
                mColorMatrix.postConcat(mTmpColorMatrix);
                filter = true;
            }

            if (filter) {
                view.setColorFilter(new ColorMatrixColorFilter(mColorMatrix));
            } else {
                view.clearColorFilter();
            }
        }
    }

    private ImageMatrix mImageMatrix = new ImageMatrix();
    private boolean mOverlay = true;
    private Drawable mAltDrawable = null;
    private Drawable mDrawable = null;
    private float mCrossfade = 0;
    private float mRoundPercent = 0; // rounds the corners as a percent
    private float mRound = Float.NaN; // rounds the corners in dp if NaN RoundPercent is in effect
    private Path mPath;
    ViewOutlineProvider mViewOutlineProvider;
    RectF mRect;

    Drawable[] mLayers = new Drawable[2];
    LayerDrawable mLayer;

    // ======================== support for pan/zoom/rotate =================
    // defined as 0 = center of screen
    // if with < scree with,  1 is the right edge lines up with screen
    // if width > screen width, 1 is thee left edge lines up
    // -1 works similarly
    // zoom 1 = the image fits such that the view is filed

    float mPanX = Float.NaN;
    float mPanY = Float.NaN;
    float mZoom = Float.NaN;
    float mRotate = Float.NaN;

    /**
     * Gets the pan from the center
     * pan of 1 the image is "all the way to the right"
     * if the images width is greater than the screen width,
     * pan = 1 results in the left edge lining up
     * if the images width is less than the screen width,
     * pan = 1 results in the right edges lining up
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
     * pan = 1 results in the bottom edge lining up
     * if the images width is less than the screen height,
     * pan = 1 results in the top edges lining up
     * if image height == screen height it does nothing
     *
     * @return pan in y. Where 0 is centered NaN if not set
     */
    public float getImagePanY() {
        return mPanY;
    }

    /**
     * gets the zoom where 1 scales the image just enough to fill the view
     *
     * @return the zoom factor
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
     * @param pan sets the pan in X. Where 0 is centered
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
     * Set the alternative Image resource used in cross fading
     * @param resId id of drawable
     */
    public void setAltImageResource(int resId) {
        mAltDrawable = AppCompatResources.getDrawable(getContext(), resId);
        setAltImageDrawable(mAltDrawable);
    }

    /**
     * Set the alternative Image Drawable used in cross fading.
     * @param altDrawable of drawable
     */
    public void setAltImageDrawable(Drawable altDrawable) {
        mAltDrawable = altDrawable.mutate();
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

    public ImageFilterView(Context context) {
        super(context);
        init(context, null);
    }

    public ImageFilterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ImageFilterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
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
                } else if (attr == R.styleable.ImageFilterView_brightness) {
                    setBrightness(a.getFloat(attr, 0));
                } else if (attr == R.styleable.ImageFilterView_round) {
                    setRound(a.getDimension(attr, 0));
                } else if (attr == R.styleable.ImageFilterView_roundPercent) {
                    setRoundPercent(a.getFloat(attr, 0));
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

    /**
     * Defines whether the alt image will be faded in on top
     * of the original image or if it will be crossfaded with it.
     * Default is true;
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
     * Returns the currently applied brightness
     *
     * @return brightness 0 = black, 1 = original, 2 = twice as bright
     */
    public float getBrightness() {
        return mImageMatrix.mBrightness;
    }

    /**
     * Set the corner radius of curvature  as a fraction of the smaller side.
     * For squares 1 will result in a circle
     *
     * @param round the radius of curvature as a fraction of the smaller width
     */
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

            int w = getWidth();
            int h = getHeight();
            float r = Math.min(w, h) * mRoundPercent / 2;
            mRect.set(0, 0, w, h);
            mPath.reset();
            mPath.addRoundRect(mRect, r, r, Path.Direction.CW);
        } else {
            setClipToOutline(false);
        }
        if (change) {
            invalidateOutline();
        }

    }

    /**
     * Set the corner radius of curvature
     *
     * @param round the radius of curvature  NaN = default meaning roundPercent in effect
     */
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
            int w = getWidth();
            int h = getHeight();
            mRect.set(0, 0, w, h);
            mPath.reset();
            mPath.addRoundRect(mRect, mRound, mRound, Path.Direction.CW);
        } else {
            setClipToOutline(false);
        }
        if (change) {
            invalidateOutline();
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
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        setMatrix();
    }
}
