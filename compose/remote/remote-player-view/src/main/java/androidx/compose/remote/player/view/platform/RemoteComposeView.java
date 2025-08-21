/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.player.view.platform;

import static androidx.compose.remote.core.RemoteClock.nanoTime;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.RestrictTo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SystemClock;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.RootContentBehavior;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.player.view.RemoteComposeDocument;

import org.jspecify.annotations.NonNull;

import java.time.Clock;
import java.util.Set;

/**
 * Internal view handling the actual painting / interactions
 *
 * <p>Its role is to paint a document as an AndroidView as well as handling user interactions
 * (touch/click).
 */
@RestrictTo(LIBRARY_GROUP)
public class RemoteComposeView extends FrameLayout implements View.OnAttachStateChangeListener {

    static final boolean USE_VIEW_AREA_CLICK = true; // Use views to represent click areas
    static final float DEFAULT_FRAME_RATE = 60f;
    static final float POST_TO_NEXT_FRAME_THRESHOLD = 60f;
    private static final int MAX_BITMAP_MEMORY = 20 * 1024 * 1024;
    private String mErrorMessage = "";

    Clock mClock;

    RemoteComposeDocument mDocument = null;
    int mTheme = Theme.LIGHT;
    boolean mInActionDown = false;
    int mDebug = 0;
    boolean mHasClickAreas = false;
    Point mActionDownPoint = new Point(0, 0);
    AndroidRemoteContext mARContext;

    float mDensity = Float.NaN;
    long mStart;

    long mLastFrameDelay = 1;
    float mMaxFrameRate = DEFAULT_FRAME_RATE; // frames per seconds
    long mMaxFrameDelay = (long) (1000 / mMaxFrameRate);

    long mLastFrameCall;

