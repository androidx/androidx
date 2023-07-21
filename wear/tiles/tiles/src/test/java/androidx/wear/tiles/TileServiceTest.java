/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.tiles;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.ResourceBuilders.Resources;
import androidx.wear.protolayout.expression.VersionBuilders;
import androidx.wear.protolayout.expression.proto.VersionProto.VersionInfo;
import androidx.wear.protolayout.proto.DeviceParametersProto.DeviceParameters;
import androidx.wear.protolayout.proto.ResourceProto;
import androidx.wear.protolayout.protobuf.ExtensionRegistryLite;
import androidx.wear.tiles.EventBuilders.TileAddEvent;
import androidx.wear.tiles.EventBuilders.TileEnterEvent;
import androidx.wear.tiles.EventBuilders.TileLeaveEvent;
import androidx.wear.tiles.EventBuilders.TileRemoveEvent;
import androidx.wear.tiles.RequestBuilders.ResourcesRequest;
import androidx.wear.tiles.RequestBuilders.TileRequest;
import androidx.wear.tiles.TileBuilders.Version;
import androidx.wear.tiles.proto.EventProto;
import androidx.wear.tiles.proto.RequestProto;
import androidx.wear.tiles.proto.TileProto.Tile;

import com.google.common.truth.Expect;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.CancellationException;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class TileServiceTest {
    private static final int TILE_ID = 42;

    @Rule public final MockitoRule mocks = MockitoJUnit.rule();
    @Rule public final Expect expect = Expect.create();

    // This is a little nasty, but we need to ensure that the version is injected by TileService.
    // For that, we need the builder form (for FakeTileService to return), and the protobuf form (to
    // compare against, which also includes the version).
    private static final TileBuilders.Tile DUMMY_TILE_TO_RETURN =
            new TileBuilders.Tile.Builder().setResourcesVersion("5").build();
    private static final Tile DUMMY_TILE_PROTOBUF =
            Tile.newBuilder().setResourcesVersion("5").setSchemaVersion(Version.CURRENT).build();

    private TileProvider mTileProviderServiceStub;
    private ServiceController<FakeTileService> mFakeTileServiceController;

    @Mock private TileCallback mMockTileCallback;
    @Mock private ResourcesCallback mMockResourcesCallback;

    @Before
    public void setUp() {
        mMockTileCallback = mock(TileCallback.class);
        mMockResourcesCallback = mock(ResourcesCallback.class);
        mFakeTileServiceController = Robolectric.buildService(FakeTileService.class);

        Intent i = new Intent(TileService.ACTION_BIND_TILE_PROVIDER);
        IBinder binder = mFakeTileServiceController.get().onBind(i);
        mTileProviderServiceStub = TileProvider.Stub.asInterface(binder);
    }

    @Test
    public void tileService_tileRequest() throws Exception {
        mTileProviderServiceStub.onTileRequest(
                5,
                new TileRequestData(
                        RequestProto.TileRequest.getDefaultInstance().toByteArray(),
                        TileRequestData.VERSION_PROTOBUF),
                mMockTileCallback);

        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<TileData> tileCaptor = ArgumentCaptor.forClass(TileData.class);

        verify(mMockTileCallback).updateTileData(tileCaptor.capture());

        Tile tile =
                Tile.parseFrom(
                        tileCaptor.getValue().getContents(),
                        ExtensionRegistryLite.getEmptyRegistry());

        expect.that(tile).isEqualTo(DUMMY_TILE_PROTOBUF);
    }

    @Test
    public void tileService_resourcesRequest() throws Exception {
        final String resourcesVersion = "HELLO WORLD";

        ResourcesRequestData resourcesRequestData =
                new ResourcesRequestData(
                        RequestProto.ResourcesRequest.newBuilder()
                                .setVersion(resourcesVersion)
                                .build()
                                .toByteArray(),
                        ResourcesRequestData.VERSION_PROTOBUF);

        mTileProviderServiceStub.onResourcesRequest(
                5, resourcesRequestData, mMockResourcesCallback);

        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<ResourcesData> resourcesCaptor =
                ArgumentCaptor.forClass(ResourcesData.class);
        verify(mMockResourcesCallback).updateResources(resourcesCaptor.capture());

        ResourceProto.Resources resources =
                ResourceProto.Resources.parseFrom(
                        resourcesCaptor.getValue().getContents(),
                        ExtensionRegistryLite.getEmptyRegistry());

        expect.that(resources.getVersion()).isEqualTo(resourcesVersion);
    }

    @Test
    public void tileService_resourcesRequest_compatibleService() throws Exception {
        ServiceController<CompatibleFakeTileService> compatibleServiceController =
                Robolectric.buildService(CompatibleFakeTileService.class);

        Intent i = new Intent(TileService.ACTION_BIND_TILE_PROVIDER);
        IBinder binder = compatibleServiceController.get().onBind(i);
        TileProvider compatibleServiceStub = TileProvider.Stub.asInterface(binder);

        final String resourcesVersion = "HELLO WORLD";

        ResourcesRequestData resourcesRequestData =
                new ResourcesRequestData(
                        RequestProto.ResourcesRequest.newBuilder()
                                .setVersion(resourcesVersion)
                                .build()
                                .toByteArray(),
                        ResourcesRequestData.VERSION_PROTOBUF);

        compatibleServiceStub.onResourcesRequest(5, resourcesRequestData, mMockResourcesCallback);

        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<ResourcesData> resourcesCaptor =
                ArgumentCaptor.forClass(ResourcesData.class);
        verify(mMockResourcesCallback).updateResources(resourcesCaptor.capture());

        ResourceProto.Resources resources =
                ResourceProto.Resources.parseFrom(
                        resourcesCaptor.getValue().getContents(),
                        ExtensionRegistryLite.getEmptyRegistry());

        expect.that(resources.getVersion()).isEqualTo(resourcesVersion);
    }

    @Test
    public void tileService_onTileAdd() throws Exception {
        EventProto.TileAddEvent addRequest =
                EventProto.TileAddEvent.newBuilder().setTileId(TILE_ID).build();
        mTileProviderServiceStub.onTileAddEvent(
                new TileAddEventData(addRequest.toByteArray(), TileAddEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();

        expect.that(mFakeTileServiceController.get().mOnTileAddCalled).isTrue();
        expect.that(mFakeTileServiceController.get().mTileId).isEqualTo(TILE_ID);
    }

    @Test
    public void tileService_onTileRemove() throws Exception {
        EventProto.TileRemoveEvent removeRequest =
                EventProto.TileRemoveEvent.newBuilder().setTileId(TILE_ID).build();
        mTileProviderServiceStub.onTileRemoveEvent(
                new TileRemoveEventData(
                        removeRequest.toByteArray(), TileRemoveEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();

        expect.that(mFakeTileServiceController.get().mOnTileRemoveCalled).isTrue();
        expect.that(mFakeTileServiceController.get().mTileId).isEqualTo(TILE_ID);
    }

    @Test
    public void tileService_onTileEnter() throws Exception {
        EventProto.TileEnterEvent enterRequest =
                EventProto.TileEnterEvent.newBuilder().setTileId(TILE_ID).build();
        mTileProviderServiceStub.onTileEnterEvent(
                new TileEnterEventData(
                        enterRequest.toByteArray(), TileEnterEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();

        expect.that(mFakeTileServiceController.get().mOnTileEnterCalled).isTrue();
        expect.that(mFakeTileServiceController.get().mTileId).isEqualTo(TILE_ID);
    }

    @Test
    public void tileService_onTileLeave() throws Exception {
        EventProto.TileLeaveEvent leaveRequest =
                EventProto.TileLeaveEvent.newBuilder().setTileId(TILE_ID).build();
        mTileProviderServiceStub.onTileLeaveEvent(
                new TileLeaveEventData(
                        leaveRequest.toByteArray(), TileLeaveEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();

        expect.that(mFakeTileServiceController.get().mOnTileLeaveCalled).isTrue();
        expect.that(mFakeTileServiceController.get().mTileId).isEqualTo(TILE_ID);
    }

    @Test
    public void tileService_tileRequest_setsTileId() throws Exception {
        mTileProviderServiceStub.onTileRequest(
                TILE_ID,
                new TileRequestData(
                        RequestProto.TileRequest.newBuilder()
                                .build()
                                .toByteArray(),
                        TileRequestData.VERSION_PROTOBUF),
                mMockTileCallback);

        shadowOf(Looper.getMainLooper()).idle();

        expect.that(mFakeTileServiceController.get().mTileId).isEqualTo(TILE_ID);
    }

    @Test
    public void tileService_tileRequest_defaultVersionIfNotSet() throws Exception {
        // Tile request needs to have DeviceParameters least to fill in the default.
        mTileProviderServiceStub.onTileRequest(
                5,
                new TileRequestData(
                        RequestProto.TileRequest.newBuilder()
                                .setDeviceConfiguration(DeviceParameters.getDefaultInstance())
                                .build()
                                .toByteArray(),
                        TileRequestData.VERSION_PROTOBUF),
                mMockTileCallback);

        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mFakeTileServiceController.get().mTileRequestParams).isNotNull();

        VersionBuilders.VersionInfo schemaVersion =
                mFakeTileServiceController
                        .get()
                        .mTileRequestParams
                        .getDeviceConfiguration()
                        .getRendererSchemaVersion();
        expect.that(schemaVersion.getMajor()).isEqualTo(1);
        expect.that(schemaVersion.getMinor()).isEqualTo(0);
    }

    @Test
    public void tileService_tileRequest_passesVersionIfSet() throws Exception {
        mTileProviderServiceStub.onTileRequest(
                5,
                new TileRequestData(
                        RequestProto.TileRequest.newBuilder()
                                .setDeviceConfiguration(
                                        DeviceParameters.newBuilder()
                                                .setRendererSchemaVersion(
                                                        VersionInfo.newBuilder()
                                                                .setMajor(3)
                                                                .setMinor(5)))
                                .build()
                                .toByteArray(),
                        TileRequestData.VERSION_PROTOBUF),
                mMockTileCallback);

        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mFakeTileServiceController.get().mTileRequestParams).isNotNull();

        VersionBuilders.VersionInfo schemaVersion =
                mFakeTileServiceController
                        .get()
                        .mTileRequestParams
                        .getDeviceConfiguration()
                        .getRendererSchemaVersion();
        expect.that(schemaVersion.getMajor()).isEqualTo(3);
        expect.that(schemaVersion.getMinor()).isEqualTo(5);
    }

    @Test
    public void tileService_resourcesRequest_setsTileId() throws Exception {
        // Resources request needs to have DeviceParameters least to fill in the default.
        mTileProviderServiceStub.onResourcesRequest(
                TILE_ID,
                new ResourcesRequestData(
                        RequestProto.ResourcesRequest.newBuilder()
                                .build()
                                .toByteArray(),
                        ResourcesRequestData.VERSION_PROTOBUF),
                mMockResourcesCallback);

        shadowOf(Looper.getMainLooper()).idle();

        expect.that(mFakeTileServiceController.get().mTileId).isEqualTo(TILE_ID);
    }

    @Test
    public void tileService_resourcesRequest_defaultVersionIfNotSet() throws Exception {
        // Resources request needs to have DeviceParameters least to fill in the default.
        mTileProviderServiceStub.onResourcesRequest(
                5,
                new ResourcesRequestData(
                        RequestProto.ResourcesRequest.newBuilder()
                                .setDeviceConfiguration(DeviceParameters.getDefaultInstance())
                                .build()
                                .toByteArray(),
                        ResourcesRequestData.VERSION_PROTOBUF),
                mMockResourcesCallback);

        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mFakeTileServiceController.get().mResourcesRequestParams).isNotNull();

        VersionBuilders.VersionInfo schemaVersion =
                mFakeTileServiceController
                        .get()
                        .mResourcesRequestParams
                        .getDeviceConfiguration()
                        .getRendererSchemaVersion();
        expect.that(schemaVersion.getMajor()).isEqualTo(1);
        expect.that(schemaVersion.getMinor()).isEqualTo(0);
    }

    @Test
    public void tileService_resourcesRequest_passesVersionIfSet() throws Exception {
        mTileProviderServiceStub.onResourcesRequest(
                5,
                new ResourcesRequestData(
                        RequestProto.ResourcesRequest.newBuilder()
                                .setDeviceConfiguration(
                                        DeviceParameters.newBuilder()
                                                .setRendererSchemaVersion(
                                                        VersionInfo.newBuilder()
                                                                .setMajor(3)
                                                                .setMinor(5)))
                                .build()
                                .toByteArray(),
                        ResourcesRequestData.VERSION_PROTOBUF),
                mMockResourcesCallback);

        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mFakeTileServiceController.get().mResourcesRequestParams).isNotNull();

        VersionBuilders.VersionInfo schemaVersion =
                mFakeTileServiceController
                        .get()
                        .mResourcesRequestParams
                        .getDeviceConfiguration()
                        .getRendererSchemaVersion();
        expect.that(schemaVersion.getMajor()).isEqualTo(3);
        expect.that(schemaVersion.getMinor()).isEqualTo(5);
    }

    @Test
    public void tileService_tileRequest_catchesCancellationException() throws Exception {
        mFakeTileServiceController.get().mRequestFailure = new CancellationException();
        mTileProviderServiceStub.onTileRequest(
                5,
                new TileRequestData(
                        RequestProto.TileRequest.newBuilder()
                                .setDeviceConfiguration(
                                        DeviceParameters.newBuilder()
                                                .setRendererSchemaVersion(
                                                        VersionInfo.newBuilder()
                                                                .setMajor(3)
                                                                .setMinor(5)))
                                .build()
                                .toByteArray(),
                        TileRequestData.VERSION_PROTOBUF),
                mMockTileCallback);

        shadowOf(Looper.getMainLooper()).idle();

        verify(mMockTileCallback, never()).updateTileData(any());
    }

    @Test
    public void tileService_resourceRequest_catchesCancellationException() throws Exception {
        mFakeTileServiceController.get().mRequestFailure = new CancellationException();
        mTileProviderServiceStub.onResourcesRequest(
                5,
                new ResourcesRequestData(
                        RequestProto.ResourcesRequest.newBuilder()
                                .setDeviceConfiguration(
                                        DeviceParameters.newBuilder()
                                                .setRendererSchemaVersion(
                                                        VersionInfo.newBuilder()
                                                                .setMajor(3)
                                                                .setMinor(5)))
                                .build()
                                .toByteArray(),
                        ResourcesRequestData.VERSION_PROTOBUF),
                mMockResourcesCallback);

        shadowOf(Looper.getMainLooper()).idle();

        verify(mMockTileCallback, never()).updateTileData(any());
    }

    public static class FakeTileService extends TileService {
        boolean mOnTileAddCalled = false;
        boolean mOnTileRemoveCalled = false;
        boolean mOnTileEnterCalled = false;
        boolean mOnTileLeaveCalled = false;
        @Nullable TileRequest mTileRequestParams = null;
        @Nullable ResourcesRequest mResourcesRequestParams = null;
        @Nullable RuntimeException mRequestFailure = null;
        int mTileId = -1;

        @Override
        protected void onTileAddEvent(@NonNull TileAddEvent requestParams) {
            mOnTileAddCalled = true;
            mTileId = requestParams.getTileId();
        }

        @Override
        protected void onTileRemoveEvent(@NonNull TileRemoveEvent requestParams) {
            mOnTileRemoveCalled = true;
            mTileId = requestParams.getTileId();
        }

        @Override
        protected void onTileEnterEvent(@NonNull TileEnterEvent requestParams) {
            mOnTileEnterCalled = true;
            mTileId = requestParams.getTileId();
        }

        @Override
        protected void onTileLeaveEvent(@NonNull TileLeaveEvent requestParams) {
            mOnTileLeaveCalled = true;
            mTileId = requestParams.getTileId();
        }

        @Override
        @NonNull
        protected ListenableFuture<TileBuilders.Tile> onTileRequest(
                @NonNull TileRequest requestParams) {
            mTileRequestParams = requestParams;
            mTileId = requestParams.getTileId();
            if (mRequestFailure != null) {
                return Futures.immediateFailedFuture(mRequestFailure);
            }
            return Futures.immediateFuture(DUMMY_TILE_TO_RETURN);
        }

        @Override
        @NonNull
        protected ListenableFuture<Resources> onTileResourcesRequest(
                @NonNull ResourcesRequest requestParams) {
            mResourcesRequestParams = requestParams;
            mTileId = requestParams.getTileId();
            if (mRequestFailure != null) {
                return Futures.immediateFailedFuture(mRequestFailure);
            }

            Resources resources =
                    new Resources.Builder().setVersion(requestParams.getVersion()).build();

            return Futures.immediateFuture(resources);
        }
    }

    // Fake TileService that implements onResourcesRequest().
    public static class CompatibleFakeTileService extends TileService {
        @Override
        protected void onTileAddEvent(@NonNull TileAddEvent requestParams) {}

        @Override
        protected void onTileRemoveEvent(@NonNull TileRemoveEvent requestParams) {}

        @Override
        protected void onTileEnterEvent(@NonNull TileEnterEvent requestParams) {}

        @Override
        protected void onTileLeaveEvent(@NonNull TileLeaveEvent requestParams) {}

        @Override
        @NonNull
        protected ListenableFuture<TileBuilders.Tile> onTileRequest(
                @NonNull TileRequest requestParams) {
            return Futures.immediateFuture(DUMMY_TILE_TO_RETURN);
        }

        @Override
        @NonNull
        @SuppressWarnings("deprecation") // for backward compatibility
        protected ListenableFuture<androidx.wear.tiles.ResourceBuilders.Resources>
                onResourcesRequest(@NonNull ResourcesRequest requestParams) {
            androidx.wear.tiles.ResourceBuilders.Resources resources =
                    new androidx.wear.tiles.ResourceBuilders.Resources.Builder()
                            .setVersion(requestParams.getVersion())
                            .build();

            return Futures.immediateFuture(resources);
        }
    }
}
