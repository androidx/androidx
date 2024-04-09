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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.core.util.Preconditions.checkArgument;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;

import java.util.Set;

/**
 * SelectionTracker provides support for managing a selection of items in a RecyclerView instance.
 *
 * <p>
 * This class provides support for managing a "primary" set of selected items,
 * in addition to a "provisional" set of selected items using conventional
 * {@link java.util.Collections}-like methods.
 *
 * <p>
 * Create an instance of SelectionTracker using {@link Builder SelectionTracker.Builder}.
 *
 * <p>
 * <b>Inspecting the current selection</b>
 *
 * <p>
 * The underlying selection is described by the {@link Selection} class.
 *
 * <p>
 * A live view of the current selection can be obtained using {@link #getSelection}. Changes made
 * to the selection using SelectionTracker will be immediately reflected in this instance.
 *
 * <p>
 * To obtain a stable snapshot of the selection use {@link #copySelection(MutableSelection)}.
 *
 * <p>
 * Selection state for an individual item can be obtained using {@link #isSelected(Object)}.
 *
 * <p>
 * <b>Provisional Selection</b>
 *
 * <p>
 * Provisional selection exists to address issues where a transitory selection might
 * momentarily intersect with a previously established selection resulting in a some
 * or all of the established selection being erased. Such situations may arise
 * when band selection is being performed in "additive" mode (e.g. SHIFT or CTRL is pressed
 * on the keyboard prior to mouse down), or when there's an active gesture selection
 * (which can be initiated by long pressing an unselected item while there is an
 * existing selection).
 *
 * <p>
 * A provisional selection can be abandoned, or merged into the primary selection.
 *
 * <p>
 * <b>Enforcing selection policies</b>
 *
 * <p>
 * Which items can be selected by the user is a matter of policy in an Application.
 * Developers supply these policies by way of {@link SelectionPredicate}.
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
public abstract class SelectionTracker<K> {

    private static final String TAG = "SelectionTracker";

    /**
     * This value is included in the payload when SelectionTracker notifies RecyclerView
     * of changes to selection. Look for this value in the {@code payload}
     * Object argument supplied to
     * {@link RecyclerView.Adapter#onBindViewHolder
     * Adapter#onBindViewHolder}.
     * If present the call is occurring in response to a selection state change.
     * This would be a good opportunity to animate changes between unselected and selected state.
     * When state is being restored, this argument will not be present.
     */
    public static final String SELECTION_CHANGED_MARKER = "Selection-Changed";

    /**
     * Adds {@code observer} to be notified when changes to selection occur.
     *
     * <p>
     * Use an observer to track attributes about the selection and
     * update the UI to reflect the state of the selection. For example, an author
     * may use an observer to control the enabled status of menu items,
     * or to initiate {@link android.view.ActionMode}.
     */
    public abstract void addObserver(@NonNull SelectionObserver<K> observer);

    /** @return true if has a selection */
    public abstract boolean hasSelection();

    /**
     * Returns a Selection object that provides a live view on the current selection.
     *
     * @return The current selection.
     * @see #copySelection(MutableSelection) on how to get a snapshot
     * of the selection that will not reflect future changes
     * to selection.
     */
    public abstract @NonNull Selection<K> getSelection();

    /**
     * Updates {@code dest} to reflect the current selection.
     */
    public abstract void copySelection(@NonNull MutableSelection<K> dest);

    /**
     * @return true if the item specified by its id is selected. Shorthand for
     * {@code getSelection().contains(K)}.
     */
    public abstract boolean isSelected(@Nullable K key);

    /**
     * Restores the selected state of specified items. Used in cases such as restore the selection
     * after rotation etc. Provisional selection is not restored.
     *
     * <p>
     * This affords clients the ability to restore selection from selection saved
     * in Activity state.
     *
     * @param selection selection being restored.
     * @see StorageStrategy details on selection state support.
     */
    protected abstract void restoreSelection(@NonNull Selection<K> selection);

    /**
     * Clears both primary and provisional selections.
     *
     * @return true if primary selection changed.
     */
    public abstract boolean clearSelection();

    /**
     * Sets the selected state of the specified items if permitted after consulting
     * SelectionPredicate.
     */
    @SuppressLint("LambdaLast")
    public abstract boolean setItemsSelected(@NonNull Iterable<K> keys, boolean selected);

    /**
     * Attempts to select an item.
     *
     * @return true if the item was selected. False if the item could not be selected, or was
     * was already selected.
     */
    public abstract boolean select(@NonNull K key);

    /**
     * Attempts to deselect an item.
     *
     * @return true if the item was deselected. False if the item could not be deselected, or was
     * was already un-selected.
     */
    public abstract boolean deselect(@NonNull K key);

    @SuppressWarnings("HiddenAbstractMethod")
    @RestrictTo(LIBRARY)
    protected abstract @NonNull AdapterDataObserver getAdapterDataObserver();

    /**
     * Attempts to establish a range selection at {@code position}, selecting the item
     * at {@code position} if needed.
     *
     * @param position The "anchor" position for the range. Subsequent range operations
     *                 (primarily keyboard and mouse based operations like SHIFT + click)
     *                 work with the established anchor point to define selection ranges.
     */
    @SuppressWarnings("HiddenAbstractMethod")
    @RestrictTo(LIBRARY)
    public abstract void startRange(int position);

    /**
     * Sets the end point for the active range selection.
     *
     * <p>
     * This function should only be called when a range selection is active
     * (see {@link #isRangeActive()}. Items in the range [anchor, end] will be
     * selected after consulting SelectionPredicate.
     *
     * @param position The new end position for the selection range.
     * @throws IllegalStateException if a range selection is not active. Range selection
     *                               must have been started by a call to {@link #startRange(int)}.
     */
    @SuppressWarnings("HiddenAbstractMethod")
    @RestrictTo(LIBRARY)
    public abstract void extendRange(int position);

    /**
     * Clears an in-progress range selection. Provisional range selection established
     * using {@link #extendProvisionalRange(int)} will be cleared (unless
     * {@link #mergeProvisionalSelection()} is called first.)
     *
     */
    @SuppressWarnings("HiddenAbstractMethod")
    @RestrictTo(LIBRARY)
    public abstract void endRange();

    /**
     * @return Whether or not there is a current range selection active.
     */
    @SuppressWarnings("HiddenAbstractMethod")
    @RestrictTo(LIBRARY)
    public abstract boolean isRangeActive();

    /**
     * Establishes the "anchor" at which a selection range begins. This "anchor" is consulted
     * when determining how to extend, and modify selection ranges. Calling this when a
     * range selection is active will reset the range selection.
     *
     * TODO: Reconcile this with startRange. Maybe just docs need to be updated.
     *
     * @param position the anchor position. Must already be selected.
     */
    @SuppressWarnings("HiddenAbstractMethod")
    @RestrictTo(LIBRARY)
    public abstract void anchorRange(int position);

    /**
     * Creates a provisional selection from anchor to {@code position}.
     *
     * @param position the end point.
     */
    @SuppressWarnings("HiddenAbstractMethod")
    @RestrictTo(LIBRARY)
    protected abstract void extendProvisionalRange(int position);

    /**
     * Sets the provisional selection, replacing any existing selection.
     *
     */
    @SuppressWarnings("HiddenAbstractMethod")
    @RestrictTo(LIBRARY)
    protected abstract void setProvisionalSelection(@NonNull Set<K> newSelection);

    /**
     * Clears any existing provisional selection
     *
     */
    @SuppressWarnings("HiddenAbstractMethod")
    @RestrictTo(LIBRARY)
    protected abstract void clearProvisionalSelection();

    /**
     * Converts the provisional selection into primary selection, then clears
     * provisional selection.
     *
     */
    @SuppressWarnings("HiddenAbstractMethod")
    @RestrictTo(LIBRARY)
    protected abstract void mergeProvisionalSelection();

    /**
     * Preserves selection, if any. Call this method from Activity#onSaveInstanceState
     *
     * @param state Bundle instance supplied to onSaveInstanceState.
     */
    public abstract void onSaveInstanceState(@NonNull Bundle state);

    /**
     * Restores selection from previously saved state. Call this method from
     * Activity#onCreate.
     *
     * @param state Bundle instance supplied to onCreate.
     */
    public abstract void onRestoreInstanceState(@Nullable Bundle state);

    /**
     * Observer class providing access to information about Selection state changes.
     *
     * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
     */
    public abstract static class SelectionObserver<K> {

        /**
         * Called when the state of an item has been changed.
         */
        public void onItemStateChanged(@NonNull K key, boolean selected) {
        }

        /**
         * Called when Selection is cleared.
         * TODO(smckay): Make public in a future public API.
         *
         */
        @RestrictTo(LIBRARY)
        protected void onSelectionCleared() {
        }

        /**
         * Called when the underlying data set has changed. After this method is called
         * SelectionTracker will traverse the existing selection,
         * calling {@link #onItemStateChanged(K, boolean)} for each selected item,
         * and deselecting any items that cannot be selected given the updated data-set
         * (and after consulting SelectionPredicate).
         */
        public void onSelectionRefresh() {
        }

        /**
         * Called immediately after completion of any set of changes, excluding
         * those resulting in calls {@link #onSelectionRestored()}.
         */
        public void onSelectionChanged() {
        }

        /**
         * Called immediately after selection is restored.
         * {@link #onItemStateChanged(K, boolean)} will *not* be called
         * for individual items in the selection.
         */
        public void onSelectionRestored() {
        }
    }

    /**
     * Implement SelectionPredicate to control when items can be selected or unselected.
     * See {@link Builder#withSelectionPredicate(SelectionPredicate)}.
     *
     * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
     */
    public abstract static class SelectionPredicate<K> {

        /**
         * Validates a change to selection for a specific key.
         *
         * @param key       the item key
         * @param nextState the next potential selected/unselected state
         * @return true if the item at {@code id} can be set to {@code nextState}.
         */
        public abstract boolean canSetStateForKey(@NonNull K key, boolean nextState);

        /**
         * Validates a change to selection for a specific position. If necessary
         * use {@link ItemKeyProvider} to identy associated key.
         *
         * @param position  the item position
         * @param nextState the next potential selected/unselected state
         * @return true if the item at {@code id} can be set to {@code nextState}.
         */
        public abstract boolean canSetStateAtPosition(int position, boolean nextState);

        /**
         * Permits restriction to single selection mode. Single selection mode has
         * unique behaviors in that it'll deselect an item already selected
         * in order to select the new item.
         *
         * <p>
         * In order to limit the number of items that can be selected,
         * use {@link #canSetStateForKey(Object, boolean)} and
         * {@link #canSetStateAtPosition(int, boolean)}.
         *
         * @return true if more than a single item can be selected.
         */
        public abstract boolean canSelectMultiple();
    }

    /**
     * Builder is the primary mechanism for creating a {@link SelectionTracker} that
     * can be used with your RecyclerView. Once installed, users will be able to create and
     * manipulate a selection of items in a RecyclerView instance using a variety of
     * intuitive techniques like tap, gesture, and mouse-based band selection (aka 'lasso').
     *
     * <p>
     * Building a bare-bones instance:
     *
     * <pre>{@code
     * SelectionTracker<Uri> tracker = new SelectionTracker.Builder<>(
     *         "my-uri-selection",
     *         recyclerView,
     *         new YourItemKeyProvider(recyclerView.getAdapter()),
     *         new YourItemDetailsLookup(recyclerView),
     *         StorageStrategy.createParcelableStorage(Uri.class))
     *     .build();
     * }</pre>
     *
     * <p>
     * <b>Restricting which items can be selected and limiting selection size</b>
     *
     * <p>
     * {@link SelectionPredicate} and
     * {@link SelectionTracker.Builder#withSelectionPredicate(SelectionPredicate)}
     * together provide a mechanism for restricting which items can be selected and
     * limiting selection size. Use {@link SelectionPredicates#createSelectSingleAnything()}
     * for single-selection, or write your own {@link SelectionPredicate} if other
     * constraints are required.
     *
     * <pre>{@code
     * SelectionTracker<String> tracker = new SelectionTracker.Builder<>(
     *         "my-string-selection",
     *         recyclerView,
     *         new YourItemKeyProvider(recyclerView.getAdapter()),
     *         new YourItemDetailsLookup(recyclerView),
     *         StorageStrategy.createStringStorage())
     *     .withSelectionPredicate(SelectionPredicates#createSelectSingleAnything())
     *     .build();
     * }</pre>
     *
     * <p>
     * <b>Retaining state across Android lifecycle events</b>
     *
     * <p>
     * Support for storage/persistence of selection must be configured and invoked manually
     * owing to its reliance on Activity lifecycle events.
     * Failure to include support for selection storage will result in selection
     * being lost when the Activity receives a configuration change (e.g. rotation),
     * or when the application is paused or stopped. For this reason
     * {@link StorageStrategy} is a required argument to obtain a {@link Builder}
     * instance.
     *
     * <p>
     * <b>Key Type</b>
     *
     * <p>
     * A developer must decide on the key type used to identify selected items.
     * Support is provided for three types: {@link Parcelable}, {@link String}, and {@link Long}.
     *
     * <p>
     * {@link Parcelable}: Any Parcelable type can be used as the selection key. This is especially
     * useful in conjunction with {@link android.net.Uri} as the Android URI implementation is both
     * parcelable and makes for a natural stable selection key for values represented by
     * the Android Content Provider framework. If items in your view are associated with
     * stable {@code content://} uris, you should use Uri for your key type.
     *
     * <p>
     * {@link String}: Use String when a string based stable identifier is available.
     *
     * <p>
     * {@link Long}: Use Long when a project is already employing RecyclerView's built-in
     * support for stable ids. In this case you may choose to use {@link StableIdKeyProvider}
     * to supply selection keys to the SelectionTracker based on data already accessible
     * in RecyclerView and it's Adapter.
     *
     * See {@link StableIdKeyProvider} for important details and limitations (<i>and a suggestion
     * that you might just want to write your own {@link ItemKeyProvider}. It's easy!</i>)
     * See the "Gotchas" selection below for details on selection size limits.
     *
     * <p>
     * Usage:
     *
     * <pre>{@code
     * private SelectionTracker<Uri> tracker;
     *
     * public void onCreate(Bundle savedInstanceState) {
     *   if (savedInstanceState != null) {
     *     tracker.onRestoreInstanceState(savedInstanceState);
     *   }
     * }
     *
     * protected void onSaveInstanceState(Bundle outState) {
     *   super.onSaveInstanceState(outState);
     *   tracker.onSaveInstanceState(outState);
     * }
     * }</pre>
     *
     * @param <K> Selection key type. Built in support is provided for {@link String},
     *            {@link Long}, and {@link Parcelable}. {@link StorageStrategy}
     *            provides factory methods for each type:
     *            {@link StorageStrategy#createStringStorage()},
     *            {@link StorageStrategy#createParcelableStorage(Class)},
     *            {@link StorageStrategy#createLongStorage()}
     *
     * <p>
     * <b>Gotchas</b>
     *
     * <p>TransactionTooLargeException:
     *
     * <p>Many factors affect the maximum number of items that can be persisted when the application
     * is paused or stopped. Unfortunately that number is not deterministic as it depends on the
     * size of the key type used for selection, the number of selected items, and external demand
     * on system resources. For that reason it is best to use the smallest viable key type,
     * and to enforce a limit on the number of items that can be selected.
     *
     * <p>Furthermore the inability to persist a selection during a lifecycle event
     * will result in a android.os.{@link android.os.TransactionTooLargeException}.
     * See http://issuetracker.google.com/168706011 for details.
     *
     * <p>ItemTouchHelper
     *
     * <p>When using {@link SelectionTracker} along side an
     * {@link androidx.recyclerview.widget.ItemTouchHelper} with the same RecyclerView instance
     * the SelectionTracker instance must be created and installed before the ItemTouchHelper.
     * Failure to do so will result in unintended selections during item drag operations,
     * and possibly other situations.
     */
    public static final class Builder<K> {

        final RecyclerView mRecyclerView;
        private final RecyclerView.Adapter<?> mAdapter;
        private final Context mContext;
        private final String mSelectionId;
        private final StorageStrategy<K> mStorage;

        SelectionPredicate<K> mSelectionPredicate =
                SelectionPredicates.createSelectAnything();
        private OperationMonitor mMonitor = new OperationMonitor();
        private ItemKeyProvider<K> mKeyProvider;
        private ItemDetailsLookup<K> mDetailsLookup;

        private FocusDelegate<K> mFocusDelegate = FocusDelegate.stub();

        private OnItemActivatedListener<K> mOnItemActivatedListener;
        private OnDragInitiatedListener mOnDragInitiatedListener;
        private OnContextClickListener mOnContextClickListener;

        private BandPredicate mBandPredicate;
        private int mBandOverlayId = R.drawable.selection_band_overlay;

        // TODO(b/144500333): Remove support for overriding gesture and pointer tooltypes.
        private int[] mGestureToolTypes = new int[]{
                MotionEvent.TOOL_TYPE_FINGER
        };

        private int[] mPointerToolTypes = new int[]{
                MotionEvent.TOOL_TYPE_MOUSE
        };

        /**
         * Creates a new SelectionTracker.Builder useful for configuring and creating
         * a new SelectionTracker for use with your {@link RecyclerView}.
         *
         * @param selectionId   A unique string identifying this selection in the context
         *                      of the activity or fragment.
         * @param recyclerView  the owning RecyclerView
         * @param keyProvider   the source of selection keys
         * @param detailsLookup the source of information about RecyclerView items.
         * @param storage       Strategy for type-safe storage of selection state in
         *                      {@link Bundle}.
         */
        public Builder(
                @NonNull String selectionId,
                @NonNull RecyclerView recyclerView,
                @NonNull ItemKeyProvider<K> keyProvider,
                @NonNull ItemDetailsLookup<K> detailsLookup,
                @NonNull StorageStrategy<K> storage) {

            checkArgument(selectionId != null);
            checkArgument(!selectionId.trim().isEmpty());
            checkArgument(recyclerView != null);

            mSelectionId = selectionId;
            mRecyclerView = recyclerView;
            mContext = recyclerView.getContext();
            mAdapter = recyclerView.getAdapter();

            checkArgument(mAdapter != null);
            checkArgument(keyProvider != null);
            checkArgument(detailsLookup != null);
            checkArgument(storage != null);

            mDetailsLookup = detailsLookup;
            mKeyProvider = keyProvider;
            mStorage = storage;

            mBandPredicate = new BandPredicate.NonDraggableArea(mRecyclerView, detailsLookup);
        }

        /**
         * Install selection predicate.
         *
         * @param predicate the predicate to be used.
         * @return this
         */
        public @NonNull Builder<K> withSelectionPredicate(
                @NonNull SelectionPredicate<K> predicate) {

            checkArgument(predicate != null);
            mSelectionPredicate = predicate;
            return this;
        }

        /**
         * Add operation monitor allowing access to information about active
         * operations (like band selection and gesture selection).
         *
         * @param monitor the monitor to be used
         * @return this
         */
        public @NonNull Builder<K> withOperationMonitor(
                @NonNull OperationMonitor monitor) {

            checkArgument(monitor != null);
            mMonitor = monitor;
            return this;
        }

        /**
         * Add focus delegate to interact with selection related focus changes.
         *
         * @param delegate the delegate to be used
         * @return this
         */
        public @NonNull Builder<K> withFocusDelegate(@NonNull FocusDelegate<K> delegate) {
            checkArgument(delegate != null);
            mFocusDelegate = delegate;
            return this;
        }

        /**
         * Adds an item activation listener. Respond to taps/enter/double-click on items.
         *
         * @param listener the listener to be used
         * @return this
         */
        public @NonNull Builder<K> withOnItemActivatedListener(
                @NonNull OnItemActivatedListener<K> listener) {

            checkArgument(listener != null);

            mOnItemActivatedListener = listener;
            return this;
        }

        /**
         * Adds a context click listener. Respond to right-click.
         *
         * @param listener the listener to be used
         * @return this
         */
        public @NonNull Builder<K> withOnContextClickListener(
                @NonNull OnContextClickListener listener) {

            checkArgument(listener != null);

            mOnContextClickListener = listener;
            return this;
        }

        /**
         * Adds a drag initiated listener. Add support for drag and drop.
         *
         * @param listener the listener to be used
         * @return this
         */
        public @NonNull Builder<K> withOnDragInitiatedListener(
                @NonNull OnDragInitiatedListener listener) {

            checkArgument(listener != null);

            mOnDragInitiatedListener = listener;
            return this;
        }

        /**
         * Replaces default tap and gesture tool-types. Defaults are:
         * {@link MotionEvent#TOOL_TYPE_FINGER}.
         *
         * @param toolTypes the tool types to be used
         * @return this
         *
         * @deprecated GestureSelection is best bound to {@link MotionEvent#TOOL_TYPE_FINGER},
         * and only that tool type. This method will be removed in a future release.
         */
        @Deprecated
        public @NonNull Builder<K> withGestureTooltypes(@NonNull int... toolTypes) {
            Log.w(TAG, "Setting gestureTooltypes is likely to result in unexpected behavior.");
            mGestureToolTypes = toolTypes;
            return this;
        }

        /**
         * Replaces default band overlay.
         *
         * @return this
         */
        public @NonNull Builder<K> withBandOverlay(@DrawableRes int bandOverlayId) {
            mBandOverlayId = bandOverlayId;
            return this;
        }

        /**
         * Replaces default band predicate.
         *
         * @return this
         */
        public @NonNull Builder<K> withBandPredicate(@NonNull BandPredicate bandPredicate) {
            mBandPredicate = bandPredicate;
            return this;
        }

        /**
         * Replaces default pointer tool-types. Pointer tools
         * are associated with band selection, and certain
         * drag and drop behaviors. Defaults are:
         * {@link MotionEvent#TOOL_TYPE_MOUSE}.
         *
         * @param toolTypes the tool types to be used
         * @return this
         *
         * @deprecated PointerSelection is best bound to {@link MotionEvent#TOOL_TYPE_MOUSE},
         * and only that tool type. This method will be removed in a future release.
         */
        @Deprecated
        public @NonNull Builder<K> withPointerTooltypes(@NonNull int... toolTypes) {
            Log.w(TAG, "Setting pointerTooltypes is likely to result in unexpected behavior.");
            mPointerToolTypes = toolTypes;
            return this;
        }

        /**
         * Prepares and returns a SelectionTracker.
         *
         * @return this
         */
        public @NonNull SelectionTracker<K> build() {

            DefaultSelectionTracker<K> tracker = new DefaultSelectionTracker<>(
                    mSelectionId, mKeyProvider, mSelectionPredicate, mStorage);

            // Event glue between RecyclerView and SelectionTracker keeps the classes separate
            // so that a SelectionTracker can be shared across RecyclerView instances that
            // represent the same data in different ways.
            EventBridge.install(mAdapter, tracker, mKeyProvider, mRecyclerView::post);

            // Scroller is stateful and can be reset, but we don't manage it directly.
            // GestureSelectionHelper will reset scroller when it is reset.
            AutoScroller scroller =
                    new ViewAutoScroller(ViewAutoScroller.createScrollHost(mRecyclerView));

            // Setup basic input handling, with the touch handler as the default consumer
            // of events. If mouse handling is configured as well, the mouse input
            // related handlers will intercept mouse input events.

            // GestureRouter is responsible for routing GestureDetector events
            // to tool-type specific handlers.
            GestureRouter<MotionInputHandler<K>> gestureRouter = new GestureRouter<>();

            // GestureDetector cancels itself in response to ACTION_CANCEL events.
            GestureDetector gestureDetector = new GestureDetector(mContext, gestureRouter);

            // GestureSelectionHelper provides logic that interprets a combination
            // of motions and gestures in order to provide fluid "long-press and drag"
            // finger driven selection support.
            final GestureSelectionHelper gestureSelectionHelper = GestureSelectionHelper.create(
                    tracker, mSelectionPredicate, mRecyclerView, scroller, mMonitor);

            // EventRouter receives events for RecyclerView, dispatching to handlers
            // registered by tool-type.
            EventRouter eventRouter = new EventRouter();
            GestureDetectorWrapper gestureDetectorWrapper =
                    new GestureDetectorWrapper(gestureDetector);

            // Temp fix for b/166836317.
            // TODO: Add support for multiple listeners per tool type to EventRouter, then
            //  register backstop with primary router.
            EventRouter backstopRouter = new EventRouter();
            EventBackstop backstop = new EventBackstop();
            DisallowInterceptFilter backstopWrapper = new DisallowInterceptFilter(backstop);
            backstopRouter.set(MotionEvent.TOOL_TYPE_FINGER, backstopWrapper);

            // Finally hook the framework up to listening to RecycleView events.
            mRecyclerView.addOnItemTouchListener(eventRouter);
            mRecyclerView.addOnItemTouchListener(gestureDetectorWrapper);
            mRecyclerView.addOnItemTouchListener(backstopRouter);

            // Reset manager listens for cancel events from RecyclerView. In response to that it
            // advises other classes it is time to reset state.
            ResetManager<K> resetMgr = new ResetManager<>();

            // Register ResetManager to:
            //
            // 1. Monitor selection reset which can be invoked by clients in response
            //    to back key press and some application lifecycle events.
            tracker.addObserver(resetMgr.getSelectionObserver());

            // ...and  2. Monitor ACTION_CANCEL events (which arrive exclusively
            // via TOOL_TYPE_UNKNOWN).
            //
            // CAUTION! Registering resetMgr directly with RecyclerView#addOnItemTouchListener
            // will not work as expected. Once EventRouter returns true, RecyclerView will
            // no longer dispatch any events to other listeners for the duration of the
            // stream, not even ACTION_CANCEL events.
            eventRouter.set(MotionEvent.TOOL_TYPE_UNKNOWN, resetMgr.getInputListener());

            // Finally register all of the Resettables.
            resetMgr.addResetHandler(tracker);
            resetMgr.addResetHandler(mMonitor.asResettable());
            resetMgr.addResetHandler(gestureSelectionHelper);
            resetMgr.addResetHandler(gestureDetectorWrapper);
            resetMgr.addResetHandler(eventRouter);
            resetMgr.addResetHandler(backstopRouter);
            resetMgr.addResetHandler(backstop);
            resetMgr.addResetHandler(backstopWrapper);

            // But before you move on, there's more work to do. Event plumbing has been
            // installed, but we haven't registered any of our helpers or callbacks.
            // Helpers contain predefined logic converting events into selection related events.
            // Callbacks provide developers the ability to reponspond to other types of
            // events (like "activate" a tapped item). This is broken up into two main
            // suites, one for "touch" and one for "mouse", though both can and should (usually)
            // be configured to handle other types of input (to satisfy user expectation).);

            // Internally, the code doesn't permit nullable listeners, so we lazily
            // initialize stub instances if the developer didn't supply a real listener.
            mOnDragInitiatedListener = (mOnDragInitiatedListener != null)
                    ? mOnDragInitiatedListener
                    : new OnDragInitiatedListener() {
                        @Override
                        public boolean onDragInitiated(@NonNull MotionEvent e) {
                            return false;
                        }
                    };

            mOnItemActivatedListener = (mOnItemActivatedListener != null)
                    ? mOnItemActivatedListener
                    : new OnItemActivatedListener<K>() {
                        @Override
                        public boolean onItemActivated(
                                @NonNull ItemDetailsLookup.ItemDetails<K> item,
                                @NonNull MotionEvent e) {
                            return false;
                        }
                    };

            mOnContextClickListener = (mOnContextClickListener != null)
                    ? mOnContextClickListener
                    : new OnContextClickListener() {
                        @Override
                        public boolean onContextClick(@NonNull MotionEvent e) {
                            return false;
                        }
                    };

            // Provides high level glue for binding touch events
            // and gestures to selection framework.
            TouchInputHandler<K> touchHandler = new TouchInputHandler<>(
                    tracker,
                    mKeyProvider,
                    mDetailsLookup,
                    mSelectionPredicate,
                    gestureSelectionHelper::start,
                    mOnDragInitiatedListener,
                    mOnItemActivatedListener,
                    mFocusDelegate,
                    new Runnable() {
                        @Override
                        public void run() {
                            mRecyclerView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        }
                    },
                    // Provide temporary glue to address b/166836317
                    backstop::onLongPress);

            for (int toolType : mGestureToolTypes) {
                gestureRouter.register(toolType, touchHandler);
                eventRouter.set(toolType, gestureSelectionHelper);
            }

            // Provides high level glue for binding mouse events and gestures
            // to selection framework.
            MouseInputHandler<K> mouseHandler = new MouseInputHandler<>(
                    tracker,
                    mKeyProvider,
                    mDetailsLookup,
                    mOnContextClickListener,
                    mOnItemActivatedListener,
                    mFocusDelegate);

            for (int toolType : mPointerToolTypes) {
                gestureRouter.register(toolType, mouseHandler);
            }

            @Nullable BandSelectionHelper<K> bandHelper = null;

            // Band selection not supported in single select mode, or when key access
            // is limited to anything less than the entire corpus.
            if (mKeyProvider.hasAccess(ItemKeyProvider.SCOPE_MAPPED)
                    && mSelectionPredicate.canSelectMultiple()) {
                // BandSelectionHelper provides support for band selection on-top of a RecyclerView
                // instance. Given the recycling nature of RecyclerView BandSelectionController
                // necessarily models and caches list/grid information as the user's pointer
                // interacts with the item in the RecyclerView. Selectable items that intersect
                // with the band, both on and off screen, are selected.
                bandHelper = BandSelectionHelper.create(
                        mRecyclerView,
                        scroller,
                        mBandOverlayId,
                        mKeyProvider,
                        tracker,
                        mSelectionPredicate,
                        mBandPredicate,
                        mFocusDelegate,
                        mMonitor);

                resetMgr.addResetHandler(bandHelper);
            }

            OnItemTouchListener pointerEventHandler = new PointerDragEventInterceptor(
                    mDetailsLookup, mOnDragInitiatedListener, bandHelper);

            eventRouter.set(MotionEvent.TOOL_TYPE_MOUSE, pointerEventHandler);

            return tracker;
        }
    }
}
