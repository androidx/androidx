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
package androidx.room3.integration.kotlintestapp.test

import android.content.Context
import androidx.kruth.assertThat
import androidx.room3.Room
import androidx.room3.integration.kotlintestapp.FtsTestDatabase
import androidx.room3.integration.kotlintestapp.dao.MailDao
import androidx.room3.integration.kotlintestapp.dao.SongDao
import androidx.room3.integration.kotlintestapp.test.TestDatabaseTest.UseDriver
import androidx.room3.integration.kotlintestapp.vo.Song
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@SmallTest
@RunWith(Parameterized::class)
class FtsTableTest(private val useDriver: UseDriver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    private lateinit var database: FtsTestDatabase
    private lateinit var mailDao: MailDao
    private lateinit var songDao: SongDao

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database =
            Room.inMemoryDatabaseBuilder<FtsTestDatabase>(context)
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .build()
        mailDao = database.getMailDao()
        songDao = database.getSongDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun readWrite() {
        val item = TestUtil.createMail(1, "Hello old friend", "How are you? Wanna grab coffee?")
        mailDao.insert(item)
        val loaded = mailDao.getMail("coffee")
        assertThat(loaded[0]).isEqualTo(item)
    }

    @Test
    fun prefixQuery() {
        val item1 = TestUtil.createMail(1, "Linux problem", "Hi - Need help with my linux machine.")
        val item2 =
            TestUtil.createMail(
                2,
                "Math help needed",
                "Anyone able to help me with linear algebra?",
            )
        val item3 = TestUtil.createMail(3, "Chef needed", "Need a cheeseburger check ASAP")
        mailDao.insert(listOf(item1, item2, item3))
        val loaded = mailDao.getMail("lin*")
        assertThat(loaded).hasSize(2)
        assertThat(loaded[0]).isEqualTo(item1)
        assertThat(loaded[1]).isEqualTo(item2)
    }

    @Test
    fun prefixQuery_multiple() {
        val item1 = TestUtil.createMail(1, "Linux problem", "Hi - Need help with my linux machine.")
        val item2 =
            TestUtil.createMail(
                2,
                "Math help needed",
                "Anyone able to help me with linear algebra?",
            )
        val item3 = TestUtil.createMail(3, "Chef needed", "Need a cheeseburger check ASAP")
        mailDao.insert(listOf(item1, item2, item3))
        val loaded = mailDao.getMail("help linux")
        assertThat(loaded).hasSize(1)
        assertThat(loaded[0]).isEqualTo(item1)
    }

    @Test
    fun prefixQuery_multiple_OR() {
        val item1 = TestUtil.createMail(1, "Linux problem", "Hi - Need help with my linux machine.")
        val item2 =
            TestUtil.createMail(
                2,
                "Math help needed",
                "Anyone able to help me with linear algebra?",
            )
        val item3 = TestUtil.createMail(3, "Chef needed", "Need a cheeseburger check ASAP")
        mailDao.insert(listOf(item1, item2, item3))
        val loaded = mailDao.getMail("linux OR linear")
        assertThat(loaded).hasSize(2)
        assertThat(loaded[0]).isEqualTo(item1)
        assertThat(loaded[1]).isEqualTo(item2)
    }

    @Test
    fun prefixQuery_body() {
        val item1 = TestUtil.createMail(1, "Linux problem", "Hi - Need help with my linux machine.")
        val item2 =
            TestUtil.createMail(
                2,
                "Math help needed",
                "Anyone able to help me with linear algebra?",
            )
        val item3 = TestUtil.createMail(3, "Chef needed", "Need a cheeseburger check ASAP")
        mailDao.insert(listOf(item1, item2, item3))
        val loaded = mailDao.getMailWithBody("subject:help algebra")
        assertThat(loaded).hasSize(1)
        assertThat(loaded[0]).isEqualTo(item2)
    }

    @Test
    fun prefixQuery_startsWith() {
        val item = TestUtil.createMail(1, "Hello old friend", "How are you? Wanna grab coffee?")
        mailDao.insert(item)
        val loaded = mailDao.getMailWithSubject("^hello")
        assertThat(loaded[0]).isEqualTo(item)
    }

    @Test
    fun phraseQuery() {
        val item1 = TestUtil.createMail(1, "Linux problem", "Hi - Need help with my linux machine.")
        val item2 =
            TestUtil.createMail(
                2,
                "Math help needed",
                "Anyone able to help me with linear algebra?",
            )
        mailDao.insert(listOf(item1, item2))
        val loaded = mailDao.getMail("\"help me\"")
        assertThat(loaded).hasSize(1)
        assertThat(loaded[0]).isEqualTo(item2)
    }

    @Test
    fun nearQuery() {
        val item = TestUtil.createMail(1, "Hello old friend", "How are you? Wanna grab coffee?")
        mailDao.insert(item)
        val loaded = mailDao.getMail("coffee")
        assertThat(loaded[0]).isEqualTo(item)
    }

    @Test
    fun snippetQuery() {
        val item1 = TestUtil.createMail(1, "Linux problem", "Hi - Need help with my linux machine.")
        val item2 =
            TestUtil.createMail(
                2,
                "Math help needed",
                "Hello dear friends. I am in desperate need for some help. " +
                    "I've taken a lot of tutorials online but I still don't understand. " +
                    "Is anyone available to please help with some linear algebra?",
            )
        mailDao.insert(listOf(item1, item2))
        val loaded = mailDao.getMailBodySnippets("help")
        assertThat(loaded).hasSize(2)
        assertThat(loaded[0]).isEqualTo("Hi - Need <b>help</b> with my linux machine.")
        assertThat(loaded[1])
            .isEqualTo(
                "<b>...</b>I am in desperate need for some <b>help</b>." +
                    " I've taken a lot of tutorials<b>...</b>"
            )
    }

    @Test
    fun specialCommand_optimize() {
        val item = TestUtil.createMail(1, "Hello old friend", "How are you? Wanna grab coffee?")
        mailDao.insert(item)
        mailDao.optimizeMail()
    }

    @Test
    fun specialCommand_rebuild() {
        val item = TestUtil.createMail(1, "Hello old friend", "How are you? Wanna grab coffee?")
        mailDao.insert(item)
        mailDao.rebuildMail()
    }

    @Test
    fun externalContent() {
        val item1 = Song(1, "Solos (Remix)", "Plan B", "Solos", 225, 2009)
        val item2 = Song(2, "La Barría", "Wisin & Yandel", "Pa'l Mundo", 177, 2005)
        songDao.insert(listOf(item1, item2))
        val descLoaded = songDao.getSongDescriptions("remix")
        assertThat(descLoaded).hasSize(1)
        assertThat(descLoaded[0].mTitle).isEqualTo(item1.mTitle)
        val songLoaded = songDao.getSongs("remix")
        assertThat(songLoaded).hasSize(1)
        assertThat(songLoaded[0]).isEqualTo(item1)
    }

    @Test
    fun flow() = runTest {
        val mail = mailDao.getFlowDataMail().produceIn(this)
        assertThat(mail.receive()).isEmpty()

        val item = TestUtil.createMail(1, "Hello old friend", "How are you? Wanna grab coffee?")
        mailDao.insert(item)
        assertThat(mail.receive()).containsExactly(item)

        mail.cancel()
    }

    @Test
    fun flow_externalContent() = runTest {
        val songs = songDao.getFlowSong().produceIn(this)
        val songDescriptions = songDao.getFlowSongDescription().produceIn(this)

        assertThat(songs.receive()).isEmpty()
        assertThat(songDescriptions.receive()).isEmpty()

        val song1 = Song(1, "Estamos Bien", "Bad Bunny", "X 100Pre", 208, 2018)
        songDao.insert(song1)

        assertThat(songs.receive()).containsExactly(song1)
        assertThat(songDescriptions.receive().map { it.mTitle }).containsExactly(song1.mTitle)

        val song2 = Song(2, "RLNDT", "Bad Bunny", "X 100Pre", 284, 2018)
        songDao.insert(song2)

        assertThat(songs.receive()).containsExactly(song1, song2)
        assertThat(songDescriptions.receive().map { it.mTitle })
            .containsExactly(song1.mTitle, song2.mTitle)

        songs.cancel()
        songDescriptions.cancel()
    }
}
