/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.os.CancellationSignal;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.BaseMovementMethod;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.os.LocaleListCompat;
import androidx.core.util.Preconditions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link SmartLinkify}.
 */
@SmallTest
public class SmartLinkifyTest {

    private static final TextLinksParams PARAMS = new TextLinksParams.Builder().build();

    @Mock
    private TextClassifier mClassifier;

    private BlockingCallback mCallback;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mCallback = new BlockingCallback();
    }

    @Test
    public void addLinksAsync() {
        final TestLinks testObject = new TestLinks.Builder()
                .addText("Contact: ")
                .addEntity("email@android.com", TextClassifier.TYPE_EMAIL)
                .addText(" © 2018")
                .build();

        when(mClassifier.generateLinks(any(TextLinks.Request.class)))
                .thenReturn(testObject.getTextLinks());
        final Spannable text = testObject.getText();

        SmartLinkify.addLinksAsync(
                text, mClassifier, PARAMS, null /* cancel */, null /* executor */, mCallback);
        mCallback.await();  // Block for the result.

        assertThat(mCallback.getStatus()).isEqualTo(TextLinks.STATUS_LINKS_APPLIED);
        final TextLinks.TextLinkSpan[] spans =
                text.getSpans(0, text.length(), TextLinks.TextLinkSpan.class);
        assertThat(spans).asList().hasSize(1);
        final TextLinks.TextLinkSpan span = spans[0];
        assertThat(text.getSpanStart(span)).isEqualTo(testObject.getStart(span));
        assertThat(text.getSpanEnd(span)).isEqualTo(testObject.getEnd(span));
    }

    @Test
    public void addLinksAsync_stringText() {
        final TestLinks testObject = new TestLinks.Builder()
                .addEntity("email@android.com", TextClassifier.TYPE_EMAIL)
                .build();

        when(mClassifier.generateLinks(any(TextLinks.Request.class)))
                .thenReturn(testObject.getTextLinks());
        final TextView textView = new TextView(InstrumentationRegistry.getTargetContext());
        textView.setText(testObject.getText().toString());
        assertThat(textView.getText()).isNotInstanceOf(Spannable.class);

        SmartLinkify.addLinksAsync(
                textView, mClassifier, PARAMS, null /* cancel */, null /* executor */, mCallback);
        mCallback.await();  // Block for the result.

        assertThat(mCallback.getStatus()).isEqualTo(TextLinks.STATUS_LINKS_APPLIED);
        assertThat(textView.getText()).isInstanceOf(Spannable.class);
        final Spannable text = (Spannable) textView.getText();
        final TextLinks.TextLinkSpan[] spans =
                text.getSpans(0, text.length(), TextLinks.TextLinkSpan.class);
        final TextLinks.TextLinkSpan span = spans[0];
        assertThat(text.getSpanStart(span)).isEqualTo(testObject.getStart(span));
        assertThat(text.getSpanEnd(span)).isEqualTo(testObject.getEnd(span));
    }

    @Test
    public void noLinksFound() {
        final Spannable text = new SpannableString("This is some random text.");
        final TextLinks noLinks = new TextLinks.Builder(text.toString()).build();
        when(mClassifier.generateLinks(any(TextLinks.Request.class))).thenReturn(noLinks);

        SmartLinkify.addLinksAsync(
                text, mClassifier, PARAMS, null /* cancel */, null /* executor */, mCallback);
        mCallback.await();  // Block for the result.

        assertThat(mCallback.getStatus()).isEqualTo(TextLinks.STATUS_NO_LINKS_FOUND);
        final Object[] spans = text.getSpans(0, text.length(), TextLinks.TextLinkSpan.class);
        assertThat(spans).asList().isEmpty();
    }

    @Test
    public void newTextLinksReplaceOldTextLinkSpans() {
        final TestLinks testObject = new TestLinks.Builder()
                .addEntity("9876543", TextClassifier.TYPE_PHONE, TextClassifier.TYPE_FLIGHT_NUMBER)
                .addText(" ")
                .addEntity("XX 987", TextClassifier.TYPE_FLIGHT_NUMBER, TextClassifier.TYPE_OTHER)
                .build();
        final Spannable text = testObject.getText();

        when(mClassifier.generateLinks(any(TextLinks.Request.class)))
                .thenReturn(testObject.getTextLinks());

        // Insert a TextLinkSpan before calling linkify to verify that the linkify call clears it.
        final TextLinks.TextLink oldLink = new TextLinks.TextLink(0, 7, noEntities(), null);
        final TextLinks.TextLinkSpan oldSpan = new TextLinks.TextLinkSpan(oldLink);
        text.setSpan(oldSpan, oldLink.getStart(), oldLink.getEnd(), 0);
        final TextLinks.TextLinkSpan[] oldSpans =
                text.getSpans(0, text.length(), TextLinks.TextLinkSpan.class);
        assertThat(oldSpans).asList().hasSize(1);

        SmartLinkify.addLinksAsync(
                text, mClassifier, PARAMS, null /* cancel */, null /* executor */, mCallback);
        mCallback.await();  // Block for the result.

        assertThat(mCallback.getStatus()).isEqualTo(TextLinks.STATUS_LINKS_APPLIED);
        final TextLinks.TextLinkSpan[] spans =
                text.getSpans(0, text.length(), TextLinks.TextLinkSpan.class);
        assertThat(spans).asList().hasSize(2);
        for (TextLinks.TextLinkSpan span : spans) {
            assertThat(text.getSpanStart(span)).isEqualTo(testObject.getStart(span));
            assertThat(text.getSpanEnd(span)).isEqualTo(testObject.getEnd(span));
        }
    }

    @Test
    public void addsLinkMovementMethod() {
        final TestLinks testObject = new TestLinks.Builder()
                .addEntity("(987) 6543-210", TextClassifier.TYPE_PHONE)
                .build();

        when(mClassifier.generateLinks(any(TextLinks.Request.class)))
                .thenReturn(testObject.getTextLinks());
        final TextView textView = new TextView(InstrumentationRegistry.getTargetContext());
        textView.setText(testObject.getText());
        assertThat(textView.getMovementMethod()).isNull();

        SmartLinkify.addLinksAsync(
                textView, mClassifier, PARAMS, null /* cancel */, null /* executor */, mCallback);
        mCallback.await();  // Block for the result.

        assertThat(mCallback.getStatus()).isEqualTo(TextLinks.STATUS_LINKS_APPLIED);
        assertThat(textView.getMovementMethod()).isInstanceOf(LinkMovementMethod.class);
    }

    @Test
    public void addsLinkMovementMethod_differentMovementMethod() {
        final TestLinks testObject = new TestLinks.Builder()
                .addEntity("(987) 6543-210", TextClassifier.TYPE_PHONE)
                .build();

        when(mClassifier.generateLinks(any(TextLinks.Request.class)))
                .thenReturn(testObject.getTextLinks());
        final TextView textView = new TextView(InstrumentationRegistry.getTargetContext());
        textView.setText(testObject.getText());
        textView.setMovementMethod(new BaseMovementMethod());

        SmartLinkify.addLinksAsync(
                textView, mClassifier, PARAMS, null /* cancel */, null /* executor */, mCallback);
        mCallback.await();  // Block for the result.

        assertThat(mCallback.getStatus()).isEqualTo(TextLinks.STATUS_LINKS_APPLIED);
        assertThat(textView.getMovementMethod()).isInstanceOf(LinkMovementMethod.class);
    }

    @Test
    public void noLinkMovementMethodIfNotClickable() {
        final TestLinks testObject = new TestLinks.Builder()
                .addEntity("(987) 6543-210", TextClassifier.TYPE_PHONE)
                .build();

        when(mClassifier.generateLinks(any(TextLinks.Request.class)))
                .thenReturn(testObject.getTextLinks());
        final TextView textView = new TextView(InstrumentationRegistry.getTargetContext());
        textView.setText(testObject.getText());
        textView.setLinksClickable(false);

        SmartLinkify.addLinksAsync(
                textView, mClassifier, PARAMS, null /* cancel */, null /* executor */, mCallback);
        mCallback.await();  // Block for the result.

        assertThat(mCallback.getStatus()).isEqualTo(TextLinks.STATUS_LINKS_APPLIED);
        assertThat(textView.getMovementMethod()).isNull();
    }

    @Test
    public void cancelLinkify() {
        final long generateLinksDelay = TimeUnit.SECONDS.toMillis(1);
        final TestLinks testObject = new TestLinks.Builder()
                .addEntity("email@android.com", TextClassifier.TYPE_EMAIL)
                .build();
        final Spannable text = testObject.getText();

        final TextClassifier classifier = new TextClassifier(
                new TextClassificationContext("", "", "")) {
            @NonNull
            @Override
            public TextLinks generateLinks(@NonNull TextLinks.Request request) {
                SystemClock.sleep(generateLinksDelay);
                return testObject.getTextLinks();
            }
        };
        final CancellationSignal cancel = new CancellationSignal();
        // Use an executor so this test doesn't corrupt test thread and cause other tests to fail.
        final Executor executor = Executors.newSingleThreadExecutor();
        final SmartLinkify.Callback callback = mock(SmartLinkify.Callback.class);

        SmartLinkify.addLinksAsync(text, classifier, PARAMS, cancel, executor, callback);
        cancel.cancel();
        SystemClock.sleep(generateLinksDelay * 2);

        verifyNoMoreInteractions(callback);
    }

    @Test
    public void useExecutor() {
        final TestLinks testObject = new TestLinks.Builder()
                .addEntity("email@android.com", TextClassifier.TYPE_EMAIL)
                .build();

        when(mClassifier.generateLinks(any(TextLinks.Request.class)))
                .thenReturn(testObject.getTextLinks());
        final Spannable text = testObject.getText();

        final Executor executor = mock(Executor.class);
        verifyZeroInteractions(executor);

        SmartLinkify.addLinksAsync(
                text, mClassifier, PARAMS, null /* cancel */, executor, mCallback);
        mCallback.await();  // Block for the result.

        verify(executor).execute(any(Runnable.class));
    }

    @Test
    public void useParams() {
        final TestLinks testObject = new TestLinks.Builder()
                .addEntity("email@android.com", TextClassifier.TYPE_EMAIL)
                .build();

        final Spannable text = testObject.getText();
        final ArgumentCaptor<TextLinks.Request> requestCapture =
                ArgumentCaptor.forClass(TextLinks.Request.class);
        when(mClassifier.generateLinks(requestCapture.capture()))
                .thenReturn(testObject.getTextLinks());
        when(mClassifier.getMaxGenerateLinksTextLength()).thenReturn(text.length());

        final TextLinks.TextLinkSpan span = new TextLinks.TextLinkSpan(null);
        final TextLinksParams params = new TextLinksParams.Builder()
                .setEntityConfig(new TextClassifier.EntityConfig.Builder().build())
                .setDefaultLocales(LocaleListCompat.create(Locale.CANADA_FRENCH))
                .setSpanFactory(new TextLinks.SpanFactory() {
                    @Override
                    public TextLinks.TextLinkSpan createSpan(TextLinks.TextLink textLink) {
                        return span;
                    }
                })
                .build();

        SmartLinkify.addLinksAsync(
                text, mClassifier, params, null /* cancel */, null /* executor */, mCallback);
        mCallback.await();  // Block for the result.

        assertThat(mCallback.getStatus()).isEqualTo(TextLinks.STATUS_LINKS_APPLIED);
        final TextLinks.Request request = requestCapture.getValue();
        assertThat(request.getText().toString()).isEqualTo(text.toString());
        assertThat(request.getEntityConfig()).isEqualTo(params.getEntityConfig());
        assertThat(request.getDefaultLocales()).isEqualTo(params.getDefaultLocales());
        final TextLinks.TextLinkSpan insertedSpan =
                text.getSpans(0, text.length(), TextLinks.TextLinkSpan.class)[0];
        assertThat(insertedSpan).isEqualTo(span);
    }

    @Test
    public void ignoreApplyStrategy() {
        final TestLinks testObject = new TestLinks.Builder()
                .addEntity("XX 987", TextClassifier.TYPE_FLIGHT_NUMBER)
                .build();
        final Spannable text = testObject.getText();

        when(mClassifier.generateLinks(any(TextLinks.Request.class)))
                .thenReturn(testObject.getTextLinks());

        // Insert a URLSpan before calling linkify to verify that the smart links are ignored.
        final URLSpan urlSpan = new URLSpan("http://flight.android.com/XX-987");
        text.setSpan(urlSpan, 0, text.length(), 0);
        final URLSpan[] urlSpans = text.getSpans(0, text.length(), URLSpan.class);
        assertThat(urlSpans).asList().hasSize(1);
        final TextLinksParams params = new TextLinksParams.Builder()
                .setApplyStrategy(TextLinks.APPLY_STRATEGY_IGNORE)
                .build();

        SmartLinkify.addLinksAsync(
                text, mClassifier, params, null /* cancel */, null /* executor */, mCallback);
        mCallback.await();  // Block for the result.

        assertThat(mCallback.getStatus()).isEqualTo(TextLinks.STATUS_NO_LINKS_APPLIED);
        final TextLinks.TextLinkSpan[] spans =
                text.getSpans(0, text.length(), TextLinks.TextLinkSpan.class);
        assertThat(spans).asList().isEmpty();
    }

    @Test
    public void replaceApplyStrategy() {
        final TestLinks testObject = new TestLinks.Builder()
                .addEntity("XX 987", TextClassifier.TYPE_FLIGHT_NUMBER)
                .build();
        final Spannable text = testObject.getText();

        when(mClassifier.generateLinks(any(TextLinks.Request.class)))
                .thenReturn(testObject.getTextLinks());

        // Insert a URLSpan before calling linkify to verify that the smart link replaces it.
        final URLSpan urlSpan = new URLSpan("http://flight.android.com/XX-987");
        text.setSpan(urlSpan, 0, text.length(), 0);
        final URLSpan[] urlSpans = text.getSpans(0, text.length(), URLSpan.class);
        assertThat(urlSpans).asList().hasSize(1);
        final TextLinksParams params = new TextLinksParams.Builder()
                .setApplyStrategy(TextLinks.APPLY_STRATEGY_REPLACE)
                .build();

        SmartLinkify.addLinksAsync(
                text, mClassifier, params, null /* cancel */, null /* executor */, mCallback);
        mCallback.await();  // Block for the result.

        assertThat(mCallback.getStatus()).isEqualTo(TextLinks.STATUS_LINKS_APPLIED);
        final TextLinks.TextLinkSpan[] spans =
                text.getSpans(0, text.length(), TextLinks.TextLinkSpan.class);
        assertThat(spans).asList().hasSize(1);
        final TextLinks.TextLinkSpan span = spans[0];
        assertThat(text.getSpanStart(span)).isEqualTo(testObject.getStart(span));
        assertThat(text.getSpanEnd(span)).isEqualTo(testObject.getEnd(span));
    }

    @Test
    public void differentText() {
        final TestLinks testObject = new TestLinks.Builder()
                .addEntity("email@android.com", TextClassifier.TYPE_EMAIL)
                .build();

        when(mClassifier.generateLinks(any(TextLinks.Request.class)))
                .thenReturn(testObject.getTextLinks());
        final TextView textView = new TextView(InstrumentationRegistry.getTargetContext());
        textView.setText(testObject.getText());

        SmartLinkify.addLinksAsync(
                textView, mClassifier, PARAMS, null /* cancel */, null /* executor */, mCallback);
        final Spannable text = new SpannableString("different text");
        textView.setText(text);
        mCallback.await();  // Block for the result.

        assertThat(mCallback.getStatus()).isEqualTo(TextLinks.STATUS_DIFFERENT_TEXT);
        assertThat(text).isEqualTo(textView.getText());
        final TextLinks.TextLinkSpan[] spans =
                text.getSpans(0, text.length(), TextLinks.TextLinkSpan.class);
        assertThat(spans).asList().isEmpty();
    }

    private static Map<String, Float> noEntities() {
        final Map<String, Float> scores = new HashMap<>();
        scores.put(TextClassifier.TYPE_UNKNOWN, 1f);
        return scores;
    }

    /**
     * Helper class for building test textlink objects that have been applied to text.
     *
     * <p>e.g.
     *
     * <pre>{@code
     *   // email@android.com is of type "email" (score 0.9) and type "other" (score 0.1)
     *   // 12345 is of type "flight number" (score 0.9) and type "phone" (score 0.1)
     *   TestLinks testObject = new TestLinks.Builder()
     *       .addText("Email: ")
     *       .addEntity("email@android.com", TextClasifier.TYPE_EMAIL, TextClassifier.TYPE_OTHER)
     *       .addText("\nNumber: ")
     *       .addEntity("12345", TextClassifier.TYPE_FLIGHT_NUMBER, TextClassifier.TYPE_PHONE)
     *       .build();
     *
     *   // Get the text.
     *   // i.e. Email: email@android.com\nNumber: 12345
     *   Spannable text = testObject.getText();
     *
     *   // Get start and end index for a link span in the text
     *   int start = testObject.getStart(textLinkSpan);
     *   int end = testObject.getEnd(textLinkSpan);
     *
     *   // Get all the text links
     *   TextLinks textLinks = testObject.getTextLinks();
     * }</pre>
     */
    private static final class TestLinks {

        private final Spannable mText;
        private final TextLinks mTextLinks;

        TestLinks(Spannable text, TextLinks textLinks) {
            mText = Preconditions.checkNotNull(text);
            mTextLinks = Preconditions.checkNotNull(textLinks);
        }

        Spannable getText() {
            return mText;
        }

        TextLinks getTextLinks() {
            return mTextLinks;
        }

        int getStart(TextLinks.TextLinkSpan span) {
            for (TextLinks.TextLink link : mTextLinks.getLinks()) {
                if (span.getTextLink() == link) {
                    return link.getStart();
                }
            }
            return -1;
        }

        int getEnd(TextLinks.TextLinkSpan span) {
            for (TextLinks.TextLink link : mTextLinks.getLinks()) {
                if (span.getTextLink() == link) {
                    return link.getEnd();
                }
            }
            return -1;
        }

        static final class Builder {

            final SpannableStringBuilder mText = new SpannableStringBuilder();
            final Collection<TextLinks.TextLink> mLinks = new ArrayList<>();

            Builder addText(String text) {
                mText.append(text);
                return this;
            }

            Builder addEntity(String text, @TextClassifier.EntityType  String... entities) {
                final int start = mText.length();
                addText(text);
                final int end = mText.length();
                mLinks.add(new TextLinks.TextLink(start, end, entityScores(entities), null));
                return this;
            }

            TestLinks build() {
                final TextLinks.Builder textLinks = new TextLinks.Builder(mText.toString());
                for (TextLinks.TextLink link : mLinks) {
                    textLinks.addLink(link);
                }
                return new TestLinks(mText, textLinks.build());
            }

            private static Map<String, Float> entityScores(String... entities) {
                final Map<String, Float> scores = new HashMap<>();
                double score = 1;
                for (String entity : entities) {
                    scores.put(entity, (float) (0.9 * score));
                    score *= 0.1;
                }
                final String lastEntity = entities[entities.length - 1];
                scores.put(lastEntity, (float) (scores.get(lastEntity) + score));
                return scores;
            }
        }
    }

    private static final class BlockingCallback implements SmartLinkify.Callback {

        private final CountDownLatch mLatch = new CountDownLatch(1);
        private int mStatus = TextLinks.STATUS_UNKNOWN;

        @Override
        public void onLinkify(Spannable text, int status) {
            mStatus = status;
            mLatch.countDown();
        }

        public void await() {
            try {
                mLatch.await(1, TimeUnit.SECONDS); // Don't wait forever for a slow test.
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @TextLinks.Status
        public int getStatus() {
            return mStatus;
        }
    }
}
