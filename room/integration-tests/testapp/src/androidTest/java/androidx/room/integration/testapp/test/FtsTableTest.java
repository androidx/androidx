/*
 * Copyright 2018 The Android Open Source Project
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;

import androidx.room.Room;
import androidx.room.integration.testapp.FtsTestDatabase;
import androidx.room.integration.testapp.dao.MailDao;
import androidx.room.integration.testapp.dao.SongDao;
import androidx.room.integration.testapp.vo.Mail;
import androidx.room.integration.testapp.vo.Song;
import androidx.room.integration.testapp.vo.SongDescription;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FtsTableTest {

    private FtsTestDatabase mDatabase;
    private MailDao mMailDao;
    private SongDao mSongDao;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        mDatabase = Room.inMemoryDatabaseBuilder(context, FtsTestDatabase.class).build();
        mMailDao = mDatabase.getMailDao();
        mSongDao = mDatabase.getSongDao();
    }

    @Test
    public void readWrite() {
        Mail item = TestUtil.createMail(1,
                "Hello old friend",
                "How are you? Wanna grab coffee?");

        mMailDao.insert(item);

        List<Mail> loaded = mMailDao.getMail("coffee");
        assertThat(loaded.get(0), is(item));
    }

    @Test
    public void prefixQuery() {
        Mail item1 = TestUtil.createMail(1,
                "Linux problem",
                "Hi - Need help with my linux machine.");

        Mail item2 = TestUtil.createMail(2,
                "Math help needed",
                "Anyone able to help me with linear algebra?");

        Mail item3 = TestUtil.createMail(3,
                "Chef needed",
                "Need a cheeseburger check ASAP");

        mMailDao.insert(Lists.newArrayList(item1, item2, item3));

        List<Mail> loaded = mMailDao.getMail("lin*");
        assertThat(loaded.size(), is(2));
        assertThat(loaded.get(0), is(item1));
        assertThat(loaded.get(1), is(item2));
    }

    @Test
    public void prefixQuery_multiple() {
        Mail item1 = TestUtil.createMail(1,
                "Linux problem",
                "Hi - Need help with my linux machine.");

        Mail item2 = TestUtil.createMail(2,
                "Math help needed",
                "Anyone able to help me with linear algebra?");

        Mail item3 = TestUtil.createMail(3,
                "Chef needed",
                "Need a cheeseburger check ASAP");

        mMailDao.insert(Lists.newArrayList(item1, item2, item3));

        List<Mail> loaded = mMailDao.getMail("help linux");
        assertThat(loaded.size(), is(1));
        assertThat(loaded.get(0), is(item1));
    }

    @Test
    public void prefixQuery_multiple_OR() {
        Mail item1 = TestUtil.createMail(1,
                "Linux problem",
                "Hi - Need help with my linux machine.");

        Mail item2 = TestUtil.createMail(2,
                "Math help needed",
                "Anyone able to help me with linear algebra?");

        Mail item3 = TestUtil.createMail(3,
                "Chef needed",
                "Need a cheeseburger check ASAP");

        mMailDao.insert(Lists.newArrayList(item1, item2, item3));

        List<Mail> loaded = mMailDao.getMail("linux OR linear");
        assertThat(loaded.size(), is(2));
        assertThat(loaded.get(0), is(item1));
        assertThat(loaded.get(1), is(item2));
    }

    @Test
    public void prefixQuery_body() {
        Mail item1 = TestUtil.createMail(1,
                "Linux problem",
                "Hi - Need help with my linux machine.");

        Mail item2 = TestUtil.createMail(2,
                "Math help needed",
                "Anyone able to help me with linear algebra?");

        Mail item3 = TestUtil.createMail(3,
                "Chef needed",
                "Need a cheeseburger check ASAP");

        mMailDao.insert(Lists.newArrayList(item1, item2, item3));

        List<Mail> loaded = mMailDao.getMailWithBody("subject:help algebra");
        assertThat(loaded.size(), is(1));
        assertThat(loaded.get(0), is(item2));
    }

    @Test
    public void prefixQuery_startsWith() {
        Mail item = TestUtil.createMail(1,
                "Hello old friend",
                "How are you? Wanna grab coffee?");

        mMailDao.insert(item);

        List<Mail> loaded = mMailDao.getMailWithSubject("^hello");
        assertThat(loaded.get(0), is(item));
    }

    @Test
    public void phraseQuery() {
        Mail item1 = TestUtil.createMail(1,
                "Linux problem",
                "Hi - Need help with my linux machine.");
        Mail item2 = TestUtil.createMail(2,
                "Math help needed",
                "Anyone able to help me with linear algebra?");

        mMailDao.insert(Lists.newArrayList(item1, item2));

        List<Mail> loaded = mMailDao.getMail("\"help me\"");
        assertThat(loaded.size(), is(1));
        assertThat(loaded.get(0), is(item2));
    }

    @Test
    public void nearQuery() {
        Mail item = TestUtil.createMail(1,
                "Hello old friend",
                "How are you? Wanna grab coffee?");

        mMailDao.insert(item);

        List<Mail> loaded = mMailDao.getMail("coffee");
        assertThat(loaded.get(0), is(item));
    }

    @Test
    public void snippetQuery() {
        Mail item1 = TestUtil.createMail(1,
                "Linux problem",
                "Hi - Need help with my linux machine.");

        Mail item2 = TestUtil.createMail(2,
                "Math help needed",
                "Hello dear friends. I am in desperate need for some help. "
                        + "I've taken a lot of tutorials online but I still don't understand. "
                        + "Is anyone available to please help with some linear algebra?");
        mMailDao.insert(Lists.newArrayList(item1, item2));

        List<String> loaded = mMailDao.getMailBodySnippets("help");
        assertThat(loaded.size(), is(2));
        assertThat(loaded.get(0), is("Hi - Need <b>help</b> with my linux machine."));
        assertThat(loaded.get(1), is("<b>...</b>I am in desperate need for some <b>help</b>."
                + " I've taken a lot of tutorials<b>...</b>"));
    }

    @Test
    public void specialCommand_optimize() {
        Mail item = TestUtil.createMail(1,
                "Hello old friend",
                "How are you? Wanna grab coffee?");

        mMailDao.insert(item);
        mMailDao.optimizeMail();
    }

    @Test
    public void specialCommand_rebuild() {
        Mail item = TestUtil.createMail(1,
                "Hello old friend",
                "How are you? Wanna grab coffee?");

        mMailDao.insert(item);
        mMailDao.rebuildMail();
    }

    @Test
    public void externalContent() {
        Song item1 = new Song(
                1,
                "Solos (Remix)",
                "Plan B",
                "Solos",
                225,
                2009);

        Song item2 = new Song(
                2,
                "La Barría",
                "Wisin & Yandel",
                "Pa'l Mundo",
                177,
                2005);

        mSongDao.insert(Lists.newArrayList(item1, item2));

        List<SongDescription> descLoaded = mSongDao.getSongDescriptions("remix");
        assertThat(descLoaded.size(), is(1));
        assertThat(descLoaded.get(0).mTitle, is(item1.mTitle));

        List<Song> songLoaded = mSongDao.getSongs("remix");
        assertThat(songLoaded.size(), is(1));
        assertThat(songLoaded.get(0), is(item1));
    }
}
