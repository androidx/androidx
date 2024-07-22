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

package androidx.pdf.viewer;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;
import androidx.pdf.data.DisplayData;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.Observables;
import androidx.pdf.util.Observables.ExposedValue;

/**
 * A widget that displays the contents of a file in a given PDF format.
 *
 * <p>This class introduces 2 new life-cycle callbacks:
 *
 * <ul>
 *   <li>{@link #onEnter} will be called either when the user slides the film strip so that this
 *       Viewer comes on-screen or during {@link #onStart()}) if it starts visible.
 *   <li>{@link #onExit} is the reverse of {@link #onEnter} and will be called when the user slides
 *       the film strip so that this Viewer goes off-screen or during {@link #onStop()}.
 * </ul>
 *
 * <p>A Viewer also reports precisely on the status of its view hierarchy, since typically it takes
 * time to load the relevant data and make it ready to be displayed: {@link #mViewState} reports 3
 * states: {@link ViewState#NO_VIEW}, {@link ViewState#VIEW_CREATED} (the view skeleton has been
 * created, and is empty) and {@link ViewState#VIEW_READY} (the data has been loaded into the view).
 *
 * <p>This class doesn't take care of any loading or saving of data - subclasses must handle this
 * themselves - see {@link LoadingViewer} which handles some of this.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
public abstract class Viewer extends Fragment {

    protected static final String KEY_DATA = "data";

    /**
     * The state of the view hierarchy for this {@link Fragment}, as exposed by {@link #mViewState}.
     */
    public enum ViewState {

        /**
         * The view hierarchy does not exist yet or anymore (e.g. before {@link #onCreateView}).
         */
        NO_VIEW,

        /**
         * The view hierarchy exists but may be showing no or partial contents.
         * <p>
         * The state as reported by {@link #mViewState} is guaranteed to change to this value after
         * {@link #onCreateView} completes, and when {@link #getView} returns a non-null View.
         */
        VIEW_CREATED,

        /**
         * The view hierarchy is ready for prime time: all Views are populated and responding.
         * This is to be reported by subclasses, when that condition happens (but no sooner than
         * {@link #onStart}).
         * <p>
         * This is unrelated to {@link #onStart}, {@link #onResume} or {@link #onEnter}, as the view
         * could be ready but not currently showing because of other factors.
         */
        VIEW_READY,

        /**
         * There is no view because this Viewer failed to start up (e.g. broken file).
         */
        ERROR
    }

    /** True when this Fragment's life-cycle is between {@link #onStart} and {@link #onStop}. */
    private boolean mStarted;

    /**
     * True when this Viewer is on-screen (but independent on whether it is actually started, so it
     * could be invisible, because obscured by another app).
     * This value is controlled by {@link #postEnter} and {@link #exit}.
     */
    private boolean mOnScreen;

    /** Marks that {@link #onEnter} must be run after {@link #onCreateView}. */
    private boolean mDelayedEnter;

    protected boolean mIsPasswordProtected;

    /** The container where this viewer is attached. */
    @NonNull
    protected ViewGroup mContainer;

    @NonNull
    protected ExposedValue<ViewState> mViewState = Observables.newExposedValueWithInitialValue(
            ViewState.NO_VIEW);

    {
        // We can call getArguments() from setters and know that it will not be null.
        setArguments(new Bundle());
    }

    /** Reports the {@link ViewState} of this Fragment. */
    @NonNull
    public ObservableValue<ViewState> viewState() {
        return mViewState;
    }

    /**
     * Configures whether this viewer has to share scroll gestures in any direction with its
     * container or any neighbouring view.
     * <p>
     * This call is only permitted when the viewer has a view, i.e. {@link #mViewState} reports at
     * least {@link ViewState#VIEW_CREATED}.
     *
     * @param left   If true, will pass on scroll gestures that extend beyond the left bound.
     * @param right  If true, will pass on scroll gestures that extend beyond the right bound.
     * @param top    If true, will pass on scroll gestures that extend beyond the top bound.
     * @param bottom If true, will pass on scroll gestures that extend beyond the bottom bound.
     */
    public void configureShareScroll(boolean left, boolean right, boolean top, boolean bottom) {
        // Nothing by default.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // editFabTarget = new BaseViewerEditFabTargetImpl(requireActivity(), this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedState) {
        if (container == null) {
            // Don't throw an exception here, as this may happen during restoreInstanceState for
            // Viewers that we don't need anymore.
            return null;
        }
        this.mContainer = container;
        return null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mViewState.get() == ViewState.NO_VIEW || mViewState.get() == ViewState.ERROR) {
            mViewState.set(ViewState.VIEW_CREATED);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mStarted = true;
        if (mDelayedEnter || mOnScreen) {
            onEnter();
            mDelayedEnter = false;
        }
    }

    /**
     * Notifies this Viewer goes on-screen. Guarantees that {@link #onEnter} will be called now or
     * when the Viewer is started.
     */
    public void postEnter() {

        mOnScreen = true;
        if (mStarted) {
            onEnter();
        } else {
            mDelayedEnter = true;
        }
    }

    /** Called after this viewer enters the screen and becomes visible. */
    @CallSuper
    protected void onEnter() {
        // TODO: Track file opened event, content length and view progress.
        participateInAccessibility(true);
    }

    /** Called after this viewer exits the screen and becomes invisible to the user. */
    @CallSuper
    protected void onExit() {
        // TODO: Track file closed event, content length and view progress.
        participateInAccessibility(false);
    }

    @Override
    public void onStop() {
        if (mOnScreen) {
            onExit();
        }
        mStarted = false;
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        destroyView();
        mContainer = null;
        super.onDestroyView();
    }

    /**
     * Called when this viewer no longer needs (or has) a view. Resets {@link #mViewState} to
     * {@link ViewState#NO_VIEW}. If the viewer is to be reused, it will restart its whole
     * life-cycle including {@link #onCreateView}.
     * When overridden by subclasses, it must be idempotent, and this method must be called. It is
     * possible (and likely) it will be called more than once.
     * <p>
     * We could include this in {@link #onDestroyView}, if only it was guaranteed to be called.
     */
    protected void destroyView() {
        if (mViewState.get() != ViewState.NO_VIEW) {
            mViewState.set(ViewState.NO_VIEW);
        }
        if (mContainer != null && getView() != null && mContainer == getView().getParent()) {
            // Some viewers add extra views to their container, e.g. toasts. Remove them all.
            // Do not remove what's under it though.
            int count = mContainer.getChildCount();
            View child;
            for (int i = count - 1; i > 0; i--) {
                child = mContainer.getChildAt(i);
                mContainer.removeView(child);
                if (child == getView()) {
                    break;
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Returns true when this Viewer is on-screen (= entered but not exited) and active (i.e. the
     * Activity is resumed).
     */
    protected boolean isShowing() {
        return isResumed() && mOnScreen;
    }

    protected boolean isStarted() {
        return mStarted;
    }

    /** Makes the views of this Viewer visible to TalkBack (in the swipe gesture circus) or not. */
    @SuppressLint(
            "NewApi")
    // Call requires API 16 and we're on API 19 but our Manifest config thinks its 14
    protected void participateInAccessibility(boolean participate) {
        if (!participate) {
            disableAccessibilityPostKitKat();
        } else {
            requireView()
                    .setImportantForAccessibility(
                            participate
                                    ? View.IMPORTANT_FOR_ACCESSIBILITY_YES
                                    : View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    @SuppressLint(
            "NewApi")
    // Call requires API 16 and we're on API 19 but our Manifest config thinks its 14
    private void disableAccessibilityPostKitKat() {
        getView().setImportantForAccessibility(
                View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    /** Save the {@link DisplayData}'s content reference (not the contents itself) to arguments. */
    protected void saveToArguments(@NonNull DisplayData data) {
        getArguments().putBundle(KEY_DATA, data.asBundle());
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
