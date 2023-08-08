/*
 * Copyright 2023 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class RouteListingPreferenceTest {

    private static final String FAKE_ROUTE_ID = "fake_id";
    private static final String FAKE_CUSTOM_SUBTEXT = "a custom subtext";
    private static final ComponentName FAKE_COMPONENT_NAME =
            new ComponentName(
                    ApplicationProvider.getApplicationContext(), RouteListingPreferenceTest.class);

    private Context mContext;
    private MediaRouter mMediaRouterUnderTest;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> mMediaRouterUnderTest = MediaRouter.getInstance(mContext));
    }

    @After
    public void tearDown() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () ->
                                mMediaRouterUnderTest.setRouteListingPreference(
                                        /* routeListingPreference= */ null));
    }

    @SmallTest
    @Test
    public void setRouteListingPreference_onAnyApiLevel_doesNotCrash() {
        // AndroidX infra runs tests on all API levels with significant usage, hence this test
        // checks this call does not crash regardless of whether route listing preference symbols
        // are defined on the current platform level.
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () ->
                                mMediaRouterUnderTest.setRouteListingPreference(
                                        new RouteListingPreference.Builder().build()));
    }

    @SmallTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
    public void routeListingPreference_yieldsExpectedPlatformEquivalent() {
        RouteListingPreference.Item fakeRlpItem =
                new RouteListingPreference.Item.Builder(FAKE_ROUTE_ID)
                        .setFlags(RouteListingPreference.Item.FLAG_SUGGESTED)
                        .setSubText(RouteListingPreference.Item.SUBTEXT_CUSTOM)
                        .setCustomSubtextMessage(FAKE_CUSTOM_SUBTEXT)
                        .setSelectionBehavior(
                                RouteListingPreference.Item.SELECTION_BEHAVIOR_GO_TO_APP)
                        .build();
        RouteListingPreference fakeRouteListingPreference =
                new RouteListingPreference.Builder()
                        .setItems(Collections.singletonList(fakeRlpItem))
                        .setLinkedItemComponentName(FAKE_COMPONENT_NAME)
                        .setSystemOrderingEnabled(false)
                        .build();
        android.media.RouteListingPreference platformRlp =
                fakeRouteListingPreference.toPlatformRouteListingPreference();

        assertThat(platformRlp.getUseSystemOrdering()).isFalse();
        assertThat(platformRlp.getLinkedItemComponentName()).isEqualTo(FAKE_COMPONENT_NAME);

        List<android.media.RouteListingPreference.Item> platformRlpItems = platformRlp.getItems();
        assertThat(platformRlpItems).hasSize(1);
        android.media.RouteListingPreference.Item platformRlpItem = platformRlpItems.get(0);
        assertThat(platformRlpItem.getRouteId()).isEqualTo(FAKE_ROUTE_ID);
        assertThat(platformRlpItem.getFlags())
                .isEqualTo(RouteListingPreference.Item.FLAG_SUGGESTED);
        assertThat(platformRlpItem.getSelectionBehavior())
                .isEqualTo(RouteListingPreference.Item.SELECTION_BEHAVIOR_GO_TO_APP);
        assertThat(platformRlpItem.getSubText())
                .isEqualTo(android.media.RouteListingPreference.Item.SUBTEXT_CUSTOM);
        assertThat(platformRlpItem.getCustomSubtextMessage()).isEqualTo(FAKE_CUSTOM_SUBTEXT);
    }
}
