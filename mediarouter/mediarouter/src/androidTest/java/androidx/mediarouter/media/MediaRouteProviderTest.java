/*
 * Copyright 2024 The Android Open Source Project
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.mediarouter.media.MediaRouteProvider.RouteControllerOptions;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/** Test for {@link MediaRouteProvider}. */
@RunWith(AndroidJUnit4.class)
@UiThreadTest
public class MediaRouteProviderTest {
    private static final String ROUTE_ID = "route_id";
    private static final String CLIENT_PACKAGE_NAME = "client_package_name";

    private Context mContext;
    private MediaRouteProvider.RouteControllerOptions mRouteControllerOptions;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();

        Bundle controlHints = new Bundle();
        controlHints.putBoolean("key", true);
        mRouteControllerOptions =
                new MediaRouteProvider.RouteControllerOptions.Builder()
                        .setControlHints(controlHints)
                        .setClientPackageName(CLIENT_PACKAGE_NAME)
                        .build();
    }

    @Test
    @SmallTest
    public void onCreateRouteControllerWithOptions_shouldProvideOptions() {
        MediaRouteProvider mediaRouteProvider = new TestMediaRouteProvider(mContext);
        TestRouteController routeController =
                (TestRouteController)
                        mediaRouteProvider.onCreateRouteController(
                                ROUTE_ID, mRouteControllerOptions);

        MediaRouteProvider.RouteControllerOptions routeControllerOptions =
                routeController.getRouteControllerOptions();
        assertEquals(mRouteControllerOptions, routeControllerOptions);
        assertEquals(mRouteControllerOptions.asBundle(), routeControllerOptions.asBundle());
        assertEquals(
                mRouteControllerOptions.getControlHints(),
                routeControllerOptions.getControlHints());
        assertEquals(
                mRouteControllerOptions.getClientPackageName(),
                routeControllerOptions.getClientPackageName());
    }

    @Test
    @SmallTest
    public void onCreateRouteController_shouldWorkWithoutOptions() {
        MediaRouteProvider mediaRouteProvider = new TestMediaRouteProviderWithoutOptions(mContext);
        TestRouteController routeController =
                (TestRouteController)
                        mediaRouteProvider.onCreateRouteController(
                                ROUTE_ID, mRouteControllerOptions);

        MediaRouteProvider.RouteControllerOptions routeControllerOptions =
                routeController.getRouteControllerOptions();
        assertNull(routeControllerOptions);
        assertNotEquals(mRouteControllerOptions, routeControllerOptions);
    }

    @Test
    @SmallTest
    public void onCreateDynamicGroupRouteControllerWithOptions_shouldProvideOptions() {
        MediaRouteProvider mediaRouteProvider = new TestMediaRouteProvider(mContext);
        TestDynamicGroupRouteController groupRouteController =
                (TestDynamicGroupRouteController)
                        mediaRouteProvider.onCreateDynamicGroupRouteController(
                                ROUTE_ID, mRouteControllerOptions);

        MediaRouteProvider.RouteControllerOptions routeControllerOptions =
                groupRouteController.getRouteControllerOptions();
        assertEquals(ROUTE_ID, groupRouteController.getInitialMemberRouteId());
        assertEquals(mRouteControllerOptions, routeControllerOptions);
        assertEquals(mRouteControllerOptions.asBundle(), routeControllerOptions.asBundle());
        assertEquals(
                mRouteControllerOptions.getControlHints(),
                routeControllerOptions.getControlHints());
        assertEquals(
                mRouteControllerOptions.getClientPackageName(),
                routeControllerOptions.getClientPackageName());
    }

    @Test
    @SmallTest
    public void onCreateDynamicGroupRouteController_shouldWorkWithoutOptions() {
        MediaRouteProvider mediaRouteProvider = new TestMediaRouteProviderWithoutOptions(mContext);
        TestDynamicGroupRouteController groupRouteController =
                (TestDynamicGroupRouteController)
                        mediaRouteProvider.onCreateDynamicGroupRouteController(
                                ROUTE_ID, mRouteControllerOptions);

        MediaRouteProvider.RouteControllerOptions routeControllerOptions =
                groupRouteController.getRouteControllerOptions();
        assertEquals(ROUTE_ID, groupRouteController.getInitialMemberRouteId());
        assertNull(routeControllerOptions);
        assertNotEquals(mRouteControllerOptions, routeControllerOptions);
    }

    private static class TestMediaRouteProvider extends MediaRouteProvider {

        private TestMediaRouteProvider(@NonNull Context context) {
            super(context);
        }

        @Override
        @Nullable
        public RouteController onCreateRouteController(
                @NonNull String routeId, @NonNull RouteControllerOptions routeControllerOptions) {
            return new TestRouteController(routeControllerOptions);
        }

        @Override
        @Nullable
        public DynamicGroupRouteController onCreateDynamicGroupRouteController(
                @NonNull String initialMemberRouteId,
                @NonNull RouteControllerOptions routeControllerOptions) {
            return new TestDynamicGroupRouteController(
                    initialMemberRouteId, routeControllerOptions);
        }
    }

    private static class TestMediaRouteProviderWithoutOptions extends MediaRouteProvider {

        TestMediaRouteProviderWithoutOptions(Context context) {
            super(context);
        }

        @Override
        @Nullable
        public RouteController onCreateRouteController(@NonNull String routeId) {
            return new TestRouteController(/* routeControllerOptions= */ null);
        }

        @Override
        @Nullable
        public DynamicGroupRouteController onCreateDynamicGroupRouteController(
                @NonNull String initialMemberRouteId) {
            return new TestDynamicGroupRouteController(
                    initialMemberRouteId, /* routeControllerOptions= */ null);
        }
    }

    private static class TestRouteController extends MediaRouteProvider.RouteController {
        @NonNull private final MediaRouteProvider.RouteControllerOptions mRouteControllerOptions;

        private TestRouteController(
                @NonNull MediaRouteProvider.RouteControllerOptions routeControllerOptions) {
            mRouteControllerOptions = routeControllerOptions;
        }

        @NonNull
        public RouteControllerOptions getRouteControllerOptions() {
            return mRouteControllerOptions;
        }
    }

    private static class TestDynamicGroupRouteController
            extends MediaRouteProvider.DynamicGroupRouteController {

        private final String mInitialMemberRouteId;
        @NonNull private final MediaRouteProvider.RouteControllerOptions mRouteControllerOptions;

        private TestDynamicGroupRouteController(
                String initialMemberRouteId,
                @NonNull MediaRouteProvider.RouteControllerOptions routeControllerOptions) {
            mInitialMemberRouteId = initialMemberRouteId;
            mRouteControllerOptions = routeControllerOptions;
        }

        @NonNull
        public String getInitialMemberRouteId() {
            return mInitialMemberRouteId;
        }

        @NonNull
        public RouteControllerOptions getRouteControllerOptions() {
            return mRouteControllerOptions;
        }

        @Override
        public void onAddMemberRoute(@NonNull String routeId) {}

        @Override
        public void onRemoveMemberRoute(@NonNull String routeId) {}

        @Override
        public void onUpdateMemberRoutes(@Nullable List<String> routeIds) {}
    }
}
