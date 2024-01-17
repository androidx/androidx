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
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
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
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowSystemClock;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowSystemClock.class})
@DoNotInstrument
public class TileServiceTest {
    private static final int TILE_ID = 42;
    private static final int TILE_ID_1 = 22;
    private static final int TILE_ID_2 = 33;
    private static final long TIMESTAMP_MS = Duration.ofDays(65).toMillis();
    private static final long TIMESTAMP_MS_NEEDS_UPDATE =
            TIMESTAMP_MS - Duration.ofDays(1).toMillis();
    private static final long TIMESTAMP_MS_NEEDS_REMOVED =
            TIMESTAMP_MS - Duration.ofDays(60).toMillis();
    private static final long TIMESTAMP_MS_NO_UPDATE =
            TIMESTAMP_MS - Duration.ofHours(10).toMillis();

    @Rule public final MockitoRule mocks = MockitoJUnit.rule();
    @Rule public final Expect expect = Expect.create();

    // This is a little nasty, but we need to ensure that the version is injected by TileService.
    // For that, we need the builder form (for FakeTileService to return), and the protobuf form (to
    // compare against, which also includes the version).
    private static final TileBuilders.Tile DUMMY_TILE_TO_RETURN =
            new TileBuilders.Tile.Builder().setResourcesVersion("5").build();
    private static final Tile DUMMY_TILE_PROTOBUF =
            Tile.newBuilder().setResourcesVersion("5").setSchemaVersion(Version.CURRENT).build();
    private static final String SHARED_PREF_NAME = "active_tiles_shared_preferences";
    private static FakeTimeSourceClockImpl sFakeTimeSourceClock = new FakeTimeSourceClockImpl();

    private TileProvider mTileProviderServiceStub;
    private TileProvider mCompatibleTileProviderServiceStub;
    private ServiceController<FakeTileService> mFakeTileServiceController;
    private ServiceController<CompatibleFakeTileService> mCompatibleFakeTileServiceController;
    private Context mTestContext;
    private SharedPreferences mSharedPreferences;

    @Mock private TileCallback mMockTileCallback;
    @Mock private ResourcesCallback mMockResourcesCallback;
    @Mock private Context mMockContext;
    private static final String FAKE_TILE_IDENTIFIER_1 =
            new ActiveTileIdentifier(
                            new ComponentName(
                                    ApplicationProvider.getApplicationContext(),
                                    FakeTileService.class),
                            TILE_ID_1)
                    .flattenToString();
    private static final String FAKE_COMPAT_TILE_IDENTIFIER_2 =
            new ActiveTileIdentifier(
                            new ComponentName(
                                    ApplicationProvider.getApplicationContext(),
                                    CompatibleFakeTileService.class),
                            TILE_ID_2)
                    .flattenToString();

    @Before
    public void setUp() {
        mMockTileCallback = mock(TileCallback.class);
        mMockResourcesCallback = mock(ResourcesCallback.class);
        mMockContext = mock(Context.class);

        mFakeTileServiceController = Robolectric.buildService(FakeTileService.class);
        mCompatibleFakeTileServiceController =
                Robolectric.buildService(CompatibleFakeTileService.class);

        Intent i = new Intent(TileService.ACTION_BIND_TILE_PROVIDER);
        mTileProviderServiceStub =
                TileProvider.Stub.asInterface(mFakeTileServiceController.get().onBind(i));
        mCompatibleTileProviderServiceStub =
                TileProvider.Stub.asInterface(mCompatibleFakeTileServiceController.get().onBind(i));

        mTestContext = ApplicationProvider.getApplicationContext();
        mSharedPreferences =
                mTestContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        assertTrue(mSharedPreferences.getAll().isEmpty());
        sFakeTimeSourceClock.setCurrentTimestampMs(TIMESTAMP_MS);
    }

    @Test
    public void getActiveTilesSnapshotAsync_noActions_readEmptySharedPref() throws Exception {
        assertThat(mSharedPreferences.getAll()).isEmpty();

        assertThat(
                        TileService.getActiveTilesSnapshotAsync(
                                        mTestContext, directExecutor(), sFakeTimeSourceClock)
                                .get())
                .isEmpty();
    }

