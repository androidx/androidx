/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Looper;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.media.subtitle.SubtitleController.Anchor;
import androidx.media.subtitle.SubtitleTrack.RenderingWidget;

@RequiresApi(21)
class SubtitleView extends BaseLayout implements Anchor {
    private static final String TAG = "SubtitleView";

    private RenderingWidget mSubtitleWidget;
    private RenderingWidget.OnChangedListener mSubtitlesChangedListener;

    SubtitleView(Context context) {
        this(context, null);
    }

    SubtitleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    SubtitleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    SubtitleView(
            Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setSubtitleWidget(RenderingWidget subtitleWidget) {
        if (mSubtitleWidget == subtitleWidget) {
            return;
        }

        final boolean attachedToWindow = isAttachedToWindow();
        if (mSubtitleWidget != null) {
            if (attachedToWindow) {
                mSubtitleWidget.onDetachedFromWindow();
            }

            mSubtitleWidget.setOnChangedListener(null);
        }
        mSubtitleWidget = subtitleWidget;

        if (subtitleWidget != null) {
            if (mSubtitlesChangedListener == null) {
                mSubtitlesChangedListener = new RenderingWidget.OnChangedListener() {
                    @Override
                    public void onChanged(RenderingWidget renderingWidget) {
                        invalidate();
                    }
                };
            }

            setWillNotDraw(false);
            subtitleWidget.setOnChangedListener(mSubtitlesChangedListener);

            if (attachedToWindow) {
                subtitleWidget.onAttachedToWindow();
                requestLayout();
            }
        } else {
            setWillNotDraw(true);
        }

        invalidate();
    }

    @Override
    public Looper getSubtitleLooper() {
        return Looper.getMainLooper();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mSubtitleWidget != null) {
            mSubtitleWidget.onAttachedToWindow();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mSubtitleWidget != null) {
            mSubtitleWidget.onDetachedFromWindow();
        }
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (mSubtitleWidget != null) {
            final int width = getWidth() - getPaddingLeft() - getPaddingRight();
            final int height = getHeight() - getPaddingTop() - getPaddingBottom();

            mSubtitleWidget.setSize(width, height);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (mSubtitleWidget != null) {
            final int saveCount = canvas.save();
            canvas.translate(getPaddingLeft(), getPaddingTop());
            mSubtitleWidget.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return SubtitleView.class.getName();
    }
}
