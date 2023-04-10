/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.R;

/**
 * A MotionButton is an AppCompatButton that can round its edges. <b>Added in 2.0</b>
 * <p>
 * Subclass of AppCompatButton to handle rounding edges dynamically.
 * </p>
 * <h2>MotionButton attributes</h2>
 * <td>round</td>
 * <td>(id) call the TransitionListener with this trigger id</td>
 * </tr>
 * <tr>
 * <td>roundPercent</td>
 * <td>Set the corner radius of curvature  as a fraction of the smaller side.
 * For squares 1 will result in a circle</td>
 * </tr>
 * <tr>
 * <td>round</td>
 * <td>Set the corner radius of curvature  as a fraction of the smaller side.
 * For squares 1 will result in a circle</td>
 * </tr>
 *
 * </table>
 */
public class MotionButton extends androidx.appcompat.widget.AppCompatButton {
    private float mRoundPercent = 0; // rounds the corners as a percent
    private float mRound = Float.NaN; // rounds the corners in dp if NaN RoundPercent is in effect
    private Path mPath;
    ViewOutlineProvider mViewOutlineProvider;
    RectF mRect;

    public MotionButton(Context context) {
        super(context);
        init(context, null);
    }

    public MotionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MotionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setPadding(0, 0, 0, 0);
        if (attrs != null) {
            TypedArray a = context
                    .obtainStyledAttributes(attrs, R.styleable.ImageFilterView);
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ImageFilterView_round) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setRound(a.getDimension(attr, 0));
                    }
                } else if (attr == R.styleable.ImageFilterView_roundPercent) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setRoundPercent(a.getFloat(attr, 0));
                    }
                }
            }
            a.recycle();
        }
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
    public void draw(@NonNull Canvas canvas) {
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
}
