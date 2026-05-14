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

package androidx.room3.writer

import COMMON
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.compileFiles
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

class DaoRelationshipKotlinCodeGenTest : BaseDaoKotlinCodeGenTest() {

    @get:Rule val testName = TestName()

    val databaseSrc =
        Source.kotlin(
            "MyDatabase.kt",
            """
            import androidx.room3.*

            @Database(
                entities = [
                    Artist::class,
                    Song::class,
                    Playlist::class,
                    PlaylistSongXRef::class
                ],
                version = 1,
                exportSchema = false
            )
            abstract class MyDatabase : RoomDatabase() {
              abstract fun getDao(): MyDao
            }
            """
                .trimIndent(),
        )

    val compositeDatabaseSrc =
        Source.kotlin(
            "MyDatabase.kt",
            """
            import androidx.room3.*

            @Database(
                entities = [
                    Parent::class,
                    Child::class,
                    ParentChildXRef::class
                ],
                version = 1,
                exportSchema = false
            )
            abstract class MyDatabase : RoomDatabase() {
              abstract fun getDao(): MyDao
            }
            """
                .trimIndent(),
        )

    @Test
    fun relations() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
                import androidx.room3.*

                @Dao
                @Suppress(
                    RoomWarnings.RELATION_QUERY_WITHOUT_TRANSACTION,
                    RoomWarnings.MISSING_INDEX_ON_JUNCTION
                )
                interface MyDao {
                    // 1 to 1
                    @Query("SELECT * FROM Song")
                    fun getSongsWithArtist(): SongWithArtist

                    // 1 to many
                    @Query("SELECT * FROM Artist")
                    fun getArtistAndSongs(): ArtistAndSongs

                    // many to many
                    @Query("SELECT * FROM Playlist")
                    fun getPlaylistAndSongs(): PlaylistAndSongs
                }

                data class SongWithArtist(
                    @Embedded
                    val song: Song,
                    @Relation(parentColumns = ["artistKey"], entityColumns = ["artistId"])
                    val artist: Artist
                )

                data class ArtistAndSongs(
                    @Embedded
                    val artist: Artist,
                    @Relation(parentColumns = ["artistId"], entityColumns = ["artistKey"])
                    val songs: List<Song>
                )

                data class PlaylistAndSongs(
                    @Embedded
                    val playlist: Playlist,
                    @Relation(
                        parentColumns = ["playlistId"],
                        entityColumns = ["songId"],
                        associateBy = Junction(
                            value = PlaylistSongXRef::class,
                            parentColumns = ["playlistKey"],
                            entityColumns = ["songKey"],
                        )
                    )
                    val songs: List<Song>
                )

                @Entity
                data class Artist(
                    @PrimaryKey
                    val artistId: Long
                )

                @Entity
                data class Song(
                    @PrimaryKey
                    val songId: Long,
                    val artistKey: Long
                )

                @Entity
                data class Playlist(
                    @PrimaryKey
                    val playlistId: Long,
                )

