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

package androidx.credentials.registry.digitalcredentials.openid4vci.filter

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
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
        val child1 = AllowedIssuers(setOf("issuer"))
        val child2 = AllowedConfigurationIds(setOf("config"))
        val filter = AllOf(listOf(child1, child2))
        assertThat(filter.filters).containsExactly(child1, child2).inOrder()
        assertThat(filter.asJson().toString())
            .isEqualTo(
                """{"And":{"filters":[{"AllowedIssuers":{"issuers":["issuer"]}},{"AllowedConfigurationIds":{"configuration_ids":["config"]}}]}}"""
            )
    }

    @Test
    fun testAnyOfFilter() {
        val child1 = AllowedIssuers(setOf("issuer"))
        val child2 = AllowedConfigurationIds(setOf("config"))
        val filter = AnyOf(listOf(child1, child2))
        assertThat(filter.filters).containsExactly(child1, child2).inOrder()
        assertThat(filter.asJson().toString())
            .isEqualTo(
                """{"Or":{"filters":[{"AllowedIssuers":{"issuers":["issuer"]}},{"AllowedConfigurationIds":{"configuration_ids":["config"]}}]}}"""
            )
    }

    @Test
    fun testNotFilter() {
        val inner = AllowedIssuers(setOf("issuer"))
        val filter = Not(inner)
        assertThat(filter.filter).isEqualTo(inner)
        assertThat(filter.asJson().toString())
            .isEqualTo("""{"Not":{"filter":{"AllowedIssuers":{"issuers":["issuer"]}}}}""")
    }

    @Test
    fun testAllowedIssuers() {
        val filter = AllowedIssuers(setOf("issuer1", "issuer2"))
        assertThat(filter.issuers).containsExactly("issuer1", "issuer2")
        assertThat(filter.asJson().toString())
            .isEqualTo("""{"AllowedIssuers":{"issuers":["issuer1","issuer2"]}}""")
    }

    @Test
    fun testAllowedConfigurationIds() {
        val filter = AllowedConfigurationIds(setOf("config1", "config2"))
        assertThat(filter.configurationIds).containsExactly("config1", "config2")
        assertThat(filter.asJson().toString())
            .isEqualTo(
                """{"AllowedConfigurationIds":{"configuration_ids":["config1","config2"]}}"""
            )
    }

    @Test
    fun testAllowedMdocDoctypes() {
        val filter = AllowedMdocDoctypes(setOf("doctype1", "doctype2"))
        assertThat(filter.doctypes).containsExactly("doctype1", "doctype2")
        assertThat(filter.asJson().toString())
            .isEqualTo("""{"AllowedMdocDoctypes":{"doctypes":["doctype1","doctype2"]}}""")
    }

    @Test
    fun testAllowedSdJwtVcts() {
        val filter = AllowedSdJwtVcts(setOf("vct1", "vct2"))
        assertThat(filter.vcts).containsExactly("vct1", "vct2")
        assertThat(filter.asJson().toString())
            .isEqualTo("""{"AllowedSdJwtVcts":{"vcts":["vct1","vct2"]}}""")
    }

    @Test
    fun testAndOperator_flattensSymmetrically() {
        val a = AllowedIssuers(setOf("a"))
        val b = AllowedIssuers(setOf("b"))
        val c = AllowedIssuers(setOf("c"))
        val d = AllowedIssuers(setOf("d"))

        // (a and b) and c
        val filter1 = (a and b) and c
        // a and (b and c)
        val filter2 = a and (b and c)
        // (a and b) and (c and d)
        val filter3 = (a and b) and (c and d)

        assertThat(filter1).isInstanceOf(AllOf::class.java)
        assertThat((filter1 as AllOf).filters).hasSize(3)
        assertThat(filter1.filters).containsExactly(a, b, c).inOrder()

        assertThat(filter2).isInstanceOf(AllOf::class.java)
        assertThat((filter2 as AllOf).filters).hasSize(3)
        assertThat(filter2.filters).containsExactly(a, b, c).inOrder()
        assertThat(filter1).isEqualTo(filter2)

        assertThat(filter3).isInstanceOf(AllOf::class.java)
        assertThat((filter3 as AllOf).filters).containsExactly(a, b, c, d).inOrder()
    }

    @Test
    fun testOrOperator_flattensSymmetrically() {
        val a = AllowedIssuers(setOf("a"))
        val b = AllowedIssuers(setOf("b"))
        val c = AllowedIssuers(setOf("c"))
        val d = AllowedIssuers(setOf("d"))

        // (a or b) or c
        val filter1 = (a or b) or c
        // a or (b or c)
        val filter2 = a or (b or c)
        // (a or b) or (c or d)
        val filter3 = (a or b) or (c or d)

        assertThat(filter1).isInstanceOf(AnyOf::class.java)
        assertThat((filter1 as AnyOf).filters).hasSize(3)
        assertThat(filter1.filters).containsExactly(a, b, c).inOrder()

        assertThat(filter2).isInstanceOf(AnyOf::class.java)
        assertThat((filter2 as AnyOf).filters).hasSize(3)
        assertThat(filter2.filters).containsExactly(a, b, c).inOrder()
        assertThat(filter1).isEqualTo(filter2)

        assertThat(filter3).isInstanceOf(AnyOf::class.java)
        assertThat((filter3 as AnyOf).filters).containsExactly(a, b, c, d).inOrder()
    }

    @Test
    fun testNotOperator_doubleNegationUnwraps() {
        val leaf = AllowedIssuers(setOf("issuer1"))
        val negated = !leaf
        assertThat(negated).isInstanceOf(Not::class.java)
        assertThat((negated as Not).filter).isSameInstanceAs(leaf)

        val doubleNegated = !negated
        assertThat(doubleNegated).isSameInstanceAs(leaf)
    }

    @Test
    fun testMixedOperators_preservesNestingAcrossDifferentCombinators() {
        val issuer = AllowedIssuers(setOf("issuer1"))
        val mdoc = AllowedMdocDoctypes(setOf("org.iso.18013.5.1.mDL"))
        val sdJwt = AllowedSdJwtVcts(setOf("urn:eudi:pid:1"))
        val excludedConfig = AllowedConfigurationIds(setOf("deprecated_config"))

        val combined = issuer and (mdoc or sdJwt) and !excludedConfig
        assertThat(combined).isInstanceOf(AllOf::class.java)
        val allOf = combined as AllOf
        assertThat(allOf.filters)
            .containsExactly(issuer, AnyOf(listOf(mdoc, sdJwt)), Not(excludedConfig))
            .inOrder()
        assertThat(allOf.asJson().toString())
            .isEqualTo(
                """{"And":{"filters":[""" +
                    """{"AllowedIssuers":{"issuers":["issuer1"]}},""" +
                    """{"Or":{"filters":[""" +
                    """{"AllowedMdocDoctypes":{"doctypes":["org.iso.18013.5.1.mDL"]}},""" +
                    """{"AllowedSdJwtVcts":{"vcts":["urn:eudi:pid:1"]}}]}},""" +
                    """{"Not":{"filter":{"AllowedConfigurationIds":{"configuration_ids":["deprecated_config"]}}}}]}}"""
            )
    }

    @Test
    fun testValidation_emptyAndBlankInputs_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) { AllOf(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { AnyOf(emptyList()) }

        assertThrows(IllegalArgumentException::class.java) { AllowedIssuers(emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { AllowedIssuers(setOf("")) }
        assertThrows(IllegalArgumentException::class.java) { AllowedIssuers(setOf("issuer1", " ")) }

        assertThrows(IllegalArgumentException::class.java) { AllowedConfigurationIds(emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { AllowedConfigurationIds(setOf("")) }
        assertThrows(IllegalArgumentException::class.java) {
            AllowedConfigurationIds(setOf("config1", "  "))
        }

        assertThrows(IllegalArgumentException::class.java) { AllowedMdocDoctypes(emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { AllowedMdocDoctypes(setOf("")) }
        assertThrows(IllegalArgumentException::class.java) {
            AllowedMdocDoctypes(setOf("doctype1", "\t"))
        }

        assertThrows(IllegalArgumentException::class.java) { AllowedSdJwtVcts(emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { AllowedSdJwtVcts(setOf("")) }
        assertThrows(IllegalArgumentException::class.java) {
            AllowedSdJwtVcts(setOf("vct1", "\n"))
        }
    }

    @Test
    fun testEqualsAndHashCode() {
        assertThat(PassFilter()).isEqualTo(PassFilter())
        assertThat(PassFilter().hashCode()).isEqualTo(PassFilter().hashCode())
        assertThat(PassFilter()).isNotEqualTo(AllowedIssuers(setOf("i1")))

        val issuersA = AllowedIssuers(setOf("i1", "i2"))
        val issuersB = AllowedIssuers(setOf("i2", "i1"))
        assertThat(issuersA).isEqualTo(issuersB)
        assertThat(issuersA.hashCode()).isEqualTo(issuersB.hashCode())
        assertThat(issuersA).isNotEqualTo(AllowedIssuers(setOf("i1")))
        assertThat(issuersA).isNotEqualTo(AllowedConfigurationIds(setOf("i1", "i2")))

        val configsA = AllowedConfigurationIds(setOf("c1", "c2"))
        val configsB = AllowedConfigurationIds(setOf("c2", "c1"))
        assertThat(configsA).isEqualTo(configsB)
        assertThat(configsA.hashCode()).isEqualTo(configsB.hashCode())
        assertThat(configsA).isNotEqualTo(AllowedConfigurationIds(setOf("c1")))

        val doctypesA = AllowedMdocDoctypes(setOf("d1", "d2"))
        val doctypesB = AllowedMdocDoctypes(setOf("d2", "d1"))
        assertThat(doctypesA).isEqualTo(doctypesB)
        assertThat(doctypesA.hashCode()).isEqualTo(doctypesB.hashCode())
        assertThat(doctypesA).isNotEqualTo(AllowedMdocDoctypes(setOf("d1")))

        val vctsA = AllowedSdJwtVcts(setOf("v1", "v2"))
        val vctsB = AllowedSdJwtVcts(setOf("v2", "v1"))
        assertThat(vctsA).isEqualTo(vctsB)
        assertThat(vctsA.hashCode()).isEqualTo(vctsB.hashCode())
        assertThat(vctsA).isNotEqualTo(AllowedSdJwtVcts(setOf("v1")))

        val notA = Not(issuersA)
        val notB = Not(issuersB)
        assertThat(notA).isEqualTo(notB)
        assertThat(notA.hashCode()).isEqualTo(notB.hashCode())
        assertThat(notA).isNotEqualTo(Not(configsA))

        val allOfA = AllOf(listOf(issuersA, configsA))
        val allOfB = AllOf(listOf(issuersB, configsB))
        assertThat(allOfA).isEqualTo(allOfB)
        assertThat(allOfA.hashCode()).isEqualTo(allOfB.hashCode())
        assertThat(allOfA).isNotEqualTo(AllOf(listOf(configsA, issuersA)))
        assertThat(allOfA).isNotEqualTo(AnyOf(listOf(issuersA, configsA)))

        val anyOfA = AnyOf(listOf(doctypesA, vctsA))
        val anyOfB = AnyOf(listOf(doctypesB, vctsB))
        assertThat(anyOfA).isEqualTo(anyOfB)
        assertThat(anyOfA.hashCode()).isEqualTo(anyOfB.hashCode())
        assertThat(anyOfA).isNotEqualTo(AnyOf(listOf(vctsA, doctypesA)))
        assertThat(anyOfA).isNotEqualTo(AllOf(listOf(doctypesA, vctsA)))
    }
}
