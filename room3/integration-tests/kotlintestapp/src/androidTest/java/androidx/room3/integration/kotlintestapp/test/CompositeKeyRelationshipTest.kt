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
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class CompositeKeyRelationshipTest {
    private lateinit var db: MyDatabase
    private lateinit var dao: MyDao

    @Before
    fun createDb() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder<MyDatabase>(context)
                .setDriver(BundledSQLiteDriver())
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
    }

    @Database(
        entities = [Student::class, Class::class, Enrollment::class, ClubMembership::class],
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
}