                @Entity(primaryKeys = ["playlistKey", "songKey"])
                data class PlaylistSongXRef(
                    val playlistKey: Long,
                    val songKey: Long,
                )
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName),
        )
    }

    @Test
    fun relations_nullable() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
                import androidx.room3.*

                @Dao
                @Suppress(
                    RoomWarnings.RELATION_QUERY_WITHOUT_TRANSACTION,
                    RoomWarnings.MISSING_INDEX_ON_JUNCTION
                )
                interface MyDao {
                    // 1 to 1
                    @Query("SELECT * FROM Song")
                    fun getSongsWithArtist(): SongWithArtist

                    // 1 to many
                    @Query("SELECT * FROM Artist")
                    fun getArtistAndSongs(): ArtistAndSongs

                    // many to many
                    @Query("SELECT * FROM Playlist")
                    fun getPlaylistAndSongs(): PlaylistAndSongs
                }

                data class SongWithArtist(
                    @Embedded
                    val song: Song,
                    @Relation(parentColumns = ["artistKey"], entityColumns = ["artistId"])
                    val artist: Artist?
                )

                data class ArtistAndSongs(
                    @Embedded
                    val artist: Artist,
                    @Relation(parentColumns = ["artistId"], entityColumns = ["artistKey"])
                    val songs: List<Song>
                )

                data class PlaylistAndSongs(
                    @Embedded
                    val playlist: Playlist,
                    @Relation(
                        parentColumns = ["playlistId"],
                        entityColumns = ["songId"],
                        associateBy = Junction(
                            value = PlaylistSongXRef::class,
                            parentColumns = ["playlistKey"],
                            entityColumns = ["songKey"],
                        )
                    )
                    val songs: List<Song>
                )

                @Entity
                data class Artist(
                    @PrimaryKey
                    val artistId: Long
                )

                @Entity
                data class Song(
                    @PrimaryKey
                    val songId: Long,
                    val artistKey: Long?
                )

                @Entity
                data class Playlist(
                    @PrimaryKey
                    val playlistId: Long,
                )

                @Entity(primaryKeys = ["playlistKey", "songKey"])
                data class PlaylistSongXRef(
                    val playlistKey: Long,
                    val songKey: Long,
                )
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName),
        )
    }

    @Test
    fun relations_set() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
                import androidx.room3.*

                @Dao
                @Suppress(
                    RoomWarnings.RELATION_QUERY_WITHOUT_TRANSACTION,
                    RoomWarnings.MISSING_INDEX_ON_JUNCTION
                )
                interface MyDao {
                    // 1 to 1
                    @Query("SELECT * FROM Song")
                    fun getSongsWithArtist(): SongWithArtist

                    // 1 to many
                    @Query("SELECT * FROM Artist")
                    fun getArtistAndSongs(): ArtistAndSongs

                    // many to many
                    @Query("SELECT * FROM Playlist")
                    fun getPlaylistAndSongs(): PlaylistAndSongs
                }

                data class SongWithArtist(
                    @Embedded
                    val song: Song,
                    @Relation(parentColumns = ["artistKey"], entityColumns = ["artistId"])
                    val artist: Artist
                )

                data class ArtistAndSongs(
                    @Embedded
                    val artist: Artist,
                    @Relation(parentColumns = ["artistId"], entityColumns = ["artistKey"])
                    val songs: Set<Song>
                )

                data class PlaylistAndSongs(
                    @Embedded
                    val playlist: Playlist,
                    @Relation(
                        parentColumns = ["playlistId"],
                        entityColumns = ["songId"],
                        associateBy = Junction(
                            value = PlaylistSongXRef::class,
                            parentColumns = ["playlistKey"],
                            entityColumns = ["songKey"],
                        )
                    )
                    val songs: Set<Song>
                )

                @Entity
                data class Artist(
                    @PrimaryKey
                    val artistId: Long
                )

                @Entity
                data class Song(
                    @PrimaryKey
                    val songId: Long,
                    val artistKey: Long
                )

                @Entity
                data class Playlist(
                    @PrimaryKey
                    val playlistId: Long,
                )

                @Entity(primaryKeys = ["playlistKey", "songKey"])
                data class PlaylistSongXRef(
                    val playlistKey: Long,
                    val songKey: Long,
                )
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName),
        )
    }

    @Test
    fun relations_longSparseArray() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
                import androidx.room3.*

                @Dao
                @Suppress(
                    RoomWarnings.RELATION_QUERY_WITHOUT_TRANSACTION,
                    RoomWarnings.MISSING_INDEX_ON_JUNCTION
                )
                interface MyDao {
                    // 1 to 1
                    @Query("SELECT * FROM Song")
                    fun getSongsWithArtist(): SongWithArtist

                    // 1 to many
                    @Query("SELECT * FROM Artist")
                    fun getArtistAndSongs(): ArtistAndSongs

                    // many to many
                    @Query("SELECT * FROM Playlist")
                    fun getPlaylistAndSongs(): PlaylistAndSongs
                }

                data class SongWithArtist(
                    @Embedded
                    val song: Song,
                    @Relation(parentColumns = ["artistKey"], entityColumns = ["artistId"])
                    val artist: Artist
                )

                data class ArtistAndSongs(
                    @Embedded
                    val artist: Artist,
                    @Relation(parentColumns = ["artistId"], entityColumns = ["artistKey"])
                    val songs: List<Song>
                )

                data class PlaylistAndSongs(
                    @Embedded
                    val playlist: Playlist,
                    @Relation(
                        parentColumns = ["playlistId"],
                        entityColumns = ["songId"],
                        associateBy = Junction(
                            value = PlaylistSongXRef::class,
                            parentColumns = ["playlistKey"],
                            entityColumns = ["songKey"],
                        )
                    )
                    val songs: List<Song>
                )

                @Entity
                data class Artist(
                    @PrimaryKey
                    val artistId: Long
                )

                @Entity
                data class Song(
                    @PrimaryKey
                    val songId: Long,
                    val artistKey: Long
                )

                @Entity
                data class Playlist(
                    @PrimaryKey
                    val playlistId: Long,
                )

                @Entity(primaryKeys = ["playlistKey", "songKey"])
                data class PlaylistSongXRef(
                    val playlistKey: Long,
                    val songKey: Long,
                )
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(src, databaseSrc),
            compiledFiles = compileFiles(listOf(COMMON.LONG_SPARSE_ARRAY)),
            expectedFilePath = getTestGoldenPath(testName.methodName),
        )
    }

    @Test
    fun relations_arrayMap() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
                import androidx.room3.*

                @Dao
                @Suppress(
                    RoomWarnings.RELATION_QUERY_WITHOUT_TRANSACTION,
                    RoomWarnings.MISSING_INDEX_ON_JUNCTION
                )
                interface MyDao {
                    // 1 to 1
                    @Query("SELECT * FROM Song")
                    fun getSongsWithArtist(): SongWithArtist

                    // 1 to many
                    @Query("SELECT * FROM Artist")
                    fun getArtistAndSongs(): ArtistAndSongs

                    // many to many
                    @Query("SELECT * FROM Playlist")
                    fun getPlaylistAndSongs(): PlaylistAndSongs
                }

                data class SongWithArtist(
                    @Embedded
                    val song: Song,
                    @Relation(parentColumns = ["artistKey"], entityColumns = ["artistId"])
                    val artist: Artist
                )

                data class ArtistAndSongs(
                    @Embedded
                    val artist: Artist,
                    @Relation(parentColumns = ["artistId"], entityColumns = ["artistKey"])
                    val songs: List<Song>
                )

                data class PlaylistAndSongs(
                    @Embedded
                    val playlist: Playlist,
                    @Relation(
                        parentColumns = ["playlistId"],
                        entityColumns = ["songId"],
                        associateBy = Junction(
                            value = PlaylistSongXRef::class,
                            parentColumns = ["playlistKey"],
                            entityColumns = ["songKey"],
                        )
                    )
                    val songs: List<Song>
                )

                @Entity
                data class Artist(
                    @PrimaryKey
                    val artistId: Long
                )

                @Entity
                data class Song(
                    @PrimaryKey
                    val songId: Long,
                    val artistKey: Long
                )

                @Entity
                data class Playlist(
                    @PrimaryKey
                    val playlistId: Long,
                )

                @Entity(primaryKeys = ["playlistKey", "songKey"])
                data class PlaylistSongXRef(
                    val playlistKey: Long,
                    val songKey: Long,
                )
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(src, databaseSrc),
            compiledFiles = compileFiles(listOf(COMMON.ARRAY_MAP)),
            expectedFilePath = getTestGoldenPath(testName.methodName),
        )
    }

    @Test
    fun relations_byteBufferKey() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
                import androidx.room3.*

                @Database(
                    entities = [Artist::class, Song::class],
                    version = 1,
                    exportSchema = false
                )
                abstract class MyDatabase : RoomDatabase() {
                  abstract fun getDao(): MyDao
                }

                @Dao
                @Suppress(
                    RoomWarnings.RELATION_QUERY_WITHOUT_TRANSACTION,
                    RoomWarnings.MISSING_INDEX_ON_JUNCTION
                )
                // To validate ByteBuffer converter is forced
                @TypeConverters(
                    builtInTypeConverters = BuiltInTypeConverters(
                        byteBuffer = BuiltInTypeConverters.State.DISABLED
                    )
                )
                interface MyDao {
                    @Query("SELECT * FROM Song")
                    fun getSongsWithArtist(): SongWithArtist
                }

                data class SongWithArtist(
                    @Embedded
                    val song: Song,
                    @Relation(parentColumns = ["artistKey"], entityColumns = ["artistId"])
                    val artist: Artist
                )

                @Entity
                data class Artist(
                    @PrimaryKey
                    val artistId: ByteArray
                )

                @Entity
                data class Song(
                    @PrimaryKey
                    val songId: Long,
                    val artistKey: ByteArray
                )
                """
                    .trimIndent(),
            )
        runTest(sources = listOf(src), expectedFilePath = getTestGoldenPath(testName.methodName))
    }

    @Test
    fun relations_composite_pair() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
                import androidx.room3.*

                @Dao
                @Suppress(
                    RoomWarnings.RELATION_QUERY_WITHOUT_TRANSACTION,
                    RoomWarnings.MISSING_INDEX_ON_JUNCTION
                )
                interface MyDao {
                    @Query("SELECT * FROM Parent")
                    fun getParentWithChild(): ParentWithChild

                    @Query("SELECT * FROM Parent")
                    fun getParentWithChildren(): ParentWithChildren

                    @Query("SELECT * FROM Parent")
                    fun getParentWithChildrenJunction(): ParentWithChildrenJunction
                }

                data class ParentWithChild(
                    @Embedded
                    val parent: Parent,
                    @Relation(
                        parentColumns = ["p1", "p2"],
                        entityColumns = ["c1", "c2"]
                    )
                    val child: Child
                )

                data class ParentWithChildren(
                    @Embedded
                    val parent: Parent,
                    @Relation(
                        parentColumns = ["p1", "p2"],
                        entityColumns = ["c1", "c2"]
                    )
                    val children: List<Child>
                )

                data class ParentWithChildrenJunction(
                    @Embedded
                    val parent: Parent,
                    @Relation(
                        parentColumns = ["p1", "p2"],
                        entityColumns = ["c1", "c2"],
                        associateBy = Junction(
                            ParentChildXRef::class,
                            parentColumns = ["pk1", "pk2"],
                            entityColumns = ["ck1", "ck2"]
                        )
                    )
                    val children: List<Child>
                )

                @Entity(primaryKeys = ["p1", "p2"])
                data class Parent(
                    val p1: Long,
                    val p2: String
                )

                @Entity(primaryKeys = ["c1", "c2"])
                data class Child(
                    val c1: Long,
                    val c2: String,
                )

                @Entity(primaryKeys = ["pk1", "pk2", "ck1", "ck2"])
                data class ParentChildXRef(
                    val pk1: Long,
                    val pk2: String,
                    val ck1: Long,
                    val ck2: String
                )
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(src, compositeDatabaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName),
        )
    }

    @Test
    fun relations_composite_triple() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
                import androidx.room3.*

                @Dao
                @Suppress(
                    RoomWarnings.RELATION_QUERY_WITHOUT_TRANSACTION,
                    RoomWarnings.MISSING_INDEX_ON_JUNCTION
                )
                interface MyDao {
                    @Query("SELECT * FROM Parent")
                    fun getParentWithChild(): ParentWithChild

                    @Query("SELECT * FROM Parent")
                    fun getParentWithChildren(): ParentWithChildren

                    @Query("SELECT * FROM Parent")
                    fun getParentWithChildrenJunction(): ParentWithChildrenJunction
                }

                data class ParentWithChild(
                    @Embedded
                    val parent: Parent,
                    @Relation(
                        parentColumns = ["p1", "p2", "p3"],
                        entityColumns = ["c1", "c2", "c3"]
                    )
                    val child: Child
                )

                data class ParentWithChildren(
                    @Embedded
                    val parent: Parent,
                    @Relation(
                        parentColumns = ["p1", "p2", "p3"],
                        entityColumns = ["c1", "c2", "c3"]
                    )
                    val children: List<Child>
                )

                data class ParentWithChildrenJunction(
                    @Embedded
                    val parent: Parent,
                    @Relation(
                        parentColumns = ["p1", "p2", "p3"],
                        entityColumns = ["c1", "c2", "c3"],
                        associateBy = Junction(
                            ParentChildXRef::class,
                            parentColumns = ["pk1", "pk2", "pk3"],
                            entityColumns = ["ck1", "ck2", "ck3"]
                        )
                    )
                    val children: List<Child>
                )

                @Entity(primaryKeys = ["p1", "p2", "p3"])
                data class Parent(
                    val p1: String,
                    val p2: String,
                    val p3: String
                )

                @Entity(primaryKeys = ["c1", "c2", "c3"])
                data class Child(
                    val c1: String,
                    val c2: String,
                    val c3: String,
                )

                @Entity(primaryKeys = [
                    "pk1",
                    "pk2",
                    "pk3",
                    "ck1",
                    "ck2",
                    "ck3"
                    ]
                )
                data class ParentChildXRef(
                    val pk1: String,
                    val pk2: String,
                    val pk3: String,
                    val ck1: String,
                    val ck2: String,
                    val ck3: String
                )
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(src, compositeDatabaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName),
        )
    }

    @Test
    fun relations_composite_list() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
                import androidx.room3.*

                @Dao
                @Suppress(
                    RoomWarnings.RELATION_QUERY_WITHOUT_TRANSACTION,
                    RoomWarnings.MISSING_INDEX_ON_JUNCTION
                )
                interface MyDao {
                    @Query("SELECT * FROM Parent")
                    fun getParentWithChild(): ParentWithChild

                    @Query("SELECT * FROM Parent")
                    fun getParentWithChildren(): ParentWithChildren

                    @Query("SELECT * FROM Parent")
                    fun getParentWithChildrenJunction(): ParentWithChildrenJunction
                }

                data class ParentWithChild(
                    @Embedded
                    val parent: Parent,
                    @Relation(
                        parentColumns = ["p1", "p2", "p3", "p4"],
                        entityColumns = ["c1", "c2", "c3", "c4"]
                    )
                    val child: Child
                )

                data class ParentWithChildren(
                    @Embedded
                    val parent: Parent,
                    @Relation(
                        parentColumns = ["p1", "p2", "p3", "p4"],
                        entityColumns = ["c1", "c2", "c3", "c4"]
                    )
                    val children: List<Child>
                )

                data class ParentWithChildrenJunction(
                    @Embedded
                    val parent: Parent,
                    @Relation(
                        parentColumns = ["p1", "p2", "p3", "p4"],
                        entityColumns = ["c1", "c2", "c3", "c4"],
                        associateBy = Junction(
                            ParentChildXRef::class,
                            parentColumns = ["pk1", "pk2", "pk3", "pk4"],
                            entityColumns = ["ck1", "ck2", "ck3", "ck4"]
                        )
                    )
                    val children: List<Child>
                )

                @Entity(primaryKeys = ["p1", "p2", "p3", "p4"])
                data class Parent(
                    val p1: Long,
                    val p2: Int,
                    val p3: Long,
                    val p4: String
                )

                @Entity(primaryKeys = ["c1", "c2", "c3", "c4"])
                data class Child(
                    val c1: Long,
                    val c2: Int,
                    val c3: Long,
                    val c4: String,
                )

                @Entity(primaryKeys = ["pk1", "pk2", "pk3", "pk4", "ck1", "ck2", "ck3", "ck4"])
                data class ParentChildXRef(
                    val pk1: Long,
                    val pk2: Int,
                    val pk3: Long,
                    val pk4: String,
                    val ck1: Long,
                    val ck2: Int,
                    val ck3: Long,
                    val ck4: String
                )
                """
                    .trimIndent(),
            )
        runTest(
            sources = listOf(src, compositeDatabaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName),
        )
    }
}
