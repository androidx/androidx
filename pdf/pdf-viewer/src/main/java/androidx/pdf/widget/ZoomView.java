/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.ViewCompat;
import androidx.pdf.R;
import androidx.pdf.ViewState;
import androidx.pdf.data.Range;
import androidx.pdf.metrics.EventCallback;
import androidx.pdf.metrics.EventState;
import androidx.pdf.metrics.GestureRecordingProcessor;
import androidx.pdf.metrics.PositionState;
import androidx.pdf.util.GestureTracker;
import androidx.pdf.util.GestureTrackingView;
import androidx.pdf.util.MathUtils;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.Observables;
import androidx.pdf.util.Preconditions;
import androidx.pdf.util.Screen;
import androidx.pdf.util.ThreadUtils;
import androidx.pdf.util.ZoomScrollRestorer;
import androidx.pdf.util.ZoomUtils;
import androidx.pdf.viewer.LayoutHandler;
import androidx.pdf.viewer.PaginatedView;
import androidx.pdf.viewer.PdfSelectionModel;

import com.google.android.material.motion.MotionUtils;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;

/**
 * Layout container for a content {@link View} that enables zooming and panning, in the same way a
 * ScrollView does for scrolling. It doesn't make any assumption on what its content is, except that
 * there should be only one content child view (as ZoomView is a FrameLayout), and that the content
 * view's measures are its scroll limits.
 *
 * <p>The zoom is uniform across dimensions ({@link #getScaleX()} == {@link #getScaleY()}).
 *
 * <p>More precisely, this view:
 *
 * <ul>
 *   <li>Enables pinch-to-zoom gesture to zoom the content around the gesture's focus point
 *   <li>Allows scrolling along both dimensions till the bounds of the content (and no further)
 *   <li>Keeps the content centered in case it is smaller than the viewport (in either dimension),
 *       including at the initial load time
 *   <li>Guarantees the 2 constraints above are applied at all times, including during a gesture
 *   <li>Handles fling gestures in any direction on the 2D surface
 *   <li>After a screen rotation (or any change of layout), keeps the view centered on whatever it
 *       was previously centered on.
 *   <li>Shares scrolling gestures with parent views: it captures a scroll gesture as long as it can
 *       make use of it (i.e. the content actually scrolls that way), but releases the gesture when
 *       the content reaches the end of the scroll area in that direction, and gives its parent
 *       views an opportunity to use the same gesture for a wider scroll effect (e.g. moving this
 *       view altogether).
 *   <li>Reports its position accurately, both during a movement (e.g. a scale or a fling), and at
 *       when the movement stops and the position is stable.
 *   <li>Handles double-tap to cycle the zoom between fit-to-width and the last saved zoom.
 * </ul>
 *
 * <p>XML Layout attributes that control this View:
 *
 * <ul>
 *   <li>minZoom, maxZoom (integers): min and max possible zoom.
 *   <li>saveState (boolean, defaults as true): preserve the viewing state when destroyed.
 * </ul>
 *
 * <p>The following extra attributes can currently be configured only from Java:
 *
 * <ul>
 *   <li>setInitialZoom, setInitialZoomMode - control the zoom level the ZoomView starts at. Default
 *       is 100% zoom.
 *   <li>setFitMode - control how the content is fit onto the screen. Default is to fit the entire
 *       width and height of the content onto the screen.
 *   <li>setRotateMode - control how the zoom changes when the device is rotated. Default is not to
 *       change the zoom when the device is rotated, excepting that:
 *   <li>setKeepFitZoomOnRotate - a special case is when the content exactly fits the screen before
 *       rotation. If this flag is set, then the zoom will be changed so it still exactly fits after
 *       the rotation. Default is true.
 *   <li>setContentResizedMode - control how the viewport moves when the size of the content
 *       changes. The viewport can either try to stay with the same absolute point eg (10px, 20px)
 *       or the same relative point, eg (25%, 50%). Default is same absolute point.
 *   <li>setOverrideMinZoomToFit - if this flag is set, zoom levels lower than min zoom are allowed
 *       if they are needed to fit the entire content on screen. Default is false.
 *   <li>setOverrideMaxZoomToFit - if this flag is set, zoom levels lower than max zoom are allowed
 *       if they are needed to make the content fill the entire screen. Default is false.
 * </ul>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings({"deprecation", "RestrictedApiAndroidX"})
public class ZoomView extends GestureTrackingView implements ZoomScrollRestorer {

    private static final float ZOOM_RESET = 1.5F;
    private static final float DEFAULT_MIN_ZOOM = 0.5f;
    private static final float DEFAULT_MAX_ZOOM = 64.0f;
    private static final boolean STABLE = true;
    private static final boolean UNSTABLE = false;
    private static final String KEY_SUPER = "s";
    private static final String KEY_POSITION = "p";
    private static final String KEY_VIEWPORT_INITIALIZED = "vi";
    private static final String KEY_VIEWPORT = "v";
    private static final String KEY_CONTENT_ZOOM = "z";
    private static final String KEY_RAW_BOUNDS = "b";
    private static final String KEY_PADDING = "pa";
    private static final String KEY_LOOKAT_POINT = "l";
    private static final String KEY_DOCUMENT_LOADED = "dl";
    private static final String KEY_INITIAL_ZOOM_DONE = "izd";
    private static final int OVERSCROLL_THRESHOLD = 25;
    /** Fallback duration for the zoom animation, when material attributes are unavailable. */
    private static final int FALLBACK_ZOOM_ANIMATION_DURATION_MS = 250;
    private static final int UNBOUNDED = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    public static final float LINEAR_SCALE_ZOOM_FACTOR = 2.F;
    public static final float DOUBLE_TAP_ZOOM_EPSILON = 0.25f;
    @VisibleForTesting
    protected final ZoomGestureHandler mGestureHandler =
            new ZoomGestureHandler(new Screen(getContext()));
    private final Handler mHandler = new Handler();
    /** The viewport is the usable area of this view, i.e. its dimensions less padding. */
    private final Rect mViewport = new Rect();
    private final boolean mSaveState;
    private final RelativeScroller mScroller;
    private final Observables.ExposedValue<ZoomScroll> mPosition;
    /**
     * The raw bounds of the content view, i.e. before any transformation: (0, 0 - width,
     * height).
     */
    private Rect mContentRawBounds = new Rect();
    /** Enables the double tap gesture to zoom in/out. */
    private boolean mDoubleTapEnabled = true;
    /** Whether we are in a fling movement. This is used to detect the end of that movement. */
    private boolean mIsFling = false;
    /**
     * Accumulates, during one single gesture, the amount that the user scrolls past the left (if
     * negative) or right (if positive) edge of the content. This amount is not actually scrolled
     * (as the scroll area is limited by that edge), but can be compared to a threshold to decide to
     * release this gesture for parent views to use.
     */
    private int mOverScrollX;
    /**
     * Accumulates, during one single gesture, the amount that the user scrolls past the top (if
     * negative) or bottom (if positive) edge of the content. This amount is not actually scrolled
     * (as the scroll area is limited by that edge), but can be compared to a threshold to decide to
     * release this gesture for parent views to use.
     */
    private int mOverScrollY;
    /**
     * If non-null, the next layout pass attempts to move the window to this position, as faithfully
     * as possible.
     */
    private ZoomScroll mPositionToRestore;
    private Runnable mRestorePositionRunnable;
    /** The animation started on a double-tap, if any is currently running. */
    @Nullable
    private Animator mZoomScrollAnimation;

    private boolean mViewportInitialized;
    /** The content view. */
    private View mContentView;
    /** Client configurable settings. */
    private float mMinZoom;
    private float mMaxZoom;
    private float mInitialZoom = 1.0f;
    private int mInitialZoomMode = InitialZoomMode.CONSTANT;
    private int mFitMode = FitMode.FIT_TO_BOTH;
    private int mRotateMode = RotateMode.KEEP_SAME_ZOOM;
    private boolean mKeepFitZoomOnRotate = true;
    private int mContentResizedModeX = ContentResizedMode.KEEP_SAME_ABSOLUTE;
    private int mContentResizedModeY = ContentResizedMode.KEEP_SAME_ABSOLUTE;
    private int mContentResizedModeZoom = ContentResizedMode.KEEP_SAME_ABSOLUTE;
    private boolean mOverrideMinZoomToFit = false;
    private boolean mOverrideMaxZoomToFit = false;

    /** The last stable zoom: we only re-draw bitmaps at stable zoom (not during a gesture). */
    private float mStableZoom;
    /**
     * If set to true, suppresses {@link ZoomGestureHandler#onScale(ScaleGestureDetector)} behavior.
     */
    private boolean mAllowParentToHandleScaleEvents = false;
    private boolean mStraightenVerticalScroll;
    private boolean mInitialZoomDone = false;
    private boolean mScaleInProgress = false;
    /**
     * Padding changes require a {@link #mViewport} update. {@link #setPadding(int, int, int, int)}
     * will request a layout pass but {@link #onLayout(boolean, int, int, int, int)} will be called
     * with {@code changed} == false so we need a way to know if padding has been changed since the
     * last time we set the {@link #mViewport} and it should be updated.
     */
    private Rect mPaddingOnLastViewportUpdate;
    private PdfSelectionModel mPdfSelectionModel;
    private PointF mRestoreLookAtPoint;
    private EventCallback mEventCallback;

    /**
     * We disregard an initial zoom change for logging purposes. We do not want to treat ZoomView's
     * initial fit-to-zoom or restore-zoom change as a user zoom gesture.
     */
    private boolean mTrackedInitialZoom = false;
    private boolean mStableZoomChanged = false;

    /**
     * Represents if the document has been loaded and the contents are ready for rendering.
     */
    private boolean mDocumentLoaded = false;
    /**
     * If true, indicates that a config change has happened when recreation is disabled. This is
     * important because we will need to reset mRestoreLookAtPoint manually since
     * onSaveInstanceState() and onRestoreInstanceState(Parcelable) do not get invoked.
     */
    private boolean mConfigChangeObserved = false;

    {
        mScroller = new RelativeScroller(getContext());
        mPosition = Observables.newExposedValueWithInitialValue(new ZoomScroll(1, 0, 0, STABLE));
        mGestureTracker.setDelegateHandler(mGestureHandler);
    }

    public ZoomView(@NonNull Context context) {
        this(context, null);
    }

    public ZoomView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ZoomView, defStyle,
                defStyle);
        mMinZoom = ta.getFloat(R.styleable.ZoomView_minZoom, DEFAULT_MIN_ZOOM);
        mMaxZoom = ta.getFloat(R.styleable.ZoomView_maxZoom, DEFAULT_MAX_ZOOM);
        mSaveState = ta.getBoolean(R.styleable.ZoomView_saveState, true);

        ta.recycle();
        ViewCompat.setLayoutDirection(this, ViewCompat.LAYOUT_DIRECTION_LTR);
    }

    /**
     * Resets zoomView data for state restoration.
     */

    public void resetContents() {
        scrollTo(0, 0);

        // Reset content bounds
        mContentRawBounds = new Rect();

        // Reset gesture and scrolling states
        mOverScrollX = mOverScrollY = 0;
        mIsFling = false;
        mScroller.forceFinished(true);

        // Reset zoom and fit states
        mInitialZoomDone = false;
        mPositionToRestore = null;

        mScaleInProgress = false;
    }

    /**
     * Set the document loaded property when the service is ready to receive requests.
     */
    public void setDocumentLoaded(boolean documentLoaded) {
        mDocumentLoaded = documentLoaded;
        restoreLookAtPointIfNecessary();
    }

    /**
     * Set values of shareScrollToLeft, shareScrollToRight, shareScrollToTop and
     * shareScrollToBottom.
     */
    public void setShareScroll(boolean left, boolean right, boolean top, boolean bottom) {
        mGestureHandler.mShareScrollToLeft = left;
        mGestureHandler.mShareScrollToRight = right;
        mGestureHandler.mShareScrollToTop = top;
        mGestureHandler.mShareScrollToBottom = bottom;
    }

    /** Straighten vertical scroll - useful on tall, narrow documents. */
    public void setStraightenVerticalScroll(boolean straighten) {
        this.mStraightenVerticalScroll = straighten;
    }

    /**
     * Configure how this view fits the content into the screen. See {@link FitMode}. Default is
     * FitMode.FIT_TO_BOTH - the content is fit so both its width and height fit on screen.
     */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setFitMode(int fitMode) {
        this.mFitMode = fitMode;
        return this;
    }

    /**
     * Configure which zoom this view starts at, from a value in {@link InitialZoomMode}. Default is
     * InitialZoomMode.CONSTANT with a constant of 1, meaning 100% zoom.
     */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setInitialZoomMode(int initialZoomMode) {
        this.mInitialZoomMode = initialZoomMode;
        return this;
    }

    /**
     * Configure how the zoom changes when device rotates, from a value in {@link RotateMode}.
     * Default is RotateMode.KEEP_SAME_ZOOM - the zoom level doesn't change on rotation.
     */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setRotateMode(int rotateMode) {
        this.mRotateMode = rotateMode;
        return this;
    }

    /**
     * Configure how the viewport moves when the content changes size. Default is that the viewport
     * tracks the same point in absolute terms, before and after the change.
     */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setContentResizedMode(int contentResizedMode) {
        mContentResizedModeX = mContentResizedModeY = mContentResizedModeZoom = contentResizedMode;
        return this;
    }

    /**
     * Each axis can be configured to separately - for instance, when viewing a tall document that
     * grows as it is loaded, you might want the view to stay centered as it widens (so X mode is
     * relative) but not to lose the page as it gets taller (so Y mode is absolute).
     */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setContentResizedModeX(int contentResizedMode) {
        this.mContentResizedModeX = contentResizedMode;
        return this;
    }

    /** Set value of contentResizedModeY. */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setContentResizedModeY(int contentResizedMode) {
        this.mContentResizedModeY = contentResizedMode;
        return this;
    }

    /** Set value of contentResizedModeZoom. */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setContentResizedModeZoom(int contentResizedMode) {
        this.mContentResizedModeZoom = contentResizedMode;
        return this;
    }

    /**
     * When set to true, we keep content that exactly fit the screen before the device rotated so
     * that it still exactly fits the screen after the rotation. This might involve changing the
     * zoom by an apparently arbitrary amount, depending on the exact screen and content dimensions.
     * If the content does not currently exactly fit the screen, the RotateMode is honored.
     */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setKeepFitZoomOnRotate(boolean keepFitZoomOnRotate) {
        this.mKeepFitZoomOnRotate = keepFitZoomOnRotate;
        return this;
    }

    /** Allows zooming out further than min zoom if this is needed to fit content on screen. */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setOverrideMinZoomToFit(boolean overrideMinZoomToFit) {
        this.mOverrideMinZoomToFit = overrideMinZoomToFit;
        return this;
    }

    /** Allows zooming in further than max zoom if this is needed to make content fill screen. */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setOverrideMaxZoomToFit(boolean overrideMaxZoomToFit) {
        this.mOverrideMaxZoomToFit = overrideMaxZoomToFit;
        return this;
    }

    /** If true, parent must handle scaling/zooming gestures by calling {@link #setZoom(float)}. */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setAllowParentToHandleScaleEvents(boolean allowParentToHandleScaleEvents) {
        this.mAllowParentToHandleScaleEvents = allowParentToHandleScaleEvents;
        return this;
    }

    /** Enables/Disables double tap to zoom. */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setEnableDoubleTap(boolean doubleTapEnabled) {
        this.mDoubleTapEnabled = doubleTapEnabled;
        return this;
    }

    @Nullable
    public PdfSelectionModel getPdfSelectionModel() {
        return mPdfSelectionModel;
    }

    public void setPdfSelectionModel(@NonNull PdfSelectionModel pdfSelectionModel) {
        mPdfSelectionModel = pdfSelectionModel;
    }

    public void setMetricEventCallback(
            @Nullable EventCallback eventCallback) {
        mEventCallback = eventCallback;
    }

    /** Exposes this view's position as an observable value. */
    @NonNull
    public ObservableValue<ZoomScroll> zoomScroll() {
        return mPosition;
    }

    private void reportPosition(boolean stable) {
        reportPosition(stable, /* forceReport = */ false);
    }

    /** Reports the current position to listeners. */
    private void reportPosition(boolean stable, boolean forceReport) {
        ZoomScroll newPos = new ZoomScroll(getZoom(), getScrollX(), getScrollY(), stable);
        if (!Objects.equals(mPosition.get(), newPos) || forceReport) {
            mPosition.set(newPos);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 0) {
            setContentView(getChildAt(0));
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        // We used to reportPosition(UNSTABLE) here, but now we do it on all possible ways to scroll
        // This is true as long as we use scrollTo(x, y, stable);
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        setContentView(child);
    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();
        mContentView = null;
    }

    private void setContentView(View contentView) {
        Preconditions.checkState(
                this.mContentView == null || this.mContentView == contentView,
                "ZoomView can't take a second View");
        this.mContentView = contentView;
        contentView.setPivotX(0);
        contentView.setPivotY(0);
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec,
            int parentHeightMeasureSpec) {
        child.measure(UNBOUNDED, UNBOUNDED);
    }

    @Override
    protected void measureChildWithMargins(
            View child,
            int parentWidthMeasureSpec,
            int widthUsed,
            int parentHeightMeasureSpec,
            int heightUsed) {
        child.measure(UNBOUNDED, UNBOUNDED);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // As a result of this method, this view's position is very likely to change, and we have to
        // report that change with reportPosition(). However, as that call is likely to trigger
        // modifications to the content view (add/remove/resize...) and consequently new
        // requestLayout()
        // call(s), it must be done when this layout pass is over (i.e. posted).
        // It is therefore important that the only reportPosition() call issued during the
        // execution of
        // this method is the one posted at the end of the method.
        super.onLayout(changed, left, top, right, bottom);
        boolean hasContents = mContentView != null && mContentView.getWidth() > 0;

        boolean zoomChanged = false;

        // Need to flip this to true if we think onLayout changed the position of anything.
        boolean shouldConstrainPosition = false;

        // 1) Changes to the contents (e.g. init, new contents appended):
        // Update contentRawBounds in all cases (even when !changed) because contentView's layout
        // often changes without impacting our layout.
        if (hasContents
                && (mContentRawBounds.width() != mContentView.getWidth()
                || mContentRawBounds.height() != mContentView.getHeight())) {
            int prevWidth = mContentRawBounds.width();
            int prevHeight = mContentRawBounds.height();
            mContentRawBounds.set(0, 0, mContentView.getWidth(), mContentView.getHeight());

            if (!mViewport.isEmpty() && prevWidth > 0 && prevHeight > 0) {
                // Might need to move the viewport depending on the ContentResizedMode
                // configuration.
                float lookAtX = toContentX(mViewport.width() / 2f);
                float lookAtY = toContentY(mViewport.height() / 2f);
                if (mContentResizedModeX == ContentResizedMode.KEEP_SAME_RELATIVE) {
                    lookAtX *= mContentRawBounds.width() / ((float) prevWidth);
                }
                if (mContentResizedModeY == ContentResizedMode.KEEP_SAME_RELATIVE) {
                    lookAtY *= mContentRawBounds.height() / ((float) prevHeight);
                }
                scrollTo(
                        (int) (lookAtX * getZoom() - mViewport.width() / 2f),
                        (int) (lookAtY * getZoom() - mViewport.height() / 2f));
                if (mContentResizedModeZoom == ContentResizedMode.KEEP_SAME_RELATIVE) {
                    float newZoom =
                            getZoom()
                                    * (prevWidth + prevHeight)
                                    / (mContentRawBounds.width() + mContentRawBounds.height());
                    setZoom(newZoom);
                }

                // No need to adjust v-scroll when height changes (ie more pages are added to the
                // bottom).
                shouldConstrainPosition = true;
            }
        }

        // 2) Changes in the screen (init, rotation):
        // Update viewport; compute position under the old viewport and apply them in the new one.
        boolean paddingHasChanged =
                mPaddingOnLastViewportUpdate != null
                        && !mPaddingOnLastViewportUpdate.equals(getPaddingRect());
        if (changed || paddingHasChanged) {
            mPaddingOnLastViewportUpdate = getPaddingRect();
            if (!mViewportInitialized) { // Init
                Preconditions.checkState(mViewport.isEmpty());
                mViewport.set(
                        0,
                        0,
                        right - getPaddingRight() - getPaddingLeft() - left,
                        bottom - getPaddingBottom() - getPaddingTop() - top);
                shouldConstrainPosition = true;

                mViewportInitialized = true;
            } else { // Some other layout trigger - could be a configuration change (rotation)
                PointF lookAtPoint = computeLookAtPoint();
                int oldWidth = mViewport.width();
                int oldHeight = mViewport.height();
                boolean isFitZoom = MathUtils.almostEqual(getZoom(), getUnconstrainedZoomToFit(),
                        0.05f);

                mViewport.set(
                        0,
                        0,
                        right - getPaddingRight() - getPaddingLeft() - left,
                        bottom - getPaddingBottom() - getPaddingTop() - top);

                if (hasContents) {
                    int newWidth = mViewport.width();
                    int newHeight = mViewport.height();

                    // Possibly change the zoom,
                    // depending on keepFitZoomOnRotate or RotateMode setting.
                    if (!mInitialZoomDone) {
                        setZoom(getInitialZoom(), 0, 0);
                        mInitialZoomDone = true;
                    } else if (isFitZoom && mKeepFitZoomOnRotate) {
                        setZoom(getConstrainedZoomToFit());
                    } else if (mRotateMode == RotateMode.KEEP_SAME_VIEWPORT_WIDTH) {
                        setZoom(constrainZoom(getZoom() * newWidth / oldWidth));
                    } else if (mRotateMode == RotateMode.KEEP_SAME_VIEWPORT_HEIGHT) {
                        setZoom(constrainZoom(getZoom() * newHeight / oldHeight));
                    } else {
                        // Nothing required: keep same zoom as before.
                    }

                    zoomChanged = true;
                    restoreLookAtPointIfNecessary();

                    if (!mSaveState || mRestoreLookAtPoint == null) {
                        centerAt(lookAtPoint.x, lookAtPoint.y);
                    }
                    shouldConstrainPosition = true;


                    mPositionToRestore = new ZoomScroll(getZoom(), getScrollX(), getScrollY(),
                            true);
                }
            }
        }

        // 3) Apply the initial position: restore or fit-to-screen.
        if (hasContents && !zoomChanged) {
            if (mPositionToRestore == null) {
                // Initially fit the content to width and/or height.
                if (!mInitialZoomDone) {
                    setZoom(getInitialZoom(), 0, 0); // Stay at the top of the document.
                    shouldConstrainPosition = true;
                }
            } else if (restoreSavedPosition()) {
                shouldConstrainPosition = true;
            }
            mInitialZoomDone = true;
        }

        if (shouldConstrainPosition) {
            constrainPosition();
            // Report position needs to be posted because it may trigger requestLayout().
            ThreadUtils.postOnUiThread(() -> reportPosition(STABLE));
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mConfigChangeObserved = true;
    }

    private void restoreLookAtPointIfNecessary() {
        if (mSaveState && mRestoreLookAtPoint != null) {
            if (mDocumentLoaded) {
                centerAt(mRestoreLookAtPoint.x, mRestoreLookAtPoint.y, /* forceReport= */ true);
                if (mConfigChangeObserved) {
                    mRestoreLookAtPoint = null;
                }
            }
        }
    }

    /**
     *
     */
    public float getInitialZoom() {
        switch (mInitialZoomMode) {
            case InitialZoomMode.ZOOM_TO_FIT:
                return getConstrainedZoomToFit();
            case InitialZoomMode.MIN_ZOOM:
                return getMinZoom();
            case InitialZoomMode.MAX_ZOOM:
                return getMaxZoom();
            case InitialZoomMode.CONSTANT:
            default:
                return constrainZoom(mInitialZoom);
        }
    }

    /** Configure which zoom this view starts at. Default is 1, meaning 100% zoom. */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setInitialZoom(float initialZoom) {
        setInitialZoomMode(InitialZoomMode.CONSTANT);
        this.mInitialZoom = initialZoom;
        return this;
    }

    /** Get minimum zoom. */
    public float getMinZoom() {
        if (mOverrideMinZoomToFit) {
            return Math.min(mMinZoom, getUnconstrainedZoomToFit());
        }
        return mMinZoom;
    }

    /** Set the minimum zoom - also configurable in XML. Returns this. */
    @NonNull
    @CanIgnoreReturnValue
    public ZoomView setMinZoom(float minZoom) {
        this.mMinZoom = minZoom;
        return this;
    }

    /**
     *
     */
    public float getMaxZoom() {
        if (mOverrideMaxZoomToFit) {
            return Math.max(mMaxZoom, getUnconstrainedZoomToFit());
        }
        return mMaxZoom;
    }

    /** Set the maximum zoom - also configurable in XML. Returns this. */
    public void setMaxZoom(float maxZoom) {
        this.mMaxZoom = maxZoom;
    }

    public boolean getIsInitialZoomDone() {
        return mInitialZoomDone;
    }

    public float getStableZoom() {
        return mStableZoom;
    }

    public void setStableZoom(float stableZoom) {
        this.mStableZoom = stableZoom;
    }

    private float getConstrainedZoomToFit() {
        return constrainZoom(getUnconstrainedZoomToFit());
    }

    private float getUnconstrainedZoomToFit() {
        float widthZoom = (float) mViewport.width() / mContentView.getWidth();
        float heightZoom = (float) mViewport.height() / mContentView.getHeight();
        switch (mFitMode) {
            case FitMode.FIT_TO_BOTH:
                return Math.min(widthZoom, heightZoom);
            case FitMode.FIT_TO_HEIGHT:
                return heightZoom;
            case FitMode.FIT_TO_WIDTH:
            default:
                return widthZoom;
        }
    }

    /**
     * Given a potential new zoom, return the nearest valid zoom, constrained within the configured
     * min- and max- zoom.
     */
    private float constrainZoom(float newZoom) {
        return MathUtils.clamp(newZoom, getMinZoom(), getMaxZoom());
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            // Fling does not support overscroll. It will simply not scroll past an edge.
            mScroller.apply(this);
            reportPosition(UNSTABLE);

            // Need to invalidate even when delta == 0 to let the scroller properly finish.
            invalidate();
        } else if (mIsFling) {
            reportPosition(STABLE);
            mIsFling = false;
        }
    }

    /** The width of the usable area of this view - total width less padding. */
    public int getViewportWidth() {
        return mViewport.width();
    }

    /** The height of the usable area of this view - total height less padding. */
    public int getViewportHeight() {
        return mViewport.height();
    }

    /**
     * Computes the part of the content visible within the inner part of this view (minus this
     * view's padding) in co-ordinates of the content. The result is relative to
     * (0, 0)-(rawContentWidth, rawContentHeight).
     */
    @NonNull
    public Rect getUsableAreaInContentCoords() {
        return new Rect(
                (int) toContentX(0),
                (int) toContentY(0),
                (int) toContentX(getViewportWidth()),
                (int) toContentY(getViewportHeight()));
    }

    /**
     * Computes the part of the content visible within the outer part of this view (including this
     * view's padding) in co-ordinates of the content. The result is relative to (0,
     * 0)-(rawContentWidth, rawContentHeight).
     */
    @NonNull
    public Rect getVisibleAreaInContentCoords() {
        return new Rect(
                (int) toContentX(-getPaddingLeft()),
                (int) toContentY(-getPaddingTop()),
                (int) toContentX(getViewportWidth() + getPaddingRight()),
                (int) toContentY(getViewportHeight() + getPaddingBottom()));
    }

    /**
     * Centers the view at the given point, or as close to centered as is reasonable. The point is
     * given in the co-ordinate system of the content, before scaling is applied by the zoom factor.
     */
    public void centerAt(float x, float y) {
        centerAt(x, y, /* forceReport = */ false);
    }

    private void centerAt(float x, float y, boolean forceReport) {
        float zoom = getZoom();
        int left = (int) (x * zoom - mViewport.width() / 2f);
        int top = (int) (y * zoom - mViewport.height() / 2f);

        scrollTo(left, top);
        constrainPosition();
        reportPosition(STABLE, forceReport);
    }

    /**
     * Animates a zoom/scroll operation which will result in the given point being centered, or as
     * close to centered as is reasonable, at the new zoom level. The point is given in the
     * co-ordinate system of the content, before scaling is applied by the zoom factor.
     */
    public void zoomAndCenterAtAnimated(
            float x, float y, float newZoom,
            @NonNull ValueAnimator.AnimatorUpdateListener updateListener) {
        if (newZoom > getMaxZoom() || newZoom < getMinZoom()) {
            return;
        }
        float left = x * newZoom - mViewport.width() / 2f;
        float top = y * newZoom - mViewport.height() / 2f;
        left += ZoomUtils.constrainCoordinate(newZoom, (int) left, mContentRawBounds.width(),
                mViewport.width());
        top += ZoomUtils.constrainCoordinate(newZoom, (int) top, mContentRawBounds.height(),
                mViewport.height());
        zoomScrollAnimated(left, top, newZoom, updateListener);
    }

    private void zoomScrollAnimated(
            float x, float y, float newZoom, ValueAnimator.AnimatorUpdateListener updateListener) {
        zoomScrollAnimated(
                x,
                y,
                newZoom,
                updateListener,
                new AccelerateInterpolator());
    }

    private void zoomScrollAnimated(
            float x,
            float y,
            float newZoom,
            ValueAnimator.AnimatorUpdateListener updateListener,
            TimeInterpolator interpolator) {
        if (mZoomScrollAnimation != null) {
            mZoomScrollAnimation.cancel();
        }
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleXAnimator = new ObjectAnimator();
        scaleXAnimator.setTarget(mContentView);
        scaleXAnimator.setPropertyName("scaleX");
        scaleXAnimator.setFloatValues(getZoom(), newZoom);
        ObjectAnimator scaleYAnimator = new ObjectAnimator();
        scaleYAnimator.setTarget(mContentView);
        scaleYAnimator.setPropertyName("scaleY");
        scaleYAnimator.setFloatValues(getZoom(), newZoom);
        scaleYAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (updateListener != null) {
                            updateListener.onAnimationUpdate(animation);
                        }
                    }
                });
        animatorSet.playTogether(
                ObjectAnimator.ofInt(ZoomView.this, "scrollX", getScrollX(), (int) x),
                ObjectAnimator.ofInt(ZoomView.this, "scrollY", getScrollY(), (int) y),
                scaleXAnimator,
                scaleYAnimator
                // It's important for scaleYAnimator to be last, since it has the listener.
        );
        int themeDuration = FALLBACK_ZOOM_ANIMATION_DURATION_MS;
        try {
            themeDuration = MotionUtils.resolveThemeDuration(
                    getContext(), com.google.android.material.R.attr.motionDurationMedium1,
                    FALLBACK_ZOOM_ANIMATION_DURATION_MS);
        } catch (NoClassDefFoundError ignored) {
            // Material resources not present in host app, fallback to default
        }
        animatorSet.setDuration(themeDuration);
        animatorSet.setInterpolator(interpolator);
        animatorSet.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mZoomScrollAnimation = null;
                        constrainPosition();
                        reportPosition(STABLE);
                    }
                });
        mZoomScrollAnimation = animatorSet;
        animatorSet.start();
    }

    /** Scroll to the given position. */
    public void scrollTo(int left, int top, boolean stable) {
        scrollTo(left, top);
        constrainPosition();
        reportPosition(stable);
    }

    public float getZoom() {
        return mContentView != null ? mContentView.getScaleX() : 1.f;
    }

    /** Set the zoom. Zooms in or out of the center of the zoomview. */
    public void setZoom(float zoom) {
        int pivotX = mViewport.width() / 2;
        int pivotY = mViewport.height() / 2;
        setZoom(zoom, pivotX, pivotY);
    }

    /** Set the zoom, using the given point as a pivot point to zoom in or out of. */
    public void setZoom(float zoom, float pivotX, float pivotY) {
        zoom = Float.isNaN(zoom) ? ZOOM_RESET : zoom;
        mInitialZoomDone = true;
        int deltaX = ZoomUtils.scrollDeltaNeededForZoomChange(getZoom(), zoom, pivotX,
                getScrollX());
        int deltaY = ZoomUtils.scrollDeltaNeededForZoomChange(getZoom(), zoom, pivotY,
                getScrollY());

        mContentView.setScaleX(zoom);
        mContentView.setScaleY(zoom);
        scrollBy(deltaX, deltaY);
    }

    /**
     * Loads and refreshes page assets based on the current zoom and scroll state.
     */
    public void loadPageAssets(@NonNull LayoutHandler layoutHandler,
            @Nullable ObservableValue<ViewState> viewState) {

        PaginatedView paginatedView = this.findViewById(R.id.pdf_view);
        ZoomScroll position = this.zoomScroll().get();
        if (position == null || !paginatedView.getModel().isInitialized()) {
            return;
        }
        Range prevVisiblePages = paginatedView.getPageRangeHandler().getVisiblePages();
        boolean isInitialDocumentLoad = false;
        // If visiblePages is equal to null, this is the first time we're requesting page assets.
        if (prevVisiblePages == null) {
            prevVisiblePages = new Range();
            isInitialDocumentLoad = true;
        }

        paginatedView.setViewArea(this.getVisibleAreaInContentCoords());
        paginatedView.refreshPageRangeInVisibleArea(position, this.getHeight());

        mStableZoomChanged = false;
        // Change the resolution of the bitmaps only when a gesture is not in progress.
        // Only update zoom if the gesture is stable (not in progress) or if the stable zoom is not
        // yet set.
        if (position.stable || this.getStableZoom() == 0f) {
            if (this.getStableZoom() != position.zoom) {
                handleStableZoomChange(position.zoom);
            }
        }

        // Did any new pages become visible?
        // On the first load, treat all visible pages as newly visible. Otherwise, discount pages
        // that were visible previously
        Range visiblePages = paginatedView.getPageRangeHandler().getVisiblePages();
        Range newlyVisiblePages = isInitialDocumentLoad
                ? visiblePages
                : GestureRecordingProcessor.Companion
                        .getNewlyVisiblePages(visiblePages, prevVisiblePages);
        // We're loading new assets on scroll when:
        // * New pages are visible AND
        // * Scroll position has settled OR we're actively scrolling at a stable zoom level
        boolean loadingNewAssetsOnScroll = GestureRecordingProcessor.Companion
                .loadingNewAssetsOnScroll(newlyVisiblePages, position, this.getStableZoom());
        PositionState scrollPositionState = new PositionState(
                loadingNewAssetsOnScroll ? EventState.NEW_ASSETS_LOADED : EventState.NO_EVENT,
                newlyVisiblePages);
        PositionState zoomPositionState = new PositionState(
                mStableZoomChanged ? EventState.ZOOM_CHANGED : EventState.NO_EVENT,
                paginatedView.getPageRangeHandler().getVisiblePages());

        // Start Event Tracking based upon current vs new state, after gesture is completed.
        GestureRecordingProcessor gestureRecordingProcessor =
                new GestureRecordingProcessor(mEventCallback, mStableZoom);
        gestureRecordingProcessor.processNewPositionState(scrollPositionState, zoomPositionState);

        paginatedView.handleGonePages(false);
        paginatedView.loadInvisibleNearPageRange(this.getStableZoom());


        if (mDocumentLoaded) {
            // The step (4) below requires page Views to be created and laid out.
            // So we create them here and set this flag
            // if that operation needs to wait for a layout pass.
            boolean requiresLayoutPass = paginatedView.createPageViewsForVisiblePageRange();

            // 4. Refresh tiles and/or full pages.
            if (position.stable) {
                if (viewState != null) {
                    // Perform a full refresh on all visible pages
                    ViewState currentViewState = viewState.get();
                    if (currentViewState != null) {
                        paginatedView.refreshVisiblePages(
                                requiresLayoutPass, currentViewState, this.getStableZoom());
                    }
                }
                paginatedView.handleGonePages(true);
            } else if (this.getStableZoom() == position.zoom) {
                // Just load a few more tiles in case of tile-scroll
                ViewState currentViewState = viewState.get();
                if (currentViewState != null) {
                    paginatedView.refreshVisibleTiles(requiresLayoutPass, currentViewState);
                }
            }

            if (paginatedView.getPageRangeHandler().getVisiblePages() != null) {
                layoutHandler.maybeLayoutPages(
                        paginatedView.getPageRangeHandler().getVisiblePages().getLast()
                );
            }
        }
    }

    private void handleStableZoomChange(float newZoom) {
        // If the stable zoom has already been set (not the initial setting)
        if (this.getStableZoom() != 0f) {
            markStableZoomChangeIfNeeded();
        }
        // Update the stable zoom level to the current position's zoom
        this.setStableZoom(newZoom);
    }

    private void markStableZoomChangeIfNeeded() {
        if (mTrackedInitialZoom) {
            // Mark that a stable zoom change has occurred
            mStableZoomChanged = true;
        }
        // Mark that we've now tracked the initial zoom change
        mTrackedInitialZoom = true;
    }

    /**
     * Given a point in the zoom-view's co-ordinates, convert it to the content's co-ordinates,
     * using the current zoom and scroll position of the zoomview.
     */
    protected float toContentX(float zoomViewX) {
        return ZoomUtils.toContentCoordinate(zoomViewX, getZoom(), getScrollX());
    }

    protected float toContentY(float zoomViewY) {
        return ZoomUtils.toContentCoordinate(zoomViewY, getZoom(), getScrollY());
    }

    /**
     * Given a point in the content's co-ordinates, convert it to the zoom-view's co-ordinates,
     * using the current zoom and scroll position of the zoomview.
     */
    protected float toZoomViewX(float contentX) {
        return ZoomUtils.toZoomViewCoordinate(contentX, getZoom(), getScrollX());
    }

    protected float toZoomViewY(float contentY) {
        return ZoomUtils.toZoomViewCoordinate(contentY, getZoom(), getScrollY());
    }

    /**
     * Adjust the content view's position (by scrolling) in order to:
     *
     * <ul>
     *   <li>eliminate dead margins where the content is larger than the viewport,
     *   <li>keep the content centered when it is smaller than the viewport.
     * </ul>
     *
     * <p>As a side-effect, sets the values {@link #mOverScrollX} and {@link #mOverScrollY} to how
     * much
     * scrolling had happened out of the bounds that needed to be adjusted for.
     *
     * @return True if the position had to be adjusted, false if untouched.
     */
    @CanIgnoreReturnValue
    private boolean constrainPosition() {
        int dx = constrainX();
        int dy = constrainY();
        mOverScrollX -= dx;
        mOverScrollY -= dy;

        if (dx != 0 || dy != 0) {
            scrollBy(dx, dy);
            return true;
        } else {
            return false;
        }
    }

    private int constrainX() {
        return ZoomUtils.constrainCoordinate(getZoom(), getScrollX(), mContentRawBounds.width(),
                mViewport.width());
    }

    private int constrainY() {
        return ZoomUtils.constrainCoordinate(getZoom(), getScrollY(), mContentRawBounds.height(),
                mViewport.height());
    }

    /**
     * Find the point that is being looked at - to keep looking it at if the viewport changes size.
     */
    private PointF computeLookAtPoint() {
        float lookAtX = toContentX(mViewport.width() / 2f);
        float lookAtY = toContentY(mViewport.height() / 2f);
        if (getScrollY() <= 0) {
            lookAtY = 0; // Stick to the top of the document if the layout changes while there.
        }

        return new PointF(lookAtX, lookAtY);
    }

    /**
     * The position and bounds of the contentView relative to this view's origin, including any
     * transformation (zoom) and scrolling.
     */
    private Rect contentPosition() {
        Rect contentPosition =
                new Rect(
                        (int) toZoomViewX(0), (int) toZoomViewY(0),
                        (int) toZoomViewX(mContentView.getWidth()),
                        (int) toZoomViewY(mContentView.getHeight()));
        return contentPosition;
    }

    @NonNull
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_SUPER, super.onSaveInstanceState());
        if (mSaveState) {
            bundle.putBundle(KEY_POSITION, mPosition.get().asBundle());
            bundle.putBoolean(KEY_VIEWPORT_INITIALIZED, mViewportInitialized);
            bundle.putBoolean(KEY_INITIAL_ZOOM_DONE, mInitialZoomDone);
            bundle.putParcelable(KEY_VIEWPORT, mViewport);
            bundle.putFloat(KEY_CONTENT_ZOOM, mContentView.getScaleX());
            bundle.putParcelable(KEY_RAW_BOUNDS, mContentRawBounds);
            bundle.putParcelable(KEY_PADDING, mPaddingOnLastViewportUpdate);
            bundle.putParcelable(KEY_LOOKAT_POINT, computeLookAtPoint());
            bundle.putBoolean(KEY_DOCUMENT_LOADED, mDocumentLoaded);
        }
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        super.onRestoreInstanceState(bundle.getParcelable(KEY_SUPER));
        if (mSaveState) {
            Bundle positionBundle = bundle.getBundle(KEY_POSITION);
            assert positionBundle != null;
            mPositionToRestore = ZoomScroll.fromBundle(positionBundle);
            mViewportInitialized = bundle.getBoolean(KEY_VIEWPORT_INITIALIZED);
            mInitialZoomDone = bundle.getBoolean(KEY_INITIAL_ZOOM_DONE);
            mViewport.set(Objects.requireNonNull(bundle.getParcelable(KEY_VIEWPORT)));
            mContentView.setScaleX(bundle.getFloat(KEY_CONTENT_ZOOM));
            mContentRawBounds.set(Objects.requireNonNull(bundle.getParcelable(KEY_RAW_BOUNDS)));
            mPaddingOnLastViewportUpdate = bundle.getParcelable(KEY_PADDING);
            mRestoreLookAtPoint = bundle.getParcelable(KEY_LOOKAT_POINT);
            mDocumentLoaded = bundle.getBoolean(KEY_DOCUMENT_LOADED);
        }
    }

    /**
     * Restores the state to the saved position, or as close as it can. This can be done immediately
     * or delayed a bit (after this method returns). This method will call {@link #reportPosition}
     * only in case it's executing a delayed restore; if it's applying a restored position right
     * away, it won't call it.
     *
     * @return true if a saved position was applied right now (which needs to be reported).
     */
    private boolean restoreSavedPosition() {
        if (mPositionToRestore == null) {
            return false;
        }

        // availableHeight will keep increasing with successive calls of this method. If we can't
        // restore now, we wait for the next call.
        int availableHeight = (int) (mContentRawBounds.height() * mPositionToRestore.zoom);
        if (mRestorePositionRunnable != null) {
            mHandler.removeCallbacks(mRestorePositionRunnable);
            mRestorePositionRunnable = null;
        }

        if (availableHeight < mPositionToRestore.scrollY) {
            // Not enough height to restore now.
            return false;
        } else if (availableHeight < mPositionToRestore.scrollY + mViewport.height()) {
            // This is not a perfect restore, but might be the last call to this method, so we'll
            // never have a perfect restore. Schedule this restore for that case (and we'll cancel
            // it on the next method call if that happens).
            mRestorePositionRunnable =
                    () -> {
                        restorePosition();
                        reportPosition(STABLE);
                    };
            mHandler.postDelayed(mRestorePositionRunnable, 200);
            return false;
        } else {
            // That's a perfect restore, do it.
            restorePosition();
            return true;
        }
    }

    /** Restores the given position and reports it. */
    private void restorePosition() {
        ZoomScroll restore = mPositionToRestore;
        mPositionToRestore = null;
        mRestorePositionRunnable = null;
        if (restore == null) {
            return;
        }

        setZoom(restore.zoom);
        scrollTo(restore.scrollX, restore.scrollY);
        constrainPosition();
        mPosition.set(restore);
    }

    /** Attempt to restore to the given {@link ZoomScroll} as faithfully as possible. */
    @Override
    public void attemptRestorePosition(@NonNull ZoomScroll position) {
        mPositionToRestore = position;
        requestLayout();
    }

    @Override
    protected boolean interceptGesture(@NonNull GestureTracker gestureTracker) {
        if (gestureTracker.matches(GestureTracker.Gesture.DOUBLE_TAP) && mDoubleTapEnabled) {
            // We have to send a CANCEL to any child that might track this gesture and would
            // otherwise think it was a single tap.
            final long now = SystemClock.uptimeMillis();
            MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f,
                    0);
            // NOTE: This works only with immediate child, not further down.
            getChildAt(0).dispatchTouchEvent(event);
            return true;
        }
        return gestureTracker.matches(GestureTracker.Gesture.DRAG, GestureTracker.Gesture.DRAG_X,
                GestureTracker.Gesture.DRAG_Y, GestureTracker.Gesture.ZOOM)
                || (mIsFling && gestureTracker.matches(GestureTracker.Gesture.TOUCH));
    }

    /** Returns current padding in a Rect for easy comparison. */
    private Rect getPaddingRect() {
        return new Rect(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
    }

    /** Different options for the initial zoom that this ZoomView should start with. */
    public static final class InitialZoomMode {
        /** Start with a constant initial zoom, as specified by {@link #setInitialZoom(float)}. */
        public static final int CONSTANT = 0;
        /** Start with a zoom that fits the content on the screen. */
        public static final int ZOOM_TO_FIT = 1;
        /** Start zoomed out as far as possible. */
        public static final int MIN_ZOOM = 2;
        /** Start zoomed in as far as possible. */
        public static final int MAX_ZOOM = 3;
    }

    /** Different options for how this ZoomView should fit the content to the screen. */
    public static final class FitMode {
        /** Fit content to screen by fitting the entire width of the content on screen. */
        public static final int FIT_TO_WIDTH = 0;
        /** Fit content to screen by fitting the entire width of the content on screen. */
        public static final int FIT_TO_HEIGHT = 1;
        /** Fit entire content to screen by fitting both its width and height on screen. */
        public static final int FIT_TO_BOTH = 2;
    }

    /** Different options for how the zoom should change or not change when screen rotates. */
    public static final class RotateMode {
        /** After device is rotated, keep the same zoom level as before the rotation. */
        public static final int KEEP_SAME_ZOOM = 0;
        /** When rotated, keep the same content visible between the left and right screen edges. */
        public static final int KEEP_SAME_VIEWPORT_WIDTH = 1;
        /** When rotated, keep the same content visible between the top and bottom screen edges. */
        public static final int KEEP_SAME_VIEWPORT_HEIGHT = 2;
    }

    /** Different options for how the viewport should move when the content changes size. */
    public static final class ContentResizedMode {
        /** When content resizes, keep looking at the same absolute co-ordinates eg (10px, 20px). */
        public static final int KEEP_SAME_ABSOLUTE = 0;
        /** When content resizes, keep looking at the same relative position eg (25%, 50%). */
        public static final int KEEP_SAME_RELATIVE = 1;
    }

    /**
     * The position (zoom and scroll) of the underlying view. Also indicates whether this position
     * is stable (i.e. not in the middle of a user gesture).
     *
     * <p>An immutable object.
     */
    public static final class ZoomScroll {

        private static final String KEY_SCROLL_X = "sx";
        private static final String KEY_SCROLL_Y = "sy";
        private static final String KEY_ZOOM = "z";

        public final float zoom;
        public final int scrollX;
        public final int scrollY;
        public final boolean stable;

        public ZoomScroll(float zoom, int scrollX, int scrollY, boolean stable) {
            this.zoom = zoom;
            this.scrollX = scrollX;
            this.scrollY = scrollY;
            this.stable = stable;
        }

        /** Get ZoomScroll position from bundle. */
        @NonNull
        public static ZoomScroll fromBundle(@androidx.annotation.NonNull Bundle bundle) {
            ZoomScroll position =
                    new ZoomScroll(
                            bundle.getFloat(KEY_ZOOM),
                            bundle.getInt(KEY_SCROLL_X),
                            bundle.getInt(KEY_SCROLL_Y),
                            STABLE);
            return position;
        }

        @Override
        public String toString() {
            return String.format(
                    Locale.US, "Position: zoom: %.2f; scroll: %d, %d; ", zoom, scrollX, scrollY)
                    + (stable ? "(stable)" : "(transient)");
        }

        /** Save and return ZoomScroll position in a Bundle object. */
        @NonNull
        public Bundle asBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_SCROLL_X, scrollX);
            bundle.putInt(KEY_SCROLL_Y, scrollY);
            bundle.putFloat(KEY_ZOOM, zoom);
            return bundle;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ZoomScroll that = (ZoomScroll) obj;
            return this.scrollX == that.scrollX
                    && this.scrollY == that.scrollY
                    && this.stable == that.stable
                    && Float.floatToIntBits(this.zoom) == Float.floatToIntBits(that.zoom);
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + scrollX;
            result = 31 * result + scrollY;
            result = 31 * result + (stable ? 1231 : 1237);
            result = 31 * result + Float.floatToIntBits(zoom);
            return result;
        }
    }

    /**
     * This Scroller applies relative changes to the View's scroll position ({@link View#scrollBy}),
     * so that it doesn't override any scroll change coming from another source (e.g. a
     * configuration change's layout pass).
     */
    private static class RelativeScroller extends OverScroller {
        private int mPrevX;
        private int mPrevY;

        RelativeScroller(Context ctx) {
            super(ctx);
        }

        private void reset() {
            mPrevX = mPrevY = 0;
        }

        @Override
        public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX,
                int minY, int maxY) {
            reset();
            super.fling(0, 0, velocityX, velocityY, minX - startX, maxX - startX, minY - startY,
                    maxY - startY);
        }

        @Override
        public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX,
                int minY, int maxY, int overX, int overY) {
            reset();
            super.fling(0, 0, velocityX, velocityY, minX - startX, maxX - startX, minY - startY,
                    maxY - startY, overX, overY);
        }

        public void apply(View v) {
            int x = getCurrX() - mPrevX;
            int y = getCurrY() - mPrevY;
            v.scrollBy(x, y);
            mPrevX = getCurrX();
            mPrevY = getCurrY();
        }
    }

    /** Gesture handler. */
    @VisibleForTesting
    protected final class ZoomGestureHandler extends GestureTracker.GestureHandler {

        /** The ratio of vertical to horizontal scroll that is assumed to be vertical only. */
        private static final float SCROLL_CORRECTION_RATIO = 1.5f;

        private static final int MAX_SCROLL_WINDOW_DP = 70;
        private static final int MIN_SCROLL_TO_SWITCH_DP = 30;
        /**
         * The multiplier to convert from a scale gesture's delta span, in pixels, to scale factor.
         *
         * <p>The Android framework returns scale factors proportional to the ratio of {@code
         * currentSpan / prevSpan}. This is problematic because it results in scale factors that are
         * very large for small pixel spans, which is particularly problematic for quickScale
         * gestures, where the span pixel values can be small, but the ratio can yield very large
         * scale factors.
         *
         * <p>Instead, we use this to ensure that pinching or quick scale dragging a certain
         * number of pixels always corresponds to a certain change in zoom. The equation that we've
         * found to work well is a delta span of the larger screen dimension should result in a
         * zoom change of 2x.
         */
        @VisibleForTesting
        protected final float mLinearScaleSpanMultiplier;
        /** The maximum scroll distance used to determine if the direction is vertical. */
        private final int mMaxScrollWindow;
        /** The smallest scroll distance that can switch mode to "free scrolling". */
        private final int mMinScrollToSwitch;
        /** Remember recent scroll events so we can examine the general direction. */
        private final Queue<PointF> mScrollQueue = new LinkedList<>();
        private boolean mShareScrollToLeft = true;
        private boolean mShareScrollToRight = true;
        private boolean mShareScrollToTop = false;
        private boolean mShareScrollToBottom = false;
        /** Are we correcting vertical scroll for the current gesture. */
        private boolean mStraightenCurrentVerticalScroll;
        private float mTotalX;
        private float mTotalY;

        protected ZoomGestureHandler(Screen screen) {
            mMaxScrollWindow = screen.pxFromDp(MAX_SCROLL_WINDOW_DP);
            mMinScrollToSwitch = screen.pxFromDp(MIN_SCROLL_TO_SWITCH_DP);
            int largestScreenDimensionPx = Math.max(screen.getHeightPx(), screen.getWidthPx());
            // A delta span of the screen dimension should correspond to a zoom of 2x.
            mLinearScaleSpanMultiplier = LINEAR_SCALE_ZOOM_FACTOR / largestScreenDimensionPx;
        }

        @Override
        protected void onGestureStart() {
            mOverScrollX = mOverScrollY = 0;
            mScroller.forceFinished(true);
        }

        @Override
        @CanIgnoreReturnValue
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            int dx = Math.round(distanceX);
            int dy = Math.round(distanceY);

            if (mStraightenCurrentVerticalScroll) {
                // Remember a window of recent scroll events.
                mScrollQueue.offer(new PointF(distanceX, distanceY));
                mTotalX += distanceX;
                mTotalY += distanceY;

                // Only consider scroll direction for a certain window of scroll events.
                while (totalScrollLength() > mMaxScrollWindow && mScrollQueue.size() > 1) {
                    // Remove the oldest scroll event - it is too far away to determine scroll
                    // direction.
                    PointF oldest = mScrollQueue.poll();
                    mTotalY -= oldest.y;
                    mTotalX -= oldest.x;
                }

                if (totalScrollLength() > mMinScrollToSwitch
                        && Math.abs(mTotalY / mTotalX) < SCROLL_CORRECTION_RATIO) {
                    mStraightenCurrentVerticalScroll = false;
                } else {
                    // Ignore the horizontal component of the scroll.
                    dx = 0;
                }
            }

            scrollBy(dx, dy);
            constrainPosition();
            reportPosition(UNSTABLE);

            // For a pure scroll gesture (no zoom), if it reaches somewhat past an edge, pass the
            // gesture on to the parents.
            if (!mGestureTracker.matches(GestureTracker.Gesture.ZOOM)) {
                int axis =
                        Math.abs(distanceX) > Math.abs(distanceY) ? MotionEvent.AXIS_X
                                : MotionEvent.AXIS_Y;
                if (shareOverscroll(axis, mOverScrollX, mOverScrollY)) {
                    // Scrolling past the bounds, so let the parent handle further scrolling if
                    // they want to.
                    releaseGesture();
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (mAllowParentToHandleScaleEvents
                    || ((mMinZoom == mMaxZoom) && (!mOverrideMinZoomToFit
                    && !mOverrideMaxZoomToFit))) {
                releaseGesture();
                return false;
            }
            mScaleInProgress = true;
            float rawScaleFactor = detector.getScaleFactor();
            float deltaSpan = Math.abs(detector.getCurrentSpan() - detector.getPreviousSpan());
            float scaleDelta = deltaSpan * mLinearScaleSpanMultiplier;
            float linearScaleFactor = rawScaleFactor >= 1.F ? 1.F + scaleDelta : 1.F - scaleDelta;
            float zoom = getZoom();
            float newZoom = constrainZoom(zoom * linearScaleFactor);

            if (newZoom != zoom) {
                setZoom(newZoom, detector.getFocusX(), detector.getFocusY());
                constrainPosition();
                reportPosition(UNSTABLE);
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
            mScaleInProgress = false;
        }

        @Override
        protected void onGestureEnd(GestureTracker.Gesture gesture) {
            switch (gesture) {
                case ZOOM:
                    constrainPosition();
                    reportPosition(STABLE);
                    break;
                case DRAG:
                case DRAG_Y:
                case DRAG_X:
                    reportPosition(STABLE);
                    break;
                default:
                    // Double-tap is reported at the end of the animation. Nothing else. Keeps
                    // warning at bay.
            }
            mTotalX = 0;
            mTotalY = 0;
            mStraightenCurrentVerticalScroll = mStraightenVerticalScroll;
            mScrollQueue.clear();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // Assume a fling in a roughly vertical direction was meant to be exactly vertical.
            if (velocityY / velocityX > SCROLL_CORRECTION_RATIO) {
                velocityX = 0;
            }

            if (mScaleInProgress) {
                // This fling is the end of a quick scale gesture. (Note, for pinch scale gestures,
                // scaleInProgress is set to false before onFling is called).
                float currentZoom = getZoom();
                int oldScrollX = getScrollX();
                int oldScrollY = getScrollY();
                float maxFlingVelocity =
                        ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();
                float velocityPercentY = velocityY / maxFlingVelocity;
                float zoomRange = getMaxZoom() - getMinZoom();
                float newZoom = constrainZoom(currentZoom + (velocityPercentY * zoomRange / 4.F));

                int newScrollX = oldScrollX;
                int newScrollY = oldScrollY;
                // Adjust new scroll positions due to changed zoom.
                newScrollX += ZoomUtils.scrollDeltaNeededForZoomChange(currentZoom, newZoom,
                        e1.getX(),
                        oldScrollX);
                newScrollY += ZoomUtils.scrollDeltaNeededForZoomChange(currentZoom, newZoom,
                        e1.getY(),
                        oldScrollY);

                // Constrain new scroll positions.
                newScrollX += ZoomUtils.constrainCoordinate(newZoom, newScrollX,
                        mContentRawBounds.width(),
                        mViewport.width());
                newScrollY += ZoomUtils.constrainCoordinate(newZoom, newScrollY,
                        mContentRawBounds.height(),
                        mViewport.height());

                zoomScrollAnimated(
                        newScrollX,
                        newScrollY,
                        newZoom,
                        null /* updateListener */,
                        new DecelerateInterpolator());
                return true;
            }

            int x = getScrollX();
            int y = getScrollY();
            Rect content = contentPosition();
            if (mViewport.contains(content)) {
                // content fits into the viewport: pan/fling are disabled.
                return true;
            }

            int minX;
            int maxX;
            int minY;
            int maxY;
            if (content.width() < mViewport.width()) {
                minX = maxX = (content.width() - mViewport.width()) / 2;
            } else {
                minX = 0;
                maxX = Math.max(0, content.width() - mViewport.width());
            }

            if (content.height() < mViewport.height()) {
                minY = maxY = (content.height() - mViewport.height()) / 2;
            } else {
                minY = 0;
                maxY = Math.max(0, content.height() - mViewport.height());
            }

            mIsFling = true;
            mScroller.fling(x, y, (int) -velocityX, (int) -velocityY, minX, maxX, minY, maxY);
            invalidate();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!mDoubleTapEnabled || mMinZoom == mMaxZoom) {
                releaseGesture();
                return false;
            }
            // Cycles between fit-to-width and last saved zoom.
            float fitZoom = getUnconstrainedZoomToFit();
            if (fitZoom == 0 || Float.isInfinite(fitZoom) || Float.isNaN(fitZoom)) {
                // viewport not initialized yet maybe?
                return false;
            }

            // The animation to change zoom states requires start and end values for zoom & scroll.
            float currentZoom = getZoom();
            int oldScrollX = getScrollX();
            int oldScrollY = getScrollY();

            float newZoom = constrainZoom(fitZoom);
            // Using 0.25f as the epsilons mean that double tapping is more likely to make a big
            // change the user doesn't want to go to the effort to double tap for it to change zoom
            // by 1%.
            if (MathUtils.almostEqual(newZoom, currentZoom, DOUBLE_TAP_ZOOM_EPSILON)) {
                newZoom = constrainZoom(newZoom * 2);
            }
            if (MathUtils.almostEqual(newZoom, currentZoom, DOUBLE_TAP_ZOOM_EPSILON)) {
                newZoom = constrainZoom(1);
            }

            int newScrollX = oldScrollX;
            int newScrollY = oldScrollY;
            // Adjust new scroll positions due to changed zoom.
            newScrollX += ZoomUtils.scrollDeltaNeededForZoomChange(currentZoom, newZoom, e.getX(),
                    oldScrollX);
            newScrollY += ZoomUtils.scrollDeltaNeededForZoomChange(currentZoom, newZoom, e.getY(),
                    oldScrollY);

            // Constrain new scroll positions.
            newScrollX += ZoomUtils.constrainCoordinate(newZoom, newScrollX,
                    mContentRawBounds.width(),
                    mViewport.width());
            newScrollY += ZoomUtils.constrainCoordinate(newZoom, newScrollY,
                    mContentRawBounds.height(),
                    mViewport.height());

            zoomScrollAnimated(newScrollX, newScrollY, newZoom, null /* updateListener */);
            return true;
        }


        private float totalScrollLength() {
            // Do not need accuracy of correct hypotenuse calculation.
            return Math.abs(mTotalY) + Math.abs(mTotalX);
        }

        /**
         * Returns whether we have to share the current scrolling gesture with our parent view. This
         * happens only when the gesture is mostly on the same axis as the considered overscroll.
         *
         * @param axis        the main direction of the current move: one of
         *                    {@link MotionEvent#AXIS_X} or
         *                    {@link MotionEvent#AXIS_Y}
         * @param overScrollX the value in pixels that the gesture goes beyond the left or right
         *                    bounds
         * @param overScrollY the value in pixels that the gesture goes beyond the top or bottom
         *                    bounds
         * @return true if the gesture handling should be given up and sent to the parent view.
         */
        private boolean shareOverscroll(int axis, int overScrollX, int overScrollY) {
            switch (axis) {
                case MotionEvent.AXIS_X:
                    return (mShareScrollToLeft && -overScrollX > OVERSCROLL_THRESHOLD)
                            || (mShareScrollToRight && overScrollX > OVERSCROLL_THRESHOLD);
                case MotionEvent.AXIS_Y:
                    return (mShareScrollToTop && -overScrollY > OVERSCROLL_THRESHOLD)
                            || (mShareScrollToBottom && overScrollY > OVERSCROLL_THRESHOLD);
                default:
                    throw new IllegalArgumentException(
                            "Axis must be MotionEvent.AXIS_X or _Y " + axis);
            }
        }
    }
}

