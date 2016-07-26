/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v4.widget;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import android.support.v4.view.ViewCompat.FocusRealDirection;
import android.support.v4.view.ViewCompat.FocusRelativeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Implements absolute and relative focus movement strategies. Adapted from
 * android.view.FocusFinder to work with generic collections of bounded items.
 */
class FocusStrategy {
    public static <L, T> T findNextFocusInRelativeDirection(@NonNull L focusables,
            @NonNull CollectionAdapter<L, T> collectionAdapter, @NonNull BoundsAdapter<T> adapter,
            @Nullable T focused, @FocusRelativeDirection int direction, boolean isLayoutRtl,
            boolean wrap) {
        final int count = collectionAdapter.size(focusables);
        final ArrayList<T> sortedFocusables = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            sortedFocusables.add(collectionAdapter.get(focusables, i));
        }

        final SequentialComparator<T> comparator = new SequentialComparator<>(isLayoutRtl, adapter);
        Collections.sort(sortedFocusables, comparator);

        switch (direction) {
            case View.FOCUS_FORWARD:
                return getNextFocusable(focused, sortedFocusables, wrap);
            case View.FOCUS_BACKWARD:
                return getPreviousFocusable(focused, sortedFocusables, wrap);
            default:
                throw new IllegalArgumentException("direction must be one of "
                        + "{FOCUS_FORWARD, FOCUS_BACKWARD}.");
        }
    }

    private static <T> T getNextFocusable(T focused, ArrayList<T> focusables, boolean wrap) {
        final int count = focusables.size();

        // The position of the next focusable item, which is the first item if
        // no item is currently focused.
        final int position = (focused == null ? -1 : focusables.lastIndexOf(focused)) + 1;
        if (position < count) {
            return focusables.get(position);
        } else if (wrap && count > 0) {
            return focusables.get(0);
        } else {
            return null;
        }
    }

    private static <T> T getPreviousFocusable(T focused, ArrayList<T> focusables, boolean wrap) {
        final int count = focusables.size();

        // The position of the previous focusable item, which is the last item
        // if no item is currently focused.
        final int position = (focused == null ? count : focusables.indexOf(focused)) - 1;
        if (position >= 0) {
            return focusables.get(position);
        } else if (wrap && count > 0) {
            return focusables.get(count - 1);
        } else {
            return null;
        }
    }

    /**
     * Sorts views according to their visual layout and geometry for default tab order.
     * This is used for sequential focus traversal.
     */
    private static class SequentialComparator<T> implements Comparator<T> {
        private final Rect mTemp1 = new Rect();
        private final Rect mTemp2 = new Rect();

        private final boolean mIsLayoutRtl;
        private final BoundsAdapter<T> mAdapter;

        SequentialComparator(boolean isLayoutRtl, BoundsAdapter<T> adapter) {
            mIsLayoutRtl = isLayoutRtl;
            mAdapter = adapter;
        }

        @Override
        public int compare(T first, T second) {
            final Rect firstRect = mTemp1;
            final Rect secondRect = mTemp2;

            mAdapter.obtainBounds(first, firstRect);
            mAdapter.obtainBounds(second, secondRect);

            if (firstRect.top < secondRect.top) {
                return -1;
            } else if (firstRect.top > secondRect.top) {
                return 1;
            } else if (firstRect.left < secondRect.left) {
                return mIsLayoutRtl ? 1 : -1;
            } else if (firstRect.left > secondRect.left) {
                return mIsLayoutRtl ? -1 : 1;
            } else if (firstRect.bottom < secondRect.bottom) {
                return -1;
            } else if (firstRect.bottom > secondRect.bottom) {
                return 1;
            } else if (firstRect.right < secondRect.right) {
                return mIsLayoutRtl ? 1 : -1;
            } else if (firstRect.right > secondRect.right) {
                return mIsLayoutRtl ? -1 : 1;
            } else {
                // The view are distinct but completely coincident so we
                // consider them equal for our purposes. Since the sort is
                // stable, this means that the views will retain their
                // layout order relative to one another.
                return 0;
            }
        }
    }

    public static <L, T> T findNextFocusInAbsoluteDirection(@NonNull L focusables,
            @NonNull CollectionAdapter<L, T> collectionAdapter, @NonNull BoundsAdapter<T> adapter,
            @Nullable T focused, @NonNull Rect focusedRect, int direction) {
        // Initialize the best candidate to something impossible so that
        // the first plausible view will become the best choice.
        final Rect bestCandidateRect = new Rect(focusedRect);

        switch (direction) {
            case View.FOCUS_LEFT:
                bestCandidateRect.offset(focusedRect.width() + 1, 0);
                break;
            case View.FOCUS_RIGHT:
                bestCandidateRect.offset(-(focusedRect.width() + 1), 0);
                break;
            case View.FOCUS_UP:
                bestCandidateRect.offset(0, focusedRect.height() + 1);
                break;
            case View.FOCUS_DOWN:
                bestCandidateRect.offset(0, -(focusedRect.height() + 1));
                break;
            default:
                throw new IllegalArgumentException("direction must be one of "
                        + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
        }

        T closest = null;

        final int count = collectionAdapter.size(focusables);
        final Rect focusableRect = new Rect();
        for (int i = 0; i < count; i++) {
            final T focusable = collectionAdapter.get(focusables, i);
            if (focusable == focused) {
                continue;
            }

            // get focus bounds of other view
            adapter.obtainBounds(focusable, focusableRect);
            if (isBetterCandidate(direction, focusedRect, focusableRect, bestCandidateRect)) {
                bestCandidateRect.set(focusableRect);
                closest = focusable;
            }
        }

        return closest;
    }

    /**
     * Is candidate a better candidate than currentBest for a focus search
     * in a particular direction from a source rect? This is the core
     * routine that determines the order of focus searching.
     *
     * @param direction   the direction (up, down, left, right)
     * @param source      the source from which we are searching
     * @param candidate   the candidate rectangle
     * @param currentBest the current best rectangle
     * @return {@code true} if the candidate rectangle is a better than the
     * current best rectangle, {@code false} otherwise
     */
    private static boolean isBetterCandidate(
            @FocusRealDirection int direction, @NonNull Rect source,
            @NonNull Rect candidate, @NonNull Rect currentBest) {
        // To be a better candidate, need to at least be a candidate in the
        // first place. :)
        if (!isCandidate(source, candidate, direction)) {
            return false;
        }

        // We know that candidateRect is a candidate. If currentBest is not
        // a candidate, candidateRect is better.
        if (!isCandidate(source, currentBest, direction)) {
            return true;
        }

        // If candidateRect is better by beam, it wins.
        if (beamBeats(direction, source, candidate, currentBest)) {
            return true;
        }

        // If currentBest is better, then candidateRect cant' be. :)
        if (beamBeats(direction, source, currentBest, candidate)) {
            return false;
        }

        // Otherwise, do fudge-tastic comparison of the major and minor
        // axis.
        final int candidateDist = getWeightedDistanceFor(
                majorAxisDistance(direction, source, candidate),
                minorAxisDistance(direction, source, candidate));
        final int currentBestDist = getWeightedDistanceFor(
                majorAxisDistance(direction, source, currentBest),
                minorAxisDistance(direction, source, currentBest));
        return candidateDist < currentBestDist;
    }

    /**
     * One rectangle may be another candidate than another by virtue of
     * being exclusively in the beam of the source rect.
     *
     * @return whether rect1 is a better candidate than rect2 by virtue of
     * it being in source's beam
     */
    private static boolean beamBeats(@FocusRealDirection int direction,
            @NonNull Rect source, @NonNull Rect rect1, @NonNull Rect rect2) {
        final boolean rect1InSrcBeam = beamsOverlap(direction, source, rect1);
        final boolean rect2InSrcBeam = beamsOverlap(direction, source, rect2);

        // If rect1 isn't exclusively in the src beam, it doesn't win.
        if (rect2InSrcBeam || !rect1InSrcBeam) {
            return false;
        }

        // We know rect1 is in the beam, and rect2 is not.

        // If rect1 is to the direction of, and rect2 is not, rect1 wins.
        // For example, for direction left, if rect1 is to the left of the
        // source and rect2 is below, then we always prefer the in beam
        // rect1, since rect2 could be reached by going down.
        if (!isToDirectionOf(direction, source, rect2)) {
            return true;
        }

        // For horizontal directions, being exclusively in beam always
        // wins.
        if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) {
            return true;
        }

        // For vertical directions, beams only beat up to a point: now, as
        // long as rect2 isn't completely closer, rect1 wins, e.g. for
        // direction down, completely closer means for rect2's top edge to
        // be closer to the source's top edge than rect1's bottom edge.
        return majorAxisDistance(direction, source, rect1)
                < majorAxisDistanceToFarEdge(direction, source, rect2);
    }

    /**
     * Fudge-factor opportunity: how to calculate distance given major and
     * minor axis distances.
     * <p/>
     * Warning: this fudge factor is finely tuned, be sure to run all focus
     * tests if you dare tweak it.
     */
    private static int getWeightedDistanceFor(int majorAxisDistance, int minorAxisDistance) {
        return 13 * majorAxisDistance * majorAxisDistance
                + minorAxisDistance * minorAxisDistance;
    }

    /**
     * Is destRect a candidate for the next focus given the direction? This
     * checks whether the dest is at least partially to the direction of
     * (e.g. left of) from source.
     * <p/>
     * Includes an edge case for an empty rect,which is used in some cases
     * when searching from a point on the screen.
     */
    private static boolean isCandidate(@NonNull Rect srcRect, @NonNull Rect destRect,
            @FocusRealDirection int direction) {
        switch (direction) {
            case View.FOCUS_LEFT:
                return (srcRect.right > destRect.right || srcRect.left >= destRect.right)
                        && srcRect.left > destRect.left;
            case View.FOCUS_RIGHT:
                return (srcRect.left < destRect.left || srcRect.right <= destRect.left)
                        && srcRect.right < destRect.right;
            case View.FOCUS_UP:
                return (srcRect.bottom > destRect.bottom || srcRect.top >= destRect.bottom)
                        && srcRect.top > destRect.top;
            case View.FOCUS_DOWN:
                return (srcRect.top < destRect.top || srcRect.bottom <= destRect.top)
                        && srcRect.bottom < destRect.bottom;
        }
        throw new IllegalArgumentException("direction must be one of "
                + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
    }


    /**
     * Do the "beams" w.r.t the given direction's axis of rect1 and rect2 overlap?
     *
     * @param direction the direction (up, down, left, right)
     * @param rect1     the first rectangle
     * @param rect2     the second rectangle
     * @return whether the beams overlap
     */
    private static boolean beamsOverlap(@FocusRealDirection int direction,
            @NonNull Rect rect1, @NonNull Rect rect2) {
        switch (direction) {
            case View.FOCUS_LEFT:
            case View.FOCUS_RIGHT:
                return (rect2.bottom >= rect1.top) && (rect2.top <= rect1.bottom);
            case View.FOCUS_UP:
            case View.FOCUS_DOWN:
                return (rect2.right >= rect1.left) && (rect2.left <= rect1.right);
        }
        throw new IllegalArgumentException("direction must be one of "
                + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
    }

    /**
     * e.g for left, is 'to left of'
     */
    private static boolean isToDirectionOf(@FocusRealDirection int direction,
            @NonNull Rect src, @NonNull Rect dest) {
        switch (direction) {
            case View.FOCUS_LEFT:
                return src.left >= dest.right;
            case View.FOCUS_RIGHT:
                return src.right <= dest.left;
            case View.FOCUS_UP:
                return src.top >= dest.bottom;
            case View.FOCUS_DOWN:
                return src.bottom <= dest.top;
        }
        throw new IllegalArgumentException("direction must be one of "
                + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
    }

    /**
     * @return the distance from the edge furthest in the given direction
     * of source to the edge nearest in the given direction of
     * dest. If the dest is not in the direction from source,
     * returns 0.
     */
    private static int majorAxisDistance(@FocusRealDirection int direction,
            @NonNull Rect source, @NonNull Rect dest) {
        return Math.max(0, majorAxisDistanceRaw(direction, source, dest));
    }

    private static int majorAxisDistanceRaw(@FocusRealDirection int direction,
            @NonNull Rect source, @NonNull Rect dest) {
        switch (direction) {
            case View.FOCUS_LEFT:
                return source.left - dest.right;
            case View.FOCUS_RIGHT:
                return dest.left - source.right;
            case View.FOCUS_UP:
                return source.top - dest.bottom;
            case View.FOCUS_DOWN:
                return dest.top - source.bottom;
        }
        throw new IllegalArgumentException("direction must be one of "
                + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
    }

    /**
     * @return the distance along the major axis w.r.t the direction from
     * the edge of source to the far edge of dest. If the dest is
     * not in the direction from source, returns 1 to break ties
     * with {@link #majorAxisDistance}.
     */
    private static int majorAxisDistanceToFarEdge(@FocusRealDirection int direction,
            @NonNull Rect source, @NonNull Rect dest) {
        return Math.max(1, majorAxisDistanceToFarEdgeRaw(direction, source, dest));
    }

    private static int majorAxisDistanceToFarEdgeRaw(
            @FocusRealDirection int direction, @NonNull Rect source,
            @NonNull Rect dest) {
        switch (direction) {
            case View.FOCUS_LEFT:
                return source.left - dest.left;
            case View.FOCUS_RIGHT:
                return dest.right - source.right;
            case View.FOCUS_UP:
                return source.top - dest.top;
            case View.FOCUS_DOWN:
                return dest.bottom - source.bottom;
        }
        throw new IllegalArgumentException("direction must be one of "
                + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
    }

    /**
     * Finds the distance on the minor axis w.r.t the direction to the
     * nearest edge of the destination rectangle.
     *
     * @param direction the direction (up, down, left, right)
     * @param source the source rect
     * @param dest the destination rect
     * @return the distance
     */
    private static int minorAxisDistance(@FocusRealDirection int direction, @NonNull Rect source,
            @NonNull Rect dest) {
        switch (direction) {
            case View.FOCUS_LEFT:
            case View.FOCUS_RIGHT:
                // the distance between the center verticals
                return Math.abs(
                        ((source.top + source.height() / 2) - ((dest.top + dest.height() / 2))));
            case View.FOCUS_UP:
            case View.FOCUS_DOWN:
                // the distance between the center horizontals
                return Math.abs(
                        ((source.left + source.width() / 2) -
                                ((dest.left + dest.width() / 2))));
        }
        throw new IllegalArgumentException("direction must be one of "
                + "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
    }

    /**
     * Adapter used to obtain bounds from a generic data type.
     */
    public interface BoundsAdapter<T> {
        void obtainBounds(T data, Rect outBounds);
    }

    /**
     * Adapter used to obtain items from a generic collection type.
     */
    public interface CollectionAdapter<T, V> {
        V get(T collection, int index);
        int size(T collection);
    }
}
