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

package androidx.recyclerview.widget;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * DiffUtil is a utility class that calculates the difference between two lists and outputs a
 * list of update operations that converts the first list into the second one.
 * <p>
 * It can be used to calculate updates for a RecyclerView Adapter. See {@link ListAdapter} and
 * {@link AsyncListDiffer} which can simplify the use of DiffUtil on a background thread.
 * <p>
 * DiffUtil uses Eugene W. Myers's difference algorithm to calculate the minimal number of updates
 * to convert one list into another. Myers's algorithm does not handle items that are moved so
 * DiffUtil runs a second pass on the result to detect items that were moved.
 * <p>
 * Note that DiffUtil, {@link ListAdapter}, and {@link AsyncListDiffer} require the list to not
 * mutate while in use.
 * This generally means that both the lists themselves and their elements (or at least, the
 * properties of elements used in diffing) should not be modified directly. Instead, new lists
 * should be provided any time content changes. It's common for lists passed to DiffUtil to share
 * elements that have not mutated, so it is not strictly required to reload all data to use
 * DiffUtil.
 * <p>
 * If the lists are large, this operation may take significant time so you are advised to run this
 * on a background thread, get the {@link DiffResult} then apply it on the RecyclerView on the main
 * thread.
 * <p>
 * This algorithm is optimized for space and uses O(N) space to find the minimal
 * number of addition and removal operations between the two lists. It has O(N + D^2) expected time
 * performance where D is the length of the edit script.
 * <p>
 * If move detection is enabled, it takes an additional O(MN) time where M is the total number of
 * added items and N is the total number of removed items. If your lists are already sorted by
 * the same constraint (e.g. a created timestamp for a list of posts), you can disable move
 * detection to improve performance.
 * <p>
 * The actual runtime of the algorithm significantly depends on the number of changes in the list
 * and the cost of your comparison methods. Below are some average run times for reference:
 * (The test list is composed of random UUID Strings and the tests are run on Nexus 5X with M)
 * <ul>
 *     <li>100 items and 10 modifications: avg: 0.39 ms, median: 0.35 ms
 *     <li>100 items and 100 modifications: 3.82 ms, median: 3.75 ms
 *     <li>100 items and 100 modifications without moves: 2.09 ms, median: 2.06 ms
 *     <li>1000 items and 50 modifications: avg: 4.67 ms, median: 4.59 ms
 *     <li>1000 items and 50 modifications without moves: avg: 3.59 ms, median: 3.50 ms
 *     <li>1000 items and 200 modifications: 27.07 ms, median: 26.92 ms
 *     <li>1000 items and 200 modifications without moves: 13.54 ms, median: 13.36 ms
 * </ul>
 * <p>
 * Due to implementation constraints, the max size of the list can be 2^26.
 *
 * @see ListAdapter
 * @see AsyncListDiffer
 */
public class DiffUtil {
    private DiffUtil() {
        // utility class, no instance.
    }

    private static final Comparator<Diagonal> DIAGONAL_COMPARATOR = new Comparator<Diagonal>() {
        @Override
        public int compare(Diagonal o1, Diagonal o2) {
            return o1.x - o2.x;
        }
    };

    // Myers' algorithm uses two lists as axis labels. In DiffUtil's implementation, `x` axis is
    // used for old list and `y` axis is used for new list.

    /**
     * Calculates the list of update operations that can covert one list into the other one.
     *
     * @param cb The callback that acts as a gateway to the backing list data
     * @return A DiffResult that contains the information about the edit sequence to convert the
     * old list into the new list.
     */
    @NonNull
    public static DiffResult calculateDiff(@NonNull Callback cb) {
        return calculateDiff(cb, true);
    }

