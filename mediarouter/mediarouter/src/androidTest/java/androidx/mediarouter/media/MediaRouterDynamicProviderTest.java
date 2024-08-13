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

import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_ID_GROUP;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.mediarouter.testing.MediaRouterTestHelper;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Test for {@link MediaRouter} functionality around routes from a provider that supports {@link
 * MediaRouteProviderDescriptor#supportsDynamicGroupRoute() dynamic group routes}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R) // Dynamic groups require API 30+.
@SmallTest
public final class MediaRouterDynamicProviderTest {

    private Context mContext;
    private MediaRouter mRouter;

    @Before
    public void setUp() {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mContext = getApplicationContext();
                            mRouter = MediaRouter.getInstance(mContext);
                        });
    }

    @After
    public void tearDown() {
        getInstrumentation().runOnMainSync(MediaRouterTestHelper::resetMediaRouter);
    }

    // Tests.

    @Test()
    public void selectDynamicRoute_doesNotMarkMemberAsSelected() {
        MediaRouteSelector selector =
                new MediaRouteSelector.Builder()
                        .addControlCategory(
                                StubDynamicMediaRouteProviderService.CATEGORY_DYNAMIC_PROVIDER_TEST)
                        .build();
        MediaRouterCallbackImpl callback = new MediaRouterCallbackImpl();
        getInstrumentation()
                .runOnMainSync(
                        () ->
                                mRouter.addCallback(
                                        selector,
                                        callback,
                                        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN));
        Map<String, MediaRouter.RouteInfo> routeSnapshot =
                callback.waitForRoutes(
                        StubDynamicMediaRouteProviderService.ROUTE_ID_1,
                        StubDynamicMediaRouteProviderService.ROUTE_ID_1);
        MediaRouter.RouteInfo routeToSelect =
                routeSnapshot.get(StubDynamicMediaRouteProviderService.ROUTE_ID_1);
        Objects.requireNonNull(routeToSelect);
        MediaRouter.RouteInfo newSelectedRoute = callback.selectAndWaitForOnSelected(routeToSelect);
        assertEquals(ROUTE_ID_GROUP, newSelectedRoute.getDescriptorId());
        assertFalse(runBlockingOnMainThreadWithResult(routeToSelect::isSelected));
        assertTrue(runBlockingOnMainThreadWithResult(newSelectedRoute::isSelected));
    }

    // Internal methods.

    private Map<String, MediaRouter.RouteInfo> getCurrentRoutesAsMap() {
        Supplier<Map<String, MediaRouter.RouteInfo>> supplier =
                () -> {
                    Map<String, MediaRouter.RouteInfo> routeIds = new HashMap<>();
                    for (MediaRouter.RouteInfo route : mRouter.getRoutes()) {
                        routeIds.put(route.getDescriptorId(), route);
                    }
                    return routeIds;
                };
        return runBlockingOnMainThreadWithResult(supplier);
    }

    @SuppressWarnings("unchecked") // Allows us to pull a generic result out of runOnMainSync.
    private <Result> Result runBlockingOnMainThreadWithResult(Supplier<Result> supplier) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return supplier.get();
        } else {
            Result[] resultHolder = (Result[]) new Object[1];
            getInstrumentation().runOnMainSync(() -> resultHolder[0] = supplier.get());
            return resultHolder[0];
        }
    }

    // Internal classes and interfaces.

    // Equivalent to java.util.function.Supplier, except it's available before API 24.
    private interface Supplier<Result> {

        Result get();
    }

    private class MediaRouterCallbackImpl extends MediaRouter.Callback {

        private final ConditionVariable mPendingRoutesConditionVariable = new ConditionVariable();
        private final Set<String> mRouteIdsPending = new HashSet<>();
        private final ConditionVariable mSelectedRouteChangeConditionVariable =
                new ConditionVariable(/* state= */ true);

        public Map<String, MediaRouter.RouteInfo> waitForRoutes(String... routeIds) {
            Set<String> routesIdsSet = new HashSet<>(Arrays.asList(routeIds));
            getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                Map<String, MediaRouter.RouteInfo> routes = getCurrentRoutesAsMap();
                                if (!routes.keySet().containsAll(routesIdsSet)) {
                                    mPendingRoutesConditionVariable.close();
                                    mRouteIdsPending.clear();
                                    mRouteIdsPending.addAll(routesIdsSet);
                                } else {
                                    mPendingRoutesConditionVariable.open();
                                }
                            });
            mPendingRoutesConditionVariable.block();
            return getCurrentRoutesAsMap();
        }

        public MediaRouter.RouteInfo selectAndWaitForOnSelected(
                MediaRouter.RouteInfo routeToSelect) {
            mSelectedRouteChangeConditionVariable.close();
            getInstrumentation().runOnMainSync(routeToSelect::select);
            mSelectedRouteChangeConditionVariable.block();
            return runBlockingOnMainThreadWithResult(() -> mRouter.getSelectedRoute());
        }

        @Override
        public void onRouteSelected(
                @NonNull MediaRouter router,
                @NonNull MediaRouter.RouteInfo selectedRoute,
                int reason,
                @NonNull MediaRouter.RouteInfo requestedRoute) {
            mSelectedRouteChangeConditionVariable.open();
        }

        @Override
        public void onRouteAdded(
                @NonNull MediaRouter router, @NonNull MediaRouter.RouteInfo route) {
            if (getCurrentRoutesAsMap().keySet().containsAll(mRouteIdsPending)) {
                mPendingRoutesConditionVariable.open();
            }
        }
    }
}
