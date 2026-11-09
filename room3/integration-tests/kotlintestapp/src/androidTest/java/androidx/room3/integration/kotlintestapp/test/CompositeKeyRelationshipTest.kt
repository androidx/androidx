/*
 * Copyright (C) 2026 The Android Open Source Project
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
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.Junction
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.integration.kotlintestapp.test.TestDatabaseTest.UseDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@SmallTest
@RunWith(Parameterized::class)
class CompositeKeyRelationshipTest(private val useDriver: UseDriver) {
    private lateinit var db: MyDatabase
    private lateinit var dao: MyDao

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.BUNDLED, UseDriver.ANDROID)
    }

    @Before
    fun createDb() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder<MyDatabase>(context)
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .build()
        dao = db.getDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Entity data class Student(@PrimaryKey val id: Long, val grade: Int, val name: String)

    @Entity data class Class(@PrimaryKey val id: Long, val name: String)

    @Entity(
        primaryKeys = ["studentId", "studentGrade", "classId"],
        indices = [Index("studentGrade"), Index("classId")],
    )
    data class Enrollment(val studentId: Long, val studentGrade: Int, val classId: Long)

    @Entity
    data class ClubMembership(
        @PrimaryKey val membershipId: Long,
        val clubName: String,
        val studentId: Long,
        val studentGrade: Int,
    )

    data class StudentWithClubMemberships(
        @Embedded val student: Student,
        @Relation(parentColumns = ["id", "grade"], entityColumns = ["studentId", "studentGrade"])
        val memberships: List<ClubMembership>,
    )

    data class StudentWithClasses(
        @Embedded val student: Student,
        @Relation(
            parentColumns = ["id", "grade"],
            entityColumns = ["id"],
            associateBy =
                Junction(
                    value = Enrollment::class,
                    parentColumns = ["studentId", "studentGrade"],
                    entityColumns = ["classId"],
                ),
        )
        val classes: List<Class>,
    )

    data class StudentWithClassNames(
        @Embedded val student: Student,
        @Relation(
            entity = Class::class,
            parentColumns = ["id", "grade"],
            entityColumns = ["id"],
            associateBy =
                Junction(
                    value = Enrollment::class,
                    parentColumns = ["studentId", "studentGrade"],
                    entityColumns = ["classId"],
                ),
            projection = ["name"],
        )
        val classNames: List<String>,
    )

    data class ClassInfo(val name: String)

    data class StudentWithClassInfo(
        @Embedded val student: Student,
        @Relation(
            entity = Class::class,
            parentColumns = ["id", "grade"],
            entityColumns = ["id"],
            associateBy =
                Junction(
                    value = Enrollment::class,
                    parentColumns = ["studentId", "studentGrade"],
                    entityColumns = ["classId"],
                ),
        )
        val classInfo: List<ClassInfo>,
    )

    @Entity(primaryKeys = ["k1", "k2", "k3", "k4", "k5", "k6"])
    data class SixKeyParent(
        val k1: Long,
        val k2: Long,
        val k3: Long,
        val k4: Long,
        val k5: Long,
        val k6: Long,
    )

    @Entity
    data class SixKeyChild(
        @PrimaryKey val childId: Long,
        val p1: Long,
        val p2: Long,
        val p3: Long,
        val p4: Long,
        val p5: Long,
        val p6: Long,
    )

    data class SixKeyParentWithChildren(
        @Embedded val parent: SixKeyParent,
        @Relation(
            parentColumns = ["k1", "k2", "k3", "k4", "k5", "k6"],
            entityColumns = ["p1", "p2", "p3", "p4", "p5", "p6"],
        )
        val children: List<SixKeyChild>,
    )

    @Dao
    interface MyDao {
        @Insert fun insertStudent(student: Student)

        @Insert fun insertClass(clazz: Class)

        @Insert fun insertEnrollment(enrollment: Enrollment)

        @Insert fun insertClubMembership(membership: ClubMembership)

        @Transaction
        @Query("SELECT * FROM Student")
        fun getStudentsWithClubMemberships(): List<StudentWithClubMemberships>

        @Transaction
        @Query("SELECT * FROM Student")
        fun getStudentsWithClasses(): List<StudentWithClasses>

        @Transaction
        @Query("SELECT * FROM Student")
        fun getStudentsWithClassInfo(): List<StudentWithClassInfo>

        @Transaction
        @Query("SELECT * FROM Student")
        fun getStudentsWithClassNames(): List<StudentWithClassNames>

        @Insert fun insertSixKeyParents(parents: List<SixKeyParent>)

        @Insert fun insertSixKeyChildren(children: List<SixKeyChild>)

        @Transaction
        @Query("SELECT * FROM SixKeyParent")
        fun getSixKeyParentsWithChildren(): List<SixKeyParentWithChildren>
    }

    @Database(
        entities =
            [
                Student::class,
                Class::class,
                Enrollment::class,
                ClubMembership::class,
                SixKeyParent::class,
                SixKeyChild::class,
            ],
        version = 1,
        exportSchema = false,
    )
    abstract class MyDatabase : RoomDatabase() {
        abstract fun getDao(): MyDao
    }

    @Test
    fun testOneToMany() {
        val student = Student(1, 10, "Alice")
        val membership1 = ClubMembership(1, "Chess", 1, 10)
        val membership2 = ClubMembership(2, "Art", 1, 10)
        dao.insertStudent(student)
        dao.insertClubMembership(membership1)
        dao.insertClubMembership(membership2)

        val result = dao.getStudentsWithClubMemberships()
        assertThat(result).hasSize(1)
        assertThat(result.first().student).isEqualTo(student)
        assertThat(result.first().memberships).containsExactly(membership1, membership2)
    }

    @Test
    fun testManyToManyWithJunction() {
        val student1 = Student(1, 10, "Alice")
        val student2 = Student(2, 12, "Pedro")
        val clazz1 = Class(1, "Math")
        val clazz2 = Class(2, "History")
        val enrollment1 = Enrollment(1, 10, 1)
        val enrollment2 = Enrollment(1, 10, 2)
        val enrollment3 = Enrollment(2, 12, 2)

        dao.insertStudent(student1)
        dao.insertStudent(student2)
        dao.insertClass(clazz1)
        dao.insertClass(clazz2)
        dao.insertEnrollment(enrollment1)
        dao.insertEnrollment(enrollment2)
        dao.insertEnrollment(enrollment3)

        val result = dao.getStudentsWithClasses()
        assertThat(result).hasSize(2)
        assertThat(result[0].student).isEqualTo(student1)
        assertThat(result[0].classes).containsExactly(clazz1, clazz2)
        assertThat(result[1].student).isEqualTo(student2)
        assertThat(result[1].classes).containsExactly(clazz2)
    }

    @Test
    fun testSingleColumnProjection() {
        val student = Student(1, 10, "Alice")
        val clazz1 = Class(1, "Math")
        val clazz2 = Class(2, "History")
        val enrollment1 = Enrollment(1, 10, 1)
        val enrollment2 = Enrollment(1, 10, 2)

        dao.insertStudent(student)
        dao.insertClass(clazz1)
        dao.insertClass(clazz2)
        dao.insertEnrollment(enrollment1)
        dao.insertEnrollment(enrollment2)

        val result = dao.getStudentsWithClassNames()
        assertThat(result).hasSize(1)
        assertThat(result.first().student).isEqualTo(student)
        assertThat(result.first().classNames).containsExactly("Math", "History")
    }

    @Test
    fun testCustomDataClass() {
        val student = Student(1, 10, "Alice")
        val clazz1 = Class(1, "Math")
        val clazz2 = Class(2, "History")
        val enrollment1 = Enrollment(1, 10, 1)
        val enrollment2 = Enrollment(1, 10, 2)

        dao.insertStudent(student)
        dao.insertClass(clazz1)
        dao.insertClass(clazz2)
        dao.insertEnrollment(enrollment1)
        dao.insertEnrollment(enrollment2)

        val result = dao.getStudentsWithClassInfo()
        assertThat(result).hasSize(1)
        assertThat(result.first().student).isEqualTo(student)
        assertThat(result.first().classInfo)
            .containsExactly(ClassInfo("Math"), ClassInfo("History"))
    }

    @Test
    fun testQueryForManyParentsStaysUnderVariableLimit() {
        // Skipped with the Android driver
        // The query binds 911 parents * 6 key columns = 5,466 values
        // The bundled SQLite allows up to 32,766 per query
        // The device's SQLite on older Android versions allows only 999
        assumeTrue(useDriver == UseDriver.BUNDLED)
        val parents = (1L..911L).map { SixKeyParent(it, it, it, it, it, it) }
        val child = SixKeyChild(1, 5, 5, 5, 5, 5, 5)
        dao.insertSixKeyParents(parents)
        dao.insertSixKeyChildren(listOf(child))

        val result = dao.getSixKeyParentsWithChildren()
        assertThat(result).hasSize(911)
        assertThat(result.single { it.parent.k1 == 5L }.children).containsExactly(child)
        assertThat(result.filter { it.parent.k1 != 5L }.all { it.children.isEmpty() }).isTrue()
    }
}
