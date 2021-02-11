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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.wear.tiles.builders.ResourceBuilders;
import androidx.wear.tiles.builders.TileBuilders;
import androidx.wear.tiles.proto.RequestProto;
import androidx.wear.tiles.proto.ResourceProto.Resources;
import androidx.wear.tiles.proto.TileProto.Tile;
import androidx.wear.tiles.readers.RequestReaders.ResourcesRequest;
import androidx.wear.tiles.readers.RequestReaders.TileRequest;

import com.google.common.truth.Expect;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TileProviderServiceTest {
    @Rule public final Expect expect = Expect.create();

    private static final TileBuilders.Tile DUMMY_TILE =
            TileBuilders.Tile.builder().setResourcesVersion("5").build();
    private TileProvider mTileProviderService;

    @Mock private TileCallback mMockTileCallback;
    @Mock private ResourcesCallback mMockResourcesCallback;

    @Before
    public void setUp() {
        mMockTileCallback = mock(TileCallback.class);
        mMockResourcesCallback = mock(ResourcesCallback.class);

        ServiceController<DummyTileProviderService> dummyTileProviderServiceController =
                Robolectric.buildService(
                        DummyTileProviderService.class);

        Intent i = new Intent(TileProviderService.ACTION_BIND_TILE_PROVIDER);
        IBinder binder = dummyTileProviderServiceController.get().onBind(i);
        mTileProviderService = TileProvider.Stub.asInterface(binder);
    }

    @Test
    public void tileProvider_tileRequest() throws Exception {
        mTileProviderService.onTileRequest(
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
                        tileCaptor.getValue().getContents());

        expect.that(tile).isEqualTo(DUMMY_TILE.toProto());
    }

    @Ignore("Disabled due to b/179074319")
    @Test
    public void tileProvider_resourcesRequest() throws Exception {
        final String resourcesVersion = "HELLO WORLD";

        ResourcesRequestData resourcesRequestData =
                new ResourcesRequestData(
                        RequestProto.ResourcesRequest.newBuilder()
                                .setVersion(resourcesVersion)
                                .build()
                                .toByteArray(),
                        ResourcesRequestData.VERSION_PROTOBUF);

        mTileProviderService.onResourcesRequest(5, resourcesRequestData, mMockResourcesCallback);

        shadowOf(Looper.getMainLooper()).idle();

        ArgumentCaptor<ResourcesData> resourcesCaptor = ArgumentCaptor.forClass(
                ResourcesData.class);
        verify(mMockResourcesCallback).updateResources(resourcesCaptor.capture());

        Resources resources =
                Resources.parseFrom(
                        resourcesCaptor.getValue().getContents());

        expect.that(resources.getVersion()).isEqualTo(resourcesVersion);
    }

    public static class DummyTileProviderService extends TileProviderService {
        @Override
        @NonNull
        protected ListenableFuture<TileBuilders.Tile> onTileRequest(
                @NonNull TileRequest requestParams) {
            return Futures.immediateFuture(DUMMY_TILE);
        }

        @Override
        @NonNull
        protected ListenableFuture<ResourceBuilders.Resources> onResourcesRequest(
                @NonNull ResourcesRequest requestParams) {
            ResourceBuilders.Resources resources =
                    ResourceBuilders.Resources.builder().setVersion(
                            requestParams.getVersion()).build();

            return Futures.immediateFuture(resources);
        }
    }
}
