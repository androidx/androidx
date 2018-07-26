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

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.textclassifier.service.DummyTextClassifierService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

@SmallTest
public class RemoteServiceTextClassifierTest {
    public static final int START = 4;
    public static final int END = 6;
    public static final String ENTITY_TYPE = TextClassifier.TYPE_ADDRESS;
    public static final float SCORE = 0.3f;
    public static final String TEXT = "tax for testing text";

    private RemoteServiceTextClassifier mTextClassifier;

    @Before
    public void setup() {
        mTextClassifier = new RemoteServiceTextClassifier(
                InstrumentationRegistry.getContext(),
                new TextClassificationContext.Builder(
                        "pkg", TextClassifier.WIDGET_TYPE_TEXTVIEW).build(),
                InstrumentationRegistry.getContext().getPackageName());

        DummyTextClassifierService.setTextSelection(new TextSelection.Builder(START, END).build());
        DummyTextClassifierService.setTextLinks(new TextLinks.Builder(TEXT).addLink(START, END,
                Collections.singletonMap(ENTITY_TYPE, SCORE)).build());
        DummyTextClassifierService.setTextClassification(
                new TextClassification.Builder()
                        .setText(TEXT).setEntityType(ENTITY_TYPE, SCORE).build());
    }

    @After
    public void tearDown() {
        DummyTextClassifierService.cleanup();
    }

    @Test
    public void testTextSelection() {
        TextSelection.Request request =
                new TextSelection.Request.Builder(TEXT, START, END).build();
        TextSelection textSelection = mTextClassifier.suggestSelection(request);

        assertThat(textSelection).isNotNull();
        assertThat(textSelection.getSelectionStartIndex()).isEqualTo(START);
        assertThat(textSelection.getSelectionEndIndex()).isEqualTo(END);
    }

    @Test
    public void testTextClassification() {
        TextClassification.Request request =
                new TextClassification.Request.Builder(TEXT, START, END).build();
        TextClassification textClassification = mTextClassifier.classifyText(request);
        assertThat(textClassification).isNotNull();
        assertThat(textClassification.getEntityTypeCount()).isEqualTo(1);
        assertThat(textClassification.getEntityType(0)).isEqualTo(ENTITY_TYPE);
    }

    @Test
    public void testTextLinks() {
        TextLinks.Request request = new TextLinks.Request.Builder(TEXT).build();
        TextLinks textLinks = mTextClassifier.generateLinks(request);
        assertThat(textLinks).isNotNull();
        assertThat(textLinks.getLinks().size()).isEqualTo(1);
        TextLinks.TextLink textLink = textLinks.getLinks().iterator().next();
        assertThat(textLink.getEntity(0)).isEqualTo(ENTITY_TYPE);
    }

    @Test
    public void testOnSelectionEvent() {
        SelectionEvent selectionEvent = SelectionEvent.createSelectionModifiedEvent(0, 10);
        mTextClassifier.onSelectionEvent(selectionEvent);
        SelectionEvent lastReportedSelectionEvent =
                DummyTextClassifierService.getLastReportedSelectionEvent();
        assertThat(lastReportedSelectionEvent).isEqualTo(selectionEvent);
    }
}
