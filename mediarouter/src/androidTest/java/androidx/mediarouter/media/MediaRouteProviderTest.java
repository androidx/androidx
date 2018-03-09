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

import android.os.Bundle;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Test {@link MediaRouteProvider} and its related classes.
 */
@RunWith(AndroidJUnit4.class)
public class MediaRouteProviderTest {
    private static final String FAKE_MEDIA_ROUTE_ID_1 = "fakeMediaRouteId1";
    private static final String FAKE_MEDIA_ROUTE_ID_2 = "fakeMediaRouteId2";
    private static final String FAKE_MEDIA_ROUTE_ID_3 = "fakeMediaRouteId3";
    private static final String FAKE_MEDIA_ROUTE_ID_4 = "fakeMediaRouteId4";
    private static final String FAKE_MEDIA_ROUTE_NAME_1 = "fakeMediaRouteName1";
    private static final String FAKE_MEDIA_ROUTE_NAME_2 = "fakeMediaRouteName2";
    private static final String FAKE_MEDIA_ROUTE_NAME_3 = "fakeMediaRouteName3";
    private static final String FAKE_MEDIA_ROUTE_NAME_4 = "fakeMediaRouteName4";

    @Test
    @SmallTest
    public void testDescriptorBuilder() {
        // Tests for empty descriptor
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        MediaRouteProviderDescriptor descriptor = builder.build();
        assertTrue(descriptor.getRoutes().isEmpty());

        // Tests for addRoute()
        builder.addRoute(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_1,
                FAKE_MEDIA_ROUTE_NAME_1).build());
        builder.addRoute(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_2,
                FAKE_MEDIA_ROUTE_NAME_2).build());
        descriptor = builder.build();
        List<MediaRouteDescriptor> routes = descriptor.getRoutes();
        assertEquals(2, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_1, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_1, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_2, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_2, routes.get(1).getName());

        // Tests for addRoutes()
        List<MediaRouteDescriptor> otherRoutes = new ArrayList<>();
        otherRoutes.add(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_3,
                FAKE_MEDIA_ROUTE_NAME_3).build());
        otherRoutes.add(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_4,
                FAKE_MEDIA_ROUTE_NAME_4).build());
        builder.addRoutes(otherRoutes);
        descriptor = builder.build();
        routes = descriptor.getRoutes();
        assertEquals(4, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_1, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_1, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_2, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_2, routes.get(1).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_3, routes.get(2).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_3, routes.get(2).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, routes.get(3).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_4, routes.get(3).getName());

        // Tests for setRoutes()
        builder.setRoutes(otherRoutes);
        descriptor = builder.build();
        routes = descriptor.getRoutes();
        assertEquals(2, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_3, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_3, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_4, routes.get(1).getName());

        // Tests setRoutes() for side effects
        otherRoutes.add(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_1,
                FAKE_MEDIA_ROUTE_NAME_1).build());
        descriptor = builder.build();
        routes = descriptor.getRoutes();
        assertEquals(2, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_3, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_3, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_4, routes.get(1).getName());

        // Tests setRoutes against null
        builder.setRoutes(null);
        descriptor = builder.build();
        assertTrue(descriptor.getRoutes().isEmpty());
    }

    @Test
    @SmallTest
    public void testCreateDescriptorBundleForClient() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        builder.addRoute(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_1,
                FAKE_MEDIA_ROUTE_NAME_1).setMaxClientVersion(15).setMinClientVersion(10).build());
        builder.addRoute(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_2,
                FAKE_MEDIA_ROUTE_NAME_2).setMaxClientVersion(18).setMinClientVersion(11).build());
        builder.addRoute(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_3,
                FAKE_MEDIA_ROUTE_NAME_3).setMaxClientVersion(25).setMinClientVersion(16).build());
        builder.addRoute(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_4,
                FAKE_MEDIA_ROUTE_NAME_4).setMaxClientVersion(12).setMinClientVersion(4).build());
        MediaRouteProviderDescriptor descriptor = builder.build();

        Bundle bundle = MediaRouteProviderService
                .createDescriptorBundleForClientVersion(descriptor, 3);
        MediaRouteProviderDescriptor resultDescriptor =
                MediaRouteProviderDescriptor.fromBundle(bundle);
        assertTrue(resultDescriptor.getRoutes().isEmpty());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 4);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        List<MediaRouteDescriptor> routes = resultDescriptor.getRoutes();
        assertEquals(1, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_4, routes.get(0).getName());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 10);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        routes = resultDescriptor.getRoutes();
        assertEquals(2, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_1, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_1, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_4, routes.get(1).getName());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 12);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        routes = resultDescriptor.getRoutes();
        assertEquals(3, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_1, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_1, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_2, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_2, routes.get(1).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, routes.get(2).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_4, routes.get(2).getName());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 15);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        routes = resultDescriptor.getRoutes();
        assertEquals(2, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_1, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_1, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_2, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_2, routes.get(1).getName());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 16);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        routes = resultDescriptor.getRoutes();
        assertEquals(2, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_2, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_2, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_3, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_3, routes.get(1).getName());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 19);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        routes = resultDescriptor.getRoutes();
        assertEquals(1, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_3, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_3, routes.get(0).getName());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 26);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        assertTrue(resultDescriptor.getRoutes().isEmpty());
    }
}
