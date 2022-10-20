/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.measurement

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class DeletionRequestTest {
    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun testToString() {
        val now = Instant.now()
        val result = "DeletionRequest { DeletionMode=DELETION_MODE_ALL, DomainUris=[www.abc.com]" +
            ", OriginUris=[www.xyz.com], Start=$now, End=$now, " +
            "MatchBehavior=MATCH_BEHAVIOR_DELETE }"

        val deletionRequest = DeletionRequest(
            DeletionRequest.DeletionMode.DELETION_MODE_ALL,
            listOf(Uri.parse("www.abc.com")),
            listOf(Uri.parse("www.xyz.com")),
            now,
            now,
            DeletionRequest.MatchBehavior.MATCH_BEHAVIOR_DELETE
        )
        Truth.assertThat(deletionRequest.toString()).isEqualTo(result)
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun testEquals() {
        val now = Instant.now()
        val deletionRequest1 = DeletionRequest(
            DeletionRequest.DeletionMode.DELETION_MODE_ALL,
            listOf(Uri.parse("www.abc.com")),
            listOf(Uri.parse("www.xyz.com")),
            now,
            now,
            DeletionRequest.MatchBehavior.MATCH_BEHAVIOR_DELETE
        )
        val deletionRequest2 = DeletionRequest.Builder(
            DeletionRequest.DeletionMode.DELETION_MODE_ALL,
            now,
            now,
            DeletionRequest.MatchBehavior.MATCH_BEHAVIOR_DELETE)
            .setDomainUris(listOf(Uri.parse("www.abc.com")))
            .setOriginUris(listOf(Uri.parse("www.xyz.com")))
            .build()
        Truth.assertThat(deletionRequest1 == deletionRequest2).isTrue()
    }
}