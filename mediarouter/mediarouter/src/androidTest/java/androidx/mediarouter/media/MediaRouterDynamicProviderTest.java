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

import static androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController.DynamicRouteDescriptor.SELECTED;
import static androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController.DynamicRouteDescriptor.UNSELECTED;
import static androidx.mediarouter.media.MediaRouter.GroupRouteInfo.ADD_ROUTE_SUCCESSFUL;
import static androidx.mediarouter.media.MediaRouter.GroupRouteInfo.REMOVE_ROUTE_SUCCESSFUL;
import static androidx.mediarouter.media.MediaRouter.GroupRouteInfo.UPDATE_ROUTES_SUCCESSFUL;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_GROUPABLE_1;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_GROUPABLE_2;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_GROUPABLE_3;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_ID_1;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_ID_2;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_ID_3;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_ID_GROUP;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_NAME_1;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_NAME_2;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_NAME_3;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_TRANSFERABLE_1;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_TRANSFERABLE_2;
import static androidx.mediarouter.media.StubDynamicMediaRouteProviderService.ROUTE_TRANSFERABLE_3;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.mediarouter.testing.MediaRouterTestHelper;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test for {@link MediaRouter} functionality around routes from a provider that supports {@link
 * MediaRouteProviderDescriptor#supportsDynamicGroupRoute() dynamic group routes}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R) // Dynamic groups require API 30+.
@SmallTest
public final class MediaRouterDynamicProviderTest {

    private enum RouteConnectionState {
        STATE_UNKNOWN,
        STATE_CONNECTED,
        STATE_DISCONNECTED
    }

    private static final List<String> EXPECTED_ROUTE_IDS_AFTER_ROUTE_ADDED =
            List.of(ROUTE_ID_1, ROUTE_ID_2);
    private static final List<String> EXPECTED_ROUTE_IDS_AFTER_ROUTE_REMOVED = List.of(ROUTE_ID_1);
    private static final List<String> EXPECTED_ROUTE_IDS_AFTER_ROUTE_UPDATED = List.of(ROUTE_ID_3);
    private Context mContext;
    private MediaRouter mRouter;
    private MediaRouteSelector mSelector;
    private MediaRouterCallbackImpl mCallback;
    private MediaRouter.RouteInfo mRoute1;
    private MediaRouter.RouteInfo mRoute2;
    private MediaRouter.RouteInfo mRoute3;
    private RouteConnectionState mRouteConnectionState;
    private MediaRouter.RouteInfo mChangedRoute;
    private MediaRouter.RouteInfo mConnectedRoute;
    private MediaRouter.RouteInfo mDisconnectedRoute;
    private MediaRouter.RouteInfo mRequestedRoute;
    private MediaRouter.GroupRouteInfo mGroupRouteAfterAddingRoute;
    private MediaRouter.GroupRouteInfo mGroupRouteAfterRemovingRoute;
    private MediaRouter.GroupRouteInfo mGroupRouteAfterUpdatingRoutes;
    private int mRouteDisconnectedReason;

    @Before
    public void setUp() {
        mRouteConnectionState = RouteConnectionState.STATE_UNKNOWN;
        mSelector =
                new MediaRouteSelector.Builder()
                        .addControlCategory(
                                StubDynamicMediaRouteProviderService.CATEGORY_DYNAMIC_PROVIDER_TEST)
                        .build();
        mCallback = new MediaRouterCallbackImpl();
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mContext = getApplicationContext();
                            mRouter = MediaRouter.getInstance(mContext);
                            mRouter.addCallback(
                                    mSelector,
                                    mCallback,
                                    MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
                        });
        Map<String, MediaRouter.RouteInfo> routeSnapshot =
                mCallback.waitForRoutes(ROUTE_ID_1, ROUTE_ID_1);
        mRoute1 = routeSnapshot.get(ROUTE_ID_1);
        Objects.requireNonNull(mRoute1);
        MediaRouteDescriptor mediaRouteDescriptor1 = mRoute1.getMediaRouteDescriptor();
        Objects.requireNonNull(mediaRouteDescriptor1);
        assertEquals(ROUTE_ID_1, mediaRouteDescriptor1.getId());
        assertEquals(ROUTE_NAME_1, mediaRouteDescriptor1.getName());

        mRoute2 = routeSnapshot.get(ROUTE_ID_2);
        Objects.requireNonNull(mRoute2);
        MediaRouteDescriptor mediaRouteDescriptor2 = mRoute2.getMediaRouteDescriptor();
        Objects.requireNonNull(mediaRouteDescriptor2);
        assertEquals(ROUTE_ID_2, mediaRouteDescriptor2.getId());
        assertEquals(ROUTE_NAME_2, mediaRouteDescriptor2.getName());

        mRoute3 = routeSnapshot.get(ROUTE_ID_3);
        Objects.requireNonNull(mRoute3);
        MediaRouteDescriptor mediaRouteDescriptor3 = mRoute3.getMediaRouteDescriptor();
        Objects.requireNonNull(mediaRouteDescriptor3);
        assertEquals(ROUTE_ID_3, mediaRouteDescriptor3.getId());
        assertEquals(ROUTE_NAME_3, mediaRouteDescriptor3.getName());
    }

    @After
    public void tearDown() {
        getInstrumentation().runOnMainSync(MediaRouterTestHelper::resetMediaRouter);
    }

    // Tests.

    @Test()
    public void selectDynamicRoute_doesNotMarkMemberAsSelected() {
        MediaRouter.RouteInfo newSelectedRoute = mCallback.selectAndWaitForOnSelected(mRoute1);

        assertEquals(ROUTE_ID_GROUP, newSelectedRoute.getDescriptorId());
        assertFalse(runBlockingOnMainThreadWithResult(mRoute1::isSelected));
        assertTrue(runBlockingOnMainThreadWithResult(newSelectedRoute::isSelected));
    }

    @Test()
    public void connectDynamicRoute_shouldNotifyRouteConnected() {
        assertEquals(RouteConnectionState.STATE_UNKNOWN, mRouteConnectionState);
        List<MediaRouter.GroupRouteInfo> connectedGroupRoutes =
                mCallback.connectAndWaitForOnConnected(mRoute2);

        assertNotNull(mConnectedRoute);
        assertEquals(ROUTE_ID_GROUP, mConnectedRoute.getDescriptorId());
        assertEquals(1, connectedGroupRoutes.size());
        MediaRouter.GroupRouteInfo connectedGroupRoute = connectedGroupRoutes.get(0);
        assertEquals(ROUTE_ID_GROUP, connectedGroupRoute.getDescriptorId());
        assertTrue(runBlockingOnMainThreadWithResult(connectedGroupRoute::isConnected));

        assertNotNull(mRequestedRoute);
        assertEquals(ROUTE_ID_2, mRequestedRoute.getDescriptorId());
        assertEquals(RouteConnectionState.STATE_CONNECTED, mRouteConnectionState);
    }

    @Test()
    public void disconnectDynamicRoute_shouldNotifyRouteDisconnected() {
        assertEquals(RouteConnectionState.STATE_UNKNOWN, mRouteConnectionState);
        List<MediaRouter.GroupRouteInfo> connectedGroupRoutes =
                mCallback.connectAndWaitForOnConnected(mRoute2);
        assertEquals(RouteConnectionState.STATE_CONNECTED, mRouteConnectionState);
        assertEquals(1, connectedGroupRoutes.size());

        connectedGroupRoutes = mCallback.disconnectAndWaitForOnDisconnected(mRoute2);

        assertNotNull(mConnectedRoute);
        assertNotNull(mDisconnectedRoute);
        assertEquals(0, connectedGroupRoutes.size());

        assertNotNull(mRequestedRoute);
        assertEquals(ROUTE_ID_2, mRequestedRoute.getDescriptorId());
        assertEquals(RouteConnectionState.STATE_DISCONNECTED, mRouteConnectionState);
        assertEquals(MediaRouter.REASON_DISCONNECT_CALLED, mRouteDisconnectedReason);
    }

    @Test()
    public void connectDynamicRoute_failedToConnect_shouldNotifyRouteDisconnected() {
        assertEquals(RouteConnectionState.STATE_UNKNOWN, mRouteConnectionState);
        MediaRouter.RouteInfo selectedRoute = mCallback.selectAndWaitForOnSelected(mRoute1);
        assertEquals(ROUTE_ID_GROUP, selectedRoute.getDescriptorId());
        assertFalse(runBlockingOnMainThreadWithResult(mRoute1::isSelected));
        assertTrue(runBlockingOnMainThreadWithResult(selectedRoute::isSelected));

        List<MediaRouter.GroupRouteInfo> connectedGroupRoutes =
                mCallback.connectAndWaitForOnDisconnected(selectedRoute);

        assertNull(mConnectedRoute);
        assertNull(mDisconnectedRoute);
        assertEquals(0, connectedGroupRoutes.size());

        assertNotNull(mRequestedRoute);
        assertEquals(ROUTE_ID_GROUP, mRequestedRoute.getDescriptorId());
        assertEquals(RouteConnectionState.STATE_DISCONNECTED, mRouteConnectionState);
    }

    @Test()
    public void addRouteToGroupAndRemoveRouteFromGroup_shouldChangeGroup() {
        assertEquals(RouteConnectionState.STATE_UNKNOWN, mRouteConnectionState);
        List<MediaRouter.GroupRouteInfo> connectedGroupRoutes =
                mCallback.connectAndWaitForOnConnected(mRoute2);

        assertNotNull(mConnectedRoute);
        MediaRouter.GroupRouteInfo groupRouteInfo = mConnectedRoute.asGroup();
        assertNotNull(groupRouteInfo);
        assertEquals(ROUTE_ID_GROUP, mConnectedRoute.getDescriptorId());
        assertEquals(1, connectedGroupRoutes.size());
        MediaRouter.GroupRouteInfo connectedGroupRoute = connectedGroupRoutes.get(0);
        assertEquals(ROUTE_ID_GROUP, connectedGroupRoute.getDescriptorId());
        assertTrue(runBlockingOnMainThreadWithResult(connectedGroupRoute::isConnected));

        assertEquals(1, mConnectedRoute.getSelectedRoutesInGroup().size());
        assertEquals(
                ROUTE_ID_2, mConnectedRoute.getSelectedRoutesInGroup().get(0).getDescriptorId());
        assertEquals(3, groupRouteInfo.getRoutesInGroup().size());
        verifyMemberRouteState(groupRouteInfo, mRoute1, /* isSelected= */ false);
        verifyMemberRouteState(groupRouteInfo, mRoute2, /* isSelected= */ true);
        verifyMemberRouteState(groupRouteInfo, mRoute3, /* isSelected= */ false);

        List<MediaRouter.RouteInfo> memberRoutes =
                mCallback.addRouteToGroupAndWaitForOnChanged(groupRouteInfo, mRoute1);
        assertEquals(2, mConnectedRoute.getSelectedRoutesInGroup().size());
        assertEquals(2, memberRoutes.size());
        assertNotNull(mGroupRouteAfterAddingRoute);
        assertEquals(ROUTE_ID_GROUP, mGroupRouteAfterAddingRoute.getDescriptorId());
        assertEquals(3, groupRouteInfo.getRoutesInGroup().size());
        verifyMemberRouteState(groupRouteInfo, mRoute1, /* isSelected= */ true);
        verifyMemberRouteState(groupRouteInfo, mRoute2, /* isSelected= */ true);
        verifyMemberRouteState(groupRouteInfo, mRoute3, /* isSelected= */ false);

        memberRoutes = mCallback.removeRouteFromGroupAndWaitForOnChanged(groupRouteInfo, mRoute2);
        assertEquals(1, mConnectedRoute.getSelectedRoutesInGroup().size());
        assertEquals(1, memberRoutes.size());
        assertEquals(ROUTE_ID_1, memberRoutes.get(0).getDescriptorId());
        assertNotNull(mGroupRouteAfterRemovingRoute);
        assertEquals(ROUTE_ID_GROUP, mGroupRouteAfterRemovingRoute.getDescriptorId());
        assertEquals(3, groupRouteInfo.getRoutesInGroup().size());
        verifyMemberRouteState(groupRouteInfo, mRoute1, /* isSelected= */ true);
        verifyMemberRouteState(groupRouteInfo, mRoute2, /* isSelected= */ false);
        verifyMemberRouteState(groupRouteInfo, mRoute3, /* isSelected= */ false);
    }

    @Test()
    public void updateRoutesForGroup_shouldChangeGroup() {
        assertEquals(RouteConnectionState.STATE_UNKNOWN, mRouteConnectionState);
        List<MediaRouter.GroupRouteInfo> connectedGroupRoutes =
                mCallback.connectAndWaitForOnConnected(mRoute1);

        assertNotNull(mConnectedRoute);
        MediaRouter.GroupRouteInfo groupRouteInfo = mConnectedRoute.asGroup();
        assertNotNull(groupRouteInfo);
        assertEquals(ROUTE_ID_GROUP, mConnectedRoute.getDescriptorId());
        assertEquals(1, connectedGroupRoutes.size());
        MediaRouter.GroupRouteInfo connectedGroupRoute = connectedGroupRoutes.get(0);
        assertEquals(ROUTE_ID_GROUP, connectedGroupRoute.getDescriptorId());
        assertTrue(runBlockingOnMainThreadWithResult(connectedGroupRoute::isConnected));

        assertEquals(1, mConnectedRoute.getSelectedRoutesInGroup().size());
        assertEquals(
                ROUTE_ID_1, mConnectedRoute.getSelectedRoutesInGroup().get(0).getDescriptorId());
        assertEquals(3, groupRouteInfo.getRoutesInGroup().size());
        verifyMemberRouteState(groupRouteInfo, mRoute1, /* isSelected= */ true);
        verifyMemberRouteState(groupRouteInfo, mRoute2, /* isSelected= */ false);
        verifyMemberRouteState(groupRouteInfo, mRoute3, /* isSelected= */ false);

        List<MediaRouter.RouteInfo> memberRoutes =
                mCallback.updateRoutesForGroupAndWaitForOnChanged(groupRouteInfo, List.of(mRoute3));
        assertEquals(1, mConnectedRoute.getSelectedRoutesInGroup().size());
        assertEquals(1, memberRoutes.size());
        assertNotNull(mGroupRouteAfterUpdatingRoutes);
        assertEquals(ROUTE_ID_GROUP, mGroupRouteAfterUpdatingRoutes.getDescriptorId());
        assertEquals(3, groupRouteInfo.getRoutesInGroup().size());
        verifyMemberRouteState(groupRouteInfo, mRoute1, /* isSelected= */ false);
        verifyMemberRouteState(groupRouteInfo, mRoute2, /* isSelected= */ false);
        verifyMemberRouteState(groupRouteInfo, mRoute3, /* isSelected= */ true);
    }

    @Test()
    public void setGroupVolume_shouldSetGroupVolume() {
        assertEquals(RouteConnectionState.STATE_UNKNOWN, mRouteConnectionState);
        List<MediaRouter.GroupRouteInfo> connectedGroupRoutes =
                mCallback.connectAndWaitForOnConnected(mRoute1);

        assertNotNull(mConnectedRoute);
        MediaRouter.GroupRouteInfo groupRouteInfo = mConnectedRoute.asGroup();
        assertNotNull(groupRouteInfo);
        assertEquals(
                StubDynamicMediaRouteProviderService.VOLUME_INITIAL_VALUE,
                groupRouteInfo.getVolume());
        assertEquals(1, connectedGroupRoutes.size());
        MediaRouter.GroupRouteInfo connectedGroupRoute = connectedGroupRoutes.get(0);
        assertTrue(runBlockingOnMainThreadWithResult(connectedGroupRoute::isConnected));
        assertEquals(
                StubDynamicMediaRouteProviderService.VOLUME_INITIAL_VALUE,
                connectedGroupRoute.getVolume());

        final int expectedVolume = 6;
        int updatedVolume =
                mCallback.setGroupVolumeAndWaitForOnVolumeSet(groupRouteInfo, expectedVolume);
        assertEquals(expectedVolume, updatedVolume);
        assertEquals(expectedVolume, mConnectedRoute.getVolume());
    }

    @Test()
    public void updateGroupVolume_shouldUpdateGroupVolume() {
        assertEquals(RouteConnectionState.STATE_UNKNOWN, mRouteConnectionState);
        List<MediaRouter.GroupRouteInfo> connectedGroupRoutes =
                mCallback.connectAndWaitForOnConnected(mRoute2);

        assertNotNull(mConnectedRoute);
        MediaRouter.GroupRouteInfo groupRouteInfo = mConnectedRoute.asGroup();
        assertNotNull(groupRouteInfo);
        assertEquals(
                StubDynamicMediaRouteProviderService.VOLUME_INITIAL_VALUE,
                groupRouteInfo.getVolume());
        assertEquals(1, connectedGroupRoutes.size());
        MediaRouter.GroupRouteInfo connectedGroupRoute = connectedGroupRoutes.get(0);
        assertTrue(runBlockingOnMainThreadWithResult(connectedGroupRoute::isConnected));
        assertEquals(
                StubDynamicMediaRouteProviderService.VOLUME_INITIAL_VALUE,
                connectedGroupRoute.getVolume());

        final int volumeDelta = -3;
        final int expectedVolume =
                StubDynamicMediaRouteProviderService.VOLUME_INITIAL_VALUE + volumeDelta;
        int updatedVolume =
                mCallback.updateGroupVolumeAndWaitForOnVolumeUpdated(groupRouteInfo, volumeDelta);
        assertEquals(expectedVolume, updatedVolume);
        assertEquals(expectedVolume, mConnectedRoute.getVolume());
    }

    @Test()
    public void setMemberVolume_shouldSetMemberVolume() {
        assertEquals(RouteConnectionState.STATE_UNKNOWN, mRouteConnectionState);
        List<MediaRouter.GroupRouteInfo> connectedGroupRoutes =
                mCallback.connectAndWaitForOnConnected(mRoute2);

        assertNotNull(mConnectedRoute);
        MediaRouter.GroupRouteInfo groupRouteInfo = mConnectedRoute.asGroup();
        assertNotNull(groupRouteInfo);
        assertEquals(
                StubDynamicMediaRouteProviderService.VOLUME_INITIAL_VALUE,
                groupRouteInfo.getVolume());
        assertEquals(1, connectedGroupRoutes.size());
        MediaRouter.GroupRouteInfo connectedGroupRoute = connectedGroupRoutes.get(0);
        assertTrue(runBlockingOnMainThreadWithResult(connectedGroupRoute::isConnected));
        assertEquals(
                StubDynamicMediaRouteProviderService.VOLUME_INITIAL_VALUE,
                connectedGroupRoute.getVolume());

        final int expectedVolume = 14;
        getInstrumentation().runOnMainSync(() -> mRoute2.requestSetVolume(expectedVolume));
        MediaRouter.RouteInfo memberRoute =
                mCallback.waitForRouteVolume(ROUTE_ID_2, expectedVolume);
        assertEquals(expectedVolume, memberRoute.getVolume());
    }

    @Test()
    public void updateMemberVolume_shouldUpdateMemberVolume() {
        assertEquals(RouteConnectionState.STATE_UNKNOWN, mRouteConnectionState);
        List<MediaRouter.GroupRouteInfo> connectedGroupRoutes =
                mCallback.connectAndWaitForOnConnected(mRoute1);

        assertNotNull(mConnectedRoute);
        MediaRouter.GroupRouteInfo groupRouteInfo = mConnectedRoute.asGroup();
        assertNotNull(groupRouteInfo);
        assertEquals(
                StubDynamicMediaRouteProviderService.VOLUME_INITIAL_VALUE,
                groupRouteInfo.getVolume());
        assertEquals(1, connectedGroupRoutes.size());
        MediaRouter.GroupRouteInfo connectedGroupRoute = connectedGroupRoutes.get(0);
        assertTrue(runBlockingOnMainThreadWithResult(connectedGroupRoute::isConnected));
        assertEquals(
                StubDynamicMediaRouteProviderService.VOLUME_INITIAL_VALUE,
                connectedGroupRoute.getVolume());

        final int volumeDelta = 4;
        final int expectedVolume =
                StubDynamicMediaRouteProviderService.VOLUME_INITIAL_VALUE + volumeDelta;
        getInstrumentation().runOnMainSync(() -> mRoute1.requestUpdateVolume(volumeDelta));
        MediaRouter.RouteInfo memberRoute =
                mCallback.waitForRouteVolume(ROUTE_ID_1, expectedVolume);
        assertEquals(expectedVolume, memberRoute.getVolume());
    }

    @Test()
    public void sendControlRequest_shouldSendControlRequestToGroupRouteController() {
        final ConditionVariable sendControlRequestConditionVariable =
                new ConditionVariable(/* state= */ true);
        assertEquals(RouteConnectionState.STATE_UNKNOWN, mRouteConnectionState);
        List<MediaRouter.GroupRouteInfo> connectedGroupRoutes =
                mCallback.connectAndWaitForOnConnected(mRoute2);

        assertNotNull(mConnectedRoute);
        MediaRouter.GroupRouteInfo groupRouteInfo = mConnectedRoute.asGroup();
        assertNotNull(groupRouteInfo);
        assertEquals(1, connectedGroupRoutes.size());
        MediaRouter.GroupRouteInfo connectedGroupRoute = connectedGroupRoutes.get(0);
        assertTrue(runBlockingOnMainThreadWithResult(connectedGroupRoute::isConnected));

        final Bundle[] sendControlRequestResults = new Bundle[1];
        MediaRouter.ControlRequestCallback callback =
                new MediaRouter.ControlRequestCallback() {
                    @Override
                    public void onResult(@Nullable Bundle data) {
                        sendControlRequestResults[0] = data;
                        sendControlRequestConditionVariable.open();
                    }
                };
        sendControlRequestConditionVariable.close();
        getInstrumentation()
                .runOnMainSync(() -> groupRouteInfo.sendControlRequest(new Intent(), callback));
        sendControlRequestConditionVariable.block();

        Bundle sendControlRequestResult = sendControlRequestResults[0];
        assertEquals(
                StubDynamicMediaRouteProviderService.SEND_CONTROL_REQUEST_RESULT,
                sendControlRequestResult);
        assertEquals(
                StubDynamicMediaRouteProviderService.SEND_CONTROL_REQUEST_VALUE,
                sendControlRequestResult.getString(
                        StubDynamicMediaRouteProviderService.SEND_CONTROL_REQUEST_KEY));
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

    private void verifyMemberRouteState(
            MediaRouter.GroupRouteInfo groupRoute,
            MediaRouter.RouteInfo route,
            boolean isSelected) {
        assertEquals(isSelected ? SELECTED : UNSELECTED, groupRoute.getSelectionState(route));
        assertEquals(isSelected, groupRoute.isUnselectable(route));
        switch (route.getDescriptorId()) {
            case ROUTE_ID_1:
                assertEquals(ROUTE_GROUPABLE_1, groupRoute.isGroupable(route));
                assertEquals(ROUTE_TRANSFERABLE_1, groupRoute.isTransferable(route));
                break;
            case ROUTE_ID_2:
                assertEquals(ROUTE_GROUPABLE_2, groupRoute.isGroupable(route));
                assertEquals(ROUTE_TRANSFERABLE_2, groupRoute.isTransferable(route));
                break;
            case ROUTE_ID_3:
                assertEquals(ROUTE_GROUPABLE_3, groupRoute.isGroupable(route));
                assertEquals(ROUTE_TRANSFERABLE_3, groupRoute.isTransferable(route));
                break;
            default:
                // Ignore.
        }
    }

    // Internal classes and interfaces.

    // Equivalent to java.util.function.Supplier, except it's available before API 24.
    private interface Supplier<Result> {

        Result get();
    }

    private class MediaRouterCallbackImpl extends MediaRouter.Callback {

        private final Set<String> mRouteIdsPending = new HashSet<>();
        private final Map<String, Integer> mRouteVolumePending = new HashMap<>();
        private final ConditionVariable mPendingRoutesConditionVariable = new ConditionVariable();
        private final ConditionVariable mPendingRouteVolumeConditionVariable =
                new ConditionVariable();
        private final ConditionVariable mRouteSelectedConditionVariable =
                new ConditionVariable(/* state= */ true);
        private final ConditionVariable mRouteConnectedConditionVariable =
                new ConditionVariable(/* state= */ true);
        private final ConditionVariable mRouteDisconnectedConditionVariable =
                new ConditionVariable(/* state= */ true);
        private final ConditionVariable mMemberRouteAddedConditionVariable =
                new ConditionVariable(/* state= */ true);
        private final ConditionVariable mMemberRouteRemovedConditionVariable =
                new ConditionVariable(/* state= */ true);
        private final ConditionVariable mMemberRoutesUpdatedConditionVariable =
                new ConditionVariable(/* state= */ true);
        private final ConditionVariable mGroupVolumeChangedConditionVariable =
                new ConditionVariable(/* state= */ true);

        private Map<String, MediaRouter.RouteInfo> waitForRoutes(String... routeIds) {
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
            mRouteSelectedConditionVariable.close();
            getInstrumentation().runOnMainSync(routeToSelect::select);
            mRouteSelectedConditionVariable.block();
            return runBlockingOnMainThreadWithResult(() -> mRouter.getSelectedRoute());
        }

        public List<MediaRouter.GroupRouteInfo> connectAndWaitForOnConnected(
                MediaRouter.RouteInfo routeToConnect) {
            mRouteConnectedConditionVariable.close();
            getInstrumentation().runOnMainSync(routeToConnect::connect);
            mRouteConnectedConditionVariable.block();
            return runBlockingOnMainThreadWithResult(() -> mRouter.getConnectedGroupRoutes());
        }

        public List<MediaRouter.GroupRouteInfo> connectAndWaitForOnDisconnected(
                MediaRouter.RouteInfo routeToConnect) {
            mRouteDisconnectedConditionVariable.close();
            getInstrumentation().runOnMainSync(routeToConnect::connect);
            mRouteDisconnectedConditionVariable.block();
            return runBlockingOnMainThreadWithResult(() -> mRouter.getConnectedGroupRoutes());
        }

        public List<MediaRouter.GroupRouteInfo> disconnectAndWaitForOnDisconnected(
                MediaRouter.RouteInfo routeToDisconnect) {
            mRouteDisconnectedConditionVariable.close();
            getInstrumentation().runOnMainSync(routeToDisconnect::disconnect);
            mRouteDisconnectedConditionVariable.block();
            return runBlockingOnMainThreadWithResult(() -> mRouter.getConnectedGroupRoutes());
        }

        public List<MediaRouter.RouteInfo> addRouteToGroupAndWaitForOnChanged(
                MediaRouter.GroupRouteInfo groupRoute, MediaRouter.RouteInfo memberRoute) {
            mMemberRouteAddedConditionVariable.close();
            AtomicInteger addMemberStatus = new AtomicInteger();
            getInstrumentation()
                    .runOnMainSync(() -> addMemberStatus.set(groupRoute.addRoute(memberRoute)));
            assertEquals(ADD_ROUTE_SUCCESSFUL, addMemberStatus.get());
            mMemberRouteAddedConditionVariable.block();
            return runBlockingOnMainThreadWithResult(groupRoute::getSelectedRoutesInGroup);
        }

        public List<MediaRouter.RouteInfo> removeRouteFromGroupAndWaitForOnChanged(
                MediaRouter.GroupRouteInfo groupRoute, MediaRouter.RouteInfo memberRoute) {
            mMemberRouteRemovedConditionVariable.close();
            AtomicInteger removeMemberStatus = new AtomicInteger();
            getInstrumentation()
                    .runOnMainSync(
                            () -> removeMemberStatus.set(groupRoute.removeRoute(memberRoute)));
            assertEquals(REMOVE_ROUTE_SUCCESSFUL, removeMemberStatus.get());
            mMemberRouteRemovedConditionVariable.block();
            return runBlockingOnMainThreadWithResult(groupRoute::getSelectedRoutesInGroup);
        }

        public List<MediaRouter.RouteInfo> updateRoutesForGroupAndWaitForOnChanged(
                MediaRouter.GroupRouteInfo groupRoute, List<MediaRouter.RouteInfo> memberRoutes) {
            mMemberRoutesUpdatedConditionVariable.close();
            AtomicInteger updateMembersStatus = new AtomicInteger();
            getInstrumentation()
                    .runOnMainSync(
                            () -> updateMembersStatus.set(groupRoute.updateRoutes(memberRoutes)));
            assertEquals(UPDATE_ROUTES_SUCCESSFUL, updateMembersStatus.get());
            mMemberRoutesUpdatedConditionVariable.block();
            return runBlockingOnMainThreadWithResult(groupRoute::getSelectedRoutesInGroup);
        }

        public int setGroupVolumeAndWaitForOnVolumeSet(
                MediaRouter.GroupRouteInfo groupRoute, int volume) {
            mGroupVolumeChangedConditionVariable.close();
            getInstrumentation().runOnMainSync(() -> groupRoute.requestSetVolume(volume));
            mGroupVolumeChangedConditionVariable.block();
            return runBlockingOnMainThreadWithResult(groupRoute::getVolume);
        }

        public int updateGroupVolumeAndWaitForOnVolumeUpdated(
                MediaRouter.GroupRouteInfo groupRoute, int delta) {
            mGroupVolumeChangedConditionVariable.close();
            getInstrumentation().runOnMainSync(() -> groupRoute.requestUpdateVolume(delta));
            mGroupVolumeChangedConditionVariable.block();
            return runBlockingOnMainThreadWithResult(groupRoute::getVolume);
        }

        private MediaRouter.RouteInfo waitForRouteVolume(String routeId, int expectedVolume) {
            getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                Map<String, MediaRouter.RouteInfo> routes = getCurrentRoutesAsMap();
                                if (!routes.containsKey(routeId)
                                        || routes.get(routeId).getVolume() != expectedVolume) {
                                    mPendingRouteVolumeConditionVariable.close();
                                    mRouteVolumePending.clear();
                                    mRouteVolumePending.put(routeId, expectedVolume);
                                } else {
                                    mPendingRouteVolumeConditionVariable.open();
                                }
                            });
            mPendingRouteVolumeConditionVariable.block();
            return getCurrentRoutesAsMap().get(routeId);
        }

        @Override
        public void onRouteSelected(
                @NonNull MediaRouter router,
                @NonNull MediaRouter.RouteInfo selectedRoute,
                int reason,
                @NonNull MediaRouter.RouteInfo requestedRoute) {
            mRouteSelectedConditionVariable.open();
        }

        @Override
        public void onRouteAdded(
                @NonNull MediaRouter router, @NonNull MediaRouter.RouteInfo route) {
            if (getCurrentRoutesAsMap().keySet().containsAll(mRouteIdsPending)) {
                mPendingRoutesConditionVariable.open();
            }
            checkPendingRouteVolume(route);
        }

        @Override
        public void onRouteChanged(
                @NonNull MediaRouter router, @NonNull MediaRouter.RouteInfo route) {
            mChangedRoute = route;
            MediaRouter.GroupRouteInfo groupRoute = route.asGroup();
            if (groupRoute != null) {
                List<String> selectedRouteIds = new ArrayList<>();
                for (MediaRouter.RouteInfo selectedRoute : route.getSelectedRoutesInGroup()) {
                    selectedRouteIds.add(selectedRoute.getDescriptorId());
                }
                if (selectedRouteIds.containsAll(EXPECTED_ROUTE_IDS_AFTER_ROUTE_ADDED)) {
                    mGroupRouteAfterAddingRoute = groupRoute;
                    mMemberRouteAddedConditionVariable.open();
                } else if (selectedRouteIds.containsAll(EXPECTED_ROUTE_IDS_AFTER_ROUTE_REMOVED)) {
                    mGroupRouteAfterRemovingRoute = groupRoute;
                    mMemberRouteRemovedConditionVariable.open();
                } else if (selectedRouteIds.containsAll(EXPECTED_ROUTE_IDS_AFTER_ROUTE_UPDATED)) {
                    mGroupRouteAfterUpdatingRoutes = groupRoute;
                    mMemberRoutesUpdatedConditionVariable.open();
                }
                boolean isVolumeChanged = false;
                if (route.getVolume()
                        != StubDynamicMediaRouteProviderService.VOLUME_INITIAL_VALUE) {
                    isVolumeChanged = true;
                } else {
                    for (MediaRouter.RouteInfo memberRoute : route.getSelectedRoutesInGroup()) {
                        if (memberRoute.getVolume()
                                != StubDynamicMediaRouteProviderService.VOLUME_INITIAL_VALUE) {
                            isVolumeChanged = true;
                        }
                    }
                }
                if (isVolumeChanged) {
                    mGroupVolumeChangedConditionVariable.open();
                }
            }
            checkPendingRouteVolume(route);
        }

        @Override
        public void onRouteConnected(
                @NonNull MediaRouter router,
                @NonNull MediaRouter.RouteInfo connectedRoute,
                @NonNull MediaRouter.RouteInfo requestedRoute) {
            mRouteConnectionState = RouteConnectionState.STATE_CONNECTED;
            mConnectedRoute = connectedRoute;
            mRequestedRoute = requestedRoute;
            mRouteConnectedConditionVariable.open();
        }

        @Override
        public void onRouteDisconnected(
                @NonNull MediaRouter router,
                @Nullable MediaRouter.RouteInfo disconnectedRoute,
                @NonNull MediaRouter.RouteInfo requestedRoute,
                int reason) {
            mRouteConnectionState = RouteConnectionState.STATE_DISCONNECTED;
            mDisconnectedRoute = disconnectedRoute;
            mRequestedRoute = requestedRoute;
            mRouteDisconnectedReason = reason;
            mRouteDisconnectedConditionVariable.open();
        }

        private void checkPendingRouteVolume(MediaRouter.RouteInfo route) {
            String routeDescriptorId = route.getDescriptorId();
            if (routeDescriptorId != null) {
                Integer expectedVolume = mRouteVolumePending.get(routeDescriptorId);
                if (expectedVolume != null && route.getVolume() == expectedVolume) {
                    mPendingRouteVolumeConditionVariable.open();
                }
            }
        }
    }
}