    @Test
    public void getActiveTilesSnapshotAsync_onTileAdded_addTileToSharedPref() throws Exception {
        assertFalse(mSharedPreferences.contains(FAKE_TILE_IDENTIFIER_1));

        mTileProviderServiceStub.onTileAddEvent(
                new TileAddEventData(
                        EventProto.TileAddEvent.newBuilder()
                                .setTileId(TILE_ID_1)
                                .build()
                                .toByteArray(),
                        TileAddEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();
        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(Map.of(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS));
        assertThat(serializeTilesList(result))
                .containsExactlyElementsIn(Arrays.asList(FAKE_TILE_IDENTIFIER_1));
    }

    @Test
    public void getActiveTilesSnapshotAsync_onTileEnter_addTileToSharedPref() throws Exception {
        assertFalse(mSharedPreferences.contains(FAKE_TILE_IDENTIFIER_1));

        mTileProviderServiceStub.onTileEnterEvent(
                new TileEnterEventData(
                        EventProto.TileEnterEvent.newBuilder()
                                .setTileId(TILE_ID_1)
                                .build()
                                .toByteArray(),
                        TileEnterEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();
        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(Map.of(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS));
        assertThat(serializeTilesList(result))
                .containsExactlyElementsIn(Arrays.asList(FAKE_TILE_IDENTIFIER_1));
    }

    @Test
    public void getActiveTilesSnapshotAsync_onTileLeave_addTileToSharedPref() throws Exception {
        assertFalse(mSharedPreferences.contains(FAKE_TILE_IDENTIFIER_1));

        mTileProviderServiceStub.onTileLeaveEvent(
                new TileLeaveEventData(
                        EventProto.TileLeaveEvent.newBuilder()
                                .setTileId(TILE_ID_1)
                                .build()
                                .toByteArray(),
                        TileLeaveEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();
        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(Map.of(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS));
        assertThat(serializeTilesList(result))
                .containsExactlyElementsIn(Arrays.asList(FAKE_TILE_IDENTIFIER_1));
    }

    @Test
    public void getActiveTilesSnapshotAsync_onTileRequest_addTileToSharedPref() throws Exception {
        assertFalse(mSharedPreferences.contains(FAKE_TILE_IDENTIFIER_1));

        mTileProviderServiceStub.onTileRequest(
                TILE_ID_1,
                new TileRequestData(
                        RequestProto.TileRequest.newBuilder().build().toByteArray(),
                        TileRequestData.VERSION_PROTOBUF),
                mMockTileCallback);
        shadowOf(Looper.getMainLooper()).idle();
        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(Map.of(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS));
        assertThat(serializeTilesList(result))
                .containsExactlyElementsIn(Arrays.asList(FAKE_TILE_IDENTIFIER_1));
    }

    @Test
    public void getActiveTilesSnapshotAsync_onTileResourcesRequest_addTileToSharedPref()
            throws Exception {
        assertFalse(mSharedPreferences.contains(FAKE_COMPAT_TILE_IDENTIFIER_2));

        ResourcesRequestData resourcesRequestData =
                new ResourcesRequestData(
                        RequestProto.ResourcesRequest.newBuilder()
                                .setVersion("HELLO WORLD")
                                .build()
                                .toByteArray(),
                        ResourcesRequestData.VERSION_PROTOBUF);
        mCompatibleTileProviderServiceStub.onResourcesRequest(
                TILE_ID_2, resourcesRequestData, mMockResourcesCallback);
        shadowOf(Looper.getMainLooper()).idle();
        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(Map.of(FAKE_COMPAT_TILE_IDENTIFIER_2, TIMESTAMP_MS));
        assertThat(serializeTilesList(result))
                .containsExactlyElementsIn(Arrays.asList(FAKE_COMPAT_TILE_IDENTIFIER_2));
    }

    @Test
    public void getActiveTilesSnapshotAsync_addToSharedPref_doNothingIfAlreadyAddedRecently()
            throws Exception {
        mSharedPreferences.edit().putLong(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS_NO_UPDATE).commit();
        assertTrue(mSharedPreferences.contains(FAKE_TILE_IDENTIFIER_1));

        mTileProviderServiceStub.onTileAddEvent(
                new TileAddEventData(
                        EventProto.TileAddEvent.newBuilder()
                                .setTileId(TILE_ID_1)
                                .build()
                                .toByteArray(),
                        TileAddEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();
        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(Map.of(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS_NO_UPDATE));
        assertThat(serializeTilesList(result))
                .containsExactlyElementsIn(Arrays.asList(FAKE_TILE_IDENTIFIER_1));
    }

    @Test
    public void getActiveTilesSnapshotAsync_addToSharedPref_alreadyAddedTimestampNeedsUpdate()
            throws Exception {
        mSharedPreferences
                .edit()
                .putLong(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS_NEEDS_UPDATE)
                .commit();
        assertTrue(mSharedPreferences.contains(FAKE_TILE_IDENTIFIER_1));

        mTileProviderServiceStub.onTileAddEvent(
                new TileAddEventData(
                        EventProto.TileAddEvent.newBuilder()
                                .setTileId(TILE_ID_1)
                                .build()
                                .toByteArray(),
                        TileAddEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();
        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(Map.of(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS));
        assertThat(serializeTilesList(result))
                .containsExactlyElementsIn(Arrays.asList(FAKE_TILE_IDENTIFIER_1));
    }

    @Test
    public void getActiveTilesSnapshotAsync_onTileRemoved_removeFromSharedPref() throws Exception {
        mSharedPreferences.edit().putLong(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS).commit();
        assertTrue(mSharedPreferences.contains(FAKE_TILE_IDENTIFIER_1));

        mTileProviderServiceStub.onTileRemoveEvent(
                new TileRemoveEventData(
                        EventProto.TileRemoveEvent.newBuilder()
                                .setTileId(TILE_ID_1)
                                .build()
                                .toByteArray(),
                        TileRemoveEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();
        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertTrue(mSharedPreferences.getAll().isEmpty());
        assertTrue(result.isEmpty());
    }

    @Test
    public void getActiveTilesSnapshotAsync_removeFromSharedPref_doNothingIfNotInSharedPref()
            throws Exception {
        assertFalse(mSharedPreferences.contains(FAKE_TILE_IDENTIFIER_1));

        mTileProviderServiceStub.onTileRemoveEvent(
                new TileRemoveEventData(
                        EventProto.TileRemoveEvent.newBuilder()
                                .setTileId(TILE_ID_1)
                                .build()
                                .toByteArray(),
                        TileRemoveEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();
        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertTrue(mSharedPreferences.getAll().isEmpty());
        assertTrue(result.isEmpty());
    }

    @Test
    public void getActiveTilesSnapshotAsync_onTileAddedFromMultipleServicesFromSameApp()
            throws Exception {
        assertFalse(mSharedPreferences.contains(FAKE_TILE_IDENTIFIER_1));
        assertFalse(mSharedPreferences.contains(FAKE_COMPAT_TILE_IDENTIFIER_2));

        sFakeTimeSourceClock.setCurrentTimestampMs(TIMESTAMP_MS_NO_UPDATE);
        mTileProviderServiceStub.onTileAddEvent(
                new TileAddEventData(
                        EventProto.TileAddEvent.newBuilder()
                                .setTileId(TILE_ID_1)
                                .build()
                                .toByteArray(),
                        TileAddEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();
        sFakeTimeSourceClock.setCurrentTimestampMs(TIMESTAMP_MS);
        mCompatibleTileProviderServiceStub.onTileAddEvent(
                new TileAddEventData(
                        EventProto.TileAddEvent.newBuilder()
                                .setTileId(TILE_ID_2)
                                .build()
                                .toByteArray(),
                        TileAddEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();
        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(
                        Map.of(
                                FAKE_TILE_IDENTIFIER_1,
                                TIMESTAMP_MS_NO_UPDATE,
                                FAKE_COMPAT_TILE_IDENTIFIER_2,
                                TIMESTAMP_MS));
        assertThat(serializeTilesList(result))
                .containsExactlyElementsIn(
                        Arrays.asList(FAKE_TILE_IDENTIFIER_1, FAKE_COMPAT_TILE_IDENTIFIER_2));
    }

    @Test
    public void getActiveTilesSnapshotAsync_afterEvent_readAllDataFromSharedPref()
            throws Exception {
        mSharedPreferences
                .edit()
                .putLong(FAKE_COMPAT_TILE_IDENTIFIER_2, TIMESTAMP_MS_NO_UPDATE)
                .commit();
        assertTrue(mSharedPreferences.contains(FAKE_COMPAT_TILE_IDENTIFIER_2));

        mTileProviderServiceStub.onTileAddEvent(
                new TileAddEventData(
                        EventProto.TileAddEvent.newBuilder()
                                .setTileId(TILE_ID_1)
                                .build()
                                .toByteArray(),
                        TileAddEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();

        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(
                        Map.of(
                                FAKE_COMPAT_TILE_IDENTIFIER_2,
                                TIMESTAMP_MS_NO_UPDATE,
                                FAKE_TILE_IDENTIFIER_1,
                                TIMESTAMP_MS));
        assertThat(serializeTilesList(result))
                .containsExactlyElementsIn(
                        Arrays.asList(FAKE_COMPAT_TILE_IDENTIFIER_2, FAKE_TILE_IDENTIFIER_1));
    }

    @Test
    public void getActiveTilesSnapshotAsync_addToSharedPref_cleanupOldDataFromSharedPref()
            throws Exception {
        mSharedPreferences
                .edit()
                .putLong(FAKE_COMPAT_TILE_IDENTIFIER_2, TIMESTAMP_MS_NEEDS_REMOVED)
                .commit();
        assertTrue(mSharedPreferences.contains(FAKE_COMPAT_TILE_IDENTIFIER_2));

        mTileProviderServiceStub.onTileAddEvent(
                new TileAddEventData(
                        EventProto.TileAddEvent.newBuilder()
                                .setTileId(TILE_ID_1)
                                .build()
                                .toByteArray(),
                        TileAddEventData.VERSION_PROTOBUF));
        shadowOf(Looper.getMainLooper()).idle();
        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(Map.of(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS));
        assertThat(serializeTilesList(result))
                .containsExactlyElementsIn(Arrays.asList(FAKE_TILE_IDENTIFIER_1));
    }

    @Test
    public void getActiveTilesSnapshotAsync_readFromSharedPref_cleanupOldDataFromSharedPref()
            throws Exception {
        mSharedPreferences.edit().putLong(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS).commit();
        mSharedPreferences
                .edit()
                .putLong(FAKE_COMPAT_TILE_IDENTIFIER_2, TIMESTAMP_MS_NEEDS_REMOVED)
                .commit();
        assertTrue(mSharedPreferences.contains(FAKE_TILE_IDENTIFIER_1));
        assertTrue(mSharedPreferences.contains(FAKE_COMPAT_TILE_IDENTIFIER_2));

        List<ActiveTileIdentifier> result =
                TileService.getActiveTilesSnapshotAsync(
                                mTestContext, directExecutor(), sFakeTimeSourceClock)
                        .get();

        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(Map.of(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS));
        assertThat(serializeTilesList(result))
                .containsExactlyElementsIn(Arrays.asList(FAKE_TILE_IDENTIFIER_1));
    }

    @Test
    public void getActiveTilesSnapshotAsync_overriddenSharedPreferences_throwsException() {
        ExecutionException thrownException =
                assertThrows(
                        ExecutionException.class,
                        () ->
                                TileService.getActiveTilesSnapshotAsync(
                                                mMockContext,
                                                directExecutor(),
                                                sFakeTimeSourceClock)
                                        .get());

        assertThat(thrownException).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void getActiveTilesSnapshotAsync_overriddenPackageName_throwsException() {
        mSharedPreferences.edit().putLong(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS).commit();
        assertTrue(mSharedPreferences.contains(FAKE_TILE_IDENTIFIER_1));
        when(mMockContext.getPackageName()).thenReturn("WrongPackageNameRequested");
        when(mMockContext.getSharedPreferences(anyString(), anyInt()))
                .thenReturn(mSharedPreferences);

        ExecutionException thrownException =
                assertThrows(
                        ExecutionException.class,
                        () ->
                                TileService.getActiveTilesSnapshotAsync(
                                                mMockContext,
                                                directExecutor(),
                                                sFakeTimeSourceClock)
                                        .get());

        assertThat(thrownException).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(Map.of(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS));
    }

    @Test
    public void getActiveTilesSnapshotAsync_notAllPackageNamesMatching_throwsException() {
        String fakeTileIdentifierWrongPackage =
                new ActiveTileIdentifier(
                                new ComponentName(
                                        "different_package_name", FakeTileService.class.getName()),
                                TILE_ID_2)
                        .flattenToString();
        mSharedPreferences.edit().putLong(FAKE_TILE_IDENTIFIER_1, TIMESTAMP_MS).commit();
        mSharedPreferences.edit().putLong(fakeTileIdentifierWrongPackage, TIMESTAMP_MS).commit();
        assertTrue(mSharedPreferences.contains(FAKE_TILE_IDENTIFIER_1));
        assertTrue(mSharedPreferences.contains(fakeTileIdentifierWrongPackage));

        ExecutionException thrownException =
                assertThrows(
                        ExecutionException.class,
                        () ->
                                TileService.getActiveTilesSnapshotAsync(
                                                mTestContext,
                                                directExecutor(),
                                                sFakeTimeSourceClock)
                                        .get());

        assertThat(thrownException).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
        assertThat(mSharedPreferences.getAll())
                .containsExactlyEntriesIn(
                        Map.of(
                                FAKE_TILE_IDENTIFIER_1,
                                TIMESTAMP_MS,
                                fakeTileIdentifierWrongPackage,
                                TIMESTAMP_MS));
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
                        RequestProto.TileRequest.newBuilder().build().toByteArray(),
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
                        RequestProto.ResourcesRequest.newBuilder().build().toByteArray(),
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
        TimeSourceClock getTimeSourceClock() {
            return sFakeTimeSourceClock;
        }

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
        TimeSourceClock getTimeSourceClock() {
            return sFakeTimeSourceClock;
        }

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

    private static List<String> serializeTilesList(List<ActiveTileIdentifier> result) {
        return result.stream()
                .map(ActiveTileIdentifier::flattenToString)
                .collect(Collectors.toList());
    }

    static class FakeTimeSourceClockImpl implements TileService.TimeSourceClock {
        long mTestCurrentTimeMs = -1L;

        @Override
        public long getCurrentTimestampMillis() {
            return mTestCurrentTimeMs;
        }

        void setCurrentTimestampMs(long timestampMs) {
            mTestCurrentTimeMs = timestampMs;
        }
    }
}