    /**
     * Calculates the list of update operations that can covert one list into the other one.
     * <p>
     * If your old and new lists are sorted by the same constraint and items never move (swap
     * positions), you can disable move detection which takes <code>O(N^2)</code> time where
     * N is the number of added, moved, removed items.
     *
     * @param cb The callback that acts as a gateway to the backing list data
     * @param detectMoves True if DiffUtil should try to detect moved items, false otherwise.
     *
     * @return A DiffResult that contains the information about the edit sequence to convert the
     * old list into the new list.
     */
    @NonNull
    public static DiffResult calculateDiff(@NonNull Callback cb, boolean detectMoves) {
        final int oldSize = cb.getOldListSize();
        final int newSize = cb.getNewListSize();

        final List<Diagonal> diagonals = new ArrayList<>();

        // instead of a recursive implementation, we keep our own stack to avoid potential stack
        // overflow exceptions
        final List<Range> stack = new ArrayList<>();

        stack.add(new Range(0, oldSize, 0, newSize));

        final int max = (oldSize + newSize + 1) / 2;
        // allocate forward and backward k-lines. K lines are diagonal lines in the matrix. (see the
        // paper for details)
        // These arrays lines keep the max reachable position for each k-line.
        final CenteredArray forward = new CenteredArray(max * 2 + 1);
        final CenteredArray backward = new CenteredArray(max * 2 + 1);

        // We pool the ranges to avoid allocations for each recursive call.
        final List<Range> rangePool = new ArrayList<>();
        while (!stack.isEmpty()) {
            final Range range = stack.remove(stack.size() - 1);
            final Snake snake = midPoint(range, cb, forward, backward);
            if (snake != null) {
                // if it has a diagonal, save it
                if (snake.diagonalSize() > 0) {
                    diagonals.add(snake.toDiagonal());
                }
                // add new ranges for left and right
                final Range left = rangePool.isEmpty() ? new Range() : rangePool.remove(
                        rangePool.size() - 1);
                left.oldListStart = range.oldListStart;
                left.newListStart = range.newListStart;
                left.oldListEnd = snake.startX;
                left.newListEnd = snake.startY;
                stack.add(left);

                // re-use range for right
                //noinspection UnnecessaryLocalVariable
                final Range right = range;
                right.oldListEnd = range.oldListEnd;
                right.newListEnd = range.newListEnd;
                right.oldListStart = snake.endX;
                right.newListStart = snake.endY;
                stack.add(right);
            } else {
                rangePool.add(range);
            }

        }
        // sort snakes
        Collections.sort(diagonals, DIAGONAL_COMPARATOR);

        return new DiffResult(cb, diagonals,
                forward.backingData(), backward.backingData(),
                detectMoves);
    }

    /**
     * Finds a middle snake in the given range.
     */
    @Nullable
    private static Snake midPoint(
            Range range,
            Callback cb,
            CenteredArray forward,
            CenteredArray backward) {
        if (range.oldSize() < 1 || range.newSize() < 1) {
            return null;
        }
        int max = (range.oldSize() + range.newSize() + 1) / 2;
        forward.set(1, range.oldListStart);
        backward.set(1, range.oldListEnd);
        for (int d = 0; d < max; d++) {
            Snake snake = forward(range, cb, forward, backward, d);
            if (snake != null) {
                return snake;
            }
            snake = backward(range, cb, forward, backward, d);
            if (snake != null) {
                return snake;
            }
        }
        return null;
    }

    @Nullable
    private static Snake forward(
            Range range,
            Callback cb,
            CenteredArray forward,
            CenteredArray backward,
            int d) {
        boolean checkForSnake = Math.abs(range.oldSize() - range.newSize()) % 2 == 1;
        int delta = range.oldSize() - range.newSize();
        for (int k = -d; k <= d; k += 2) {
            // we either come from d-1, k-1 OR d-1. k+1
            // as we move in steps of 2, array always holds both current and previous d values
            // k = x - y and each array value holds the max X, y = x - k
            final int startX;
            final int startY;
            int x, y;
            if (k == -d || (k != d && forward.get(k + 1) > forward.get(k - 1))) {
                // picking k + 1, incrementing Y (by simply not incrementing X)
                x = startX = forward.get(k + 1);
            } else {
                // picking k - 1, incrementing X
                startX = forward.get(k - 1);
                x = startX + 1;
            }
            y = range.newListStart + (x - range.oldListStart) - k;
            startY = (d == 0 || x != startX) ? y : y - 1;
            // now find snake size
            while (x < range.oldListEnd
                    && y < range.newListEnd
                    && cb.areItemsTheSame(x, y)) {
                x++;
                y++;
            }
            // now we have furthest reaching x, record it
            forward.set(k, x);
            if (checkForSnake) {
                // see if we did pass over a backwards array
                // mapping function: delta - k
                int backwardsK = delta - k;
                // if backwards K is calculated and it passed me, found match
                if (backwardsK >= -d + 1
                        && backwardsK <= d - 1
                        && backward.get(backwardsK) <= x) {
                    // match
                    Snake snake = new Snake();
                    snake.startX = startX;
                    snake.startY = startY;
                    snake.endX = x;
                    snake.endY = y;
                    snake.reverse = false;
                    return snake;
                }
            }
        }
        return null;
    }

