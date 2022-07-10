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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.os.Build;

import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.testing.TestLifecycleOwner;
import androidx.room.InvalidationTrackerTrojan;
import androidx.room.Room;
import androidx.room.integration.testapp.FtsTestDatabase;
import androidx.room.integration.testapp.MusicTestDatabase;
import androidx.room.integration.testapp.dao.MailDao;
import androidx.room.integration.testapp.dao.MusicDao;
import androidx.room.integration.testapp.dao.SongDao;
import androidx.room.integration.testapp.vo.AvgWeightByAge;
import androidx.room.integration.testapp.vo.Mail;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.PetWithUser;
import androidx.room.integration.testapp.vo.PetsToys;
import androidx.room.integration.testapp.vo.Playlist;
import androidx.room.integration.testapp.vo.PlaylistSongXRef;
import androidx.room.integration.testapp.vo.PlaylistWithSongs;
import androidx.room.integration.testapp.vo.Song;
import androidx.room.integration.testapp.vo.SongDescription;
import androidx.room.integration.testapp.vo.Toy;
import androidx.room.integration.testapp.vo.User;
import androidx.room.integration.testapp.vo.UserAndAllPets;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests invalidation tracking.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class LiveDataQueryTest extends TestDatabaseTest {
    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();

    @Test
    public void observeById() throws InterruptedException, ExecutionException, TimeoutException {
        final LiveData<User> userLiveData = mUserDao.liveUserById(5);
        final TestLifecycleOwner testOwner = new TestLifecycleOwner(Lifecycle.State.CREATED);
        final TestObserver<User> observer = new MyTestObserver<>();
        TestUtil.observeOnMainThread(userLiveData, testOwner, observer);
        assertThat(observer.hasValue(), is(false));
        observer.reset();

        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);
        assertThat(observer.get(), is(nullValue()));

        // another id
        observer.reset();
        mUserDao.insert(TestUtil.createUser(7));
        assertThat(observer.get(), is(nullValue()));

        observer.reset();
        final User u5 = TestUtil.createUser(5);
        mUserDao.insert(u5);
        assertThat(observer.get(), is(notNullValue()));

        u5.setName("foo-foo-foo");
        observer.reset();
        mUserDao.insertOrReplace(u5);
        final User updated = observer.get();
        assertThat(updated, is(notNullValue()));
        assertThat(updated.getName(), is("foo-foo-foo"));

        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        observer.reset();
        u5.setName("baba");
        mUserDao.insertOrReplace(u5);
        assertThat(observer.hasValue(), is(false));
    }

    @Test
    public void observeListQuery() throws InterruptedException, ExecutionException,
            TimeoutException {
        final LiveData<List<User>> userLiveData = mUserDao.liveUsersListByName("frida");
        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        final TestObserver<List<User>> observer = new MyTestObserver<>();
        TestUtil.observeOnMainThread(userLiveData, lifecycleOwner, observer);
        assertThat(observer.get(), is(Collections.<User>emptyList()));

        observer.reset();
        final User user1 = TestUtil.createUser(3);
        user1.setName("dog frida");
        mUserDao.insert(user1);
        assertThat(observer.get(), is(Collections.singletonList(user1)));

        observer.reset();
        final User user2 = TestUtil.createUser(5);
        user2.setName("does not match");
        mUserDao.insert(user2);
        assertThat(observer.get(), is(Collections.singletonList(user1)));

        observer.reset();
        user1.setName("i don't match either");
        mUserDao.insertOrReplace(user1);
        assertThat(observer.get(), is(Collections.<User>emptyList()));

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        observer.reset();
        final User user3 = TestUtil.createUser(9);
        user3.setName("painter frida");
        mUserDao.insertOrReplace(user3);
        assertThat(observer.hasValue(), is(false));

        observer.reset();
        final User user4 = TestUtil.createUser(11);
        user4.setName("friday");
        mUserDao.insertOrReplace(user4);
        assertThat(observer.hasValue(), is(false));

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);
        assertThat(observer.get(), is(Arrays.asList(user4, user3)));
    }

    @Test
    public void liveDataWithPojo() throws ExecutionException, InterruptedException,
            TimeoutException {
        User[] users = TestUtil.createUsersArray(3, 5, 7, 9);
        users[0].setAge(10);
        users[0].setWeight(15);

        users[1].setAge(20);
        users[1].setWeight(25);

        users[2].setAge(20);
        users[2].setWeight(26);

        users[3].setAge(10);
        users[3].setWeight(21);

        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();

        final TestObserver<AvgWeightByAge> observer = new MyTestObserver<>();
        LiveData<AvgWeightByAge> liveData = mUserDao.maxWeightByAgeGroup();

        TestUtil.observeOnMainThread(liveData, lifecycleOwner, observer);
        assertThat(observer.get(), is(nullValue()));

        observer.reset();
        mUserDao.insertAll(users);
        assertThat(observer.get(), is(new AvgWeightByAge(20, 25.5f)));

        observer.reset();
        User user3 = mUserDao.load(3);
        user3.setWeight(79);
        mUserDao.insertOrReplace(user3);

        assertThat(observer.get(), is(new AvgWeightByAge(10, 50)));
    }

    @Test
    public void liveDataWithView() throws ExecutionException, InterruptedException,
            TimeoutException {
        User user = TestUtil.createUser(1);
        Pet pet = TestUtil.createPet(3);
        pet.setUserId(user.getId());

        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();

        final TestObserver<PetWithUser> observer = new MyTestObserver<>();
        LiveData<PetWithUser> liveData = mPetDao.petWithUserLiveData(3);

        TestUtil.observeOnMainThread(liveData, lifecycleOwner, observer);
        assertThat(observer.get(), is(nullValue()));

        observer.reset();
        mUserDao.insert(user);
        mPetDao.insertOrReplace(pet);
        PetWithUser petWithUser = observer.get();
        assertThat(petWithUser.pet, is(equalTo(pet)));
        assertThat(petWithUser.user, is(equalTo(user)));
    }

    @Test
    public void withRelation() throws ExecutionException, InterruptedException, TimeoutException {
        final LiveData<UserAndAllPets> liveData = mUserPetDao.liveUserWithPets(3);
        final TestObserver<UserAndAllPets> observer = new MyTestObserver<>();
        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        TestUtil.observeOnMainThread(liveData, lifecycleOwner, observer);
        assertThat(observer.get(), is(nullValue()));

        observer.reset();
        User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        final UserAndAllPets noPets = observer.get();
        assertThat(noPets.user, is(user));

        observer.reset();
        Pet[] pets = TestUtil.createPetsForUser(3, 1, 2);
        mPetDao.insertAll(pets);

        final UserAndAllPets withPets = observer.get();
        assertThat(withPets.user, is(user));
        assertThat(withPets.pets, is(Arrays.asList(pets)));
    }

    @Test
    public void withRelationOnly() throws ExecutionException, InterruptedException,
            TimeoutException {
        LiveData<PetsToys> liveData = mSpecificDogDao.getSpecificDogsToys();

        PetsToys expected = new PetsToys();
        expected.petId = 123;

        Toy toy = new Toy();
        toy.setId(1);
        toy.setPetId(123);
        toy.setName("ball");

        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        final TestObserver<PetsToys> observer = new MyTestObserver<>();
        TestUtil.observeOnMainThread(liveData, lifecycleOwner, observer);
        assertThat(observer.get(), is(expected));

        observer.reset();
        expected.toys.add(toy);
        mToyDao.insert(toy);
        assertThat(observer.get(), is(expected));
    }

    @Test
    public void withRelationAndJunction() throws ExecutionException, InterruptedException,
            TimeoutException {
        Context context = ApplicationProvider.getApplicationContext();
        final MusicTestDatabase db = Room.inMemoryDatabaseBuilder(context, MusicTestDatabase.class)
                .build();
        final MusicDao musicDao = db.getDao();

        final Song mSong1 = new Song(
                1,
                "I Know Places",
                "Taylor Swift",
                "1989",
                195,
                2014);
        final Song mSong2 = new Song(
                2,
                "Blank Space",
                "Taylor Swift",
                "1989",
                241,
                2014);

        final Playlist mPlaylist1 = new Playlist(1);
        final Playlist mPlaylist2 = new Playlist(2);

        musicDao.addSongs(mSong1, mSong2);
        musicDao.addPlaylists(mPlaylist1, mPlaylist2);

        musicDao.addPlaylistSongRelation(new PlaylistSongXRef(1, 1));

        LiveData<PlaylistWithSongs> liveData = musicDao.getPlaylistsWithSongsLiveData(1);

        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        final TestObserver<PlaylistWithSongs> observer = new MyTestObserver<>();
        TestUtil.observeOnMainThread(liveData, lifecycleOwner, observer);

        assertThat(observer.get().songs.size(), is(1));
        assertThat(observer.get().songs.get(0), is(mSong1));

        observer.reset();

        musicDao.addPlaylistSongRelation(new PlaylistSongXRef(1, 2));

        assertThat(observer.get().songs.size(), is(2));
        assertThat(observer.get().songs.get(0), is(mSong1));
        assertThat(observer.get().songs.get(1), is(mSong2));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void withWithClause() throws ExecutionException, InterruptedException,
            TimeoutException {
        LiveData<List<String>> actual =
                mWithClauseDao.getUsersWithFactorialIdsLiveData(0);
        List<String> expected = new ArrayList<>();

        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        final TestObserver<List<String>> observer = new MyTestObserver<>();
        TestUtil.observeOnMainThread(actual, lifecycleOwner, observer);
        assertThat(observer.get(), is(expected));

        observer.reset();
        User user = new User();
        user.setId(0);
        user.setName("Zero");
        mUserDao.insert(user);
        assertThat(observer.get(), is(expected));

        observer.reset();
        user = new User();
        user.setId(1);
        user.setName("One");
        mUserDao.insert(user);
        expected.add("One");
        assertThat(observer.get(), is(expected));

        observer.reset();
        user = new User();
        user.setId(6);
        user.setName("Six");
        mUserDao.insert(user);
        assertThat(observer.get(), is(expected));

        actual = mWithClauseDao.getUsersWithFactorialIdsLiveData(3);
        TestUtil.observeOnMainThread(actual, lifecycleOwner, observer);
        expected.add("Six");
        assertThat(observer.get(), is(expected));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
    public void withFtsTable() throws ExecutionException, InterruptedException, TimeoutException {
        final Context context = ApplicationProvider.getApplicationContext();
        final FtsTestDatabase db = Room.inMemoryDatabaseBuilder(context, FtsTestDatabase.class)
                .build();
        final MailDao mailDao = db.getMailDao();
        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();

        final TestObserver<List<Mail>> observer = new MyTestObserver<>();
        LiveData<List<Mail>> liveData = mailDao.getLiveDataMail();

        TestUtil.observeOnMainThread(liveData, lifecycleOwner, observer);
        assertThat(observer.get(), is(Collections.emptyList()));

        observer.reset();

        Mail mail = TestUtil.createMail(1, "subject", "body");
        mailDao.insert(mail);
        assertThat(observer.get().get(0), is(mail));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
    public void withExternalContentFtsTable()
            throws ExecutionException, InterruptedException, TimeoutException {
        final Context context = ApplicationProvider.getApplicationContext();
        final FtsTestDatabase db = Room.inMemoryDatabaseBuilder(context, FtsTestDatabase.class)
                .build();
        final SongDao songDao = db.getSongDao();
        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();

        final TestObserver<List<Song>> songObserver = new MyTestObserver<>();
        final TestObserver<List<SongDescription>> songDescriptionObserver = new MyTestObserver<>();
        LiveData<List<Song>> songData = songDao.getLiveDataSong();
        LiveData<List<SongDescription>> songDescriptionData = songDao.getLiveDataSongDescription();
        TestUtil.observeOnMainThread(songData, lifecycleOwner, songObserver);
        TestUtil.observeOnMainThread(songDescriptionData, lifecycleOwner, songDescriptionObserver);

        assertThat(songObserver.get(), is(Collections.emptyList()));
        assertThat(songDescriptionObserver.get(), is(Collections.emptyList()));

        songObserver.reset();
        songDescriptionObserver.reset();

        Song song1 = new Song(
                1,
                "Estamos Bien",
                "Bad Bunny",
                "X 100Pre",
                208,
                2018);

        songDao.insert(song1);

        assertThat(songObserver.get().get(0), is(song1));
        assertThat(songDescriptionObserver.get().size(), is(1));

        songObserver.reset();
        songDescriptionObserver.reset();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                songDescriptionData.removeObserver(songDescriptionObserver);
            }
        });

        Song song2 = new Song(
                2,
                "RLNDT",
                "Bad Bunny",
                "X 100Pre",
                284,
                2018);

        songDao.insert(song2);

        assertThat(songObserver.get().get(1), is(song2));
    }

    @MediumTest
    @Test
    public void handleGc() throws ExecutionException, InterruptedException, TimeoutException {
        LiveData<User> liveData = mUserDao.liveUserById(3);
        final TestObserver<User> observer = new MyTestObserver<>();
        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner(Lifecycle.State.STARTED);
        TestUtil.observeOnMainThread(liveData, lifecycleOwner, observer);
        assertThat(observer.get(), is(nullValue()));
        observer.reset();
        final User user = TestUtil.createUser(3);
        mUserDao.insert(user);
        assertThat(observer.get(), is(notNullValue()));
        observer.reset();
        TestUtil.forceGc();
        String name = UUID.randomUUID().toString();
        mUserDao.updateById(3, name);
        assertThat(observer.get().getName(), is(name));

        // release references
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        WeakReference<LiveData> weakLiveData = new WeakReference<LiveData>(liveData);
        //noinspection UnusedAssignment
        liveData = null;
        TestUtil.forceGc();
        mUserDao.updateById(3, "Bar");
        TestUtil.forceGc();
        assertThat(InvalidationTrackerTrojan.countObservers(mDatabase.getInvalidationTracker()),
                is(0));
        assertThat(weakLiveData.get(), nullValue());
    }

    @Test
    public void handleGcWithObserveForever() throws TimeoutException, InterruptedException {
        final AtomicReference<User> referenced = new AtomicReference<>();
        Observer<User> observer = referenced::set;
        AtomicReference<WeakReference<LiveData<User>>> liveDataReference = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> {
                    LiveData<User> userLiveData = mUserDao.liveUserById(3);
                    userLiveData.observeForever(observer);
                    liveDataReference.set(new WeakReference<>(userLiveData));
                });
        User v1 = TestUtil.createUser(3);
        mUserDao.insert(v1);
        drain();
        assertThat(referenced.get(), is(v1));
        TestUtil.forceGc();
        User v2 = TestUtil.createUser(3);
        v2.setName("handle gc");
        mUserDao.insertOrReplace(v2);
        drain();
        assertThat(referenced.get(), is(v2));
        assertThat(liveDataReference.get().get(), notNullValue());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> liveDataReference.get().get().removeObserver(observer));
        drain();
        TestUtil.forceGc();
        drain();
        User v3 = TestUtil.createUser(3);
        v3.setName("handle gc, get rid of LiveData");
        mUserDao.insertOrReplace(v3);
        drain();
        assertThat(referenced.get(), is(v2));
        assertThat(liveDataReference.get().get(), is(nullValue()));
    }

    @Test
    public void booleanLiveData() throws ExecutionException, InterruptedException,
            TimeoutException {
        User user = TestUtil.createUser(3);
        user.setAdmin(false);
        LiveData<Boolean> adminLiveData = mUserDao.isAdminLiveData(3);
        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        final TestObserver<Boolean> observer = new MyTestObserver<>();
        TestUtil.observeOnMainThread(adminLiveData, lifecycleOwner, observer);
        assertThat(observer.get(), is(nullValue()));
        mUserDao.insert(user);
        assertThat(observer.get(), is(false));
        user.setAdmin(true);
        mUserDao.insertOrReplace(user);
        assertThat(observer.get(), is(true));
    }

    @Test
    public void largeQueryLiveData() throws ExecutionException, InterruptedException,
            TimeoutException {
        int[] ids = new int[50000];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = i;
        }
        User[] users = TestUtil.createUsersArray(ids);
        mUserDao.insertAll(users);

        LiveData<List<User>> usersLiveData = mUserDao.liveUsersListByByIds(ids);
        final TestLifecycleOwner lifecycleOwner = new TestLifecycleOwner();
        final TestObserver<List<User>> observer = new MyTestObserver<>();
        TestUtil.observeOnMainThread(usersLiveData, lifecycleOwner, observer);
        assertThat(observer.get(), equalTo(users));
    }

    private void drain() throws TimeoutException, InterruptedException {
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
    }

    private class MyTestObserver<T> extends TestObserver<T> {

        @Override
        protected void drain() throws TimeoutException, InterruptedException {
            LiveDataQueryTest.this.drain();
        }
    }
}
