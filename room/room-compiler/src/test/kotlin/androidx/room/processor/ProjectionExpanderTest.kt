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

package androidx.room.processor

import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.isTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.parser.SqlParser
import androidx.room.parser.expansion.ProjectionExpander
import androidx.room.runProcessorTestWithK1
import androidx.room.testing.context
import createVerifierFromEntitiesAndViews
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectionExpanderTest {

    companion object {
        const val DATABASE_PREFIX =
            """
            package foo.bar;
            import androidx.room.*;
            import androidx.annotation.NonNull;
            import java.util.*;
        """

        val ENTITIES =
            listOf(
                Source.java(
                    "foo.bar.User",
                    DATABASE_PREFIX +
                        """
                    @Entity
                    public class User {
                        @PrimaryKey
                        public int id;
                        public String firstName;
                        public String lastName;
                        public int teamId;
                    }
                """
                ),
                Source.java(
                    "foo.bar.Pet",
                    DATABASE_PREFIX +
                        """
                    @Entity
                    public class Pet {
                        @PrimaryKey
                        public int petId;
                        public int ownerId;
                    }
                """
                ),
                Source.java(
                    "foo.bar.Team",
                    DATABASE_PREFIX +
                        """
                    @Entity
                    public class Team {
                        @PrimaryKey
                        public int id;
                        public String name;
                    }
                """
                ),
                Source.java(
                    "foo.bar.Employee",
                    DATABASE_PREFIX +
                        """
                    @Entity
                    public class Employee {
                        @PrimaryKey
                        public int id;
                        public String name;
                        public Integer managerId;
                    }
                """
                ),
                Source.java(
                    "foo.bar.EmployeeSummary",
                    DATABASE_PREFIX +
                        """
                    public class EmployeeSummary {
                        public int id;
                        public String name;
                    }
                """
                )
            )
    }

    @Test
    fun summary() {
        testInterpret(
            "foo.bar.UserSummary",
            """
                public class UserSummary {
                    public int id;
                    public String firstName;
                }
            """,
            "SELECT * FROM User",
            "SELECT `id`, `firstName` FROM User"
        )
    }

    @Test
    fun embedded() {
        testInterpret(
            "foo.bar.UserCopy",
            """
                public class UserCopy {
                    @Embedded
                    public User user;
                }
            """,
            "SELECT * FROM User",
            """
                SELECT `User`.`id` AS `id`, `User`.`firstName` AS `firstName`,
                `User`.`lastName` AS `lastName`, `User`.`teamId` AS `teamId` FROM User
            """
        )
    }

    @Test
    fun selectConstant() {
        testInterpret(
            "foo.bar.JustFirstName",
            """
                public class JustFirstName {
                    public String firstName;
                }
            """,
            "SELECT 'a' AS firstName",
            "SELECT 'a' AS firstName"
        )
    }

    @Test
    fun selectParameter() {
        testInterpret(
            "foo.bar.JustFirstName",
            """
                public class JustFirstName {
                    public String firstName;
                }
            """,
            "SELECT :firstName AS firstName",
            "SELECT ? AS firstName"
        )
    }

    @Test
    fun irrelevantAlias() {
        testInterpret(
            "foo.bar.UserAndPet",
            """
                public class UserAndPet {
                    @Embedded
                    public User user;
                    @Embedded
                    public Pet pet;
                }
            """,
            "SELECT * FROM user u LEFT OUTER JOIN pet p ON u.id = p.ownerId",
            """
                SELECT `u`.`id` AS `id`, `u`.`firstName` AS `firstName`,
                `u`.`lastName` AS `lastName`, `u`.`teamId` AS `teamId`,
                `p`.`petId` AS `petId`, `p`.`ownerId` AS `ownerId`
                FROM user u LEFT OUTER JOIN pet p ON u.id = p.ownerId
            """
        )
    }

    @Test
    fun additional() {
        testInterpret(
            "foo.bar.UserSummary",
            """
                public class UserSummary {
                    public int id;
                    public String name;
                }
            """,
            "SELECT *, firstName | ' ' | lastName AS name FROM User",
            "SELECT `id`, firstName | ' ' | lastName AS name FROM User"
        )
    }

    @Test
    fun additional_immediateValue() {
        testInterpret(
            "foo.bar.Uno",
            """
                public class Uno {
                    public int id;
                    public String firstName;
                    public int uno;
                }
            """,
            "SELECT *, 1 AS uno FROM User",
            "SELECT `id`, `firstName`, 1 AS uno FROM User"
        )
    }

    @Test
    fun additional_logic() {
        testInterpret(
            "foo.bar.UserJuanOrPedro",
            """
                public class UserJuanOrPedro {
                    public int id;
                    public boolean isJuanOrPedro;
                }
            """,
            "SELECT *, firstName IN ('juan', 'pedro') AS isJuanOrPedro FROM User",
            "SELECT `id`, firstName IN ('juan', 'pedro') AS isJuanOrPedro FROM User"
        )
    }

    @Test
    fun additional_innerQuery() {
        testInterpret(
            "foo.bar.UserUnique",
            """
                public class UserUnique {
                    public int id;
                    public String firstName;
                    public String hasUniqueFirstName;
                }
            """,
            "SELECT *, (SELECT COUNT(*) FROM User AS u WHERE u.firstName = User.firstName) = 1 " +
                "AS hasUniqueFirstName FROM User",
            """
                SELECT `id`, `firstName`, (SELECT COUNT(*) FROM User AS u
                WHERE u.firstName = User.firstName) = 1 AS hasUniqueFirstName FROM User
            """
        )
    }

    @Test
    fun ignore() {
        testInterpret(
            "foo.bar.UserSummary",
            """
                public class UserSummary {
                    public int id;
                    public String firstName;
                    @Ignore
                    public String lastName;
                }
            """,
            "SELECT * FROM User",
            "SELECT `id`, `firstName` FROM User"
        )
    }

    @Test
    fun extraColumn() {
        testInterpret(
            "foo.bar.UserVariant",
            """
                public class UserVariant {
                    public int id;
                    public String firstName;
                    public String noSuchColumn;
                }
            """,
            "SELECT * FROM User",
            "SELECT `id`, `firstName` FROM User"
        )
    }

    @Test
    fun join() {
        testInterpret(
            "foo.bar.UserDetail",
            """
                public class UserDetail {
                    @Embedded
                    public User user;
                    @Embedded(prefix = "team_")
                    public Team team;
                }
            """,
            "SELECT * FROM User INNER JOIN Team AS team_ ON User.teamId = team_.id",
            """
                SELECT `User`.`id` AS `id`, `User`.`firstName` AS `firstName`,
                `User`.`lastName` AS `lastName`, `User`.`teamId` AS `teamId`,
                `team_`.`id` AS `team_id`, `team_`.`name` AS `team_name`
                FROM User INNER JOIN Team AS team_ ON User.teamId = team_.id
            """
        )
    }

    @Test
    fun joinSelf() {
        testInterpret(
            "foo.bar.EmployeeWithManager",
            """
                public class EmployeeWithManager {
                    @Embedded
                    public Employee employee;
                    @Embedded(prefix = "manager_")
                    public Employee manager;
                }
            """,
            "SELECT * FROM Employee LEFT OUTER JOIN Employee AS manager_ " +
                "ON User.managerId = manager_.id",
            """
                SELECT `Employee`.`id` AS `id`, `Employee`.`name` AS `name`,
                `Employee`.`managerId` AS `managerId`, `manager_`.`id` AS `manager_id`,
                `manager_`.`name` AS `manager_name`,
                `manager_`.`managerId` AS `manager_managerId` FROM Employee
                LEFT OUTER JOIN Employee AS manager_ ON User.managerId = manager_.id
            """
        )
    }

    @Test
    fun joinWithoutPrefix() {
        testInterpret(
            "foo.bar.UserAndPet",
            """
                public class UserAndPet {
                    @Embedded
                    public User user;
                    @Embedded
                    public Pet pet;
                }
            """,
            "SELECT * FROM User LEFT OUTER JOIN Pet ON User.id = Pet.ownerId",
            """
                SELECT `User`.`id` AS `id`, `User`.`firstName` AS `firstName`,
                `User`.`lastName` AS `lastName`, `User`.`teamId` AS `teamId`,
                `Pet`.`petId` AS `petId`, `Pet`.`ownerId` AS `ownerId`
                FROM User LEFT OUTER JOIN Pet ON User.id = Pet.ownerId
            """
        )
    }

    @Test
    fun embedPojo() {
        testInterpret(
            "foo.bar.TeamMember",
            """
                public class TeamMember {
                    @Embedded
                    Team team;
                    @Embedded(prefix = "employee_")
                    public EmployeeSummary employee;
                }
            """,
            "SELECT * FROM Team LEFT OUTER JOIN Employee AS employee_" +
                " ON Team.id = employee_.teamId",
            """
                SELECT `Team`.`id` AS `id`, `Team`.`name` AS `name`,
                `employee_`.`id` AS `employee_id`, `employee_`.`name` AS `employee_name`
                FROM Team LEFT OUTER JOIN Employee AS employee_
                ON Team.id = employee_.teamId
            """
        )
    }

    @Test
    fun specifyTable() {
        testInterpret(
            "foo.bar.UserDetail",
            """
                public class UserDetail {
                    @Embedded
                    public User user;
                    @Embedded(prefix = "team_")
                    public Team team;
                }
            """,
            "SELECT User.*, team_.* FROM User INNER JOIN Team AS team_ ON User.teamId = team_.id",
            """
                SELECT `User`.`id` AS `id`, `User`.`firstName` AS `firstName`,
                `User`.`lastName` AS `lastName`, `User`.`teamId` AS `teamId`,
                `team_`.`id` AS `team_id`, `team_`.`name` AS `team_name`
                FROM User INNER JOIN Team AS team_ ON User.teamId = team_.id
            """
        )
    }

    @Test
    fun specifyAlias() {
        testInterpret(
            "foo.bar.UserPair",
            """
                public class UserPair {
                    @Embedded(prefix = "a_")
                    public User a;
                    @Embedded(prefix = "b_")
                    public User b;
                }
            """,
            "SELECT a_.*, b_.* FROM User AS a_, User AS b_",
            """
                SELECT `a_`.`id` AS `a_id`, `a_`.`firstName` AS `a_firstName`,
                `a_`.`lastName` AS `a_lastName`, `a_`.`teamId` AS `a_teamId`, `b_`.`id` AS `b_id`,
                `b_`.`firstName` AS `b_firstName`, `b_`.`lastName` AS `b_lastName`,
                `b_`.`teamId` AS `b_teamId` FROM User AS a_, User AS b_
            """
        )
    }

    @Test
    fun parameter() {
        testInterpret(
            "foo.bar.UserSummary",
            """
                public class UserSummary {
                    public int id;
                    public String firstName;
                }
            """,
            "SELECT id, firstName FROM User WHERE id = :id",
            "SELECT id, firstName FROM User WHERE id = ?"
        )
    }

    @Test
    fun noNeedToExpand() {
        testInterpret(
            "foo.bar.UserSummary",
            """
                public class UserSummary {
                    public int id;
                    public String firstName;
                }
            """,
            "SELECT id, firstName FROM User",
            "SELECT id, firstName FROM User"
        )
    }

    @Test
    fun withTableName() {
        testInterpret(
            "foo.bar.UserSummary",
            """
            public class UserSummary {
                public int id;
                public String firstName;
            }
            """,
            "SELECT User.* FROM User",
            "SELECT `User`.`id`, `User`.`firstName` FROM User"
        )
    }

    @Test
    fun withTableNameAndAlias() {
        testInterpret(
            "foo.bar.UserSummary",
            """
            public class UserSummary {
                public int id;
                public String firstName;
            }
            """,
            "SELECT `u`.* FROM User u",
            "SELECT `u`.`id`, `u`.`firstName` FROM User u"
        )
    }

    @Test
    fun aliasWithInnerJoin() {
        testInterpret(
            name = "foo.bar.User",
            input = null,
            original = "SELECT * FROM user as u INNER JOIN Employee AS e ON(u.id = e.id)",
            expected =
                "SELECT `u`.`id` AS `id`, `u`.`firstName` AS `firstName`, `u`" +
                    ".`lastName` AS `lastName`, `u`.`teamId` AS `teamId` FROM user as u INNER " +
                    "JOIN Employee AS e ON(u.id = e.id)"
        )
    }

    @Test
    fun joinAndAbandon() {
        testInterpret(
            "foo.bar.UserCopy",
            """
                public class UserCopy {
                    @Embedded
                    User user;
                }
            """,
            "SELECT * FROM User JOIN Team ON User.id = Team.id",
            """
                SELECT `User`.`id` AS `id`, `User`.`firstName` AS `firstName`,
                `User`.`lastName` AS `lastName`, `User`.`teamId` AS `teamId`
                FROM User JOIN Team ON User.id = Team.id
            """
        )
    }

    @Test
    fun joinAndAbandonEntity() {
        runProcessorTestWithK1(sources = ENTITIES) { invocation ->
            val entities =
                invocation.roundEnv
                    .getElementsAnnotatedWith(androidx.room.Entity::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map { element -> TableEntityProcessor(invocation.context, element).process() }
            val entityElement = invocation.processingEnv.requireTypeElement("foo.bar.User")
            check(entityElement.isTypeElement())
            val entity =
                PojoProcessor.createFor(
                        invocation.context,
                        entityElement,
                        bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                        parent = null
                    )
                    .process()
            val query = SqlParser.parse("SELECT * FROM User JOIN Team ON User.id = Team.id")
            val verifier = createVerifierFromEntitiesAndViews(invocation)
            query.resultInfo = verifier.analyze(query.original)
            val interpreter = ProjectionExpander(entities)
            val expanded = interpreter.interpret(query, entity)
            val expected =
                """
                SELECT `User`.`id` AS `id`, `User`.`firstName` AS `firstName`,
                `User`.`lastName` AS `lastName`, `User`.`teamId` AS `teamId`
                FROM User JOIN Team ON User.id = Team.id
            """
                    .trimIndent()
                    .lines()
                    .joinToString(" ")
            assertThat(expanded, `is`(equalTo(expected)))
        }
    }

    @Test
    fun newlineInProjection() {
        queryWithPojo(
            "foo.bar.UserSummary",
            """
                public class UserSummary {
                    public int id;
                    public String name;
                }
            """,
            """
                SELECT User
                .
                *,
                firstName
                |
                ' '
                |
                lastName
                AS
                `name` FROM User
            """
        ) { expanded, _ ->
            assertThat(
                expanded,
                `is`(
                    equalTo(
                        """
                SELECT `User`.`id`,
                firstName
                |
                ' '
                |
                lastName
                AS
                `name` FROM User
            """
                    )
                )
            )
        }
    }

    private fun testInterpret(name: String, input: String?, original: String, expected: String) {
        queryWithPojo(name, input, original) { actual, _ ->
            assertThat(actual, `is`(equalTo(expected.trimIndent().lines().joinToString(" "))))
        }
    }

    private fun queryWithPojo(
        name: String,
        input: String?,
        original: String,
        handler: (expanded: String, invocation: XTestInvocation) -> Unit
    ) {
        val extraSource =
            input?.let { listOf(Source.java(name, DATABASE_PREFIX + input)) } ?: emptyList()
        val all = ENTITIES + extraSource
        return runProcessorTestWithK1(sources = all) { invocation ->
            val entities =
                invocation.roundEnv
                    .getElementsAnnotatedWith(androidx.room.Entity::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map { element -> TableEntityProcessor(invocation.context, element).process() }
            val pojoElement = invocation.processingEnv.requireTypeElement(name)
            check(pojoElement.isTypeElement())
            val pojo =
                PojoProcessor.createFor(
                        invocation.context,
                        pojoElement,
                        bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                        parent = null
                    )
                    .process()
            val query = SqlParser.parse(original)
            val verifier = createVerifierFromEntitiesAndViews(invocation)
            query.resultInfo = verifier.analyze(query.original)
            val interpreter = ProjectionExpander(entities)
            val expanded = interpreter.interpret(query, pojo)
            handler(expanded, invocation)
        }
    }
}
