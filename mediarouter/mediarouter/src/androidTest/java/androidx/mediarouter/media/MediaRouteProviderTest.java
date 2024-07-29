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

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private Context mContext;
    private Bundle mControlHints;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mControlHints = new Bundle();
        mControlHints.putBoolean("key", true);
    }

    @Test
    @SmallTest
    public void onCreateDynamicGroupRouteControllerWithHints_shouldProvideHints() {
        MediaRouteProvider mediaRouteProvider = new TestMediaRouteProvider(mContext);
        TestDynamicGroupRouteController groupRouteController =
                (TestDynamicGroupRouteController)
                        mediaRouteProvider.onCreateDynamicGroupRouteController(
                                ROUTE_ID, mControlHints);

        assertEquals(ROUTE_ID, groupRouteController.getInitialMemberRouteId());
        assertEquals(mControlHints, groupRouteController.getControlHints());
    }

    @Test
    @SmallTest
    public void onCreateDynamicGroupRouteController_shouldWorkWithoutHints() {
        MediaRouteProvider mediaRouteProvider = new TestMediaRouteProviderWithoutHints(mContext);
        TestDynamicGroupRouteController groupRouteController =
                (TestDynamicGroupRouteController)
                        mediaRouteProvider.onCreateDynamicGroupRouteController(
                                ROUTE_ID, mControlHints);

        assertEquals(ROUTE_ID, groupRouteController.getInitialMemberRouteId());
        assertNotEquals(mControlHints, groupRouteController.getControlHints());
    }

    private static class TestMediaRouteProvider extends MediaRouteProvider {

        private TestMediaRouteProvider(@NonNull Context context) {
            super(context);
        }

        @Override
        @Nullable
        public DynamicGroupRouteController onCreateDynamicGroupRouteController(
                @NonNull String initialMemberRouteId, @Nullable Bundle controlHints) {
            return new TestDynamicGroupRouteController(initialMemberRouteId, controlHints);
        }
    }

    private static class TestMediaRouteProviderWithoutHints extends MediaRouteProvider {

        TestMediaRouteProviderWithoutHints(Context context) {
            super(context);
        }

        @Override
        @Nullable
        public DynamicGroupRouteController onCreateDynamicGroupRouteController(
                @NonNull String initialMemberRouteId) {
            return new TestDynamicGroupRouteController(
                    initialMemberRouteId, /* controlHints= */ null);
        }
    }

    private static class TestDynamicGroupRouteController
            extends MediaRouteProvider.DynamicGroupRouteController {

        private final String mInitialMemberRouteId;
        @NonNull private final Bundle mControlHints;

        private TestDynamicGroupRouteController(
                String initialMemberRouteId, @Nullable Bundle controlHints) {
            mInitialMemberRouteId = initialMemberRouteId;
            mControlHints = (controlHints != null) ? controlHints : new Bundle();
        }

        @NonNull
        public String getInitialMemberRouteId() {
            return mInitialMemberRouteId;
        }

        @NonNull
        public Bundle getControlHints() {
            return mControlHints;
        }

        @Override
        public void onAddMemberRoute(@NonNull String routeId) {}

        @Override
        public void onRemoveMemberRoute(@NonNull String routeId) {}

        @Override
        public void onUpdateMemberRoutes(@Nullable List<String> routeIds) {}
    }
}
