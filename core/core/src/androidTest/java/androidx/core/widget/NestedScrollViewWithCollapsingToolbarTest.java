/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.core.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that CollapsingToolbarLayout properly collapses/expands with a NestedScrollView.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class NestedScrollViewWithCollapsingToolbarTest {
    private static final String LONG_TEXT = "This is some long text. It just keeps going. Look at"
            + " it. Scroll it. Scroll the nested version of it. This is some long text. It just"
            + " keeps going. Look at it. Scroll it. Scroll the nested version of it. This is some"
            + " long text. It just keeps going. Look at it. Scroll it. Scroll the nested version of"
            + " it. This is some long text. It just keeps going. Look at it. Scroll it. Scroll the"
            + " nested version of it. This is some long text. It just keeps going. Look at it."
            + " Scroll it. Scroll the nested version of it. This is some long text. It just keeps"
            + " going. Look at it. Scroll it. Scroll the nested version of it. This is some long"
            + " text. It just keeps going. Look at it. Scroll it. Scroll the nested version of it."
            + " This is some long text. It just keeps going. Look at it. Scroll it. Scroll the"
            + " nested version of it. This is some long text. It just keeps going. Look at it."
            + " Scroll it. Scroll the nested version of it. This is some long text. It just keeps"
            + " going. Look at it. Scroll it. Scroll the nested version of it. This is some long"
            + " text. It just keeps going. Look at it. Scroll it. Scroll the nested version of it."
            + " This is some long text. It just keeps going. Look at it. Scroll it. Scroll the"
            + " nested version of it. This is some long text. It just keeps going. Look at it."
            + " Scroll it. Scroll the nested version of it. This is some long text. It just keeps"
            + " going. Look at it. Scroll it. Scroll the nested version of it. This is some long"
            + " text. It just keeps going. Look at it. Scroll it. Scroll the nested version of it."
            + " This is some long text. It just keeps going. Look at it. Scroll it. Scroll the"
            + " nested version of it. This is some long text. It just keeps going. Look at it."
            + " Scroll it. Scroll the nested version of it. This is some long text. It just keeps"
            + " going. Look at it. Scroll it. Scroll the nested version of it. This is some long"
            + " text. It just keeps going. Look at it. Scroll it. Scroll the nested version of it."
            + " This is some long text. It just keeps going. Look at it. Scroll it. Scroll the"
            + " nested version of it.";

    private MockCoordinatorLayoutWithCollapsingToolbarAndNestedScrollView mParentNestedScrollView;

    private MockCoordinatorLayoutWithCollapsingToolbarAndNestedScrollView mChildNestedScrollView;

    /*** Touch swiping tests at the top/bottom of the child ***/
    @Test
    public void isOnStartNestedScrollCalled_touchSwipeUpInChild_calledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);

        // Act
        // Swipes from the bottom of the child to the top of child.
        swipeUp(mChildNestedScrollView, false);

        // Assert
        // Should trigger scroll event(s) in parent (touch may trigger more than one).
        assertTrue(mParentNestedScrollView.getOnStartNestedScrollCount() > 0);
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    @Test
    public void isOnStartNestedScrollCalled_touchSwipeDownInChild_calledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);

        // Act
        // Swipes from the top of the child to the bottom of child
        swipeDown(mChildNestedScrollView, false);

        // Assert
        // Should trigger scroll event(s) in parent (touch may trigger more than one).
        assertTrue(mParentNestedScrollView.getOnStartNestedScrollCount() > 0);
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    /*** Rotary scrolling tests at the top/bottom of the child ***/
    @Test
    public void isOnStartNestedScrollCalled_rotaryScrollInChildPastTop_calledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);

        // Act
        sendScroll(
                mChildNestedScrollView,
                2f,
                InputDevice.SOURCE_ROTARY_ENCODER
        );

        // Assert
        // Should trigger in parent of scroll event.
        assertEquals(1, mParentNestedScrollView.getOnStartNestedScrollCount());
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    @Test
    public void isOnStartNestedScrollCalled_rotaryScrollInChildPastBottom_calledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);
        // Move to bottom of the child NestedScrollView, so we can try scrolling past it.
        int scrollRange = mChildNestedScrollView.getScrollRange();
        mChildNestedScrollView.scrollTo(0, scrollRange);

        // Act
        sendScroll(
                mChildNestedScrollView,
                -2f,
                InputDevice.SOURCE_ROTARY_ENCODER
        );

        // Assert
        // Should trigger in parent of scroll event.
        assertEquals(1, mParentNestedScrollView.getOnStartNestedScrollCount());
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    /*** Mouse scrolling tests at the top/bottom of the child ***/
    @Test
    public void isOnStartNestedScrollCalled_mouseScrollInChildPastTop_calledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);

        // Act
        sendScroll(
                mChildNestedScrollView,
                2f,
                InputDevice.SOURCE_MOUSE
        );

        // Assert
        // Should trigger in parent of scroll event.
        assertEquals(1, mParentNestedScrollView.getOnStartNestedScrollCount());
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    @Test
    public void isOnStartNestedScrollCalled_mouseScrollInChildPastBottom_calledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);
        // Move to bottom of the child NestedScrollView, so we can try scrolling past it.
        int scrollRange = mChildNestedScrollView.getScrollRange();
        mChildNestedScrollView.scrollTo(0, scrollRange);

        // Act
        sendScroll(
                mChildNestedScrollView,
                -2f,
                InputDevice.SOURCE_MOUSE
        );

        // Assert
        // Should trigger in parent of scroll event.
        assertEquals(1, mParentNestedScrollView.getOnStartNestedScrollCount());
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    /*** Keyboard event tests BOTH inside the child and at the top/bottom of the child ***/
    // Keyboard events within the child (should trigger OnStartNestedScroll() in parent)
    @Test
    public void isOnStartNestedScrollCalled_keyboardUpInChild_calledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);

        // Move to bottom of the child NestedScrollView, so we can scroll up and not go past child.
        int scrollRange = mChildNestedScrollView.getScrollRange();
        mChildNestedScrollView.scrollTo(0, scrollRange);

        // Act
        mChildNestedScrollView.requestFocus();
        KeyEvent keyEventPressDown = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_UP,
                0);
        mChildNestedScrollView.executeKeyEvent(keyEventPressDown);

        KeyEvent keyEventPressUp = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_DPAD_UP,
                0);
        mChildNestedScrollView.executeKeyEvent(keyEventPressUp);

        // Assert
        // Should trigger a scroll event in parent. Note: OnStartNestedScroll is triggered on
        // key action down only, not key action up, so that is why the count is one.
        assertEquals(1, mParentNestedScrollView.getOnStartNestedScrollCount());
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }


    @Test
    public void isOnStartNestedScrollCalled_keyboardDownInChild_calledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);

        // Act
        mChildNestedScrollView.requestFocus();
        KeyEvent keyEventPressDown = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_DOWN,
                0);
        mChildNestedScrollView.executeKeyEvent(keyEventPressDown);

        KeyEvent keyEventPressUp = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                0);
        mChildNestedScrollView.executeKeyEvent(keyEventPressUp);

        // Assert
        // Should trigger a scroll event in parent. Note: OnStartNestedScroll is triggered on
        // key action down only, not key action up, so that is why the count is one.
        // Should trigger in parent of scroll event.
        assertEquals(1, mParentNestedScrollView.getOnStartNestedScrollCount());
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    // Keyboard events at the top/bottom bounds of the child (should NOT trigger
    // OnStartNestedScroll() in the parent).

    // For events at the bounds of the nested child, Keyboard events are handled a little different
    // from the rest. If they are at the bound, they will not handle the event
    // (return false) and so the container view will handle it (something like CoordinatorLayout).
    // Where with the other types (from Touch, Rotary, and Scroll), the NestedScrollView will
    // handle those bound crossing events itself, and thus why these tests don't have a
    // OnStartNestedScroll() in the parent

    // Keyboard events inside the child (should still trigger OnStartNestedScroll() in parent)
    @Test
    public void isOnStartNestedScrollCalled_keyboardUpInChildPastTop_notCalledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);

        // Act
        mChildNestedScrollView.requestFocus();
        KeyEvent keyEventPressDown = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_UP,
                0);
        mChildNestedScrollView.executeKeyEvent(keyEventPressDown);

        KeyEvent keyEventPressUp = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_DPAD_UP,
                0);
        mChildNestedScrollView.executeKeyEvent(keyEventPressUp);

        // Assert
        // Should trigger in parent of scroll event. Note: OnStartNestedScroll is triggered on
        // key action down only, not key action up, so that is why the count is one.
        assertEquals(0, mParentNestedScrollView.getOnStartNestedScrollCount());
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    @Test
    public void isOnStartNestedScrollCalled_keyboardDownInChildPastBottom_notCalledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);
        // Move to bottom of the child NestedScrollView, so we can try scrolling past it.
        int scrollRange = mChildNestedScrollView.getScrollRange();
        mChildNestedScrollView.scrollTo(0, scrollRange);

        // Act
        mChildNestedScrollView.requestFocus();
        KeyEvent keyEventPressDown = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_DOWN,
                0);
        mChildNestedScrollView.executeKeyEvent(keyEventPressDown);

        KeyEvent keyEventPressUp = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                0);
        mChildNestedScrollView.executeKeyEvent(keyEventPressUp);

        // Assert
        // Should trigger in parent of scroll event. Note: OnStartNestedScroll is triggered on
        // key action down only, not key action up, so that is why the count is one.
        // Should trigger in parent of scroll event.
        assertEquals(0, mParentNestedScrollView.getOnStartNestedScrollCount());
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    @Test
    public void isOnStartNestedScrollCalled_keyboardAltUpInChildPastTop_notCalledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);

        // Act
        mChildNestedScrollView.requestFocus();
        KeyEvent keyEventPressDown = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_UP,
                0,
                KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON
        );
        mChildNestedScrollView.executeKeyEvent(keyEventPressDown);

        KeyEvent keyEventPressUp = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_DPAD_UP,
                0,
                KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON
        );
        mChildNestedScrollView.executeKeyEvent(keyEventPressUp);

        // Assert
        // Should trigger in parent of scroll event. Note: OnStartNestedScroll is triggered on
        // key action down only, not key action up, so that is why the count is one.
        // Should trigger in parent of scroll event.
        assertEquals(0, mParentNestedScrollView.getOnStartNestedScrollCount());
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    @Test
    public void isOnStartNestedScrollCalled_keyboardAltDownInChildPastBottom_notCalledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);
        // Move to bottom of the child NestedScrollView, so we can try scrolling past it.
        int scrollRange = mChildNestedScrollView.getScrollRange();
        mChildNestedScrollView.scrollTo(0, scrollRange);

        mChildNestedScrollView.requestFocus();
        KeyEvent keyEventPressDown = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DPAD_DOWN,
                0,
                KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON
        );
        mChildNestedScrollView.executeKeyEvent(keyEventPressDown);

        KeyEvent keyEventPressUp = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                0,
                KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON
        );
        mChildNestedScrollView.executeKeyEvent(keyEventPressUp);

        // Assert
        // Should trigger in parent of scroll event. Note: OnStartNestedScroll is triggered on
        // key action down only, not key action up, so that is why the count is one.
        // Should trigger in parent of scroll event.
        assertEquals(0, mParentNestedScrollView.getOnStartNestedScrollCount());
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    @Test
    public void isOnStartNestedScrollCalled_keyboardShiftSpaceInChildPastTop_notCalledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);

        // Act
        mChildNestedScrollView.requestFocus();
        KeyEvent keyEventPressDown = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_SPACE,
                0,
                KeyEvent.META_SHIFT_ON);
        mChildNestedScrollView.executeKeyEvent(keyEventPressDown);

        KeyEvent keyEventPressUp = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_SPACE,
                0,
                KeyEvent.META_SHIFT_ON);
        mChildNestedScrollView.executeKeyEvent(keyEventPressUp);

        // Assert
        // Should trigger in parent of scroll event. Note: OnStartNestedScroll is triggered on
        // key action down only, not key action up, so that is why the count is one.
        // Should trigger in parent of scroll event.
        assertEquals(0, mParentNestedScrollView.getOnStartNestedScrollCount());
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    @Test
    public void isOnStartNestedScrollCalled_keyboardSpaceInChildPastBottom_notCalledInParent() {
        // Arrange
        setupNestedScrollViewInNestedScrollView(
                ApplicationProvider.getApplicationContext(),
                100,
                600);
        // Move to bottom of the child NestedScrollView, so we can try scrolling past it.
        int scrollRange = mChildNestedScrollView.getScrollRange();
        mChildNestedScrollView.scrollTo(0, scrollRange);

        // Act
        mChildNestedScrollView.requestFocus();
        KeyEvent keyEventPressDown = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_SPACE,
                0);
        mChildNestedScrollView.executeKeyEvent(keyEventPressDown);

        KeyEvent keyEventPressUp = new KeyEvent(
                0,
                0,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_SPACE,
                0);
        mChildNestedScrollView.executeKeyEvent(keyEventPressUp);

        // Assert
        // Should trigger in parent of scroll event. Note: OnStartNestedScroll is triggered on
        // key action down only, not key action up, so that is why the count is one.
        // Should trigger in parent of scroll event.
        assertEquals(0, mParentNestedScrollView.getOnStartNestedScrollCount());
        // Should not trigger in child (because child doesn't have its own inner NestedScrollView).
        assertEquals(0, mChildNestedScrollView.getOnStartNestedScrollCount());
    }

    private TextView createTextView(Context context, int width, int height, String textContent) {
        TextView textView = new TextView(context);
        textView.setMinimumWidth(width);
        textView.setMinimumHeight(height);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textView.setLayoutParams(layoutParams);
        textView.setText(textContent);

        return textView;
    }

    private void setupNestedScrollViewInNestedScrollView(Context context, int width, int height) {

        // 1. Setup Views

        // The parent NestedScrollView contains a LinearLayout with three Views:
        //  a. TextView
        //  b. A child NestedScrollView (contains its own TextView)
        //  c. TextView
        int childHeight = height / 3;

        // Creates child NestedScrollView first
        mChildNestedScrollView =
                new MockCoordinatorLayoutWithCollapsingToolbarAndNestedScrollView(context);
        mChildNestedScrollView.setMinimumWidth(width);
        mChildNestedScrollView.setMinimumHeight(childHeight);
        NestedScrollView.LayoutParams nestedChildLayoutParams = new NestedScrollView.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                childHeight
        );
        mChildNestedScrollView.setLayoutParams(nestedChildLayoutParams);

        mChildNestedScrollView.setBackgroundColor(0xFF0000FF);
        mChildNestedScrollView.addView(createTextView(context, width, childHeight, LONG_TEXT));


        // Creates LinearLayout containing three Views (TextViews and previously created child
        // NestedScrollView.
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setMinimumWidth(width);
        linearLayout.setMinimumHeight(height);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        linearLayout.setLayoutParams(linearLayoutParams);

        linearLayout.addView(createTextView(context, width, childHeight, LONG_TEXT));
        linearLayout.addView(mChildNestedScrollView);
        linearLayout.addView(createTextView(context, width, childHeight, LONG_TEXT));

        mParentNestedScrollView =
                new MockCoordinatorLayoutWithCollapsingToolbarAndNestedScrollView(context);
        mParentNestedScrollView.setMinimumWidth(width);
        mParentNestedScrollView.setMinimumHeight(height);
        mParentNestedScrollView.setBackgroundColor(0xCC00FF00);
        mParentNestedScrollView.addView(linearLayout);

        // 2. Measure Parent
        int measureSpecWidth =
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int measureSpecHeight =
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        mParentNestedScrollView.measure(measureSpecWidth, measureSpecHeight);

        // 3. Layout Parent
        mParentNestedScrollView.layout(0, 0, width, height);
    }

    private void swipeDown(View view, boolean shortSwipe) {
        float endY = shortSwipe ? view.getHeight() / 2f : view.getHeight() - 1;
        swipe(0, endY, view);
    }

    private void swipeUp(View view, boolean shortSwipe) {
        float endY = shortSwipe ? view.getHeight() / 2f : 0;
        swipe(view.getHeight() - 1, endY, view);
    }

    private void swipe(float startY, float endY, View view) {
        float x = view.getWidth() / 2f;

        MotionEvent down = MotionEvent.obtain(
                0,
                0,
                MotionEvent.ACTION_DOWN,
                x,
                startY,
                0
        );
        view.dispatchTouchEvent(down);

        MotionEvent move = MotionEvent.obtain(
                0,
                10,
                MotionEvent.ACTION_MOVE,
                x,
                endY,
                0
        );
        view.dispatchTouchEvent(move);

        MotionEvent up = MotionEvent.obtain(0,
                1000,
                MotionEvent.ACTION_UP,
                x,
                endY,
                0
        );
        view.dispatchTouchEvent(up);
    }

    private void sendScroll(View view, float scrollAmount, int source) {
        float x = view.getWidth() / 2f;
        float y = view.getHeight() / 2f;
        MotionEvent.PointerProperties pointerProperties = new MotionEvent.PointerProperties();
        pointerProperties.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
        pointerCoords.x = x;
        pointerCoords.y = y;
        int axis = source == InputDevice.SOURCE_ROTARY_ENCODER ? MotionEvent.AXIS_SCROLL
                : MotionEvent.AXIS_VSCROLL;
        pointerCoords.setAxisValue(axis, scrollAmount);

        MotionEvent scroll = MotionEvent.obtain(
                0, /* downTime */
                0, /* eventTime */
                MotionEvent.ACTION_SCROLL, /* action */
                1, /* pointerCount */
                new MotionEvent.PointerProperties[] { pointerProperties },
                new MotionEvent.PointerCoords[] { pointerCoords },
                0, /* metaState */
                0, /* buttonState */
                0f, /* xPrecision */
                0f, /* yPrecision */
                0, /* deviceId */
                0, /* edgeFlags */
                source, /* source */
                0 /* flags */
        );

        view.dispatchGenericMotionEvent(scroll);
    }

    /*
     * Since CollapsingToolbarLayout relies on NestedScrollView.onStartNestedScroll() being
     * triggered
     * to collapse/expand itself, we can just test when that method is triggered (and count that) to
     * cover testing CollapsingToolbarLayout collapsing/expanding.
     */
    class MockCoordinatorLayoutWithCollapsingToolbarAndNestedScrollView extends NestedScrollView {
        private int mOnStartNestedScrollCount = 0;
        public int getOnStartNestedScrollCount() {
            return mOnStartNestedScrollCount;
        }

        MockCoordinatorLayoutWithCollapsingToolbarAndNestedScrollView(Context context) {
            super(context);
        }

        MockCoordinatorLayoutWithCollapsingToolbarAndNestedScrollView(
                Context context,
                AttributeSet attrs
        ) {
            super(context, attrs);
        }

        MockCoordinatorLayoutWithCollapsingToolbarAndNestedScrollView(
                Context context,
                AttributeSet attrs,
                int defStyleAttr
        ) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public boolean onStartNestedScroll(View child, View target, int axes, int type) {
            mOnStartNestedScrollCount++;
            return super.onStartNestedScroll(child, target, axes, type);
        }
    }
}
