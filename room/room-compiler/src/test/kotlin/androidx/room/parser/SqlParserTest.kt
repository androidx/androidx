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
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SqlParserTest {
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
        val parsed = SqlParser.parse("DELETE FROM users where id > 3")
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
        val parsed = SqlParser.parse("UPDATE users set name = :name where id = :id")
        assertThat(parsed.errors, `is`(emptyList()))
        assertThat(parsed.type, `is`(QueryType.UPDATE))
    }

    @Test
    fun insertQuery() {
        val parsed = SqlParser.parse(
            "INSERT OR REPLACE INTO notes (id, content) VALUES (:id, :content)"
        )
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
    fun extractTableNames() {
        assertThat(
            SqlParser.parse("select * from users").tables,
            `is`(setOf(Table("users", "users")))
        )
        assertThat(
            SqlParser.parse("select * from users as ux").tables,
            `is`(setOf(Table("users", "ux")))
        )
        assertThat(
            SqlParser.parse("select * from (select * from books)").tables,
            `is`(setOf(Table("books", "books")))
        )
        assertThat(
            SqlParser.parse("select x.id from (select * from books) as x").tables,
            `is`(setOf(Table("books", "books")))
        )
    }

    @Test
    fun unescapeTableNames() {
        assertThat(
            SqlParser.parse("select * from `users`").tables,
            `is`(setOf(Table("users", "users")))
        )
        assertThat(
            SqlParser.parse("select * from \"users\"").tables,
            `is`(setOf(Table("users", "users")))
        )
        assertThat(
            SqlParser.parse("select * from 'users'").tables,
            `is`(setOf(Table("users", "users")))
        )
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
        val query = SqlParser.parse(
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
    fun selectMultipleBindingParameter() {
        SqlParser.parse("SELECT * FROM Foo WHERE id IN (:param1) AND bar = :param2").let {
            assertThat(it.inputs.size, `is`(2))
            assertThat(it.inputs[0].isMultiple, `is`(true))
            assertThat(it.inputs[1].isMultiple, `is`(false))
        }
        SqlParser.parse("SELECT * FROM Foo WHERE id NOT IN (:param1)").let {
            assertThat(it.inputs.size, `is`(1))
            assertThat(it.inputs[0].isMultiple, `is`(true))
        }
        SqlParser.parse("SELECT * FROM Foo WHERE id IN ('a', :param1, 'c')").let {
            assertThat(it.inputs.size, `is`(1))
            assertThat(it.inputs[0].isMultiple, `is`(true))
        }
        SqlParser.parse("SELECT (:param1), ifnull(:param2, data) FROM Foo").let {
            assertThat(it.inputs.size, `is`(2))
            assertThat(it.inputs[0].isMultiple, `is`(true))
            assertThat(it.inputs[1].isMultiple, `is`(false))
        }
        SqlParser.parse("SELECT max(:param1) FROM Foo").let {
            assertThat(it.inputs.size, `is`(1))
            assertThat(it.inputs[0].isMultiple, `is`(true))
        }
        SqlParser.parse("SELECT * FROM Foo JOIN Bar ON (Foo.id = Bar.id) GROUP BY (:param1)").let {
            assertThat(it.inputs.size, `is`(1))
            assertThat(it.inputs[0].isMultiple, `is`(true))
        }
        SqlParser.parse("SELECT MAX(:param1) AS num FROM Foo WHERE num > ABS(:param2)").let {
            assertThat(it.inputs.size, `is`(2))
            assertThat(it.inputs[0].isMultiple, `is`(true))
            assertThat(it.inputs[1].isMultiple, `is`(false))
        }
        SqlParser.parse("SELECT * FROM Foo WHERE num > customFun(:param)").let {
            assertThat(it.inputs.size, `is`(1))
            assertThat(it.inputs[0].isMultiple, `is`(true))
        }
    }

    @Test
    fun insertMultipleBindingParameter() {
        val query = SqlParser.parse("INSERT INTO Foo VALUES (:param)")
        assertThat(query.inputs.size, `is`(1))
        assertThat(query.inputs.first().isMultiple, `is`(true))
    }

    @Test
    fun foo() {
        assertSections(
            "select * from users where name like ?",
            Section.text("select * from users where name like "),
            Section.bindVar("?", false)
        )
        assertSections(
            "select * from users where name like :name AND last_name like :lastName",
            Section.text("select * from users where name like "),
            Section.bindVar(":name", false),
            Section.text(" AND last_name like "),
            Section.bindVar(":lastName", false)
        )
        assertSections(
            "select * from users where name \nlike :name AND last_name like :lastName",
            Section.text("select * from users where name "),
            Section.newline(),
            Section.text("like "),
            Section.bindVar(":name", false),
            Section.text(" AND last_name like "),
            Section.bindVar(":lastName", false)
        )
        assertSections(
            "select * from users where name like :name \nAND last_name like :lastName",
            Section.text("select * from users where name like "),
            Section.bindVar(":name", false),
            Section.text(" "),
            Section.newline(),
            Section.text("AND last_name like "),
            Section.bindVar(":lastName", false)
        )
        assertSections(
            "select * from users where name like :name \nAND last_name like \n:lastName",
            Section.text("select * from users where name like "),
            Section.bindVar(":name", false),
            Section.text(" "),
            Section.newline(),
            Section.text("AND last_name like "),
            Section.newline(),
            Section.bindVar(":lastName", false)
        )
        assertSections(
            "select * from users where name in (?)",
            Section.text("select * from users where name in ("),
            Section.bindVar("?", true),
            Section.text(")")
        )
        assertSections(
            "select * from users where name in (:names)",
            Section.text("select * from users where name in ("),
            Section.bindVar(":names", true),
            Section.text(")")
        )
    }

    fun assertVariables(query: String, vararg expected: String) {
        assertThat((SqlParser.parse(query)).inputs.map { it.text }, `is`(expected.toList()))
    }

    fun assertErrors(query: String, vararg errors: String) {
        assertThat((SqlParser.parse(query)).errors, `is`(errors.toList()))
    }

    fun assertSections(query: String, vararg sections: Section) {
        assertThat(SqlParser.parse(query).sections, `is`(sections.toList()))
    }
}