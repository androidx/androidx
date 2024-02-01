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
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_BUNDLE_ARRAY_KEY;
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
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_COMPONENT_BROWSE_LIST;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.content.ComponentName;
import android.os.Bundle;

import androidx.car.app.mediaextensions.analytics.client.AnalyticsCallback;
import androidx.car.app.mediaextensions.analytics.client.AnalyticsParser;
import androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent;
import androidx.car.app.mediaextensions.analytics.event.BrowseChangeEvent;
import androidx.car.app.mediaextensions.analytics.event.ErrorEvent;
import androidx.car.app.mediaextensions.analytics.event.MediaClickedEvent;
import androidx.car.app.mediaextensions.analytics.event.ViewChangeEvent;
import androidx.car.app.mediaextensions.analytics.event.VisibleItemsEvent;

import com.google.common.collect.Lists;

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
public class AnalyticsParserTest {
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
    public void isAnalyticsActionTest() {
        String action = "incorrect";
        boolean isAnalyticsAction = AnalyticsParser.isAnalyticsAction(action);
        assertFalse(isAnalyticsAction);

        action = Constants.ACTION_ANALYTICS;
        isAnalyticsAction = AnalyticsParser.isAnalyticsAction(action);
        assertTrue(isAnalyticsAction);
    }


