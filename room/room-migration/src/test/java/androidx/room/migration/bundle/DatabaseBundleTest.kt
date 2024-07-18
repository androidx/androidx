/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.migration.bundle

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DatabaseBundleTest {

    @Test
    fun buildCreateQueries_noFts() {
        val entity1 = EntityBundle("e1", "sq1",
                listOf(createFieldBundle("foo1"), createFieldBundle("bar")),
                PrimaryKeyBundle(false, listOf("foo1")),
                emptyList(),
                emptyList())
        val entity2 = EntityBundle("e2", "sq2",
            listOf(createFieldBundle("foo2"), createFieldBundle("bar")),
                PrimaryKeyBundle(false, listOf("foo2")),
                emptyList(),
                emptyList())
        val bundle = DatabaseBundle(1, "hash",
            listOf(entity1, entity2), emptyList(),
                emptyList())

        assertThat(bundle.buildCreateQueries(), `is`(listOf("sq1", "sq2")))
    }

    @Test
    fun buildCreateQueries_withFts() {
        val entity1 = EntityBundle("e1", "sq1",
            listOf(createFieldBundle("foo1"), createFieldBundle("bar")),
                PrimaryKeyBundle(false, listOf("foo1")),
                emptyList(),
                emptyList())
        val entity2 = FtsEntityBundle("e2", "sq2",
            listOf(createFieldBundle("foo2"), createFieldBundle("bar")),
                PrimaryKeyBundle(false, listOf("foo2")),
                "FTS4",
                createFtsOptionsBundle(""),
                emptyList())
        val entity3 = EntityBundle("e3", "sq3",
            listOf(createFieldBundle("foo3"), createFieldBundle("bar")),
                PrimaryKeyBundle(false, listOf("foo3")),
                emptyList(),
                emptyList())
        val bundle = DatabaseBundle(1, "hash",
            listOf(entity1, entity2, entity3), emptyList(),
                emptyList())

        assertThat(bundle.buildCreateQueries(), `is`(listOf("sq1", "sq2", "sq3")))
    }

    @Test
    fun buildCreateQueries_withExternalContentFts() {
        val entity1 = EntityBundle("e1", "sq1",
            listOf(createFieldBundle("foo1"), createFieldBundle("bar")),
                PrimaryKeyBundle(false, listOf("foo1")),
                emptyList(),
                emptyList())
        val entity2 = FtsEntityBundle("e2", "sq2",
            listOf(createFieldBundle("foo2"), createFieldBundle("bar")),
                PrimaryKeyBundle(false, listOf("foo2")),
                "FTS4",
                createFtsOptionsBundle("e3"),
            listOf("e2_trig"))
        val entity3 = EntityBundle("e3", "sq3",
        listOf(createFieldBundle("foo3"), createFieldBundle("bar")),
                PrimaryKeyBundle(false, listOf("foo3")),
                emptyList(),
                emptyList())
        val bundle = DatabaseBundle(
            1,
            "hash",
            listOf(entity1, entity2, entity3),
            emptyList(),
            emptyList()
        )

        assertThat(bundle.buildCreateQueries(), `is`(listOf("sq1", "sq3", "sq2", "e2_trig")))
    }

    @Test
    fun schemaEquality_missingView_notEqual() {
        val entity = EntityBundle("e", "sq",
            listOf(createFieldBundle("foo"), createFieldBundle("bar")),
            PrimaryKeyBundle(false, listOf("foo")),
            emptyList(),
            emptyList())
        val view = DatabaseViewBundle("bar", "sq")
        val bundle1 = DatabaseBundle(1, "hash",
            listOf(entity), emptyList(),
            emptyList())
        val bundle2 = DatabaseBundle(1, "hash",
            listOf(entity), listOf(view),
            emptyList())
        assertThat(bundle1.isSchemaEqual(bundle2), `is`(false))
    }

    private fun createFieldBundle(name: String): FieldBundle {
        return FieldBundle("foo", name, "text", false, null)
    }

    private fun createFtsOptionsBundle(contentTableName: String): FtsOptionsBundle {
        return FtsOptionsBundle("", emptyList(), contentTableName,
                "", "", emptyList(), emptyList(), "")
    }
}
