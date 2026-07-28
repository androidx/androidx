/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.registry.digitalcredentials.openid4vci

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class OpenId4VciFilterTest {
    @Test
    fun testPassFilter() {
        val filter = PassFilter()
        assertThat(filter.asJson().toString()).isEqualTo("""{"Pass":{}}""")
    }

    @Test
    fun testAllOfFilter() {
        val filter =
            AllOf(listOf(AllowedIssuers(setOf("issuer")), AllowedConfigurationIds(setOf("config"))))
        assertThat(filter.asJson().toString())
            .isEqualTo(
                """{"And":{"filters":[{"AllowedIssuers":{"issuers":["issuer"]}},{"AllowedConfigurationIds":{"configuration_ids":["config"]}}]}}"""
            )
    }

    @Test
    fun testAnyOfFilter() {
        val filter =
            AnyOf(listOf(AllowedIssuers(setOf("issuer")), AllowedConfigurationIds(setOf("config"))))
        assertThat(filter.asJson().toString())
            .isEqualTo(
                """{"Or":{"filters":[{"AllowedIssuers":{"issuers":["issuer"]}},{"AllowedConfigurationIds":{"configuration_ids":["config"]}}]}}"""
            )
    }

    @Test
    fun testNotFilter() {
        val filter = Not(AllowedIssuers(setOf("issuer")))
        assertThat(filter.asJson().toString())
            .isEqualTo("""{"Not":{"filter":{"AllowedIssuers":{"issuers":["issuer"]}}}}""")
    }

    @Test
    fun testAllowedIssuers() {
        val filter = AllowedIssuers(setOf("issuer1", "issuer2"))
        assertThat(filter.asJson().toString())
            .isEqualTo("""{"AllowedIssuers":{"issuers":["issuer1","issuer2"]}}""")
    }

    @Test
    fun testAllowedConfigurationIds() {
        val filter = AllowedConfigurationIds(setOf("config1", "config2"))
        assertThat(filter.asJson().toString())
            .isEqualTo(
                """{"AllowedConfigurationIds":{"configuration_ids":["config1","config2"]}}"""
            )
    }

    @Test
    fun testAllowedMdocDoctypes() {
        val filter = AllowedMdocDoctypes(setOf("doctype1", "doctype2"))
        assertThat(filter.asJson().toString())
            .isEqualTo("""{"AllowedMdocDoctypes":{"doctypes":["doctype1","doctype2"]}}""")
    }

    @Test
    fun testAllowedSdJwtVcts() {
        val filter = AllowedSdJwtVcts(setOf("vct1", "vct2"))
        assertThat(filter.asJson().toString())
            .isEqualTo("""{"AllowedSdJwtVcts":{"vcts":["vct1","vct2"]}}""")
    }

    @Test
    fun testAndOperator_flattensSymmetrically() {
        val a = AllowedIssuers(setOf("a"))
        val b = AllowedIssuers(setOf("b"))
        val c = AllowedIssuers(setOf("c"))

        // (a and b) and c
        val filter1 = (a and b) and c
        // a and (b and c)
        val filter2 = a and (b and c)

        assertThat(filter1).isInstanceOf(AllOf::class.java)
        assertThat((filter1 as AllOf).filters).hasSize(3)
        assertThat(filter1.filters).containsExactly(a, b, c).inOrder()

        assertThat(filter2).isInstanceOf(AllOf::class.java)
        assertThat((filter2 as AllOf).filters).hasSize(3)
        assertThat(filter2.filters).containsExactly(a, b, c).inOrder()
    }

    @Test
    fun testOrOperator_flattensSymmetrically() {
        val a = AllowedIssuers(setOf("a"))
        val b = AllowedIssuers(setOf("b"))
        val c = AllowedIssuers(setOf("c"))

        // (a or b) or c
        val filter1 = (a or b) or c
        // a or (b or c)
        val filter2 = a or (b or c)

        assertThat(filter1).isInstanceOf(AnyOf::class.java)
        assertThat((filter1 as AnyOf).filters).hasSize(3)
        assertThat(filter1.filters).containsExactly(a, b, c).inOrder()

        assertThat(filter2).isInstanceOf(AnyOf::class.java)
        assertThat((filter2 as AnyOf).filters).hasSize(3)
        assertThat(filter2.filters).containsExactly(a, b, c).inOrder()
    }
}
