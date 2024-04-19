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

package androidx.car.app.mediaextensions.analytics;

import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_ROOT_KEY_OPT_IN;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_SHARE_OEM_DIAGNOSTICS;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_SHARE_PLATFORM_DIAGNOSTICS;

import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import androidx.car.app.mediaextensions.analytics.client.RootHintsPopulator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class RootHintsPopulatorTest {

    @Test
    public void testRootHintPopulator() {
        Bundle bundle = new Bundle();
        new RootHintsPopulator(bundle)
                .setAnalyticsOptIn(true)
                .setShareOem(true)
                .setSharePlatform(true);

        boolean optIn = bundle.getBoolean(ANALYTICS_ROOT_KEY_OPT_IN, false);
        boolean oemShare = bundle.getBoolean(ANALYTICS_SHARE_OEM_DIAGNOSTICS, false);
        boolean platformShare = bundle.getBoolean(ANALYTICS_SHARE_PLATFORM_DIAGNOSTICS, false);

        assertTrue(optIn);
        assertTrue(oemShare);
        assertTrue(platformShare);
    }
}
