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

package androidx.mediarouter.media;

import static org.junit.Assert.assertFalse;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Test for {@link MediaRouteDiscoveryRequest}. */
@RunWith(AndroidJUnit4.class)
public class MediaRouteDiscoveryRequestTest {

    @Test
    public void testFromBundle_withScreenOffScanning_doesNotBundleToMediaRouteDiscoveryRequest() {
        MediaRouteDiscoveryRequest request =
                new MediaRouteDiscoveryRequest(
                        new MediaRouteSelector.Builder()
                                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                                .build(),
                        /* activeScan= */ true,
                        /* shouldScanWithScreenOff= */ true);

        Bundle bundle = request.asBundle();
        MediaRouteDiscoveryRequest requestFromBundle =
                MediaRouteDiscoveryRequest.fromBundle(bundle);

        assertFalse(requestFromBundle.shouldScanWithScreenOff());
    }
}
