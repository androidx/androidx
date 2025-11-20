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
package androidx.room3.integration.kotlintestapp.test

import android.content.Context
import androidx.kruth.assertThat
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.DatabaseView
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ExperimentalRoomApi
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseViewTest {

    @Entity(
        foreignKeys =
            [
                ForeignKey(
                    entity = Employee::class,
                    childColumns = ["managerId"],
                    parentColumns = ["id"],
                ),
                ForeignKey(entity = Team::class, childColumns = ["teamId"], parentColumns = ["id"]),
            ],
        indices = [Index("managerId"), Index("teamId")],
    )
    data class Employee(
        @PrimaryKey val id: Long,
        val name: String,
        val managerId: Long?,
        val teamId: Long,
    )

    @DatabaseView(
        """
        SELECT
          employee.id,
          employee.name,
          employee.teamId,
          manager.id AS manager_id,
          manager.name AS manager_name,
          manager.managerId AS manager_managerId,
          manager.teamId AS manager_teamId
         FROM Employee AS employee LEFT JOIN Employee AS manager
         ON employee.managerId = manager.id
        """
    )
    data class EmployeeWithManager(
        val id: Long,
        val name: String,
        val teamId: Long,
        @Embedded(prefix = "manager_") val manager: Employee?,
    )

    @DatabaseView(
        """
        SELECT
          employee.id,
          employee.name,
          employee.manager_id,
          employee.manager_name,
          employee.manager_managerId,
          employee.manager_teamId,
          team.id AS team_id,
          team.name AS team_name,
          team.departmentId AS team_departmentId,
          team.departmentName AS team_departmentName
         FROM EmployeeWithManager AS employee
         LEFT JOIN TeamDetail AS team ON employee.teamId = team.id
        """
    )
    data class EmployeeDetail(
        val id: Long,
        val name: String,
        @Embedded(prefix = "manager_") val manager: Employee?,
        @Embedded(prefix = "team_") val team: TeamDetail,
    )

    data class TeamWithMembers(
        @Embedded val teamDetail: TeamDetail,
        @Relation(parentColumn = "id", entityColumn = "teamId")
        val members: List<EmployeeWithManager>,
    )

    @Entity(
        foreignKeys =
            [
                ForeignKey(
                    entity = Department::class,
                    childColumns = ["departmentId"],
                    parentColumns = ["id"],
                )
            ],
        indices = [Index("departmentId")],
    )
    data class Team(@PrimaryKey val id: Long, val departmentId: Long, val name: String)

    @Entity data class Department(@PrimaryKey val id: Long, val name: String)

    @DatabaseView(
        """
        SELECT
          Team.id,
          Team.name,
          Team.departmentId,
          Department.name AS departmentName
        FROM Team INNER JOIN Department ON Team.departmentId = Department.id
        """
    )
    data class TeamDetail(
        val id: Long,
        val name: String,
        val departmentId: Long,
        val departmentName: String,
    )

    @DatabaseView(
        """
        SELECT 
          Team.*,
          dep.id AS department_id,
          dep.name as department_name
        FROM Team INNER JOIN Department AS dep ON Team.departmentId = dep.id
        """
    )
    data class TeamDetail2(
        @Embedded val team: Team,
        @Embedded(prefix = "department_") val department: Department,
    )

    @DatabaseView(
        """
        SELECT
          td1.id AS first_id,
          td1.name AS first_name,
          td1.departmentId AS first_departmentId,
          td1.departmentName AS first_departmentName,
          td2.id AS second_id, td2.name AS second_name,
          td2.departmentId AS second_departmentId,
          td2.departmentName AS second_departmentName
        FROM TeamDetail AS td1, TeamDetail AS td2 WHERE td1.id <> td2.id
        """
    )
    data class TeamPair(
        @Embedded(prefix = "first_") val first: TeamDetail,
        @Embedded(prefix = "second_") val second: TeamDetail,
    )

    @Dao
    interface EmployeeDao {
        @Insert fun insert(employee: Employee): Long

        @Query("UPDATE Employee SET managerId = :managerId WHERE id = :id")
        fun updateReport(id: Long, managerId: Long)

        @Query("SELECT * FROM EmployeeWithManager WHERE id = :id")
        fun withManagerById(id: Long): EmployeeWithManager

        @Query("SELECT * FROM EmployeeDetail WHERE id = :id")
        fun detailById(id: Long): EmployeeDetail

        @Query("SELECT * FROM EmployeeDetail WHERE id = :id")
        fun flowDetailById(id: Long): Flow<EmployeeDetail?>
    }

    @Dao
    interface TeamDao {
        @Insert fun insert(team: Team): Long

        @Query("SELECT * FROM TeamDetail WHERE id = :id") fun detailById(id: Long): TeamDetail

        @Query("SELECT * FROM TeamDetail WHERE id = :id")
        fun flowDetailById(id: Long): Flow<TeamDetail?>

        @Transaction
        @Query("SELECT * FROM TeamDetail WHERE id = :id")
        fun withMembers(id: Long): TeamWithMembers

        @Query("SELECT * FROM TeamDetail2 WHERE id = :id") fun detail2ById(id: Long): TeamDetail2

        @Query("SELECT * FROM TeamPair WHERE first_id = :id")
        fun roundRobinById(id: Long): List<TeamPair>
    }

    @Dao
    interface DepartmentDao {
        @Insert fun insert(department: Department): Long

        @Query("UPDATE Department SET name = :name WHERE id = :id")
        fun rename(id: Long, name: String)
    }

    @Database(
        entities = [Department::class, Team::class, Employee::class],
        views =
            [
                TeamDetail::class,
                TeamDetail2::class,
                TeamPair::class,
                EmployeeWithManager::class,
                EmployeeDetail::class,
            ],
        version = 1,
        exportSchema = false,
    )
    abstract class CompanyDatabase : RoomDatabase() {
        abstract fun department(): DepartmentDao

        abstract fun team(): TeamDao

        abstract fun employee(): EmployeeDao
    }

    private lateinit var db: CompanyDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder<CompanyDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    @SmallTest
    fun basic() {
        db.department().insert(Department(1L, "Sales"))
        db.department().insert(Department(2L, "IT"))
        db.team().insert(Team(1L, 1L, "Books"))
        db.team().insert(Team(2L, 2L, "Backend"))
        val team1 = db.team().detailById(1L)
        assertThat(team1.name).isEqualTo("Books")
        assertThat(team1.departmentName).isEqualTo("Sales")
        val team2 = db.team().detailById(2L)
        assertThat(team2.name).isEqualTo("Backend")
        assertThat(team2.departmentName).isEqualTo("IT")
    }

    @Test
    @SmallTest
    fun embedded() {
        db.department().insert(Department(1, "Sales"))
        db.team().insert(Team(1L, 1L, "Books"))
        db.employee().insert(Employee(1L, "CEO", null, 1L))
        db.employee().insert(Employee(2L, "John", 1L, 1L))
        db.employee().insert(Employee(3L, "Jane", 2L, 1L))
        assertThat(db.employee().withManagerById(1L).manager).isNull()
        assertThat(db.employee().withManagerById(2L).manager?.name).isEqualTo("CEO")
        assertThat(db.employee().withManagerById(3L).manager?.name).isEqualTo("John")
    }

    @Test
    @MediumTest
    fun flow() = runTest {
        db.department().insert(Department(1L, "Shop"))
        val team = db.team().flowDetailById(1L).produceIn(this)

        assertThat(team.receive()).isNull()

        db.team().insert(Team(1L, 1L, "Books"))
        team.receive().let { detail ->
            checkNotNull(detail)
            assertThat(detail.name).isEqualTo("Books")
            assertThat(detail.departmentName).isEqualTo("Shop")
        }

        db.department().rename(1L, "Sales")
        team.receive().let { detail ->
            checkNotNull(detail)
            assertThat(detail.name).isEqualTo("Books")
            assertThat(detail.departmentName).isEqualTo("Sales")
        }

        team.cancel()
    }

    @Test
    @SmallTest
    fun nested() {
        db.department().insert(Department(1L, "Shop"))
        db.team().insert(Team(1L, 1L, "Books"))
        db.employee().insert(Employee(1L, "CEO", null, 1L))
        db.employee().insert(Employee(2L, "John", 1L, 1L))

        val employee = db.employee().detailById(2L)
        assertThat(employee.name).isEqualTo("John")
        assertThat(employee.manager?.name).isEqualTo("CEO")
        assertThat(employee.team.name).isEqualTo("Books")
        assertThat(employee.team.departmentName).isEqualTo("Shop")
    }

    @Test
    @MediumTest
    @OptIn(ExperimentalRoomApi::class)
    fun nestedFlow() = runTest {
        val employee = db.employee().flowDetailById(2L).produceIn(this)

        assertThat(employee.receive()).isNull()

        db.withWriteTransaction {
            db.department().insert(Department(1L, "Shop"))
            db.team().insert(Team(1L, 1L, "Books"))
            db.employee().insert(Employee(1L, "CEO", null, 1L))
            db.employee().insert(Employee(2L, "Jane", 1L, 1L))
        }
        employee.receive().let { detail ->
            checkNotNull(detail)
            assertThat(detail.name).isEqualTo("Jane")
            assertThat(detail.manager!!.name).isEqualTo("CEO")
            assertThat(detail.team.name).isEqualTo("Books")
            assertThat(detail.team.departmentName).isEqualTo("Shop")
        }

        db.withWriteTransaction {
            db.department().rename(1L, "Sales")
            db.employee().insert(Employee(3L, "John", 1L, 1L))
            db.employee().updateReport(2L, 3L)
        }
        employee.receive().let { detail ->
            checkNotNull(detail)
            assertThat(detail.name).isEqualTo("Jane")
            assertThat(detail.manager!!.name).isEqualTo("John")
            assertThat(detail.team.name).isEqualTo("Books")
            assertThat(detail.team.departmentName).isEqualTo("Sales")
        }

        employee.cancel()
    }

    @Test
    @MediumTest
    fun viewInRelation() {
        db.department().insert(Department(1L, "Shop"))
        db.team().insert(Team(1L, 1L, "Books"))
        db.employee().insert(Employee(1L, "CEO", null, 1L))
        db.employee().insert(Employee(2L, "John", 1L, 1L))

        val teamWithMembers = db.team().withMembers(1L)
        assertThat(teamWithMembers.teamDetail.name).isEqualTo("Books")
        assertThat(teamWithMembers.teamDetail.departmentName).isEqualTo("Shop")
        assertThat(teamWithMembers.members).hasSize(2)
        assertThat(teamWithMembers.members[0].name).isEqualTo("CEO")
        assertThat(teamWithMembers.members[1].name).isEqualTo("John")
        assertThat(teamWithMembers.members[1].manager?.name).isEqualTo("CEO")
    }

    @Test
    @MediumTest
    fun expandedProjection() {
        db.department().insert(Department(3L, "Sales"))
        db.team().insert(Team(5L, 3L, "Books"))
        val detail = db.team().detail2ById(5L)
        assertThat(detail.team.id).isEqualTo(5L)
        assertThat(detail.team.name).isEqualTo("Books")
        assertThat(detail.team.departmentId).isEqualTo(3L)
        assertThat(detail.department.id).isEqualTo(3L)
        assertThat(detail.department.name).isEqualTo("Sales")
    }

    @Test
    @MediumTest
    fun expandedProjection_embedView() {
        db.department().insert(Department(3L, "Sales"))
        db.team().insert(Team(5L, 3L, "Books"))
        db.team().insert(Team(7L, 3L, "Toys"))
        val pairs = db.team().roundRobinById(5L)
        assertThat(pairs).hasSize(1)
        assertThat(pairs.single().first.name).isEqualTo("Books")
        assertThat(pairs.single().first.departmentName).isEqualTo("Sales")
        assertThat(pairs.single().second.name).isEqualTo("Toys")
        assertThat(pairs.single().second.departmentName).isEqualTo("Sales")
    }
}
