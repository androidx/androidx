/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.kruth.assertThat
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.TypeConverter
import androidx.room3.TypeConverters
import androidx.room3.integration.kotlintestapp.dao.RobotsDao
import androidx.room3.integration.kotlintestapp.vo.Hivemind
import androidx.room3.integration.kotlintestapp.vo.Robot
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.nio.ByteBuffer
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** A PoKo is not a tasty fish, it stands for Plain Old Kotlin Object. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PoKoTest {

    @Entity data class SampleEntity(@PrimaryKey val id: Int, val number: Int?, val text: String?)

    class SampleAvgNumber(@ColumnInfo(name = "AVG(number)") val avg: Int)

    @Entity data class SampleChild(@PrimaryKey val childId: Int, val parentNumber: Int)

    @Entity
    data class SampleItem(@PrimaryKey val itemId: Int, val ownerId: Int, val ownedSince: Date) {
        constructor(itemId: Int, ownerId: Int) : this(itemId, ownerId, Date())
    }

    class SampleParentWithChildren(
        @Embedded val parent: SampleEntity,
        @Relation(parentColumn = "number", entityColumn = "parentNumber")
        val children: Set<SampleChild>,
    )

    class SampleParentWithChild(
        @Embedded val parent: SampleEntity,
        @Relation(parentColumn = "number", entityColumn = "parentNumber") val child: SampleChild?,
    )

    class SampleParentWithChildrenIds(
        @Embedded val parent: SampleEntity,
        @Relation(
            entity = SampleChild::class,
            parentColumn = "number",
            entityColumn = "parentNumber",
            projection = ["childId"],
        )
        val childrenIds: List<Int>,
    )

    class SampleChildWithItems(
        @Embedded val child: SampleChild,
        @Relation(parentColumn = "childId", entityColumn = "ownerId") val items: List<SampleItem>,
    )

    class SampleParentWithChildrenAndItems(
        @Embedded val parent: SampleEntity,
        @Relation(
            entity = SampleChild::class,
            parentColumn = "number",
            entityColumn = "parentNumber",
        )
        val children: List<SampleChildWithItems>,
    )

    @Dao
    interface SampleDao {
        @Insert suspend fun insert(sampleEntity: SampleEntity)

        @Insert suspend fun insertChild(child: SampleChild)

        @Insert suspend fun insertItem(item: SampleItem)

        @Query("SELECT AVG(number) FROM SampleEntity") suspend fun getAvgNumber(): SampleAvgNumber

        @Transaction
        @Query("SELECT * FROM SampleEntity")
        suspend fun getAllParentWithChildren(): List<SampleParentWithChildren>

        @Transaction
        @Query("SELECT * FROM SampleEntity")
        suspend fun getAllParentWithSingleChild(): List<SampleParentWithChild>

        @Transaction
        @Query("SELECT * FROM SampleEntity")
        suspend fun getAllParentWithSingleChildId(): List<SampleParentWithChildrenIds>

        @Transaction
        @Query("SELECT * FROM SampleEntity")
        suspend fun getAllParentWithChildrenAndItems(): List<SampleParentWithChildrenAndItems>

        @Transaction
        @Query("SELECT * FROM SampleEntity UNION ALL SELECT * FROM SampleEntity")
        suspend fun unionByItself(): List<SampleParentWithChildren>
    }

    @Database(
        entities =
            [
                SampleEntity::class,
                SampleChild::class,
                SampleItem::class,
                Robot::class,
                Hivemind::class,
            ],
        version = 1,
        exportSchema = false,
    )
    @TypeConverters(DateConverter::class, UUIDConverter::class)
    abstract class PokoDatabase : RoomDatabase() {
        abstract fun dao(): SampleDao

        abstract fun robots(): RobotsDao
    }

    object DateConverter {
        @TypeConverter fun toDate(d: Long) = Date(d)

        @TypeConverter fun fromDate(d: Date) = d.time
    }

    object UUIDConverter {
        @TypeConverter
        fun asUuid(bytes: ByteArray): UUID {
            val bb = ByteBuffer.wrap(bytes)
            val firstLong = bb.long
            val secondLong = bb.long
            return UUID(firstLong, secondLong)
        }

        @TypeConverter
        fun asBytes(uuid: UUID): ByteArray {
            val bb = ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            return bb.array()
        }
    }

    private lateinit var db: PokoDatabase
    private lateinit var dao: SampleDao

    @Before
    fun setup() {
        db =
            Room.inMemoryDatabaseBuilder<PokoDatabase>(ApplicationProvider.getApplicationContext())
                .setDriver(AndroidSQLiteDriver())
                .build()
        dao = db.dao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun scalarFunctionInColumnName() = runTest {
        dao.insert(SampleEntity(1, 5, null))
        dao.insert(SampleEntity(2, 12, null))
        dao.insert(SampleEntity(3, 9, null))

        assertThat(dao.getAvgNumber().avg).isEqualTo(8)
    }

    @Test
    fun relationshipNullParentColumnKey() = runTest {
        dao.insert(SampleEntity(1, 5, null))
        dao.insert(SampleEntity(2, null, null))
        dao.insertChild(SampleChild(1, 5))
        dao.insertChild(SampleChild(2, 10))

        val result = dao.getAllParentWithChildren()
        assertThat(result).hasSize(2) // two parents

        // one child for the parent with the non-null relation key
        assertThat(result[0].parent.id).isEqualTo(1)
        assertThat(result[0].children).hasSize(1)

        // no children for the parent with the non-null relation key
        assertThat(result[1].parent.id).isEqualTo(2)
        assertThat(result[1].children).hasSize(0)
    }

    @Test
    fun relationParentColumnKeyBlob() {
        val robotsDao = db.robots()
        val hiveId1 = UUID.randomUUID()
        val hiveId2 = UUID.randomUUID()
        val robotId1 = UUID.randomUUID()
        val robotId2 = UUID.randomUUID()
        val robotId3 = UUID.randomUUID()
        robotsDao.putHivemind(Hivemind(hiveId1))
        robotsDao.putHivemind(Hivemind(hiveId2))
        robotsDao.putRobot(Robot(robotId1, hiveId1))
        robotsDao.putRobot(Robot(robotId2, hiveId1))
        robotsDao.putRobot(Robot(robotId3, hiveId2))

        val firstHiveRobots = robotsDao.getHiveRobots(hiveId1)
        assertThat(firstHiveRobots.size).isEqualTo(2)
        assertThat(firstHiveRobots.map { it.mId }).containsExactly(robotId1, robotId2)

        val secondHiveRobots = robotsDao.getHiveRobots(hiveId2)
        assertThat(secondHiveRobots.size).isEqualTo(1)
        assertThat(secondHiveRobots.map { it.mId }).containsExactly(robotId3)

        val cluster = robotsDao.getCluster()
        assertThat(cluster).hasSize(2)
        assertThat(cluster[0].mRobotList.map { it.mId }).containsExactly(robotId1, robotId2)
        assertThat(cluster[1].mRobotList.map { it.mId }).containsExactly(robotId3)
    }

    @Test
    fun relationshipNullableRelation() = runTest {
        dao.insert(SampleEntity(1, 5, null))
        dao.insert(SampleEntity(2, null, null))
        dao.insertChild(SampleChild(1, 5))

        val result = dao.getAllParentWithSingleChild()
        assertThat(result).hasSize(2) // two parents

        // parent with a child
        assertThat(result[0].parent.id).isEqualTo(1)
        assertThat(result[0].child).isNotNull()

        // parent with no child
        assertThat(result[1].parent.id).isEqualTo(2)
        assertThat(result[1].child).isNull()
    }

    @Test
    fun relationshipColumnProjection() = runTest {
        dao.insert(SampleEntity(1, 5, null))
        dao.insertChild(SampleChild(1, 5))
        dao.insertChild(SampleChild(2, 5))

        val result = dao.getAllParentWithSingleChildId()
        assertThat(result).hasSize(1)
        assertThat(result.single().parent.id).isEqualTo(1)
        assertThat(result.single().childrenIds).containsExactly(1, 2)
    }

    @Test
    fun relationshipNested() = runTest {
        dao.insert(SampleEntity(1, 5, null))
        dao.insertChild(SampleChild(1, 5))
        dao.insertChild(SampleChild(2, 5))
        dao.insertItem(SampleItem(1, 1))
        dao.insertItem(SampleItem(2, 1))
        dao.insertItem(SampleItem(3, 2))
        dao.insertItem(SampleItem(4, 2))

        val result = dao.getAllParentWithChildrenAndItems()
        assertThat(result).hasSize(1)
        assertThat(result.single().children).hasSize(2)
        assertThat(result.single().children.flatMap { it.items }).hasSize(4)
    }

    @Test
    fun relationshipDuplicateParentColumn() = runTest {
        dao.insert(SampleEntity(1, 5, null))
        dao.insert(SampleEntity(2, 10, null))
        dao.insertChild(SampleChild(1, 5))
        dao.insertChild(SampleChild(2, 10))

        val result = dao.unionByItself()
        assertThat(result).hasSize(4)

        assertThat(result[0].children.single().childId).isEqualTo(1)
        assertThat(result[2].children.single().childId).isEqualTo(1)

        assertThat(result[1].children.single().childId).isEqualTo(2)
        assertThat(result[3].children.single().childId).isEqualTo(2)
    }

    @Test
    fun relationshipTypeConverter() = runTest {
        val now = Date()
        dao.insert(SampleEntity(1, 5, null))
        dao.insertChild(SampleChild(1, 5))
        dao.insertItem(SampleItem(1, 1, now))

        val result = dao.getAllParentWithChildrenAndItems()
        assertThat(result).hasSize(1)
        assertThat(result.single().children.single().items.single().ownedSince).isEqualTo(now)
    }

    @Test
    fun relationshipLarge() = runTest {
        db.withWriteTransaction {
            dao.insert(SampleEntity(1, 5, null))
            repeat(2000) { i -> dao.insertChild(SampleChild(i, 5)) }
        }

        val result = dao.getAllParentWithChildren()
        assertThat(result).hasSize(1)
        assertThat(result.single().children).hasSize(2000)
    }
}
