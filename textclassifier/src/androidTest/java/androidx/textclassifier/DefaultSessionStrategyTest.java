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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.support.test.filters.SmallTest;

import androidx.collection.ArraySet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;

@SmallTest
public class DefaultSessionStrategyTest {
    private static final int START = 3;
    private static final int END = 8;

    private static final int MODIFIED_START = 1;
    private static final int MODIFIED_END = 9;

    @Mock
    private TextClassifier mTextClassifier;

    private static final TextClassificationContext TEXT_CLASSIFICATION_CONTEXT =
            new TextClassificationContext.Builder("PKG",
                    TextClassifier.WIDGET_TYPE_TEXTVIEW).build();

    private DefaultSessionStrategy mDefaultSessionStrategy;

    private ArgumentCaptor<SelectionEvent> mSelectionEventArgumentCaptor =
            ArgumentCaptor.forClass(SelectionEvent.class);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mDefaultSessionStrategy = new DefaultSessionStrategy(
                TEXT_CLASSIFICATION_CONTEXT, mTextClassifier);
    }

    @Test
    public void testSanitizeEvent_notStarted() throws InterruptedException {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionActionEvent(START, END, SelectionEvent.ACTION_COPY);
        reportSelectionEvent(selectionEvent);
        verify(mTextClassifier, Mockito.never()).onSelectionEvent(any(SelectionEvent.class));
    }


    @Test
    public void testSanitizeEvent_startEvent() throws InterruptedException {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionStartedEvent(SelectionEvent.INVOCATION_MANUAL, START);
        reportSelectionEvent(selectionEvent);

        verify(mTextClassifier).onSelectionEvent(mSelectionEventArgumentCaptor.capture());
        SelectionEvent captured = mSelectionEventArgumentCaptor.getValue();
        assertStartedEvent(captured);
    }

    @Test
    public void testSanitizeEvent_actionEventAfterStarted() throws InterruptedException {
        SelectionEvent startedSelectionEvent =
                SelectionEvent.createSelectionStartedEvent(SelectionEvent.INVOCATION_MANUAL, START);
        reportSelectionEvent(startedSelectionEvent);
        SelectionEvent actionSelectionEvent = SelectionEvent.createSelectionActionEvent(
                START, END, SelectionEvent.ACTION_COPY);
        reportSelectionEvent(actionSelectionEvent);

        verify(mTextClassifier, times(2))
                .onSelectionEvent(mSelectionEventArgumentCaptor.capture());
        List<SelectionEvent> events = mSelectionEventArgumentCaptor.getAllValues();
        assertThat(events).hasSize(2);
        SelectionEvent capturedStartedEvent = events.get(0);
        SelectionEvent capturedActionEvent = events.get(1);
        assertStartedEvent(capturedStartedEvent);
        assertNonStartedEvent(capturedActionEvent, SelectionEvent.ACTION_COPY);
        assertAllSelectionEventsHaveSameSessionId(events);
    }

    @Test
    public void testSanitizeEvent_modifiedSelectionEvent() throws InterruptedException {
        SelectionEvent startedSelectionEvent =
                SelectionEvent.createSelectionStartedEvent(SelectionEvent.INVOCATION_MANUAL, START);
        reportSelectionEvent(startedSelectionEvent);

        SelectionEvent selectionModifiedEvent = SelectionEvent.createSelectionModifiedEvent(
                MODIFIED_START, MODIFIED_END);
        reportSelectionEvent(selectionModifiedEvent);
        reportSelectionEvent(selectionModifiedEvent);

        // Two identical selection events, the second one should be skipped.
        verify(mTextClassifier, times(2))
                .onSelectionEvent(mSelectionEventArgumentCaptor.capture());
        List<SelectionEvent> events = mSelectionEventArgumentCaptor.getAllValues();
        assertThat(events).hasSize(2);
        SelectionEvent capturedStartedEvent = events.get(0);
        assertStartedEvent(capturedStartedEvent);
        SelectionEvent capturedModifiedSelectionEvent = events.get(1);
        assertSelectionModifiedEvent(capturedModifiedSelectionEvent);
        assertAllSelectionEventsHaveSameSessionId(events);
    }

    private void assertStartedEvent(SelectionEvent selectionEvent) {
        assertThat(selectionEvent.getSessionId()).isNotNull();
        assertThat(selectionEvent.getEventType()).isEqualTo(SelectionEvent.EVENT_SELECTION_STARTED);
        assertThat(selectionEvent.getEventTime()).isGreaterThan(0L);
    }

    private void assertSelectionModifiedEvent(SelectionEvent selectionEvent) {
        assertActionEvent(selectionEvent, SelectionEvent.EVENT_SELECTION_MODIFIED);
        assertThat(selectionEvent.getStart()).isEqualTo(MODIFIED_START - START);
    }

    private void assertActionEvent(
            SelectionEvent selectionEvent, @SelectionEvent.ActionType int actionType) {
        assertNonStartedEvent(selectionEvent, actionType);
    }

    private void assertNonStartedEvent(
            SelectionEvent selectionEvent, @SelectionEvent.EventType int eventType) {
        assertThat(selectionEvent.getSessionId()).isNotNull();
        assertThat(selectionEvent.getEventType()).isEqualTo(eventType);
        assertThat(selectionEvent.getEventTime()).isGreaterThan(0L);
        assertThat(selectionEvent.getDurationSincePreviousEvent()).isGreaterThan(0L);
    }

    private void assertAllSelectionEventsHaveSameSessionId(List<SelectionEvent> selectionEvents) {
        ArraySet<TextClassificationSessionId> sessionIds = new ArraySet<>();
        for (SelectionEvent selectionEvent : selectionEvents) {
            assertThat(sessionIds).isNotNull();
            sessionIds.add(selectionEvent.getSessionId());
        }
        assertThat(sessionIds).hasSize(1);
    }

    private void reportSelectionEvent(SelectionEvent selectionEvent) throws InterruptedException {
        // To make sure there is a little interval between each reported event in order to test
        // setDurationSincePreviousEvent.
        Thread.sleep(10);
        mDefaultSessionStrategy.reportSelectionEvent(selectionEvent);
    }
}
