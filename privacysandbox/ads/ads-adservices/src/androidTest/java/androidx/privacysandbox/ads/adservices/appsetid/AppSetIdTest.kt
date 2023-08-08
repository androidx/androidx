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

package androidx.privacysandbox.ads.adservices.appsetid

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AppSetIdTest {
    @Test
    fun testToString() {
        val result = "AppSetId: id=1234, scope=SCOPE_DEVELOPER"
        val id = AppSetId("1234", AppSetId.SCOPE_DEVELOPER)
        Truth.assertThat(id.toString()).isEqualTo(result)

        val result2 = "AppSetId: id=4321, scope=SCOPE_APP"
        val id2 = AppSetId("4321", AppSetId.SCOPE_APP)
        Truth.assertThat(id2.toString()).isEqualTo(result2)
    }

    @Test
    fun testEquals() {
        val id1 = AppSetId("1234", AppSetId.SCOPE_DEVELOPER)
        val id2 = AppSetId("1234", AppSetId.SCOPE_DEVELOPER)
        Truth.assertThat(id1 == id2).isTrue()

        val id3 = AppSetId("1234", AppSetId.SCOPE_APP)
        Truth.assertThat(id1 == id3).isFalse()
    }

    @Test
    fun testScopeUndefined() {
        assertThrows<IllegalArgumentException> {
            AppSetId("1234", 3 /* Invalid scope */)
        }.hasMessageThat().contains("Scope undefined.")
    }
}
