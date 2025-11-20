/*
 * Copyright 2025 The Android Open Source Project
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

import static android.os.Looper.getMainLooper;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.wear.tiles.SysUiTileUpdateRequester.CATEGORY_HOME_MAIN;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build.VERSION_CODES;
import android.provider.Settings;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.ResourceBuilders.Resources;
import androidx.wear.protolayout.protobuf.InvalidProtocolBufferException;
import androidx.wear.tiles.RequestBuilders.ResourcesRequest;
import androidx.wear.tiles.RequestBuilders.TileRequest;
import androidx.wear.tiles.TileBuilders.Tile;
import androidx.wear.tiles.proto.RequestProto;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class SysUiTileUpdateRequesterTest {
    private static final String SYSUI_SETTINGS_KEY = "clockwork_sysui_package";

    private static final ComponentName STANDARD_SYSUI_RECEIVER_COMPONENT_NAME =
            new ComponentName("com.google.android.wearable.app", "TileUpdateReceiver");
    private static final ComponentName OTHER_SYSUI_RECEIVER_COMPONENT_NAME =
            new ComponentName("my.awesome.sysui", "UpdateReceiver");
    private static final ComponentName HOME_ACTIVITY_COMPONENT_NAME =
            new ComponentName(OTHER_SYSUI_RECEIVER_COMPONENT_NAME.getPackageName(), "HomeActivity");
    private TileUpdateReceiver mStandardSysUiFakeReceiver;
    private TileUpdateReceiver mOtherSysUiFakeReceiver;
    private SysUiTileUpdateRequester mUpdateRequester;

    @Before
    public void setUp() throws Exception {
        Settings.Global.putString(
                getApplicationContext().getContentResolver(), SYSUI_SETTINGS_KEY, null);
        mStandardSysUiFakeReceiver = new TileUpdateReceiver();
        mOtherSysUiFakeReceiver = new TileUpdateReceiver();

        // SysUiTileUpdateRequester searches PM for the service accepting
        // ACTION_BIND_UPDATE_REQUESTER; register it as a package here...
        IntentFilter intentFilter =
                new IntentFilter(SysUiTileUpdateRequester.ACTION_BIND_UPDATE_REQUESTER);

        // Used on U when the app's target SDK is higher than U.
        IntentFilter homeIntentFilter = new IntentFilter(Intent.ACTION_MAIN);
        homeIntentFilter.addCategory(CATEGORY_HOME_MAIN);
        homeIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        // Robolectric won't tell us what service was bound (it'll just tell us the
        // ServiceConnection, which doesn't help). Instead, use two different instances of
        // TileUpdateReceiver, and just check which one received the call.
        ShadowPackageManager spm = shadowOf(getApplicationContext().getPackageManager());
        spm.addServiceIfNotPresent(STANDARD_SYSUI_RECEIVER_COMPONENT_NAME);
        spm.addServiceIfNotPresent(OTHER_SYSUI_RECEIVER_COMPONENT_NAME);
        spm.addIntentFilterForService(STANDARD_SYSUI_RECEIVER_COMPONENT_NAME, intentFilter);
        spm.addIntentFilterForService(OTHER_SYSUI_RECEIVER_COMPONENT_NAME, intentFilter);

        spm.addActivityIfNotPresent(HOME_ACTIVITY_COMPONENT_NAME);
        spm.addIntentFilterForActivity(HOME_ACTIVITY_COMPONENT_NAME, homeIntentFilter);

        // Don't believe the lies of Intent-less setComponentNameAndServiceForBindService; it just
        // sets the default service to bind to, rather than setting the service instance for that
        // ComponentName (so the second call overwrites the first, and everything breaks). You
        // **must** use setComponentNameAndServiceForBindServiceForIntent to simulate binding to
        // different services.
        ShadowApplication shadowApp = shadowOf((Application) getApplicationContext());

        Intent intent = new Intent(SysUiTileUpdateRequester.ACTION_BIND_UPDATE_REQUESTER);
        intent.setPackage(STANDARD_SYSUI_RECEIVER_COMPONENT_NAME.getPackageName());
        intent.setComponent(STANDARD_SYSUI_RECEIVER_COMPONENT_NAME);
        shadowApp.setComponentNameAndServiceForBindServiceForIntent(
                intent,
                STANDARD_SYSUI_RECEIVER_COMPONENT_NAME,
                mStandardSysUiFakeReceiver.asBinder());

        Intent otherSysUiIntent = new Intent(SysUiTileUpdateRequester.ACTION_BIND_UPDATE_REQUESTER);
        otherSysUiIntent.setPackage(OTHER_SYSUI_RECEIVER_COMPONENT_NAME.getPackageName());
        otherSysUiIntent.setComponent(OTHER_SYSUI_RECEIVER_COMPONENT_NAME);
        shadowApp.setComponentNameAndServiceForBindServiceForIntent(
                otherSysUiIntent,
                OTHER_SYSUI_RECEIVER_COMPONENT_NAME,
                mOtherSysUiFakeReceiver.asBinder());
        mUpdateRequester = new SysUiTileUpdateRequester(getApplicationContext());
    }

    @Test
    public void requestUpdate_canBeCalled() {
        mUpdateRequester.requestUpdate(MyTileService.class);
        shadowOf(getMainLooper()).idle();

        assertThat(mOtherSysUiFakeReceiver.mRequestedComponents).isEmpty();
        assertThat(mStandardSysUiFakeReceiver.mRequestedComponents)
                .contains(new ComponentName(getApplicationContext(), MyTileService.class));
    }

    @Test
    public void requestUpdate_unbindsAfterCall() {
        ShadowApplication shadowApp = shadowOf((Application) getApplicationContext());

        mUpdateRequester.requestUpdate(MyTileService.class);
        shadowOf(getMainLooper()).idle();

        assertThat(shadowApp.getBoundServiceConnections()).isEmpty();
        assertThat(shadowApp.getUnboundServiceConnections()).hasSize(1);
    }

    @Test
    public void requestUpdate_queuesUpdatesWhileBinding() {
        // Obviously, we can't properly control the service lifecycle here. The best we can do is to
        // just block the looper, so the service-side calls can't execute yet, to simulate a second
        // requestUpdate coming in while the service is binding.
        mUpdateRequester.requestUpdate(MyTileService.class);
        mUpdateRequester.requestUpdate(MyOtherTileService.class);
        shadowOf(getMainLooper()).idle();

        assertThat(mStandardSysUiFakeReceiver.mRequestedComponents)
                .contains(new ComponentName(getApplicationContext(), MyTileService.class));
        assertThat(mStandardSysUiFakeReceiver.mRequestedComponents)
                .contains(new ComponentName(getApplicationContext(), MyOtherTileService.class));

        // Ensure that there was only one connection made...
        ShadowApplication shadowApp = shadowOf((Application) getApplicationContext());
        assertThat(shadowApp.getBoundServiceConnections()).isEmpty();
        assertThat(shadowApp.getUnboundServiceConnections()).hasSize(1);
    }

    @Test
    public void requestUpdate_multipleUpdatesDebounced() {
        mUpdateRequester.requestUpdate(MyTileService.class);
        mUpdateRequester.requestUpdate(MyTileService.class);
        shadowOf(getMainLooper()).idle();

        assertThat(mStandardSysUiFakeReceiver.mRequestedComponents).hasSize(1);
    }

    @Test
    public void requestUpdateWithId_sendsId() {
        final int tileId = 27;
        mUpdateRequester.requestUpdate(MyTileService.class, tileId);
        shadowOf(getMainLooper()).idle();

        assertThat(mStandardSysUiFakeReceiver.mRequestedComponents)
                .containsExactly(new ComponentName(getApplicationContext(), MyTileService.class));
        assertThat(mStandardSysUiFakeReceiver.mRequestedIds).containsExactly(tileId);
    }

    @Test
    public void requestUpdateWithId_multipleIds_notDebounced() {
        final int tileId1 = 27;
        final int tileId2 = 28;
        mUpdateRequester.requestUpdate(MyTileService.class, tileId1);
        mUpdateRequester.requestUpdate(MyTileService.class, tileId2);
        shadowOf(getMainLooper()).idle();

        assertThat(mStandardSysUiFakeReceiver.mRequestedComponents).hasSize(2);
        assertThat(mStandardSysUiFakeReceiver.mRequestedIds).containsExactly(tileId1, tileId2);
    }

    @Test
    public void requestUpdateWithId_multipleSameId_debounced() {
        mUpdateRequester.requestUpdate(MyTileService.class, 1);
        mUpdateRequester.requestUpdate(MyTileService.class, 1);
        shadowOf(getMainLooper()).idle();

        assertThat(mStandardSysUiFakeReceiver.mRequestedComponents).hasSize(1);
    }

    @Test
    public void requestUpdate_usesGivenSysUiIfSet() {
        Settings.Global.putString(
                getApplicationContext().getContentResolver(),
                SYSUI_SETTINGS_KEY,
                OTHER_SYSUI_RECEIVER_COMPONENT_NAME.getPackageName());

        mUpdateRequester.requestUpdate(MyTileService.class);
        shadowOf(getMainLooper()).idle();

        assertThat(mStandardSysUiFakeReceiver.mRequestedComponents).isEmpty();
        assertThat(mOtherSysUiFakeReceiver.mRequestedComponents).hasSize(1);
    }

    @Test
    @Config(sdk = VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void requestUpdate_onU_withHigherTargetSdk_usesSysUiPackageFromHomeActivity() {
        mUpdateRequester.requestUpdate(MyTileService.class);
        shadowOf(getMainLooper()).idle();

        assertThat(mStandardSysUiFakeReceiver.mRequestedComponents).isEmpty();
        assertThat(mOtherSysUiFakeReceiver.mRequestedComponents).hasSize(1);
    }

    static class TileUpdateReceiver extends TileUpdateRequesterService.Stub {
        List<ComponentName> mRequestedComponents = new ArrayList<>();
        List<Integer> mRequestedIds = new ArrayList<>();

        @Override
        public int getApiVersion() {
            return TileUpdateRequesterService.API_VERSION;
        }

        @Override
        public void requestUpdate(ComponentName component, TileUpdateRequestData updateData) {
            mRequestedComponents.add(component);
            try {
                RequestProto.TileUpdateRequest request =
                        RequestProto.TileUpdateRequest.parseFrom(updateData.getContents());
                mRequestedIds.add(request.getTileId());
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Tile Providers are referred to by class; make two fake classes (extending Service) that we
    // can use to pass to SysUiTileUpdateRequester#requestUpdate.
    static class MyTileService extends TileService {
        @Override
        protected @NonNull ListenableFuture<Tile> onTileRequest(
                @NonNull TileRequest requestParams) {
            return Futures.immediateFuture(null);
        }

        @Override
        protected @NonNull ListenableFuture<Resources> onTileResourcesRequest(
                @NonNull ResourcesRequest requestParams) {
            return Futures.immediateFuture(null);
        }
    }

    static class MyOtherTileService extends TileService {
        @Override
        protected @NonNull ListenableFuture<Tile> onTileRequest(
                @NonNull TileRequest requestParams) {
            return Futures.immediateFuture(null);
        }

        @Override
        protected @NonNull ListenableFuture<Resources> onTileResourcesRequest(
                @NonNull ResourcesRequest requestParams) {
            return Futures.immediateFuture(null);
        }
    }
}
