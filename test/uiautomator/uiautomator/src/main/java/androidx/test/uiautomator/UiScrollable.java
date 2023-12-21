/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

/**
 * UiScrollable is a {@link UiCollection} and provides support for searching
 * for items in scrollable layout elements. This class can be used with
 * horizontally or vertically scrollable controls.
 */
public class UiScrollable extends UiCollection {
    private static final String TAG = UiScrollable.class.getSimpleName();

    // More steps slows the swipe and prevents contents from being flung too far
    private static final int SCROLL_STEPS = 55;

    private static final int FLING_STEPS = 5;

    // Restrict a swipe's starting and ending points inside a 10% margin of the target
    private static final double DEFAULT_SWIPE_DEADZONE_PCT = 0.1;

    // Limits the number of swipes/scrolls performed during a search
    private static int mMaxSearchSwipes = 30;

    // Used in ScrollForward() and ScrollBackward() to determine swipe direction
    private boolean mIsVerticalList = true;

    private double mSwipeDeadZonePercentage = DEFAULT_SWIPE_DEADZONE_PCT;

    private final Configurator mConfig = Configurator.getInstance();

    /**
     * Constructor.
     *
     * @param container a {@link UiSelector} selector to identify the scrollable
     *     layout element.
     */
    public UiScrollable(@NonNull UiSelector container) {
        // wrap the container selector with container so that QueryController can handle
        // this type of enumeration search accordingly
        super(container);
    }

    /**
     * Set the direction of swipes to be vertical when performing scroll actions.
     * @return reference to itself
     */
    @NonNull
    public UiScrollable setAsVerticalList() {
        mIsVerticalList = true;
        return this;
    }

    /**
     * Set the direction of swipes to be horizontal when performing scroll actions.
     * @return reference to itself
     */
    @NonNull
    public UiScrollable setAsHorizontalList() {
        mIsVerticalList = false;
        return this;
    }

    /**
     * Used privately when performing swipe searches to decide if an element has become
     * visible or not.
     *
     * @param selector
     * @return true if found else false
     */
    protected boolean exists(@NonNull UiSelector selector) {
        return getQueryController().findAccessibilityNodeInfo(selector) != null;
    }

    /**
     * Searches for a child element in the present scrollable container.
     * The search first looks for a child element that matches the selector
     * you provided, then looks for the content-description in its children elements.
     * If both search conditions are fulfilled, the method returns a {@link UiObject}
     * representing the element matching the selector (not the child element in its
     * subhierarchy containing the content-description). By default, this method performs a
     * scroll search.
     * See {@link #getChildByDescription(UiSelector, String, boolean)}
     *
     * @param childPattern {@link UiSelector} for a child in a scollable layout element
     * @param text Content-description to find in the children of
     * the <code>childPattern</code> match
     * @return {@link UiObject} representing the child element that matches the search conditions
     * @throws UiObjectNotFoundException
     */
    @NonNull
    @Override
    public UiObject getChildByDescription(
            @NonNull UiSelector childPattern, @NonNull String text)
            throws UiObjectNotFoundException {
        return getChildByDescription(childPattern, text, true);
    }

    /**
     * Searches for a child element in the present scrollable container.
     * The search first looks for a child element that matches the selector
     * you provided, then looks for the content-description in its children elements.
     * If both search conditions are fulfilled, the method returns a {@link UiObject}
     * representing the element matching the selector (not the child element in its
     * subhierarchy containing the content-description).
     *
     * @param childPattern {@link UiSelector} for a child in a scollable layout element
     * @param text Content-description to find in the children of 
     * the <code>childPattern</code> match (may be a partial match)
     * @param allowScrollSearch set to true if scrolling is allowed
     * @return {@link UiObject} representing the child element that matches the search conditions
     * @throws UiObjectNotFoundException
     */
    @NonNull
    public UiObject getChildByDescription(@NonNull UiSelector childPattern, @NonNull String text,
            boolean allowScrollSearch) throws UiObjectNotFoundException {
        if (text != null) {
            if (allowScrollSearch) {
                scrollIntoView(new UiSelector().descriptionContains(text));
            }
            return super.getChildByDescription(childPattern, text);
        }
        throw new UiObjectNotFoundException("for description= \"" + text + "\"");
    }

