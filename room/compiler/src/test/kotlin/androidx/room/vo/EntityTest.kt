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

package androidx.room.vo

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

@RunWith(JUnit4::class)
class EntityTest {

    @Test
    fun shouldBeDeletedAfter() {
        val child = createEntity("Child", listOf(
                createForeignKey("NoAction", ForeignKeyAction.NO_ACTION, false),
                createForeignKey("NoActionDeferred", ForeignKeyAction.NO_ACTION, true),
                createForeignKey("Restrict", ForeignKeyAction.RESTRICT, false),
                createForeignKey("RestrictDeferred", ForeignKeyAction.RESTRICT, true),
                createForeignKey("SetNull", ForeignKeyAction.SET_NULL, false),
                createForeignKey("SetNullDeferred", ForeignKeyAction.SET_NULL, true),
                createForeignKey("SetDefault", ForeignKeyAction.SET_DEFAULT, false),
                createForeignKey("SetDefaultDeferred", ForeignKeyAction.SET_DEFAULT, true),
                createForeignKey("Cascade", ForeignKeyAction.CASCADE, false),
                createForeignKey("CascadeDeferred", ForeignKeyAction.CASCADE, true)))
        val noAction = createEntity("NoAction")
        val noActionDeferred = createEntity("NoActionDeferred")
        val restrict = createEntity("Restrict")
        val restrictDeferred = createEntity("RestrictDeferred")
        val setNull = createEntity("SetNull")
        val setNullDeferred = createEntity("SetNullDeferred")
        val setDefault = createEntity("SetDefault")
        val setDefaultDeferred = createEntity("SetDefaultDeferred")
        val cascade = createEntity("Cascade")
        val cascadeDeferred = createEntity("CascadeDeferred")
        val irrelevant = createEntity("Irrelevant")
        assertThat(child.shouldBeDeletedAfter(noAction), `is`(true))
        assertThat(child.shouldBeDeletedAfter(noActionDeferred), `is`(false))
        assertThat(child.shouldBeDeletedAfter(restrict), `is`(true))
        assertThat(child.shouldBeDeletedAfter(restrictDeferred), `is`(true))
        assertThat(child.shouldBeDeletedAfter(setNull), `is`(false))
        assertThat(child.shouldBeDeletedAfter(setNullDeferred), `is`(false))
        assertThat(child.shouldBeDeletedAfter(setDefault), `is`(false))
        assertThat(child.shouldBeDeletedAfter(setDefaultDeferred), `is`(false))
        assertThat(child.shouldBeDeletedAfter(cascade), `is`(false))
        assertThat(child.shouldBeDeletedAfter(cascadeDeferred), `is`(false))
        assertThat(child.shouldBeDeletedAfter(irrelevant), `is`(false))
    }

    private fun createEntity(
            tableName: String,
            foreignKeys: List<ForeignKey> = emptyList()): Entity {
        return Entity(
                element = mock(TypeElement::class.java),
                tableName = tableName,
                type = mock(DeclaredType::class.java),
                fields = emptyList(),
                embeddedFields = emptyList(),
                primaryKey = PrimaryKey(mock(Element::class.java), emptyList(), false),
                indices = emptyList(),
                foreignKeys = foreignKeys,
                constructor = Constructor(mock(ExecutableElement::class.java), emptyList()))
    }

    private fun createForeignKey(
            parentTable: String,
            onDelete: ForeignKeyAction,
            deferred: Boolean): ForeignKey {
        return ForeignKey(
                parentTable = parentTable,
                parentColumns = emptyList(),
                childFields = emptyList(),
                onDelete = onDelete,
                onUpdate = ForeignKeyAction.NO_ACTION,
                deferred = deferred)
    }
}
