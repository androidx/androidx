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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.EdgeEffect;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.LayoutCallback;
import androidx.compose.remote.core.RemoteClock;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SystemClock;
import androidx.compose.remote.core.operations.ColorTheme;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.RootContentBehavior;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.player.core.RemoteDocument;
import androidx.compose.remote.player.core.platform.AndroidRemoteContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Internal view handling the actual painting / interactions
 *
 * <p>Its role is to paint a document as an AndroidView as well as handling user interactions
 * (touch/click).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeView extends FrameLayout implements View.OnAttachStateChangeListener,
        LayoutCallback {

    static final boolean USE_VIEW_AREA_CLICK = true; // Use views to represent click areas
    static final float DEFAULT_FRAME_RATE = 60f;
    static final float POST_TO_NEXT_FRAME_THRESHOLD = 60f;
    private static final int MAX_BITMAP_MEMORY = 20 * 1024 * 1024;
    private String mErrorMessage = "";

    RemoteClock mClock;

    RemoteDocument mDocument = null;
    int mTheme = Theme.SYSTEM;
    boolean mInActionDown = false;
    int mDebug = 0;
    boolean mHasClickAreas = false;
    Point mActionDownPoint = new Point(0, 0);
    AndroidRemoteContext mARContext;
    Map<Integer, Object> mResolvedData = null;

    float mDensity = Float.NaN;
    long mStart;

    long mLastFrameDelay = 1;
    float mMaxFrameRate = DEFAULT_FRAME_RATE; // frames per seconds
    long mMaxFrameDelay = (long) (1000 / mMaxFrameRate);

    long mLastFrameCall;

    private Choreographer mChoreographer;
    private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
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
    public RemoteComposeView(@NonNull Context context) {
        super(context);
        addOnAttachStateChangeListener(this);
        setClock(RemoteClock.SYSTEM);
    }

    /**
     * Constructor for RemoteComposeView.
     *
     * @param context The Context the view is running in.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public RemoteComposeView(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context);
        addOnAttachStateChangeListener(this);
        setClock(RemoteClock.SYSTEM);
    }

    /**
     * Constructor for RemoteComposeView.
     *
     * @param context      The Context the view is running in.
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style
     *                     resource that supplies default values for the view.
     */
    public RemoteComposeView(@NonNull Context context, @NonNull AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.WHITE);
        addOnAttachStateChangeListener(this);
        setClock(RemoteClock.SYSTEM);
    }

    /**
     * Constructor for RemoteComposeView.
     *
     * @param context      The Context the view is running in.
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style
     *                     resource that supplies default values for the view.
     * @param clock        The {@link Clock} to use for timing.
     */
    public RemoteComposeView(@NonNull Context context, @NonNull AttributeSet attrs,
            int defStyleAttr, @NonNull Clock clock) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.WHITE);
        addOnAttachStateChangeListener(this);
        setClock(new SystemClock(clock));
    }

    /**
     * Sets the {@link Clock} for the view. This will also reset the start time, last frame call
     * time, and create a new {@link AndroidRemoteContext} with the given clock.
     *
     * @param clock The {@link Clock} to set.
     */
    private void setClock(@NonNull RemoteClock clock) {
        this.mClock = clock;
        mStart = clock.nanoTime();
        mLastFrameCall = clock.millis();
        mARContext = new AndroidRemoteContext(clock);
        mARContext.setEdgeEffectBuilder(() -> new EdgeEffect(getContext()));
    }

    /**
     * Sets the debug mode for the view.
     */
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
     * Sets the {@link RemoteDocument} for the view to render. This will also reset the clock
     * and frame rate, initialize the context, and update click areas.
     *
     * @param value The {@link RemoteDocument} to set.
     */
    @SuppressWarnings("ReferenceEquality") // newClock != mClock
    public void setDocument(@NonNull RemoteDocument value) {
        RemoteClock newClock = value.getClock();
        if (newClock != mClock) {
            mClock = newClock;
            mStart = mClock.nanoTime();
            mLastFrameCall = mClock.millis();
        }

        mDocument = value;
        mMaxFrameRate = DEFAULT_FRAME_RATE;
        mDocument.initializeContext(mARContext, mResolvedData);
        mDisable = false;
        if (mDocument.getDocument().bitmapMemory() > MAX_BITMAP_MEMORY) {
            mDisable = true;
            mErrorMessage =
                    "Bitmap memory " + mDocument.getDocument().bitmapMemory() / (1024 * 1024)
                            + "MB!";
        }
        mARContext.setDocLoadTime();
        mARContext.setAnimationEnabled(true);
        mARContext.setDensity(mDensity);
        mARContext.setUseChoreographer(true);
        setContentDescription(mDocument.getDocument().getContentDescription());

        mDocument.getDocument().setLayoutCallback(this);

        updateClickAreas();
        requestLayout();
        mARContext.loadFloat(RemoteContext.ID_TOUCH_EVENT_TIME, -Float.MAX_VALUE);
        mARContext.loadFloat(RemoteContext.ID_FONT_SIZE, getDefaultTextSize());
        try {
            mDocument.applyDataOperations(mARContext);
        } catch (Exception e) {
            e.printStackTrace();
            mDisable = true;
            mErrorMessage = e.getMessage();
            invalidate();
        }
        invalidate();
        Integer debug = (Integer) mDocument.getDocument().getProperty(Header.DEBUG);
        if (debug != null) {
            if (debug > 0) {
                System.out.println("Debug level set to " + debug);
                setDebug(debug);
            }
        }
        Integer fps = (Integer) mDocument.getDocument().getProperty(Header.DOC_DESIRED_FPS);
        if (fps != null && fps > 0) {
            mMaxFrameRate = fps;
            mMaxFrameDelay = (long) (1000 / mMaxFrameRate);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull View view) {
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
                ClickAreaView viewArea = new ClickAreaView(getContext(), mDebug == 1, area.getId(),
                        area.getContentDescription(), area.getMetadata());
                int w = (int) area.width();
                int h = (int) area.height();
                FrameLayout.LayoutParams param = new FrameLayout.LayoutParams(w, h);
                param.width = w;
                param.height = h;
                param.leftMargin = (int) area.getLeft();
                param.topMargin = (int) area.getTop();
                viewArea.setOnClickListener(
                        view1 -> mDocument.getDocument().performClick(mARContext, area.getId(),
                                area.getMetadata()));
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
    public void setHapticEngine(CoreDocument.@NonNull HapticEngine engine) {
        mDocument.getDocument().setHapticEngine(engine);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull View view) {
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
    public String @Nullable [] getNamedColors() {
        return mDocument.getNamedColors();
    }

    /**
     * Get a array of the names of the "Themed Colors" defined in the loaded doc
     *
     * @return array of names or null
     */
    public  @NonNull ArrayList<ColorTheme> getThemedColors() {
        return mDocument.getThemedColors();
    }

    /**
     * Gets a array of Names of the named variables of a specific type defined in the loaded doc.
     *
     * @param type the type of variable NamedVariable.COLOR_TYPE, STRING_TYPE, etc
     * @return array of name or null
     */
    public String @NonNull [] getNamedVariables(int type) {
        return mDocument.getNamedVariables(type);
    }

    /**
     * set the color associated with this name.
     *
     * @param colorName  Name of color typically "android.xxx"
     * @param colorValue "the argb value"
     */
    public void setColor(@NonNull String colorName, int colorValue) {
        mARContext.setNamedColorOverride(colorName, colorValue);
    }

    /**
     * set the value of a long associated with this name.
     *
     * @param name  Name of color typically "android.xxx"
     * @param value the long value
     */
    public void setLong(@NonNull String name, long value) {
        mARContext.setNamedLong(name, value);
    }

    /**
     * Get the document associated with this RemoteComposeView
     *
     * @return the document
     */
    public @NonNull RemoteDocument getDocument() {
        return mDocument;
    }

    /**
     * Set a local named string
     *
     * @param name    name of the string
     * @param content value of the string
     */
    public void setLocalString(@NonNull String name, @NonNull String content) {
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
    public void clearLocalString(@NonNull String name) {
        mARContext.clearNamedStringOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Set a local named int
     *
     * @param name    name of the int
     * @param content value of the int
     */
    public void setLocalInt(@NonNull String name, int content) {
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
    public void clearLocalInt(@NonNull String name) {
        mARContext.clearNamedIntegerOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Set a local named color
     */
    public void setLocalColor(@NonNull String name, int content) {
        mARContext.setNamedColorOverride(name, content);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Clear a local named color
     */
    public void clearLocalColor(@NonNull String name) {
        mARContext.clearNamedDataOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Set a local named float
     *
     * @param name    name of the float
     * @param content value of the float
     */
    public void setLocalFloat(@NonNull String name, @NonNull Float content) {
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
    public void clearLocalFloat(@NonNull String name) {
        mARContext.clearNamedFloatOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Set a local named bitmap
     *
     * @param name    name of the bitmap
     * @param content value of the bitmap
     */
    public void setLocalBitmap(@NonNull String name, @NonNull Bitmap content) {
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
    public void clearLocalBitmap(@NonNull String name) {
        mARContext.clearNamedDataOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    int copySensorListeners(int @NonNull [] ids) {
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
    public void checkShaders(CoreDocument.@NonNull ShaderControl shaderControl) {
        mDocument.getDocument().checkShaders(mARContext, shaderControl);
    }

    /**
     * Set to true to use the choreographer
     */
    public void setUseChoreographer(boolean value) {
        mARContext.setUseChoreographer(value);
    }

    /**
     * Returns the current RemoteContext
     */
    public @NonNull RemoteContext getRemoteContext() {
        return mARContext;
    }

    /**
     * Update the current document with the data contained in the passed document
     *
     * @param document document containing updates
     */
    public void applyUpdate(@NonNull RemoteDocument document) {
        mDocument.getDocument().applyUpdate(document.getDocument());
    }

    @Override
    public void onRequestLayout() {
        requestLayout();
    }

    /**
     * Interface to receive click events on components.
     */
    public interface ClickCallbacks {
        /**
         * Called to notify the document that something has been clicked on.
         *
         * @param id       The id for component clicked on.
         * @param metadata Optional metadata for the event.
         */
        void click(int id, @NonNull String metadata);
    }

    /**
     * Add a listener to events sent from this document, with an id and optional metadata.
     *
     * @param callback the callback to process events.
     */
    public void addIdActionListener(@NonNull ClickCallbacks callback) {
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
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int index = event.getActionIndex();
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
                    mARContext.loadFloat(RemoteContext.ID_TOUCH_EVENT_TIME,
                            mARContext.getAnimationTime());
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
                    mARContext.loadFloat(RemoteContext.ID_TOUCH_EVENT_TIME,
                            mARContext.getAnimationTime());
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
                        mARContext.loadFloat(RemoteContext.ID_TOUCH_EVENT_TIME,
                                mARContext.getAnimationTime());
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
        mDocument.getDocument().onClick(mARContext, (float) mActionDownPoint.x,
                (float) mActionDownPoint.y);
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

        int w;
        int h;

        if (!mDocument.useFeature(Header.FEATURE_PAINT_MEASURE)) {
            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            float maxWidth = Float.MAX_VALUE;
            float maxHeight = Float.MAX_VALUE;
            switch (widthMode) {
                case MeasureSpec.EXACTLY:
                    maxWidth = widthSize;
                    break;
                case MeasureSpec.AT_MOST:
                    maxWidth = widthSize;
                    break;
                case MeasureSpec.UNSPECIFIED:
                    break;
            }
            switch (heightMode) {
                case MeasureSpec.EXACTLY:
                    maxHeight = heightSize;
                    break;
                case MeasureSpec.AT_MOST:
                    maxHeight = heightSize;
                    break;
                case MeasureSpec.UNSPECIFIED:
                    break;
            }

            if (mARContext.getPaintContext() != null) {
                mDocument.getDocument().measure(mARContext, 0, maxWidth, 0,
                        maxHeight);
            }

            w = measureDimension(widthMeasureSpec, mDocument.getWidth());
            h = measureDimension(heightMeasureSpec, mDocument.getHeight());

            if (mARContext.getPaintContext() == null) {
                if (w == 0) {
                    w = (int) maxWidth;
                }
                if (h == 0) {
                    h = (int) maxHeight;
                }
            }
        } else {
            w = measureDimension(widthMeasureSpec, mDocument.getWidth());
            h = measureDimension(heightMeasureSpec, mDocument.getHeight());
        }

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
     * This returns the amount of time in ms the player used to evaluate a pass it is averaged over
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
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (mDocument == null) {
            return;
        }
        if (mDisable) {
            drawDisable(canvas, mErrorMessage);
            return;
        }
        int theme = (mTheme == Theme.SYSTEM) ? Theme.LIGHT : mTheme;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // REMOVE IN PLATFORM
            if (mTheme == Theme.SYSTEM) {
                int mode = getResources().getConfiguration().isNightModeActive() ? Theme.DARK
                        : Theme.LIGHT;
                theme = mode;
            }
        } // REMOVE IN PLATFORM
        try {
            long nanoStart = mClock.nanoTime();
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
            mDocument.paint(mARContext, theme);
            if (mDebug == 1) {
                mCount++;
                long nanoEnd = mClock.nanoTime();
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
                        System.err.println("RC : POST CHOREOGRAPHER WITH " + mLastFrameDelay
                                + " (nextFrame was " + nextFrame + ", max delay " + mMaxFrameDelay
                                + ", " + " max framerate is " + mMaxFrameRate + ")");
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
                mDuration += mClock.nanoTime() - start;
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
            System.err.println("RC : Delay since last frame " + frameDelay + " ms (" + (1000f
                    / (float) frameDelay) + " fps)");
            mLastFrameCall = System.currentTimeMillis();
        }
    }

    private void drawDisable(@NonNull Canvas canvas, String message) {
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
        if (message != null) {
            paint.getTextBounds(message, 0, message.length(), rect);
            x = w / 2f - rect.width() / 2f - rect.left;
            canvas.drawText(message, x, y, paint);
        }
    }

    private float getDefaultTextSize() {
        return new TextView(getContext()).getTextSize();
    }

    /**
     * Set the resolved data for the document
     *
     * @param resolvedData the resolved data
     */
    public void setResolvedData(@Nullable Map<Integer, Object> resolvedData) {
        mResolvedData = resolvedData;
    }
}
