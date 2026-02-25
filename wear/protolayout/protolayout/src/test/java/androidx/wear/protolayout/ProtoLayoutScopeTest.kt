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

package androidx.wear.protolayout

import android.app.PendingIntent
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.ProtoLayoutScope.RendererCapability.LOTTIE_COLOR_FOR_SLOT
import androidx.wear.protolayout.ProtoLayoutScope.RendererCapability.PENDING_INTENT_ACTION
import androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId
import androidx.wear.protolayout.ResourceBuilders.AndroidLottieResourceByResId
import androidx.wear.protolayout.ResourceBuilders.ImageResource
import androidx.wear.protolayout.ResourceBuilders.InlineImageResource
import androidx.wear.protolayout.expression.VersionBuilders
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProtoLayoutScopeTest {
    @Test
    public fun resourcesRegistered_collected() {
        val scope = ProtoLayoutScope()
        val id = "1234"
        val image =
            ImageResource.Builder()
                .setAndroidResourceByResId(
                    AndroidImageResourceByResId.Builder().setResourceId(1234).build()
                )
                .build()

        scope.registerResource(id, image)
        val collectedResources = scope.collectResources()
        val resourcesList = collectedResources.idToImageMapping

        assertThat(scope.hasResources()).isTrue()
        assertThat(resourcesList).hasSize(1)
        assertThat(resourcesList).containsKey(id)
        assertThat(resourcesList[id]!!.toProto()).isEqualTo(image.toProto())
    }

    @Test
    public fun multipleSameResourcesRegistered_versionStaysTheSame() {
        val scope1 = ProtoLayoutScope()
        val scope2 = ProtoLayoutScope()
        val image1 =
            ImageResource.Builder()
                .setAndroidResourceByResId(
                    AndroidImageResourceByResId.Builder().setResourceId(1234).build()
                )
                .build()
        val image2 =
            ImageResource.Builder()
                .setAndroidLottieResourceByResId(
                    AndroidLottieResourceByResId.Builder().setRawResourceId(1234).build()
                )
                .setInlineResource(
                    InlineImageResource.Builder().setData(byteArrayOf(1, 2, 3, 4)).build()
                )
                .build()

        scope1.registerResource("1", image1)
        scope1.registerResource("2", image2)
        scope2.registerResource("2", image2)
        scope2.registerResource("1", image1)

        assertThat(scope1.collectResources().version).isEqualTo(scope2.collectResources().version)
    }

    @Test
    public fun multipleSameResourcesRegistered_withDifferentId_versionIsDifferent() {
        val scope1 = ProtoLayoutScope()
        val scope2 = ProtoLayoutScope()
        val image1 =
            ImageResource.Builder()
                .setAndroidResourceByResId(
                    AndroidImageResourceByResId.Builder().setResourceId(1234).build()
                )
                .build()
        val image2 =
            ImageResource.Builder()
                .setAndroidLottieResourceByResId(
                    AndroidLottieResourceByResId.Builder().setRawResourceId(1234).build()
                )
                .setInlineResource(
                    InlineImageResource.Builder().setData(byteArrayOf(1, 2, 3, 4)).build()
                )
                .build()

        scope1.registerResource("1", image1)
        scope1.registerResource("2", image2)
        scope2.registerResource("11", image1)
        scope2.registerResource("2", image2)

        assertThat(scope1.collectResources().version)
            .isNotEqualTo(scope2.collectResources().version)
    }

    @Test
    public fun pendingIntentRegistered_collected() {
        val scope = ProtoLayoutScope()
        val id = "test"
        val intent = PendingIntent.getActivity(getApplicationContext(), 1, Intent(), 1)

        scope.registerPendingIntent(id, intent)
        val intentsBundle = scope.collectPendingIntents()

        assertThat(BundleCompat.getParcelable(intentsBundle, id, PendingIntent::class.java))
            .isEqualTo(intent)
    }

    @Test
    public fun twoPendingIntents_registerToSameId_throws() {
        val scope = ProtoLayoutScope()
        val id = "test"
        val intent = PendingIntent.getActivity(getApplicationContext(), 1, Intent(), 1)
        scope.registerPendingIntent(id, intent)
        val secondIntent = PendingIntent.getActivity(getApplicationContext(), 2, Intent(), 1)

        assertThrows(IllegalArgumentException::class.java) {
            scope.registerPendingIntent(id, secondIntent)
        }
    }

    @Test
    public fun hasCapability_withVersionBelowMinimum_returnsFalse() {
        val scopeWithSchema1300 =
            ProtoLayoutScope(
                VersionBuilders.VersionInfo.Builder().setMajor(1).setMinor(300).build()
            )

        assertThat(scopeWithSchema1300.hasCapability(LOTTIE_COLOR_FOR_SLOT)).isFalse()
        assertThat(scopeWithSchema1300.hasCapability(PENDING_INTENT_ACTION)).isFalse()
    }

    @Test
    public fun hasCapability_withVersionMeetsMinimum_returnsTrue() {
        val scopeWithSchema1600 =
            ProtoLayoutScope(
                VersionBuilders.VersionInfo.Builder().setMajor(1).setMinor(600).build()
            )

        assertThat(scopeWithSchema1600.hasCapability(LOTTIE_COLOR_FOR_SLOT)).isTrue()
        assertThat(scopeWithSchema1600.hasCapability(PENDING_INTENT_ACTION)).isTrue()
    }
}
