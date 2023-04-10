/*
 * Copyright (C) 2014 The Android Open Source Project
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

package androidx.test.uiautomator;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Provides utility methods for searching the {@link AccessibilityNodeInfo} hierarchy for nodes
 * that match {@link BySelector}s.
 */
class ByMatcher {

    private static final String TAG = ByMatcher.class.getSimpleName();

    private final UiDevice mDevice;
    private final BySelector mSelector;
    private final boolean mShortCircuit;

    /**
     * Constructs a {@link ByMatcher} instance to store parameters for recursive searches.
     *
     * @param selector     search criteria
     * @param shortCircuit true to stop searching when the first match is found
     */
    private ByMatcher(UiDevice device, BySelector selector, boolean shortCircuit) {
        mDevice = device;
        mSelector = selector;
        mShortCircuit = shortCircuit;
    }

    /**
     * Searches the hierarchy under each root for a node that matches the selector.
     * <p>Note: call {@link AccessibilityNodeInfo#recycle()} when done to avoid leaking resources.
     */
    static AccessibilityNodeInfo findMatch(UiDevice device, BySelector selector,
            AccessibilityNodeInfo... roots) {
        ByMatcher matcher = new ByMatcher(device, selector, true);
        for (AccessibilityNodeInfo root : roots) {
            List<AccessibilityNodeInfo> matches = matcher.findMatches(root);
            if (!matches.isEmpty()) {
                return matches.get(0);
            }
        }
        return null;
    }

    /**
     * Searches the hierarchy under each root for nodes that match the selector.
     * <p>Note: call {@link AccessibilityNodeInfo#recycle()} when done to avoid leaking resources.
     */
    static List<AccessibilityNodeInfo> findMatches(UiDevice device, BySelector selector,
            AccessibilityNodeInfo... roots) {
        List<AccessibilityNodeInfo> ret = new ArrayList<>();
        ByMatcher matcher = new ByMatcher(device, selector, false);
        for (AccessibilityNodeInfo root : roots) {
            ret.addAll(matcher.findMatches(root));
        }
        return ret;
    }

    /** Searches the hierarchy under the root for nodes that match the selector. */
    private List<AccessibilityNodeInfo> findMatches(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> ret = findMatches(root, 0, new PartialMatchList());
        if (ret.isEmpty()) {
            // No matches found, run watchers and retry.
            mDevice.runWatchers();
            ret = findMatches(root, 0, new PartialMatchList());
        }
        return ret;
    }

    /**
     * Recursively searches a node's hierarchy for nodes that match the search criteria.
     *
     * @param node           node to search
     * @param depth          distance between the node and the search root
     * @param partialMatches list of potential matches to track
     * @return list of matching nodes
     */
    private List<AccessibilityNodeInfo> findMatches(AccessibilityNodeInfo node, int depth,
            PartialMatchList partialMatches) {
        List<AccessibilityNodeInfo> ret = new ArrayList<>();

        // Don't bother searching the subtree if it is not visible
        if (!node.isVisibleToUser()) {
            Log.v(TAG, String.format("Skipping invisible child: %s", node));
            return ret;
        }

        // Update partial matches
        for (PartialMatch partialMatch : partialMatches) {
            partialMatches = partialMatch.matchChildren(node, depth, partialMatches);
        }

        // Create a new match, if necessary
        PartialMatch currentMatch = PartialMatch.create(mSelector, node, depth, depth);
        if (currentMatch != null) {
            partialMatches = partialMatches.prepend(currentMatch);
        }

        // For each child
        int numChildren = node.getChildCount();
        boolean hasNullChild = false;
        for (int i = 0; i < numChildren; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                if (!hasNullChild) {
                    Log.w(TAG, String.format("Node returned null child: %s", node));
                }
                hasNullChild = true;
                Log.w(TAG, String.format("Skipping null child (%s of %s)", i, numChildren));
                continue;
            }

            // Add any matches found under the child subtree
            ret.addAll(findMatches(child, depth + 1, partialMatches));

            // We're done with the child
            child.recycle();

            // Return early if we sound a match and shortCircuit is true
            if (!ret.isEmpty() && mShortCircuit) {
                return ret;
            }
        }

        // Finalize match, if necessary
        if (currentMatch != null && currentMatch.isComplete()) {
            ret.add(AccessibilityNodeInfo.obtain(node));
        }

