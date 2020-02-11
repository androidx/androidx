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

package androidx.textclassifier.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.textclassifier.widget.FloatingToolbarEspressoUtils.onFloatingToolbar;
import static androidx.textclassifier.widget.FloatingToolbarEspressoUtils.onFloatingToolbarItem;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Build;
import android.text.Spanned;
import android.view.ActionMode;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.app.RemoteActionCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.textclassifier.test.R;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;


/**
 * Tests for {@link ToolbarController}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
public final class ToolbarControllerTest {

    private static final IconCompat ICON = IconCompat.createWithData(new byte[0], 0, 0);
    private static final String SMART_ACTION = "Action";
    private static final int COPY = android.R.string.copy;
    private static final int SHARE = R.string.abc_share;
    private static final int START = 0;
    private static final int END = 1;
    private static final long TRANSITION_DELAY = 400;

    @Rule
    public ActivityTestRule<? extends FloatingToolbarActivity> mActivityTestRule =
            new ActivityTestRule<>(FloatingToolbarActivity.class);

    private TextView mTextView;
    private ToolbarController mController;
    private RemoteActionCompat mAction;
    private ActionMode mMockActionMode;
    private ActionMode.Callback mMockActionModeCallback;

    @Before
    public void setUp() throws Exception {
        mTextView = mActivityTestRule.getActivity().findViewById(R.id.textview);
        mController = ToolbarController.getInstance(mTextView);
        mAction = new RemoteActionCompat(
                ICON, SMART_ACTION, "description",
                PendingIntent.getActivity(mTextView.getContext(), 0, new Intent(), 0));
        mAction.setShouldShowIcon(false);
        mMockActionMode = mock(ActionMode.class);
        when(mMockActionMode.getType()).thenReturn(ActionMode.TYPE_FLOATING);
        mMockActionModeCallback = mock(ActionMode.Callback.class);
        when(mMockActionModeCallback.onCreateActionMode(any(ActionMode.class), any(Menu.class)))
                .thenReturn(true);
        mTextView.requestFocus();
    }

    @Test
    public void onFocusChange() throws Exception {
        onTextView().perform(showToolbar());
        assertThat(mController.isToolbarShowing()).isTrue();
        onTextView().check(matches(hasBackgroundSpan()));
        onView(withId(R.id.button)).perform(requestFocus());
        assertThat(mController.isToolbarShowing()).isFalse();
        onTextView().check(matches(not(hasBackgroundSpan())));
    }

    @Test
    public void onWindowFocusChange() throws Exception {
        // TODO: Implement when we can depend on uiautomator.
    }

    @Test
    public void onDetachFromWindow() throws Exception {
        onTextView().perform(showToolbar());
        assertThat(mController.isToolbarShowing()).isTrue();
        onTextView().perform(detach());
        assertThat(mController.isToolbarShowing()).isFalse();
    }

    @Test
    public void selectionActionMode() throws Exception {
        mTextView.setCustomSelectionActionModeCallback(mMockActionModeCallback);
        onTextView().perform(showToolbar());
        assertThat(mController.isToolbarShowing()).isTrue();
        onTextView().perform(startSelectionActionMode());
        assertThat(mController.isToolbarShowing()).isFalse();
        verify(mMockActionModeCallback).onCreateActionMode(any(ActionMode.class), any(Menu.class));
    }

    @Test
    public void insertionActionMode() throws Exception {
        mTextView.setCustomInsertionActionModeCallback(mMockActionModeCallback);
        onTextView().perform(showToolbar());
        assertThat(mController.isToolbarShowing()).isTrue();
        onTextView().perform(startInsertionActionMode());
        assertThat(mController.isToolbarShowing()).isTrue();
        verifyZeroInteractions(mMockActionModeCallback);
    }

    @Test
    public void menuItems() throws Exception {
        onTextView().perform(showToolbar());
        onFloatingToolbar().check(matches(hasDescendant(withText(SMART_ACTION))));
        onFloatingToolbar().check(matches(hasDescendant(withText(COPY))));
        onFloatingToolbar().check(matches(hasDescendant(withText(SHARE))));
    }

    @Test
    public void copyAction() throws Exception {
        onTextView().perform(showToolbar());
        final String copy = mTextView.getContext().getString(COPY);
        onFloatingToolbarItem(copy).perform(click());

        final ClipboardManager clipboard =
                mTextView.getContext().getSystemService(ClipboardManager.class);
        final String copied = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
        final String expected = mTextView.getText().toString().substring(START, END);
        assertThat(copied).isEqualTo(expected);
    }

    @Test
    public void shareAction() throws Exception {
        // TODO: Find a way to implement this.
    }

    private ViewInteraction onTextView() {
        return onView(withId(mTextView.getId()));
    }

    private ViewAction showToolbar() {
        return actionWithAssertions(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return is((View) mTextView);
            }

            @Override
            public String getDescription() {
                return "showToolbar";
            }

            @Override
            public void perform(UiController uiController, View view) {
                mController.show(Collections.singletonList(mAction), START, END);
                uiController.loopMainThreadForAtLeast(TRANSITION_DELAY);
            }
        });
    }

    private ViewAction requestFocus() {
        return actionWithAssertions(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "requestFocus";
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.requestFocus();
                uiController.loopMainThreadForAtLeast(TRANSITION_DELAY);
            }
        });
    }

    private ViewAction detach() {
        return actionWithAssertions(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "detach";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((ViewGroup) view.getParent()).removeView(view);
                uiController.loopMainThreadForAtLeast(TRANSITION_DELAY);
            }
        });
    }

    private ViewAction startSelectionActionMode() {
        return actionWithAssertions(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "startSelectionActionMode";
            }

            @Override
            public void perform(UiController uiController, View view) {
                final TextView textView = (TextView) view;
                textView.getCustomSelectionActionModeCallback()
                        .onCreateActionMode(mMockActionMode, mock(Menu.class));
                uiController.loopMainThreadForAtLeast(TRANSITION_DELAY);
            }
        });
    }

    private ViewAction startInsertionActionMode() {
        return actionWithAssertions(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "startInsertionActionMode";
            }

            @Override
            public void perform(UiController uiController, View view) {
                final TextView textView = (TextView) view;
                textView.getCustomInsertionActionModeCallback()
                        .onCreateActionMode(mMockActionMode, mock(Menu.class));
                uiController.loopMainThreadForAtLeast(TRANSITION_DELAY);
            }
        });
    }

    private Matcher<? super View> hasBackgroundSpan() {
        return new BaseMatcher<View>() {
            @Override
            public boolean matches(Object item) {
                final CharSequence text = mTextView.getText();
                if (text instanceof Spanned) {
                    return ((Spanned) text).getSpans(
                            START, END, ToolbarController.BackgroundSpan.class).length > 0;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("hasBackgroundSpan");
            }
        };
    }
}
