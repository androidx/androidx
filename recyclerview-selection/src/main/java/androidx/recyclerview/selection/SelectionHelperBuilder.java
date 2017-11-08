/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static android.support.v4.util.Preconditions.checkArgument;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;

import androidx.recyclerview.selection.SelectionHelper.SelectionPredicate;

/**
 * Builder class for assembling selection support. Example usage:
 *
 * <p><pre>SelectionHelperBuilder selSupport = new SelectionHelperBuilder(
        mRecView, new DemoStableIdProvider(mAdapter), detailsLookup);

 // By default multi-select is supported.
 SelectionHelper selHelper = selSupport
       .build();

 // This configuration support single selection for any element.
 SelectionHelper selHelper = selSupport
       .withSelectionPredicate(SelectionHelper.SelectionPredicate.SINGLE_ANYTHING)
       .build();

 // Lazily bind SelectionHelper. Allows us to defer initialization of the
 // SelectionHelper dependency until after the adapter is created.
 mAdapter.bindSelectionHelper(selHelper);

 * </pre></p>
 *
 * @see SelectionStorage for important deatils on retaining selection across Activity
 * lifecycle events.
 *
 * @param <K> Selection key type. Usually String or Long.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class SelectionHelperBuilder<K> {

    private final RecyclerView mRecView;
    private final RecyclerView.Adapter<?> mAdapter;
    private final Context mContext;

    // Content lock provides a mechanism to block content reload while selection
    // activities are active. If using a loader to load content, route
    // the call through the content lock using ContentLock#runWhenUnlocked.
    // This is especially useful when listening on content change notification.
    private final ContentLock mLock = new ContentLock();

    private SelectionPredicate<K> mSelectionPredicate = SelectionPredicates.selectAnything();
    private ItemKeyProvider<K> mKeyProvider;
    private ItemDetailsLookup<K> mDetailsLookup;

    private ActivationCallbacks<K> mActivationCallbacks = ActivationCallbacks.dummy();
    private FocusCallbacks<K> mFocusCallbacks = FocusCallbacks.dummy();
    private TouchCallbacks mTouchCallbacks = TouchCallbacks.DUMMY;
    private MouseCallbacks mMouseCallbacks = MouseCallbacks.DUMMY;

    private BandPredicate mBandPredicate;
    private int mBandOverlayId = R.drawable.selection_band_overlay;

    private int[] mGestureToolTypes = new int[] {
        MotionEvent.TOOL_TYPE_FINGER,
        MotionEvent.TOOL_TYPE_UNKNOWN
    };

    private int[] mBandToolTypes = new int[] {
        MotionEvent.TOOL_TYPE_MOUSE,
        MotionEvent.TOOL_TYPE_STYLUS
    };

    public SelectionHelperBuilder(
            RecyclerView recView,
            ItemKeyProvider<K> keyProvider,
            ItemDetailsLookup<K> detailsLookup) {

        checkArgument(recView != null);

        mRecView = recView;
        mContext = recView.getContext();
        mAdapter = recView.getAdapter();

        checkArgument(mAdapter != null);
        checkArgument(keyProvider != null);
        checkArgument(detailsLookup != null);

        mDetailsLookup = detailsLookup;
        mKeyProvider = keyProvider;

        mBandPredicate = BandPredicate.notDraggable(mRecView, detailsLookup);
    }

    /**
     * Install seleciton predicate.
     * @param predicate
     * @return
     */
    public SelectionHelperBuilder<K> withSelectionPredicate(SelectionPredicate<K> predicate) {
        checkArgument(predicate != null);
        mSelectionPredicate = predicate;
        return this;
    }

    /**
     * Add activation callbacks to respond to taps/enter/double-click on items.
     *
     * @param callbacks
     * @return
     */
    public SelectionHelperBuilder<K> withActivationCallbacks(ActivationCallbacks<K> callbacks) {
        checkArgument(callbacks != null);
        mActivationCallbacks = callbacks;
        return this;
    }

    /**
     * Add focus callbacks to interfact with selection related focus changes.
     * @param callbacks
     * @return
     */
    public SelectionHelperBuilder<K> withFocusCallbacks(FocusCallbacks<K> callbacks) {
        checkArgument(callbacks != null);
        mFocusCallbacks = callbacks;
        return this;
    }

    /**
     * Configures mouse callbacks, replacing defaults.
     *
     * @param callbacks
     * @return
     */
    public SelectionHelperBuilder<K> withMouseCallbacks(MouseCallbacks callbacks) {
        checkArgument(callbacks != null);

        mMouseCallbacks = callbacks;
        return this;
    }

    /**
     * Replaces default touch callbacks.
     *
     * @param callbacks
     * @return
     */
    public SelectionHelperBuilder<K> withTouchCallbacks(TouchCallbacks callbacks) {
        checkArgument(callbacks != null);

        mTouchCallbacks = callbacks;
        return this;
    }

    /**
     * Replaces default gesture tooltypes.
     * @param toolTypes
     * @return
     */
    public SelectionHelperBuilder<K> withTouchTooltypes(int... toolTypes) {
        mGestureToolTypes = toolTypes;
        return this;
    }

    /**
     * Replaces default band overlay.
     *
     * @param bandOverlayId
     * @return
     */
    public SelectionHelperBuilder<K> withBandOverlay(@DrawableRes int bandOverlayId) {
        mBandOverlayId = bandOverlayId;
        return this;
    }

    /**
     * Replaces default band predicate.
     * @param bandPredicate
     * @return
     */
    public SelectionHelperBuilder<K> withBandPredicate(BandPredicate bandPredicate) {

        checkArgument(bandPredicate != null);

        mBandPredicate = bandPredicate;
        return this;
    }

    /**
     * Replaces default band tools types.
     * @param toolTypes
     * @return
     */
    public SelectionHelperBuilder<K> withBandTooltypes(int... toolTypes) {
        mBandToolTypes = toolTypes;
        return this;
    }

    /**
     * Prepares selection support and returns the corresponding SelectionHelper.
     *
     * @return
     */
    public SelectionHelper<K> build() {

        SelectionHelper<K> selectionHelper =
                new DefaultSelectionHelper<>(mKeyProvider, mSelectionPredicate);

        // Event glue between RecyclerView and SelectionHelper keeps the classes separate
        // so that a SelectionHelper can be shared across RecyclerView instances that
        // represent the same data in different ways.
        EventBridge.install(mAdapter, selectionHelper, mKeyProvider);

        AutoScroller scroller = new ViewAutoScroller(ViewAutoScroller.createScrollHost(mRecView));

        // Setup basic input handling, with the touch handler as the default consumer
        // of events. If mouse handling is configured as well, the mouse input
        // related handlers will intercept mouse input events.

        // GestureRouter is responsible for routing GestureDetector events
        // to tool-type specific handlers.
        GestureRouter<MotionInputHandler> gestureRouter = new GestureRouter<>();
        GestureDetector gestureDetector = new GestureDetector(mContext, gestureRouter);

        // TouchEventRouter takes its name from RecyclerView#OnItemTouchListener.
        // Despite "Touch" being in the name, it receives events for all types of tools.
        // This class is responsible for routing events to tool-type specific handlers,
        // and if not handled by a handler, on to a GestureDetector for analysis.
        TouchEventRouter eventRouter = new TouchEventRouter(gestureDetector);

        // GestureSelectionHelper provides logic that interprets a combination
        // of motions and gestures in order to provide gesture driven selection support
        // when used in conjunction with RecyclerView.
        final GestureSelectionHelper gestureHelper =
                GestureSelectionHelper.create(selectionHelper, mRecView, scroller, mLock);

        // Finally hook the framework up to listening to recycle view events.
        mRecView.addOnItemTouchListener(eventRouter);

        // But before you move on, there's more work to do. Event plumbing has been
        // installed, but we haven't registered any of our helpers or callbacks.
        // Helpers contain predefined logic converting events into selection related events.
        // Callbacks provide authors the ability to reponspond to other types of
        // events (like "active" a tapped item). This is broken up into two main
        // suites, one for "touch" and one for "mouse", though both can and should (usually)
        // be configued to handle other types of input (to satisfy user expectation).);

        // Provides high level glue for binding touch events
        // and gestures to selection framework.
        TouchInputHandler<K> touchHandler = new TouchInputHandler<K>(
                selectionHelper,
                mKeyProvider,
                mDetailsLookup,
                mSelectionPredicate,
                new Runnable() {
                    @Override
                    public void run() {
                        if (mSelectionPredicate.canSelectMultiple()) {
                            gestureHelper.start();
                        }
                    }
                },
                mTouchCallbacks,
                mActivationCallbacks,
                mFocusCallbacks,
                new Runnable() {
                    @Override
                    public void run() {
                        mRecView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    }
                });

        for (int toolType : mGestureToolTypes) {
            gestureRouter.register(toolType, touchHandler);
            eventRouter.register(toolType, gestureHelper);
        }

        // Provides high level glue for binding mouse/stylus events and gestures
        // to selection framework.
        MouseInputHandler<K> mouseHandler = new MouseInputHandler<>(
                selectionHelper,
                mKeyProvider,
                mDetailsLookup,
                mMouseCallbacks,
                mActivationCallbacks,
                mFocusCallbacks);

        for (int toolType : mBandToolTypes) {
            gestureRouter.register(toolType, mouseHandler);
        }

        // Band selection not supported in single select mode, or when key access
        // is limited to anything less than the entire corpus.
        // TODO: Since we cach grid info from laid out items, we could cache key too.
        // Then we couldn't have to limit to CORPUS access.
        if (mKeyProvider.hasAccess(ItemKeyProvider.SCOPE_MAPPED)
                && mSelectionPredicate.canSelectMultiple()) {
            // BandSelectionHelper provides support for band selection on-top of a RecyclerView
            // instance. Given the recycling nature of RecyclerView BandSelectionController
            // necessarily models and caches list/grid information as the user's pointer
            // interacts with the item in the RecyclerView. Selectable items that intersect
            // with the band, both on and off screen, are selected.
            BandSelectionHelper bandHelper = BandSelectionHelper.create(
                    mRecView,
                    scroller,
                    mBandOverlayId,
                    mKeyProvider,
                    selectionHelper,
                    mSelectionPredicate,
                    mBandPredicate,
                    mFocusCallbacks,
                    mLock);

            for (int toolType : mBandToolTypes) {
                eventRouter.register(toolType, bandHelper);
            }
        }

        return selectionHelper;
    }
}