        return ret;
    }

    /**
     * Represents a potential match with a {@link BySelector}. The attributes of the selector were
     * matched, but its child selectors may not have been matched.
     */
    private static class PartialMatch {
        private final BySelector mMatchSelector;
        private final int mMatchDepth;
        private final List<PartialMatch> mChildMatches = new ArrayList<>();

        private PartialMatch(BySelector selector, int depth) {
            mMatchSelector = selector;
            mMatchDepth = depth;
        }

        /**
         * Creates a match if the provided selector matches the node.
         *
         * @param selector      search criteria
         * @param node          node to check
         * @param absoluteDepth distance between the node and the search root
         * @param relativeDepth distance between the node and its relevant ancestor
         * @return potential match or null
         */
        static PartialMatch create(BySelector selector, AccessibilityNodeInfo node,
                int absoluteDepth, int relativeDepth) {
            if (!matchesSelector(selector, node, relativeDepth)) {
                return null;
            }
            return new PartialMatch(selector, absoluteDepth);
        }

        /**
         * Returns true if the node matches the selector, ignoring child selectors.
         *
         * @param selector search criteria to match
         * @param node node to check
         * @param depth distance between the node and its relevant ancestor
         */
        private static boolean matchesSelector(
                BySelector selector, AccessibilityNodeInfo node, int depth) {
            return (selector.mMinDepth == null || depth >= selector.mMinDepth)
                    && (selector.mMaxDepth == null || depth <= selector.mMaxDepth)
                    && matchesCriteria(selector.mClazz, node.getClassName())
                    && matchesCriteria(selector.mDesc, node.getContentDescription())
                    && matchesCriteria(selector.mPkg, node.getPackageName())
                    && matchesCriteria(selector.mRes, node.getViewIdResourceName())
                    && matchesCriteria(selector.mText, node.getText())
                    && matchesCriteria(selector.mChecked, node.isChecked())
                    && matchesCriteria(selector.mCheckable, node.isCheckable())
                    && matchesCriteria(selector.mClickable, node.isClickable())
                    && matchesCriteria(selector.mEnabled, node.isEnabled())
                    && matchesCriteria(selector.mFocused, node.isFocused())
                    && matchesCriteria(selector.mFocusable, node.isFocusable())
                    && matchesCriteria(selector.mLongClickable, node.isLongClickable())
                    && matchesCriteria(selector.mScrollable, node.isScrollable())
                    && matchesCriteria(selector.mSelected, node.isSelected());
        }

        /** Returns true if the criteria is null or matches the value. */
        private static boolean matchesCriteria(Pattern criteria, CharSequence value) {
            if (criteria == null) {
                return true;
            }
            return criteria.matcher(value != null ? value : "").matches();
        }

        /** Returns true if the criteria is null or equal to the value. */
        private static boolean matchesCriteria(Boolean criteria, boolean value) {
            return criteria == null || criteria.equals(value);
        }

        /**
         * Checks whether a node matches any child selector. Creates a child match if it does and
         * adds it to the list of tracked matches.
         *
         * @param node           node to check
         * @param depth          distance between the node and the search root
         * @param partialMatches list of matches to track
         * @return new list of matches to track
         */
        PartialMatchList matchChildren(AccessibilityNodeInfo node, int depth,
                PartialMatchList partialMatches) {
            for (BySelector childSelector : mMatchSelector.mChildSelectors) {
                PartialMatch pm = PartialMatch.create(
                        childSelector, node, depth, depth - mMatchDepth);
                if (pm != null) {
                    mChildMatches.add(pm);
                    partialMatches = partialMatches.prepend(pm);
                }
            }
            return partialMatches;
        }

        /** Returns true if all child selectors were matched. */
        boolean isComplete() {
            Set<BySelector> matches = new HashSet<>();
            for (PartialMatch pm : mChildMatches) {
                if (pm.isComplete()) {
                    matches.add(pm.mMatchSelector);
                }
            }
            return matches.containsAll(mMatchSelector.mChildSelectors);
        }
    }

    /** Immutable singly-linked list of matches that is safe for tree traversal. */
    private static class PartialMatchList implements Iterable<PartialMatch> {

        final Node mHead;

        PartialMatchList() {
            this(null);
        }

        private PartialMatchList(Node head) {
            mHead = head;
        }

        /** Returns a new list obtained by prepending a match. */
        PartialMatchList prepend(PartialMatch match) {
            return new PartialMatchList(new Node(match, mHead));
        }

        @Override
        @NonNull
        public Iterator<PartialMatch> iterator() {
            return new Iterator<PartialMatch>() {
                private Node mNext = mHead;

                @Override
                public boolean hasNext() {
                    return mNext != null;
                }

                @Override
                public PartialMatch next() {
                    PartialMatch match = mNext.mMatch;
                    mNext = mNext.mNext;
                    return match;
                }
            };
        }

        private static class Node {
            final PartialMatch mMatch;
            final Node mNext;

            Node(PartialMatch match, Node next) {
                mMatch = match;
                mNext = next;
            }
        }
    }
}
