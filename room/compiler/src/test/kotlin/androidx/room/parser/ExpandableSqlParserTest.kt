/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.parser

import androidx.room.parser.expansion.ExpandableSection
import androidx.room.parser.expansion.ExpandableSqlParser
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExpandableSqlParserTest {

    @Test
    fun multipleQueries() {
        assertErrors(
            "SELECT * FROM users; SELECT * FROM books;",
            ParserErrors.NOT_ONE_QUERY
        )
    }

    @Test
    fun empty() {
        assertErrors("", ParserErrors.NOT_ONE_QUERY)
    }

    @Test
    fun deleteQuery() {
        val parsed = ExpandableSqlParser.parse("DELETE FROM users where id > 3")
        assertThat(parsed.errors, `is`(emptyList()))
        assertThat(parsed.type, `is`(QueryType.DELETE))
    }

    @Test
    fun badDeleteQuery() {
        assertErrors(
            "delete from user where mAge >= :min && mAge <= :max",
            "no viable alternative at input 'delete from user where mAge >= :min &&'"
        )
    }

    @Test
    fun updateQuery() {
        val parsed = ExpandableSqlParser.parse("UPDATE users set name = :name where id = :id")
        assertThat(parsed.errors, `is`(emptyList()))
        assertThat(parsed.type, `is`(QueryType.UPDATE))
    }

    @Test
    fun insertQuery() {
        val parsed = ExpandableSqlParser.parse(
            "INSERT OR REPLACE INTO notes (id, content) VALUES (:id, :content)"
        )
        assertThat(parsed.errors, `is`(emptyList()))
        assertThat(parsed.type, `is`(QueryType.INSERT))
    }

    @Test
    fun upsertQuery() {
        val parsed = ExpandableSqlParser.parse(
            "INSERT INTO notes (id, content) VALUES (:id, :content) " +
                "ON CONFLICT (id) DO UPDATE SET content = excluded.content, " +
                "revision = revision + 1, modifiedTime = strftime('%s','now')"
        )
        assertThat(parsed.errors, `is`(emptyList()))
        assertThat(parsed.type, `is`(QueryType.INSERT))
    }

    @Test
    fun explain() {
        assertErrors(
            "EXPLAIN QUERY PLAN SELECT * FROM users",
            ParserErrors.invalidQueryType(QueryType.EXPLAIN)
        )
    }

    @Test
    fun validColumnNames() {
        listOf(
            "f", "fo", "f2", "f 2", "foo_2", "foo-2", "_", "foo bar baz",
            "foo 2 baz", "_baz", "fooBar", "2", "*", "foo*2", "dsa$", "\$fsa",
            "-bar", "şoöğüı"
        ).forEach {
            assertThat("name: $it", SqlParser.isValidIdentifier(it), `is`(true))
        }
    }

    @Test
    fun invalidColumnNames() {
        listOf("", " ", "fd`a`", "f`a", "`a", "\"foo bar\"", "\"", "`").forEach {
            assertThat("name: $it", SqlParser.isValidIdentifier(it), `is`(false))
        }
    }

    @Test
    fun projection() {
        val query = ExpandableSqlParser.parse(
            "SELECT * FROM User WHERE teamId IN " +
                "(SELECT * FROM Team WHERE active != 0)"
        )
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.projections.size, `is`(1))
        assertThat(query.projections.first().section.text, `is`(equalTo("*")))
    }

    @Test
    fun projection_tableName() {
        val query = ExpandableSqlParser.parse("SELECT User.* FROM User")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.projections.size, `is`(1))
        assertThat(query.projections.first().section.text, `is`(equalTo("User.*")))
    }

    @Test
    fun projection_columnNames() {
        val query = ExpandableSqlParser.parse("SELECT `id` AS `a_id`, name FROM User")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.projections.size, `is`(0))
        assertThat(query.sections.size, `is`(1))
        assertThat(query.sections[0], `is`(instanceOf(ExpandableSection.Text::class.java)))
        assertThat(query.explicitColumns.size, `is`(2))
        assertThat(query.explicitColumns[0], `is`(equalTo("a_id")))
        assertThat(query.explicitColumns[1], `is`(equalTo("name")))
    }

    @Test
    fun projection_containsNewline() {
        val query = ExpandableSqlParser.parse("SELECT User   \n.   \n* FROM User")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.projections.size, `is`(1))
        assertThat(query.projections.first().section.text, `is`(equalTo("User   \n.   \n*")))
        assertThat(query.sections.size, `is`(3))
        assertThat(query.sections[0], `is`(instanceOf(ExpandableSection.Text::class.java)))
        assertThat(
            query.sections[1],
            `is`(instanceOf(ExpandableSection.Projection.Table::class.java))
        )
        assertThat(query.sections[2], `is`(instanceOf(ExpandableSection.Text::class.java)))
    }

    @Test
    fun projection_containsExpression() {
        val query = ExpandableSqlParser.parse("SELECT firstName || lastName AS fullName FROM User")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.projections.size, `is`(0))
        assertThat(query.sections.size, `is`(1))
        assertThat(query.sections[0], `is`(instanceOf(ExpandableSection.Text::class.java)))
        assertThat(query.explicitColumns.size, `is`(1))
        assertThat(query.explicitColumns[0], `is`(equalTo("fullName")))
    }

    @Test
    fun projection_containsParameter() {
        val query = ExpandableSqlParser.parse("SELECT firstName || :suffix AS nickname FROM User")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.projections.size, `is`(0))
        assertThat(query.sections.size, `is`(3))
        assertThat(query.sections[0], `is`(instanceOf(ExpandableSection.Text::class.java)))
        assertThat(query.sections[1], `is`(instanceOf(ExpandableSection.BindVar::class.java)))
        assertThat(
            (query.sections[1] as ExpandableSection.BindVar).symbol,
            `is`(equalTo(":suffix"))
        )
        assertThat(query.sections[2], `is`(instanceOf(ExpandableSection.Text::class.java)))
        assertThat(query.explicitColumns.size, `is`(1))
        assertThat(query.explicitColumns[0], `is`(equalTo("nickname")))
    }

    @Test
    fun extractTableNames() {
        assertThat(
            ExpandableSqlParser.parse("select * from users").tables,
            `is`(setOf(Table("users", "users")))
        )
        assertThat(
            ExpandableSqlParser.parse("select * from users as ux").tables,
            `is`(setOf(Table("users", "ux")))
        )
        assertThat(
            ExpandableSqlParser.parse("select * from (select * from books)").tables,
            `is`(setOf(Table("books", "books")))
        )
        assertThat(
            ExpandableSqlParser.parse("select x.id from (select * from books) as x").tables,
            `is`(setOf(Table("books", "books")))
        )
    }

    @Test
    fun unescapeTableNames() {
        assertThat(
            ExpandableSqlParser.parse("select * from `users`").tables,
            `is`(setOf(Table("users", "users")))
        )
        assertThat(
            ExpandableSqlParser.parse("select * from \"users\"").tables,
            `is`(setOf(Table("users", "users")))
        )
        assertThat(
            ExpandableSqlParser.parse("select * from 'users'").tables,
            `is`(setOf(Table("users", "users")))
        )
    }

    @Test
    fun tablePrefixInInsert_set() {
        // this is an invalid query, b/64539805
        val query = ExpandableSqlParser.parse("UPDATE trips SET trips.title=:title")
        assertThat(query.errors, not(emptyList()))
    }

    @Test
    fun tablePrefixInInsert_where() {
        val query = ExpandableSqlParser.parse("UPDATE trips SET title=:title WHERE trips.id=:id")
        assertThat(query.errors, `is`(emptyList()))
    }

    @Test
    fun tablePrefixInSelect_projection() {
        val query = ExpandableSqlParser.parse("SELECT a.name, b.last_name from user a, book b")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(
            query.tables,
            `is`(
                setOf(
                    Table("user", "a"),
                    Table("book", "b")
                )
            )
        )
    }

    @Test
    fun tablePrefixInSelect_where() {
        val query = ExpandableSqlParser.parse(
            "SELECT a.name, b.last_name from user a, book b" +
                " WHERE a.name = b.name"
        )
        assertThat(query.errors, `is`(emptyList()))
        assertThat(
            query.tables,
            `is`(
                setOf(
                    Table("user", "a"),
                    Table("book", "b")
                )
            )
        )
    }

    @Test
    fun unicodeInIdentifiers() {
        val query = ExpandableSqlParser.parse("SELECT 名, 色 FROM 猫")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.tables, `is`(setOf(Table("猫", "猫"))))
    }

    @Test
    fun rowValue_where() {
        val query =
            ExpandableSqlParser.parse("SELECT * FROM notes WHERE (id, content) > (:id, :content)")
        assertThat(query.errors, `is`(emptyList()))
    }

    @Test
    fun findBindVariables() {
        assertVariables("select * from users")
        assertVariables("select * from users where name like ?", "?")
        assertVariables("select * from users where name like :name", ":name")
        assertVariables("select * from users where name like ?2", "?2")
        assertVariables("select * from users where name like ?2 OR name LIKE ?1", "?2", "?1")
        assertVariables("select * from users where name like @a", "@a")
        assertVariables("select * from users where name like \$a", "\$a")
    }

    @Test
    fun indexedVariablesError() {
        assertErrors(
            "select * from users where name like ?",
            ParserErrors.ANONYMOUS_BIND_ARGUMENT
        )
        assertErrors(
            "select * from users where name like ? or last_name like ?",
            ParserErrors.ANONYMOUS_BIND_ARGUMENT
        )
        assertErrors(
            "select * from users where name like ?1",
            ParserErrors.cannotUseVariableIndices("?1", 36)
        )
    }

    @Test
    fun splitSections() {
        assertSections(
            "select * from users where name like ?",
            ExpandableSection.Text("select "),
            ExpandableSection.Projection.All,
            ExpandableSection.Text(" from users where name like "),
            ExpandableSection.BindVar("?")
        )

        assertSections(
            "select * from users where name like :name AND last_name like :lastName",
            ExpandableSection.Text("select "),
            ExpandableSection.Projection.All,
            ExpandableSection.Text(" from users where name like "),
            ExpandableSection.BindVar(":name"),
            ExpandableSection.Text(" AND last_name like "),
            ExpandableSection.BindVar(":lastName")
        )

        assertSections(
            "select * from users where name \nlike :name AND last_name like :lastName",
            ExpandableSection.Text("select "),
            ExpandableSection.Projection.All,
            ExpandableSection.Text(" from users where name "),
            ExpandableSection.Newline,
            ExpandableSection.Text("like "),
            ExpandableSection.BindVar(":name"),
            ExpandableSection.Text(" AND last_name like "),
            ExpandableSection.BindVar(":lastName")
        )

        assertSections(
            "select * from users where name like :name \nAND last_name like :lastName",
            ExpandableSection.Text("select "),
            ExpandableSection.Projection.All,
            ExpandableSection.Text(" from users where name like "),
            ExpandableSection.BindVar(":name"),
            ExpandableSection.Text(" "),
            ExpandableSection.Newline,
            ExpandableSection.Text("AND last_name like "),
            ExpandableSection.BindVar(":lastName")
        )

        assertSections(
            "select * from users where name like :name \nAND last_name like \n:lastName",
            ExpandableSection.Text("select "),
            ExpandableSection.Projection.All,
            ExpandableSection.Text(" from users where name like "),
            ExpandableSection.BindVar(":name"),
            ExpandableSection.Text(" "),
            ExpandableSection.Newline,
            ExpandableSection.Text("AND last_name like "),
            ExpandableSection.Newline,
            ExpandableSection.BindVar(":lastName")
        )
    }

    fun assertVariables(query: String, vararg expected: String) {
        assertThat(
            (ExpandableSqlParser.parse(query)).inputs.map { it.section.text },
            `is`(expected.toList())
        )
    }

    fun assertErrors(query: String, vararg errors: String) {
        assertThat((ExpandableSqlParser.parse(query)).errors, `is`(errors.toList()))
    }

    fun assertSections(query: String, vararg sections: ExpandableSection) {
        assertThat(ExpandableSqlParser.parse(query).sections, `is`(sections.toList()))
    }
}
