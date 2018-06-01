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

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;
import android.os.CancellationSignal;
import android.os.RemoteException;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Instrumentation unit tests for {@link TextClassifierService}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class TextClassifierServiceTest {
    private static final String TEXT = "text is text";
    private static final TextClassificationSessionId SESSION =
            new TextClassificationSessionId("session");
    private static final int START = 1;
    private static final int END = 3;

    private DummyTextClassifierService mTextClassifierService;
    private ITextClassifierService mBinder;
    @Mock
    private ITextClassificationCallback mTextClassificationCallback;
    @Mock
    private ITextLinksCallback mTextLinksCallback;
    @Mock
    private ITextSelectionCallback mTextSelectionCallback;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mTextClassifierService = new DummyTextClassifierService();
        mBinder = (ITextClassifierService) mTextClassifierService.onBind(
                new Intent(TextClassifierService.SERVICE_INTERFACE));
    }

    @Test
    public void testOnClassifyText_nullSession() throws RemoteException {
        TextClassification.Request reference =
                new TextClassification.Request.Builder(TEXT, START, END).build();
        mBinder.onClassifyText(null, reference.toBundle(), mTextClassificationCallback);
        assertThat(mTextClassifierService.getTextClassificationSessionId()).isNull();
        assertThat(mTextClassifierService.getTextLinksRequest()).isNull();
        assertThat(mTextClassifierService.getTextSelectionRequest()).isNull();

        TextClassification.Request textClassificationRequest =
                mTextClassifierService.getTextClassificationRequest();
        assertThat(textClassificationRequest.getStartIndex()).isEqualTo(START);
        assertThat(textClassificationRequest.getEndIndex()).isEqualTo(END);
        assertThat(textClassificationRequest.getText()).isEqualTo(TEXT);

        assertThat(mTextClassifierService.getSelectionEvent()).isNull();
    }

    @Test
    public void testOnClassifyText_withSession() throws RemoteException {
        TextClassification.Request reference =
                new TextClassification.Request.Builder(TEXT, START, END).build();
        mBinder.onClassifyText(
                SESSION.toBundle(), reference.toBundle(), mTextClassificationCallback);
        assertThat(mTextClassifierService.getTextClassificationSessionId()).isEqualTo(SESSION);
        assertThat(mTextClassifierService.getTextLinksRequest()).isNull();
        assertThat(mTextClassifierService.getTextSelectionRequest()).isNull();

        TextClassification.Request textClassificationRequest =
                mTextClassifierService.getTextClassificationRequest();
        assertThat(textClassificationRequest.getText()).isEqualTo(TEXT);
        assertThat(textClassificationRequest.getStartIndex()).isEqualTo(START);
        assertThat(textClassificationRequest.getEndIndex()).isEqualTo(END);

        assertThat(mTextClassifierService.getSelectionEvent()).isNull();
    }

    @Test
    public void testOnGenerateLinks() throws RemoteException {
        TextLinks.Request reference = new TextLinks.Request.Builder(TEXT).build();
        mBinder.onGenerateLinks(SESSION.toBundle(), reference.toBundle(), mTextLinksCallback);
        assertThat(mTextClassifierService.getTextClassificationSessionId()).isEqualTo(SESSION);
        assertThat(mTextClassifierService.getTextLinksRequest().getText()).isEqualTo(TEXT);
        assertThat(mTextClassifierService.getTextSelectionRequest()).isNull();
        assertThat(mTextClassifierService.getTextClassificationRequest()).isNull();
        assertThat(mTextClassifierService.getSelectionEvent()).isNull();
    }

    @Test
    public void testOnSuggestSelection() throws RemoteException {
        TextSelection.Request reference = new TextSelection.Request.Builder(TEXT, START,
                END).build();
        mBinder.onSuggestSelection(SESSION.toBundle(), reference.toBundle(),
                mTextSelectionCallback);
        assertThat(mTextClassifierService.getTextClassificationSessionId()).isEqualTo(SESSION);
        assertThat(mTextClassifierService.getTextLinksRequest()).isNull();

        TextSelection.Request textSelectionRequest =
                mTextClassifierService.getTextSelectionRequest();
        assertThat(textSelectionRequest.getText()).isEqualTo(TEXT);
        assertThat(textSelectionRequest.getStartIndex()).isEqualTo(START);
        assertThat(textSelectionRequest.getEndIndex()).isEqualTo(END);

        assertThat(mTextClassifierService.getTextClassificationRequest()).isNull();
        assertThat(mTextClassifierService.getSelectionEvent()).isNull();
    }

    @Test
    public void testOnSelectionEvent() throws RemoteException {
        SelectionEvent reference = SelectionEvent.createSelectionModifiedEvent(START, END);
        mBinder.onSelectionEvent(SESSION.toBundle(), reference.toBundle());
        assertThat(mTextClassifierService.getTextClassificationSessionId()).isEqualTo(SESSION);
        assertThat(mTextClassifierService.getTextLinksRequest()).isNull();
        assertThat(mTextClassifierService.getTextSelectionRequest()).isNull();
        assertThat(mTextClassifierService.getTextClassificationRequest()).isNull();

        SelectionEvent selectionEvent = mTextClassifierService.getSelectionEvent();
        assertThat(selectionEvent.getAbsoluteStart()).isEqualTo(START);
        assertThat(selectionEvent.getAbsoluteEnd()).isEqualTo(END);
    }

    private static class DummyTextClassifierService extends TextClassifierService {
        private TextClassificationSessionId mTextClassificationSessionId;
        private TextSelection.Request mTextSelectionRequest;
        private TextClassification.Request mTextClassificationRequest;
        private TextLinks.Request mTextLinksRequest;
        private SelectionEvent mSelectionEvent;

        @Override
        public void onSuggestSelection(TextClassificationSessionId sessionId,
                TextSelection.Request request, CancellationSignal cancellationSignal,
                Callback<TextSelection> callback) {
            mTextClassificationSessionId = sessionId;
            mTextSelectionRequest = request;
        }

        @Override
        public void onClassifyText(TextClassificationSessionId sessionId,
                TextClassification.Request request, CancellationSignal cancellationSignal,
                Callback<TextClassification> callback) {
            mTextClassificationSessionId = sessionId;
            mTextClassificationRequest = request;
        }

        @Override
        public void onGenerateLinks(TextClassificationSessionId sessionId,
                TextLinks.Request request,
                CancellationSignal cancellationSignal,
                Callback<TextLinks> callback) {
            mTextClassificationSessionId = sessionId;
            mTextLinksRequest = request;
        }

        @Override
        public void onSelectionEvent(TextClassificationSessionId sessionId, SelectionEvent event) {
            mTextClassificationSessionId = sessionId;
            mSelectionEvent = event;
        }

        public TextClassificationSessionId getTextClassificationSessionId() {
            return mTextClassificationSessionId;
        }

        public TextSelection.Request getTextSelectionRequest() {
            return mTextSelectionRequest;
        }

        public TextClassification.Request getTextClassificationRequest() {
            return mTextClassificationRequest;
        }

        public TextLinks.Request getTextLinksRequest() {
            return mTextLinksRequest;
        }

        public SelectionEvent getSelectionEvent() {
            return mSelectionEvent;
        }
    }
}