    @Test
    public void parserCallbackAction() throws InterruptedException {
        Bundle bundle1 = createTestBundle(ANALYTICS_EVENT_MEDIA_CLICKED);
        String mediaID = "test_media_id_1";
        bundle1.putString(ANALYTICS_EVENT_DATA_KEY_MEDIA_ID, mediaID);

        Bundle bundle2 = createTestBundle(ANALYTICS_EVENT_VIEW_CHANGE);
        int viewID = VIEW_COMPONENT_BROWSE_LIST;
        bundle2.putInt(ANALYTICS_EVENT_DATA_KEY_VIEW_COMPONENT, viewID);

        Bundle list = new Bundle();
        list.putParcelableArrayList(ANALYTICS_EVENT_BUNDLE_ARRAY_KEY,
                Lists.newArrayList(bundle1, bundle2));

        AnalyticsParser.parseAnalyticsAction(Constants.ACTION_ANALYTICS, list,
                mExecutor, mAnalyticsCallback);

        verify(mAnalyticsCallback).onMediaClickedEvent(
                (MediaClickedEvent) mEventArgCaptor.capture());
        verify(mAnalyticsCallback).onViewChangeEvent(
                (ViewChangeEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called

        List<AnalyticsEvent> events = mEventArgCaptor.getAllValues();

        MediaClickedEvent clickEvent = (MediaClickedEvent) events.get(0);
        ViewChangeEvent viewEvent = (ViewChangeEvent) events.get(1);

        assertEquals(mediaID, clickEvent.getMediaId());
        assertEquals(viewID, viewEvent.getViewComponent());
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
                        AnalyticsParserTest.class.getPackage().toString(),
                        AnalyticsParserTest.class.getName());
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
        int viewComponent = VIEW_COMPONENT_BROWSE_LIST;
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
        int viewComponent = VIEW_COMPONENT_BROWSE_LIST;
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
    public void parserCallbackErrorEmptyBundleTest() {
        AnalyticsParser.parseAnalyticsBundle(new Bundle(), mExecutor, mAnalyticsCallback);
        verify(mAnalyticsCallback)
                .onErrorEvent((ErrorEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called
        assertTrue(mEventArgCaptor.getValue() instanceof ErrorEvent);
        ErrorEvent event = (ErrorEvent) mEventArgCaptor.getValue();
        int errorCode = event.getErrorCode();
        assertEquals(errorCode, ErrorEvent.ERROR_CODE_INVALID_EVENT);
        int type = event.getEventType();
        assertEquals(type, AnalyticsEvent.EVENT_TYPE_ERROR_EVENT);
    }

    @Test
    public void parserCallbackErrorInvalidActionTest() {
        AnalyticsParser.parseAnalyticsAction("bad_action",
                createTestBundle(ANALYTICS_EVENT_VIEW_CHANGE),
                mExecutor, mAnalyticsCallback);
        verify(mAnalyticsCallback)
                .onErrorEvent((ErrorEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called
        assertTrue(mEventArgCaptor.getValue() instanceof ErrorEvent);
        ErrorEvent event = (ErrorEvent) mEventArgCaptor.getValue();
        int errorCode = event.getErrorCode();
        assertEquals(errorCode, ErrorEvent.ERROR_CODE_INVALID_EVENT);
        int type = event.getEventType();
        assertEquals(type, AnalyticsEvent.EVENT_TYPE_ERROR_EVENT);
    }

    @Test
    public void parserCallbackErrorInvalidExtrasTest() {
        AnalyticsParser.parseAnalyticsAction(Constants.ACTION_ANALYTICS,
                new Bundle(), mExecutor, mAnalyticsCallback);
        verify(mAnalyticsCallback)
                .onErrorEvent((ErrorEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called
        assertTrue(mEventArgCaptor.getValue() instanceof ErrorEvent);
        ErrorEvent event = (ErrorEvent) mEventArgCaptor.getValue();
        int errorCode = event.getErrorCode();
        assertEquals(errorCode, ErrorEvent.ERROR_CODE_INVALID_EXTRAS);
        int type = event.getEventType();
        assertEquals(type, AnalyticsEvent.EVENT_TYPE_ERROR_EVENT);
    }

    @Test
    public void parserCallbackErrorInvalidEventNameTest() {
        Bundle eventBundle = createTestBundle("");
        AnalyticsParser.parseAnalyticsBundle(eventBundle, mExecutor, mAnalyticsCallback);
        verify(mAnalyticsCallback)
                .onErrorEvent((ErrorEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called
        assertTrue(mEventArgCaptor.getValue() instanceof ErrorEvent);
        ErrorEvent event = (ErrorEvent) mEventArgCaptor.getValue();
        int errorCode = event.getErrorCode();
        assertEquals(errorCode, ErrorEvent.ERROR_CODE_INVALID_EVENT);
        int type = event.getEventType();
        assertEquals(type, AnalyticsEvent.EVENT_TYPE_ERROR_EVENT);
    }

    @Test
    public void parserCallbackErrorInvalidBundlesTest() {
        AnalyticsParser.parseAnalyticsAction(Constants.ACTION_ANALYTICS,
                createTestBundle(ANALYTICS_EVENT_VIEW_CHANGE), mExecutor, mAnalyticsCallback);
        verify(mAnalyticsCallback)
                .onErrorEvent((ErrorEvent) mEventArgCaptor.capture());
        verifyNoMoreInteractions(mAnalyticsCallback); // Verify onError not called
        assertTrue(mEventArgCaptor.getValue() instanceof ErrorEvent);
        ErrorEvent event = (ErrorEvent) mEventArgCaptor.getValue();
        int errorCode = event.getErrorCode();
        assertEquals(ErrorEvent.ERROR_CODE_INVALID_BUNDLE, errorCode);
        int type = event.getEventType();
        assertEquals(AnalyticsEvent.EVENT_TYPE_ERROR_EVENT, type);
    }

    private Bundle createTestBundle(String eventName) {
        Bundle bundle = new Bundle();
        bundle.putString(ANALYTICS_EVENT_DATA_KEY_EVENT_NAME, eventName);
        bundle.putLong(ANALYTICS_EVENT_DATA_KEY_TIMESTAMP, System.currentTimeMillis());
        bundle.putInt(ANALYTICS_EVENT_DATA_KEY_VERSION, 1);
        ComponentName componentName =
                new ComponentName(
                        AnalyticsParserTest.class.getPackage().toString(),
                        AnalyticsParserTest.class.getName());
        bundle.putString(
                ANALYTICS_EVENT_DATA_KEY_HOST_COMPONENT_ID, componentName.flattenToString());
        return bundle;
    }
}
