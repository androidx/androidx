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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for {@link MediaRouteProviderDescriptor}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class MediaRouteProviderDescriptorTest {

    private static final String FAKE_MEDIA_ROUTE_ID_1 = "fakeMediaRouteId1";
    private static final String FAKE_MEDIA_ROUTE_ID_2 = "fakeMediaRouteId2";
    private static final String FAKE_MEDIA_ROUTE_ID_3 = "fakeMediaRouteId3";
    private static final String FAKE_MEDIA_ROUTE_ID_4 = "fakeMediaRouteId4";
    private static final String FAKE_MEDIA_ROUTE_NAME_1 = "fakeMediaRouteName1";
    private static final String FAKE_MEDIA_ROUTE_NAME_2 = "fakeMediaRouteName2";
    private static final String FAKE_MEDIA_ROUTE_NAME_3 = "fakeMediaRouteName3";
    private static final String FAKE_MEDIA_ROUTE_NAME_4 = "fakeMediaRouteName4";

    private static final MediaRouteDescriptor FAKE_MEDIA_ROUTE_1 = new MediaRouteDescriptor
            .Builder(FAKE_MEDIA_ROUTE_ID_1, FAKE_MEDIA_ROUTE_NAME_1)
            .build();
    private static final MediaRouteDescriptor FAKE_MEDIA_ROUTE_2 = new MediaRouteDescriptor
            .Builder(FAKE_MEDIA_ROUTE_ID_2, FAKE_MEDIA_ROUTE_NAME_2)
            .build();
    private static final MediaRouteDescriptor FAKE_MEDIA_ROUTE_3 = new MediaRouteDescriptor
            .Builder(FAKE_MEDIA_ROUTE_ID_3, FAKE_MEDIA_ROUTE_NAME_3)
            .build();
    private static final MediaRouteDescriptor FAKE_MEDIA_ROUTE_4 = new MediaRouteDescriptor
            .Builder(FAKE_MEDIA_ROUTE_ID_4, FAKE_MEDIA_ROUTE_NAME_4)
            .build();

    @Test
    public void defaultBuilder_createsEmptyDescriptor() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        MediaRouteProviderDescriptor descriptor = builder.build();
        assertTrue(descriptor.getRoutes().isEmpty());
    }

    @Test
    public void builderAddRoute_addsRoute() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        builder.addRoute(FAKE_MEDIA_ROUTE_1);
        builder.addRoute(FAKE_MEDIA_ROUTE_2);
        MediaRouteProviderDescriptor descriptor = builder.build();
        List<MediaRouteDescriptor> routes = descriptor.getRoutes();

        assertEquals(2, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_1, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_1, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_2, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_2, routes.get(1).getName());
    }

    @Test
    public void builderAddRoutes_addsRoutes() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        List<MediaRouteDescriptor> routesList = new ArrayList<>();
        routesList.add(FAKE_MEDIA_ROUTE_3);
        routesList.add(FAKE_MEDIA_ROUTE_4);
        builder.addRoutes(routesList);
        MediaRouteProviderDescriptor descriptor = builder.build();

        List<MediaRouteDescriptor> descriptorRoutes = descriptor.getRoutes();
        assertEquals(2, descriptorRoutes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_3, descriptorRoutes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_3, descriptorRoutes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, descriptorRoutes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_4, descriptorRoutes.get(1).getName());
    }

    @Test
    public void builderSetRoutes_clearsListAndAddsRoutes() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();

        List<MediaRouteDescriptor> routesList = new ArrayList<>();
        routesList.add(FAKE_MEDIA_ROUTE_3);
        routesList.add(FAKE_MEDIA_ROUTE_4);
        builder.setRoutes(routesList);

        MediaRouteProviderDescriptor descriptor = builder.build();
        List<MediaRouteDescriptor> descriptorRoutes = descriptor.getRoutes();
        assertEquals(2, descriptorRoutes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_3, descriptorRoutes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_3, descriptorRoutes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, descriptorRoutes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_4, descriptorRoutes.get(1).getName());
    }

    @Test
    public void builderSetRoutes_deepCopiesList() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();

        List<MediaRouteDescriptor> routesList = new ArrayList<>();
        routesList.add(FAKE_MEDIA_ROUTE_3);
        routesList.add(FAKE_MEDIA_ROUTE_4);
        builder.setRoutes(routesList);
        routesList.add(FAKE_MEDIA_ROUTE_1);

        MediaRouteProviderDescriptor descriptor = builder.build();
        List<MediaRouteDescriptor> descriptorRoutes = descriptor.getRoutes();
        assertEquals(2, descriptorRoutes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_3, descriptorRoutes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_3, descriptorRoutes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, descriptorRoutes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_4, descriptorRoutes.get(1).getName());
    }

    @Test
    public void builderSetRoutes_withNull_createsEmptyDescriptor() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        builder.setRoutes(null);
        MediaRouteProviderDescriptor descriptor = builder.build();
        assertTrue(descriptor.getRoutes().isEmpty());
    }
}
