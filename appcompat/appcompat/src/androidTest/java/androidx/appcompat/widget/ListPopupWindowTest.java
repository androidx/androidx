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
package androidx.appcompat.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SdkSuppress(maxSdkVersion=29) // Broken on API 30 b/159743495
@RunWith(AndroidJUnit4.class)
public class ListPopupWindowTest {
    @Rule
    public final ActivityTestRule<PopupTestActivity> mActivityTestRule =
            new ActivityTestRule<>(PopupTestActivity.class);

    private FrameLayout mContainer;

    private Button mButton;

    private ListPopupWindow mListPopupWindow;

    private BaseAdapter mListPopupAdapter;

    private AdapterView.OnItemClickListener mItemClickListener;

    /**
     * Item click listener that dismisses our <code>ListPopupWindow</code> when any item
     * is clicked. Note that this needs to be a separate class that is also protected (not
     * private) so that Mockito can "spy" on it.
     */
    protected class PopupItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            mListPopupWindow.dismiss();
        }
    }

    @Before
    public void setUp() throws Exception {
        final PopupTestActivity activity = mActivityTestRule.getActivity();
        mContainer = activity.findViewById(R.id.container);
        mButton = mContainer.findViewById(R.id.test_button);
        mItemClickListener = new PopupItemClickListener();
    }

    @Test
    @MediumTest
    public void testBasicContent() {
        Builder popupBuilder = new Builder();
        popupBuilder.wireToActionButton();

        onView(withId(R.id.test_button)).perform(click());
        assertNotNull("Popup window created", mListPopupWindow);
        assertTrue("Popup window showing", mListPopupWindow.isShowing());

        final View mainDecorView = mActivityTestRule.getActivity().getWindow().getDecorView();
        onView(withText("Alice"))
                .inRoot(withDecorView(not(is(mainDecorView))))
                .check(matches(isDisplayed()));
        onView(withText("Bob"))
                .inRoot(withDecorView(not(is(mainDecorView))))
                .check(matches(isDisplayed()));
        onView(withText("Charlie"))
                .inRoot(withDecorView(not(is(mainDecorView))))
                .check(matches(isDisplayed()));
        onView(withText("Deirdre"))
                .inRoot(withDecorView(not(is(mainDecorView))))
                .check(matches(isDisplayed()));
        onView(withText("El"))
                .inRoot(withDecorView(not(is(mainDecorView))))
                .check(matches(isDisplayed()));
    }

    @FlakyTest(bugId = 33669575)
    @Test
    @LargeTest
    public void testAnchoring() {
        Builder popupBuilder = new Builder();
        popupBuilder.wireToActionButton();

        onView(withId(R.id.test_button)).perform(click());
        assertTrue("Popup window showing", mListPopupWindow.isShowing());
        assertEquals("Popup window anchor", mButton, mListPopupWindow.getAnchorView());

        final int[] anchorOnScreenXY = new int[2];
        final int[] popupOnScreenXY = new int[2];
        final int[] popupInWindowXY = new int[2];
        final Rect rect = new Rect();

        mListPopupWindow.getListView().getLocationOnScreen(popupOnScreenXY);
        mButton.getLocationOnScreen(anchorOnScreenXY);
        mListPopupWindow.getListView().getLocationInWindow(popupInWindowXY);
        mListPopupWindow.getBackground().getPadding(rect);

        assertEquals("Anchoring X", anchorOnScreenXY[0] + popupInWindowXY[0], popupOnScreenXY[0]);
        assertEquals("Anchoring Y", anchorOnScreenXY[1] + popupInWindowXY[1] + mButton.getHeight(),
                popupOnScreenXY[1] + rect.top);
    }

    @Test
    @MediumTest
    public void testDismissalViaAPI() throws Throwable {
        Builder popupBuilder = new Builder().withDismissListener();
        popupBuilder.wireToActionButton();

        onView(withId(R.id.test_button)).perform(click());
        assertTrue("Popup window showing", mListPopupWindow.isShowing());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListPopupWindow.dismiss();
            }
        });

        // Verify that our dismiss listener has been called
        verify(popupBuilder.mOnDismissListener, times(1)).onDismiss();
        assertFalse("Popup window not showing after dismissal", mListPopupWindow.isShowing());
    }

    private void testDismissalViaTouch(boolean setupAsModal) throws Throwable {
        Builder popupBuilder = new Builder().setModal(setupAsModal).withDismissListener();
        popupBuilder.wireToActionButton();

        final View.OnClickListener mockContainerClickListener = mock(View.OnClickListener.class);
        // Also register a click listener on the top-level container
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mContainer.setOnClickListener(mockContainerClickListener);
            }
        });

        onView(withId(R.id.test_button)).perform(click());
        assertTrue("Popup window showing", mListPopupWindow.isShowing());
        // Make sure that the modality of the popup window is set up correctly
        assertEquals("Popup window modality", setupAsModal, mListPopupWindow.isModal());

        // Determine the location of the popup on the screen so that we can emulate
        // a tap outside of its bounds to dismiss it
        final int[] popupOnScreenXY = new int[2];
        final Rect rect = new Rect();
        mListPopupWindow.getListView().getLocationOnScreen(popupOnScreenXY);
        mListPopupWindow.getBackground().getPadding(rect);

        int emulatedTapX = popupOnScreenXY[0] - rect.left - 20;
        int emulatedTapY = popupOnScreenXY[1] - 20;

        // The logic below uses Instrumentation to emulate a tap outside the bounds of the
        // displayed list popup window. This tap is then treated by the framework to be "split" as
        // the ACTION_OUTSIDE for the popup itself, as well as DOWN / MOVE / UP for the underlying
        // view root if the popup is not modal.
        // It is not correct to emulate these two sequences separately in the test, as it
        // wouldn't emulate the user-facing interaction for this test. Note that usage
        // of Instrumentation is necessary here since Espresso's actions operate at the level
        // of view or data. Also, we don't want to use View.dispatchTouchEvent directly as
        // that would require emulation of two separate sequences as well.

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        // Inject DOWN event
        long downTime = SystemClock.uptimeMillis();
        MotionEvent eventDown = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN, emulatedTapX, emulatedTapY, 1);
        instrumentation.sendPointerSync(eventDown);

        // Inject MOVE event
        long moveTime = SystemClock.uptimeMillis();
        MotionEvent eventMove = MotionEvent.obtain(
                moveTime, moveTime, MotionEvent.ACTION_MOVE, emulatedTapX, emulatedTapY, 1);
        instrumentation.sendPointerSync(eventMove);

        // Inject UP event
        long upTime = SystemClock.uptimeMillis();
        MotionEvent eventUp = MotionEvent.obtain(
                upTime, upTime, MotionEvent.ACTION_UP, emulatedTapX, emulatedTapY, 1);
        instrumentation.sendPointerSync(eventUp);

        // Wait for the system to process all events in the queue
        instrumentation.waitForIdleSync();

        // At this point our popup should not be showing and should have notified its
        // dismiss listener
        verify(popupBuilder.mOnDismissListener, times(1)).onDismiss();
        assertFalse("Popup window not showing after outside click", mListPopupWindow.isShowing());

        // Also test that the click outside the popup bounds has been "delivered" to the main
        // container only if the popup is not modal
        verify(mockContainerClickListener, times(setupAsModal ? 0 : 1)).onClick(mContainer);
    }

    @Test
    @MediumTest
    public void testDismissalOutsideNonModal() throws Throwable {
        testDismissalViaTouch(false);
    }

    @Test
    @MediumTest
    public void testDismissalOutsideModal() throws Throwable {
        testDismissalViaTouch(true);
    }

    @Test
    @LargeTest
    public void testItemClickViaEvent() {
        Builder popupBuilder = new Builder().withItemClickListener();
        popupBuilder.wireToActionButton();

        onView(withId(R.id.test_button)).perform(click());
        assertTrue("Popup window showing", mListPopupWindow.isShowing());

        // Verify that our menu item click listener hasn't been called yet
        verify(popupBuilder.mOnItemClickListener, never()).onItemClick(
                any(AdapterView.class), any(View.class), any(int.class), anyLong());

        final View mainDecorView = mActivityTestRule.getActivity().getWindow().getDecorView();
        onView(withText("Charlie"))
                .inRoot(withDecorView(not(is(mainDecorView))))
                .perform(click());
        // Verify that out menu item click listener has been called with the expected item
        // position. Note that we use any() for other parameters, as we don't want to tie ourselves
        // to the specific implementation details of how ListPopupWindow displays its content.
        verify(popupBuilder.mOnItemClickListener, times(1)).onItemClick(
                any(AdapterView.class), any(View.class), eq(2), anyLong());

        // Our item click listener also dismisses the popup
        assertFalse("Popup window not showing after click", mListPopupWindow.isShowing());
    }

    @Test
    @SmallTest
    public void testAccessEpicenter() {
        new Builder().configure();
        final Rect initialRect = new Rect(10, 10, 20, 20);
        assertNull(mListPopupWindow.getEpicenterBounds());

        mListPopupWindow.setEpicenterBounds(initialRect);
        assertEquals(initialRect, mListPopupWindow.getEpicenterBounds());

        mListPopupWindow.setEpicenterBounds(null);
        assertNull(mListPopupWindow.getEpicenterBounds());
    }

    @Test
    @SmallTest
    public void testEpicenterSetterImmutability() {
        new Builder().configure();
        final Rect initialRect = new Rect(10, 10, 20, 20);

        mListPopupWindow.setEpicenterBounds(initialRect);
        assertEquals(initialRect, mListPopupWindow.getEpicenterBounds());

        initialRect.offset(5, 5);
        assertNotEquals(initialRect, mListPopupWindow.getEpicenterBounds());
    }

    @Test
    @SmallTest
    public void testEpicenterGetterImmutability() {
        new Builder().configure();
        final Rect initialRect = new Rect(10, 10, 20, 20);

        mListPopupWindow.setEpicenterBounds(initialRect);
        final Rect retrievedRect = mListPopupWindow.getEpicenterBounds();
        assertEquals(initialRect, retrievedRect);

        retrievedRect.offset(5, 5);
        assertNotEquals(initialRect, retrievedRect);
    }

    @Test
    @MediumTest
    public void testItemClickViaAPI() throws Throwable {
        Builder popupBuilder = new Builder().withItemClickListener();
        popupBuilder.wireToActionButton();

        onView(withId(R.id.test_button)).perform(click());
        assertTrue("Popup window showing", mListPopupWindow.isShowing());

        // Verify that our menu item click listener hasn't been called yet
        verify(popupBuilder.mOnItemClickListener, never()).onItemClick(
                any(AdapterView.class), any(View.class), any(int.class), anyLong());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListPopupWindow.performItemClick(1);
            }
        });

        // Verify that out menu item click listener has been called with the expected item
        // position. Note that we use any() for other parameters, as we don't want to tie ourselves
        // to the specific implementation details of how ListPopupWindow displays its content.
        verify(popupBuilder.mOnItemClickListener, times(1)).onItemClick(
                any(AdapterView.class), any(View.class), eq(1), anyLong());
        // Our item click listener also dismisses the popup
        assertFalse("Popup window not showing after click", mListPopupWindow.isShowing());
    }

    /**
     * Emulates a drag-down gestures by injecting ACTION events with {@link Instrumentation}.
     */
    private void emulateDragDownGesture(int emulatedX, int emulatedStartY, int swipeAmount) {
        // The logic below uses Instrumentation to emulate a swipe / drag gesture to bring up
        // the popup content. Note that we don't want to use Espresso's GeneralSwipeAction
        // as that operates on the level of an individual view. Here we want to test correct
        // forwarding of events that cross the boundary between the anchor and the popup menu.

        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        // Inject DOWN event
        long downTime = SystemClock.uptimeMillis();
        MotionEvent eventDown = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN, emulatedX, emulatedStartY, 1);
        instrumentation.sendPointerSync(eventDown);

        // Inject a sequence of MOVE events that emulate a "swipe down" gesture
        for (int i = 0; i < 10; i++) {
            long moveTime = SystemClock.uptimeMillis();
            final int moveY = emulatedStartY + swipeAmount * i / 10;
            MotionEvent eventMove = MotionEvent.obtain(
                    moveTime, moveTime, MotionEvent.ACTION_MOVE, emulatedX, moveY, 1);
            instrumentation.sendPointerSync(eventMove);
            // sleep for a bit to emulate a 200ms swipe
            SystemClock.sleep(20);
        }

        // Inject UP event
        long upTime = SystemClock.uptimeMillis();
        MotionEvent eventUp = MotionEvent.obtain(
                upTime, upTime, MotionEvent.ACTION_UP, emulatedX, emulatedStartY + swipeAmount, 1);
        instrumentation.sendPointerSync(eventUp);

        // Wait for the system to process all events in the queue
        instrumentation.waitForIdleSync();
    }

    @Test
    @MediumTest
    public void testCreateOnDragListener() throws Throwable {
        // In this test we want precise control over the height of the popup content since
        // we need to know by how much to swipe down to end the emulated gesture over the
        // specific item in the popup. This is why we're using a popup style that removes
        // all decoration around the popup content, as well as our own row layout with known
        // height.
        Builder popupBuilder = new Builder()
                .withPopupStyleAttr(R.style.PopupEmptyStyle)
                .withContentRowLayoutId(R.layout.popup_window_item)
                .withItemClickListener().withDismissListener();

        // Configure ListPopupWindow without showing it
        popupBuilder.configure();

        // Get the anchor view and configure it with ListPopupWindow's drag-to-open listener
        final View anchor = mActivityTestRule.getActivity().findViewById(R.id.test_button);
        View.OnTouchListener dragListener = mListPopupWindow.createDragToOpenListener(anchor);
        anchor.setOnTouchListener(dragListener);
        // And also configure it to show the popup window on click
        anchor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListPopupWindow.show();
            }
        });

        // Get the height of a row item in our popup window
        final int popupRowHeight = mActivityTestRule.getActivity().getResources()
                .getDimensionPixelSize(R.dimen.popup_row_height);

        final int[] anchorOnScreenXY = new int[2];
        anchor.getLocationOnScreen(anchorOnScreenXY);

        // Compute the start coordinates of a downward swipe and the amount of swipe. We'll
        // be swiping by twice the row height. That, combined with the swipe originating in the
        // center of the anchor should result in clicking the second row in the popup.
        int emulatedX = anchorOnScreenXY[0] + anchor.getWidth() / 2;
        int emulatedStartY = anchorOnScreenXY[1] + anchor.getHeight() / 2;
        int swipeAmount = 2 * popupRowHeight;

        // Emulate drag-down gesture with a sequence of motion events
        emulateDragDownGesture(emulatedX, emulatedStartY, swipeAmount);

        // We expect the swipe / drag gesture to result in clicking the second item in our list.
        verify(popupBuilder.mOnItemClickListener, times(1)).onItemClick(
                any(AdapterView.class), any(View.class), eq(1), eq(1L));
        // Since our item click listener calls dismiss() on the popup, we expect the popup to not
        // be showing
        assertFalse(mListPopupWindow.isShowing());
        // At this point our popup should have notified its dismiss listener
        verify(popupBuilder.mOnDismissListener, times(1)).onDismiss();
    }

    /**
     * Inner helper class to configure an instance of <code>ListPopupWindow</code> for the
     * specific test. The main reason for its existence is that once a popup window is shown
     * with the show() method, most of its configuration APIs are no-ops. This means that
     * we can't add logic that is specific to a certain test (such as dismissing a non-modal
     * popup window) once it's shown and we have a reference to a displayed ListPopupWindow.
     */
    public class Builder {
        private boolean mIsModal;
        private boolean mHasDismissListener;
        private boolean mHasItemClickListener;

        private AdapterView.OnItemClickListener mOnItemClickListener;
        private PopupWindow.OnDismissListener mOnDismissListener;

        private int mContentRowLayoutId = androidx.appcompat.R.layout.abc_popup_menu_item_layout;

        private boolean mUseCustomPopupStyle;
        private int mPopupStyleAttr;

        public Builder setModal(boolean isModal) {
            mIsModal = isModal;
            return this;
        }

        public Builder withContentRowLayoutId(int contentRowLayoutId) {
            mContentRowLayoutId = contentRowLayoutId;
            return this;
        }

        public Builder withPopupStyleAttr(int popupStyleAttr) {
            mUseCustomPopupStyle = true;
            mPopupStyleAttr = popupStyleAttr;
            return this;
        }

        public Builder withItemClickListener() {
            mHasItemClickListener = true;
            return this;
        }

        public Builder withDismissListener() {
            mHasDismissListener = true;
            return this;
        }

        private void configure() {
            final Context context = mContainer.getContext();
            if (mUseCustomPopupStyle) {
                mListPopupWindow = new ListPopupWindow(context, null, mPopupStyleAttr, 0);
            } else {
                mListPopupWindow = new ListPopupWindow(context);
            }

            final String[] POPUP_CONTENT =
                    new String[]{"Alice", "Bob", "Charlie", "Deirdre", "El"};
            mListPopupAdapter = new BaseAdapter() {
                class ViewHolder {
                    private TextView title;
                }

                @Override
                public int getCount() {
                    return POPUP_CONTENT.length;
                }

                @Override
                public Object getItem(int position) {
                    return POPUP_CONTENT[position];
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = LayoutInflater.from(parent.getContext()).inflate(
                                mContentRowLayoutId, parent, false);
                        ViewHolder viewHolder = new ViewHolder();
                        viewHolder.title = (TextView) convertView.findViewById(R.id.title);
                        convertView.setTag(viewHolder);
                    }

                    ViewHolder viewHolder = (ViewHolder) convertView.getTag();
                    viewHolder.title.setText(POPUP_CONTENT[position]);
                    return convertView;
                }
            };

            mListPopupWindow.setAdapter(mListPopupAdapter);
            mListPopupWindow.setAnchorView(mButton);

            // The following mock listeners have to be set before the call to show() as
            // they are set on the internally constructed drop down.
            if (mHasItemClickListener) {
                // Wrap our item click listener with a Mockito spy
                mOnItemClickListener = spy(mItemClickListener);
                // Register that spy as the item click listener on the ListPopupWindow
                mListPopupWindow.setOnItemClickListener(mOnItemClickListener);
                // And configure Mockito to call our original listener with onItemClick.
                // This way we can have both our item click listener running to dismiss the popup
                // window, and track the invocations of onItemClick with Mockito APIs.
                doCallRealMethod().when(mOnItemClickListener).onItemClick(
                        any(AdapterView.class), any(View.class), any(int.class), any(int.class));
            }

            if (mHasDismissListener) {
                mOnDismissListener = mock(PopupWindow.OnDismissListener.class);
                mListPopupWindow.setOnDismissListener(mOnDismissListener);
            }

            mListPopupWindow.setModal(mIsModal);
        }

        private void show() {
            configure();
            mListPopupWindow.show();
        }

        public void wireToActionButton() {
            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    show();
                }
            });
        }
    }
}
