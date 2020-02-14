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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// Note: This is forked from android.media.Cea708CaptionRenderer since P
class Cea708CaptionRenderer extends SubtitleController.Renderer {
    private final Context mContext;
    private Cea708CCWidget mCCWidget;

    Cea708CaptionRenderer(@NonNull Context context) {
        mContext = context;
    }

    @Override
    public boolean supports(@NonNull MediaFormat format) {
        if (format.containsKey(MediaFormat.KEY_MIME)) {
            String mimeType = format.getString(MediaFormat.KEY_MIME);
            return MediaFormat.MIMETYPE_TEXT_CEA_708.equals(mimeType);
        }
        return false;
    }

    @Override
    public @NonNull SubtitleTrack createTrack(@NonNull MediaFormat format) {
        String mimeType = format.getString(MediaFormat.KEY_MIME);
        if (MediaFormat.MIMETYPE_TEXT_CEA_708.equals(mimeType)) {
            if (mCCWidget == null) {
                mCCWidget = new Cea708CCWidget(mContext);
            }
            return new Cea708CaptionTrack(mCCWidget, format);
        }
        throw new RuntimeException("No matching format: " + format.toString());
    }

    static class Cea708CaptionTrack extends SubtitleTrack {
        private final Cea708CCParser mCCParser;
        private final Cea708CCWidget mRenderingWidget;