    /**
     * Searches for a child element in the present scrollable container that
     * matches the selector you provided. The search is performed without
     * scrolling and only on visible elements.
     *
     * @param childPattern {@link UiSelector} for a child in a scollable layout element
     * @param instance int number representing the occurance of 
     * a <code>childPattern</code> match
     * @return {@link UiObject} representing the child element that matches the search conditions
     */
    @NonNull
    @Override
    public UiObject getChildByInstance(@NonNull UiSelector childPattern, int instance)
            throws UiObjectNotFoundException {
        UiSelector patternSelector = UiSelector.patternBuilder(getSelector(),
                UiSelector.patternBuilder(childPattern).instance(instance));
        return new UiObject(patternSelector);
    }

    /**
     * Searches for a child element in the present scrollable
     * container. The search first looks for a child element that matches the
     * selector you provided, then looks for the text in its children elements.
     * If both search conditions are fulfilled, the method returns a {@link UiObject}
     * representing the element matching the selector (not the child element in its
     * subhierarchy containing the text). By default, this method performs a
     * scroll search.
     * See {@link #getChildByText(UiSelector, String, boolean)}
     *
     * @param childPattern {@link UiSelector} selector for a child in a scrollable layout element
     * @param text String to find in the children of the <code>childPattern</code> match
     * @return {@link UiObject} representing the child element that matches the search conditions
     * @throws UiObjectNotFoundException
     */
    @NonNull
    @Override
    public UiObject getChildByText(@NonNull UiSelector childPattern, @NonNull String text)
            throws UiObjectNotFoundException {
        return getChildByText(childPattern, text, true);
    }

    /**
     * Searches for a child element in the present scrollable container. The
     * search first looks for a child element that matches the
     * selector you provided, then looks for the text in its children elements.
     * If both search conditions are fulfilled, the method returns a {@link UiObject}
     * representing the element matching the selector (not the child element in its
     * subhierarchy containing the text).
     *
     * @param childPattern {@link UiSelector} selector for a child in a scrollable layout element
     * @param text String to find in the children of the <code>childPattern</code> match
     * @param allowScrollSearch set to true if scrolling is allowed
     * @return {@link UiObject} representing the child element that matches the search conditions
     * @throws UiObjectNotFoundException
     */
    @NonNull
    public UiObject getChildByText(@NonNull UiSelector childPattern,
            @NonNull String text,
            boolean allowScrollSearch) throws UiObjectNotFoundException {
        if (text != null) {
            if (allowScrollSearch) {
                scrollIntoView(new UiSelector().text(text));
            }
            return super.getChildByText(childPattern, text);
        }
        throw new UiObjectNotFoundException("for text= \"" + text + "\"");
    }

    /**
     * Performs a forward scroll action on the scrollable layout element until
     * the content-description is found, or until swipe attempts have been exhausted.
     * See {@link #setMaxSearchSwipes(int)}
     *
     * @param text content-description to find within the contents of this scrollable layout
     * element.
     * @return true if item is found; else, false
     */
    public boolean scrollDescriptionIntoView(@NonNull String text)
            throws UiObjectNotFoundException {
        return scrollIntoView(new UiSelector().description(text));
    }

    /**
     * Perform a forward scroll action to move through the scrollable layout element until
     * a visible item that matches the {@link UiObject} is found.
     *
     * @param obj {@link UiObject}
     * @return true if the item was found and now is in view else false
     */
    public boolean scrollIntoView(@NonNull UiObject obj) throws UiObjectNotFoundException {
        return scrollIntoView(obj.getSelector());
    }

