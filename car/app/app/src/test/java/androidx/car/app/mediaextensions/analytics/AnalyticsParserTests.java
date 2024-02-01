/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.car.app.mediaextensions.analytics;

import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_BROWSE_NODE_CHANGE;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_EVENT_NAME;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_HOST_COMPONENT_ID;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_ITEM_IDS;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_MEDIA_ID;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_TIMESTAMP;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_VERSION;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_VIEW_ACTION;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_VIEW_COMPONENT;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_MEDIA_CLICKED;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_VIEW_CHANGE;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_VISIBLE_ITEMS;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import androidx.car.app.mediaextensions.analytics.client.AnalyticsCallback;
import androidx.car.app.mediaextensions.analytics.client.AnalyticsParser;
import androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent;
import androidx.car.app.mediaextensions.analytics.event.BrowseChangeEvent;
import androidx.car.app.mediaextensions.analytics.event.ErrorEvent;
import androidx.car.app.mediaextensions.analytics.event.MediaClickedEvent;
import androidx.car.app.mediaextensions.analytics.event.ViewChangeEvent;
import androidx.car.app.mediaextensions.analytics.event.VisibleItemsEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class AnalyticsParserTests {
    @Rule public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    AnalyticsCallback mAnalyticsCallback;

    @Captor ArgumentCaptor<AnalyticsEvent> mEventArgCaptor;

    AutoCloseable mOpenMocks;

    Executor mExecutor = Runnable::run;

    @Before
    public void setup() {
        mOpenMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        mOpenMocks.close();
    }

    @Test
    public void parserCallbackTimeComponentTest() throws InterruptedException {
        Bundle bundle = createTestBundle(ANALYTICS_EVENT_MEDIA_CLICKED);
        String mediaID = "test_media_id_1";
        bundle.putString(ANALYTICS_EVENT_DATA_KEY_MEDIA_ID, mediaID);
        AnalyticsParser.parseAnalyticsBundle(bundle, mExecutor, mAnalyticsCallback);
        verify(mAnalyticsCallback).onMediaClickedEvent(
                (MediaClickedEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called
        assertNotNull(mEventArgCaptor.getValue());
        AnalyticsEvent event = mEventArgCaptor.getValue();
        ComponentName componentName =
                new ComponentName(
                        AnalyticsParserTests.class.getPackage().toString(),
                        AnalyticsParserTests.class.getName());
        assertEquals(componentName.flattenToString(), event.getComponent());
        assertTrue(event.getTimestampMillis() > 0);
        assertTrue(event.getAnalyticsVersion() > 0);
    }

    @Test
    public void parserCallbackMediaClickedTest() {
        Bundle bundle = createTestBundle(ANALYTICS_EVENT_MEDIA_CLICKED);
        String mediaID = "test_media_id_1";
        bundle.putString(ANALYTICS_EVENT_DATA_KEY_MEDIA_ID, mediaID);
        AnalyticsParser.parseAnalyticsBundle(bundle, mExecutor, mAnalyticsCallback);
        verify(mAnalyticsCallback)
                .onMediaClickedEvent((MediaClickedEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called
        assertTrue(mEventArgCaptor.getValue() instanceof MediaClickedEvent);
        MediaClickedEvent event = (MediaClickedEvent) mEventArgCaptor.getValue();
        String eventMediaId = event.getMediaId();
        assertEquals(eventMediaId, mediaID);
    }

    @Test
    public void parserCallbackVisibleItemsTest() {
        Bundle bundle = createTestBundle(ANALYTICS_EVENT_VISIBLE_ITEMS);
        List<String> visibleItems = Arrays.asList("test_media_id_1", "test_media_id_2",
                "test_media_id_3");
        bundle.putStringArrayList(ANALYTICS_EVENT_DATA_KEY_ITEM_IDS,
                new ArrayList<>(visibleItems));
        AnalyticsParser.parseAnalyticsBundle(bundle, mExecutor, mAnalyticsCallback);
        verify(mAnalyticsCallback)
                .onVisibleItemsEvent((VisibleItemsEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called
        assertTrue(mEventArgCaptor.getValue() instanceof VisibleItemsEvent);
        VisibleItemsEvent event = (VisibleItemsEvent) mEventArgCaptor.getValue();
        List<String> eventVisibleItems = event.getItemsIds();
        assertArrayEquals(eventVisibleItems.toArray(), visibleItems.toArray());
    }

    @Test
    public void parserCallbackBrowseNodeChangeTest() {
        Bundle bundle = createTestBundle(ANALYTICS_EVENT_BROWSE_NODE_CHANGE);
        String node = "test_media_id_1";
        int action = 1;
        bundle.putString(ANALYTICS_EVENT_DATA_KEY_MEDIA_ID, node);
        bundle.putInt(ANALYTICS_EVENT_DATA_KEY_VIEW_ACTION, action);
        AnalyticsParser.parseAnalyticsBundle(bundle, mExecutor, mAnalyticsCallback);
        verify(mAnalyticsCallback)
                .onBrowseNodeChangeEvent((BrowseChangeEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called
        assertTrue(mEventArgCaptor.getValue() instanceof BrowseChangeEvent);
        BrowseChangeEvent event = (BrowseChangeEvent) mEventArgCaptor.getValue();
        String eventBrowseNew = event.getBrowseNodeId();
        int eventAction = event.getViewAction();
        assertEquals(eventAction, action);
        assertEquals(eventBrowseNew, node);
    }

    @Test
    public void parserCallbackViewEntryTest() {
        Bundle bundle = createTestBundle(ANALYTICS_EVENT_VIEW_CHANGE);
        int viewComponent = AnalyticsEvent.VIEW_COMPONENT_BROWSE_LIST;
        bundle.putInt(ANALYTICS_EVENT_DATA_KEY_VIEW_COMPONENT, viewComponent);
        bundle.putInt(ANALYTICS_EVENT_DATA_KEY_VIEW_ACTION, ViewChangeEvent.VIEW_ACTION_SHOW);
        AnalyticsParser.parseAnalyticsBundle(bundle, mExecutor, mAnalyticsCallback);
        verify(mAnalyticsCallback)
                .onViewChangeEvent((ViewChangeEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called
        assertTrue(mEventArgCaptor.getValue() instanceof ViewChangeEvent);
        ViewChangeEvent event = (ViewChangeEvent) mEventArgCaptor.getValue();
        int eventViewType = event.getViewComponent();
        assertEquals(eventViewType, viewComponent);
        int type = event.getViewAction();
        assertEquals(type, ViewChangeEvent.VIEW_ACTION_SHOW);
    }

    @Test
    public void parserCallbackViewExitTest() {
        Bundle bundle = createTestBundle(ANALYTICS_EVENT_VIEW_CHANGE);
        int viewComponent = AnalyticsEvent.VIEW_COMPONENT_BROWSE_LIST;
        bundle.putInt(ANALYTICS_EVENT_DATA_KEY_VIEW_COMPONENT, viewComponent);
        bundle.putInt(ANALYTICS_EVENT_DATA_KEY_VIEW_ACTION, ViewChangeEvent.VIEW_ACTION_HIDE);
        AnalyticsParser.parseAnalyticsBundle(bundle, mExecutor, mAnalyticsCallback);
        verify(mAnalyticsCallback)
                .onViewChangeEvent((ViewChangeEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called
        assertTrue(mEventArgCaptor.getValue() instanceof ViewChangeEvent);
        ViewChangeEvent event = (ViewChangeEvent) mEventArgCaptor.getValue();
        int eventViewType = event.getViewComponent();
        assertEquals(eventViewType, viewComponent);
        int type = event.getViewAction();
        assertEquals(type, ViewChangeEvent.VIEW_ACTION_HIDE);
    }

    @Test
    public void parserCallbackErrorTest() {
        Intent intent = new Intent();
        intent.putExtras(new Bundle());
        AnalyticsParser.parseAnalyticsIntent(intent, mExecutor, mAnalyticsCallback);
        verify(mAnalyticsCallback)
                .onErrorEvent((ErrorEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called
        assertTrue(mEventArgCaptor.getValue() instanceof ErrorEvent);
        ErrorEvent event = (ErrorEvent) mEventArgCaptor.getValue();
        int errorCode = event.getErrorCode();
        assertEquals(errorCode, ErrorEvent.ERROR_CODE_INVALID_INTENT);
        int type = event.getEventType();
        assertEquals(type, AnalyticsEvent.EVENT_TYPE_ERROR_EVENT);
    }

    private Bundle createTestBundle(String eventName) {
        Bundle bundle = new Bundle();
        bundle.putString(ANALYTICS_EVENT_DATA_KEY_EVENT_NAME, eventName);
        bundle.putLong(ANALYTICS_EVENT_DATA_KEY_TIMESTAMP, System.currentTimeMillis());
        bundle.putInt(ANALYTICS_EVENT_DATA_KEY_VERSION, 1);
        ComponentName componentName =
                new ComponentName(
                        AnalyticsParserTests.class.getPackage().toString(),
                        AnalyticsParserTests.class.getName());
        bundle.putString(
                ANALYTICS_EVENT_DATA_KEY_HOST_COMPONENT_ID, componentName.flattenToString());
        return bundle;
    }
}
