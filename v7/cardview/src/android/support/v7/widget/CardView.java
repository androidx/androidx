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

package android.support.v7.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.cardview.R;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * A ViewGroup with a rounded corner background and shadow behind.
 * <p>
 * CardView uses <code>elevation</code> property on L for shadows and falls back to a custom shadow
 * implementation on older platforms.
 * <p>
 * Due to expensive nature of rounded corner clipping, on platforms before L, CardView does not clip
 * its children that intersect with rounded corners. Instead, it adds padding to avoid such
 * intersection.
 *
 * @attr ref android.support.v7.cardview.R.styleable#CardView_cardBackgroundColor
 * @attr ref android.support.v7.cardview.R.styleable#CardView_cardCornerRadius
 */
public class CardView extends FrameLayout implements CardViewDelegate {

    private final static CardViewImpl IMPL;
    static {
        if ("L".equals(Build.VERSION.CODENAME) || Build.VERSION.SDK_INT >= 21) {
            IMPL = new CardViewApi21();
        } else if (Build.VERSION.SDK_INT >= 17) {
            IMPL = new CardViewJellybeanMr1();
        } else {
            IMPL = new CardViewEclairMr1();
        }
        IMPL.initStatic();
    }

    public CardView(Context context) {
        super(context);
        initialize(context, null, 0);
    }

    public CardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public CardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CardView, defStyleAttr,
                R.style.CardView_Light);
        int backgroundColor = a.getColor(R.styleable.CardView_cardBackgroundColor, 0);
        float radius = a.getDimension(R.styleable.CardView_cardCornerRadius, 0);

        a.recycle();
        IMPL.initialize(this, context, backgroundColor, radius);
    }

    /**
     * Updates the corner radius of the CardView.
     *
     * @param radius The radius in pixels of the corners of the rectangle shape
     *
     * @attr ref android.support.v7.cardview.R.styleable#CardView_cardCornerRadius
     *
     * @see #setRadius(float)
     *
     */
    public void setRadius(float radius) {
        IMPL.setRadius(this, radius);
    }

    /**
     * Returns the corner radius of the CardView.
     *
     * @return Corner radius of the CardView
     *
     * @see #getRadius()
     */
    public float getRadius() {
        return IMPL.getRadius(this);
    }
}
