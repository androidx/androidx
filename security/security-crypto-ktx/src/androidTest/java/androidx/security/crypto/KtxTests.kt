/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.security.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore

private const val PREFS_FILE = "test_shared_prefs"

@MediumTest
@RunWith(JUnit4::class)
class KtxTests {

    @Before
    @Throws(Exception::class)
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Delete all previous keys and shared preferences.
        val parentDir = context.filesDir?.parent
            ?: throw IllegalStateException("filesDir?.parent is null?")
        var filePath = (parentDir + "/shared_prefs/" +
                "__androidx_security__crypto_encrypted_prefs__")
        var deletePrefFile = File(filePath)
        deletePrefFile.delete()
        val notEncryptedSharedPrefs = context.getSharedPreferences(
            PREFS_FILE,
            Context.MODE_PRIVATE
        )
        notEncryptedSharedPrefs.edit().clear().commit()
        filePath = ("$parentDir/shared_prefs/$PREFS_FILE")
        deletePrefFile = File(filePath)
        deletePrefFile.delete()
        val encryptedSharedPrefs = context.getSharedPreferences(
            "TinkTestPrefs",
            Context.MODE_PRIVATE
        )
        encryptedSharedPrefs.edit().clear().commit()
        filePath = ("$parentDir/shared_prefs/TinkTestPrefs")
        deletePrefFile = File(filePath)
        deletePrefFile.delete()
        // Delete MasterKeys
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.deleteEntry("_androidx_security_master_key_")
    }

    @Test
    fun testMasterKeyExtension() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val ktxMasterKey = MasterKey(
            context = context,
            authenticationRequired = false,
            userAuthenticationValidityDurationSeconds = 123
        )
        val jMasterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false, 123)
            .build()
        Assert.assertEquals(ktxMasterKey.keyAlias, jMasterKey.keyAlias)
        Assert.assertEquals(
            ktxMasterKey.isUserAuthenticationRequired,
            jMasterKey.isUserAuthenticationRequired
        )
        Assert.assertEquals(
            ktxMasterKey.userAuthenticationValidityDurationSeconds,
            jMasterKey.userAuthenticationValidityDurationSeconds
        )
    }

    @Test
    fun testEncryptedSharedPreferencesExtension() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val masterKey = MasterKey(context)
        val filename = "test"

        val ktxSharedPreferences = EncryptedSharedPreferences(
            context = context,
            fileName = filename,
            masterKey = masterKey
        )
        ktxSharedPreferences.edit().putString("test_key", "KTX Write").commit()

        val jSharedPreferences = EncryptedSharedPreferences.create(
            context,
            filename,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val readValue = jSharedPreferences.getString("test_key", "error")
        Assert.assertEquals(readValue, "KTX Write")
    }

    @Test
    fun testEncryptedFileExtension() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val masterKey = MasterKey(context)
        val file = File(context.cacheDir, "test.file")
        val testContent = "This is a test"

        if (file.exists()) {
            file.delete()
        }

        val ktFile = EncryptedFile(context, file, masterKey)
        ktFile.openFileOutput().use {
            it.write("This is a test".toByteArray(StandardCharsets.UTF_8))
        }

        val jFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val buffer = ByteArray(1024)
        jFile.openFileInput().use {
            val size = it.read(buffer)
            Assert.assertEquals(size, testContent.length)

            val contentBuffer = buffer.slice(IntRange(0, size - 1)).toByteArray()
            val content = String(contentBuffer, StandardCharsets.UTF_8)
            Assert.assertEquals(testContent, content)
        }

        if (!file.exists()) {
            Assert.fail("File didn't exist?")
        }
        file.delete()
    }
}
