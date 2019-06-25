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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.MediaFormat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.CaptioningManager.CaptionStyle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import java.util.ArrayList;

// Note: This is forked from android.media.Cea608CaptionRenderer since P
class Cea608CaptionRenderer extends SubtitleController.Renderer {
    private static final String TAG = "Cea608CaptionRenderer";
    private final Context mContext;
    private Cea608CCWidget mCCWidget;

    Cea608CaptionRenderer(@NonNull Context context) {
        mContext = context;
    }

    @Override
    public boolean supports(@NonNull MediaFormat format) {
        if (format.containsKey(MediaFormat.KEY_MIME)) {
            String mimeType = format.getString(MediaFormat.KEY_MIME);
            return MediaFormat.MIMETYPE_TEXT_CEA_608.equals(mimeType);
        }
        return false;
    }

    @Override
    public @NonNull SubtitleTrack createTrack(@NonNull MediaFormat format) {
        String mimeType = format.getString(MediaFormat.KEY_MIME);
        if (MediaFormat.MIMETYPE_TEXT_CEA_608.equals(mimeType)) {
            if (mCCWidget == null) {
                mCCWidget = new Cea608CCWidget(mContext);
            }
            return new Cea608CaptionTrack(mCCWidget, format);
        }
        throw new RuntimeException("No matching format: " + format.toString());
    }

    static class Cea608CaptionTrack extends SubtitleTrack {
        private final Cea608CCParser mCCParser;
        private final Cea608CCWidget mRenderingWidget;

        Cea608CaptionTrack(Cea608CCWidget renderingWidget, MediaFormat format) {
            super(format);

            mRenderingWidget = renderingWidget;
            mCCParser = new Cea608CCParser(mRenderingWidget);
        }

        @Override
        public void onData(byte[] data, boolean eos, long runID) {
            mCCParser.parse(data);
        }

        @Override
        public RenderingWidget getRenderingWidget() {
            return mRenderingWidget;
        }

        @Override
        public void updateView(ArrayList<Cue> activeCues) {
            // Overriding with NO-OP, CC rendering by-passes this
        }
    }

    /**
     * Widget capable of rendering CEA-608 closed captions.
     */
    class Cea608CCWidget extends ClosedCaptionWidget implements Cea608CCParser.DisplayListener {
        private static final String DUMMY_TEXT = "1234567890123456789012345678901234";
        final Rect mTextBounds = new Rect();

        Cea608CCWidget(Context context) {
            this(context, null);
        }

