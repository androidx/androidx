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

package androidx.textclassifier;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.textclassifier.widget.FloatingToolbarEspressoUtils.onFloatingToolbar;
import static androidx.textclassifier.widget.FloatingToolbarEspressoUtils.onFloatingToolbarItem;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.core.app.RemoteActionCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.textclassifier.TextLinks.TextLink;
import androidx.textclassifier.TextLinks.TextLinkSpan;
import androidx.textclassifier.test.R;
import androidx.textclassifier.widget.FloatingToolbarActivity;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Unit tests for {@link TextLinkSpan}. */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class TextLinkSpanTest {

    private static final IconCompat ICON = IconCompat.createWithData(new byte[0], 0, 0);
    private static final String ITEM = "¯\\_(ツ)_/¯";
    private static final String ENTITY = "myemail@android.com";
    private static final String TEXT = "Email me at " + ENTITY + " tomorrow";
    private static final int START = "Email me at ".length();
    private static final int END = START + ENTITY.length();

    @Rule
    public ActivityTestRule<? extends FloatingToolbarActivity> mActivityTestRule =
            new ActivityTestRule<>(FloatingToolbarActivity.class);

    private Context mContext;
    private BlockingReceiver mReceiver;
    private TextLink mTextLink;
    private TextClassifier mTextClassifier;

    @Before
    public void setUp() throws Throwable {
        Activity activity = mActivityTestRule.getActivity();
        TestUtils.keepScreenOn(mActivityTestRule, activity);
        mContext = activity;
        mReceiver = BlockingReceiver.registerForPendingIntent(mContext);
        final PendingIntent intent = mReceiver.getPendingIntent();
        mTextClassifier = new TextClassifier() {
            @Override
            public TextClassification classifyText(@NonNull TextClassification.Request r) {
                final RemoteActionCompat remoteAction =
                        new RemoteActionCompat(ICON, ITEM, "desc", intent);
                remoteAction.setShouldShowIcon(false);
                return new TextClassification.Builder()
                        .addAction(remoteAction)
                        .build();
            }
        };

        final Map<String, Float> scores = new ArrayMap<>();
        scores.put(TextClassifier.TYPE_EMAIL, 1f);
        mTextLink = new TextLink(0, ENTITY.length(), scores);
    }

    @Test
    public void onClick() throws Exception {
        final TextLinkSpan span = createDefaultTextLinkSpan(mTextLink);
        final TextView textView = createTextViewWithSpan(span);

        performSpanClick(span, textView);
        maybeClickMenu();
        mReceiver.assertIntentReceived();
    }

    @Test
    public void onClick_unsupportedWidget() throws Exception {
        performSpanClick(createDefaultTextLinkSpan(mTextLink), null);
        performSpanClick(createDefaultTextLinkSpan(mTextLink), new View(mContext));
        mReceiver.assertIntentNotReceived();
    }

    @Test
    public void onClick_nonSpannedText() throws Exception {
        final TextLinkSpan span = createDefaultTextLinkSpan(mTextLink);
        final TextView textView = new TextView(mContext);
        textView.setText(TEXT);

        performSpanClick(span, textView);
        mReceiver.assertIntentNotReceived();
    }

    @Test
    public void onClick_noActions() throws Exception {
        mTextClassifier = TextClassifier.NO_OP;
        final TextLinkSpan span = createDefaultTextLinkSpan(mTextLink);
        final TextView textView = createTextViewWithSpan(span);

        performSpanClick(span, textView);
        maybeCheckMenuIsDisplayed();
        mReceiver.assertIntentNotReceived();
    }

    @Test
    public void overrideOnTextClassificationResult() throws Exception {
        final AtomicBoolean callbackIsInvoked = new AtomicBoolean(false);
        final TextLinkSpan span = new TextLinks.DefaultTextLinkSpan(
                new TextLinks.TextLinkSpanData(mTextLink, mTextClassifier, null)) {
            @Override
            public void onTextClassificationResult(
                    TextView textView, TextClassification textClassification) {
                callbackIsInvoked.set(true);
                RemoteActionCompat action = textClassification.getActions().get(0);
                try {
                    action.getActionIntent().send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e("TextLinkSpanTest", "onTextClassificationResult: ", e);
                }
            }
        };
        final TextView textView = createTextViewWithSpan(span);
        performSpanClick(span, textView);
        mReceiver.assertIntentReceived();
        assertThat(callbackIsInvoked.get()).isTrue();
    }

    @Test
    public void customTextLinkSpan() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final TextLinkSpan span = new TextLinks.TextLinkSpan(
                new TextLinks.TextLinkSpanData(mTextLink, mTextClassifier, null)) {
            @Override
            public void onClick(View widget) {
                countDownLatch.countDown();
            }
        };
        final TextView textView = createTextViewWithSpan(span);
        performSpanClick(span, textView);
        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void onClick_classifyRequestContent() throws Exception {
        // TODO: Implement when we can sufficiently mock a TextClassifier.
    }

    private TextView createTextViewWithSpan(TextLinkSpan span) {
        final Spannable text = new SpannableString(TEXT);
        text.setSpan(span, START, END, 0);
        final TextView textView = mActivityTestRule.getActivity().findViewById(R.id.textview);
        onView(withId(R.id.textview)).perform(newSimpleViewAction(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        }));
        return textView;
    }

    private TextLinks.TextLinkSpan createDefaultTextLinkSpan(TextLinks.TextLink textLink) {
        return new TextLinks.DefaultTextLinkSpan(
                new TextLinks.TextLinkSpanData(textLink, mTextClassifier, null));
    }

    private static void performSpanClick(final TextLinkSpan span, final View host) {
        onView(withId(R.id.textview)).perform(newSimpleViewAction(new Runnable() {
            @Override
            public void run() {
                span.onClick(host);
            }
        }));
    }

    private static ViewAction newSimpleViewAction(final Runnable action) {
        return actionWithAssertions(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "SimpleViewAction";
            }

            @Override
            public void perform(UiController uiController, View view) {
                action.run();
            }
        });
    }

    private static void maybeClickMenu() throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            onFloatingToolbarItem(ITEM).perform(click());
        }
    }

    private static void maybeCheckMenuIsDisplayed() throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            onFloatingToolbar().check(matches(isDisplayed()));
        }
    }
}
