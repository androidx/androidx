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
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.app.PendingIntent;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.core.app.RemoteActionCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
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

/** Unit tests for {@link TextLinkSpan}. */
@SmallTest
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
    public void setUp() {
        mContext = mActivityTestRule.getActivity();
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
        mTextLink = new TextLink(0, ENTITY.length(), scores, null);
    }

    @Test
    public void onClick() throws Exception {
        final TextLinkSpan span = createTextLinkSpan(mTextLink);
        final TextView textView = createTextViewWithSpan(span);

        performSpanClick(span, textView);
        mReceiver.assertIntentReceived();
    }

    @Test
    public void onClick_unsupportedWidget() throws Exception {
        performSpanClick(createTextLinkSpan(mTextLink), null);
        performSpanClick(createTextLinkSpan(mTextLink), new View(mContext));
        mReceiver.assertIntentNotReceived();
    }

    @Test
    public void onClick_nonSpannedText() throws Exception {
        final TextLinkSpan span = createTextLinkSpan(mTextLink);
        final TextView textView = new TextView(mContext);
        textView.setText(TEXT);

        performSpanClick(span, textView);
        mReceiver.assertIntentNotReceived();
    }

    @Test
    public void onClick_noActions() throws Exception {
        mTextClassifier = TextClassifier.NO_OP;
        final TextLinkSpan span = createTextLinkSpan(mTextLink);
        final TextView textView = createTextViewWithSpan(span);

        performSpanClick(span, textView);
        mReceiver.assertIntentNotReceived();
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

    private TextLinks.TextLinkSpan createTextLinkSpan(TextLinks.TextLink textLink) {
        return new TextLinks.TextLinkSpan(
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
}
