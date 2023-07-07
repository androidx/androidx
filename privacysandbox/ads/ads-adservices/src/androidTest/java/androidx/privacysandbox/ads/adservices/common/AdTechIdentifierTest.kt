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

package androidx.privacysandbox.ads.adservices.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AdTechIdentifierTest {
    private val identifier = "ad-tech-identifier"

    @Test
    fun testToString() {
        val result = "$identifier"
        val request = AdTechIdentifier(identifier)
        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val id1 = AdTechIdentifier(identifier)
        var id2 = AdTechIdentifier("ad-tech-identifier")
        Truth.assertThat(id1 == id2).isTrue()
    }
}
