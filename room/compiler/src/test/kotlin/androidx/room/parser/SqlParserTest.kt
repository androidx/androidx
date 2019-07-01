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

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SqlParserTest {

    @Test
    fun multipleQueries() {
        assertErrors("SELECT * FROM users; SELECT * FROM books;",
                ParserErrors.NOT_ONE_QUERY)
    }

    @Test
    fun empty() {
        assertErrors("", ParserErrors.NOT_ONE_QUERY)
    }

    @Test
    fun deleteQuery() {
        val parsed = SqlParser.parse("DELETE FROM users where id > 3")
        assertThat(parsed.errors, `is`(emptyList()))
        assertThat(parsed.type, `is`(QueryType.DELETE))
    }

    @Test
    fun badDeleteQuery() {
        assertErrors("delete from user where mAge >= :min && mAge <= :max",
                "no viable alternative at input 'delete from user where mAge >= :min &&'")
    }

    @Test
    fun updateQuery() {
        val parsed = SqlParser.parse("UPDATE users set name = :name where id = :id")
        assertThat(parsed.errors, `is`(emptyList()))
        assertThat(parsed.type, `is`(QueryType.UPDATE))
    }

    @Test
    fun insertQuery() {
        val parsed = SqlParser.parse(
            "INSERT OR REPLACE INTO notes (id, content) VALUES (:id, :content)")
        assertThat(parsed.errors, `is`(emptyList()))
        assertThat(parsed.type, `is`(QueryType.INSERT))
    }

    @Test
    fun upsertQuery() {
        val parsed = SqlParser.parse(
            "INSERT INTO notes (id, content) VALUES (:id, :content) " +
                "ON CONFLICT (id) DO UPDATE SET content = excluded.content, " +
                "revision = revision + 1, modifiedTime = strftime('%s','now')"
        )
        assertThat(parsed.errors, `is`(emptyList()))
        assertThat(parsed.type, `is`(QueryType.INSERT))
    }

    @Test
    fun explain() {
        assertErrors("EXPLAIN QUERY PLAN SELECT * FROM users",
                ParserErrors.invalidQueryType(QueryType.EXPLAIN))
    }

    @Test
    fun validColumnNames() {
        listOf("f", "fo", "f2", "f 2", "foo_2", "foo-2", "_", "foo bar baz",
                "foo 2 baz", "_baz", "fooBar", "2", "*", "foo*2", "dsa$", "\$fsa",
                "-bar", "şoöğüı").forEach {
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
        val query = SqlParser.parse("SELECT * FROM User WHERE teamId IN " +
                "(SELECT * FROM Team WHERE active != 0)")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.projections.size, `is`(1))
        assertThat(query.projections.first().section.text, `is`(equalTo("*")))
    }

    @Test
    fun projection_tableName() {
        val query = SqlParser.parse("SELECT User.* FROM User")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.projections.size, `is`(1))
        assertThat(query.projections.first().section.text, `is`(equalTo("User.*")))
    }

    @Test
    fun projection_columnNames() {
        val query = SqlParser.parse("SELECT `id` AS `a_id`, name FROM User")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.projections.size, `is`(0))
        assertThat(query.sections.size, `is`(1))
        assertThat(query.sections[0], `is`(instanceOf(Section.Text::class.java)))
        assertThat(query.explicitColumns.size, `is`(2))
        assertThat(query.explicitColumns[0], `is`(equalTo("a_id")))
        assertThat(query.explicitColumns[1], `is`(equalTo("name")))
    }

    @Test
    fun projection_containsNewline() {
        val query = SqlParser.parse("SELECT User   \n.   \n* FROM User")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.projections.size, `is`(1))
        assertThat(query.projections.first().section.text, `is`(equalTo("User   \n.   \n*")))
        assertThat(query.sections.size, `is`(3))
        assertThat(query.sections[0], `is`(instanceOf(Section.Text::class.java)))
        assertThat(query.sections[1], `is`(instanceOf(Section.Projection.Table::class.java)))
        assertThat(query.sections[2], `is`(instanceOf(Section.Text::class.java)))
    }

    @Test
    fun projection_containsExpression() {
        val query = SqlParser.parse("SELECT firstName || lastName AS fullName FROM User")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.projections.size, `is`(0))
        assertThat(query.sections.size, `is`(1))
        assertThat(query.sections[0], `is`(instanceOf(Section.Text::class.java)))
        assertThat(query.explicitColumns.size, `is`(1))
        assertThat(query.explicitColumns[0], `is`(equalTo("fullName")))
    }

    @Test
    fun projection_containsParameter() {
        val query = SqlParser.parse("SELECT firstName || :suffix AS nickname FROM User")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.projections.size, `is`(0))
        assertThat(query.sections.size, `is`(3))
        assertThat(query.sections[0], `is`(instanceOf(Section.Text::class.java)))
        assertThat(query.sections[1], `is`(instanceOf(Section.BindVar::class.java)))
        assertThat((query.sections[1] as Section.BindVar).symbol, `is`(equalTo(":suffix")))
        assertThat(query.sections[2], `is`(instanceOf(Section.Text::class.java)))
        assertThat(query.explicitColumns.size, `is`(1))
        assertThat(query.explicitColumns[0], `is`(equalTo("nickname")))
    }

    @Test
    fun extractTableNames() {
        assertThat(SqlParser.parse("select * from users").tables,
                `is`(setOf(Table("users", "users"))))
        assertThat(SqlParser.parse("select * from users as ux").tables,
                `is`(setOf(Table("users", "ux"))))
        assertThat(SqlParser.parse("select * from (select * from books)").tables,
                `is`(setOf(Table("books", "books"))))
        assertThat(SqlParser.parse("select x.id from (select * from books) as x").tables,
                `is`(setOf(Table("books", "books"))))
    }

    @Test
    fun unescapeTableNames() {
        assertThat(SqlParser.parse("select * from `users`").tables,
                `is`(setOf(Table("users", "users"))))
        assertThat(SqlParser.parse("select * from \"users\"").tables,
                `is`(setOf(Table("users", "users"))))
        assertThat(SqlParser.parse("select * from 'users'").tables,
                `is`(setOf(Table("users", "users"))))
    }

    @Test
    fun tablePrefixInInsert_set() {
        // this is an invalid query, b/64539805
        val query = SqlParser.parse("UPDATE trips SET trips.title=:title")
        assertThat(query.errors, not(emptyList()))
    }

    @Test
    fun tablePrefixInInsert_where() {
        val query = SqlParser.parse("UPDATE trips SET title=:title WHERE trips.id=:id")
        assertThat(query.errors, `is`(emptyList()))
    }

    @Test
    fun tablePrefixInSelect_projection() {
        val query = SqlParser.parse("SELECT a.name, b.last_name from user a, book b")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.tables, `is`(setOf(Table("user", "a"),
                Table("book", "b"))))
    }

    @Test
    fun tablePrefixInSelect_where() {
        val query = SqlParser.parse("SELECT a.name, b.last_name from user a, book b" +
                " WHERE a.name = b.name")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.tables, `is`(setOf(Table("user", "a"),
                Table("book", "b"))))
    }

    @Test
    fun unicodeInIdentifiers() {
        val query = SqlParser.parse("SELECT 名, 色 FROM 猫")
        assertThat(query.errors, `is`(emptyList()))
        assertThat(query.tables, `is`(setOf(Table("猫", "猫"))))
    }

    @Test
    fun rowValue_where() {
        val query = SqlParser.parse("SELECT * FROM notes WHERE (id, content) > (:id, :content)")
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
        assertErrors("select * from users where name like ?",
                ParserErrors.ANONYMOUS_BIND_ARGUMENT)
        assertErrors("select * from users where name like ? or last_name like ?",
                ParserErrors.ANONYMOUS_BIND_ARGUMENT)
        assertErrors("select * from users where name like ?1",
                ParserErrors.cannotUseVariableIndices("?1", 36))
    }

    @Test
    fun splitSections() {
        assertSections("select * from users where name like ?",
                Section.Text("select "),
                Section.Projection.All,
                Section.Text(" from users where name like "),
                Section.BindVar("?"))

        assertSections("select * from users where name like :name AND last_name like :lastName",
                Section.Text("select "),
                Section.Projection.All,
                Section.Text(" from users where name like "),
                Section.BindVar(":name"),
                Section.Text(" AND last_name like "),
                Section.BindVar(":lastName"))

        assertSections("select * from users where name \nlike :name AND last_name like :lastName",
                Section.Text("select "),
                Section.Projection.All,
                Section.Text(" from users where name "),
                Section.Newline,
                Section.Text("like "),
                Section.BindVar(":name"),
                Section.Text(" AND last_name like "),
                Section.BindVar(":lastName"))

        assertSections("select * from users where name like :name \nAND last_name like :lastName",
                Section.Text("select "),
                Section.Projection.All,
                Section.Text(" from users where name like "),
                Section.BindVar(":name"),
                Section.Text(" "),
                Section.Newline,
                Section.Text("AND last_name like "),
                Section.BindVar(":lastName"))

        assertSections("select * from users where name like :name \nAND last_name like \n:lastName",
                Section.Text("select "),
                Section.Projection.All,
                Section.Text(" from users where name like "),
                Section.BindVar(":name"),
                Section.Text(" "),
                Section.Newline,
                Section.Text("AND last_name like "),
                Section.Newline,
                Section.BindVar(":lastName"))
    }

    fun assertVariables(query: String, vararg expected: String) {
        assertThat((SqlParser.parse(query)).inputs.map { it.section.text }, `is`(expected.toList()))
    }

    fun assertErrors(query: String, vararg errors: String) {
        assertThat((SqlParser.parse(query)).errors, `is`(errors.toList()))
    }

    fun assertSections(query: String, vararg sections: Section) {
        assertThat(SqlParser.parse(query).sections, `is`(sections.toList()))
    }
}
