/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.collection.ArrayMap
import androidx.collection.LongSparseArray
import androidx.collection.SparseArrayCompat
import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.room.Room
import androidx.room.integration.kotlintestapp.TestDatabase
import androidx.room.integration.kotlintestapp.dao.MusicDao
import androidx.room.integration.kotlintestapp.vo.Album
import androidx.room.integration.kotlintestapp.vo.AlbumNameAndBandName
import androidx.room.integration.kotlintestapp.vo.AlbumWithSongs
import androidx.room.integration.kotlintestapp.vo.Artist
import androidx.room.integration.kotlintestapp.vo.Image
import androidx.room.integration.kotlintestapp.vo.ImageFormat
import androidx.room.integration.kotlintestapp.vo.ReleasedAlbum
import androidx.room.integration.kotlintestapp.vo.Song
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSetMultimap
import io.reactivex.Flowable
import java.nio.ByteBuffer
import java.util.Date
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests multimap return type for JOIN statements.
 *
 * Deprecation has been suppressed for @MapInfo. We still need these tests, but the annotation
 * is deprecated.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class MultimapQueryTest {
    private lateinit var mMusicDao: MusicDao
    private val mRhcpSong1: Song = Song(
        1,
        "Dani California",
        "Red Hot Chili Peppers",
        "Stadium Arcadium",
        442,
        2006
    )
    private val mRhcpSong2: Song = Song(
        2,
        "Snow (Hey Oh)",
        "Red Hot Chili Peppers",
        "Stadium Arcadium",
        514,
        2006
    )
    private val mAcdcSong1: Song = Song(
        3,
        "Highway to Hell",
        "AC/DC",
        "Highway to Hell",
        328,
        1979
    )
    private val mPinkFloydSong1: Song = Song(
        4,
        "The Great Gig in the Sky",
        "Pink Floyd",
        "The Dark Side of the Moon",
        443,
        1973
    )
    private val mRhcpSong3: Song = Song(
        5,
        "Parallel Universe",
        "Red Hot Chili Peppers",
        "Californication",
        529,
        1999
    )
    private val mRhcp: Artist = Artist(
        1,
        "Red Hot Chili Peppers",
        true
    )
    private val mAcDc: Artist = Artist(
        2,
        "AC/DC",
        true
    )
    private val mTheClash: Artist = Artist(
        3,
        "The Clash",
        false
    )
    private val mPinkFloyd: Artist = Artist(
        4,
        "Pink Floyd",
        false
    )
    private val mGlassAnimals: Artist = Artist(
        5,
        "Glass Animals",
        true
    )
    private val mStadiumArcadium: Album = Album(
        1,
        "Stadium Arcadium",
        "Red Hot Chili Peppers",
        2006,
        "N/A"
    )
    private val mCalifornication: Album = Album(
        2,
        "Californication",
        "Red Hot Chili Peppers",
        1999,
        "N/A"
    )
    private val mHighwayToHell: Album = Album(
        3,
        "Highway to Hell",
        "AC/DC",
        1979,
        null
    )
    private val mTheDarkSideOfTheMoon: Album = Album(
        4,
        "The Dark Side of the Moon",
        "Pink Floyd",
        1973,
        "N/A"
    )
    private val mDreamland: Album = Album(
        5,
        "Dreamland",
        null,
        2020,
        null
    )
    private val mPinkFloydAlbumCover: Image = Image(
        1,
        1973L,
        "Pink Floyd",
        "dark_side_of_the_moon_image".toByteArray(),
        Date(101779200000L),
        ImageFormat.JPG
    )
    private val mRhcpAlbumCover: Image = Image(
        2,
        2006L,
        "Red Hot Chili Peppers",
        "stadium_arcadium_image".toByteArray(),
        Date(1146787200000L),
        ImageFormat.MPEG
    )

    private val mTheClashAlbumCover: Image = Image(
        3,
        1979L,
        "The Clash",
        "london_calling_image".toByteArray(),
        Date(11873445200000L),
        ImageFormat.MPEG
    )

    @JvmField
    @Rule
    var mExecutorRule = CountingTaskExecutorRule()
    @Throws(TimeoutException::class, InterruptedException::class)
    private fun drain() {
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES)
        assertThat(mExecutorRule.isIdle).isTrue()
    }

    private open inner class MyTestObserver<T> : TestObserver<T>() {
        @Throws(TimeoutException::class, InterruptedException::class)
        override fun drain() {
            this@MultimapQueryTest.drain()
        }
    }

    @Before
    fun createDb() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db: TestDatabase =
            Room.inMemoryDatabaseBuilder(context, TestDatabase::class.java)
                .build()
        mMusicDao = db.musicDao()
    }

    /**
     * Tests a simple JOIN query between two tables.
     */
    @Test
    fun testGetFirstSongForArtist() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMap: Map<Artist, Song> = mMusicDao.getArtistAndFirstSongMap()
        assertThat(artistToSongsMap[mAcDc]).isEqualTo(mAcdcSong1)
        assertThat(artistToSongsMap[mRhcp]).isEqualTo(mRhcpSong1)
    }

    @Test
    fun testGetSongToArtistMapping() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val songToArtistMap: Map<Song, Artist> = mMusicDao.getSongAndArtist()
        assertThat(songToArtistMap[mAcdcSong1]).isEqualTo(mAcDc)
        assertThat(songToArtistMap[mPinkFloydSong1]).isEqualTo(mPinkFloyd)
        assertThat(songToArtistMap[mRhcpSong1]).isEqualTo(mRhcp)
        assertThat(songToArtistMap[mRhcpSong2]).isEqualTo(mRhcp)
    }

    @Test
    fun testJoinByArtistNameList() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMap: Map<Artist, List<Song>> = mMusicDao.getAllArtistAndTheirSongsList()
        assertContentsOfResultMapWithList(artistToSongsMap)
    }

    @Test
    fun testJoinByArtistNameListOrdered() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)

        assertThat(mMusicDao.getAllArtistAndTheirSongsListOrdered().keys).containsExactlyElementsIn(
            arrayOf(mRhcp, mAcDc, mPinkFloyd)
        )
    }

    @Test
    fun testJoinByArtistNameSet() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsSet: Map<Artist, Set<Song>> = mMusicDao.getAllArtistAndTheirSongsSet()
        assertContentsOfResultMapWithSet(artistToSongsSet)
    }

    /**
     * Tests a JOIN using [androidx.room.RawQuery] between two tables.
     */
    @Test
    fun testJoinByArtistNameRawQuery() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMap: Map<Artist, Song> = mMusicDao.getAllArtistAndTheirSongsRawQuery(
            SimpleSQLiteQuery(
                "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist"
            )
        )
        assertThat(artistToSongsMap[mAcDc]).isEqualTo(mAcdcSong1)
    }

    @Test
    fun testJoinByArtistNameRawQueryList() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMap: Map<Artist, List<Song>> =
            mMusicDao.getAllArtistAndTheirSongsRawQueryList(
                SimpleSQLiteQuery(
                    "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist"
                )
            )
        assertContentsOfResultMapWithList(artistToSongsMap)
    }

    @Test
    fun testJoinByArtistNameRawQuerySet() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMap: Map<Artist, Set<Song>> =
            mMusicDao.getAllArtistAndTheirSongsRawQuerySet(
                SimpleSQLiteQuery(
                    "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist"
                )
            )
        assertContentsOfResultMapWithSet(artistToSongsMap)
    }

    /**
     * Tests a simple JOIN query between two tables with a [LiveData] map return type.
     */
    @Test
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testJoinByArtistNameLiveData() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMapLiveData: LiveData<Map<Artist, Song>> =
            mMusicDao.getAllArtistAndTheirSongsAsLiveData()
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer: TestObserver<Map<Artist, Song>> = MyTestObserver()
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer)
        assertThat(observer.hasValue()).isFalse()
        observer.reset()
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(observer.get()).isNotNull()
        assertThat(observer.get()?.get(mAcDc)).isEqualTo(mAcdcSong1)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testJoinByArtistNameLiveDataList() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMapLiveData: LiveData<Map<Artist, List<Song>>> =
            mMusicDao.getAllArtistAndTheirSongsAsLiveDataList()
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer: TestObserver<Map<Artist, List<Song>>> = MyTestObserver()
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer)
        assertThat(observer.hasValue()).isFalse()
        observer.reset()
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(observer.get()).isNotNull()
        assertContentsOfResultMapWithList(observer.get()!!)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testJoinByArtistNameLiveDataSet() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMapLiveData: LiveData<Map<Artist, Set<Song>>> =
            mMusicDao.allArtistAndTheirSongsAsLiveDataSet()
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer: TestObserver<Map<Artist, Set<Song>>> = MyTestObserver()
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer)
        assertThat(observer.hasValue()).isFalse()
        observer.reset()
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(observer.get()).isNotNull()
        assertContentsOfResultMapWithSet(observer.get()!!)
    }

    /**
     * Tests a simple JOIN query between two tables with a [Flowable] map return type.
     */
    @Test
    fun testJoinByArtistNameFlowableList() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMapFlowable: Flowable<Map<Artist, List<Song>>> =
            mMusicDao.getAllArtistAndTheirSongsAsFlowableList()
        assertContentsOfResultMapWithList(artistToSongsMapFlowable.blockingFirst())
    }

    @Test
    fun testJoinByArtistNameFlowableSet() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMapFlowable: Flowable<Map<Artist, Set<Song>>> =
            mMusicDao.allArtistAndTheirSongsAsFlowableSet()
        assertContentsOfResultMapWithSet(artistToSongsMapFlowable.blockingFirst())
    }

    /**
     * Tests a simple JOIN query between two tables with a return type of a map with a key that
     * is an entity [Artist] and a POJO [AlbumWithSongs] that use
     * [androidx.room.Embedded] and [androidx.room.Relation].
     */
    @Test
    fun testPojoWithEmbeddedAndRelation() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val artistToAlbumsWithSongsMap: Map<Artist, AlbumWithSongs> =
            mMusicDao.getAllArtistAndTheirAlbumsWithSongs()
        val rhcpAlbum: AlbumWithSongs? = artistToAlbumsWithSongsMap[mRhcp]

        assertThat(rhcpAlbum).isNotNull()
        assertThat(artistToAlbumsWithSongsMap.keys).containsExactlyElementsIn(
            arrayOf(mRhcp, mAcDc, mPinkFloyd)
        )
        assertThat(artistToAlbumsWithSongsMap.containsKey(mTheClash)).isFalse()
        assertThat(artistToAlbumsWithSongsMap[mPinkFloyd]?.album)
            .isEqualTo(mTheDarkSideOfTheMoon)
        assertThat(artistToAlbumsWithSongsMap[mAcDc]?.album)
            .isEqualTo(mHighwayToHell)
        assertThat(artistToAlbumsWithSongsMap[mAcDc]?.songs?.get(0)).isEqualTo(mAcdcSong1)
        if (rhcpAlbum?.album?.equals(mStadiumArcadium) == true) {
            assertThat(rhcpAlbum.songs).containsExactlyElementsIn(
                listOf(mRhcpSong1, mRhcpSong2)
            )
        } else if (rhcpAlbum?.album?.equals(mCalifornication) == true) {
            assertThat(rhcpAlbum.songs).isEmpty()
        } else {
            Assert.fail()
        }
    }

    /**
     * Tests a simple JOIN query between two tables with a return type of a map with a key that
     * is an entity [Artist] and a list of entity POJOs [AlbumWithSongs] that use
     * [androidx.room.Embedded] and [androidx.room.Relation].
     */
    @Test
    fun testPojoWithEmbeddedAndRelationList() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val artistToAlbumsWithSongsMap: Map<Artist, List<AlbumWithSongs>> =
            mMusicDao.getAllArtistAndTheirAlbumsWithSongsList()
        mMusicDao.getAllArtistAndTheirAlbumsWithSongs()
        val rhcpList: List<AlbumWithSongs> = artistToAlbumsWithSongsMap[mRhcp]!!
        assertThat(artistToAlbumsWithSongsMap.keys).containsExactlyElementsIn(
            listOf<Any>(mRhcp, mAcDc, mPinkFloyd)
        )
        assertThat(artistToAlbumsWithSongsMap.containsKey(mTheClash)).isFalse()
        assertThat(artistToAlbumsWithSongsMap[mPinkFloyd]?.single()?.album)
            .isEqualTo(mTheDarkSideOfTheMoon)
        assertThat(artistToAlbumsWithSongsMap[mAcDc]?.single()?.album)
            .isEqualTo(mHighwayToHell)
        assertThat(artistToAlbumsWithSongsMap[mAcDc]?.single()?.songs?.get(0))
            .isEqualTo(mAcdcSong1)
        for (albumAndSong in rhcpList) {
            when (albumAndSong.album) {
                mStadiumArcadium -> {
                    assertThat(albumAndSong.songs).containsExactlyElementsIn(
                        listOf(mRhcpSong1, mRhcpSong2)
                    )
                }
                mCalifornication -> {
                    assertThat(albumAndSong.songs).isEmpty()
                }
                else -> {
                    Assert.fail()
                }
            }
        }
    }

    /**
     * Tests a simple JOIN query between two tables with a return type of a map with a key
     * [ReleasedAlbum] and value (list of [AlbumNameAndBandName]) that are non-entity
     * POJOs.
     */
    @Test
    fun testNonEntityPojosList() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val map: Map<ReleasedAlbum, List<AlbumNameAndBandName>> =
            mMusicDao.getReleaseYearToAlbumsAndBandsList()
        val allReleasedAlbums: Set<ReleasedAlbum> = map.keys
        assertThat(allReleasedAlbums.size).isEqualTo(3)
        allReleasedAlbums.forEach { album ->
            when (album.mAlbumName) {
                mStadiumArcadium.mAlbumName -> {
                    assertThat(album.mReleaseYear).isEqualTo(
                        mStadiumArcadium.mAlbumReleaseYear
                    )
                    val resultList = map[album] ?: emptyList()
                    assertThat(resultList.size).isEqualTo(2)
                    assertThat(resultList[0].mBandName)
                        .isEqualTo(mRhcp.mArtistName)
                    assertThat(resultList[0].mAlbumName)
                        .isEqualTo(mStadiumArcadium.mAlbumName)
                    assertThat(resultList[1].mBandName)
                        .isEqualTo(mRhcp.mArtistName)
                    assertThat(map[album]!![1].mAlbumName)
                        .isEqualTo(mStadiumArcadium.mAlbumName)
                }
                mHighwayToHell.mAlbumName -> {
                    assertThat(album.mReleaseYear).isEqualTo(mHighwayToHell.mAlbumReleaseYear)
                    val resultList = map[album] ?: emptyList()
                    assertThat(resultList.size).isEqualTo(1)
                    assertThat(resultList[0].mBandName)
                        .isEqualTo(mAcDc.mArtistName)
                    assertThat(resultList[0].mAlbumName)
                        .isEqualTo(mHighwayToHell.mAlbumName)
                }
                mTheDarkSideOfTheMoon.mAlbumName -> {
                    assertThat(album.mReleaseYear)
                        .isEqualTo(mTheDarkSideOfTheMoon.mAlbumReleaseYear)
                    val resultList = map[album] ?: emptyList()
                    assertThat(resultList.size).isEqualTo(1)
                    assertThat(resultList[0].mBandName).isEqualTo(mPinkFloyd.mArtistName)
                    assertThat(resultList[0].mAlbumName).isEqualTo(mTheDarkSideOfTheMoon.mAlbumName)
                }
                else -> {
                    // Shouldn't get here as we expect only the 3 albums to be keys in the map
                    Assert.fail()
                }
            }
        }
    }

    @Test
    fun testJoinByArtistNameGuavaImmutableListMultimap() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongs: ImmutableListMultimap<Artist, Song> =
            mMusicDao.allArtistAndTheirSongsGuavaImmutableListMultimap()
        assertContentsOfResultMultimap(artistToSongs)
    }

    @Test
    fun testJoinByArtistNameGuavaImmutableSetMultimap() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongs: ImmutableSetMultimap<Artist, Song> =
            mMusicDao.allArtistAndTheirSongsGuavaImmutableSetMultimap()
        assertContentsOfResultMultimap(artistToSongs)
    }

    @Test
    fun testJoinByArtistNameRawQueryGuavaImmutableListMultimap() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMap: ImmutableListMultimap<Artist, Song> =
            mMusicDao.getAllArtistAndTheirSongsRawQueryGuavaImmutableListMultimap(
                SimpleSQLiteQuery(
                    "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song" +
                        ".mArtist"
                )
            )
        assertThat(artistToSongsMap[mAcDc]).containsExactly(mAcdcSong1)
    }

    @Test
    fun testJoinByArtistNameRawQueryGuavaImmutableSetMultimap() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMap: ImmutableSetMultimap<Artist, Song> =
            mMusicDao.getAllArtistAndTheirSongsRawQueryGuavaImmutableSetMultimap(
                SimpleSQLiteQuery(
                    "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song" +
                        ".mArtist"
                )
            )
        assertThat(artistToSongsMap[mAcDc]).containsExactly(mAcdcSong1)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testJoinByArtistNameLiveDataGuavaImmutableListMultimap() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMapLiveData: LiveData<ImmutableListMultimap<Artist, Song>> =
            mMusicDao.allArtistAndTheirSongsAsLiveDataGuavaImmutableListMultimap()
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer: TestObserver<ImmutableListMultimap<Artist, Song>> = MyTestObserver()
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer)
        assertThat(observer.hasValue()).isFalse()
        observer.reset()
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(observer.get()).isNotNull()
        assertContentsOfResultMultimap(observer.get()!!)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testJoinByArtistNameLiveDataGuavaImmutableSetMultimap() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMapLiveData: LiveData<ImmutableSetMultimap<Artist, Song>> =
            mMusicDao.allArtistAndTheirSongsAsLiveDataGuavaImmutableSetMultimap()
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer: TestObserver<ImmutableSetMultimap<Artist, Song>> = MyTestObserver()
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer)
        assertThat(observer.hasValue()).isFalse()
        observer.reset()
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(observer.get()).isNotNull()
        assertContentsOfResultMultimap(observer.get()!!)
    }

    @Test
    fun testPojoWithEmbeddedAndRelationGuavaImmutableListMultimap() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val artistToAlbumsWithSongsMap: ImmutableListMultimap<Artist, AlbumWithSongs> =
            mMusicDao.allArtistAndTheirAlbumsWithSongsGuavaImmutableListMultimap()
        val rhcpList: ImmutableList<AlbumWithSongs> = artistToAlbumsWithSongsMap[mRhcp]
        assertThat(artistToAlbumsWithSongsMap.keySet()).containsExactlyElementsIn(
            listOf<Any>(mRhcp, mAcDc, mPinkFloyd)
        )
        assertThat(artistToAlbumsWithSongsMap.containsKey(mTheClash)).isFalse()
        assertThat(artistToAlbumsWithSongsMap[mPinkFloyd][0].album)
            .isEqualTo(mTheDarkSideOfTheMoon)
        assertThat(artistToAlbumsWithSongsMap[mAcDc][0].album)
            .isEqualTo(mHighwayToHell)
        assertThat(
            artistToAlbumsWithSongsMap[mAcDc][0].songs[0]
        ).isEqualTo(mAcdcSong1)
        for (albumAndSong in rhcpList) {
            when (albumAndSong.album) {
                mStadiumArcadium -> {
                    assertThat(albumAndSong.songs).containsExactlyElementsIn(
                        listOf(mRhcpSong1, mRhcpSong2)
                    )
                }
                mCalifornication -> {
                    assertThat(albumAndSong.songs).isEmpty()
                }
                else -> {
                    Assert.fail()
                }
            }
        }
    }

    @Test
    fun testPojoWithEmbeddedAndRelationGuavaImmutableSetMultimap() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val artistToAlbumsWithSongsMap: ImmutableSetMultimap<Artist, AlbumWithSongs> =
            mMusicDao.allArtistAndTheirAlbumsWithSongsGuavaImmutableSetMultimap()
        val rhcpList: ImmutableSet<AlbumWithSongs> = artistToAlbumsWithSongsMap[mRhcp]
        assertThat(artistToAlbumsWithSongsMap.keySet()).containsExactlyElementsIn(
            listOf<Any>(mRhcp, mAcDc, mPinkFloyd)
        )
        assertThat(artistToAlbumsWithSongsMap.containsKey(mTheClash)).isFalse()
        assertThat(artistToAlbumsWithSongsMap[mPinkFloyd].asList()[0].album)
            .isEqualTo(mTheDarkSideOfTheMoon)
        assertThat(artistToAlbumsWithSongsMap[mAcDc].asList()[0].album)
            .isEqualTo(mHighwayToHell)
        assertThat(
            artistToAlbumsWithSongsMap[mAcDc].asList()[0].songs[0]
        ).isEqualTo(mAcdcSong1)
        for (albumAndSong in rhcpList) {
            when (albumAndSong.album) {
                mStadiumArcadium -> {
                    assertThat(albumAndSong.songs).containsExactlyElementsIn(
                        listOf(mRhcpSong1, mRhcpSong2)
                    )
                }
                mCalifornication -> {
                    assertThat(albumAndSong.songs).isEmpty()
                }
                else -> {
                    Assert.fail()
                }
            }
        }
    }

    @Test
    fun testJoinByArtistNameImmutableMap() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMap: ImmutableMap<Artist, List<Song>> =
            mMusicDao.allArtistAndTheirSongsImmutableMap()
        assertContentsOfResultMapWithList(artistToSongsMap)
    }

    @Test
    fun testJoinByArtistNameRawQueryImmutableMap() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMap: ImmutableMap<Artist, List<Song>> =
            mMusicDao.getAllArtistAndTheirSongsRawQueryImmutableMap(
                SimpleSQLiteQuery(
                    "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song" +
                        ".mArtist"
                )
            )
        assertContentsOfResultMapWithList(artistToSongsMap)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testJoinByArtistNameImmutableMapWithSet() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistToSongsMapLiveData: LiveData<ImmutableMap<Artist, Set<Song>>> =
            mMusicDao.allArtistAndTheirSongsAsLiveDataImmutableMap()
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer: TestObserver<ImmutableMap<Artist, Set<Song>>> = MyTestObserver()
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer)
        assertThat(observer.hasValue()).isFalse()
        observer.reset()
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(observer.get()).isNotNull()
        assertContentsOfResultMapWithSet(observer.get()!!)
    }

    @Test
    fun testStringToListOfSongs() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistNameToSongsMap: Map<String, List<Song>> = mMusicDao.artistNameToSongs()
        assertThat(artistNameToSongsMap.containsKey("Pink Floyd")).isTrue()
        assertThat(artistNameToSongsMap["Red Hot Chili Peppers"]).containsExactlyElementsIn(
            listOf<Any>(mRhcpSong1, mRhcpSong2)
        )
    }

    @Test
    fun testIntegerToListOfAlbums() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val releaseYearToAlbumsMap: Map<Int, List<Song>> = mMusicDao.releaseYearToAlbums()
        assertThat(releaseYearToAlbumsMap.containsKey(2006)).isTrue()
        assertThat(releaseYearToAlbumsMap[2006]).containsExactlyElementsIn(
            listOf<Any>(mRhcpSong1, mRhcpSong2)
        )
        assertThat(releaseYearToAlbumsMap[1979]).containsExactlyElementsIn(
            listOf<Any>(mAcdcSong1)
        )
    }

    @Test
    fun testIntegerToStringOfAlbumNames() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val releaseYearToAlbumNameMap: Map<Int, List<String>> =
            mMusicDao.releaseYearToSongNames()
        assertThat(releaseYearToAlbumNameMap.containsKey(2006)).isTrue()
        assertThat(releaseYearToAlbumNameMap[2006]).containsExactlyElementsIn(
            listOf("Snow (Hey Oh)", "Dani California")
        )
    }

    @Test
    fun testStringToListOfSongsRawQuery() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistNameToSongsMap: Map<String, List<Song>> = mMusicDao.getArtistNameToSongsRawQuery(
            SimpleSQLiteQuery(
                "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist"
            )
        )
        assertThat(artistNameToSongsMap.containsKey("Pink Floyd")).isTrue()
        assertThat(artistNameToSongsMap["Red Hot Chili Peppers"]).containsExactlyElementsIn(
            listOf<Any>(mRhcpSong1, mRhcpSong2)
        )
    }

    @Test
    fun testIntegerToListOfAlbumsRawQuery() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val releaseYearToAlbumsMap: Map<Int, List<Song>> =
            mMusicDao.getReleaseYearToAlbumsRawQuery(
                SimpleSQLiteQuery(
                    "SELECT * FROM Album JOIN Song ON Song.mReleasedYear = Album" +
                        ".mAlbumReleaseYear"
                )
            )
        assertThat(releaseYearToAlbumsMap.containsKey(2006)).isTrue()
        assertThat(releaseYearToAlbumsMap[2006]).containsExactlyElementsIn(
            listOf<Any>(mRhcpSong1, mRhcpSong2)
        )
        assertThat(releaseYearToAlbumsMap[1979]).containsExactlyElementsIn(
            listOf<Any>(mAcdcSong1)
        )
    }

    @Test
    fun testIntegerToStringOfAlbumNamesRawQuery() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val releaseYearToAlbumNameMap: Map<Int, List<String>> =
            mMusicDao.getReleaseYearToSongNamesRawQuery(
                SimpleSQLiteQuery(
                    "SELECT * FROM Album JOIN Song ON Song.mReleasedYear = Album" +
                        ".mAlbumReleaseYear"
                )
            )
        assertThat(releaseYearToAlbumNameMap.containsKey(2006)).isTrue()
        assertThat(releaseYearToAlbumNameMap[2006]).containsExactlyElementsIn(
            listOf("Snow (Hey Oh)", "Dani California")
        )
    }

    @Test
    fun testArtistToSongCount() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistNameToSongsMap: Map<Artist, Int> = mMusicDao.artistAndSongCountMap()
        assertThat(artistNameToSongsMap.containsKey(mPinkFloyd)).isTrue()
        assertThat(artistNameToSongsMap[mRhcp]).isEqualTo(2)
    }

    @Test
    fun testArtistToSongCountWithRawQuery() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistNameToSongsMap: Map<Artist, Int> = mMusicDao.getArtistAndSongCountMapRawQuery(
            SimpleSQLiteQuery(
                "SELECT *, COUNT(mSongId) as songCount FROM Artist JOIN Song ON Artist" +
                    ".mArtistName = Song.mArtist GROUP BY mArtistName"
            )
        )
        assertThat(artistNameToSongsMap.containsKey(mPinkFloyd)).isTrue()
        assertThat(artistNameToSongsMap[mRhcp]).isEqualTo(2)
    }

    @Test
    fun testArtistToImage() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val artistNameToImagesMap: ImmutableMap<Artist, ByteBuffer> =
            mMusicDao.allArtistsWithAlbumCovers()
        assertThat(artistNameToImagesMap.containsKey(mPinkFloyd)).isTrue()
        assertThat(artistNameToImagesMap[mRhcp]).isEqualTo(
            ByteBuffer.wrap("stadium_arcadium_image".toByteArray())
        )
    }

    @Test
    fun testArtistToImageRawQuery() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val artistNameToImagesMap: ImmutableMap<Artist, ByteBuffer> =
            mMusicDao.getAllArtistsWithAlbumCoversRawQuery(
                SimpleSQLiteQuery(
                    "SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image" +
                        ".mArtistInImage"
                )
            )
        assertThat(artistNameToImagesMap.containsKey(mPinkFloyd)).isTrue()
        assertThat(artistNameToImagesMap[mRhcp]).isEqualTo(
            ByteBuffer.wrap("stadium_arcadium_image".toByteArray())
        )
    }

    @Test
    fun testArtistToImageYear() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val artistNameToImagesMap: ImmutableMap<Artist, Long> =
            mMusicDao.allArtistsWithAlbumCoverYear()
        assertThat(artistNameToImagesMap[mRhcp]).isEqualTo(2006L)
    }

    @Test
    fun testImageYearToArtistRawQuery() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<Long, Artist> =
            mMusicDao.getAllAlbumCoverYearToArtistsWithRawQuery(
                SimpleSQLiteQuery(
                    "SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image" +
                        ".mArtistInImage"
                )
            )
        assertThat(imageToArtistsMap[2006L]).isEqualTo(mRhcp)
        assertThat(
            imageToArtistsMap.keys
        ).containsExactlyElementsIn(
            listOf(2006L, 1973L)
        )
    }

    @Test
    fun testAlbumCoversWithBandActivity() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<ByteBuffer, Boolean> =
            mMusicDao.albumCoversWithBandActivity()
        assertThat(
            imageToArtistsMap[ByteBuffer.wrap("stadium_arcadium_image".toByteArray())]
        ).isEqualTo(true)
        assertThat(
            imageToArtistsMap[ByteBuffer.wrap("dark_side_of_the_moon_image".toByteArray())]
        ).isEqualTo(false)
    }

    @Test
    fun testAlbumCoversWithBandActivityRawQuery() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<ByteBuffer, Boolean> =
            mMusicDao.getAlbumCoversWithBandActivityRawQuery(
                SimpleSQLiteQuery(
                    "SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image" +
                        ".mArtistInImage"
                )
            )
        assertThat(imageToArtistsMap[ByteBuffer.wrap("stadium_arcadium_image".toByteArray())])
            .isEqualTo(true)
        assertThat(
            imageToArtistsMap[ByteBuffer.wrap("dark_side_of_the_moon_image".toByteArray())]
        ).isEqualTo(false)
    }

    @Test
    fun testAlbumReleaseDateWithBandActivity() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<Date, Boolean> =
            mMusicDao.albumDateWithBandActivity()
        assertThat(imageToArtistsMap[Date(101779200000L)]).isEqualTo(false)
        assertThat(imageToArtistsMap[Date(1146787200000L)]).isEqualTo(true)
    }

    @Test
    fun testAlbumReleaseDateWithBandActivityRawQuery() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<Date, Boolean> =
            mMusicDao.getAlbumDateWithBandActivityRawQuery(
                SimpleSQLiteQuery(
                    "SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image" +
                        ".mArtistInImage"
                )
            )
        assertThat(imageToArtistsMap[Date(101779200000L)]).isEqualTo(false)
        assertThat(imageToArtistsMap[Date(1146787200000L)]).isEqualTo(true)
    }

    @Test
    fun testEnumMap() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<ImageFormat, Boolean> =
            mMusicDao.imageFormatWithBandActivity()
        assertThat(imageToArtistsMap[ImageFormat.JPG]).isEqualTo(false)
        assertThat(imageToArtistsMap[ImageFormat.MPEG]).isEqualTo(true)
    }

    @Test
    fun testEnumMapWithRawQuery() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<ImageFormat, Boolean> =
            mMusicDao.getImageFormatWithBandActivityRawQuery(
                SimpleSQLiteQuery(
                    "SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image" +
                        ".mArtistInImage"
                )
            )
        assertThat(imageToArtistsMap[ImageFormat.JPG]).isEqualTo(false)
        assertThat(imageToArtistsMap[ImageFormat.MPEG]).isEqualTo(true)
    }

    @Test
    fun testInvalidMapInfoColumnsWithRawQuery() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        try {
            mMusicDao.getMapWithInvalidColumnRawQuery(
                SimpleSQLiteQuery(
                    "SELECT *, COUNT(mSongId) as songCount FROM Artist JOIN Song ON Artist" +
                        ".mArtistName = Song.mArtist GROUP BY mArtistName"
                )
            )
        } catch (e: IllegalArgumentException) {
            assertThat(e.message!!.contains("column 'cat' does not exist"))
        }
    }

    @Test
    fun testLeftJoin() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val map: Map<Artist, List<Album>> = mMusicDao.artistAndAlbumsLeftJoin()
        assertThat(map.containsKey(mTheClash))
        assertThat(map[mTheClash]).isEmpty()
    }

    @Test
    fun testLeftJoinGuava() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val map: ImmutableListMultimap<Artist, Album> = mMusicDao.artistAndAlbumsLeftJoinGuava()
        assertThat(map.containsKey(mTheClash))
        assertThat(map[mTheClash]).isEmpty()
    }

    @Test
    fun testNonPojoLeftJoin() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val map: Map<Artist, List<String>> = mMusicDao.artistAndAlbumNamesLeftJoin()
        assertThat(map.containsKey(mTheClash))
        assertThat(map[mTheClash]).isEmpty()
    }

    @Test
    fun nullKeyColumnLeftJoin() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val map: Map<Album, Artist> = mMusicDao.albumToArtistLeftJoin()
        assertThat(map.containsKey(mHighwayToHell))
        assertThat(map[mHighwayToHell]).isEqualTo(mAcDc)
    }

    @Test
    fun nullValueColumnLeftJoin() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell
        )
        val map: Map<Artist, Album> = mMusicDao.artistToAlbumLeftJoin()
        assertThat(map.containsKey(mAcDc))
        assertThat(map[mAcDc]).isEqualTo(mHighwayToHell)
    }

    @Test
    fun nullAlbumAddedLeftJoin() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd, mGlassAnimals)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell,
            mDreamland
        )
        val map: Map<Artist, Album> = mMusicDao.artistToAlbumLeftJoin()
        assertThat(map.containsKey(mGlassAnimals)).isFalse()
    }

    @Test
    fun testImageYearToArtistLongSparseArray() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val imageToArtistsMap: LongSparseArray<Artist> =
            mMusicDao.allAlbumCoverYearToArtistsWithLongSparseArray()
        assertThat(imageToArtistsMap.size()).isEqualTo(2)
        assertThat(imageToArtistsMap[2006L]).isEqualTo(mRhcp)
    }

    @Test
    fun testImageYearToArtistSparseArrayCompat() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val imageToArtistsMap: SparseArrayCompat<Artist> =
            mMusicDao.allAlbumCoverYearToArtistsWithIntSparseArray()
        assertThat(imageToArtistsMap.size()).isEqualTo(2)
        assertThat(imageToArtistsMap[2006]).isEqualTo(mRhcp)
        assertThat(imageToArtistsMap[1973]).isEqualTo(mPinkFloyd)
    }

    @Test
    fun testImageYearToArtistRawQueryArrayMap() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val imageToArtistsMap: ArrayMap<Long, Artist> =
            mMusicDao.getAllAlbumCoverYearToArtistsWithRawQueryArrayMap(
                SimpleSQLiteQuery(
                    "SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image" +
                        ".mArtistInImage"
                )
            )
        assertThat(imageToArtistsMap[2006L]).isEqualTo(mRhcp)
        assertThat(
            imageToArtistsMap.keys
        ).containsExactlyElementsIn(
            listOf(2006L, 1973L)
        )
    }

    @Test
    fun testArtistToImageYearArrayMap() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)
        val artistNameToImagesMap: ArrayMap<Artist, Long> =
            mMusicDao.allArtistsWithAlbumCoverYearArrayMap()
        assertThat(artistNameToImagesMap[mRhcp]).isEqualTo(2006L)
    }

    @Test
    fun testSingleNestedMap() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell,
            mDreamland
        )
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1, mRhcpSong3)

        val singleNestedMap = mMusicDao.getArtistToAlbumsMappedToSongs()
        val rhcpMap = singleNestedMap.getValue(mRhcp)
        val stadiumArcadiumList = rhcpMap.getValue(mStadiumArcadium)
        val californicationList = rhcpMap.getValue(mCalifornication)

        val stadiumArcadiumExpectedList = listOf(mRhcpSong1, mRhcpSong2)
        val californicationExpectedList = listOf(mRhcpSong3)

        assertThat(rhcpMap.keys).containsExactlyElementsIn(
            listOf(mCalifornication, mStadiumArcadium)
        )
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)
    }

    @Test
    fun testDoubleNestedMap() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell,
            mDreamland
        )
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1, mRhcpSong3)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover)

        val doubleNestedMap = mMusicDao.getImageToArtistToAlbumsMappedToSongs()
        val rhcpImageMap = doubleNestedMap.getValue(mRhcpAlbumCover)
        val rhcpMap = rhcpImageMap.getValue(mRhcp)
        val stadiumArcadiumList = rhcpMap.getValue(mStadiumArcadium)
        val californicationList = rhcpMap.getValue(mCalifornication)

        val stadiumArcadiumExpectedList = listOf(mRhcpSong1, mRhcpSong2)
        val californicationExpectedList = listOf(mRhcpSong3)

        assertThat(doubleNestedMap.keys).containsExactlyElementsIn(
            listOf(mPinkFloydAlbumCover, mRhcpAlbumCover)
        )
        assertThat(rhcpImageMap.keys).containsExactly(mRhcp)
        assertThat(rhcpMap.keys).containsExactlyElementsIn(
            listOf(mCalifornication, mStadiumArcadium)
        )
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)
    }

    @Test
    fun testSingleNestedMapWithMapInfoLeftJoin() {
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell,
            mDreamland
        )
        mMusicDao.addSongs(mRhcpSong1, mAcdcSong1, mPinkFloydSong1, mRhcpSong3)

        val singleNestedMap = mMusicDao.getArtistToAlbumsMappedToSongNamesMapInfoLeftJoin()
        val rhcpMap = singleNestedMap.getValue(mRhcp)

        assertThat(rhcpMap.keys).containsExactlyElementsIn(
            listOf(mCalifornication, mStadiumArcadium)
        )
        assertThat(rhcpMap[mStadiumArcadium]).isEqualTo(mRhcpSong1.mTitle)
        assertThat(rhcpMap[mCalifornication]).isEqualTo(mRhcpSong3.mTitle)

        // LEFT JOIN Checks
        assertThat(singleNestedMap[mTheClash]).isEmpty()
    }

    @Test
    fun testDoubleNestedMapWithMapInfoKeyLeftJoin() {
        mMusicDao.addArtists(mRhcp, mAcDc, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell,
            mDreamland
        )
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mRhcpSong3)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover, mTheClashAlbumCover)

        val doubleNestedMap = mMusicDao.getImageYearToArtistToAlbumsMappedToSongs()
        val rhcpImageMap = doubleNestedMap.getValue(mRhcpAlbumCover.mImageYear)
        val rhcpMap = rhcpImageMap.getValue(mRhcp)
        val stadiumArcadiumList = rhcpMap.getValue(mStadiumArcadium)
        val californicationList = rhcpMap.getValue(mCalifornication)

        val stadiumArcadiumExpectedList = listOf(mRhcpSong1, mRhcpSong2)
        val californicationExpectedList = listOf(mRhcpSong3)

        assertThat(doubleNestedMap.keys).containsExactlyElementsIn(
            listOf(
                mPinkFloydAlbumCover.mImageYear,
                mRhcpAlbumCover.mImageYear,
                mTheClashAlbumCover.mImageYear
            )
        )
        assertThat(rhcpImageMap.keys).containsExactly(mRhcp)
        assertThat(rhcpMap.keys).containsExactlyElementsIn(
            listOf(mCalifornication, mStadiumArcadium)
        )
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)

        // LEFT JOIN Checks
        assertThat(doubleNestedMap).containsKey(mTheClashAlbumCover.mImageYear)
        assertThat(doubleNestedMap[mTheClashAlbumCover.mImageYear]).isEmpty()
        assertThat(doubleNestedMap).containsKey(mPinkFloydAlbumCover.mImageYear)
        assertThat(doubleNestedMap[mPinkFloydAlbumCover.mImageYear]).containsKey(mPinkFloyd)
        assertThat(doubleNestedMap[mPinkFloydAlbumCover.mImageYear]!![mPinkFloyd])
            .containsKey(mTheDarkSideOfTheMoon)
        assertThat(
            doubleNestedMap[mPinkFloydAlbumCover.mImageYear]
            !![mPinkFloyd]
            !![mTheDarkSideOfTheMoon]
        ).isEmpty()
    }

    @Test
    fun testNestedMapWithMapInfoKeyAndValue() {
        mMusicDao.addArtists(mRhcp, mAcDc, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell,
            mDreamland
        )
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mRhcpSong3)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover, mTheClashAlbumCover)

        val doubleNestedMap = mMusicDao.getNestedMapWithMapInfoKeyAndValue()
        val rhcpImageMap = doubleNestedMap.getValue(mRhcpAlbumCover.mImageYear)
        val rhcpMap = rhcpImageMap.getValue(mRhcp)
        val stadiumArcadiumList = rhcpMap.getValue(mStadiumArcadium)
        val californicationList = rhcpMap.getValue(mCalifornication)

        val stadiumArcadiumExpectedList = listOf(mRhcpSong1.mTitle, mRhcpSong2.mTitle)
        val californicationExpectedList = listOf(mRhcpSong3.mTitle)

        assertThat(doubleNestedMap.keys).containsExactlyElementsIn(
            listOf(
                mPinkFloydAlbumCover.mImageYear,
                mRhcpAlbumCover.mImageYear,
                mTheClashAlbumCover.mImageYear
            )
        )
        assertThat(rhcpImageMap.keys).containsExactly(mRhcp)
        assertThat(rhcpMap.keys).containsExactlyElementsIn(
            listOf(mCalifornication, mStadiumArcadium)
        )
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)

        // LEFT JOIN Checks
        assertThat(doubleNestedMap).containsKey(mTheClashAlbumCover.mImageYear)
        assertThat(doubleNestedMap[mTheClashAlbumCover.mImageYear]).isEmpty()
        assertThat(doubleNestedMap).containsKey(mPinkFloydAlbumCover.mImageYear)
        assertThat(doubleNestedMap[mPinkFloydAlbumCover.mImageYear]).containsKey(mPinkFloyd)
        assertThat(doubleNestedMap[mPinkFloydAlbumCover.mImageYear]!![mPinkFloyd])
            .containsKey(mTheDarkSideOfTheMoon)
        assertThat(
            doubleNestedMap[mPinkFloydAlbumCover.mImageYear]
            !![mPinkFloyd]
            !![mTheDarkSideOfTheMoon]
        ).isEmpty()
    }

    @Test
    fun testDoubleNestedMapWithMapColumnKeyLeftJoin() {
        mMusicDao.addArtists(mRhcp, mAcDc, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell,
            mDreamland
        )
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mRhcpSong3)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover, mTheClashAlbumCover)

        val doubleNestedMap = mMusicDao.getImageYearToArtistToAlbumsToSongsMultiMapColumn()
        val rhcpImageMap = doubleNestedMap.getValue(mRhcpAlbumCover)
        val rhcpMap = rhcpImageMap.getValue(mRhcp)
        val stadiumArcadiumList = rhcpMap[mStadiumArcadium.mAlbumName]
        val californicationList = rhcpMap[mCalifornication.mAlbumName]

        val stadiumArcadiumExpectedList = listOf(mRhcpSong1.mReleasedYear, mRhcpSong2.mReleasedYear)
        val californicationExpectedList = listOf(mRhcpSong3.mReleasedYear)

        assertThat(doubleNestedMap.keys).containsExactlyElementsIn(
            listOf(
                mPinkFloydAlbumCover,
                mRhcpAlbumCover,
                mTheClashAlbumCover
            )
        )
        assertThat(rhcpImageMap.keys).containsExactly(mRhcp)
        assertThat(rhcpMap.keys).containsExactlyElementsIn(
            listOf(mCalifornication.mAlbumName, mStadiumArcadium.mAlbumName)
        )
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)

        // LEFT JOIN Checks
        assertThat(doubleNestedMap).containsKey(mTheClashAlbumCover)
        assertThat(doubleNestedMap[mTheClashAlbumCover]).isEmpty()
        assertThat(doubleNestedMap).containsKey(mPinkFloydAlbumCover)
        assertThat(doubleNestedMap[mPinkFloydAlbumCover]).containsKey(mPinkFloyd)
        assertThat(doubleNestedMap[mPinkFloydAlbumCover]!![mPinkFloyd])
            .containsKey(mTheDarkSideOfTheMoon.mAlbumName)
        assertThat(
            doubleNestedMap[mPinkFloydAlbumCover]
            !![mPinkFloyd]
            !![mTheDarkSideOfTheMoon.mAlbumName]
        ).isEmpty()
    }

    @Test
    fun testDoubleNestedMapWithMapColumnKeyLeftJoinRawQuery() {
        mMusicDao.addArtists(mRhcp, mAcDc, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell,
            mDreamland
        )
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mRhcpSong3)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover, mTheClashAlbumCover)

        val doubleNestedMap = mMusicDao.getImageYearToArtistToAlbumsToSongsMultiMapColumn(
            SimpleSQLiteQuery(
                """
                SELECT * FROM Image
                LEFT JOIN Artist ON Image.mArtistInImage = Artist.mArtistName
                LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist
                LEFT JOIN Song ON Album.mAlbumName = Song.mAlbum
                """
            )
        )
        val rhcpImageMap = doubleNestedMap.getValue(mRhcpAlbumCover)
        val rhcpMap = rhcpImageMap.getValue(mRhcp)
        val stadiumArcadiumList = rhcpMap[mStadiumArcadium.mAlbumName]
        val californicationList = rhcpMap[mCalifornication.mAlbumName]

        val stadiumArcadiumExpectedList = listOf(mRhcpSong1.mReleasedYear, mRhcpSong2.mReleasedYear)
        val californicationExpectedList = listOf(mRhcpSong3.mReleasedYear)

        assertThat(doubleNestedMap.keys).containsExactlyElementsIn(
            listOf(
                mPinkFloydAlbumCover,
                mRhcpAlbumCover,
                mTheClashAlbumCover
            )
        )
        assertThat(rhcpImageMap.keys).containsExactly(mRhcp)
        assertThat(rhcpMap.keys).containsExactlyElementsIn(
            listOf(mCalifornication.mAlbumName, mStadiumArcadium.mAlbumName)
        )
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)

        // LEFT JOIN Checks
        assertThat(doubleNestedMap).containsKey(mTheClashAlbumCover)
        assertThat(doubleNestedMap[mTheClashAlbumCover]).isEmpty()
        assertThat(doubleNestedMap).containsKey(mPinkFloydAlbumCover)
        assertThat(doubleNestedMap[mPinkFloydAlbumCover]).containsKey(mPinkFloyd)
        assertThat(doubleNestedMap[mPinkFloydAlbumCover]!![mPinkFloyd])
            .containsKey(mTheDarkSideOfTheMoon.mAlbumName)
        assertThat(
            doubleNestedMap[mPinkFloydAlbumCover]
            !![mPinkFloyd]
            !![mTheDarkSideOfTheMoon.mAlbumName]
        ).isEmpty()
    }

    @Test
    fun testStringToListOfSongsMapColumn() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1)
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd)
        val artistNameToSongsMap: Map<String, List<Int>> = mMusicDao.artistNameToSongsMapColumn()
        assertThat(artistNameToSongsMap.containsKey("Pink Floyd")).isTrue()
        assertThat(artistNameToSongsMap["Red Hot Chili Peppers"]).containsExactlyElementsIn(
            listOf(mRhcpSong1.mReleasedYear, mRhcpSong2.mReleasedYear)
        )
    }

    @Test
    fun testDoubleNestedMapWithOneMapColumn() {
        mMusicDao.addArtists(mRhcp, mAcDc, mPinkFloyd)
        mMusicDao.addAlbums(
            mStadiumArcadium,
            mCalifornication,
            mTheDarkSideOfTheMoon,
            mHighwayToHell,
            mDreamland
        )
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mRhcpSong3)
        mMusicDao.addImages(mPinkFloydAlbumCover, mRhcpAlbumCover, mTheClashAlbumCover)

        val doubleNestedMap = mMusicDao.getImageYearToArtistToAlbumsToSongsMapColumn()
        val rhcpImageMap = doubleNestedMap.getValue(mRhcpAlbumCover.mImageYear)
        val rhcpMap = rhcpImageMap.getValue(mRhcp)
        val stadiumArcadiumList = rhcpMap.getValue("Stadium Arcadium")
        val californicationList = rhcpMap.getValue("Californication")

        val stadiumArcadiumExpectedList = listOf(mRhcpSong1, mRhcpSong2)
        val californicationExpectedList = listOf(mRhcpSong3)

        assertThat(doubleNestedMap.keys).containsExactlyElementsIn(
            listOf(
                mPinkFloydAlbumCover.mImageYear,
                mRhcpAlbumCover.mImageYear,
                mTheClashAlbumCover.mImageYear
            )
        )
        assertThat(rhcpImageMap.keys).containsExactly(mRhcp)
        assertThat(rhcpMap.keys).containsExactlyElementsIn(
            listOf("Stadium Arcadium", "Californication")
        )
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)

        // LEFT JOIN Checks
        assertThat(doubleNestedMap).containsKey(mTheClashAlbumCover.mImageYear)
        assertThat(doubleNestedMap[mTheClashAlbumCover.mImageYear]).isEmpty()
        assertThat(doubleNestedMap).containsKey(mPinkFloydAlbumCover.mImageYear)
        assertThat(doubleNestedMap[mPinkFloydAlbumCover.mImageYear]).containsKey(mPinkFloyd)
        assertThat(doubleNestedMap[mPinkFloydAlbumCover.mImageYear]!![mPinkFloyd])
            .containsKey(mTheDarkSideOfTheMoon.mAlbumName)
        assertThat(
            doubleNestedMap[mPinkFloydAlbumCover.mImageYear]
            !![mPinkFloyd]
            !![mTheDarkSideOfTheMoon.mAlbumName]
        ).isEmpty()
    }

    /**
     * Checks that the contents of the map are as expected.
     *
     * @param artistToSongsMap Map of Artists to list of Songs joined by the artist name
     */
    private fun assertContentsOfResultMapWithList(artistToSongsMap: Map<Artist, List<Song>>) {
        assertThat(artistToSongsMap.keys).containsExactlyElementsIn(
            listOf<Any>(mRhcp, mAcDc, mPinkFloyd)
        )
        assertThat(artistToSongsMap.containsKey(mTheClash)).isFalse()
        assertThat(artistToSongsMap[mPinkFloyd]).containsExactly(mPinkFloydSong1)
        assertThat(artistToSongsMap[mRhcp]).containsExactlyElementsIn(
            listOf<Any>(mRhcpSong1, mRhcpSong2)
        )
        assertThat(artistToSongsMap[mAcDc]).containsExactly(mAcdcSong1)
    }

    /**
     * Checks that the contents of the map are as expected.
     *
     * @param artistToSongsMap Map of Artists to set of Songs joined by the artist name
     */
    private fun assertContentsOfResultMapWithSet(artistToSongsMap: Map<Artist, Set<Song>>) {
        assertThat(artistToSongsMap.keys).containsExactlyElementsIn(
            listOf<Any>(mRhcp, mAcDc, mPinkFloyd)
        )
        assertThat(artistToSongsMap.containsKey(mTheClash)).isFalse()
        assertThat(artistToSongsMap[mPinkFloyd]).containsExactly(mPinkFloydSong1)
        assertThat(artistToSongsMap[mRhcp]).containsExactlyElementsIn(
            listOf<Any>(mRhcpSong1, mRhcpSong2)
        )
        assertThat(artistToSongsMap[mAcDc]).containsExactly(mAcdcSong1)
    }

    /**
     * Checks that the contents of the map are as expected.
     *
     * @param artistToSongsMap Map of Artists to Collection of Songs joined by the artist name
     */
    private fun assertContentsOfResultMultimap(artistToSongsMap: ImmutableMultimap<Artist, Song>) {
        assertThat(artistToSongsMap.keySet()).containsExactlyElementsIn(
            listOf<Any>(mRhcp, mAcDc, mPinkFloyd)
        )
        assertThat(artistToSongsMap.containsKey(mTheClash)).isFalse()
        assertThat(artistToSongsMap[mPinkFloyd]).containsExactly(mPinkFloydSong1)
        assertThat(artistToSongsMap[mRhcp]).containsExactlyElementsIn(
            listOf<Any>(mRhcpSong1, mRhcpSong2)
        )
        assertThat(artistToSongsMap[mAcDc]).containsExactly(mAcdcSong1)
    }
}