    @Nullable
    private static Snake backward(
            Range range,
            Callback cb,
            CenteredArray forward,
            CenteredArray backward,
            int d) {
        boolean checkForSnake = (range.oldSize() - range.newSize()) % 2 == 0;
        int delta = range.oldSize() - range.newSize();
        // same as forward but we go backwards from end of the lists to be beginning
        // this also means we'll try to optimize for minimizing x instead of maximizing it
        for (int k = -d; k <= d; k += 2) {
            // we either come from d-1, k-1 OR d-1, k+1
            // as we move in steps of 2, array always holds both current and previous d values
            // k = x - y and each array value holds the MIN X, y = x - k
            // when x's are equal, we prioritize deletion over insertion
            final int startX;
            final int startY;
            int x, y;

            if (k == -d || (k != d && backward.get(k + 1) < backward.get(k - 1))) {
                // picking k + 1, decrementing Y (by simply not decrementing X)
                x = startX = backward.get(k + 1);
            } else {
                // picking k - 1, decrementing X
                startX = backward.get(k - 1);
                x = startX - 1;
            }
            y = range.newListEnd - ((range.oldListEnd - x) - k);
            startY = (d == 0 || x != startX) ? y : y + 1;
            // now find snake size
            while (x > range.oldListStart
                    && y > range.newListStart
                    && cb.areItemsTheSame(x - 1, y - 1)) {
                x--;
                y--;
            }
            // now we have furthest point, record it (min X)
            backward.set(k, x);
            if (checkForSnake) {
                // see if we did pass over a backwards array
                // mapping function: delta - k
                int forwardsK = delta - k;
                // if forwards K is calculated and it passed me, found match
                if (forwardsK >= -d
                        && forwardsK <= d
                        && forward.get(forwardsK) >= x) {
                    // match
                    Snake snake = new Snake();
                    // assignment are reverse since we are a reverse snake
                    snake.startX = x;
                    snake.startY = y;
                    snake.endX = startX;
                    snake.endY = startY;
                    snake.reverse = true;
                    return snake;
                }
            }
        }
        return null;
    }

    /**
     * A Callback class used by DiffUtil while calculating the diff between two lists.
     */
    public abstract static class Callback {
        /**
         * Returns the size of the old list.
         *
         * @return The size of the old list.
         */
        public abstract int getOldListSize();

        /**
         * Returns the size of the new list.
         *
         * @return The size of the new list.
         */
        public abstract int getNewListSize();

        /**
         * Called by the DiffUtil to decide whether two object represent the same Item.
         * <p>
         * For example, if your items have unique ids, this method should check their id equality.
         *
         * @param oldItemPosition The position of the item in the old list
         * @param newItemPosition The position of the item in the new list
         * @return True if the two items represent the same object or false if they are different.
         */
        public abstract boolean areItemsTheSame(int oldItemPosition, int newItemPosition);

        /**
         * Called by the DiffUtil when it wants to check whether two items have the same data.
         * DiffUtil uses this information to detect if the contents of an item has changed.
         * <p>
         * DiffUtil uses this method to check equality instead of {@link Object#equals(Object)}
         * so that you can change its behavior depending on your UI.
         * For example, if you are using DiffUtil with a
         * {@link RecyclerView.Adapter RecyclerView.Adapter}, you should
         * return whether the items' visual representations are the same.
         * <p>
         * This method is called only if {@link #areItemsTheSame(int, int)} returns
         * {@code true} for these items.
         *
         * @param oldItemPosition The position of the item in the old list
         * @param newItemPosition The position of the item in the new list which replaces the
         *                        oldItem
         * @return True if the contents of the items are the same or false if they are different.
         */
        public abstract boolean areContentsTheSame(int oldItemPosition, int newItemPosition);

