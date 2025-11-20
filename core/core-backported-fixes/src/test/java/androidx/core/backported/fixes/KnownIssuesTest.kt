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

package androidx.core.backported.fixes

import com.google.common.truth.Truth.assertThat
import kotlin.collections.map
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberProperties
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests for [KnownIssue] */
@RunWith(JUnit4::class)
class KnownIssuesTest {

    @Test
    fun allKnownIssues_IdsAreUnique() {
        val ids = getAllKnownIssues().map { ki -> ki.id }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun allKnownIssues_AliasesAreUnique() {
        val aliases = getAllKnownIssues().map { ki -> ki.alias }
        assertThat(aliases).containsNoDuplicates()
    }

    private fun getAllKnownIssues(): Iterable<KnownIssue> {
        val coi = KnownIssues::class.companionObjectInstance as KnownIssues.Companion
        val props = getAllKnownIssueProperties()
        return props.map { p -> getKiValue(p, coi) }
    }

    private fun getKiValue(p: KProperty1<out Any, *>, coi: KnownIssues.Companion): KnownIssue =
        p.getter.call(coi) as KnownIssue

    private fun getAllKnownIssueProperties(): List<KProperty1<out Any, *>> {
        val coi = KnownIssues::class.companionObjectInstance as KnownIssues.Companion
        val props = coi::class.memberProperties.filter { p -> p.name.startsWith("KI_") }
        return props
    }

    @Test
    fun allKnownIssues_nameMatchesId() {
        val coi = KnownIssues::class.companionObjectInstance as KnownIssues.Companion
        val props = getAllKnownIssueProperties()
        props.forEach {
            val ki = getKiValue(it, coi)
            assertThat(it.name).isEqualTo("KI_${ki.id}")
        }
    }
}
