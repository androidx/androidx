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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A utility class which provides static methods for searching the {@link AccessibilityNodeInfo}
 * hierarchy for nodes that match {@link BySelector} criteria.
 */
class ByMatcher {

    private static final String TAG = ByMatcher.class.getSimpleName();

    private UiDevice mDevice;
    private BySelector mSelector;
    private boolean mShortCircuit;

    /**
     * Constructs a new {@link ByMatcher} instance. Used by
     * {@link ByMatcher#findMatch(UiDevice, BySelector, AccessibilityNodeInfo...)} to store state information
     * that does not change during recursive calls.
     *
     * @param selector The criteria used to determine if a {@link AccessibilityNodeInfo} is a match.
     * @param shortCircuit If true, this method will return early when the first match is found.
     */
    private ByMatcher(UiDevice device, BySelector selector, boolean shortCircuit) {
        mDevice = device;
        mSelector = selector;
        mShortCircuit = shortCircuit;
    }

    /**
     * Traverses the {@link AccessibilityNodeInfo} hierarchy starting at each {@code root} and
     * returns the first node to match the {@code selector} criteria. <br />
     * <strong>Note:</strong> The caller must release the {@link AccessibilityNodeInfo} instance by
     * calling {@link AccessibilityNodeInfo#recycle()} to avoid leaking resources.
     *
     * @param device A reference to the {@link UiDevice}.
     * @param selector The {@link BySelector} criteria used to determine if a node is a match.
     * @return The first {@link AccessibilityNodeInfo} which matched the search criteria.
     */
    static AccessibilityNodeInfo findMatch(UiDevice device, BySelector selector,
            AccessibilityNodeInfo... roots) {

        // TODO: Don't short-circuit when debugging, and warn if more than one match.
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
     * Traverses the {@link AccessibilityNodeInfo} hierarchy starting at each {@code root} and
     * returns a list of nodes which match the {@code selector} criteria. <br />
     * <strong>Note:</strong> The caller must release each {@link AccessibilityNodeInfo} instance
     * by calling {@link AccessibilityNodeInfo#recycle()} to avoid leaking resources.
     *
     * @param device A reference to the {@link UiDevice}.
     * @param selector The {@link BySelector} criteria used to determine if a node is a match.
     * @return A list containing all of the nodes which matched the search criteria.
     */
    static List<AccessibilityNodeInfo> findMatches(UiDevice device, BySelector selector,
            AccessibilityNodeInfo... roots) {

        List<AccessibilityNodeInfo> ret = new ArrayList<AccessibilityNodeInfo>();
        ByMatcher matcher = new ByMatcher(device, selector, false);
        for (AccessibilityNodeInfo root : roots) {
            ret.addAll(matcher.findMatches(root));
        }
        return ret;
    }

    /**
     * Traverses the {@link AccessibilityNodeInfo} hierarchy starting at {@code root}, and returns
     * a list of nodes which match the {@code selector} criteria. <br />
     * <strong>Note:</strong> The caller must release each {@link AccessibilityNodeInfo} instance
     * by calling {@link AccessibilityNodeInfo#recycle()} to avoid leaking resources.
     *
     * @param root The root {@link AccessibilityNodeInfo} from which to start the search.
     * @return A list containing all of the nodes which matched the search criteria.
     */
    private List<AccessibilityNodeInfo> findMatches(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> ret =
                findMatches(root, 0, 0, new SinglyLinkedList<PartialMatch>());

        // If no matches were found
        if (ret.isEmpty()) {
            // Run watchers and retry
            mDevice.runWatchers();
            ret = findMatches(root, 0, 0, new SinglyLinkedList<PartialMatch>());
        }

        return ret;
    }

    /**
     * Traverses the {@link AccessibilityNodeInfo} hierarchy starting at {@code node}, and returns
     * a list of nodes which match the {@code selector} criteria. <br />
     * <strong>Note:</strong> The caller must release each {@link AccessibilityNodeInfo} instance
     * by calling {@link AccessibilityNodeInfo#recycle()} to avoid leaking resources.
     *
     * @param node The root of the {@link AccessibilityNodeInfo} subtree we are currently searching.
     * @param index The index of this node underneath its parent.
     * @param depth The distance between {@code node} and the root node.
     * @param partialMatches The current list of {@link PartialMatch}es that need to be updated.
     * @return A {@link List} of {@link AccessibilityNodeInfo}s that meet the search criteria.
     */
    private List<AccessibilityNodeInfo> findMatches(AccessibilityNodeInfo node,
            int index, int depth, SinglyLinkedList<PartialMatch> partialMatches) {
        try {
            return findMatchesRecursively(node, index, depth, partialMatches);
        } catch (RuntimeException e) {
            throw newRecursionError(node, e);
        } catch (Error e) {
            throw newRecursionError(node, e);
        }
    }

    private IllegalStateException newRecursionError(AccessibilityNodeInfo node, Throwable t) {
        return new IllegalStateException(String.format("Recursion error for node =%s", node), t);
    }

    private List<AccessibilityNodeInfo> findMatchesRecursively(AccessibilityNodeInfo node,
            int index, int depth, SinglyLinkedList<PartialMatch> partialMatches) {
        List<AccessibilityNodeInfo> ret = new ArrayList<AccessibilityNodeInfo>();

        // Don't bother searching the subtree if it is not visible
        if (!node.isVisibleToUser()) {
            return ret;
        }

        // Update partial matches
        for (PartialMatch partialMatch : partialMatches) {
            partialMatches = partialMatch.update(node, index, depth, partialMatches);
        }

        // Create a new match, if necessary
        PartialMatch currentMatch = PartialMatch.accept(node, mSelector, index, depth);
        if (currentMatch != null) {
            partialMatches = SinglyLinkedList.prepend(currentMatch, partialMatches);
        }

        // For each child
        int numChildren = node.getChildCount();
        boolean hasNullChild = false;
        for (int i = 0; i < numChildren; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                if (!hasNullChild) {
                    Log.w(TAG, String.format("Node returned null child: %s", node.toString()));
                }
                hasNullChild = true;
                Log.w(TAG, String.format("Skipping null child (%s of %s)", i, numChildren));
                continue;
            }

            // Add any matches found under the child subtree
            ret.addAll(findMatchesRecursively(child, i, depth + 1, partialMatches));

            // We're done with the child
            child.recycle();

            // Return early if we sound a match and shortCircuit is true
            if (!ret.isEmpty() && mShortCircuit) {
                return ret;
            }
        }

        // Finalize match, if necessary
        if (currentMatch != null && currentMatch.finalizeMatch()) {
            ret.add(AccessibilityNodeInfo.obtain(node));
        }

        return ret;
    }

    /** Helper method used to evaluate a {@link Pattern} criteria if it is set. */
    static boolean checkCriteria(Pattern criteria, CharSequence value) {
        if (criteria == null) {
            return true;
        }
        return criteria.matcher(value != null ? value : "").matches();
    }

    /** Helper method used to evaluate a {@link Boolean} criteria if it is set. */
    static boolean checkCriteria(Boolean criteria, boolean value) {
        if (criteria == null) {
            return true;
        }
        return criteria.equals(value);
    }

    /**
     * A {@link PartialMatch} instance represents a potential match against the given
     * {@link BySelector}. Attributes of the current node itself have been evaluated, but any child
     * selectors have not, since we must first visit the subtree under the node before we can tell
     * if the child selectors were matched.
     */
    static private class PartialMatch {
        private final int matchDepth;
        private final BySelector matchSelector;
        private final List<PartialMatch> partialMatches = new ArrayList<PartialMatch>();

        /**
         * Private constructor. Should be instanciated by calling the
         * {@link PartialMatch#accept(AccessibilityNodeInfo, BySelector, int, int)} factory method.
         */
        private PartialMatch(BySelector selector, int depth) {
            matchSelector = selector;
            matchDepth = depth;
        }

        /**
         * Factory method which returns a new {@link PartialMatch} if the node partially matches
         * the {@code selector}, otherwise returns null.
         *
         * @param node The node to check.
         * @param selector The criteria used to evaluate the node.
         * @param index The index of this node underneath its parent.
         * @param depth The distance between {@code node} and the root node.
         * @return A {@link PartialMatch} instance if the node matches all non-child selector
         * criteria, otherwise null.
         */
        public static PartialMatch accept(AccessibilityNodeInfo node, BySelector selector,
                int index, int depth) {
            return accept(node, selector, index, depth, depth);
        }

        /**
         * Factory method which returns a new {@link PartialMatch} if the node partially matches
         * the {@code selector}, otherwise returns null.
         *
         * @param node The node to check.
         * @param selector The criteria used to evaluate the node.
         * @param index The index of this node underneath its parent.
         * @param absoluteDepth The distance between {@code node} and the root node.
         * @param relativeDepth The distance between {@code node} and the matching ancestor.
         * @return A {@link PartialMatch} instance if the node matches all non-child selector
         * criteria, otherwise null.
         */
        public static PartialMatch accept(AccessibilityNodeInfo node, BySelector selector,
                int index, int absoluteDepth, int relativeDepth) {

            if ((selector.mMinDepth != null && relativeDepth < selector.mMinDepth) ||
                    (selector.mMaxDepth != null && relativeDepth > selector.mMaxDepth)) {
                return null;
            }

            // NB: index is not checked, as it is not a BySelector criteria (yet). Keeping the
            // parameter in place in case matching on index is really needed.

            PartialMatch ret = null;
            if (checkCriteria(selector.mClazz, node.getClassName()) &&
                    checkCriteria(selector.mDesc, node.getContentDescription()) &&
                    checkCriteria(selector.mPkg, node.getPackageName()) &&
                    checkCriteria(selector.mRes, node.getViewIdResourceName()) &&
                    checkCriteria(selector.mText, node.getText()) &&
                    checkCriteria(selector.mChecked, node.isChecked()) &&
                    checkCriteria(selector.mCheckable, node.isCheckable()) &&
                    checkCriteria(selector.mClickable, node.isClickable()) &&
                    checkCriteria(selector.mEnabled, node.isEnabled()) &&
                    checkCriteria(selector.mFocused, node.isFocused()) &&
                    checkCriteria(selector.mFocusable, node.isFocusable()) &&
                    checkCriteria(selector.mLongClickable, node.isLongClickable()) &&
                    checkCriteria(selector.mScrollable, node.isScrollable()) &&
                    checkCriteria(selector.mSelected, node.isSelected())) {

                ret = new PartialMatch(selector, absoluteDepth);
            }
            return ret;
        }

        /**
         * Updates this {@link PartialMatch} as part of the tree traversal. Checks to see if
         * {@code node} matches any of the child selectors that need to match for this
         * {@link PartialMatch} to be considered a full match.
         *
         * @param node The node to process.
         * @param index The index of this node underneath its parent.
         * @param depth The distance between {@code node} and the root node.
         * @param rest The list of {@link PartialMatch}es that our caller is currently tracking
         * @return The list of {@link PartialMatch}es that our caller should track while traversing
         * the subtree under this node.
         */
        public SinglyLinkedList<PartialMatch> update(AccessibilityNodeInfo node,
                int index, int depth, SinglyLinkedList<PartialMatch> rest) {

            // Check if this node matches any of our children
            for (BySelector childSelector : matchSelector.mChildSelectors) {
                PartialMatch m = PartialMatch.accept(node, childSelector, index, depth,
                        depth - matchDepth);
                if (m != null) {
                    partialMatches.add(m);
                    rest = SinglyLinkedList.prepend(m, rest);
                }
            }
            return rest;
        }

        /**
         * Finalizes this {@link PartialMatch} and returns true if it was a full match, or false
         * otherwise.
         */
        public boolean finalizeMatch() {
            // Find out which of our child selectors were fully matched
            Set<BySelector> matches = new HashSet<BySelector>();
            for (PartialMatch p : partialMatches) {
                if (p.finalizeMatch()) {
                    matches.add(p.matchSelector);
                }
            }

            // Return true if matches were found for all of the child selectors
            return matches.containsAll(matchSelector.mChildSelectors);
        }
    }

    /**
     * Immutable, singly-linked List. Used to keep track of the {@link PartialMatch}es that we
     * need to update when visiting nodes.
     */
    private static class SinglyLinkedList<T> implements Iterable<T> {

        final Node<T> mHead;

        /** Constructs an empty list. */
        public SinglyLinkedList() {
            this(null);
        }

        private SinglyLinkedList(Node<T> head) {
            mHead = head;
        }

        /** Returns a new list obtained by prepending {@code data} to {@code rest}. */
        public static <T> SinglyLinkedList<T> prepend(T data, SinglyLinkedList<T> rest) {
            return new SinglyLinkedList<T>(new Node<T>(data, rest.mHead));
        }

        @SuppressWarnings("MissingOverride")
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                private Node<T> mNext = mHead;

                @Override
                public boolean hasNext() {
                    return mNext != null;
                }

                @Override
                public T next() {
                    T ret = mNext.data;
                    mNext = mNext.next;
                    return ret;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        private static class Node<T> {
            public final T data;
            public final Node<T> next;

            public Node(T d, Node<T> n) {
                data = d;
                next = n;
            }
        }
    }
}
