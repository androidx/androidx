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
@SdkSuppress(minSdkVersion = 33)
class DeletionRequestTest {
    @Test
    fun testToString() {
        val now = Instant.now()
        val result = "DeletionRequest { DeletionMode=DELETION_MODE_ALL, " +
            "MatchBehavior=MATCH_BEHAVIOR_DELETE, " +
            "Start=$now, End=$now, DomainUris=[www.abc.com], OriginUris=[www.xyz.com] }"

        val deletionRequest = DeletionRequest(
            DeletionRequest.DELETION_MODE_ALL,
            DeletionRequest.MATCH_BEHAVIOR_DELETE,
            now,
            now,
            listOf(Uri.parse("www.abc.com")),
            listOf(Uri.parse("www.xyz.com")),
        )
        Truth.assertThat(deletionRequest.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val deletionRequest1 = DeletionRequest(
            DeletionRequest.DELETION_MODE_ALL,
            DeletionRequest.MATCH_BEHAVIOR_DELETE,
            Instant.MIN,
            Instant.MAX,
            listOf(Uri.parse("www.abc.com")),
            listOf(Uri.parse("www.xyz.com")))
        val deletionRequest2 = DeletionRequest.Builder(
            deletionMode = DeletionRequest.DELETION_MODE_ALL,
            matchBehavior = DeletionRequest.MATCH_BEHAVIOR_DELETE)
            .setDomainUris(listOf(Uri.parse("www.abc.com")))
            .setOriginUris(listOf(Uri.parse("www.xyz.com")))
            .build()
        Truth.assertThat(deletionRequest1 == deletionRequest2).isTrue()
    }
}
