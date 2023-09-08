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

package androidx.room.writer

import COMMON
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compileFiles
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

class DaoRelationshipKotlinCodeGenTest : BaseDaoKotlinCodeGenTest() {

    @get:Rule
    val testName = TestName()

    val databaseSrc = Source.kotlin(
        "MyDatabase.kt",
        """
        import androidx.room.*

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
        """.trimIndent()
    )

    @Test
    fun relations() {
        val src = Source.kotlin(
            "MyDao.kt",
            """
            import androidx.room.*

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
                @Relation(parentColumn = "artistKey", entityColumn = "artistId")
                val artist: Artist
            )

            data class ArtistAndSongs(
                @Embedded
                val artist: Artist,
                @Relation(parentColumn = "artistId", entityColumn = "artistKey")
                val songs: List<Song>
            )

            data class PlaylistAndSongs(
                @Embedded
                val playlist: Playlist,
                @Relation(
                    parentColumn = "playlistId",
                    entityColumn = "songId",
                    associateBy = Junction(
                        value = PlaylistSongXRef::class,
                        parentColumn = "playlistKey",
                        entityColumn = "songKey",
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
            """.trimIndent()
        )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun relations_nullable() {
        val src = Source.kotlin(
            "MyDao.kt",
            """
            import androidx.room.*

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
                @Relation(parentColumn = "artistKey", entityColumn = "artistId")
                val artist: Artist?
            )

            data class ArtistAndSongs(
                @Embedded
                val artist: Artist,
                @Relation(parentColumn = "artistId", entityColumn = "artistKey")
                val songs: List<Song>
            )

            data class PlaylistAndSongs(
                @Embedded
                val playlist: Playlist,
                @Relation(
                    parentColumn = "playlistId",
                    entityColumn = "songId",
                    associateBy = Junction(
                        value = PlaylistSongXRef::class,
                        parentColumn = "playlistKey",
                        entityColumn = "songKey",
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
            """.trimIndent()
        )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun relations_longSparseArray() {
        val src = Source.kotlin(
            "MyDao.kt",
            """
            import androidx.room.*

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
                @Relation(parentColumn = "artistKey", entityColumn = "artistId")
                val artist: Artist
            )

            data class ArtistAndSongs(
                @Embedded
                val artist: Artist,
                @Relation(parentColumn = "artistId", entityColumn = "artistKey")
                val songs: List<Song>
            )

            data class PlaylistAndSongs(
                @Embedded
                val playlist: Playlist,
                @Relation(
                    parentColumn = "playlistId",
                    entityColumn = "songId",
                    associateBy = Junction(
                        value = PlaylistSongXRef::class,
                        parentColumn = "playlistKey",
                        entityColumn = "songKey",
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
            """.trimIndent()
        )
        runTest(
            sources = listOf(src, databaseSrc),
            compiledFiles = compileFiles(listOf(COMMON.LONG_SPARSE_ARRAY)),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun relations_arrayMap() {
        val src = Source.kotlin(
            "MyDao.kt",
            """
            import androidx.room.*

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
                @Relation(parentColumn = "artistKey", entityColumn = "artistId")
                val artist: Artist
            )

            data class ArtistAndSongs(
                @Embedded
                val artist: Artist,
                @Relation(parentColumn = "artistId", entityColumn = "artistKey")
                val songs: List<Song>
            )

            data class PlaylistAndSongs(
                @Embedded
                val playlist: Playlist,
                @Relation(
                    parentColumn = "playlistId",
                    entityColumn = "songId",
                    associateBy = Junction(
                        value = PlaylistSongXRef::class,
                        parentColumn = "playlistKey",
                        entityColumn = "songKey",
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
            """.trimIndent()
        )
        runTest(
            sources = listOf(src, databaseSrc),
            compiledFiles = compileFiles(listOf(COMMON.ARRAY_MAP)),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun relations_byteBufferKey() {
        val src = Source.kotlin(
            "MyDao.kt",
            """
            import androidx.room.*

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
                @Relation(parentColumn = "artistKey", entityColumn = "artistId")
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
            """.trimIndent()
        )
        runTest(
            sources = listOf(src),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }
}
