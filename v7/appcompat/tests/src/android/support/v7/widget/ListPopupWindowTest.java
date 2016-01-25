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
package android.support.v7.widget;

import android.app.Instrumentation;
import android.graphics.Rect;
import android.os.SystemClock;
import android.support.v7.app.BaseInstrumentationTestCase;
import android.support.v7.appcompat.test.R;
import android.test.suitebuilder.annotation.SmallTest;
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

import org.junit.Before;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ListPopupWindowTest extends BaseInstrumentationTestCase<PopupTestActivity> {
    private FrameLayout mContainer;

    private Button mButton;

    private ListPopupWindow mListPopupWindow;

    private BaseAdapter mListPopupAdapter;

    private int mListPopupClickedItem = -1;

    private boolean mIsDismissedCalled = false;

    private boolean mIsContainerClicked = false;

    public ListPopupWindowTest() {
        super(PopupTestActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        final PopupTestActivity activity = getActivity();
        mContainer = (FrameLayout) activity.findViewById(R.id.container);
        mButton = (Button) mContainer.findViewById(R.id.test_button);
    }

    @Test
    @SmallTest
    public void testBasicContent() {
        Builder popupBuilder = new Builder();
        popupBuilder.wireToActionButton();

        onView(withId(R.id.test_button)).perform(click());
        assertNotNull("Popup window created", mListPopupWindow);
        assertTrue("Popup window showing", mListPopupWindow.isShowing());

        final View mainDecorView = getActivity().getWindow().getDecorView();
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

    @Test
    @SmallTest
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
    @SmallTest
    public void testDismissalViaAPI() throws Throwable {
        Builder popupBuilder = new Builder().withDismissListener();
        popupBuilder.wireToActionButton();

        onView(withId(R.id.test_button)).perform(click());
        assertTrue("Popup window showing", mListPopupWindow.isShowing());

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListPopupWindow.dismiss();
            }
        });

        assertTrue("Dismiss listener called", mIsDismissedCalled);
        assertFalse("Popup window not showing after dismissal", mListPopupWindow.isShowing());
    }

    private void testDismissalViaTouch(boolean setupAsModal) throws Throwable {
        Builder popupBuilder = new Builder().setModal(setupAsModal).withDismissListener();
        popupBuilder.wireToActionButton();

        // Also register a click listener on the top-level container
        mContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsContainerClicked = true;
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
        int emulatedTapY = popupOnScreenXY[1] + mListPopupWindow.getListView().getHeight() +
                rect.top + rect.bottom + 20;

        // The logic below uses Instrumentation to emulate a tap outside the bounds of the
        // displayed list popup window. This tap is then treated by the framework to be "split" as
        // the ACTION_OUTSIDE for the popup itself, as well as DOWN / MOVE / UP for the underlying
        // view root if the popup is not modal.
        // It is not correct to emulate these two sequences separately in the test, as it
        // wouldn't emulate the user-facing interaction for this test. Note that usage
        // of Instrumentation is necessary here since Espresso's actions operate at the level
        // of view or data. Also, we don't want to use View.dispatchTouchEvent directly as
        // that would require emulation of two separate sequences as well.

        Instrumentation instrumentation = getInstrumentation();

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
        assertTrue("Dismiss listener called", mIsDismissedCalled);
        assertFalse("Popup window not showing after outside click", mListPopupWindow.isShowing());

        // Also test that the click outside the popup bounds has been "delivered" to the main
        // container only if the popup is not modal
        assertEquals("Click on underlying container", !setupAsModal, mIsContainerClicked);
    }

    @Test
    @SmallTest
    public void testDismissalOutsideNonModal() throws Throwable {
        testDismissalViaTouch(false);
    }

    @Test
    @SmallTest
    public void testDismissalOutsideModal() throws Throwable {
        testDismissalViaTouch(true);
    }

    @Test
    @SmallTest
    public void testItemClickViaEvent() {
        Builder popupBuilder = new Builder().withItemClickListener();
        popupBuilder.wireToActionButton();

        onView(withId(R.id.test_button)).perform(click());
        assertTrue("Popup window showing", mListPopupWindow.isShowing());

        assertEquals("Clicked item before click", -1, mListPopupClickedItem);

        onView(withText("Charlie"))
                .inRoot(withDecorView(not(is(getActivity().getWindow().getDecorView()))))
                .perform(click());
        assertEquals("Clicked item after click", 2, mListPopupClickedItem);
        // Our item click listener also dismisses the popup
        assertFalse("Popup window not showing after click", mListPopupWindow.isShowing());
    }

    @Test
    @SmallTest
    public void testItemClickViaAPI() throws Throwable {
        Builder popupBuilder = new Builder().withItemClickListener();
        popupBuilder.wireToActionButton();

        onView(withId(R.id.test_button)).perform(click());
        assertTrue("Popup window showing", mListPopupWindow.isShowing());

        assertEquals("Clicked item before click", -1, mListPopupClickedItem);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListPopupWindow.performItemClick(1);
            }
        });

        assertEquals("Clicked item after click", 1, mListPopupClickedItem);
        // Our item click listener also dismisses the popup
        assertFalse("Popup window not showing after click", mListPopupWindow.isShowing());
    }

    /**
     * Inner helper class to configure an instance of <code>ListPopupWindow</code> for the
     * specific test. The main reason for its existence is that once a popup window is shown
     * with the show() method, most of its configuration APIs are no-ops. This means that
     * we can't add logic that is specific to a certain test (such as dismissing a non-modal
     * popup window) once it's shown and we have a reference to a displayed ListPopupWindow.
     */
    private class Builder {
        private boolean mIsModal;
        private boolean mHasDismissListener;
        private boolean mHasItemClickListener;

        public Builder() {
        }

        public Builder setModal(boolean isModal) {
            mIsModal = isModal;
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

        private void show() {
            mListPopupWindow = new ListPopupWindow(mContainer.getContext());

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
                                R.layout.abc_popup_menu_item_layout, parent, false);
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

            // The following listeners have to be set before the call to show() as
            // they are set on the internally constructed drop down.
            if (mHasItemClickListener) {
                mListPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
                        mListPopupClickedItem = position;
                        mListPopupWindow.dismiss();
                    }
                });
            }

            if (mHasDismissListener) {
                mListPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        mIsDismissedCalled = true;
                    }
                });
            }

            mListPopupWindow.setModal(mIsModal);
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
