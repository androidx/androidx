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
package androidx.room3.integration.kotlintestapp.test

import androidx.collection.ArrayMap
import androidx.collection.LongSparseArray
import androidx.collection.SparseArrayCompat
import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.room3.RoomRawQuery
import androidx.room3.integration.kotlintestapp.dao.MusicDao
import androidx.room3.integration.kotlintestapp.vo.Album
import androidx.room3.integration.kotlintestapp.vo.AlbumNameAndBandName
import androidx.room3.integration.kotlintestapp.vo.AlbumWithSongs
import androidx.room3.integration.kotlintestapp.vo.Artist
import androidx.room3.integration.kotlintestapp.vo.Image
import androidx.room3.integration.kotlintestapp.vo.ImageFormat
import androidx.room3.integration.kotlintestapp.vo.ReleasedAlbum
import androidx.room3.integration.kotlintestapp.vo.Song
import androidx.test.filters.MediumTest
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSetMultimap
import io.reactivex.rxjava3.core.Flowable
import java.nio.ByteBuffer
import java.util.Date
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/** Tests multimap return type for JOIN statements. */
@MediumTest
@RunWith(Parameterized::class)
class MultimapQueryTest(driver: UseDriver) : TestDatabaseTest(driver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    private lateinit var musicDao: MusicDao
    private val rhcpSong1: Song =
        Song(1, "Dani California", "Red Hot Chili Peppers", "Stadium Arcadium", 442, 2006)
    private val rhcpSong2: Song =
        Song(2, "Snow (Hey Oh)", "Red Hot Chili Peppers", "Stadium Arcadium", 514, 2006)
    private val acdcSong1: Song = Song(3, "Highway to Hell", "AC/DC", "Highway to Hell", 328, 1979)
    private val pinkFloydSong1: Song =
        Song(4, "The Great Gig in the Sky", "Pink Floyd", "The Dark Side of the Moon", 443, 1973)
    private val rhcpSong3: Song =
        Song(5, "Parallel Universe", "Red Hot Chili Peppers", "Californication", 529, 1999)
    private val rhcp: Artist = Artist(1, "Red Hot Chili Peppers", true)
    private val acdc: Artist = Artist(2, "AC/DC", true)
    private val theClash: Artist = Artist(3, "The Clash", false)
    private val pinkFloyd: Artist = Artist(4, "Pink Floyd", false)
    private val glassAnimals: Artist = Artist(5, "Glass Animals", true)
    private val stadiumArcadium: Album =
        Album(1, "Stadium Arcadium", "Red Hot Chili Peppers", 2006, "N/A")
    private val californication: Album =
        Album(2, "Californication", "Red Hot Chili Peppers", 1999, "N/A")
    private val highwayToHell: Album = Album(3, "Highway to Hell", "AC/DC", 1979, null)
    private val theDarkSideOfTheMoon: Album =
        Album(4, "The Dark Side of the Moon", "Pink Floyd", 1973, "N/A")
    private val dreamland: Album = Album(5, "Dreamland", null, 2020, null)
    private val pinkFloydAlbumCover: Image =
        Image(
            1,
            1973L,
            "Pink Floyd",
            "dark_side_of_the_moon_image".toByteArray(),
            Date(101779200000L),
            ImageFormat.JPG,
        )
    private val rhcpAlbumCover: Image =
        Image(
            2,
            2006L,
            "Red Hot Chili Peppers",
            "stadium_arcadium_image".toByteArray(),
            Date(1146787200000L),
            ImageFormat.MPEG,
        )

    private val theClashAlbumCover: Image =
        Image(
            3,
            1979L,
            "The Clash",
            "london_calling_image".toByteArray(),
            Date(11873445200000L),
            ImageFormat.MPEG,
        )

    @Before
    fun createDb() {
        musicDao = database.musicDao()
    }

    /** Tests a simple JOIN query between two tables. */
    @Test
    fun testGetFirstSongForArtist() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMap: Map<Artist, Song> = musicDao.getArtistAndFirstSongMap()
        assertThat(artistToSongsMap[acdc]).isEqualTo(acdcSong1)
        assertThat(artistToSongsMap[rhcp]).isEqualTo(rhcpSong1)
    }

    @Test
    fun testGetSongToArtistMapping() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val songToArtistMap: Map<Song, Artist> = musicDao.getSongAndArtist()
        assertThat(songToArtistMap[acdcSong1]).isEqualTo(acdc)
        assertThat(songToArtistMap[pinkFloydSong1]).isEqualTo(pinkFloyd)
        assertThat(songToArtistMap[rhcpSong1]).isEqualTo(rhcp)
        assertThat(songToArtistMap[rhcpSong2]).isEqualTo(rhcp)
    }

    @Test
    fun testJoinByArtistNameList() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMap: Map<Artist, List<Song>> = musicDao.getAllArtistAndTheirSongsList()
        assertContentsOfResultMapWithList(artistToSongsMap)
    }

    @Test
    fun testJoinByArtistNameListOrdered() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)

        assertThat(musicDao.getAllArtistAndTheirSongsListOrdered().keys)
            .containsExactlyElementsIn(arrayOf(rhcp, acdc, pinkFloyd))
    }

    @Test
    fun testJoinByArtistNameSet() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsSet: Map<Artist, Set<Song>> = musicDao.getAllArtistAndTheirSongsSet()
        assertContentsOfResultMapWithSet(artistToSongsSet)
    }

    /** Tests a JOIN using [androidx.room3.RawQuery] between two tables. */
    @Test
    fun testJoinByArtistNameRawQuery() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMap: Map<Artist, Song> =
            musicDao.getAllArtistAndTheirSongsRawQuery(
                RoomRawQuery("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
            )
        assertThat(artistToSongsMap[acdc]).isEqualTo(acdcSong1)
    }

    @Test
    fun testJoinByArtistNameRawQueryList() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMap: Map<Artist, List<Song>> =
            musicDao.getAllArtistAndTheirSongsRawQueryList(
                RoomRawQuery("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
            )
        assertContentsOfResultMapWithList(artistToSongsMap)
    }

    @Test
    fun testJoinByArtistNameRawQuerySet() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMap: Map<Artist, Set<Song>> =
            musicDao.getAllArtistAndTheirSongsRawQuerySet(
                RoomRawQuery("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
            )
        assertContentsOfResultMapWithSet(artistToSongsMap)
    }

    /** Tests a simple JOIN query between two tables with a [LiveData] map return type. */
    @Test
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testJoinByArtistNameLiveData() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMapLiveData: LiveData<Map<Artist, Song>> =
            musicDao.getAllArtistAndTheirSongsAsLiveData()
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer = LiveDataTestObserver<Map<Artist, Song>>()
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer)
        assertThat(observer.hasValue()).isFalse()
        observer.reset()
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(observer.get()).isNotNull()
        assertThat(observer.get()?.get(acdc)).isEqualTo(acdcSong1)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testJoinByArtistNameLiveDataList() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMapLiveData: LiveData<Map<Artist, List<Song>>> =
            musicDao.getAllArtistAndTheirSongsAsLiveDataList()
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer = LiveDataTestObserver<Map<Artist, List<Song>>>()
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
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMapLiveData: LiveData<Map<Artist, Set<Song>>> =
            musicDao.allArtistAndTheirSongsAsLiveDataSet()
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer = LiveDataTestObserver<Map<Artist, Set<Song>>>()
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer)
        assertThat(observer.hasValue()).isFalse()
        observer.reset()
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(observer.get()).isNotNull()
        assertContentsOfResultMapWithSet(observer.get()!!)
    }

    /** Tests a simple JOIN query between two tables with a [Flowable] map return type. */
    @Test
    fun testJoinByArtistNameFlowableList() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMapFlowable: Flowable<Map<Artist, List<Song>>> =
            musicDao.getAllArtistAndTheirSongsAsFlowableList()
        assertContentsOfResultMapWithList(artistToSongsMapFlowable.blockingFirst())
    }

    @Test
    fun testJoinByArtistNameFlowableSet() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMapFlowable: Flowable<Map<Artist, Set<Song>>> =
            musicDao.allArtistAndTheirSongsAsFlowableSet()
        assertContentsOfResultMapWithSet(artistToSongsMapFlowable.blockingFirst())
    }

    /**
     * Tests a simple JOIN query between two tables with a return type of a map with a key that is
     * an entity [Artist] and a POJO [AlbumWithSongs] that use [androidx.room3.Embedded] and
     * [androidx.room3.Relation].
     */
    @Test
    fun testPojoWithEmbeddedAndRelation() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val artistToAlbumsWithSongsMap: Map<Artist, AlbumWithSongs> =
            musicDao.getAllArtistAndTheirAlbumsWithSongs()
        val rhcpAlbum: AlbumWithSongs? = artistToAlbumsWithSongsMap[rhcp]

        assertThat(rhcpAlbum).isNotNull()
        assertThat(artistToAlbumsWithSongsMap.keys)
            .containsExactlyElementsIn(arrayOf(rhcp, acdc, pinkFloyd))
        assertThat(artistToAlbumsWithSongsMap.containsKey(theClash)).isFalse()
        assertThat(artistToAlbumsWithSongsMap[pinkFloyd]?.album).isEqualTo(theDarkSideOfTheMoon)
        assertThat(artistToAlbumsWithSongsMap[acdc]?.album).isEqualTo(highwayToHell)
        assertThat(artistToAlbumsWithSongsMap[acdc]?.songs?.get(0)).isEqualTo(acdcSong1)
        if (rhcpAlbum?.album?.equals(stadiumArcadium) == true) {
            assertThat(rhcpAlbum.songs).containsExactlyElementsIn(listOf(rhcpSong1, rhcpSong2))
        } else if (rhcpAlbum?.album?.equals(californication) == true) {
            assertThat(rhcpAlbum.songs).isEmpty()
        } else {
            Assert.fail()
        }
    }

    /**
     * Tests a simple JOIN query between two tables with a return type of a map with a key that is
     * an entity [Artist] and a list of entity POJOs [AlbumWithSongs] that use
     * [androidx.room3.Embedded] and [androidx.room3.Relation].
     */
    @Test
    fun testPojoWithEmbeddedAndRelationList() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val artistToAlbumsWithSongsMap: Map<Artist, List<AlbumWithSongs>> =
            musicDao.getAllArtistAndTheirAlbumsWithSongsList()
        musicDao.getAllArtistAndTheirAlbumsWithSongs()
        val rhcpList: List<AlbumWithSongs> = artistToAlbumsWithSongsMap[rhcp]!!
        assertThat(artistToAlbumsWithSongsMap.keys)
            .containsExactlyElementsIn(listOf<Any>(rhcp, acdc, pinkFloyd))
        assertThat(artistToAlbumsWithSongsMap.containsKey(theClash)).isFalse()
        assertThat(artistToAlbumsWithSongsMap[pinkFloyd]?.single()?.album)
            .isEqualTo(theDarkSideOfTheMoon)
        assertThat(artistToAlbumsWithSongsMap[acdc]?.single()?.album).isEqualTo(highwayToHell)
        assertThat(artistToAlbumsWithSongsMap[acdc]?.single()?.songs?.get(0)).isEqualTo(acdcSong1)
        for (albumAndSong in rhcpList) {
            when (albumAndSong.album) {
                stadiumArcadium -> {
                    assertThat(albumAndSong.songs)
                        .containsExactlyElementsIn(listOf(rhcpSong1, rhcpSong2))
                }
                californication -> {
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
     * [ReleasedAlbum] and value (list of [AlbumNameAndBandName]) that are non-entity POJOs.
     */
    @Test
    fun testNonEntityPojosList() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val map: Map<ReleasedAlbum, List<AlbumNameAndBandName>> =
            musicDao.getReleaseYearToAlbumsAndBandsList()
        val allReleasedAlbums: Set<ReleasedAlbum> = map.keys
        assertThat(allReleasedAlbums.size).isEqualTo(3)
        allReleasedAlbums.forEach { album ->
            when (album.mAlbumName) {
                stadiumArcadium.mAlbumName -> {
                    assertThat(album.mReleaseYear).isEqualTo(stadiumArcadium.mAlbumReleaseYear)
                    val resultList = map[album] ?: emptyList()
                    assertThat(resultList.size).isEqualTo(2)
                    assertThat(resultList[0].mBandName).isEqualTo(rhcp.mArtistName)
                    assertThat(resultList[0].mAlbumName).isEqualTo(stadiumArcadium.mAlbumName)
                    assertThat(resultList[1].mBandName).isEqualTo(rhcp.mArtistName)
                    assertThat(map[album]!![1].mAlbumName).isEqualTo(stadiumArcadium.mAlbumName)
                }
                highwayToHell.mAlbumName -> {
                    assertThat(album.mReleaseYear).isEqualTo(highwayToHell.mAlbumReleaseYear)
                    val resultList = map[album] ?: emptyList()
                    assertThat(resultList.size).isEqualTo(1)
                    assertThat(resultList[0].mBandName).isEqualTo(acdc.mArtistName)
                    assertThat(resultList[0].mAlbumName).isEqualTo(highwayToHell.mAlbumName)
                }
                theDarkSideOfTheMoon.mAlbumName -> {
                    assertThat(album.mReleaseYear).isEqualTo(theDarkSideOfTheMoon.mAlbumReleaseYear)
                    val resultList = map[album] ?: emptyList()
                    assertThat(resultList.size).isEqualTo(1)
                    assertThat(resultList[0].mBandName).isEqualTo(pinkFloyd.mArtistName)
                    assertThat(resultList[0].mAlbumName).isEqualTo(theDarkSideOfTheMoon.mAlbumName)
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
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongs: ImmutableListMultimap<Artist, Song> =
            musicDao.allArtistAndTheirSongsGuavaImmutableListMultimap()
        assertContentsOfResultMultimap(artistToSongs)
    }

    @Test
    fun testJoinByArtistNameGuavaImmutableSetMultimap() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongs: ImmutableSetMultimap<Artist, Song> =
            musicDao.allArtistAndTheirSongsGuavaImmutableSetMultimap()
        assertContentsOfResultMultimap(artistToSongs)
    }

    @Test
    fun testJoinByArtistNameRawQueryGuavaImmutableListMultimap() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMap: ImmutableListMultimap<Artist, Song> =
            musicDao.getAllArtistAndTheirSongsRawQueryGuavaImmutableListMultimap(
                RoomRawQuery(
                    "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song" + ".mArtist"
                )
            )
        assertThat(artistToSongsMap[acdc]).containsExactly(acdcSong1)
    }

    @Test
    fun testJoinByArtistNameRawQueryGuavaImmutableSetMultimap() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMap: ImmutableSetMultimap<Artist, Song> =
            musicDao.getAllArtistAndTheirSongsRawQueryGuavaImmutableSetMultimap(
                RoomRawQuery(
                    "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song" + ".mArtist"
                )
            )
        assertThat(artistToSongsMap[acdc]).containsExactly(acdcSong1)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testJoinByArtistNameLiveDataGuavaImmutableListMultimap() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMapLiveData: LiveData<ImmutableListMultimap<Artist, Song>> =
            musicDao.allArtistAndTheirSongsAsLiveDataGuavaImmutableListMultimap()
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer = LiveDataTestObserver<ImmutableListMultimap<Artist, Song>>()
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
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMapLiveData: LiveData<ImmutableSetMultimap<Artist, Song>> =
            musicDao.allArtistAndTheirSongsAsLiveDataGuavaImmutableSetMultimap()
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer = LiveDataTestObserver<ImmutableSetMultimap<Artist, Song>>()
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer)
        assertThat(observer.hasValue()).isFalse()
        observer.reset()
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(observer.get()).isNotNull()
        assertContentsOfResultMultimap(observer.get()!!)
    }

    @Test
    fun testPojoWithEmbeddedAndRelationGuavaImmutableListMultimap() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val artistToAlbumsWithSongsMap: ImmutableListMultimap<Artist, AlbumWithSongs> =
            musicDao.allArtistAndTheirAlbumsWithSongsGuavaImmutableListMultimap()
        val rhcpList: ImmutableList<AlbumWithSongs> = artistToAlbumsWithSongsMap[rhcp]
        assertThat(artistToAlbumsWithSongsMap.keySet())
            .containsExactlyElementsIn(listOf<Any>(rhcp, acdc, pinkFloyd))
        assertThat(artistToAlbumsWithSongsMap.containsKey(theClash)).isFalse()
        assertThat(artistToAlbumsWithSongsMap[pinkFloyd][0].album).isEqualTo(theDarkSideOfTheMoon)
        assertThat(artistToAlbumsWithSongsMap[acdc][0].album).isEqualTo(highwayToHell)
        assertThat(artistToAlbumsWithSongsMap[acdc][0].songs[0]).isEqualTo(acdcSong1)
        for (albumAndSong in rhcpList) {
            when (albumAndSong.album) {
                stadiumArcadium -> {
                    assertThat(albumAndSong.songs)
                        .containsExactlyElementsIn(listOf(rhcpSong1, rhcpSong2))
                }
                californication -> {
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
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val artistToAlbumsWithSongsMap: ImmutableSetMultimap<Artist, AlbumWithSongs> =
            musicDao.allArtistAndTheirAlbumsWithSongsGuavaImmutableSetMultimap()
        val rhcpList: ImmutableSet<AlbumWithSongs> = artistToAlbumsWithSongsMap[rhcp]
        assertThat(artistToAlbumsWithSongsMap.keySet())
            .containsExactlyElementsIn(listOf<Any>(rhcp, acdc, pinkFloyd))
        assertThat(artistToAlbumsWithSongsMap.containsKey(theClash)).isFalse()
        assertThat(artistToAlbumsWithSongsMap[pinkFloyd].asList()[0].album)
            .isEqualTo(theDarkSideOfTheMoon)
        assertThat(artistToAlbumsWithSongsMap[acdc].asList()[0].album).isEqualTo(highwayToHell)
        assertThat(artistToAlbumsWithSongsMap[acdc].asList()[0].songs[0]).isEqualTo(acdcSong1)
        for (albumAndSong in rhcpList) {
            when (albumAndSong.album) {
                stadiumArcadium -> {
                    assertThat(albumAndSong.songs)
                        .containsExactlyElementsIn(listOf(rhcpSong1, rhcpSong2))
                }
                californication -> {
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
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMap: ImmutableMap<Artist, List<Song>> =
            musicDao.allArtistAndTheirSongsImmutableMap()
        assertContentsOfResultMapWithList(artistToSongsMap)
    }

    @Test
    fun testJoinByArtistNameRawQueryImmutableMap() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMap: ImmutableMap<Artist, List<Song>> =
            musicDao.getAllArtistAndTheirSongsRawQueryImmutableMap(
                RoomRawQuery(
                    "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song" + ".mArtist"
                )
            )
        assertContentsOfResultMapWithList(artistToSongsMap)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun testJoinByArtistNameImmutableMapWithSet() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistToSongsMapLiveData: LiveData<ImmutableMap<Artist, Set<Song>>> =
            musicDao.allArtistAndTheirSongsAsLiveDataImmutableMap()
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer = LiveDataTestObserver<ImmutableMap<Artist, Set<Song>>>()
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer)
        assertThat(observer.hasValue()).isFalse()
        observer.reset()
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(observer.get()).isNotNull()
        assertContentsOfResultMapWithSet(observer.get()!!)
    }

    @Test
    fun testStringToListOfSongs() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistNameToSongsMap: Map<String, List<Song>> = musicDao.artistNameToSongs()
        assertThat(artistNameToSongsMap.containsKey("Pink Floyd")).isTrue()
        assertThat(artistNameToSongsMap["Red Hot Chili Peppers"])
            .containsExactlyElementsIn(listOf<Any>(rhcpSong1, rhcpSong2))
    }

    @Test
    fun testIntegerToListOfAlbums() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val releaseYearToAlbumsMap: Map<Int, List<Song>> = musicDao.releaseYearToAlbums()
        assertThat(releaseYearToAlbumsMap.containsKey(2006)).isTrue()
        assertThat(releaseYearToAlbumsMap[2006])
            .containsExactlyElementsIn(listOf<Any>(rhcpSong1, rhcpSong2))
        assertThat(releaseYearToAlbumsMap[1979]).containsExactlyElementsIn(listOf<Any>(acdcSong1))
    }

    @Test
    fun testIntegerToStringOfAlbumNames() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val releaseYearToAlbumNameMap: Map<Int, List<String>> = musicDao.releaseYearToSongNames()
        assertThat(releaseYearToAlbumNameMap.containsKey(2006)).isTrue()
        assertThat(releaseYearToAlbumNameMap[2006])
            .containsExactlyElementsIn(listOf("Snow (Hey Oh)", "Dani California"))
    }

    @Test
    fun testStringToListOfSongsRawQuery() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistNameToSongsMap: Map<String, List<Song>> =
            musicDao.getArtistNameToSongsRawQuery(
                RoomRawQuery("SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist")
            )
        assertThat(artistNameToSongsMap.containsKey("Pink Floyd")).isTrue()
        assertThat(artistNameToSongsMap["Red Hot Chili Peppers"])
            .containsExactlyElementsIn(listOf<Any>(rhcpSong1, rhcpSong2))
    }

    @Test
    fun testIntegerToListOfAlbumsRawQuery() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val releaseYearToAlbumsMap: Map<Int, List<Song>> =
            musicDao.getReleaseYearToAlbumsRawQuery(
                RoomRawQuery(
                    "SELECT * FROM Album JOIN Song ON Song.mReleasedYear = Album" +
                        ".mAlbumReleaseYear"
                )
            )
        assertThat(releaseYearToAlbumsMap.containsKey(2006)).isTrue()
        assertThat(releaseYearToAlbumsMap[2006])
            .containsExactlyElementsIn(listOf<Any>(rhcpSong1, rhcpSong2))
        assertThat(releaseYearToAlbumsMap[1979]).containsExactlyElementsIn(listOf<Any>(acdcSong1))
    }

    @Test
    fun testIntegerToStringOfAlbumNamesRawQuery() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val releaseYearToAlbumNameMap: Map<Int, List<String>> =
            musicDao.getReleaseYearToSongNamesRawQuery(
                RoomRawQuery(
                    "SELECT * FROM Album JOIN Song ON Song.mReleasedYear = Album" +
                        ".mAlbumReleaseYear"
                )
            )
        assertThat(releaseYearToAlbumNameMap.containsKey(2006)).isTrue()
        assertThat(releaseYearToAlbumNameMap[2006])
            .containsExactlyElementsIn(listOf("Snow (Hey Oh)", "Dani California"))
    }

    @Test
    fun testArtistToSongCount() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistNameToSongsMap: Map<Artist, Int> = musicDao.artistAndSongCountMap()
        assertThat(artistNameToSongsMap.containsKey(pinkFloyd)).isTrue()
        assertThat(artistNameToSongsMap[rhcp]).isEqualTo(2)
    }

    @Test
    fun testArtistToSongCountWithRawQuery() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistNameToSongsMap: Map<Artist, Int> =
            musicDao.getArtistAndSongCountMapRawQuery(
                RoomRawQuery(
                    "SELECT *, COUNT(mSongId) as songCount FROM Artist JOIN Song ON Artist" +
                        ".mArtistName = Song.mArtist GROUP BY mArtistName"
                )
            )
        assertThat(artistNameToSongsMap.containsKey(pinkFloyd)).isTrue()
        assertThat(artistNameToSongsMap[rhcp]).isEqualTo(2)
    }

    @Test
    fun testArtistToImage() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val artistNameToImagesMap: ImmutableMap<Artist, ByteBuffer> =
            musicDao.allArtistsWithAlbumCovers()
        assertThat(artistNameToImagesMap.containsKey(pinkFloyd)).isTrue()
        assertThat(artistNameToImagesMap[rhcp])
            .isEqualTo(ByteBuffer.wrap("stadium_arcadium_image".toByteArray()))
    }

    @Test
    fun testArtistToImageRawQuery() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val artistNameToImagesMap: ImmutableMap<Artist, ByteBuffer> =
            musicDao.getAllArtistsWithAlbumCoversRawQuery(
                RoomRawQuery(
                    "SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image" +
                        ".mArtistInImage"
                )
            )
        assertThat(artistNameToImagesMap.containsKey(pinkFloyd)).isTrue()
        assertThat(artistNameToImagesMap[rhcp])
            .isEqualTo(ByteBuffer.wrap("stadium_arcadium_image".toByteArray()))
    }

    @Test
    fun testArtistToImageYear() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val artistNameToImagesMap: ImmutableMap<Artist, Long> =
            musicDao.allArtistsWithAlbumCoverYear()
        assertThat(artistNameToImagesMap[rhcp]).isEqualTo(2006L)
    }

    @Test
    fun testImageYearToArtistRawQuery() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<Long, Artist> =
            musicDao.getAllAlbumCoverYearToArtistsWithRawQuery(
                RoomRawQuery(
                    "SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image" +
                        ".mArtistInImage"
                )
            )
        assertThat(imageToArtistsMap[2006L]).isEqualTo(rhcp)
        assertThat(imageToArtistsMap.keys).containsExactlyElementsIn(listOf(2006L, 1973L))
    }

    @Test
    fun testAlbumCoversWithBandActivity() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<ByteBuffer, Boolean> =
            musicDao.albumCoversWithBandActivity()
        assertThat(imageToArtistsMap[ByteBuffer.wrap("stadium_arcadium_image".toByteArray())])
            .isEqualTo(true)
        assertThat(imageToArtistsMap[ByteBuffer.wrap("dark_side_of_the_moon_image".toByteArray())])
            .isEqualTo(false)
    }

    @Test
    fun testAlbumCoversWithBandActivityRawQuery() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<ByteBuffer, Boolean> =
            musicDao.getAlbumCoversWithBandActivityRawQuery(
                RoomRawQuery(
                    "SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image" +
                        ".mArtistInImage"
                )
            )
        assertThat(imageToArtistsMap[ByteBuffer.wrap("stadium_arcadium_image".toByteArray())])
            .isEqualTo(true)
        assertThat(imageToArtistsMap[ByteBuffer.wrap("dark_side_of_the_moon_image".toByteArray())])
            .isEqualTo(false)
    }

    @Test
    fun testAlbumReleaseDateWithBandActivity() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<Date, Boolean> = musicDao.albumDateWithBandActivity()
        assertThat(imageToArtistsMap[Date(101779200000L)]).isEqualTo(false)
        assertThat(imageToArtistsMap[Date(1146787200000L)]).isEqualTo(true)
    }

    @Test
    fun testAlbumReleaseDateWithBandActivityRawQuery() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<Date, Boolean> =
            musicDao.getAlbumDateWithBandActivityRawQuery(
                RoomRawQuery(
                    "SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image" +
                        ".mArtistInImage"
                )
            )
        assertThat(imageToArtistsMap[Date(101779200000L)]).isEqualTo(false)
        assertThat(imageToArtistsMap[Date(1146787200000L)]).isEqualTo(true)
    }

    @Test
    fun testEnumMap() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<ImageFormat, Boolean> =
            musicDao.imageFormatWithBandActivity()
        assertThat(imageToArtistsMap[ImageFormat.JPG]).isEqualTo(false)
        assertThat(imageToArtistsMap[ImageFormat.MPEG]).isEqualTo(true)
    }

    @Test
    fun testEnumMapWithRawQuery() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val imageToArtistsMap: ImmutableMap<ImageFormat, Boolean> =
            musicDao.getImageFormatWithBandActivityRawQuery(
                RoomRawQuery(
                    "SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image" +
                        ".mArtistInImage"
                )
            )
        assertThat(imageToArtistsMap[ImageFormat.JPG]).isEqualTo(false)
        assertThat(imageToArtistsMap[ImageFormat.MPEG]).isEqualTo(true)
    }

    @Test
    fun testInvalidMapInfoColumnsWithRawQuery() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        try {
            musicDao.getMapWithInvalidColumnRawQuery(
                RoomRawQuery(
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
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val map: Map<Artist, List<Album>> = musicDao.artistAndAlbumsLeftJoin()
        assertThat(map.containsKey(theClash))
        assertThat(map[theClash]).isEmpty()
    }

    @Test
    fun testLeftJoinGuava() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val map: ImmutableListMultimap<Artist, Album> = musicDao.artistAndAlbumsLeftJoinGuava()
        assertThat(map.containsKey(theClash))
        assertThat(map[theClash]).isEmpty()
    }

    @Test
    fun testNonPojoLeftJoin() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val map: Map<Artist, List<String>> = musicDao.artistAndAlbumNamesLeftJoin()
        assertThat(map.containsKey(theClash))
        assertThat(map[theClash]).isEmpty()
    }

    @Test
    fun nullKeyColumnLeftJoin() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val map: Map<Album, Artist> = musicDao.albumToArtistLeftJoin()
        assertThat(map.containsKey(highwayToHell))
        assertThat(map[highwayToHell]).isEqualTo(acdc)
    }

    @Test
    fun nullValueColumnLeftJoin() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(stadiumArcadium, californication, theDarkSideOfTheMoon, highwayToHell)
        val map: Map<Artist, Album> = musicDao.artistToAlbumLeftJoin()
        assertThat(map.containsKey(acdc))
        assertThat(map[acdc]).isEqualTo(highwayToHell)
    }

    @Test
    fun nullAlbumAddedLeftJoin() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd, glassAnimals)
        musicDao.addAlbums(
            stadiumArcadium,
            californication,
            theDarkSideOfTheMoon,
            highwayToHell,
            dreamland,
        )
        val map: Map<Artist, Album> = musicDao.artistToAlbumLeftJoin()
        assertThat(map.containsKey(glassAnimals)).isFalse()
    }

    @Test
    fun testImageYearToArtistLongSparseArray() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val imageToArtistsMap: LongSparseArray<Artist> =
            musicDao.allAlbumCoverYearToArtistsWithLongSparseArray()
        assertThat(imageToArtistsMap.size()).isEqualTo(2)
        assertThat(imageToArtistsMap[2006L]).isEqualTo(rhcp)
    }

    @Test
    fun testImageYearToArtistSparseArrayCompat() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val imageToArtistsMap: SparseArrayCompat<Artist> =
            musicDao.allAlbumCoverYearToArtistsWithIntSparseArray()
        assertThat(imageToArtistsMap.size()).isEqualTo(2)
        assertThat(imageToArtistsMap[2006]).isEqualTo(rhcp)
        assertThat(imageToArtistsMap[1973]).isEqualTo(pinkFloyd)
    }

    @Test
    fun testImageYearToArtistRawQueryArrayMap() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val imageToArtistsMap: ArrayMap<Long, Artist> =
            musicDao.getAllAlbumCoverYearToArtistsWithRawQueryArrayMap(
                RoomRawQuery(
                    "SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image" +
                        ".mArtistInImage"
                )
            )
        assertThat(imageToArtistsMap[2006L]).isEqualTo(rhcp)
        assertThat(imageToArtistsMap.keys).containsExactlyElementsIn(listOf(2006L, 1973L))
    }

    @Test
    fun testArtistToImageYearArrayMap() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)
        val artistNameToImagesMap: ArrayMap<Artist, Long> =
            musicDao.allArtistsWithAlbumCoverYearArrayMap()
        assertThat(artistNameToImagesMap[rhcp]).isEqualTo(2006L)
    }

    @Test
    fun testSingleNestedMap() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(
            stadiumArcadium,
            californication,
            theDarkSideOfTheMoon,
            highwayToHell,
            dreamland,
        )
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1, rhcpSong3)

        val singleNestedMap = musicDao.getArtistToAlbumsMappedToSongs()
        val rhcpMap = singleNestedMap.getValue(rhcp)
        val stadiumArcadiumList = rhcpMap.getValue(stadiumArcadium)
        val californicationList = rhcpMap.getValue(californication)

        val stadiumArcadiumExpectedList = listOf(rhcpSong1, rhcpSong2)
        val californicationExpectedList = listOf(rhcpSong3)

        assertThat(rhcpMap.keys).containsExactlyElementsIn(listOf(californication, stadiumArcadium))
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)
    }

    @Test
    fun testDoubleNestedMap() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(
            stadiumArcadium,
            californication,
            theDarkSideOfTheMoon,
            highwayToHell,
            dreamland,
        )
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1, rhcpSong3)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover)

        val doubleNestedMap = musicDao.getImageToArtistToAlbumsMappedToSongs()
        val rhcpImageMap = doubleNestedMap.getValue(rhcpAlbumCover)
        val rhcpMap = rhcpImageMap.getValue(rhcp)
        val stadiumArcadiumList = rhcpMap.getValue(stadiumArcadium)
        val californicationList = rhcpMap.getValue(californication)

        val stadiumArcadiumExpectedList = listOf(rhcpSong1, rhcpSong2)
        val californicationExpectedList = listOf(rhcpSong3)

        assertThat(doubleNestedMap.keys)
            .containsExactlyElementsIn(listOf(pinkFloydAlbumCover, rhcpAlbumCover))
        assertThat(rhcpImageMap.keys).containsExactly(rhcp)
        assertThat(rhcpMap.keys).containsExactlyElementsIn(listOf(californication, stadiumArcadium))
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)
    }

    @Test
    fun testSingleNestedMapWithMapInfoLeftJoin() {
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        musicDao.addAlbums(
            stadiumArcadium,
            californication,
            theDarkSideOfTheMoon,
            highwayToHell,
            dreamland,
        )
        musicDao.addSongs(rhcpSong1, acdcSong1, pinkFloydSong1, rhcpSong3)

        val singleNestedMap = musicDao.getArtistToAlbumsMappedToSongNamesMapInfoLeftJoin()
        val rhcpMap = singleNestedMap.getValue(rhcp)

        assertThat(rhcpMap.keys).containsExactlyElementsIn(listOf(californication, stadiumArcadium))
        assertThat(rhcpMap[stadiumArcadium]).isEqualTo(rhcpSong1.mTitle)
        assertThat(rhcpMap[californication]).isEqualTo(rhcpSong3.mTitle)

        // LEFT JOIN Checks
        assertThat(singleNestedMap[theClash]).isEmpty()
    }

    @Test
    fun testDoubleNestedMapWithMapInfoKeyLeftJoin() {
        musicDao.addArtists(rhcp, acdc, pinkFloyd)
        musicDao.addAlbums(
            stadiumArcadium,
            californication,
            theDarkSideOfTheMoon,
            highwayToHell,
            dreamland,
        )
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, rhcpSong3)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover, theClashAlbumCover)

        val doubleNestedMap = musicDao.getImageYearToArtistToAlbumsMappedToSongs()
        val rhcpImageMap = doubleNestedMap.getValue(rhcpAlbumCover.mImageYear)
        val rhcpMap = rhcpImageMap.getValue(rhcp)
        val stadiumArcadiumList = rhcpMap.getValue(stadiumArcadium)
        val californicationList = rhcpMap.getValue(californication)

        val stadiumArcadiumExpectedList = listOf(rhcpSong1, rhcpSong2)
        val californicationExpectedList = listOf(rhcpSong3)

        assertThat(doubleNestedMap.keys)
            .containsExactlyElementsIn(
                listOf(
                    pinkFloydAlbumCover.mImageYear,
                    rhcpAlbumCover.mImageYear,
                    theClashAlbumCover.mImageYear,
                )
            )
        assertThat(rhcpImageMap.keys).containsExactly(rhcp)
        assertThat(rhcpMap.keys).containsExactlyElementsIn(listOf(californication, stadiumArcadium))
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)

        // LEFT JOIN Checks
        assertThat(doubleNestedMap).containsKey(theClashAlbumCover.mImageYear)
        assertThat(doubleNestedMap[theClashAlbumCover.mImageYear]).isEmpty()
        assertThat(doubleNestedMap).containsKey(pinkFloydAlbumCover.mImageYear)
        assertThat(doubleNestedMap[pinkFloydAlbumCover.mImageYear]).containsKey(pinkFloyd)
        assertThat(doubleNestedMap[pinkFloydAlbumCover.mImageYear]!![pinkFloyd])
            .containsKey(theDarkSideOfTheMoon)
        assertThat(
                doubleNestedMap[pinkFloydAlbumCover.mImageYear]!![pinkFloyd]!![theDarkSideOfTheMoon]
            )
            .isEmpty()
    }

    @Test
    fun testNestedMapWithMapInfoKeyAndValue() {
        musicDao.addArtists(rhcp, acdc, pinkFloyd)
        musicDao.addAlbums(
            stadiumArcadium,
            californication,
            theDarkSideOfTheMoon,
            highwayToHell,
            dreamland,
        )
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, rhcpSong3)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover, theClashAlbumCover)

        val doubleNestedMap = musicDao.getNestedMapWithMapInfoKeyAndValue()
        val rhcpImageMap = doubleNestedMap.getValue(rhcpAlbumCover.mImageYear)
        val rhcpMap = rhcpImageMap.getValue(rhcp)
        val stadiumArcadiumList = rhcpMap.getValue(stadiumArcadium)
        val californicationList = rhcpMap.getValue(californication)

        val stadiumArcadiumExpectedList = listOf(rhcpSong1.mTitle, rhcpSong2.mTitle)
        val californicationExpectedList = listOf(rhcpSong3.mTitle)

        assertThat(doubleNestedMap.keys)
            .containsExactlyElementsIn(
                listOf(
                    pinkFloydAlbumCover.mImageYear,
                    rhcpAlbumCover.mImageYear,
                    theClashAlbumCover.mImageYear,
                )
            )
        assertThat(rhcpImageMap.keys).containsExactly(rhcp)
        assertThat(rhcpMap.keys).containsExactlyElementsIn(listOf(californication, stadiumArcadium))
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)

        // LEFT JOIN Checks
        assertThat(doubleNestedMap).containsKey(theClashAlbumCover.mImageYear)
        assertThat(doubleNestedMap[theClashAlbumCover.mImageYear]).isEmpty()
        assertThat(doubleNestedMap).containsKey(pinkFloydAlbumCover.mImageYear)
        assertThat(doubleNestedMap[pinkFloydAlbumCover.mImageYear]).containsKey(pinkFloyd)
        assertThat(doubleNestedMap[pinkFloydAlbumCover.mImageYear]!![pinkFloyd])
            .containsKey(theDarkSideOfTheMoon)
        assertThat(
                doubleNestedMap[pinkFloydAlbumCover.mImageYear]!![pinkFloyd]!![theDarkSideOfTheMoon]
            )
            .isEmpty()
    }

    @Test
    fun testDoubleNestedMapWithMapColumnKeyLeftJoin() {
        musicDao.addArtists(rhcp, acdc, pinkFloyd)
        musicDao.addAlbums(
            stadiumArcadium,
            californication,
            theDarkSideOfTheMoon,
            highwayToHell,
            dreamland,
        )
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, rhcpSong3)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover, theClashAlbumCover)

        val doubleNestedMap = musicDao.getImageYearToArtistToAlbumsToSongsMultiMapColumn()
        val rhcpImageMap = doubleNestedMap.getValue(rhcpAlbumCover)
        val rhcpMap = rhcpImageMap.getValue(rhcp)
        val stadiumArcadiumList = rhcpMap[stadiumArcadium.mAlbumName]
        val californicationList = rhcpMap[californication.mAlbumName]

        val stadiumArcadiumExpectedList = listOf(rhcpSong1.mReleasedYear, rhcpSong2.mReleasedYear)
        val californicationExpectedList = listOf(rhcpSong3.mReleasedYear)

        assertThat(doubleNestedMap.keys)
            .containsExactlyElementsIn(
                listOf(pinkFloydAlbumCover, rhcpAlbumCover, theClashAlbumCover)
            )
        assertThat(rhcpImageMap.keys).containsExactly(rhcp)
        assertThat(rhcpMap.keys)
            .containsExactlyElementsIn(
                listOf(californication.mAlbumName, stadiumArcadium.mAlbumName)
            )
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)

        // LEFT JOIN Checks
        assertThat(doubleNestedMap).containsKey(theClashAlbumCover)
        assertThat(doubleNestedMap[theClashAlbumCover]).isEmpty()
        assertThat(doubleNestedMap).containsKey(pinkFloydAlbumCover)
        assertThat(doubleNestedMap[pinkFloydAlbumCover]).containsKey(pinkFloyd)
        assertThat(doubleNestedMap[pinkFloydAlbumCover]!![pinkFloyd])
            .containsKey(theDarkSideOfTheMoon.mAlbumName)
        assertThat(
                doubleNestedMap[pinkFloydAlbumCover]!![pinkFloyd]!![theDarkSideOfTheMoon.mAlbumName]
            )
            .isEmpty()
    }

    @Test
    fun testDoubleNestedMapWithMapColumnKeyLeftJoinRawQuery() {
        musicDao.addArtists(rhcp, acdc, pinkFloyd)
        musicDao.addAlbums(
            stadiumArcadium,
            californication,
            theDarkSideOfTheMoon,
            highwayToHell,
            dreamland,
        )
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, rhcpSong3)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover, theClashAlbumCover)

        val doubleNestedMap =
            musicDao.getImageYearToArtistToAlbumsToSongsMultiMapColumn(
                RoomRawQuery(
                    """
                SELECT * FROM Image
                LEFT JOIN Artist ON Image.mArtistInImage = Artist.mArtistName
                LEFT JOIN Album ON Artist.mArtistName = Album.mAlbumArtist
                LEFT JOIN Song ON Album.mAlbumName = Song.mAlbum
                """
                )
            )
        val rhcpImageMap = doubleNestedMap.getValue(rhcpAlbumCover)
        val rhcpMap = rhcpImageMap.getValue(rhcp)
        val stadiumArcadiumList = rhcpMap[stadiumArcadium.mAlbumName]
        val californicationList = rhcpMap[californication.mAlbumName]

        val stadiumArcadiumExpectedList = listOf(rhcpSong1.mReleasedYear, rhcpSong2.mReleasedYear)
        val californicationExpectedList = listOf(rhcpSong3.mReleasedYear)

        assertThat(doubleNestedMap.keys)
            .containsExactlyElementsIn(
                listOf(pinkFloydAlbumCover, rhcpAlbumCover, theClashAlbumCover)
            )
        assertThat(rhcpImageMap.keys).containsExactly(rhcp)
        assertThat(rhcpMap.keys)
            .containsExactlyElementsIn(
                listOf(californication.mAlbumName, stadiumArcadium.mAlbumName)
            )
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)

        // LEFT JOIN Checks
        assertThat(doubleNestedMap).containsKey(theClashAlbumCover)
        assertThat(doubleNestedMap[theClashAlbumCover]).isEmpty()
        assertThat(doubleNestedMap).containsKey(pinkFloydAlbumCover)
        assertThat(doubleNestedMap[pinkFloydAlbumCover]).containsKey(pinkFloyd)
        assertThat(doubleNestedMap[pinkFloydAlbumCover]!![pinkFloyd])
            .containsKey(theDarkSideOfTheMoon.mAlbumName)
        assertThat(
                doubleNestedMap[pinkFloydAlbumCover]!![pinkFloyd]!![theDarkSideOfTheMoon.mAlbumName]
            )
            .isEmpty()
    }

    @Test
    fun testStringToListOfSongsMapColumn() {
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, pinkFloydSong1)
        musicDao.addArtists(rhcp, acdc, theClash, pinkFloyd)
        val artistNameToSongsMap: Map<String, List<Int>> = musicDao.artistNameToSongsMapColumn()
        assertThat(artistNameToSongsMap.containsKey("Pink Floyd")).isTrue()
        assertThat(artistNameToSongsMap["Red Hot Chili Peppers"])
            .containsExactlyElementsIn(listOf(rhcpSong1.mReleasedYear, rhcpSong2.mReleasedYear))
    }

    @Test
    fun testDoubleNestedMapWithOneMapColumn() {
        musicDao.addArtists(rhcp, acdc, pinkFloyd)
        musicDao.addAlbums(
            stadiumArcadium,
            californication,
            theDarkSideOfTheMoon,
            highwayToHell,
            dreamland,
        )
        musicDao.addSongs(rhcpSong1, rhcpSong2, acdcSong1, rhcpSong3)
        musicDao.addImages(pinkFloydAlbumCover, rhcpAlbumCover, theClashAlbumCover)

        val doubleNestedMap = musicDao.getImageYearToArtistToAlbumsToSongsMapColumn()
        val rhcpImageMap = doubleNestedMap.getValue(rhcpAlbumCover.mImageYear)
        val rhcpMap = rhcpImageMap.getValue(rhcp)
        val stadiumArcadiumList = rhcpMap.getValue("Stadium Arcadium")
        val californicationList = rhcpMap.getValue("Californication")

        val stadiumArcadiumExpectedList = listOf(rhcpSong1, rhcpSong2)
        val californicationExpectedList = listOf(rhcpSong3)

        assertThat(doubleNestedMap.keys)
            .containsExactlyElementsIn(
                listOf(
                    pinkFloydAlbumCover.mImageYear,
                    rhcpAlbumCover.mImageYear,
                    theClashAlbumCover.mImageYear,
                )
            )
        assertThat(rhcpImageMap.keys).containsExactly(rhcp)
        assertThat(rhcpMap.keys)
            .containsExactlyElementsIn(listOf("Stadium Arcadium", "Californication"))
        assertThat(stadiumArcadiumList).containsExactlyElementsIn(stadiumArcadiumExpectedList)
        assertThat(californicationList).containsExactlyElementsIn(californicationExpectedList)

        // LEFT JOIN Checks
        assertThat(doubleNestedMap).containsKey(theClashAlbumCover.mImageYear)
        assertThat(doubleNestedMap[theClashAlbumCover.mImageYear]).isEmpty()
        assertThat(doubleNestedMap).containsKey(pinkFloydAlbumCover.mImageYear)
        assertThat(doubleNestedMap[pinkFloydAlbumCover.mImageYear]).containsKey(pinkFloyd)
        assertThat(doubleNestedMap[pinkFloydAlbumCover.mImageYear]!![pinkFloyd])
            .containsKey(theDarkSideOfTheMoon.mAlbumName)
        assertThat(
                doubleNestedMap[pinkFloydAlbumCover.mImageYear]!![pinkFloyd]!![
                    theDarkSideOfTheMoon.mAlbumName]
            )
            .isEmpty()
    }

    /**
     * Checks that the contents of the map are as expected.
     *
     * @param artistToSongsMap Map of Artists to list of Songs joined by the artist name
     */
    private fun assertContentsOfResultMapWithList(artistToSongsMap: Map<Artist, List<Song>>) {
        assertThat(artistToSongsMap.keys)
            .containsExactlyElementsIn(listOf<Any>(rhcp, acdc, pinkFloyd))
        assertThat(artistToSongsMap.containsKey(theClash)).isFalse()
        assertThat(artistToSongsMap[pinkFloyd]).containsExactly(pinkFloydSong1)
        assertThat(artistToSongsMap[rhcp])
            .containsExactlyElementsIn(listOf<Any>(rhcpSong1, rhcpSong2))
        assertThat(artistToSongsMap[acdc]).containsExactly(acdcSong1)
    }

    /**
     * Checks that the contents of the map are as expected.
     *
     * @param artistToSongsMap Map of Artists to set of Songs joined by the artist name
     */
    private fun assertContentsOfResultMapWithSet(artistToSongsMap: Map<Artist, Set<Song>>) {
        assertThat(artistToSongsMap.keys)
            .containsExactlyElementsIn(listOf<Any>(rhcp, acdc, pinkFloyd))
        assertThat(artistToSongsMap.containsKey(theClash)).isFalse()
        assertThat(artistToSongsMap[pinkFloyd]).containsExactly(pinkFloydSong1)
        assertThat(artistToSongsMap[rhcp])
            .containsExactlyElementsIn(listOf<Any>(rhcpSong1, rhcpSong2))
        assertThat(artistToSongsMap[acdc]).containsExactly(acdcSong1)
    }

    /**
     * Checks that the contents of the map are as expected.
     *
     * @param artistToSongsMap Map of Artists to Collection of Songs joined by the artist name
     */
    private fun assertContentsOfResultMultimap(artistToSongsMap: ImmutableMultimap<Artist, Song>) {
        assertThat(artistToSongsMap.keySet())
            .containsExactlyElementsIn(listOf<Any>(rhcp, acdc, pinkFloyd))
        assertThat(artistToSongsMap.containsKey(theClash)).isFalse()
        assertThat(artistToSongsMap[pinkFloyd]).containsExactly(pinkFloydSong1)
        assertThat(artistToSongsMap[rhcp])
            .containsExactlyElementsIn(listOf<Any>(rhcpSong1, rhcpSong2))
        assertThat(artistToSongsMap[acdc]).containsExactly(acdcSong1)
    }
}
