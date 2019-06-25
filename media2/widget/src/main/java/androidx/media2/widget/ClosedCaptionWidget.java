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

package androidx.media2.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;
import android.view.accessibility.CaptioningManager.CaptioningChangeListener;

/**
 * Abstract widget class to render a closed caption track.
 */
abstract class ClosedCaptionWidget extends ViewGroup implements SubtitleTrack.RenderingWidget {

    interface ClosedCaptionLayout {
        void setCaptionStyle(CaptionStyle captionStyle);
        void setFontScale(float scale);
    }

    /** Captioning manager, used to obtain and track caption properties. */
    private final CaptioningManager mManager;

    /** Current caption style. */
    protected CaptionStyle mCaptionStyle;

    /** Callback for rendering changes. */
    protected OnChangedListener mListener;

    /** Concrete layout of CC. */
    protected ClosedCaptionLayout mClosedCaptionLayout;

    /** Whether a caption style change listener is registered. */
    private boolean mHasChangeListener;

    ClosedCaptionWidget(Context context) {
        this(context, null);
    }

    ClosedCaptionWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    ClosedCaptionWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Cannot render text over video when layer type is hardware.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mManager = (CaptioningManager) context.getSystemService(Context.CAPTIONING_SERVICE);
        mCaptionStyle = mManager.getUserStyle();

        mClosedCaptionLayout = createCaptionLayout(context);
        mClosedCaptionLayout.setCaptionStyle(mCaptionStyle);
        mClosedCaptionLayout.setFontScale(mManager.getFontScale());
        addView((ViewGroup) mClosedCaptionLayout, LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        requestLayout();
    }

    public abstract ClosedCaptionLayout createCaptionLayout(Context context);

    @Override
    public void setOnChangedListener(OnChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void setSize(int width, int height) {
        final int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        final int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

        measure(widthSpec, heightSpec);
        layout(0, 0, width, height);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }

        manageChangeListener();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        manageChangeListener();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        manageChangeListener();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ((ViewGroup) mClosedCaptionLayout).measure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        ((ViewGroup) mClosedCaptionLayout).layout(l, t, r, b);
    }

    /**
     * Manages whether this renderer is listening for caption style changes.
     */
    private final CaptioningChangeListener mCaptioningListener = new CaptioningChangeListener() {
        @Override
        public void onUserStyleChanged(CaptionStyle userStyle) {
            mCaptionStyle = userStyle;
            mClosedCaptionLayout.setCaptionStyle(mCaptionStyle);
        }

        @Override
        public void onFontScaleChanged(float fontScale) {
            mClosedCaptionLayout.setFontScale(fontScale);
        }
    };

    private void manageChangeListener() {
        final boolean needsListener = isAttachedToWindow() && getVisibility() == View.VISIBLE;
        if (mHasChangeListener != needsListener) {
            mHasChangeListener = needsListener;

            if (needsListener) {
                mManager.addCaptioningChangeListener(mCaptioningListener);
            } else {
                mManager.removeCaptioningChangeListener(mCaptioningListener);
            }
        }
    }
}

