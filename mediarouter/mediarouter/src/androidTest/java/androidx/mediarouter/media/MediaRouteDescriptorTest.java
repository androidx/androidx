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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.content.IntentFilter;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static final String FAKE_PACKAGE_NAME = "com.sample.example";

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

    @Test
    @SmallTest
    public void testDefaultVisibilityIsPublic() {
        MediaRouteDescriptor routeDescriptor = new MediaRouteDescriptor.Builder(
                FAKE_MEDIA_ROUTE_ID_1, FAKE_MEDIA_ROUTE_NAME)
                .build();

        assertTrue(routeDescriptor.isVisibilityPublic());
    }

    @Test
    @SmallTest
    public void testIsVisibilityRestricted() {
        Set<String> allowedPackages = new HashSet<>();
        allowedPackages.add(FAKE_PACKAGE_NAME);
        MediaRouteDescriptor routeDescriptor = new MediaRouteDescriptor.Builder(
                FAKE_MEDIA_ROUTE_ID_1, FAKE_MEDIA_ROUTE_NAME)
                .setVisibilityRestricted(allowedPackages)
                .build();

        assertFalse(routeDescriptor.isVisibilityPublic());
    }

    @Test
    @SmallTest
    public void testGetAllowedPackagesReturnsNewInstance() {
        Set<String> sampleAllowedPackages = new HashSet<>();
        sampleAllowedPackages.add(FAKE_PACKAGE_NAME);
        MediaRouteDescriptor routeDescriptor = new MediaRouteDescriptor.Builder(
                FAKE_MEDIA_ROUTE_ID_1, FAKE_MEDIA_ROUTE_NAME)
                .setVisibilityRestricted(sampleAllowedPackages)
                .build();

        Set<String> allowedPackages = routeDescriptor.getAllowedPackages();

        assertEquals(sampleAllowedPackages, allowedPackages);
        assertNotSame(sampleAllowedPackages, allowedPackages);
    }

    @Test
    @SmallTest
    public void testGetControlFiltersReturnsNewInstance() {
        IntentFilter f1 = new IntentFilter();
        f1.addCategory("com.example.androidx.media.CATEGORY_SAMPLE_ROUTE");
        f1.addAction("com.example.androidx.media.action.TAKE_SNAPSHOT");

        IntentFilter f2 = new IntentFilter();
        f2.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        f2.addAction(MediaControlIntent.ACTION_PLAY);
        f2.addDataScheme("http");
        f2.addDataScheme("https");
        f2.addDataScheme("rtsp");
        f2.addDataScheme("file");

        IntentFilter f3 = new IntentFilter();
        f3.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        f3.addAction(MediaControlIntent.ACTION_SEEK);
        f3.addAction(MediaControlIntent.ACTION_GET_STATUS);
        f3.addAction(MediaControlIntent.ACTION_PAUSE);
        f3.addAction(MediaControlIntent.ACTION_RESUME);
        f3.addAction(MediaControlIntent.ACTION_STOP);

        List<IntentFilter> sampleControlFilters = new ArrayList<>();
        sampleControlFilters.add(f1);
        sampleControlFilters.add(f2);
        sampleControlFilters.add(f3);

        MediaRouteDescriptor routeDescriptor = new MediaRouteDescriptor.Builder(
                FAKE_MEDIA_ROUTE_ID_1, FAKE_MEDIA_ROUTE_NAME)
                .addControlFilter(f1)
                .addControlFilter(f2)
                .addControlFilter(f3)
                .build();

        List<IntentFilter> controlFilters = routeDescriptor.getControlFilters();

        assertEquals(sampleControlFilters, controlFilters);
        assertNotSame(sampleControlFilters, controlFilters);
    }

    @Test
    @SmallTest
    public void testGetGroupMemberIdsReturnsNewInstance() {
        List<String> sampleGroupMemberIds = new ArrayList<>();
        sampleGroupMemberIds.add(FAKE_MEDIA_ROUTE_ID_2);
        sampleGroupMemberIds.add(FAKE_MEDIA_ROUTE_ID_3);
        sampleGroupMemberIds.add(FAKE_MEDIA_ROUTE_ID_4);
        MediaRouteDescriptor routeDescriptor = new MediaRouteDescriptor.Builder(
                FAKE_MEDIA_ROUTE_ID_1, FAKE_MEDIA_ROUTE_NAME)
                .addGroupMemberId(FAKE_MEDIA_ROUTE_ID_2)
                .addGroupMemberId(FAKE_MEDIA_ROUTE_ID_3)
                .addGroupMemberId(FAKE_MEDIA_ROUTE_ID_4)
                .build();

        List<String> groupMemberIds = routeDescriptor.getGroupMemberIds();

        assertEquals(sampleGroupMemberIds, groupMemberIds);
        assertNotSame(sampleGroupMemberIds, groupMemberIds);
    }

    @Test
    @SmallTest
    public void testConstructorUsingBundleReturnsEmptyCollections() {
        MediaRouteDescriptor routeDescriptor = new MediaRouteDescriptor(new Bundle());

        assertTrue(routeDescriptor.getAllowedPackages().isEmpty());
        assertTrue(routeDescriptor.getControlFilters().isEmpty());
        assertTrue(routeDescriptor.getGroupMemberIds().isEmpty());
    }
}