    /**
     * Perform a scroll forward action to move through the scrollable layout 
     * element until a visible item that matches the selector is found.
     *
     * See {@link #scrollDescriptionIntoView(String)} and {@link #scrollTextIntoView(String)}.
     *
     * @param selector {@link UiSelector} selector
     * @return true if the item was found and now is in view; else, false
     */
    public boolean scrollIntoView(@NonNull UiSelector selector) throws UiObjectNotFoundException {
        Log.d(TAG, String.format("Scrolling %s into view.", selector));
        // if we happen to be on top of the text we want then return here
        UiSelector childSelector = getSelector().childSelector(selector);
        if (exists(childSelector)) {
            return true;
        } else {
            // we will need to reset the search from the beginning to start search
            scrollToBeginning(mMaxSearchSwipes);
            if (exists(childSelector)) {
                return true;
            }
            for (int x = 0; x < mMaxSearchSwipes; x++) {
                boolean scrolled = scrollForward();
                if(exists(childSelector)) {
                    return true;
                }
                if (!scrolled) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Performs a forward scroll action on the scrollable layout element until
     * the text you provided is visible, or until swipe attempts have been exhausted.
     * See {@link #setMaxSearchSwipes(int)}
     *
     * @param text test to look for
     * @return true if item is found; else, false
     */
    public boolean scrollTextIntoView(@NonNull String text) throws UiObjectNotFoundException {
        return scrollIntoView(new UiSelector().text(text));
    }

    /**
     * Sets the maximum number of scrolls allowed when performing a
     * scroll action in search of a child element.
     * See {@link UiCollection#getChildByDescription(UiSelector, String)} and
     * {@link #getChildByText(UiSelector, String)}.
     *
     * @param swipes the number of search swipes to perform until giving up
     * @return reference to itself
     */
    @NonNull
    public UiScrollable setMaxSearchSwipes(int swipes) {
        mMaxSearchSwipes = swipes;
        return this;
    }

    /**
     * Gets the maximum number of scrolls allowed when performing a
     * scroll action in search of a child element.
     * See {@link UiCollection#getChildByDescription(UiSelector, String)} and
     * {@link #getChildByText(UiSelector, String)}.
     *
     * @return max the number of search swipes to perform until giving up
     */
    public int getMaxSearchSwipes() {
        return mMaxSearchSwipes;
    }

    /**
     * Performs a forward fling with the default number of fling steps (5).
     * If the swipe direction is set to vertical, then the swipes will be
     * performed from bottom to top. If the swipe
     * direction is set to horizontal, then the swipes will be performed from
     * right to left. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @return true if scrolled, false if can't scroll anymore
     */
    public boolean flingForward() throws UiObjectNotFoundException {
        return scrollForward(FLING_STEPS);
    }

    /**
     * Performs a forward scroll with the default number of scroll steps (55).
     * If the swipe direction is set to vertical,
     * then the swipes will be performed from bottom to top. If the swipe
     * direction is set to horizontal, then the swipes will be performed from
     * right to left. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @return true if scrolled, false if can't scroll anymore
     */
    public boolean scrollForward() throws UiObjectNotFoundException {
        return scrollForward(SCROLL_STEPS);
    }

    /**
     * Performs a forward scroll. If the swipe direction is set to vertical,
     * then the swipes will be performed from bottom to top. If the swipe
     * direction is set to horizontal, then the swipes will be performed from
     * right to left. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @param steps number of steps. Use this to control the speed of the scroll action
     * @return true if scrolled, false if can't scroll anymore
     */
    public boolean scrollForward(int steps) throws UiObjectNotFoundException {
        AccessibilityNodeInfo node = findAccessibilityNodeInfo(
                mConfig.getWaitForSelectorTimeout());
        if(node == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect rect = getVisibleBounds(node);

        int downX = 0;
        int downY = 0;
        int upX = 0;
        int upY = 0;

        // scrolling is by default assumed vertically unless the object is explicitly
        // set otherwise by setAsHorizontalContainer()
        if(mIsVerticalList) {
            int swipeAreaAdjust = (int)(rect.height() * getSwipeDeadZonePercentage());
            // scroll vertically: swipe down -> up
            downX = rect.centerX();
            downY = rect.bottom - swipeAreaAdjust;
            upX = rect.centerX();
            upY = rect.top + swipeAreaAdjust;
        } else {
            int swipeAreaAdjust = (int)(rect.width() * getSwipeDeadZonePercentage());
            // scroll horizontally: swipe right -> left
            // TODO: Assuming device is not in right to left language
            downX = rect.right - swipeAreaAdjust;
            downY = rect.centerY();
            upX = rect.left + swipeAreaAdjust;
            upY = rect.centerY();
        }
        Log.d(TAG, String.format("Scrolling forward from (%d, %d) to (%d, %d) in %d steps.", downX,
                downY, upX, upY, steps));
        return getInteractionController().scrollSwipe(downX, downY, upX, upY, steps);
    }

    /**
     * Performs a backwards fling action with the default number of fling
     * steps (5). If the swipe direction is set to vertical,
     * then the swipe will be performed from top to bottom. If the swipe
     * direction is set to horizontal, then the swipes will be performed from
     * left to right. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @return true if scrolled, and false if can't scroll anymore
     */
    public boolean flingBackward() throws UiObjectNotFoundException {
        return scrollBackward(FLING_STEPS);
    }

    /**
     * Performs a backward scroll with the default number of scroll steps (55).
     * If the swipe direction is set to vertical,
     * then the swipes will be performed from top to bottom. If the swipe
     * direction is set to horizontal, then the swipes will be performed from
     * left to right. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @return true if scrolled, and false if can't scroll anymore
     */
    public boolean scrollBackward() throws UiObjectNotFoundException {
        return scrollBackward(SCROLL_STEPS);
    }

    /**
     * Performs a backward scroll. If the swipe direction is set to vertical,
     * then the swipes will be performed from top to bottom. If the swipe
     * direction is set to horizontal, then the swipes will be performed from
     * left to right. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @param steps number of steps. Use this to control the speed of the scroll action.
     * @return true if scrolled, false if can't scroll anymore
     */
    public boolean scrollBackward(int steps) throws UiObjectNotFoundException {
        AccessibilityNodeInfo node = findAccessibilityNodeInfo(
                mConfig.getWaitForSelectorTimeout());
        if (node == null) {
            throw new UiObjectNotFoundException(getSelector().toString());
        }
        Rect rect = getVisibleBounds(node);

        int downX = 0;
        int downY = 0;
        int upX = 0;
        int upY = 0;

        // scrolling is by default assumed vertically unless the object is explicitly
        // set otherwise by setAsHorizontalContainer()
        if(mIsVerticalList) {
            int swipeAreaAdjust = (int)(rect.height() * getSwipeDeadZonePercentage());
            // scroll vertically: swipe up -> down
            downX = rect.centerX();
            downY = rect.top + swipeAreaAdjust;
            upX = rect.centerX();
            upY = rect.bottom - swipeAreaAdjust;
        } else {
            int swipeAreaAdjust = (int)(rect.width() * getSwipeDeadZonePercentage());
            // scroll horizontally: swipe left -> right
            // TODO: Assuming device is not in right to left language
            downX = rect.left + swipeAreaAdjust;
            downY = rect.centerY();
            upX = rect.right - swipeAreaAdjust;
            upY = rect.centerY();
        }
        Log.d(TAG, String.format("Scrolling backward from (%d, %d) to (%d, %d) in %d steps.", downX,
                downY, upX, upY, steps));
        return getInteractionController().scrollSwipe(downX, downY, upX, upY, steps);
    }

    /**
     * Scrolls to the beginning of a scrollable layout element. The beginning
     * can be at the  top-most edge in the case of vertical controls, or the
     * left-most edge for horizontal controls. Make sure to take into account
     * devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param maxSwipes maximum number of scrolls allowed
     * @param steps use steps to control the speed, so that it may be a scroll, or fling
     * @return true if beginning reached within maxSwipes
     */
    public boolean scrollToBeginning(int maxSwipes, int steps) throws UiObjectNotFoundException {
        // protect against potential hanging and return after preset attempts
        for(int x = 0; x < maxSwipes; x++) {
            if(!scrollBackward(steps)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scrolls to the beginning of a scrollable layout element. The beginning
     * can be at the  top-most edge in the case of vertical controls, or the
     * left-most edge for horizontal controls. Make sure to take into account
     * devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param maxSwipes maximum number of scrolls allowed
     * @return true if beginning reached within maxSwipes
     */
    public boolean scrollToBeginning(int maxSwipes) throws UiObjectNotFoundException {
        return scrollToBeginning(maxSwipes, SCROLL_STEPS);
    }

    /**
     * Performs a fling gesture to reach the beginning of a scrollable layout element.
     * The beginning can be at the  top-most edge in the case of vertical controls, or
     * the left-most edge for horizontal controls. Make sure to take into
     * account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param maxSwipes maximum number of flings allowed
     * @return true if beginning reached within maxSwipes
     */
    public boolean flingToBeginning(int maxSwipes) throws UiObjectNotFoundException {
        return scrollToBeginning(maxSwipes, FLING_STEPS);
    }

    /**
     * Scrolls to the end of a scrollable layout element. The end can be at the
     * bottom-most edge in the case of vertical controls, or the right-most edge for
     * horizontal controls. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @param maxSwipes maximum number of scrolls allowed
     * @param steps use steps to control the speed, so that it may be a scroll, or fling
     * @return true if end reached within maxSwipes
     */
    public boolean scrollToEnd(int maxSwipes, int steps) throws UiObjectNotFoundException {
        // protect against potential hanging and return after preset attempts
        for(int x = 0; x < maxSwipes; x++) {
            if(!scrollForward(steps)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scrolls to the end of a scrollable layout element. The end can be at the
     * bottom-most edge in the case of vertical controls, or the right-most edge for
     * horizontal controls. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @param maxSwipes maximum number of scrolls allowed
     * @return true if end reached within maxSwipes
     */
    public boolean scrollToEnd(int maxSwipes) throws UiObjectNotFoundException {
        return scrollToEnd(maxSwipes, SCROLL_STEPS);
    }

    /**
     * Performs a fling gesture to reach the end of a scrollable layout element.
     * The end can be at the  bottom-most edge in the case of vertical controls, or
     * the right-most edge for horizontal controls. Make sure to take into
     * account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param maxSwipes maximum number of flings allowed
     * @return true if end reached within maxSwipes
     */
    public boolean flingToEnd(int maxSwipes) throws UiObjectNotFoundException {
        return scrollToEnd(maxSwipes, FLING_STEPS);
    }

    /**
     * Returns the percentage of a widget's size that's considered as a no-touch
     * zone when swiping. The no-touch zone is set as a percentage of a widget's total
     * width or height, denoting a margin around the swipable area of the widget.
     * Swipes must start and end inside this margin. This is important when the
     * widget being swiped may not respond to the swipe if started at a point
     * too near to the edge. The default is 10% from either edge.
     *
     * @return a value between 0 and 1
     */
    public double getSwipeDeadZonePercentage() {
        return mSwipeDeadZonePercentage;
    }

    /**
     * Sets the percentage of a widget's size that's considered as no-touch
     * zone when swiping.
     * The no-touch zone is set as percentage of a widget's total width or height,
     * denoting a margin around the swipable area of the widget. Swipes must
     * always start and end inside this margin. This is important when the
     * widget being swiped may not respond to the swipe if started at a point
     * too near to the edge. The default is 10% from either edge.
     *
     * @param swipeDeadZonePercentage is a value between 0 and 1
     * @return reference to itself
     */
    @NonNull
    public UiScrollable setSwipeDeadZonePercentage(double swipeDeadZonePercentage) {
        mSwipeDeadZonePercentage = swipeDeadZonePercentage;
        return this;
    }
}
