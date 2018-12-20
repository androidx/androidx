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

import static androidx.textclassifier.SelectionEvent.ACTION_COPY;
import static androidx.textclassifier.SelectionEvent.ACTION_SELECT_ALL;
import static androidx.textclassifier.SelectionEvent.INVOCATION_MANUAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Instrumentation unit tests for {@link SelectionEvent}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class SelectionEventTest {
    private static final int START = 1;
    private static final int END = 3;
    private static final int SMART_START = 0;
    private static final int SMART_END = 10;
    private static final int DURATION_SINCE_SESSION_START = 2000;
    private static final int DURATION_SINCE_PREVIOUS_EVENT = 4000;
    private static final String RESULT_ID = "result_id";
    private static final int EVENT_INDEX = 7;
    private static final int INVOCATION = INVOCATION_MANUAL;
    private static final String TEXT = "Testing for some funny texts";
    private static final TextClassificationSessionId SESSION_ID =
            new TextClassificationSessionId("session");
    private static final TextClassificationContext TEXT_CLASSIFICATION_CONTEXT =
            new TextClassificationContext.Builder("pkg", "widget").build();
    private Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testToBundle() {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionActionEvent(START, END, ACTION_COPY);
        selectionEvent.setSmartStart(SMART_START);
        selectionEvent.setSmartEnd(SMART_END);
        selectionEvent.setDurationSinceSessionStart(DURATION_SINCE_SESSION_START);
        selectionEvent.setDurationSincePreviousEvent(DURATION_SINCE_PREVIOUS_EVENT);
        selectionEvent.setResultId(RESULT_ID);
        selectionEvent.setEventIndex(EVENT_INDEX);
        selectionEvent.setInvocationMethod(INVOCATION);
        selectionEvent.setSessionId(SESSION_ID);
        selectionEvent.setTextClassificationSessionContext(TEXT_CLASSIFICATION_CONTEXT);

        Bundle bundle = selectionEvent.toBundle();
        SelectionEvent restored = SelectionEvent.createFromBundle(bundle);
        assertThat(restored).isEqualTo(selectionEvent);
    }

    @Test
    public void testCreateselectionEvent() {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionActionEvent(START, END, ACTION_COPY);
        assertThat(selectionEvent.getAbsoluteStart()).isEqualTo(START);
        assertThat(selectionEvent.getAbsoluteEnd()).isEqualTo(END);
        assertThat(selectionEvent.getEventType()).isEqualTo(ACTION_COPY);
    }

    @Test
    public void testCreateSelectionStartedEvent_withTextClassification() {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionActionEvent(START, END, ACTION_COPY,
                        new TextClassification.Builder().setText(TEXT).build());
        assertThat(selectionEvent.getAbsoluteStart()).isEqualTo(START);
        assertThat(selectionEvent.getAbsoluteEnd()).isEqualTo(END);
        assertThat(selectionEvent.getEntityType())
                .isEqualTo(TextClassifier.TYPE_UNKNOWN);
    }

    @Test
    public void testCreateSelectionModifiedEvent() {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionModifiedEvent(START, END);
        assertThat(selectionEvent.getAbsoluteStart()).isEqualTo(START);
        assertThat(selectionEvent.getAbsoluteEnd()).isEqualTo(END);
        assertThat(selectionEvent.getEventType())
                .isEqualTo(SelectionEvent.EVENT_SELECTION_MODIFIED);
    }

    @Test
    public void testCreateSelectionModifiedEvent_withTextClassification() {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionModifiedEvent(
                        START, END, new TextClassification.Builder().setText(TEXT).build());
        assertThat(selectionEvent.getAbsoluteStart()).isEqualTo(START);
        assertThat(selectionEvent.getAbsoluteEnd()).isEqualTo(END);
        assertThat(selectionEvent.getEventType())
                .isEqualTo(SelectionEvent.EVENT_SELECTION_MODIFIED);
        assertThat(selectionEvent.getEntityType())
                .isEqualTo(TextClassifier.TYPE_UNKNOWN);
    }

    @Test
    public void testCreateSelectionModifiedEvent_withTextSelection() {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionModifiedEvent(
                        START, END, new TextSelection.Builder(START, END).build());
        assertThat(selectionEvent.getAbsoluteStart()).isEqualTo(START);
        assertThat(selectionEvent.getAbsoluteEnd()).isEqualTo(END);
        assertThat(selectionEvent.getEventType())
                .isEqualTo(SelectionEvent.EVENT_AUTO_SELECTION);
        assertThat(selectionEvent.getEntityType())
                .isEqualTo(TextClassifier.TYPE_UNKNOWN);
    }

    @Test
    public void testCreateSelectionStartedEvent() {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionStartedEvent(SelectionEvent.INVOCATION_MANUAL, START);
        assertThat(selectionEvent.getAbsoluteStart()).isEqualTo(START);
        assertThat(selectionEvent.getEventType())
                .isEqualTo(SelectionEvent.EVENT_SELECTION_STARTED);
        assertThat(selectionEvent.getInvocationMethod())
                .isEqualTo(SelectionEvent.INVOCATION_MANUAL);
    }

    @Test
    public void testSmartStart() {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionModifiedEvent(START, END);
        selectionEvent.setSmartStart(START);
        assertThat(selectionEvent.getSmartStart()).isEqualTo(START);
    }

    @Test
    public void testSmartEnd() {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionModifiedEvent(START, END);
        selectionEvent.setSmartEnd(END);
        assertThat(selectionEvent.getSmartEnd()).isEqualTo(END);
    }

    @Test
    public void testIsTerminal() {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionActionEvent(START, END, ACTION_COPY);
        assertThat(selectionEvent.isTerminal()).isTrue();
    }

    @Test
    public void testIsTerminal_False() {
        SelectionEvent selectionEvent =
                SelectionEvent.createSelectionActionEvent(START, END, ACTION_SELECT_ALL);
        assertThat(selectionEvent.isTerminal()).isFalse();
    }

    @SdkSuppress(minSdkVersion = 28)
    public void toPlatform_selectionStartedEvent() {
        SelectionEvent selectionEvent = SelectionEvent.createSelectionStartedEvent(
                INVOCATION_MANUAL,
                START
        );
        android.view.textclassifier.SelectionEvent platformSelectionEvent =
                (android.view.textclassifier.SelectionEvent) selectionEvent.toPlatform(mContext);

        android.view.textclassifier.SelectionEvent expected =
                android.view.textclassifier.SelectionEvent.createSelectionStartedEvent(
                        android.view.textclassifier.SelectionEvent.INVOCATION_MANUAL,
                        START
                );
        assertThat(platformSelectionEvent).isEqualTo(expected);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void toPlatform_selectionModifiedEvent() {
        SelectionEvent selectionEvent = SelectionEvent.createSelectionModifiedEvent(
                START,
                END
        );
        android.view.textclassifier.SelectionEvent platformSelectionEvent =
                (android.view.textclassifier.SelectionEvent) selectionEvent.toPlatform(mContext);

        android.view.textclassifier.SelectionEvent expected =
                android.view.textclassifier.SelectionEvent.createSelectionModifiedEvent(
                        START,
                        END
                );
        assertThat(platformSelectionEvent).isEqualTo(expected);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void toPlatform_selectionModifiedEvent_withClassification() {
        SelectionEvent selectionEvent = SelectionEvent.createSelectionModifiedEvent(
                START,
                END,
                new TextClassification.Builder().setText(TEXT).build()
        );
        android.view.textclassifier.SelectionEvent platformSelectionEvent =
                (android.view.textclassifier.SelectionEvent) selectionEvent.toPlatform(mContext);

        android.view.textclassifier.SelectionEvent expected =
                android.view.textclassifier.SelectionEvent.createSelectionModifiedEvent(
                        START,
                        END,
                        new android.view.textclassifier.TextClassification.Builder()
                                .setText(TEXT).build()
                );
        assertThat(platformSelectionEvent).isEqualTo(expected);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void toPlatform_selectionModifiedEvent_autoSelection() {
        SelectionEvent selectionEvent = SelectionEvent.createSelectionModifiedEvent(
                START,
                END,
                new TextSelection.Builder(START, END).build()
        );
        android.view.textclassifier.SelectionEvent platformSelectionEvent =
                (android.view.textclassifier.SelectionEvent) selectionEvent.toPlatform(mContext);

        android.view.textclassifier.SelectionEvent expected =
                android.view.textclassifier.SelectionEvent.createSelectionModifiedEvent(
                        START,
                        END,
                        new android.view.textclassifier.TextSelection.Builder(START, END).build()
                );
        assertThat(platformSelectionEvent).isEqualTo(expected);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void toPlatform_selectionActionEvent() {
        SelectionEvent selectionEvent = SelectionEvent.createSelectionActionEvent(
                START,
                END,
                ACTION_COPY
        );
        android.view.textclassifier.SelectionEvent platformSelectionEvent =
                (android.view.textclassifier.SelectionEvent) selectionEvent.toPlatform(mContext);

        android.view.textclassifier.SelectionEvent expected =
                android.view.textclassifier.SelectionEvent.createSelectionActionEvent(
                        START,
                        END,
                        android.view.textclassifier.SelectionEvent.ACTION_COPY
                );
        assertThat(platformSelectionEvent).isEqualTo(expected);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void toPlatform_selectionActionEvent_withClassification() {
        SelectionEvent selectionEvent = SelectionEvent.createSelectionActionEvent(
                START,
                END,
                ACTION_COPY,
                new TextClassification.Builder().setText(TEXT).build()
        );
        android.view.textclassifier.SelectionEvent platformSelectionEvent =
                (android.view.textclassifier.SelectionEvent) selectionEvent.toPlatform(mContext);

        android.view.textclassifier.SelectionEvent expected =
                android.view.textclassifier.SelectionEvent.createSelectionActionEvent(
                        START,
                        END,
                        ACTION_COPY,
                        new android.view.textclassifier.TextClassification.Builder()
                                .setText(TEXT).build()
                );
        assertThat(platformSelectionEvent).isEqualTo(expected);
    }
}
