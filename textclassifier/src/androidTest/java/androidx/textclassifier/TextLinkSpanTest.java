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
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.textclassifier.TextLinks.TextLink;
import androidx.textclassifier.TextLinks.TextLinkSpan;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

/** Unit tests for {@link TextLinkSpan}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class TextLinkSpanTest {

    private static final IconCompat ICON = IconCompat.createWithData(new byte[0], 0, 0);
    private static final String ENTITY = "myemail@android.com";
    private static final String TEXT = "Email me at " + ENTITY + " tomorrow";
    private static final int START = "Email me at ".length();
    private static final int END = START + ENTITY.length();

    private Context mContext;
    private BlockingReceiver mReceiver;
    private TextLink mTextLink;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mReceiver = BlockingReceiver.registerForPendingIntent(mContext);
        final PendingIntent intent = mReceiver.getPendingIntent();
        final TextClassifierFactory classifierFactory = new TextClassifierFactory() {
            @NonNull
            @Override
            public TextClassifier create(@NonNull TextClassificationContext ctx) {
                return new TextClassifier(ctx) {
                    @NonNull
                    @Override
                    public TextClassification classifyText(@NonNull TextClassification.Request r) {
                        final RemoteActionCompat remoteAction =
                                new RemoteActionCompat(ICON, "title", "desc", intent);
                        remoteAction.setShouldShowIcon(false);
                        return new TextClassification.Builder()
                                .addAction(remoteAction)
                                .build();
                    }
                };
            }
        };
        TextClassificationManager.of(mContext).setTextClassifierFactory(classifierFactory);

        final Map<String, Float> scores = new ArrayMap<>();
        scores.put(TextClassifier.TYPE_EMAIL, 1f);
        mTextLink = new TextLink(0, ENTITY.length(), scores, null);
    }

    @Test
    public void onClick() throws Exception {
        final TextLinkSpan span = new TextLinkSpan(mTextLink);
        final TextView textView = createTextViewWithSpan(span);

        span.onClick(textView);
        mReceiver.assertIntentReceived();
    }

    @Test
    public void onClick_unsupportedWidget() throws Exception {
        new TextLinkSpan(mTextLink).onClick(null);
        new TextLinkSpan(mTextLink).onClick(new View(mContext));
        mReceiver.assertIntentNotReceived();
    }

    @Test
    public void onClick_nonSpannedText() throws Exception {
        final TextLinkSpan span = new TextLinkSpan(mTextLink);
        final TextView textView = new TextView(mContext);
        textView.setText(TEXT);

        span.onClick(textView);

        mReceiver.assertIntentNotReceived();
    }

    @Test
    public void onClick_noActions() throws Exception {
        final TextLinkSpan span = new TextLinkSpan(mTextLink);
        final TextView textView = createTextViewWithSpan(span);
        mTextLink.mClassifierFactory = new TextClassifierFactory() {
            @Override
            public TextClassifier create(TextClassificationContext ctx) {
                return TextClassifier.NO_OP;  // returns no actions.
            }
        };

        span.onClick(textView);

        mReceiver.assertIntentNotReceived();
    }

    @Test
    public void onClick_classifyRequestContent() throws Exception {
        // TODO: Implement when we can sufficiently mock a TextClassifier.
    }

    private TextView createTextViewWithSpan(TextLinkSpan span) {
        final Spannable text = new SpannableString(TEXT);
        text.setSpan(span, START, END, 0);
        final TextView textView = new TextView(mContext);
        textView.setText(text);
        return textView;
    }
}
