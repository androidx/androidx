/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.mediarouter.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.IntentFilter;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for {@link MediaRouteDescriptor}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaRouteDescriptorTest {

    private static final String FAKE_MEDIA_ROUTE_NAME = "fakeMediaRouteName";
    private static final String FAKE_MEDIA_ROUTE_ID_1 = "fakeMediaRouteId1";
    private static final String FAKE_MEDIA_ROUTE_ID_2 = "fakeMediaRouteId2";
    private static final String FAKE_MEDIA_ROUTE_ID_3 = "fakeMediaRouteId3";
    private static final String FAKE_MEDIA_ROUTE_ID_4 = "fakeMediaRouteId4";
    private static final String FAKE_CONTROL_ACTION_1 = "fakeControlAction1";
    private static final String FAKE_CONTROL_ACTION_2 = "fakeControlAction2";

    @Test
    @SmallTest
    public void testBuilder() {
        // Tests addGroupMemberId
        MediaRouteDescriptor routeDescriptor = new MediaRouteDescriptor.Builder(
                FAKE_MEDIA_ROUTE_ID_1, FAKE_MEDIA_ROUTE_NAME)
                .addGroupMemberId(FAKE_MEDIA_ROUTE_ID_2)
                .build();
        final List<String> memberIds1 = routeDescriptor.getGroupMemberIds();
        assertEquals(1, memberIds1.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_2, memberIds1.get(0));

        // Test addGroupMemberIds
        List<String> addingIds = new ArrayList<>();
        addingIds.add(FAKE_MEDIA_ROUTE_ID_3);
        addingIds.add(FAKE_MEDIA_ROUTE_ID_4);
        routeDescriptor = new MediaRouteDescriptor.Builder(routeDescriptor)
                .addGroupMemberIds(addingIds)
                .build();
        final List<String> memberIds2 = routeDescriptor.getGroupMemberIds();
        assertEquals(3, memberIds2.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_2, memberIds2.get(0));
        assertEquals(FAKE_MEDIA_ROUTE_ID_3, memberIds2.get(1));
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, memberIds2.get(2));

        // Test removeGroupMemberId
        routeDescriptor = new MediaRouteDescriptor.Builder(routeDescriptor)
                .removeGroupMemberId(FAKE_MEDIA_ROUTE_ID_3)
                .build();
        final List<String> memberIds3 = routeDescriptor.getGroupMemberIds();
        assertEquals(2, memberIds3.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_2, memberIds3.get(0));
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, memberIds3.get(1));

        // Test clearGroupMemberIds
        routeDescriptor = new MediaRouteDescriptor.Builder(routeDescriptor)
                .clearGroupMemberIds()
                .build();
        final List<String> memberIds4 = routeDescriptor.getGroupMemberIds();
        assertTrue(memberIds4.isEmpty());

        // Test addControlFilter
        routeDescriptor = new MediaRouteDescriptor.Builder(routeDescriptor)
                .addControlFilter(new IntentFilter(FAKE_CONTROL_ACTION_1))
                .addControlFilter(new IntentFilter(FAKE_CONTROL_ACTION_2))
                .build();
        final List<IntentFilter> controlFilters1 = routeDescriptor.getControlFilters();
        assertEquals(2, controlFilters1.size());
        assertEquals(FAKE_CONTROL_ACTION_1, controlFilters1.get(0).getAction(0));
        assertEquals(FAKE_CONTROL_ACTION_2, controlFilters1.get(1).getAction(0));

        // Test clearControlFilters
        routeDescriptor = new MediaRouteDescriptor.Builder(routeDescriptor)
                .clearControlFilters()
                .build();
        final List<IntentFilter> controlFilters2 = routeDescriptor.getControlFilters();
        assertTrue(controlFilters2.isEmpty());
    }
}
