/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.integration.kotlintestapp.test

import android.content.Context
import androidx.kruth.assertThat
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomWarnings
import androidx.room.SkipQueryVerification
import androidx.room.Transaction
import androidx.test.core.app.ApplicationProvider
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMap
import java.nio.ByteBuffer
import org.junit.Before
import org.junit.Test

class AmbiguousColumnResolverTest {

    private lateinit var dao: TestDao

    private val user1 = User(1, "Juan")
    private val user2 = User(2, "Carmen")
    private val comment1 = Comment(1, 1, "")
    private val comment2 = Comment(2, 2, "")
    private val comment3 = Comment(3, 2, "")
    private val avatar1 = Avatar(1, "", ByteBuffer.allocate(0))

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TestDatabase::class.java).build()
        dao = db.getDao()
        dao.insertUser(user1)
        dao.insertUser(user2)
        dao.insertComment(comment1)
        dao.insertComment(comment2)
        dao.insertComment(comment3)
        dao.insertAvatar(avatar1)
    }

    @Test
    fun basicMultimap() {
        dao.getUserCommentMap().let { result ->
            assertThat(result[user1]).containsExactly(comment1)
            assertThat(result[user2]).containsExactly(comment2, comment3)
        }

        dao.getUserCommentMapWithoutStarProjection().let { result ->
            assertThat(result[user1]).containsExactly(comment1)
            assertThat(result[user2]).containsExactly(comment2, comment3)
        }
    }

    @Test
    fun guavaMultimap() {
        dao.getUserCommentImmutableMap().let { result ->
            assertThat(result[user1]).containsExactly(comment1)
            assertThat(result[user2]).containsExactly(comment2, comment3)
        }

        dao.getUserCommentMultimap().let { result ->
            assertThat(result[user1]).containsExactly(comment1)
            assertThat(result[user2]).containsExactly(comment2, comment3)
        }
    }

    @Test
    fun withoutVerification() {
        // Skipping verification forces Room to use an EntityRowAdapter
        dao.getUserCommentMapWithoutQueryVerification().let { result ->
            assertThat(result[user1]).containsExactly(comment1)
            assertThat(result[user2]).containsExactly(comment2, comment3)
        }
        dao.getCommentAvatarMapWithoutQueryVerification().let { result ->
            assertThat(result[comment1]).isEqualTo(avatar1)
        }
    }

    @Test
    fun withMapColumn() {
        dao.getUserIdAndComments().let { userIdAndComments ->
            assertThat(userIdAndComments[1]).containsExactly(comment1)
            assertThat(userIdAndComments[2]).containsExactly(comment2, comment3)
        }
        dao.getUserIdAndCommentsTableOrderSwapped().let { userIdAndComments ->
            assertThat(userIdAndComments[1]).containsExactly(comment1)
            assertThat(userIdAndComments[2]).containsExactly(comment2, comment3)
        }
        dao.getUserIdAliasedAndCommentsTableOrderSwapped().let { userIdAndComments ->
            assertThat(userIdAndComments[1]).containsExactly(comment1)
            assertThat(userIdAndComments[2]).containsExactly(comment2, comment3)
        }
        dao.getUserIdAndAmountOfComments().let { userIdAndComments ->
            assertThat(userIdAndComments[1]).isEqualTo(1)
            assertThat(userIdAndComments[2]).isEqualTo(2)
        }
    }

    @Test
    fun leftJoin() {
        // Verifies the 'value columns null check' also use the resolved column indices.
        val user3 = User(3, "Tom")
        dao.insertUser(user3)
        dao.getLeftJoinUserCommentMap().let { result -> assertThat(result[user3]).isEmpty() }
    }

    @Test
    fun withRelation() {
        dao.getUserAndAvatarCommentMap().let { result ->
            assertThat(result[UserAndAvatar(user1, avatar1)]).containsExactly(comment1)
            assertThat(result[UserAndAvatar(user2, null)]).containsExactly(comment2, comment3)
        }
    }

    @Test
    fun embeddedAliased() {
        val result = dao.getUserCommentEmbeddedAliased()
        assertThat(result)
            .containsExactly(
                UserAndCommentAliased(user1.id, user1.name, comment1.id, comment1.text),
                UserAndCommentAliased(user2.id, user2.name, comment2.id, comment2.text),
                UserAndCommentAliased(user2.id, user2.name, comment3.id, comment3.text),
            )
    }

    @Database(
        entities = [User::class, Comment::class, Avatar::class],
        version = 1,
        exportSchema = false
    )
    internal abstract class TestDatabase : RoomDatabase() {
        abstract fun getDao(): TestDao
    }

    @Dao
    interface TestDao {
        @Insert fun insertUser(user: User)

        @Insert fun insertComment(comment: Comment)

        @Insert fun insertAvatar(avatar: Avatar)

        @Query("SELECT * FROM User JOIN Comment ON User.id = Comment.userId")
        fun getUserCommentMap(): Map<User, List<Comment>>

        @Query("SELECT * FROM User JOIN Comment ON User.id = Comment.userId")
        fun getUserCommentImmutableMap(): ImmutableMap<User, List<Comment>>

        @Query("SELECT * FROM User JOIN Comment ON User.id = Comment.userId")
        fun getUserCommentMultimap(): ImmutableListMultimap<User, Comment>

        @SkipQueryVerification
        @Query("SELECT * FROM User JOIN Comment ON User.id = Comment.userId")
        fun getUserCommentMapWithoutQueryVerification(): Map<User, List<Comment>>

        @SkipQueryVerification
        @Query("SELECT * FROM Comment JOIN Avatar ON Comment.userId = Avatar.userId")
        fun getCommentAvatarMapWithoutQueryVerification(): Map<Comment, Avatar>

        @Query(
            """
            SELECT User.id, name, Comment.id, userId, text
            FROM User JOIN Comment ON User.id = Comment.userId
            """
        )
        fun getUserCommentMapWithoutStarProjection(): Map<User, List<Comment>>

        // This works because star projections are ordered from queried tables, but if the JOIN
        // is swapped it would return bad results, hence the AMBIGUOUS_COLUMN_IN_RESULT.
        // Suppress on QUERY_MISMATCH is because @RewriteQueriesToDropUnusedColumns does not
        // rewrite queries with duplicate columns.
        @Suppress(RoomWarnings.QUERY_MISMATCH, RoomWarnings.AMBIGUOUS_COLUMN_IN_RESULT)
        @Query("SELECT * FROM User JOIN Comment ON User.id = Comment.userId")
        fun getUserIdAndComments(): Map<@MapColumn("id") Int, List<Comment>>

        // This works because User.id is in the projection first, but if swapped with Comment.*
        // it would return bad results, hence the AMBIGUOUS_COLUMN_IN_RESULT.
        @Suppress(RoomWarnings.AMBIGUOUS_COLUMN_IN_RESULT)
        @Query("SELECT User.id, Comment.* FROM User JOIN Comment ON User.id = Comment.userId")
        fun getUserIdAndCommentsTableOrderSwapped(): Map<@MapColumn("id") Int, List<Comment>>

        // Aliasing the single ambiguous column is good.
        @Query(
            """
            SELECT Comment.*, User.id as user_id
            FROM User JOIN Comment ON User.id = Comment.userId
            """
        )
        fun getUserIdAliasedAndCommentsTableOrderSwapped():
            Map<@MapColumn("user_id") Int, List<Comment>>

        @Query(
            """
            SELECT User.id, count(*) AS commentsCount
            FROM User JOIN Comment ON User.id = Comment.userId
            GROUP BY User.id
            """
        )
        fun getUserIdAndAmountOfComments():
            Map<@MapColumn("id") Int, @MapColumn("commentsCount") Int>

        @Query("SELECT * FROM User LEFT JOIN Comment ON User.id = Comment.userId")
        fun getLeftJoinUserCommentMap(): Map<User, List<Comment>>

        @Query(
            "SELECT * FROM User JOIN Avatar ON User.id = Avatar.userId JOIN " +
                "Comment ON Avatar.userId = Comment.userId"
        )
        fun getLeftJoinUserNestedMap(): Map<User, Map<Avatar, List<Comment>>>

        @Transaction
        @Query("SELECT * FROM User JOIN Comment ON User.id = Comment.userId")
        fun getUserAndAvatarCommentMap(): Map<UserAndAvatar, List<Comment>>

        @Query(
            """
            SELECT User.id AS user_id, name, Comment.id AS comment_id, text
            FROM User JOIN Comment ON User.id = Comment.userId
            """
        )
        fun getUserCommentEmbeddedAliased(): List<UserAndCommentAliased>
    }

    @Entity
    data class User(
        @PrimaryKey val id: Int,
        val name: String,
    )

    @Entity
    data class Comment(
        @PrimaryKey val id: Int,
        val userId: Int,
        val text: String,
    )

    @Entity
    data class Avatar(
        @PrimaryKey val userId: Int,
        val url: String,
        val data: ByteBuffer,
    )

    data class UserAndAvatar(
        @Embedded val user: User,
        @Relation(parentColumn = "id", entityColumn = "userId") val avatar: Avatar?,
    )

    data class UserAndComment(@Embedded val user: User, @Embedded val comment: Comment)

    data class UserAndCommentAliased(
        @ColumnInfo(name = "user_id") val userId: Int,
        @ColumnInfo(name = "name") val name: String,
        @ColumnInfo(name = "comment_id") val commentId: Int,
        @ColumnInfo(name = "text") val text: String,
    )
}
