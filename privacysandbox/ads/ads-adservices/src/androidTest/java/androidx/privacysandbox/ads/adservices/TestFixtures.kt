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

package androidx.privacysandbox.ads.adservices

import android.net.Uri
import androidx.privacysandbox.ads.adservices.common.ComponentAdData
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures

@OptIn(ExperimentalFeatures.Ext16OptIn::class)
class TestFixtures {
    companion object {

        // Static data for testing Component Ads
        val componentAd1: ComponentAdData = ComponentAdData(Uri.parse("ads1.com"), "render_id_1")
        val componentAd2: ComponentAdData = ComponentAdData(Uri.parse("ads2.com"), "render_id_2")
        val componentAds: List<ComponentAdData> = listOf(componentAd1, componentAd2)
    }
}