    private Choreographer mChoreographer;
    private Choreographer.FrameCallback mFrameCallback =
            new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    mARContext.currentTime = frameTimeNanos / 1000000;
                    mARContext.setDebug(mDebug);
                    postInvalidateOnAnimation();
                }
            };

    /**
     * Constructor for RemoteComposeView.
     *
     * @param context The Context the view is running in.
     */
    public RemoteComposeView(Context context) {
        super(context);
        addOnAttachStateChangeListener(this);
        setClock(new SystemClock());
    }

    /**
     * Constructor for RemoteComposeView.
     *
     * @param context The Context the view is running in.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public RemoteComposeView(Context context, AttributeSet attrs) {
        super(context);
        addOnAttachStateChangeListener(this);
        setClock(new SystemClock());
    }

    /**
     * Constructor for RemoteComposeView.
     *
     * @param context The Context the view is running in.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style
     *     resource that supplies default values for the view.
     */
    public RemoteComposeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.WHITE);
        addOnAttachStateChangeListener(this);
        setClock(new SystemClock());
    }

    /**
     * Constructor for RemoteComposeView.
     *
     * @param context The Context the view is running in.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style
     *     resource that supplies default values for the view.
     * @param clock The {@link Clock} to use for timing.
     */
    public RemoteComposeView(Context context, AttributeSet attrs, int defStyleAttr, Clock clock) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.WHITE);
        addOnAttachStateChangeListener(this);
        setClock(clock);
    }

    /**
     * Sets the {@link Clock} for the view. This will also reset the start time, last frame call
     * time, and create a new {@link AndroidRemoteContext} with the given clock.
     *
     * @param clock The {@link Clock} to set.
     */
    private void setClock(Clock clock) {
        this.mClock = clock;
        mStart = nanoTime(clock);
        mLastFrameCall = clock.millis();
        mARContext = new AndroidRemoteContext(clock);
    }

    /** Sets the debug mode for the view. */
    public void setDebug(int value) {
        if (mDebug != value) {
            mDebug = value;
            if (USE_VIEW_AREA_CLICK) {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    if (child instanceof ClickAreaView) {
                        ((ClickAreaView) child).setDebug(mDebug == 1);
                    }
                }
            }
            invalidate();
        }
    }

    /**
     * Sets the {@link RemoteComposeDocument} for the view to render. This will also reset the clock
     * and frame rate, initialize the context, and update click areas.
     *
     * @param value The {@link RemoteComposeDocument} to set.
     */
    public void setDocument(@NonNull RemoteComposeDocument value) {
        Clock newClock = value.getClock();
        if (newClock != mClock) {
            mClock = newClock;
            mStart = nanoTime(mClock);
            mLastFrameCall = mClock.millis();
        }

        mDocument = value;
        mMaxFrameRate = DEFAULT_FRAME_RATE;
        mDocument.initializeContext(mARContext);
        mDisable = false;
        if (mDocument.getDocument().bitmapMemory() > MAX_BITMAP_MEMORY) {
            mDisable = true;
            mErrorMessage =
                    "Bitmap memory "
                            + mDocument.getDocument().bitmapMemory() / (1024 * 1024)
                            + "MB!";
        }
        mARContext.setDocLoadTime();
        mARContext.setAnimationEnabled(true);
        mARContext.setDensity(mDensity);
        mARContext.setUseChoreographer(true);
        setContentDescription(mDocument.getDocument().getContentDescription());

        updateClickAreas();
        requestLayout();
        mARContext.loadFloat(RemoteContext.ID_TOUCH_EVENT_TIME, -Float.MAX_VALUE);
        mARContext.loadFloat(RemoteContext.ID_FONT_SIZE, getDefaultTextSize());

        invalidate();
        Integer fps = (Integer) mDocument.getDocument().getProperty(Header.DOC_DESIRED_FPS);
        if (fps != null && fps > 0) {
            mMaxFrameRate = fps;
            mMaxFrameDelay = (long) (1000 / mMaxFrameRate);
        }
    }

    @Override
    public void onViewAttachedToWindow(View view) {
        if (mChoreographer == null) {
            mChoreographer = Choreographer.getInstance();
            mChoreographer.postFrameCallback(mFrameCallback);
        }
        mDensity = getContext().getResources().getDisplayMetrics().density;
        mARContext.setDensity(mDensity);
        if (mDocument == null) {
            return;
        }
        updateClickAreas();
    }

    private void updateClickAreas() {
        if (USE_VIEW_AREA_CLICK && mDocument != null) {
            mHasClickAreas = false;
            Set<CoreDocument.ClickAreaRepresentation> clickAreas =
                    mDocument.getDocument().getClickAreas();
            removeAllViews();
            for (CoreDocument.ClickAreaRepresentation area : clickAreas) {
                ClickAreaView viewArea =
                        new ClickAreaView(
                                getContext(),
                                mDebug == 1,
                                area.getId(),
                                area.getContentDescription(),
                                area.getMetadata());
                int w = (int) area.width();
                int h = (int) area.height();
                FrameLayout.LayoutParams param = new FrameLayout.LayoutParams(w, h);
                param.width = w;
                param.height = h;
                param.leftMargin = (int) area.getLeft();
                param.topMargin = (int) area.getTop();
                viewArea.setOnClickListener(
                        view1 ->
                                mDocument
                                        .getDocument()
                                        .performClick(
                                                mARContext, area.getId(), area.getMetadata()));
                addView(viewArea, param);
            }
            if (!clickAreas.isEmpty()) {
                mHasClickAreas = true;
            }
        }
    }

    /**
     * Sets the haptic engine for the view
     *
     * @param engine the HapticEngine
     */
    public void setHapticEngine(CoreDocument.HapticEngine engine) {
        mDocument.getDocument().setHapticEngine(engine);
    }

    @Override
    public void onViewDetachedFromWindow(View view) {
        if (mChoreographer != null) {
            mChoreographer.removeFrameCallback(mFrameCallback);
            mChoreographer = null;
        }
        removeAllViews();
    }

    /**
     * Get a array of the names of the "named colors" defined in the loaded doc
     *
     * @return array of names or null
     */
    public String[] getNamedColors() {
        return mDocument.getNamedColors();
    }

    /**
     * Gets a array of Names of the named variables of a specific type defined in the loaded doc.
     *
     * @param type the type of variable NamedVariable.COLOR_TYPE, STRING_TYPE, etc
     * @return array of name or null
     */
    public String[] getNamedVariables(int type) {
        return mDocument.getNamedVariables(type);
    }

    /**
     * set the color associated with this name.
     *
     * @param colorName Name of color typically "android.xxx"
     * @param colorValue "the argb value"
     */
    public void setColor(String colorName, int colorValue) {
        mARContext.setNamedColorOverride(colorName, colorValue);
    }

    /**
     * set the value of a long associated with this name.
     *
     * @param name Name of color typically "android.xxx"
     * @param value the long value
     */
    public void setLong(String name, long value) {
        mARContext.setNamedLong(name, value);
    }

    /**
     * Get the document associated with this RemoteComposeView
     *
     * @return the document
     */
    public RemoteComposeDocument getDocument() {
        return mDocument;
    }

    /**
     * Set a local named string
     *
     * @param name name of the string
     * @param content value of the string
     */
    public void setLocalString(String name, String content) {
        mARContext.setNamedStringOverride(name, content);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Clear a local named string
     *
     * @param name name to clear
     */
    public void clearLocalString(String name) {
        mARContext.clearNamedStringOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Set a local named int
     *
     * @param name name of the int
     * @param content value of the int
     */
    public void setLocalInt(String name, int content) {
        mARContext.setNamedIntegerOverride(name, content);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Clear a local named int
     *
     * @param name name to clear
     */
    public void clearLocalInt(String name) {
        mARContext.clearNamedIntegerOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Set a local named color
     *
     * @param name
     * @param content
     */
    public void setLocalColor(String name, int content) {
        mARContext.setNamedColorOverride(name, content);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Clear a local named color
     *
     * @param name
     */
    public void clearLocalColor(String name) {
        mARContext.clearNamedDataOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Set a local named float
     *
     * @param name name of the float
     * @param content value of the float
     */
    public void setLocalFloat(String name, Float content) {
        mARContext.setNamedFloatOverride(name, content);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Clear a local named float
     *
     * @param name name to clear
     */
    public void clearLocalFloat(String name) {
        mARContext.clearNamedFloatOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Set a local named bitmap
     *
     * @param name name of the bitmap
     * @param content value of the bitmap
     */
    public void setLocalBitmap(String name, Bitmap content) {
        mARContext.setNamedDataOverride(name, content);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Clear a local named bitmap
     *
     * @param name name to clear.
     */
    public void clearLocalBitmap(String name) {
        mARContext.clearNamedDataOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    int copySensorListeners(int[] ids) {
        int count = 0;
        for (int id = RemoteContext.ID_ACCELERATION_X; id <= RemoteContext.ID_LIGHT; id++) {
            if (mARContext.mRemoteComposeState.hasListener(id)) {
                ids[count++] = id;
            }
        }
        return count;
    }

    /**
     * set a float externally
     *
     * @param id
     * @param value
     */
    public void setExternalFloat(int id, float value) {
        mARContext.loadFloat(id, value);
    }

    /**
     * Returns true if the document supports drag touch events
     *
     * @return true if draggable content, false otherwise
     */
    public boolean isDraggable() {
        if (mDocument == null) {
            return false;
        }
        return mDocument.getDocument().hasTouchListener();
    }

    /**
     * Check shaders and disable them
     *
     * @param shaderControl the callback to validate the shader
     */
    public void checkShaders(CoreDocument.ShaderControl shaderControl) {
        mDocument.getDocument().checkShaders(mARContext, shaderControl);
    }

    /**
     * Set to true to use the choreographer
     *
     * @param value
     */
    public void setUseChoreographer(boolean value) {
        mARContext.setUseChoreographer(value);
    }

    /**
     * Returns the current RemoteContext
     *
     * @return
     */
    public RemoteContext getRemoteContext() {
        return mARContext;
    }

    /**
     * Update the current document with the data contained in the passed document
     *
     * @param document document containing updates
     */
    public void applyUpdate(RemoteComposeDocument document) {
        mDocument.getDocument().applyUpdate(document.getDocument());
    }

    /** Interface to receive click events on components. */
    public interface ClickCallbacks {
        /**
         * Called to notify the document that something has been clicked on.
         *
         * @param id The id for component clicked on.
         * @param metadata Optional metadata for the event.
         */
        void click(int id, String metadata);
    }

    /**
     * Add a listener to events sent from this document, with an id and optional metadata.
     *
     * @param callback the callback to process events.
     */
    public void addIdActionListener(ClickCallbacks callback) {
        if (mDocument == null) {
            return;
        }
        mDocument.getDocument().addIdActionListener((id, metadata) -> callback.click(id, metadata));
    }

    public int getTheme() {
        return mTheme;
    }

    public void setTheme(int theme) {
        this.mTheme = theme;
    }

    private VelocityTracker mVelocityTracker = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int index = event.getActionIndex();
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(index);
        if (USE_VIEW_AREA_CLICK && mHasClickAreas) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mActionDownPoint.x = (int) event.getX();
                mActionDownPoint.y = (int) event.getY();
                CoreDocument doc = mDocument.getDocument();
                if (doc.hasTouchListener()) {
                    mARContext.loadFloat(
                            RemoteContext.ID_TOUCH_EVENT_TIME, mARContext.getAnimationTime());
                    mInActionDown = true;
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                    } else {
                        mVelocityTracker.clear();
                    }
                    mVelocityTracker.addMovement(event);
                    doc.touchDown(mARContext, event.getX(), event.getY());
                    invalidate();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_CANCEL:
                mInActionDown = false;
                doc = mDocument.getDocument();
                if (doc.hasTouchListener()) {
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float dx = mVelocityTracker.getXVelocity(pointerId);
                    float dy = mVelocityTracker.getYVelocity(pointerId);
                    doc.touchCancel(mARContext, event.getX(), event.getY(), dx, dy);
                    invalidate();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
                mInActionDown = false;
                performClick();
                doc = mDocument.getDocument();
                if (doc.hasTouchListener()) {
                    mARContext.loadFloat(
                            RemoteContext.ID_TOUCH_EVENT_TIME, mARContext.getAnimationTime());
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float dx = mVelocityTracker.getXVelocity(pointerId);
                    float dy = mVelocityTracker.getYVelocity(pointerId);
                    doc.touchUp(mARContext, event.getX(), event.getY(), dx, dy);
                    invalidate();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (mInActionDown) {
                    if (mVelocityTracker != null) {
                        mARContext.loadFloat(
                                RemoteContext.ID_TOUCH_EVENT_TIME, mARContext.getAnimationTime());
                        mVelocityTracker.addMovement(event);
                        doc = mDocument.getDocument();
                        boolean repaint = doc.touchDrag(mARContext, event.getX(), event.getY());
                        if (repaint) {
                            invalidate();
                        }
                    }
                    return true;
                }
                return false;
        }
        return false;
    }

    @Override
    public boolean performClick() {
        if (USE_VIEW_AREA_CLICK && mHasClickAreas) {
            return super.performClick();
        }
        mDocument
                .getDocument()
                .onClick(mARContext, (float) mActionDownPoint.x, (float) mActionDownPoint.y);
        super.performClick();
        invalidate();
        return true;
    }

    private int measureDimension(int measureSpec, int intrinsicSize) {
        int result = intrinsicSize;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        switch (mode) {
            case MeasureSpec.EXACTLY:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
                result = Integer.min(size, intrinsicSize);
                break;
            case MeasureSpec.UNSPECIFIED:
                result = intrinsicSize;
        }
        return result;
    }

    private static final float[] sScaleOutput = new float[2];

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mDocument == null) {
            return;
        }
        int preWidth = getWidth();
        int preHeight = getHeight();
        int w = measureDimension(widthMeasureSpec, mDocument.getWidth());
        int h = measureDimension(heightMeasureSpec, mDocument.getHeight());

        if (!USE_VIEW_AREA_CLICK) {
            if (mDocument.getDocument().getContentSizing() == RootContentBehavior.SIZING_SCALE) {
                mDocument.getDocument().computeScale(w, h, sScaleOutput);
                w = (int) (mDocument.getWidth() * sScaleOutput[0]);
                h = (int) (mDocument.getHeight() * sScaleOutput[1]);
            }
        }
        setMeasuredDimension(w, h);
        if (preWidth != w || preHeight != h) {
            mDocument.getDocument().invalidateMeasure();
        }
    }

    private int mCount;
    private long mTime = System.nanoTime();
    private long mDuration;
    private boolean mEvalTime = false; // turn on to measure eval time
    private float mLastAnimationTime = 0.1f; // set to random non 0 number
    private boolean mDisable = false;

    /**
     * This returns the amount of time in ms the player used to evalueate a pass it is averaged over
     * a number of evaluations.
     *
     * @return time in ms
     */
    public float getEvalTime() {
        if (!mEvalTime) {
            mEvalTime = true;
            return 0.0f;
        }
        double avg = mDuration / (double) mCount;
        if (mCount > 100) {
            mDuration /= 2;
            mCount /= 2;
        }
        return (float) (avg * 1E-6); // ms
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDocument == null) {
            return;
        }
        if (mDisable) {
            drawDisable(canvas, mErrorMessage);
            return;
        }
        try {
            long nanoStart = nanoTime(mClock);
            long start = mEvalTime ? nanoStart : 0; // measure execution of commands

            float animationTime = (nanoStart - mStart) * 1E-9f;
            mARContext.setAnimationTime(animationTime);
            mARContext.loadFloat(RemoteContext.ID_ANIMATION_TIME, animationTime);
            float loopTime = animationTime - mLastAnimationTime;
            mARContext.loadFloat(RemoteContext.ID_ANIMATION_DELTA_TIME, loopTime);
            mLastAnimationTime = animationTime;
            mARContext.setAnimationEnabled(true);
            mARContext.currentTime = mClock.millis();
            mARContext.setDebug(mDebug);
            mARContext.useCanvas(canvas);
            mARContext.mWidth = getWidth();
            mARContext.mHeight = getHeight();
            mDocument.paint(mARContext, mTheme);
            if (mDebug == 1) {
                mCount++;
                long nanoEnd = nanoTime(mClock);
                if (nanoEnd - mTime > 1000000000L) {
                    System.out.println(" count " + mCount + " fps");
                    mCount = 0;
                    mTime = nanoEnd;
                }
            }
            int nextFrame = mDocument.needsRepaint();
            if (nextFrame > 0) {
                if (mMaxFrameRate >= POST_TO_NEXT_FRAME_THRESHOLD) {
                    mLastFrameDelay = nextFrame;
                } else {
                    mLastFrameDelay = Math.max(mMaxFrameDelay, nextFrame);
                }
                if (mChoreographer != null) {
                    if (mDebug == 1) {
                        System.err.println(
                                "RC : POST CHOREOGRAPHER WITH "
                                        + mLastFrameDelay
                                        + " (nextFrame was "
                                        + nextFrame
                                        + ", max delay "
                                        + mMaxFrameDelay
                                        + ", "
                                        + " max framerate is "
                                        + mMaxFrameRate
                                        + ")");
                    }
                    mChoreographer.postFrameCallbackDelayed(mFrameCallback, mLastFrameDelay);
                }
                if (!mARContext.getUseChoreographer()) {
                    invalidate();
                }
            } else {
                if (mChoreographer != null) {
                    mChoreographer.removeFrameCallback(mFrameCallback);
                }
            }
            if (mEvalTime) {
                mDuration += nanoTime(mClock) - start;
                mCount++;
            }
        } catch (Exception ex) {
            int count = mARContext.getLastOpCount();
            mDisable = true;
            invalidate();
            int errorId = mDocument.getDocument().getHostExceptionID();
            System.err.println("Exception in draw " + count);
            System.err.println("\"" + ex.getMessage() + "\"");
            mErrorMessage = ex.getMessage();
            ex.printStackTrace();
            Utils.log(ex.toString());
            if (errorId != 0) {
                Utils.log("calling exception handler " + errorId);
                mDocument.getDocument().notifyOfException(errorId, ex.toString());
            }
        }
        if (mDebug == 1) {
            long frameDelay = mClock.millis() - mLastFrameCall;
            System.err.println(
                    "RC : Delay since last frame "
                            + frameDelay
                            + " ms ("
                            + (1000f / (float) frameDelay)
                            + " fps)");
            mLastFrameCall = System.currentTimeMillis();
        }
    }

    private void drawDisable(Canvas canvas, String message) {
        Rect rect = new Rect();
        canvas.drawColor(Color.BLACK);
        Paint paint = new Paint();
        paint.setTextSize(128f);
        paint.setColor(Color.RED);
        int w = getWidth();
        int h = getHeight();

        String str = "⚠";
        paint.getTextBounds(str, 0, 1, rect);

        float x = w / 2f - rect.width() / 2f - rect.left;
        float y = h / 2f + rect.height() / 2f - rect.bottom;

        canvas.drawText(str, x, y, paint);
        paint.setTextSize(48f);
        y += rect.height();
        paint.getTextBounds(message, 0, message.length(), rect);
        x = w / 2f - rect.width() / 2f - rect.left;
        canvas.drawText(message, x, y, paint);
    }

    private float getDefaultTextSize() {
        return new TextView(getContext()).getTextSize();
    }
}