        Cea608CCWidget(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        Cea608CCWidget(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public ClosedCaptionLayout createCaptionLayout(Context context) {
            return new CCLayout(context);
        }

        @Override
        public void onDisplayChanged(SpannableStringBuilder[] styledTexts) {
            ((CCLayout) mClosedCaptionLayout).update(styledTexts);

            if (mListener != null) {
                mListener.onChanged(this);
            }
        }

        @Override
        public CaptionStyle getCaptionStyle() {
            return mCaptionStyle;
        }

        private class CCLineBox extends AppCompatTextView {
            private static final float FONT_PADDING_RATIO = 0.75f;
            private static final float EDGE_OUTLINE_RATIO = 0.1f;
            private static final float EDGE_SHADOW_RATIO = 0.05f;
            private float mOutlineWidth;
            private float mShadowRadius;
            private float mShadowOffset;

            private int mTextColor = Color.WHITE;
            private int mBgColor = Color.BLACK;
            private int mEdgeType = CaptionStyle.EDGE_TYPE_NONE;
            private int mEdgeColor = Color.TRANSPARENT;

            CCLineBox(Context context) {
                super(context);
                setGravity(Gravity.CENTER);
                setBackgroundColor(Color.TRANSPARENT);
                setTextColor(Color.WHITE);
                setTypeface(Typeface.MONOSPACE);
                setVisibility(View.INVISIBLE);

                final Resources res = getContext().getResources();

                // get the default (will be updated later during measure)
                mOutlineWidth = res.getDimensionPixelSize(
                        R.dimen.subtitle_outline_width);
                mShadowRadius = res.getDimensionPixelSize(
                        R.dimen.subtitle_shadow_radius);
                mShadowOffset = res.getDimensionPixelSize(
                        R.dimen.subtitle_shadow_offset);
            }

            void setCaptionStyle(CaptionStyle captionStyle) {
                mTextColor = captionStyle.foregroundColor;
                mBgColor = captionStyle.backgroundColor;
                mEdgeType = captionStyle.edgeType;
                mEdgeColor = captionStyle.edgeColor;

                setTextColor(mTextColor);
                if (mEdgeType == CaptionStyle.EDGE_TYPE_DROP_SHADOW) {
                    setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mEdgeColor);
                } else {
                    setShadowLayer(0, 0, 0, 0);
                }
                invalidate();
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                float fontSize = MeasureSpec.getSize(heightMeasureSpec) * FONT_PADDING_RATIO;
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);

                mOutlineWidth = EDGE_OUTLINE_RATIO * fontSize + 1.0f;
                mShadowRadius = EDGE_SHADOW_RATIO * fontSize + 1.0f;
                mShadowOffset = mShadowRadius;

                // set font scale in the X direction to match the required width
                setScaleX(1.0f);
                getPaint().getTextBounds(DUMMY_TEXT, 0, DUMMY_TEXT.length(), mTextBounds);
                float actualTextWidth = mTextBounds.width();
                float requiredTextWidth = MeasureSpec.getSize(widthMeasureSpec);
                if (actualTextWidth != .0f) {
                    setScaleX(requiredTextWidth / actualTextWidth);
                } else {
                    Log.w(TAG, "onMeasure(): Paint#getTextBounds() returned zero width. Ignored.");
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }

            @Override
            protected void onDraw(Canvas c) {
                if (mEdgeType == CaptionStyle.EDGE_TYPE_UNSPECIFIED
                        || mEdgeType == CaptionStyle.EDGE_TYPE_NONE
                        || mEdgeType == CaptionStyle.EDGE_TYPE_DROP_SHADOW) {
                    // these edge styles don't require a second pass
                    super.onDraw(c);
                    return;
                }

                if (mEdgeType == CaptionStyle.EDGE_TYPE_OUTLINE) {
                    drawEdgeOutline(c);
                } else {
                    // Raised or depressed
                    drawEdgeRaisedOrDepressed(c);
                }
            }

            @SuppressWarnings("WrongCall")
            private void drawEdgeOutline(Canvas c) {
                TextPaint textPaint = getPaint();

                Paint.Style previousStyle = textPaint.getStyle();
                Paint.Join previousJoin = textPaint.getStrokeJoin();
                float previousWidth = textPaint.getStrokeWidth();

                setTextColor(mEdgeColor);
                textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                textPaint.setStrokeJoin(Paint.Join.ROUND);
                textPaint.setStrokeWidth(mOutlineWidth);

                // Draw outline and background only.
                super.onDraw(c);

                // Restore original settings.
                setTextColor(mTextColor);
                textPaint.setStyle(previousStyle);
                textPaint.setStrokeJoin(previousJoin);
                textPaint.setStrokeWidth(previousWidth);

                // Remove the background.
                setBackgroundSpans(Color.TRANSPARENT);
                // Draw foreground only.
                super.onDraw(c);
                // Restore the background.
                setBackgroundSpans(mBgColor);
            }

            @SuppressWarnings("WrongCall")
            private void drawEdgeRaisedOrDepressed(Canvas c) {
                TextPaint textPaint = getPaint();

                Paint.Style previousStyle = textPaint.getStyle();
                textPaint.setStyle(Paint.Style.FILL);

                final boolean raised = mEdgeType == CaptionStyle.EDGE_TYPE_RAISED;
                final int colorUp = raised ? Color.WHITE : mEdgeColor;
                final int colorDown = raised ? mEdgeColor : Color.WHITE;
                final float offset = mShadowRadius / 2f;

                // Draw background and text with shadow up
                setShadowLayer(mShadowRadius, -offset, -offset, colorUp);
                super.onDraw(c);

                // Remove the background.
                setBackgroundSpans(Color.TRANSPARENT);

                // Draw text with shadow down
                setShadowLayer(mShadowRadius, +offset, +offset, colorDown);
                super.onDraw(c);

                // Restore settings
                textPaint.setStyle(previousStyle);

                // Restore the background.
                setBackgroundSpans(mBgColor);
            }

            private void setBackgroundSpans(int color) {
                CharSequence text = getText();
                if (text instanceof Spannable) {
                    Spannable spannable = (Spannable) text;
                    Cea608CCParser.MutableBackgroundColorSpan[] bgSpans = spannable.getSpans(
                            0, spannable.length(), Cea608CCParser.MutableBackgroundColorSpan.class);
                    for (int i = 0; i < bgSpans.length; i++) {
                        bgSpans[i].setBackgroundColor(color);
                    }
                }
            }
        }

        private class CCLayout extends LinearLayout implements ClosedCaptionLayout {
            private static final int MAX_ROWS = Cea608CCParser.MAX_ROWS;
            private static final float SAFE_AREA_RATIO = 0.9f;

            private final CCLineBox[] mLineBoxes = new CCLineBox[MAX_ROWS];

            CCLayout(Context context) {
                super(context);
                setGravity(Gravity.START);
                setOrientation(LinearLayout.VERTICAL);
                for (int i = 0; i < MAX_ROWS; i++) {
                    mLineBoxes[i] = new CCLineBox(getContext());
                    addView(mLineBoxes[i], LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                }
            }

            @Override
            public void setCaptionStyle(CaptionStyle captionStyle) {
                for (int i = 0; i < MAX_ROWS; i++) {
                    mLineBoxes[i].setCaptionStyle(captionStyle);
                }
            }

            @Override
            public void setFontScale(float fontScale) {
                // Ignores the font scale changes of the system wide CC preference.
            }

            void update(SpannableStringBuilder[] textBuffer) {
                for (int i = 0; i < MAX_ROWS; i++) {
                    if (textBuffer[i] != null) {
                        mLineBoxes[i].setText(textBuffer[i], TextView.BufferType.SPANNABLE);
                        mLineBoxes[i].setVisibility(View.VISIBLE);
                    } else {
                        mLineBoxes[i].setVisibility(View.INVISIBLE);
                    }
                }
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);

                int safeWidth = getMeasuredWidth();
                int safeHeight = getMeasuredHeight();

                // CEA-608 assumes 4:3 video
                if (safeWidth * 3 >= safeHeight * 4) {
                    safeWidth = safeHeight * 4 / 3;
                } else {
                    safeHeight = safeWidth * 3 / 4;
                }
                safeWidth = (int) (safeWidth * SAFE_AREA_RATIO);
                safeHeight = (int) (safeHeight * SAFE_AREA_RATIO);

                int lineHeight = safeHeight / MAX_ROWS;
                int lineHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        lineHeight, MeasureSpec.EXACTLY);
                int lineWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        safeWidth, MeasureSpec.EXACTLY);

                for (int i = 0; i < MAX_ROWS; i++) {
                    mLineBoxes[i].measure(lineWidthMeasureSpec, lineHeightMeasureSpec);
                }
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                // safe caption area
                int viewPortWidth = r - l;
                int viewPortHeight = b - t;
                int safeWidth, safeHeight;
                // CEA-608 assumes 4:3 video
                if (viewPortWidth * 3 >= viewPortHeight * 4) {
                    safeWidth = viewPortHeight * 4 / 3;
                    safeHeight = viewPortHeight;
                } else {
                    safeWidth = viewPortWidth;
                    safeHeight = viewPortWidth * 3 / 4;
                }
                safeWidth = (int) (safeWidth * SAFE_AREA_RATIO);
                safeHeight = (int) (safeHeight * SAFE_AREA_RATIO);
                int left = (viewPortWidth - safeWidth) / 2;
                int top = (viewPortHeight - safeHeight) / 2;

                for (int i = 0; i < MAX_ROWS; i++) {
                    mLineBoxes[i].layout(
                            left,
                            top + safeHeight * i / MAX_ROWS,
                            left + safeWidth,
                            top + safeHeight * (i + 1) / MAX_ROWS);
                }
            }
        }
    }
}
