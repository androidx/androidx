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

package androidx.room.integration.testapp.test;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.testing.TestLifecycleOwner;
import androidx.room.Room;
import androidx.room.integration.testapp.MusicTestDatabase;
import androidx.room.integration.testapp.dao.MusicDao;
import androidx.room.integration.testapp.vo.Album;
import androidx.room.integration.testapp.vo.AlbumNameAndBandName;
import androidx.room.integration.testapp.vo.AlbumWithSongs;
import androidx.room.integration.testapp.vo.Artist;
import androidx.room.integration.testapp.vo.ReleasedAlbum;
import androidx.room.integration.testapp.vo.Song;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.google.common.collect.ImmutableMap;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Flowable;

/**
 * Tests multimap return type for JOIN statements.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class MultimapQueryTest {
    // TODO: (b/191265082) Handle duplicate column names in JOINs
    private MusicDao mMusicDao;

    private final Song mRhcpSong1 = new Song(
            1,
            "Dani California",
            "Red Hot Chili Peppers",
            "Stadium Arcadium",
            442,
            2006);
    private final Song mRhcpSong2 = new Song(
            2,
            "Snow (Hey Oh)",
            "Red Hot Chili Peppers",
            "Stadium Arcadium",
            514,
            2006);
    private final Song mAcdcSong1 = new Song(
            3,
            "Highway to Hell",
            "AC/DC",
            "Highway to Hell",
            328,
            1979);
    private final Song mPinkFloydSong1 = new Song(
            4,
            "The Great Gig in the Sky",
            "Pink Floyd",
            "The Dark Side of the Moon",
            443,
            1973);

    private final Artist mRhcp = new Artist(
            1,
            "Red Hot Chili Peppers"
    );
    private final Artist mAcDc = new Artist(
            2,
            "AC/DC"
    );
    private final Artist mTheClash = new Artist(
            3,
            "The Clash"
    );
    private final Artist mPinkFloyd = new Artist(
            4,
            "Pink Floyd"
    );

    private final Album mStadiumArcadium = new Album(
            1,
            "Stadium Arcadium",
            "Red Hot Chili Peppers",
            "2006"
    );

    private final Album mCalifornication = new Album(
            2,
            "Californication",
            "Red Hot Chili Peppers",
            "1999"
    );

    private final Album mHighwayToHell = new Album(
            3,
            "Highway to Hell",
            "AC/DC",
            "1979"
    );

    private final Album mTheDarkSideOfTheMoon = new Album(
            4,
            "The Dark Side of the Moon",
            "Pink Floyd",
            "1973"
    );

    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();

    private void drain() throws TimeoutException, InterruptedException {
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
        assertThat(mExecutorRule.isIdle()).isTrue();
    }

    private class MyTestObserver<T> extends TestObserver<T> {
        @Override
        protected void drain() throws TimeoutException, InterruptedException {
            MultimapQueryTest.this.drain();
        }
    }

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        MusicTestDatabase db = Room.inMemoryDatabaseBuilder(context, MusicTestDatabase.class)
                .build();
        mMusicDao = db.getDao();
    }

    /**
     * Tests a simple JOIN query between two tables.
     */
    @Test
    public void testGetFirstSongForArtist() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        Map<Artist, Song> artistToSongsMap = mMusicDao.getArtistAndFirstSongMap();
        assertThat(artistToSongsMap.get(mAcDc)).isEqualTo(mAcdcSong1);
        assertThat(artistToSongsMap.get(mRhcp)).isEqualTo(mRhcpSong1);
    }

    @Test
    public void testGetSongToArtistMapping() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        Map<Song, Artist> songToArtistMap = mMusicDao.getSongAndArtist();
        assertThat(songToArtistMap.get(mAcdcSong1)).isEqualTo(mAcDc);
        assertThat(songToArtistMap.get(mPinkFloydSong1)).isEqualTo(mPinkFloyd);
        assertThat(songToArtistMap.get(mRhcpSong1)).isEqualTo(mRhcp);
        assertThat(songToArtistMap.get(mRhcpSong2)).isEqualTo(mRhcp);
    }

    @Test
    public void testJoinByArtistNameList() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        Map<Artist, List<Song>> artistToSongsMap = mMusicDao.getAllArtistAndTheirSongsList();
        assertContentsOfResultMapWithList(artistToSongsMap);
    }

    @Test
    public void testJoinByArtistNameSet() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        Map<Artist, Set<Song>> artistToSongsSet = mMusicDao.getAllArtistAndTheirSongsSet();
        assertContentsOfResultMapWithSet(artistToSongsSet);
    }

    /**
     * Tests a JOIN using {@link androidx.room.RawQuery} between two tables.
     */
    @Test
    public void testJoinByArtistNameRawQuery() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        Map<Artist, Song> artistToSongsMap = mMusicDao.getAllArtistAndTheirSongsRawQuery(
                new SimpleSQLiteQuery(
                        "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist"
                )
        );
        assertThat(artistToSongsMap.get(mAcDc)).isEqualTo(mAcdcSong1);
    }

    @Test
    public void testJoinByArtistNameRawQueryList() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        Map<Artist, List<Song>> artistToSongsMap = mMusicDao.getAllArtistAndTheirSongsRawQueryList(
                new SimpleSQLiteQuery(
                        "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist"
                )
        );
        assertContentsOfResultMapWithList(artistToSongsMap);
    }

    @Test
    public void testJoinByArtistNameRawQuerySet() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        Map<Artist, Set<Song>> artistToSongsMap = mMusicDao.getAllArtistAndTheirSongsRawQuerySet(
                new SimpleSQLiteQuery(
                        "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song.mArtist"
                )
        );
        assertContentsOfResultMapWithSet(artistToSongsMap);
    }

    /**
     * Tests a simple JOIN query between two tables with a {@link LiveData} map return type.
     */
    @Test
    public void testJoinByArtistNameLiveData()
            throws ExecutionException, InterruptedException, TimeoutException {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        LiveData<Map<Artist, Song>> artistToSongsMapLiveData =
                mMusicDao.getAllArtistAndTheirSongsAsLiveData();
        final TestLifecycleOwner testOwner = new TestLifecycleOwner(Lifecycle.State.CREATED);
        final TestObserver<Map<Artist, Song>> observer = new MyTestObserver<>();
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer);
        MatcherAssert.assertThat(observer.hasValue(), is(false));
        observer.reset();
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(observer.get()).isNotNull();
        assertThat(observer.get().get(mAcDc)).isEqualTo(mAcdcSong1);
    }

    @Test
    public void testJoinByArtistNameLiveDataList()
            throws ExecutionException, InterruptedException, TimeoutException {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        LiveData<Map<Artist, List<Song>>> artistToSongsMapLiveData =
                mMusicDao.getAllArtistAndTheirSongsAsLiveDataList();
        final TestLifecycleOwner testOwner = new TestLifecycleOwner(Lifecycle.State.CREATED);
        final TestObserver<Map<Artist, List<Song>>> observer = new MyTestObserver<>();
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer);
        MatcherAssert.assertThat(observer.hasValue(), is(false));
        observer.reset();
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(observer.get()).isNotNull();
        assertContentsOfResultMapWithList(observer.get());
    }

    @Test
    public void testJoinByArtistNameLiveDataSet()
            throws ExecutionException, InterruptedException, TimeoutException {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        LiveData<Map<Artist, Set<Song>>> artistToSongsMapLiveData =
                mMusicDao.getAllArtistAndTheirSongsAsLiveDataSet();
        final TestLifecycleOwner testOwner = new TestLifecycleOwner(Lifecycle.State.CREATED);
        final TestObserver<Map<Artist, Set<Song>>> observer = new MyTestObserver<>();
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer);
        MatcherAssert.assertThat(observer.hasValue(), is(false));
        observer.reset();
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(observer.get()).isNotNull();
        assertContentsOfResultMapWithSet(observer.get());
    }

    /**
     * Tests a simple JOIN query between two tables with a {@link Flowable} map return type.
     */
    @Test
    public void testJoinByArtistNameFlowableList() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        Flowable<Map<Artist, List<Song>>> artistToSongsMapFlowable =
                mMusicDao.getAllArtistAndTheirSongsAsFlowableList();
        assertContentsOfResultMapWithList(artistToSongsMapFlowable.blockingFirst());
    }

    @Test
    public void testJoinByArtistNameFlowableSet() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        Flowable<Map<Artist, Set<Song>>> artistToSongsMapFlowable =
                mMusicDao.getAllArtistAndTheirSongsAsFlowableSet();
        assertContentsOfResultMapWithSet(artistToSongsMapFlowable.blockingFirst());
    }

    /**
     * Tests a simple JOIN query between two tables with a return type of a map with a key that
     * is an entity {@link Artist} and a POJO {@link AlbumWithSongs} that use
     * {@link androidx.room.Embedded} and {@link androidx.room.Relation}.
     */
    @Test
    public void testPojoWithEmbeddedAndRelation() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);
        mMusicDao.addAlbums(
                mStadiumArcadium,
                mCalifornication,
                mTheDarkSideOfTheMoon,
                mHighwayToHell
        );

        Map<Artist, AlbumWithSongs> artistToAlbumsWithSongsMap =
                mMusicDao.getAllArtistAndTheirAlbumsWithSongs();
        AlbumWithSongs rhcpAlbum = artistToAlbumsWithSongsMap.get(mRhcp);

        assertThat(artistToAlbumsWithSongsMap.keySet()).containsExactlyElementsIn(
                Arrays.asList(mRhcp, mAcDc, mPinkFloyd));
        assertThat(artistToAlbumsWithSongsMap.containsKey(mTheClash)).isFalse();
        assertThat(artistToAlbumsWithSongsMap.get(mPinkFloyd).getAlbum())
                .isEqualTo(mTheDarkSideOfTheMoon);
        assertThat(artistToAlbumsWithSongsMap.get(mAcDc).getAlbum())
                .isEqualTo(mHighwayToHell);
        assertThat(artistToAlbumsWithSongsMap.get(mAcDc).getSongs().get(0)).isEqualTo(mAcdcSong1);

        if (rhcpAlbum.getAlbum().equals(mStadiumArcadium)) {
            assertThat(rhcpAlbum.getSongs()).containsExactlyElementsIn(
                    Arrays.asList(mRhcpSong1, mRhcpSong2)
            );
        } else if (rhcpAlbum.getAlbum().equals(mCalifornication)) {
            assertThat(rhcpAlbum.getSongs()).isEmpty();
        } else {
            fail();
        }
    }

    /**
     * Tests a simple JOIN query between two tables with a return type of a map with a key that
     * is an entity {@link Artist} and a list of entity POJOs {@link AlbumWithSongs} that use
     * {@link androidx.room.Embedded} and {@link androidx.room.Relation}.
     */
    @Test
    public void testPojoWithEmbeddedAndRelationList() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);
        mMusicDao.addAlbums(
                mStadiumArcadium,
                mCalifornication,
                mTheDarkSideOfTheMoon,
                mHighwayToHell
        );

        Map<Artist, List<AlbumWithSongs>> artistToAlbumsWithSongsMap =
                mMusicDao.getAllArtistAndTheirAlbumsWithSongsList();
        mMusicDao.getAllArtistAndTheirAlbumsWithSongs();
        List<AlbumWithSongs> rhcpList = artistToAlbumsWithSongsMap.get(mRhcp);

        assertThat(artistToAlbumsWithSongsMap.keySet()).containsExactlyElementsIn(
                Arrays.asList(mRhcp, mAcDc, mPinkFloyd));
        assertThat(artistToAlbumsWithSongsMap.containsKey(mTheClash)).isFalse();
        assertThat(artistToAlbumsWithSongsMap.get(mPinkFloyd).get(0).getAlbum())
                .isEqualTo(mTheDarkSideOfTheMoon);
        assertThat(artistToAlbumsWithSongsMap.get(mAcDc).get(0).getAlbum())
                .isEqualTo(mHighwayToHell);
        assertThat(artistToAlbumsWithSongsMap.get(mAcDc).get(0).getSongs().get(0))
                .isEqualTo(mAcdcSong1);

        for (AlbumWithSongs albumAndSong : rhcpList) {
            if (albumAndSong.getAlbum().equals(mStadiumArcadium)) {
                assertThat(albumAndSong.getSongs()).containsExactlyElementsIn(
                        Arrays.asList(mRhcpSong1, mRhcpSong2)
                );
            } else if (albumAndSong.getAlbum().equals(mCalifornication)) {
                assertThat(albumAndSong.getSongs()).isEmpty();
            } else {
                fail();
            }
        }
    }

    /**
     * Tests a simple JOIN query between two tables with a return type of a map with a key
     * {@link ReleasedAlbum} and value (list of {@link AlbumNameAndBandName}) that are non-entity
     * POJOs.
     */
    @Test
    public void testNonEntityPojosList() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);
        mMusicDao.addAlbums(
                mStadiumArcadium,
                mCalifornication,
                mTheDarkSideOfTheMoon,
                mHighwayToHell
        );

        Map<ReleasedAlbum, List<AlbumNameAndBandName>> map =
                mMusicDao.getReleaseYearToAlbumsAndBandsList();
        Set<ReleasedAlbum> allReleasedAlbums = map.keySet();

        assertThat(allReleasedAlbums.size()).isEqualTo(3);

        for (ReleasedAlbum album : allReleasedAlbums) {
            if (album.getAlbumName().equals(mStadiumArcadium.mAlbumName)) {
                assertThat(album.getReleaseYear()).isEqualTo(
                        mStadiumArcadium.mAlbumReleaseYear);
                assertThat(map.get(album).size()).isEqualTo(2);
                assertThat(map.get(album).get(0).getBandName()).isEqualTo(mRhcp.mArtistName);
                assertThat(map.get(album).get(0).getAlbumName())
                        .isEqualTo(mStadiumArcadium.mAlbumName);
                assertThat(map.get(album).get(1).getBandName()).isEqualTo(mRhcp.mArtistName);
                assertThat(map.get(album).get(1).getAlbumName())
                        .isEqualTo(mStadiumArcadium.mAlbumName);

            } else if (album.getAlbumName().equals(mHighwayToHell.mAlbumName)) {
                assertThat(album.getReleaseYear()).isEqualTo(mHighwayToHell.mAlbumReleaseYear);
                assertThat(map.get(album).size()).isEqualTo(1);
                assertThat(map.get(album).get(0).getBandName()).isEqualTo(mAcDc.mArtistName);
                assertThat(map.get(album).get(0).getAlbumName())
                        .isEqualTo(mHighwayToHell.mAlbumName);

            } else if (album.getAlbumName().equals(mTheDarkSideOfTheMoon.mAlbumName)) {
                assertThat(album.getReleaseYear())
                        .isEqualTo(mTheDarkSideOfTheMoon.mAlbumReleaseYear);
                assertThat(map.get(album).size()).isEqualTo(1);
                assertThat(map.get(album).get(0).getBandName())
                        .isEqualTo(mPinkFloyd.mArtistName);
                assertThat(map.get(album).get(0).getAlbumName())
                        .isEqualTo(mTheDarkSideOfTheMoon.mAlbumName);

            } else {
                // Shouldn't get here as we expect only the 3 albums to be keys in the map
                fail();
            }
        }
    }

    @Test
    public void testJoinByArtistNameImmutableMap() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        ImmutableMap<Artist, List<Song>> artistToSongsMap =
                mMusicDao.getAllArtistAndTheirSongsImmutableMap();
        assertContentsOfResultMapWithList(artistToSongsMap);
    }

    @Test
    public void testJoinByArtistNameRawQueryImmutableMap() {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);
        ImmutableMap<Artist, List<Song>> artistToSongsMap =
                mMusicDao.getAllArtistAndTheirSongsRawQueryImmutableMap(
                        new SimpleSQLiteQuery(
                                "SELECT * FROM Artist JOIN Song ON Artist.mArtistName = Song"
                                        + ".mArtist"
                        )
                );
        assertContentsOfResultMapWithList(artistToSongsMap);
    }

    @Test
    public void testJoinByArtistNameImmutableMapWithSet()
            throws ExecutionException, InterruptedException, TimeoutException {
        mMusicDao.addSongs(mRhcpSong1, mRhcpSong2, mAcdcSong1, mPinkFloydSong1);
        mMusicDao.addArtists(mRhcp, mAcDc, mTheClash, mPinkFloyd);

        LiveData<ImmutableMap<Artist, Set<Song>>> artistToSongsMapLiveData =
                mMusicDao.getAllArtistAndTheirSongsAsLiveDataImmutableMap();
        final TestLifecycleOwner testOwner = new TestLifecycleOwner(Lifecycle.State.CREATED);
        final TestObserver<Map<Artist, Set<Song>>> observer = new MyTestObserver<>();
        TestUtil.observeOnMainThread(artistToSongsMapLiveData, testOwner, observer);
        MatcherAssert.assertThat(observer.hasValue(), is(false));
        observer.reset();
        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(observer.get()).isNotNull();
        assertContentsOfResultMapWithSet(observer.get());
    }

    /**
     * Checks that the contents of the map are as expected.
     *
     * @param artistToSongsMap Map of Artists to list of Songs joined by the artist name
     */
    private void assertContentsOfResultMapWithList(Map<Artist, List<Song>> artistToSongsMap) {
        assertThat(artistToSongsMap.keySet()).containsExactlyElementsIn(
                Arrays.asList(mRhcp, mAcDc, mPinkFloyd));
        assertThat(artistToSongsMap.containsKey(mTheClash)).isFalse();
        assertThat(artistToSongsMap.get(mPinkFloyd)).containsExactly(mPinkFloydSong1);
        assertThat(artistToSongsMap.get(mRhcp)).containsExactlyElementsIn(
                Arrays.asList(mRhcpSong1, mRhcpSong2)
        );
        assertThat(artistToSongsMap.get(mAcDc)).containsExactly(mAcdcSong1);
    }

    /**
     * Checks that the contents of the map are as expected.
     *
     * @param artistToSongsMap Map of Artists to set of Songs joined by the artist name
     */
    private void assertContentsOfResultMapWithSet(Map<Artist, Set<Song>> artistToSongsMap) {
        assertThat(artistToSongsMap.keySet()).containsExactlyElementsIn(
                Arrays.asList(mRhcp, mAcDc, mPinkFloyd));
        assertThat(artistToSongsMap.containsKey(mTheClash)).isFalse();
        assertThat(artistToSongsMap.get(mPinkFloyd)).containsExactly(mPinkFloydSong1);
        assertThat(artistToSongsMap.get(mRhcp)).containsExactlyElementsIn(
                Arrays.asList(mRhcpSong1, mRhcpSong2)
        );
        assertThat(artistToSongsMap.get(mAcDc)).containsExactly(mAcdcSong1);
    }
}