        /**
         * When {@link #areItemsTheSame(int, int)} returns {@code true} for two items and
         * {@link #areContentsTheSame(int, int)} returns false for them, DiffUtil
         * calls this method to get a payload about the change.
         * <p>
         * For example, if you are using DiffUtil with {@link RecyclerView}, you can return the
         * particular field that changed in the item and your
         * {@link RecyclerView.ItemAnimator ItemAnimator} can use that
         * information to run the correct animation.
         * <p>
         * Default implementation returns {@code null}.
         *
         * @param oldItemPosition The position of the item in the old list
         * @param newItemPosition The position of the item in the new list
         * @return A payload object that represents the change between the two items.
         */
        @Nullable
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return null;
        }
    }

    /**
     * Callback for calculating the diff between two non-null items in a list.
     * <p>
     * {@link Callback} serves two roles - list indexing, and item diffing. ItemCallback handles
     * just the second of these, which allows separation of code that indexes into an array or List
     * from the presentation-layer and content specific diffing code.
     *
     * @param <T> Type of items to compare.
     */
    public abstract static class ItemCallback<T> {
        /**
         * Called to check whether two objects represent the same item.
         * <p>
         * For example, if your items have unique ids, this method should check their id equality.
         * <p>
         * Note: {@code null} items in the list are assumed to be the same as another {@code null}
         * item and are assumed to not be the same as a non-{@code null} item. This callback will
         * not be invoked for either of those cases.
         *
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True if the two items represent the same object or false if they are different.
         * @see Callback#areItemsTheSame(int, int)
         */
        public abstract boolean areItemsTheSame(@NonNull T oldItem, @NonNull T newItem);

        /**
         * Called to check whether two items have the same data.
         * <p>
         * This information is used to detect if the contents of an item have changed.
         * <p>
         * This method to check equality instead of {@link Object#equals(Object)} so that you can
         * change its behavior depending on your UI.
         * <p>
         * For example, if you are using DiffUtil with a
         * {@link RecyclerView.Adapter RecyclerView.Adapter}, you should
         * return whether the items' visual representations are the same.
         * <p>
         * This method is called only if {@link #areItemsTheSame(T, T)} returns {@code true} for
         * these items.
         * <p>
         * Note: Two {@code null} items are assumed to represent the same contents. This callback
         * will not be invoked for this case.
         *
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True if the contents of the items are the same or false if they are different.
         * @see Callback#areContentsTheSame(int, int)
         */
        public abstract boolean areContentsTheSame(@NonNull T oldItem, @NonNull T newItem);

        /**
         * When {@link #areItemsTheSame(T, T)} returns {@code true} for two items and
         * {@link #areContentsTheSame(T, T)} returns false for them, this method is called to
         * get a payload about the change.
         * <p>
         * For example, if you are using DiffUtil with {@link RecyclerView}, you can return the
         * particular field that changed in the item and your
         * {@link RecyclerView.ItemAnimator ItemAnimator} can use that
         * information to run the correct animation.
         * <p>
         * Default implementation returns {@code null}.
         *
         * @see Callback#getChangePayload(int, int)
         */
        @SuppressWarnings({"unused"})
        @Nullable
        public Object getChangePayload(@NonNull T oldItem, @NonNull T newItem) {
            return null;
        }
    }

    /**
     * A diagonal is a match in the graph.
     * Rather than snakes, we only record the diagonals in the path.
     */
    static class Diagonal {
        public final int x;
        public final int y;
        public final int size;

        Diagonal(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }

        int endX() {
            return x + size;
        }

        int endY() {
            return y + size;
        }
    }

    /**
     * Snakes represent a match between two lists. It is optionally prefixed or postfixed with an
     * add or remove operation. See the Myers' paper for details.
     */
    @SuppressWarnings("WeakerAccess")
    static class Snake {
        /**
         * Position in the old list
         */
        public int startX;

        /**
         * Position in the new list
         */
        public int startY;

        /**
         * End position in the old list, exclusive
         */
        public int endX;

        /**
         * End position in the new list, exclusive
         */
        public int endY;

        /**
         * True if this snake was created in the reverse search, false otherwise.
         */
        public boolean reverse;

        boolean hasAdditionOrRemoval() {
            return endY - startY != endX - startX;
        }

        boolean isAddition() {
            return endY - startY > endX - startX;
        }

        int diagonalSize() {
            return Math.min(endX - startX, endY - startY);
        }

        /**
         * Extract the diagonal of the snake to make reasoning easier for the rest of the
         * algorithm where we try to produce a path and also find moves.
         */
        @NonNull
        Diagonal toDiagonal() {
            if (hasAdditionOrRemoval()) {
                if (reverse) {
                    // snake edge it at the end
                    return new Diagonal(startX, startY, diagonalSize());
                } else {
                    // snake edge it at the beginning
                    if (isAddition()) {
                        return new Diagonal(startX, startY + 1, diagonalSize());
                    } else {
                        return new Diagonal(startX + 1, startY, diagonalSize());
                    }
                }
            } else {
                // we are a pure diagonal
                return new Diagonal(startX, startY, endX - startX);
            }
        }
    }

    /**
     * Represents a range in two lists that needs to be solved.
     * <p>
     * This internal class is used when running Myers' algorithm without recursion.
     * <p>
     * Ends are exclusive
     */
    static class Range {

        int oldListStart, oldListEnd;

        int newListStart, newListEnd;

        public Range() {
        }

        public Range(int oldListStart, int oldListEnd, int newListStart, int newListEnd) {
            this.oldListStart = oldListStart;
            this.oldListEnd = oldListEnd;
            this.newListStart = newListStart;
            this.newListEnd = newListEnd;
        }

        int oldSize() {
            return oldListEnd - oldListStart;
        }

        int newSize() {
            return newListEnd - newListStart;
        }
    }

    /**
     * This class holds the information about the result of a
     * {@link DiffUtil#calculateDiff(Callback, boolean)} call.
     * <p>
     * You can consume the updates in a DiffResult via
     * {@link #dispatchUpdatesTo(ListUpdateCallback)} or directly stream the results into a
     * {@link RecyclerView.Adapter} via {@link #dispatchUpdatesTo(RecyclerView.Adapter)}.
     */
    public static class DiffResult {
        /**
         * Signifies an item not present in the list.
         */
        public static final int NO_POSITION = -1;


        /**
         * While reading the flags below, keep in mind that when multiple items move in a list,
         * Myers's may pick any of them as the anchor item and consider that one NOT_CHANGED while
         * picking others as additions and removals. This is completely fine as we later detect
         * all moves.
         * <p>
         * Below, when an item is mentioned to stay in the same "location", it means we won't
         * dispatch a move/add/remove for it, it DOES NOT mean the item is still in the same
         * position.
         */
        // item stayed the same.
        private static final int FLAG_NOT_CHANGED = 1;
        // item stayed in the same location but changed.
        private static final int FLAG_CHANGED = FLAG_NOT_CHANGED << 1;
        // Item has moved and also changed.
        private static final int FLAG_MOVED_CHANGED = FLAG_CHANGED << 1;
        // Item has moved but did not change.
        private static final int FLAG_MOVED_NOT_CHANGED = FLAG_MOVED_CHANGED << 1;
        // Item moved
        private static final int FLAG_MOVED = FLAG_MOVED_CHANGED | FLAG_MOVED_NOT_CHANGED;

        // since we are re-using the int arrays that were created in the Myers' step, we mask
        // change flags
        private static final int FLAG_OFFSET = 4;

        private static final int FLAG_MASK = (1 << FLAG_OFFSET) - 1;

        // The diagonals extracted from The Myers' snakes.
        private final List<Diagonal> mDiagonals;

        // The list to keep oldItemStatuses. As we traverse old items, we assign flags to them
        // which also includes whether they were a real removal or a move (and its new index).
        private final int[] mOldItemStatuses;
        // The list to keep newItemStatuses. As we traverse new items, we assign flags to them
        // which also includes whether they were a real addition or a move(and its old index).
        private final int[] mNewItemStatuses;
        // The callback that was given to calculate diff method.
        private final Callback mCallback;

        private final int mOldListSize;

        private final int mNewListSize;

        private final boolean mDetectMoves;

        /**
         * @param callback        The callback that was used to calculate the diff
         * @param diagonals       Matches between the two lists
         * @param oldItemStatuses An int[] that can be re-purposed to keep metadata
         * @param newItemStatuses An int[] that can be re-purposed to keep metadata
         * @param detectMoves     True if this DiffResult will try to detect moved items
         */
        DiffResult(Callback callback, List<Diagonal> diagonals, int[] oldItemStatuses,
                int[] newItemStatuses, boolean detectMoves) {
            mDiagonals = diagonals;
            mOldItemStatuses = oldItemStatuses;
            mNewItemStatuses = newItemStatuses;
            Arrays.fill(mOldItemStatuses, 0);
            Arrays.fill(mNewItemStatuses, 0);
            mCallback = callback;
            mOldListSize = callback.getOldListSize();
            mNewListSize = callback.getNewListSize();
            mDetectMoves = detectMoves;
            addEdgeDiagonals();
            findMatchingItems();
        }

        /**
         * Add edge diagonals so that we can iterate as long as there are diagonals w/o lots of
         * null checks around
         */
        private void addEdgeDiagonals() {
            Diagonal first = mDiagonals.isEmpty() ? null : mDiagonals.get(0);
            // see if we should add 1 to the 0,0
            if (first == null || first.x != 0 || first.y != 0) {
                mDiagonals.add(0, new Diagonal(0, 0, 0));
            }
            // always add one last
            mDiagonals.add(new Diagonal(mOldListSize, mNewListSize, 0));
        }

        /**
         * Find position mapping from old list to new list.
         * If moves are requested, we'll also try to do an n^2 search between additions and
         * removals to find moves.
         */
        private void findMatchingItems() {
            for (Diagonal diagonal : mDiagonals) {
                for (int offset = 0; offset < diagonal.size; offset++) {
                    int posX = diagonal.x + offset;
                    int posY = diagonal.y + offset;
                    final boolean theSame = mCallback.areContentsTheSame(posX, posY);
                    final int changeFlag = theSame ? FLAG_NOT_CHANGED : FLAG_CHANGED;
                    mOldItemStatuses[posX] = (posY << FLAG_OFFSET) | changeFlag;
                    mNewItemStatuses[posY] = (posX << FLAG_OFFSET) | changeFlag;
                }
            }
            // now all matches are marked, lets look for moves
            if (mDetectMoves) {
                // traverse each addition / removal from the end of the list, find matching
                // addition removal from before
                findMoveMatches();
            }
        }

        private void findMoveMatches() {
            // for each removal, find matching addition
            int posX = 0;
            for (Diagonal diagonal : mDiagonals) {
                while (posX < diagonal.x) {
                    if (mOldItemStatuses[posX] == 0) {
                        // there is a removal, find matching addition from the rest
                        findMatchingAddition(posX);
                    }
                    posX++;
                }
                // snap back for the next diagonal
                posX = diagonal.endX();
            }
        }

        /**
         * Search the whole list to find the addition for the given removal of position posX
         *
         * @param posX position in the old list
         */
        private void findMatchingAddition(int posX) {
            int posY = 0;
            final int diagonalsSize = mDiagonals.size();
            for (int i = 0; i < diagonalsSize; i++) {
                final Diagonal diagonal = mDiagonals.get(i);
                while (posY < diagonal.y) {
                    // found some additions, evaluate
                    if (mNewItemStatuses[posY] == 0) { // not evaluated yet
                        boolean matching = mCallback.areItemsTheSame(posX, posY);
                        if (matching) {
                            // yay found it, set values
                            boolean contentsMatching = mCallback.areContentsTheSame(posX, posY);
                            final int changeFlag = contentsMatching ? FLAG_MOVED_NOT_CHANGED
                                    : FLAG_MOVED_CHANGED;
                            // once we process one of these, it will mark the other one as ignored.
                            mOldItemStatuses[posX] = (posY << FLAG_OFFSET) | changeFlag;
                            mNewItemStatuses[posY] = (posX << FLAG_OFFSET) | changeFlag;
                            return;
                        }
                    }
                    posY++;
                }
                posY = diagonal.endY();
            }
        }

        /**
         * Given a position in the old list, returns the position in the new list, or
         * {@code NO_POSITION} if it was removed.
         *
         * @param oldListPosition Position of item in old list
         * @return Position of item in new list, or {@code NO_POSITION} if not present.
         * @see #NO_POSITION
         * @see #convertNewPositionToOld(int)
         */
        public int convertOldPositionToNew(@IntRange(from = 0) int oldListPosition) {
            if (oldListPosition < 0 || oldListPosition >= mOldListSize) {
                throw new IndexOutOfBoundsException("Index out of bounds - passed position = "
                        + oldListPosition + ", old list size = " + mOldListSize);
            }
            final int status = mOldItemStatuses[oldListPosition];
            if ((status & FLAG_MASK) == 0) {
                return NO_POSITION;
            } else {
                return status >> FLAG_OFFSET;
            }
        }

        /**
         * Given a position in the new list, returns the position in the old list, or
         * {@code NO_POSITION} if it was removed.
         *
         * @param newListPosition Position of item in new list
         * @return Position of item in old list, or {@code NO_POSITION} if not present.
         * @see #NO_POSITION
         * @see #convertOldPositionToNew(int)
         */
        public int convertNewPositionToOld(@IntRange(from = 0) int newListPosition) {
            if (newListPosition < 0 || newListPosition >= mNewListSize) {
                throw new IndexOutOfBoundsException("Index out of bounds - passed position = "
                        + newListPosition + ", new list size = " + mNewListSize);
            }
            final int status = mNewItemStatuses[newListPosition];
            if ((status & FLAG_MASK) == 0) {
                return NO_POSITION;
            } else {
                return status >> FLAG_OFFSET;
            }
        }

        /**
         * Dispatches the update events to the given adapter.
         * <p>
         * For example, if you have an {@link RecyclerView.Adapter Adapter}
         * that is backed by a {@link List}, you can swap the list with the new one then call this
         * method to dispatch all updates to the RecyclerView.
         * <pre>
         *     List oldList = mAdapter.getData();
         *     DiffResult result = DiffUtil.calculateDiff(new MyCallback(oldList, newList));
         *     mAdapter.setData(newList);
         *     result.dispatchUpdatesTo(mAdapter);
         * </pre>
         * <p>
         * Note that the RecyclerView requires you to dispatch adapter updates immediately when you
         * change the data (you cannot defer {@code notify*} calls). The usage above adheres to this
         * rule because updates are sent to the adapter right after the backing data is changed,
         * before RecyclerView tries to read it.
         * <p>
         * On the other hand, if you have another
         * {@link RecyclerView.AdapterDataObserver AdapterDataObserver}
         * that tries to process events synchronously, this may confuse that observer because the
         * list is instantly moved to its final state while the adapter updates are dispatched later
         * on, one by one. If you have such an
         * {@link RecyclerView.AdapterDataObserver AdapterDataObserver},
         * you can use
         * {@link #dispatchUpdatesTo(ListUpdateCallback)} to handle each modification
         * manually.
         *
         * @param adapter A RecyclerView adapter which was displaying the old list and will start
         *                displaying the new list.
         * @see AdapterListUpdateCallback
         */
        public void dispatchUpdatesTo(@NonNull final RecyclerView.Adapter adapter) {
            dispatchUpdatesTo(new AdapterListUpdateCallback(adapter));
        }

        /**
         * Dispatches update operations to the given Callback.
         * <p>
         * These updates are atomic such that the first update call affects every update call that
         * comes after it (the same as RecyclerView).
         *
         * @param updateCallback The callback to receive the update operations.
         * @see #dispatchUpdatesTo(RecyclerView.Adapter)
         */
        public void dispatchUpdatesTo(@NonNull ListUpdateCallback updateCallback) {
            final BatchingListUpdateCallback batchingCallback;

            if (updateCallback instanceof BatchingListUpdateCallback) {
                batchingCallback = (BatchingListUpdateCallback) updateCallback;
            } else {
                batchingCallback = new BatchingListUpdateCallback(updateCallback);
                // replace updateCallback with a batching callback and override references to
                // updateCallback so that we don't call it directly by mistake
                //noinspection UnusedAssignment
                updateCallback = batchingCallback;
            }
            // track up to date current list size for moves
            // when a move is found, we record its position from the end of the list (which is
            // less likely to change since we iterate in reverse).
            // Later when we find the match of that move, we dispatch the update
            int currentListSize = mOldListSize;
            // list of postponed moves
            final Collection<PostponedUpdate> postponedUpdates = new ArrayDeque<>();
            // posX and posY are exclusive
            int posX = mOldListSize;
            int posY = mNewListSize;
            // iterate from end of the list to the beginning.
            // this just makes offsets easier since changes in the earlier indices has an effect
            // on the later indices.
            for (int diagonalIndex = mDiagonals.size() - 1; diagonalIndex >= 0; diagonalIndex--) {
                final Diagonal diagonal = mDiagonals.get(diagonalIndex);
                int endX = diagonal.endX();
                int endY = diagonal.endY();
                // dispatch removals and additions until we reach to that diagonal
                // first remove then add so that it can go into its place and we don't need
                // to offset values
                while (posX > endX) {
                    posX--;
                    // REMOVAL
                    int status = mOldItemStatuses[posX];
                    if ((status & FLAG_MOVED) != 0) {
                        int newPos = status >> FLAG_OFFSET;
                        // get postponed addition
                        PostponedUpdate postponedUpdate = getPostponedUpdate(postponedUpdates,
                                newPos, false);
                        if (postponedUpdate != null) {
                            // this is an addition that was postponed. Now dispatch it.
                            int updatedNewPos = currentListSize - postponedUpdate.currentPos;
                            batchingCallback.onMoved(posX, updatedNewPos - 1);
                            if ((status & FLAG_MOVED_CHANGED) != 0) {
                                Object changePayload = mCallback.getChangePayload(posX, newPos);
                                batchingCallback.onChanged(updatedNewPos - 1, 1, changePayload);
                            }
                        } else {
                            // first time we are seeing this, we'll see a matching addition
                            postponedUpdates.add(new PostponedUpdate(
                                    posX,
                                    currentListSize - posX - 1,
                                    true
                            ));
                        }
                    } else {
                        // simple removal
                        batchingCallback.onRemoved(posX, 1);
                        currentListSize--;
                    }
                }
                while (posY > endY) {
                    posY--;
                    // ADDITION
                    int status = mNewItemStatuses[posY];
                    if ((status & FLAG_MOVED) != 0) {
                        // this is a move not an addition.
                        // see if this is postponed
                        int oldPos = status >> FLAG_OFFSET;
                        // get postponed removal
                        PostponedUpdate postponedUpdate = getPostponedUpdate(postponedUpdates,
                                oldPos, true);
                        // empty size returns 0 for indexOf
                        if (postponedUpdate == null) {
                            // postpone it until we see the removal
                            postponedUpdates.add(new PostponedUpdate(
                                    posY,
                                    currentListSize - posX,
                                    false
                            ));
                        } else {
                            // oldPosFromEnd = foundListSize - posX
                            // we can find posX if we swap the list sizes
                            // posX = listSize - oldPosFromEnd
                            int updatedOldPos = currentListSize - postponedUpdate.currentPos - 1;
                            batchingCallback.onMoved(updatedOldPos, posX);
                            if ((status & FLAG_MOVED_CHANGED) != 0) {
                                Object changePayload = mCallback.getChangePayload(oldPos, posY);
                                batchingCallback.onChanged(posX, 1, changePayload);
                            }
                        }
                    } else {
                        // simple addition
                        batchingCallback.onInserted(posX, 1);
                        currentListSize++;
                    }
                }
                // now dispatch updates for the diagonal
                posX = diagonal.x;
                posY = diagonal.y;
                for (int i = 0; i < diagonal.size; i++) {
                    // dispatch changes
                    if ((mOldItemStatuses[posX] & FLAG_MASK) == FLAG_CHANGED) {
                        Object changePayload = mCallback.getChangePayload(posX, posY);
                        batchingCallback.onChanged(posX, 1, changePayload);
                    }
                    posX++;
                    posY++;
                }
                // snap back for the next diagonal
                posX = diagonal.x;
                posY = diagonal.y;
            }
            batchingCallback.dispatchLastEvent();
        }

        @Nullable
        private static PostponedUpdate getPostponedUpdate(
                Collection<PostponedUpdate> postponedUpdates,
                int posInList,
                boolean removal) {
            PostponedUpdate postponedUpdate = null;
            Iterator<PostponedUpdate> itr = postponedUpdates.iterator();
            while (itr.hasNext()) {
                PostponedUpdate update = itr.next();
                if (update.posInOwnerList == posInList && update.removal == removal) {
                    postponedUpdate = update;
                    itr.remove();
                    break;
                }
            }
            while (itr.hasNext()) {
                // re-offset all others
                PostponedUpdate update = itr.next();
                if (removal) {
                    update.currentPos--;
                } else {
                    update.currentPos++;
                }
            }
            return postponedUpdate;
        }
    }

    /**
     * Represents an update that we skipped because it was a move.
     * <p>
     * When an update is skipped, it is tracked as other updates are dispatched until the matching
     * add/remove operation is found at which point the tracked position is used to dispatch the
     * update.
     */
    private static class PostponedUpdate {
        /**
         * position in the list that owns this item
         */
        int posInOwnerList;

        /**
         * position wrt to the end of the list
         */
        int currentPos;

        /**
         * true if this is a removal, false otherwise
         */
        boolean removal;

        PostponedUpdate(int posInOwnerList, int currentPos, boolean removal) {
            this.posInOwnerList = posInOwnerList;
            this.currentPos = currentPos;
            this.removal = removal;
        }
    }

    /**
     * Array wrapper w/ negative index support.
     * We use this array instead of a regular array so that algorithm is easier to read without
     * too many offsets when accessing the "k" array in the algorithm.
     */
    static class CenteredArray {
        private final int[] mData;
        private final int mMid;

        CenteredArray(int size) {
            mData = new int[size];
            mMid = mData.length / 2;
        }

        int get(int index) {
            return mData[index + mMid];
        }

        int[] backingData() {
            return mData;
        }

        void set(int index, int value) {
            mData[index + mMid] = value;
        }

        public void fill(int value) {
            Arrays.fill(mData, value);
        }
    }
}