        Cea708CaptionTrack(Cea708CCWidget renderingWidget, MediaFormat format) {
            super(format);

            mRenderingWidget = renderingWidget;
            mCCParser = new Cea708CCParser(mRenderingWidget);
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
     * Widget capable of rendering CEA-708 closed captions.
     */
    class Cea708CCWidget extends ClosedCaptionWidget implements Cea708CCParser.DisplayListener {
        private final CCHandler mCCHandler;

        Cea708CCWidget(Context context) {
            this(context, null);
        }

        Cea708CCWidget(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        Cea708CCWidget(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);

            mCCHandler = new CCHandler((CCLayout) mClosedCaptionLayout);
        }

        @Override
        public ClosedCaptionLayout createCaptionLayout(Context context) {
            return new CCLayout(context);
        }

        @Override
        public void emitEvent(Cea708CCParser.CaptionEvent event) {
            mCCHandler.processCaptionEvent(event);

            setSize(getWidth(), getHeight());

            if (mListener != null) {
                mListener.onChanged(this);
            }
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            ((ViewGroup) mClosedCaptionLayout).draw(canvas);
        }

        /**
         * A layout that scales its children using the given percentage value.
         */
        class ScaledLayout extends ViewGroup {
            private static final String TAG = "ScaledLayout";
            private static final boolean DEBUG = false;
            private final Comparator<Rect> mRectTopLeftSorter = new Comparator<Rect>() {
                @Override
                public int compare(Rect lhs, Rect rhs) {
                    if (lhs.top != rhs.top) {
                        return lhs.top - rhs.top;
                    } else {
                        return lhs.left - rhs.left;
                    }
                }
            };

            private Rect[] mRectArray;

            ScaledLayout(Context context) {
                super(context);
            }

            /**
             * ScaledLayoutParams stores the four scale factors.
             * <br>
             * Vertical coordinate system:   (scaleStartRow * 100) % ~ (scaleEndRow * 100) %
             * Horizontal coordinate system: (scaleStartCol * 100) % ~ (scaleEndCol * 100) %
             * <br>
             * In XML, for example,
             * <pre>
             * {@code
             * <View
             *     app:layout_scaleStartRow="0.1"
             *     app:layout_scaleEndRow="0.5"
             *     app:layout_scaleStartCol="0.4"
             *     app:layout_scaleEndCol="1" />
             * }
             * </pre>
             */
            class ScaledLayoutParams extends ViewGroup.LayoutParams {
                public static final float SCALE_UNSPECIFIED = -1;
                public float scaleStartRow;
                public float scaleEndRow;
                public float scaleStartCol;
                public float scaleEndCol;

                ScaledLayoutParams(float scaleStartRow, float scaleEndRow,
                        float scaleStartCol, float scaleEndCol) {
                    super(MATCH_PARENT, MATCH_PARENT);
                    this.scaleStartRow = scaleStartRow;
                    this.scaleEndRow = scaleEndRow;
                    this.scaleStartCol = scaleStartCol;
                    this.scaleEndCol = scaleEndCol;
                }

                ScaledLayoutParams(Context context, AttributeSet attrs) {
                    super(MATCH_PARENT, MATCH_PARENT);
                }
            }

            @Override
            public LayoutParams generateLayoutParams(AttributeSet attrs) {
                return new ScaledLayoutParams(getContext(), attrs);
            }

            @Override
            protected boolean checkLayoutParams(LayoutParams p) {
                return (p instanceof ScaledLayoutParams);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
                int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
                int width = widthSpecSize - getPaddingLeft() - getPaddingRight();
                int height = heightSpecSize - getPaddingTop() - getPaddingBottom();
                if (DEBUG) {
                    Log.d(TAG, String.format("onMeasure width: %d, height: %d", width, height));
                }
                int count = getChildCount();
                mRectArray = new Rect[count];
                for (int i = 0; i < count; ++i) {
                    View child = getChildAt(i);
                    ViewGroup.LayoutParams params = child.getLayoutParams();
                    float scaleStartRow, scaleEndRow, scaleStartCol, scaleEndCol;
                    if (!(params instanceof ScaledLayoutParams)) {
                        throw new RuntimeException("A child of ScaledLayout cannot have the "
                                + "UNSPECIFIED scale factors");
                    }
                    scaleStartRow = ((ScaledLayoutParams) params).scaleStartRow;
                    scaleEndRow = ((ScaledLayoutParams) params).scaleEndRow;
                    scaleStartCol = ((ScaledLayoutParams) params).scaleStartCol;
                    scaleEndCol = ((ScaledLayoutParams) params).scaleEndCol;
                    if (scaleStartRow < 0 || scaleStartRow > 1) {
                        throw new RuntimeException("A child of ScaledLayout should have a range of "
                                + "scaleStartRow between 0 and 1");
                    }
                    if (scaleEndRow < scaleStartRow || scaleStartRow > 1) {
                        throw new RuntimeException("A child of ScaledLayout should have a range of "
                                + "scaleEndRow between scaleStartRow and 1");
                    }
                    if (scaleEndCol < 0 || scaleEndCol > 1) {
                        throw new RuntimeException("A child of ScaledLayout should have a range of "
                                + "scaleStartCol between 0 and 1");
                    }
                    if (scaleEndCol < scaleStartCol || scaleEndCol > 1) {
                        throw new RuntimeException("A child of ScaledLayout should have a range of "
                                + "scaleEndCol between scaleStartCol and 1");
                    }
                    if (DEBUG) {
                        Log.d(TAG, String.format("onMeasure child scaleStartRow: %f scaleEndRow: "
                                        + "%f scaleStartCol: %f scaleEndCol: %f",
                                scaleStartRow, scaleEndRow, scaleStartCol, scaleEndCol));
                    }
                    mRectArray[i] = new Rect((int) (scaleStartCol * width), (int) (scaleStartRow
                            * height), (int) (scaleEndCol * width), (int) (scaleEndRow * height));
                    int childWidthSpec = MeasureSpec.makeMeasureSpec(
                            (int) (width * (scaleEndCol - scaleStartCol)), MeasureSpec.EXACTLY);
                    int childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                    child.measure(childWidthSpec, childHeightSpec);

                    // If the height of the measured child view is bigger than the height of the
                    // calculated region by the given ScaleLayoutParams, the height of the region
                    // should be increased to fit the size of the child view.
                    if (child.getMeasuredHeight() > mRectArray[i].height()) {
                        int overflowedHeight = child.getMeasuredHeight() - mRectArray[i].height();
                        overflowedHeight = (overflowedHeight + 1) / 2;
                        mRectArray[i].bottom += overflowedHeight;
                        mRectArray[i].top -= overflowedHeight;
                        if (mRectArray[i].top < 0) {
                            mRectArray[i].bottom -= mRectArray[i].top;
                            mRectArray[i].top = 0;
                        }
                        if (mRectArray[i].bottom > height) {
                            mRectArray[i].top -= mRectArray[i].bottom - height;
                            mRectArray[i].bottom = height;
                        }
                    }
                    childHeightSpec = MeasureSpec.makeMeasureSpec(
                            (int) (height * (scaleEndRow - scaleStartRow)), MeasureSpec.EXACTLY);
                    child.measure(childWidthSpec, childHeightSpec);
                }

                // Avoid overlapping rectangles.
                // Step 1. Sort rectangles by position (top-left).
                int visibleRectCount = 0;
                int[] visibleRectGroup = new int[count];
                Rect[] visibleRectArray = new Rect[count];
                for (int i = 0; i < count; ++i) {
                    if (getChildAt(i).getVisibility() == View.VISIBLE) {
                        visibleRectGroup[visibleRectCount] = visibleRectCount;
                        visibleRectArray[visibleRectCount] = mRectArray[i];
                        ++visibleRectCount;
                    }
                }
                Arrays.sort(visibleRectArray, 0, visibleRectCount, mRectTopLeftSorter);

                // Step 2. Move down if there are overlapping rectangles.
                for (int i = 0; i < visibleRectCount - 1; ++i) {
                    for (int j = i + 1; j < visibleRectCount; ++j) {
                        if (Rect.intersects(visibleRectArray[i], visibleRectArray[j])) {
                            visibleRectGroup[j] = visibleRectGroup[i];
                            visibleRectArray[j].set(visibleRectArray[j].left,
                                    visibleRectArray[i].bottom,
                                    visibleRectArray[j].right,
                                    visibleRectArray[i].bottom + visibleRectArray[j].height());
                        }
                    }
                }

                // Step 3. Move up if there is any overflowed rectangle.
                for (int i = visibleRectCount - 1; i >= 0; --i) {
                    if (visibleRectArray[i].bottom > height) {
                        int overflowedHeight = visibleRectArray[i].bottom - height;
                        for (int j = 0; j <= i; ++j) {
                            if (visibleRectGroup[i] == visibleRectGroup[j]) {
                                visibleRectArray[j].set(visibleRectArray[j].left,
                                        visibleRectArray[j].top - overflowedHeight,
                                        visibleRectArray[j].right,
                                        visibleRectArray[j].bottom - overflowedHeight);
                            }
                        }
                    }
                }
                setMeasuredDimension(widthSpecSize, heightSpecSize);
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                int paddingLeft = getPaddingLeft();
                int paddingTop = getPaddingTop();
                int count = getChildCount();
                for (int i = 0; i < count; ++i) {
                    View child = getChildAt(i);
                    if (child.getVisibility() != GONE) {
                        int childLeft = paddingLeft + mRectArray[i].left;
                        int childTop = paddingTop + mRectArray[i].top;
                        int childBottom = paddingLeft + mRectArray[i].bottom;
                        int childRight = paddingTop + mRectArray[i].right;
                        if (DEBUG) {
                            Log.d(TAG, String.format(
                                    "child layout bottom: %d left: %d right: %d top: %d",
                                    childBottom, childLeft, childRight, childTop));
                        }
                        child.layout(childLeft, childTop, childRight, childBottom);
                    }
                }
            }

            @Override
            public void dispatchDraw(Canvas canvas) {
                int paddingLeft = getPaddingLeft();
                int paddingTop = getPaddingTop();
                int count = getChildCount();
                for (int i = 0; i < count; ++i) {
                    View child = getChildAt(i);
                    if (child.getVisibility() != GONE) {
                        if (i >= mRectArray.length) {
                            break;
                        }
                        int childLeft = paddingLeft + mRectArray[i].left;
                        int childTop = paddingTop + mRectArray[i].top;
                        final int saveCount = canvas.save();
                        canvas.translate(childLeft, childTop);
                        child.draw(canvas);
                        canvas.restoreToCount(saveCount);
                    }
                }
            }
        }

        /**
         * Layout containing the safe title area that helps the closed captions look more prominent.
         *
         * <p>This is required by CEA-708B.
         */
        class CCLayout extends ScaledLayout implements ClosedCaptionLayout {
            private static final float SAFE_TITLE_AREA_SCALE_START_X = 0.1f;
            private static final float SAFE_TITLE_AREA_SCALE_END_X = 0.9f;
            private static final float SAFE_TITLE_AREA_SCALE_START_Y = 0.1f;
            private static final float SAFE_TITLE_AREA_SCALE_END_Y = 0.9f;

            private final ScaledLayout mSafeTitleAreaLayout;

            CCLayout(Context context) {
                super(context);

                mSafeTitleAreaLayout = new ScaledLayout(context);
                addView(mSafeTitleAreaLayout, new ScaledLayout.ScaledLayoutParams(
                        SAFE_TITLE_AREA_SCALE_START_X, SAFE_TITLE_AREA_SCALE_END_X,
                        SAFE_TITLE_AREA_SCALE_START_Y, SAFE_TITLE_AREA_SCALE_END_Y));
            }

            public void addOrUpdateViewToSafeTitleArea(CCWindowLayout captionWindowLayout,
                    ScaledLayoutParams scaledLayoutParams) {
                int index = mSafeTitleAreaLayout.indexOfChild(captionWindowLayout);
                if (index < 0) {
                    mSafeTitleAreaLayout.addView(captionWindowLayout, scaledLayoutParams);
                    return;
                }
                mSafeTitleAreaLayout.updateViewLayout(captionWindowLayout, scaledLayoutParams);
            }

            public void removeViewFromSafeTitleArea(CCWindowLayout captionWindowLayout) {
                mSafeTitleAreaLayout.removeView(captionWindowLayout);
            }

            @Override
            public void setCaptionStyle(CaptionStyle style) {
                final int count = mSafeTitleAreaLayout.getChildCount();
                for (int i = 0; i < count; ++i) {
                    final CCWindowLayout windowLayout =
                            (CCWindowLayout) mSafeTitleAreaLayout.getChildAt(i);
                    windowLayout.setCaptionStyle(style);
                }
            }

            @Override
            public void setFontScale(float fontScale) {
                final int count = mSafeTitleAreaLayout.getChildCount();
                for (int i = 0; i < count; ++i) {
                    final CCWindowLayout windowLayout =
                            (CCWindowLayout) mSafeTitleAreaLayout.getChildAt(i);
                    windowLayout.setFontScale(fontScale);
                }
            }
        }

        /**
         * Renders the selected CC track.
         */
        class CCHandler implements Handler.Callback {
            // TODO: Remaining works
            // CaptionTrackRenderer does not support the full spec of CEA-708.
            // The remaining works are described in the follows.
            // C0 Table: Backspace, FF, and HCR are not supported. The rule for P16 is not
            //           standardized but it is handled as EUC-KR charset for Korea broadcasting.
            // C1 Table: All the styles of windows and pens except underline, italic, pen size,
            //           and pen offset specified in CEA-708 are ignored and this follows system
            //           wide CC preferences for look and feel. SetPenLocation is not implemented.
            // G2 Table: TSP, NBTSP and BLK are not supported.
            // Text/commands: Word wrapping, fonts, row and column locking are not supported.

            private static final String TAG = "CCHandler";
            private static final boolean DEBUG = false;

            private static final int TENTHS_OF_SECOND_IN_MILLIS = 100;

            // According to CEA-708B, there can exist up to 8 caption windows.
            private static final int CAPTION_WINDOWS_MAX = 8;
            private static final int CAPTION_ALL_WINDOWS_BITMAP = 255;

            private static final int MSG_DELAY_CANCEL = 1;
            private static final int MSG_CAPTION_CLEAR = 2;

            private static final long CAPTION_CLEAR_INTERVAL_MS = 60000;

            private final CCLayout mCCLayout;
            private boolean mIsDelayed = false;
            private CCWindowLayout mCurrentWindowLayout;
            private final CCWindowLayout[] mCaptionWindowLayouts =
                    new CCWindowLayout[CAPTION_WINDOWS_MAX];
            private final ArrayList<Cea708CCParser.CaptionEvent> mPendingCaptionEvents =
                    new ArrayList<>();
            private final Handler mHandler;

            CCHandler(CCLayout ccLayout) {
                mCCLayout = ccLayout;
                mHandler = new Handler(this);
            }

            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_DELAY_CANCEL:
                        delayCancel();
                        return true;
                    case MSG_CAPTION_CLEAR:
                        clearWindows(CAPTION_ALL_WINDOWS_BITMAP);
                        return true;
                }
                return false;
            }

            public void processCaptionEvent(Cea708CCParser.CaptionEvent event) {
                if (mIsDelayed) {
                    mPendingCaptionEvents.add(event);
                    return;
                }
                switch (event.type) {
                    case Cea708CCParser.CAPTION_EMIT_TYPE_BUFFER:
                        sendBufferToCurrentWindow((String) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_CONTROL:
                        sendControlToCurrentWindow((char) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_CWX:
                        setCurrentWindowLayout((int) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_CLW:
                        clearWindows((int) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_DSW:
                        displayWindows((int) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_HDW:
                        hideWindows((int) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_TGW:
                        toggleWindows((int) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_DLW:
                        deleteWindows((int) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_DLY:
                        delay((int) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_DLC:
                        delayCancel();
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_RST:
                        reset();
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_SPA:
                        setPenAttr((Cea708CCParser.CaptionPenAttr) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_SPC:
                        setPenColor((Cea708CCParser.CaptionPenColor) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_SPL:
                        setPenLocation((Cea708CCParser.CaptionPenLocation) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_SWA:
                        setWindowAttr((Cea708CCParser.CaptionWindowAttr) event.obj);
                        break;
                    case Cea708CCParser.CAPTION_EMIT_TYPE_COMMAND_DFX:
                        defineWindow((Cea708CCParser.CaptionWindow) event.obj);
                        break;
                }
            }

            // The window related caption commands
            private void setCurrentWindowLayout(int windowId) {
                if (windowId < 0 || windowId >= mCaptionWindowLayouts.length) {
                    return;
                }
                CCWindowLayout windowLayout = mCaptionWindowLayouts[windowId];
                if (windowLayout == null) {
                    return;
                }
                if (DEBUG) {
                    Log.d(TAG, "setCurrentWindowLayout to " + windowId);
                }
                mCurrentWindowLayout = windowLayout;
            }

            // Each bit of windowBitmap indicates a window.
            // If a bit is set, the window id is the same as the number of the trailing zeros of the
            // bit.
            private ArrayList<CCWindowLayout> getWindowsFromBitmap(int windowBitmap) {
                ArrayList<CCWindowLayout> windows = new ArrayList<>();
                for (int i = 0; i < CAPTION_WINDOWS_MAX; ++i) {
                    if ((windowBitmap & (1 << i)) != 0) {
                        CCWindowLayout windowLayout = mCaptionWindowLayouts[i];
                        if (windowLayout != null) {
                            windows.add(windowLayout);
                        }
                    }
                }
                return windows;
            }

            private void clearWindows(int windowBitmap) {
                if (windowBitmap == 0) {
                    return;
                }
                for (CCWindowLayout windowLayout : getWindowsFromBitmap(windowBitmap)) {
                    windowLayout.clear();
                }
            }

            private void displayWindows(int windowBitmap) {
                if (windowBitmap == 0) {
                    return;
                }
                for (CCWindowLayout windowLayout : getWindowsFromBitmap(windowBitmap)) {
                    windowLayout.show();
                }
            }

            private void hideWindows(int windowBitmap) {
                if (windowBitmap == 0) {
                    return;
                }
                for (CCWindowLayout windowLayout : getWindowsFromBitmap(windowBitmap)) {
                    windowLayout.hide();
                }
            }

            private void toggleWindows(int windowBitmap) {
                if (windowBitmap == 0) {
                    return;
                }
                for (CCWindowLayout windowLayout : getWindowsFromBitmap(windowBitmap)) {
                    if (windowLayout.isShown()) {
                        windowLayout.hide();
                    } else {
                        windowLayout.show();
                    }
                }
            }

            private void deleteWindows(int windowBitmap) {
                if (windowBitmap == 0) {
                    return;
                }
                for (CCWindowLayout windowLayout : getWindowsFromBitmap(windowBitmap)) {
                    windowLayout.removeFromCaptionView();
                    mCaptionWindowLayouts[windowLayout.getCaptionWindowId()] = null;
                }
            }

            public void reset() {
                mCurrentWindowLayout = null;
                mIsDelayed = false;
                mPendingCaptionEvents.clear();
                for (int i = 0; i < CAPTION_WINDOWS_MAX; ++i) {
                    if (mCaptionWindowLayouts[i] != null) {
                        mCaptionWindowLayouts[i].removeFromCaptionView();
                    }
                    mCaptionWindowLayouts[i] = null;
                }
                mCCLayout.setVisibility(View.INVISIBLE);
                mHandler.removeMessages(MSG_CAPTION_CLEAR);
            }

            private void setWindowAttr(Cea708CCParser.CaptionWindowAttr windowAttr) {
                if (mCurrentWindowLayout != null) {
                    mCurrentWindowLayout.setWindowAttr(windowAttr);
                }
            }

            private void defineWindow(Cea708CCParser.CaptionWindow window) {
                if (window == null) {
                    return;
                }
                int windowId = window.id;
                if (windowId < 0 || windowId >= mCaptionWindowLayouts.length) {
                    return;
                }
                CCWindowLayout windowLayout = mCaptionWindowLayouts[windowId];
                if (windowLayout == null) {
                    windowLayout = new CCWindowLayout(mCCLayout.getContext());
                }
                windowLayout.initWindow(mCCLayout, window);
                mCurrentWindowLayout = mCaptionWindowLayouts[windowId] = windowLayout;
            }

            // The job related caption commands
            private void delay(int tenthsOfSeconds) {
                if (tenthsOfSeconds < 0 || tenthsOfSeconds > 255) {
                    return;
                }
                mIsDelayed = true;
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DELAY_CANCEL),
                        tenthsOfSeconds * TENTHS_OF_SECOND_IN_MILLIS);
            }

            private void delayCancel() {
                mIsDelayed = false;
                processPendingBuffer();
            }

            private void processPendingBuffer() {
                for (Cea708CCParser.CaptionEvent event : mPendingCaptionEvents) {
                    processCaptionEvent(event);
                }
                mPendingCaptionEvents.clear();
            }

            // The implicit write caption commands
            private void sendControlToCurrentWindow(char control) {
                if (mCurrentWindowLayout != null) {
                    mCurrentWindowLayout.sendControl(control);
                }
            }

            private void sendBufferToCurrentWindow(String buffer) {
                if (mCurrentWindowLayout != null) {
                    mCurrentWindowLayout.sendBuffer(buffer);
                    mHandler.removeMessages(MSG_CAPTION_CLEAR);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CAPTION_CLEAR),
                            CAPTION_CLEAR_INTERVAL_MS);
                }
            }

            // The pen related caption commands
            private void setPenAttr(Cea708CCParser.CaptionPenAttr attr) {
                if (mCurrentWindowLayout != null) {
                    mCurrentWindowLayout.setPenAttr(attr);
                }
            }

            private void setPenColor(Cea708CCParser.CaptionPenColor color) {
                if (mCurrentWindowLayout != null) {
                    mCurrentWindowLayout.setPenColor(color);
                }
            }

            private void setPenLocation(Cea708CCParser.CaptionPenLocation location) {
                if (mCurrentWindowLayout != null) {
                    mCurrentWindowLayout.setPenLocation(location.row, location.column);
                }
            }
        }

        /**
         * Layout which renders a caption window of CEA-708B. It contains a {@link TextView} that
         * takes care of displaying the actual CC text.
         */
        private class CCWindowLayout extends RelativeLayout implements View.OnLayoutChangeListener {
            private static final String TAG = "CCWindowLayout";

            private static final float PROPORTION_PEN_SIZE_SMALL = .75f;
            private static final float PROPORTION_PEN_SIZE_LARGE = 1.25f;

            // The following values indicates the maximum cell number of a window.
            private static final int ANCHOR_RELATIVE_POSITIONING_MAX = 99;
            private static final int ANCHOR_VERTICAL_MAX = 74;
            private static final int ANCHOR_HORIZONTAL_16_9_MAX = 209;
            private static final int MAX_COLUMN_COUNT_16_9 = 42;

            // The following values indicates a gravity of a window.
            private static final int ANCHOR_MODE_DIVIDER = 3;
            private static final int ANCHOR_HORIZONTAL_MODE_LEFT = 0;
            private static final int ANCHOR_HORIZONTAL_MODE_CENTER = 1;
            private static final int ANCHOR_HORIZONTAL_MODE_RIGHT = 2;
            private static final int ANCHOR_VERTICAL_MODE_TOP = 0;
            private static final int ANCHOR_VERTICAL_MODE_CENTER = 1;
            private static final int ANCHOR_VERTICAL_MODE_BOTTOM = 2;

            private CCLayout mCCLayout;

            private CCView mCCView;
            private CaptionStyle mCaptionStyle;
            private int mRowLimit = 0;
            private final SpannableStringBuilder mBuilder = new SpannableStringBuilder();
            private final List<CharacterStyle> mCharacterStyles = new ArrayList<>();
            private int mCaptionWindowId;
            private int mRow = -1;
            private float mFontScale;
            private float mTextSize;
            private String mWidestChar;
            private int mLastCaptionLayoutWidth;
            private int mLastCaptionLayoutHeight;

            CCWindowLayout(Context context) {
                this(context, null);
            }

            CCWindowLayout(Context context, AttributeSet attrs) {
                this(context, attrs, 0);
            }

            CCWindowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
                super(context, attrs, defStyleAttr);

                // Add a subtitle view to the layout.
                mCCView = new CCView(context);
                LayoutParams params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                addView(mCCView, params);

                // Set the system wide CC preferences to the subtitle view.
                CaptioningManager captioningManager =
                        (CaptioningManager) context.getSystemService(Context.CAPTIONING_SERVICE);
                mFontScale = captioningManager.getFontScale();
                setCaptionStyle(captioningManager.getUserStyle());
                mCCView.setText("");
                updateWidestChar();
            }

            public void setCaptionStyle(CaptionStyle style) {
                mCaptionStyle = style;
                mCCView.setCaptionStyle(style);
            }

            public void setFontScale(float fontScale) {
                mFontScale = fontScale;
                updateTextSize();
            }

            public int getCaptionWindowId() {
                return mCaptionWindowId;
            }

            public void setCaptionWindowId(int captionWindowId) {
                mCaptionWindowId = captionWindowId;
            }

            public void clear() {
                clearText();
                hide();
            }

            public void show() {
                setVisibility(View.VISIBLE);
                requestLayout();
            }

            public void hide() {
                setVisibility(View.INVISIBLE);
                requestLayout();
            }

            public void setPenAttr(Cea708CCParser.CaptionPenAttr penAttr) {
                mCharacterStyles.clear();
                if (penAttr.italic) {
                    mCharacterStyles.add(new StyleSpan(Typeface.ITALIC));
                }
                if (penAttr.underline) {
                    mCharacterStyles.add(new UnderlineSpan());
                }
                switch (penAttr.penSize) {
                    case Cea708CCParser.CaptionPenAttr.PEN_SIZE_SMALL:
                        mCharacterStyles.add(new RelativeSizeSpan(PROPORTION_PEN_SIZE_SMALL));
                        break;
                    case Cea708CCParser.CaptionPenAttr.PEN_SIZE_LARGE:
                        mCharacterStyles.add(new RelativeSizeSpan(PROPORTION_PEN_SIZE_LARGE));
                        break;
                }
                switch (penAttr.penOffset) {
                    case Cea708CCParser.CaptionPenAttr.OFFSET_SUBSCRIPT:
                        mCharacterStyles.add(new SubscriptSpan());
                        break;
                    case Cea708CCParser.CaptionPenAttr.OFFSET_SUPERSCRIPT:
                        mCharacterStyles.add(new SuperscriptSpan());
                        break;
                }
            }

            public void setPenColor(Cea708CCParser.CaptionPenColor penColor) {
                // TODO: apply pen colors or skip this and use the style of system wide CC style
                // as is.
            }

            public void setPenLocation(int row, int column) {
                // TODO: change the location of pen based on row and column both.
                if (mRow >= 0) {
                    for (int r = mRow; r < row; ++r) {
                        appendText("\n");
                    }
                }
                mRow = row;
            }

            public void setWindowAttr(Cea708CCParser.CaptionWindowAttr windowAttr) {
                // TODO: apply window attrs or skip this and use the style of system wide CC style
                // as is.
            }

            public void sendBuffer(String buffer) {
                appendText(buffer);
            }

            public void sendControl(char control) {
                // TODO: there are a bunch of ASCII-style control codes.
            }

            /**
             * This method places the window on a given CaptionLayout along with the anchor of the
             * window.
             * <p>
             * According to CEA-708B, the anchor id indicates the gravity of the window as the
             * follows.
             * For example, A value 7 of a anchor id says that a window is align with its parent
             * bottom and is located at the center horizontally of its parent.
             * </p>
             * <h4>Anchor id and the gravity of a window</h4>
             * <table>
             *     <tr>
             *         <th>GRAVITY</th>
             *         <th>LEFT</th>
             *         <th>CENTER_HORIZONTAL</th>
             *         <th>RIGHT</th>
             *     </tr>
             *     <tr>
             *         <th>TOP</th>
             *         <td>0</td>
             *         <td>1</td>
             *         <td>2</td>
             *     </tr>
             *     <tr>
             *         <th>CENTER_VERTICAL</th>
             *         <td>3</td>
             *         <td>4</td>
             *         <td>5</td>
             *     </tr>
             *     <tr>
             *         <th>BOTTOM</th>
             *         <td>6</td>
             *         <td>7</td>
             *         <td>8</td>
             *     </tr>
             * </table>
             * <p>
             * In order to handle the gravity of a window, there are two steps.
             * First, set the size of the window. Since the window will be positioned at
             * ScaledLayout, the size factors are determined in a ratio.
             * Second, set the gravity of the window. CaptionWindowLayout is inherited from
             * RelativeLayout. Hence, we could set the gravity of its child view, SubtitleView.
             * </p>
             * <p>
             * The gravity of the window is also related to its size. When it should be pushed to
             * one of the end of the window, like LEFT, RIGHT, TOP or BOTTOM, the anchor point
             * should be a boundary of the window. When it should be pushed
             * in the horizontal/vertical center of its container, the horizontal/vertical center
             * point of the window should be the same as the anchor point.
             * </p>
             *
             * @param ccLayout a given CaptionLayout, which contains a safe title area.
             * @param captionWindow a given CaptionWindow, which stores the construction info of the
             *                      window.
             */
            public void initWindow(CCLayout ccLayout, Cea708CCParser.CaptionWindow captionWindow) {
                if (mCCLayout != ccLayout) {
                    if (mCCLayout != null) {
                        mCCLayout.removeOnLayoutChangeListener(this);
                    }
                    mCCLayout = ccLayout;
                    mCCLayout.addOnLayoutChangeListener(this);
                    updateWidestChar();
                }

                // Both anchor vertical and horizontal indicates the position cell number of
                // the window.
                float scaleRow = (float) captionWindow.anchorVertical
                        / (captionWindow.relativePositioning
                                ? ANCHOR_RELATIVE_POSITIONING_MAX : ANCHOR_VERTICAL_MAX);

                // Assumes it has a wide aspect ratio track.
                float scaleCol = (float) captionWindow.anchorHorizontal
                        / (captionWindow.relativePositioning ? ANCHOR_RELATIVE_POSITIONING_MAX
                                : ANCHOR_HORIZONTAL_16_9_MAX);

                // The range of scaleRow/Col need to be verified to be in [0, 1].
                // Otherwise a RuntimeException will be raised in ScaledLayout.
                if (scaleRow < 0 || scaleRow > 1) {
                    Log.i(TAG, "The vertical position of the anchor point should be at the "
                            + "range of 0 and 1 but " + scaleRow);
                    scaleRow = Math.max(0, Math.min(scaleRow, 1));
                }
                if (scaleCol < 0 || scaleCol > 1) {
                    Log.i(TAG, "The horizontal position of the anchor point should be at the "
                            + "range of 0 and 1 but " + scaleCol);
                    scaleCol = Math.max(0, Math.min(scaleCol, 1));
                }
                int gravity = Gravity.CENTER;
                int horizontalMode = captionWindow.anchorId % ANCHOR_MODE_DIVIDER;
                int verticalMode = captionWindow.anchorId / ANCHOR_MODE_DIVIDER;
                float scaleStartRow = 0;
                float scaleEndRow = 1;
                float scaleStartCol = 0;
                float scaleEndCol = 1;
                switch (horizontalMode) {
                    case ANCHOR_HORIZONTAL_MODE_LEFT:
                        gravity = Gravity.LEFT;
                        mCCView.setAlignment(Alignment.ALIGN_NORMAL);
                        scaleStartCol = scaleCol;
                        break;
                    case ANCHOR_HORIZONTAL_MODE_CENTER:
                        float gap = Math.min(1 - scaleCol, scaleCol);

                        // Since all TV sets use left text alignment instead of center text
                        // alignment for this case, we follow the industry convention if possible.
                        int columnCount = captionWindow.columnCount + 1;
                        columnCount = Math.min(getScreenColumnCount(), columnCount);
                        StringBuilder widestTextBuilder = new StringBuilder();
                        for (int i = 0; i < columnCount; ++i) {
                            widestTextBuilder.append(mWidestChar);
                        }
                        Paint paint = new Paint();
                        paint.setTypeface(mCaptionStyle.getTypeface());
                        paint.setTextSize(mTextSize);
                        float maxWindowWidth = paint.measureText(widestTextBuilder.toString());
                        float halfMaxWidthScale = mCCLayout.getWidth() > 0
                                ? maxWindowWidth / 2.0f / (mCCLayout.getWidth() * 0.8f) : 0.0f;
                        if (halfMaxWidthScale > 0f && halfMaxWidthScale < scaleCol) {
                            // Calculate the expected max window size based on the column count of
                            // the caption window multiplied by average alphabets char width,
                            // then align the left side of the window with the left side of
                            // the expected max window.
                            gravity = Gravity.LEFT;
                            mCCView.setAlignment(Alignment.ALIGN_NORMAL);
                            scaleStartCol = scaleCol - halfMaxWidthScale;
                            scaleEndCol = 1.0f;
                        } else {
                            // The gap will be the minimum distance value of the distances from both
                            // horizontal end points to the anchor point.
                            // If scaleCol <= 0.5, the range of scaleCol is
                            // [0, the anchor point * 2].
                            // If scaleCol > 0.5, the range of scaleCol is
                            // [(1 - the anchor point) * 2, 1].
                            // The anchor point is located at the horizontal center of the window in
                            // both cases.
                            gravity = Gravity.CENTER_HORIZONTAL;
                            mCCView.setAlignment(Alignment.ALIGN_CENTER);
                            scaleStartCol = scaleCol - gap;
                            scaleEndCol = scaleCol + gap;
                        }
                        break;
                    case ANCHOR_HORIZONTAL_MODE_RIGHT:
                        gravity = Gravity.RIGHT;
                        // TODO: Alignment.ALIGN_RIGHT is hidden. Implement setAlignment()
                        // in a different way.
                        // mCCView.setAlignment(Alignment.ALIGN_RIGHT);
                        scaleEndCol = scaleCol;
                        break;
                }
                switch (verticalMode) {
                    case ANCHOR_VERTICAL_MODE_TOP:
                        gravity |= Gravity.TOP;
                        scaleStartRow = scaleRow;
                        break;
                    case ANCHOR_VERTICAL_MODE_CENTER:
                        gravity |= Gravity.CENTER_VERTICAL;

                        // See the above comment.
                        float gap = Math.min(1 - scaleRow, scaleRow);
                        scaleStartRow = scaleRow - gap;
                        scaleEndRow = scaleRow + gap;
                        break;
                    case ANCHOR_VERTICAL_MODE_BOTTOM:
                        gravity |= Gravity.BOTTOM;
                        scaleEndRow = scaleRow;
                        break;
                }
                mCCLayout.addOrUpdateViewToSafeTitleArea(this,
                        mCCLayout.new ScaledLayoutParams(
                                scaleStartRow, scaleEndRow, scaleStartCol, scaleEndCol));
                setCaptionWindowId(captionWindow.id);
                setRowLimit(captionWindow.rowCount);
                setGravity(gravity);
                if (captionWindow.visible) {
                    show();
                } else {
                    hide();
                }
            }

            @Override
            public void onLayoutChange(
                    View v, int left, int top, int right, int bottom, int oldLeft,
                    int oldTop, int oldRight, int oldBottom) {
                int width = right - left;
                int height = bottom - top;
                if (width != mLastCaptionLayoutWidth || height != mLastCaptionLayoutHeight) {
                    mLastCaptionLayoutWidth = width;
                    mLastCaptionLayoutHeight = height;
                    updateTextSize();
                }
            }

            private void updateWidestChar() {
                Paint paint = new Paint();
                paint.setTypeface(mCaptionStyle.getTypeface());
                Charset latin1 = Charset.forName("ISO-8859-1");
                float widestCharWidth = 0f;
                for (int i = 0; i < 256; ++i) {
                    String ch = new String(new byte[]{(byte) i}, latin1);
                    float charWidth = paint.measureText(ch);
                    if (widestCharWidth < charWidth) {
                        widestCharWidth = charWidth;
                        mWidestChar = ch;
                    }
                }
                updateTextSize();
            }

            private void updateTextSize() {
                if (mCCLayout == null) return;

                // Calculate text size based on the max window size.
                StringBuilder widestTextBuilder = new StringBuilder();
                int screenColumnCount = getScreenColumnCount();
                for (int i = 0; i < screenColumnCount; ++i) {
                    widestTextBuilder.append(mWidestChar);
                }
                String widestText = widestTextBuilder.toString();
                Paint paint = new Paint();
                paint.setTypeface(mCaptionStyle.getTypeface());
                float startFontSize = 0f;
                float endFontSize = 255f;
                while (startFontSize < endFontSize) {
                    float testTextSize = (startFontSize + endFontSize) / 2f;
                    paint.setTextSize(testTextSize);
                    float width = paint.measureText(widestText);
                    if (mCCLayout.getWidth() * 0.8f > width) {
                        startFontSize = testTextSize + 0.01f;
                    } else {
                        endFontSize = testTextSize - 0.01f;
                    }
                }
                mTextSize = endFontSize * mFontScale;
                mCCView.setTextSize(mTextSize);
            }

            private int getScreenColumnCount() {
                // Assume it has a wide aspect ratio track.
                return MAX_COLUMN_COUNT_16_9;
            }

            public void removeFromCaptionView() {
                if (mCCLayout != null) {
                    mCCLayout.removeViewFromSafeTitleArea(this);
                    mCCLayout.removeOnLayoutChangeListener(this);
                    mCCLayout = null;
                }
            }

            public void setText(String text) {
                updateText(text, false);
            }

            public void appendText(String text) {
                updateText(text, true);
            }

            public void clearText() {
                mBuilder.clear();
                mCCView.setText("");
            }

            private void updateText(String text, boolean appended) {
                if (!appended) {
                    mBuilder.clear();
                }
                if (text != null && text.length() > 0) {
                    int length = mBuilder.length();
                    mBuilder.append(text);
                    for (CharacterStyle characterStyle : mCharacterStyles) {
                        mBuilder.setSpan(characterStyle, length, mBuilder.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                String[] lines = TextUtils.split(mBuilder.toString(), "\n");

                // Truncate text not to exceed the row limit.
                // Plus one here since the range of the rows is [0, mRowLimit].
                String truncatedText = TextUtils.join("\n", Arrays.copyOfRange(
                        lines, Math.max(0, lines.length - (mRowLimit + 1)), lines.length));
                mBuilder.delete(0, mBuilder.length() - truncatedText.length());

                // Trim the buffer first then set text to CCView.
                int start = 0, last = mBuilder.length() - 1;
                int end = last;
                while ((start <= end) && (mBuilder.charAt(start) <= ' ')) {
                    ++start;
                }
                while ((end >= start) && (mBuilder.charAt(end) <= ' ')) {
                    --end;
                }
                if (start == 0 && end == last) {
                    mCCView.setText(mBuilder);
                } else {
                    SpannableStringBuilder trim = new SpannableStringBuilder();
                    trim.append(mBuilder);
                    if (end < last) {
                        trim.delete(end + 1, last + 1);
                    }
                    if (start > 0) {
                        trim.delete(0, start);
                    }
                    mCCView.setText(trim);
                }
            }

            public void setRowLimit(int rowLimit) {
                if (rowLimit < 0) {
                    throw new IllegalArgumentException("A rowLimit should have a positive number");
                }
                mRowLimit = rowLimit;
            }
        }

        class CCView extends SubtitleView {
            CCView(Context context) {
                this(context, null);
            }

            CCView(Context context, AttributeSet attrs) {
                this(context, attrs, 0);
            }

            CCView(Context context, AttributeSet attrs, int defStyleAttr) {
                super(context, attrs, defStyleAttr);
            }

            void setCaptionStyle(CaptionStyle style) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (style.hasForegroundColor()) {
                        setForegroundColor(style.foregroundColor);
                    }
                    if (style.hasBackgroundColor()) {
                        setBackgroundColor(style.backgroundColor);
                    }
                    if (style.hasEdgeType()) {
                        setEdgeType(style.edgeType);
                    }
                    if (style.hasEdgeColor()) {
                        setEdgeColor(style.edgeColor);
                    }
                }
                setTypeface(style.getTypeface());
            }
        }
    }
}
