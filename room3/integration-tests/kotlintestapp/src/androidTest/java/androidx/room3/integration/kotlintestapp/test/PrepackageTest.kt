/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.core.content.ContextCompat
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.integration.kotlintestapp.test.TestDatabaseTest.UseDriver
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipInputStream
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@MediumTest
@RunWith(Parameterized::class)
class PrepackageTest(private val useDriver: UseDriver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun createFromAsset() {
        context.deleteDatabase("products.db")
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products.db")
                .createFromAsset("databases/products_v1.db")
                .setDriver(getDriver())
                .build()
        val dao = database.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(2)
        database.close()
    }

    @Test
    fun createFromZippedAsset() {
        context.deleteDatabase("products.db")
        val inputStreamCallable =
            Callable<InputStream> {
                val zipInputStream =
                    ZipInputStream(context.assets.open("databases/products_v1.db.zip"))
                zipInputStream.nextEntry
                zipInputStream
            }
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products.db")
                .createFromInputStream(inputStreamCallable)
                .setDriver(getDriver())
                .build()
        val dao = database.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(2)
        database.close()
    }

    @Test
    fun createFromAsset_badSchema() {
        context.deleteDatabase("products_badSchema.db")
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products_badSchema.db")
                .createFromAsset("databases/products_badSchema.db")
                .setDriver(getDriver())
                .build()
        assertThrows<IllegalStateException> { database.getProductDao().countProducts() }
            .hasMessageThat()
            .contains("Pre-packaged database has an invalid schema")
        database.close()
    }

    @Test
    fun createFromAsset_notFound() {
        context.deleteDatabase("products_notFound.db")
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products_notFound.db")
                .createFromAsset("databases/products_notFound.db")
                .setDriver(getDriver())
                .build()
        assertThrows<FileNotFoundException> { database.getProductDao().countProducts() }
        database.close()
    }

    @Test
    fun createFromAsset_versionZero() {
        // A 0 version DB goes through the create path because SQLiteOpenHelper thinks the opened
        // DB was created from scratch. Therefore our onCreate callbacks will be called and we need
        // to validate the schema before completely opening the DB.
        context.deleteDatabase("products_v0.db")
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products_v0.db")
                .createFromAsset("databases/products_v0.db")
                .setDriver(getDriver())
                .build()
        val dao = database.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(2)
        database.close()
    }

    @Test
    fun createFromAsset_versionZero_badSchema() {
        context.deleteDatabase("products_v0_badSchema.db")
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products_v0_badSchema.db")
                .createFromAsset("databases/products_v0_badSchema.db")
                .setDriver(getDriver())
                .build()
        assertThrows<IllegalStateException> { database.getProductDao().countProducts() }
            .hasMessageThat()
            .contains("Pre-packaged database has an invalid schema")
        database.close()
    }

    @Test
    fun createFromAsset_closeAndReOpen() {
        context.deleteDatabase("products.db")
        Room.databaseBuilder<ProductsDatabase>(context, "products.db")
            .createFromAsset("databases/products_v1.db")
            .setDriver(getDriver())
            .build()
            .let { database ->
                val dao = database.getProductDao()
                assertThat(dao.countProducts()).isEqualTo(2)
                dao.insert("a new product")
                assertThat(dao.countProducts()).isEqualTo(3)
                database.close()
            }
        Room.databaseBuilder<ProductsDatabase>(context, "products.db")
            .createFromAsset("databases/products_v1.db")
            .setDriver(getDriver())
            .build()
            .let { database ->
                val dao = database.getProductDao()
                assertThat(dao.countProducts()).isEqualTo(3)
                database.close()
            }
    }

    @Test
    fun createFromAsset_badDatabaseFile() {
        // TODO(b/316944352): Implement retry mechanism for non-Android driver.
        if (useDriver != UseDriver.ANDROID) {
            throw AssumptionViolatedException("Test requires Android driver")
        }
        // A bad database file is a 'corrupted' database, it'll get deleted and a new file will be
        // created, the usual corrupted db recovery process in Android.
        context.deleteDatabase("products_badFile.db")
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products_badFile.db")
                .createFromAsset("databases/products_badFile.db")
                .setDriver(getDriver())
                .build()
        val dao = database.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(0)
        database.close()
    }

    @Test
    fun createFromAsset_upgrade() {
        context.deleteDatabase("products.db")
        val database =
            Room.databaseBuilder<ProductsDatabase_v2>(context, "products.db")
                .createFromAsset("databases/products_v1.db")
                .addMigrations(
                    object : Migration(1, 2) {
                        override suspend fun migrate(connection: SQLiteConnection) {
                            connection.execSQL(
                                "INSERT INTO Products (id, name) VALUES (null, 'Mofongo')"
                            )
                        }
                    }
                )
                .setDriver(getDriver())
                .build()
        val dao = database.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(3)
        assertThat(dao.getProductById(3).name).isEqualTo("Mofongo")
        database.close()
    }

    @Test
    fun createFromAsset_upgrade_destructiveMigration() {
        context.deleteDatabase("products.db")
        val database =
            Room.databaseBuilder<ProductsDatabase_v2>(context, "products.db")
                .createFromAsset("databases/products_v1.db")
                .fallbackToDestructiveMigration(false)
                .setDriver(getDriver())
                .build()
        val dao = database.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(0)
        database.close()
    }

    @Test
    fun createFromAsset_copyOnDestructiveMigration() {
        context.deleteDatabase("products.db")
        val dao: ProductDao
        val databaseV1 =
            Room.databaseBuilder<ProductsDatabase>(context, "products.db")
                .createFromAsset("databases/products_v1.db")
                .setDriver(getDriver())
                .build()
        dao = databaseV1.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(2)
        databaseV1.close()
        val databaseV2 =
            Room.databaseBuilder<ProductsDatabase_v2>(context, "products.db")
                .createFromAsset("databases/products_v2.db")
                .fallbackToDestructiveMigration(false)
                .setDriver(getDriver())
                .build()
        val dao2 = databaseV2.getProductDao()
        assertThat(dao2.countProducts()).isEqualTo(3)
        databaseV2.close()
    }

    @Test
    fun createFromAsset_copyOnDestructiveMigration_noRecursion() {
        context.deleteDatabase("products.db")
        val dao: ProductDao
        val databaseV1 =
            Room.databaseBuilder<ProductsDatabase>(context, "products.db")
                .createFromAsset("databases/products_v1.db")
                .setDriver(getDriver())
                .build()
        dao = databaseV1.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(2)
        databaseV1.close()
        val databaseV2 =
            Room.databaseBuilder<ProductsDatabase_v2>(context, "products.db")
                .createFromAsset("databases/products_v1.db")
                .fallbackToDestructiveMigration(false)
                .setDriver(getDriver())
                .build()
        val dao2 = databaseV2.getProductDao()
        assertThat(dao2.countProducts()).isEqualTo(0)
        databaseV2.close()
    }

    @Test
    fun createFromAsset_copyOnDestructiveMigration_migrationProvided() {
        context.deleteDatabase("products.db")
        val dao: ProductDao
        val databaseV1 =
            Room.databaseBuilder<ProductsDatabase>(context, "products.db")
                .createFromAsset("databases/products_v1.db")
                .setDriver(getDriver())
                .build()
        dao = databaseV1.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(2)
        databaseV1.close()
        val databaseV2 =
            Room.databaseBuilder<ProductsDatabase_v2>(context, "products.db")
                .createFromAsset("databases/products_v1.db")
                .addMigrations(
                    object : Migration(1, 2) {
                        override suspend fun migrate(connection: SQLiteConnection) {
                            connection.execSQL(
                                "INSERT INTO Products (id, name) VALUES (null, 'Mofongo')"
                            )
                        }
                    }
                )
                .fallbackToDestructiveMigration(false)
                .setDriver(getDriver())
                .build()
        val dao2 = databaseV2.getProductDao()
        assertThat(dao2.countProducts()).isEqualTo(3)
        assertThat(dao2.getProductById(3).name).isEqualTo("Mofongo")
        databaseV2.close()
    }

    @Test
    fun createFromFile() {
        context.deleteDatabase("products_external.db")
        val dataDbFile = File(ContextCompat.getDataDir(context), "products_external.db")
        context.deleteDatabase(dataDbFile.absolutePath)
        val toCopyInput = context.assets.open("databases/products_v1.db")
        copyAsset(toCopyInput, dataDbFile)
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products_external.db")
                .createFromFile(dataDbFile)
                .setDriver(getDriver())
                .build()
        val dao = database.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(2)
        database.close()
    }

    @Test
    fun createFromFile_copyOnDestructiveMigration_fileNotFound() {
        context.deleteDatabase("products_external.db")
        val dataDbFile = File(ContextCompat.getDataDir(context), "products_external.db")
        context.deleteDatabase(dataDbFile.absolutePath)
        val toCopyInput = context.assets.open("databases/products_v1.db")
        copyAsset(toCopyInput, dataDbFile)

        Room.databaseBuilder<ProductsDatabase>(context, "products_external.db")
            .createFromFile(dataDbFile)
            .setDriver(getDriver())
            .build()
            .let { databaseV1 ->
                val dao = databaseV1.getProductDao()
                assertThat(dao.countProducts()).isEqualTo(2)
                databaseV1.close()
            }

        context.deleteDatabase(dataDbFile.absolutePath)
        assertThat(dataDbFile.exists()).isFalse()

        Room.databaseBuilder<ProductsDatabase_v2>(context, "products_external.db")
            .createFromFile(dataDbFile)
            .fallbackToDestructiveMigration(false)
            .setDriver(getDriver())
            .build()
            .let { databaseV2 ->
                val dao2 = databaseV2.getProductDao()
                assertThat(dao2.countProducts()).isEqualTo(0)
                databaseV2.close()
            }
    }

    @Test
    fun createFromInputStream() {
        context.deleteDatabase("products_external.db")
        val dataDbFile = File(ContextCompat.getDataDir(context), "products_external.db.gz")
        context.deleteDatabase(dataDbFile.absolutePath)
        val toCopyInput = context.assets.open("databases/products_v1.db")

        // gzip the file while copying it - note that gzipping files in assets doesn't work because
        // aapt drops the gz extension and makes them available without requiring a GZip stream.
        val output: OutputStream = GZIPOutputStream(FileOutputStream(dataDbFile))
        copyStream(toCopyInput, output)
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products_external.db")
                .createFromInputStream { GZIPInputStream(FileInputStream(dataDbFile)) }
                .setDriver(getDriver())
                .build()
        val dao = database.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(2)
        database.close()
    }

    @Test
    fun openDataDirDatabase() {
        val dataDbFile = File(ContextCompat.getDataDir(context), "products.db")
        context.deleteDatabase(dataDbFile.absolutePath)
        val toCopyInput = context.assets.open("databases/products_v1.db")
        copyAsset(toCopyInput, dataDbFile)
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, dataDbFile.absolutePath)
                .setDriver(getDriver())
                .build()
        val dao = database.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(2)
        database.close()
    }

    @Test
    fun openDataDirDatabase_badSchema() {
        val dataDbFile = File(ContextCompat.getDataDir(context), "products.db")
        context.deleteDatabase(dataDbFile.absolutePath)
        val toCopyInput = context.assets.open("databases/products_badSchema.db")
        copyAsset(toCopyInput, dataDbFile)
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, dataDbFile.absolutePath)
                .setDriver(getDriver())
                .build()
        assertThrows<IllegalStateException> { database.getProductDao().countProducts() }
            .hasMessageThat()
            .contains("Pre-packaged database has an invalid schema")
        database.close()
    }

    @Test
    fun openDataDirDatabase_versionZero() {
        val dataDbFile = File(ContextCompat.getDataDir(context), "products.db")
        context.deleteDatabase(dataDbFile.absolutePath)
        val toCopyInput = context.assets.open("databases/products_v0.db")
        copyAsset(toCopyInput, dataDbFile)
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, dataDbFile.absolutePath)
                .setDriver(getDriver())
                .build()
        val dao = database.getProductDao()
        assertThat(dao.countProducts()).isEqualTo(2)
        database.close()
    }

    @Test
    fun openDataDirDatabase_versionZero_badSchema() {
        val dataDbFile = File(ContextCompat.getDataDir(context), "products.db")
        context.deleteDatabase(dataDbFile.absolutePath)
        val toCopyInput = context.assets.open("databases/products_v0_badSchema.db")
        copyAsset(toCopyInput, dataDbFile)
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, dataDbFile.absolutePath)
                .setDriver(getDriver())
                .build()
        assertThrows<IllegalStateException> { database.getProductDao().countProducts() }
            .hasMessageThat()
            .contains("Pre-packaged database has an invalid schema")
        database.close()
    }

    @Test
    fun onCreateFromAsset_calledOnOpenPrepackagedDatabase() {
        context.deleteDatabase("products.db")
        val openPrepackagedDatabaseCount = AtomicInteger()
        val callback: RoomDatabase.PrepackagedDatabaseCallback =
            object : RoomDatabase.PrepackagedDatabaseCallback() {
                override fun onOpenPrepackagedDatabase(connection: SQLiteConnection) {
                    connection.execSQL("INSERT INTO products (name) VALUES ('Mofongo')")
                    openPrepackagedDatabaseCount.getAndIncrement()
                }
            }
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products.db")
                .createFromAsset("databases/products_v1.db", callback)
                .setDriver(getDriver())
                .build()
        assertThat(openPrepackagedDatabaseCount.get()).isEqualTo(0)

        // Assert 3 products since pre-package had 2 and we inserted one in callback, this verifies
        // statements executed during callback are committed.
        assertThat(database.getProductDao().countProducts()).isEqualTo(3)
        assertThat(openPrepackagedDatabaseCount.get()).isEqualTo(1)
        database.close()
    }

    @Test
    fun onCreateFromFile_calledOnOpenPrepackagedDatabase() {
        context.deleteDatabase("products_external.db")
        val dataDbFile = File(ContextCompat.getDataDir(context), "products_external.db")
        context.deleteDatabase(dataDbFile.absolutePath)
        val toCopyInput = context.assets.open("databases/products_v1.db")
        copyAsset(toCopyInput, dataDbFile)
        val callback = TestPrepackagedDatabaseCallback()
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products_external.db")
                .createFromFile(dataDbFile, callback)
                .setDriver(getDriver())
                .build()
        assertThat(callback.openPrepackagedDatabaseCount).isEqualTo(0)
        database.getProductDao().countProducts()
        assertThat(callback.openPrepackagedDatabaseCount).isEqualTo(1)
        database.close()
    }

    @Test
    fun onCreateFromZippedAsset_calledOnOpenPrepackagedDatabase() {
        context.deleteDatabase("products.db")
        val inputStreamCallable =
            Callable<InputStream> {
                val zipInputStream =
                    ZipInputStream(context.assets.open("databases/products_v1.db.zip"))
                zipInputStream.nextEntry
                zipInputStream
            }
        val callback = TestPrepackagedDatabaseCallback()
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products.db")
                .createFromInputStream(inputStreamCallable, callback)
                .setDriver(getDriver())
                .build()
        assertThat(callback.openPrepackagedDatabaseCount).isEqualTo(0)
        database.getProductDao().countProducts()
        assertThat(callback.openPrepackagedDatabaseCount).isEqualTo(1)
        database.close()
    }

    @Test
    fun versionZero_calledOnOpenPrepackagedDatabase() {
        context.deleteDatabase("products.db")
        val callback = TestPrepackagedDatabaseCallback()
        val database =
            Room.databaseBuilder<ProductsDatabase>(context, "products.db")
                .createFromAsset("databases/products_v0.db", callback)
                .setDriver(getDriver())
                .build()
        assertThat(callback.openPrepackagedDatabaseCount).isEqualTo(0)
        database.getProductDao().countProducts()
        assertThat(callback.openPrepackagedDatabaseCount).isEqualTo(1)
        database.close()
    }

    @Test
    fun onOpenedDbTwice_calledOnPrepackagedCallbackOnce() {
        context.deleteDatabase("products.db")
        var db1: ProductsDatabase? = null
        var db2: ProductsDatabase? = null
        try {
            val callback = TestPrepackagedDatabaseCallback()
            db1 =
                Room.databaseBuilder<ProductsDatabase>(context, "products.db")
                    .createFromAsset("databases/products_v1.db", callback)
                    .setDriver(getDriver())
                    .build()
            assertThat(callback.openPrepackagedDatabaseCount).isEqualTo(0)
            db1.getProductDao().countProducts()
            assertThat(callback.openPrepackagedDatabaseCount).isEqualTo(1)
            db2 =
                Room.databaseBuilder<ProductsDatabase>(context, "products.db")
                    .createFromAsset("databases/products_v1.db", callback)
                    .setDriver(getDriver())
                    .build()
            assertThat(callback.openPrepackagedDatabaseCount).isEqualTo(1)
            db2.getProductDao().countProducts()
            // Not called this time; db file was already copied and callback was called by db1
            assertThat(callback.openPrepackagedDatabaseCount).isEqualTo(1)
        } finally {
            db1?.close()
            db2?.close()
        }
    }

    @Test
    fun onPrepackagedCallbackException_calledOnPrepackagedCallbackWhenOpenedAgain() {
        context.deleteDatabase("products.db")
        val throwingCallback: TestPrepackagedDatabaseCallback =
            object : TestPrepackagedDatabaseCallback() {
                override fun onOpenPrepackagedDatabase(connection: SQLiteConnection) {
                    throw RuntimeException("Something went wrong!")
                }
            }
        val callback = TestPrepackagedDatabaseCallback()
        val db1 =
            Room.databaseBuilder<ProductsDatabase>(context, "products.db")
                .createFromAsset("databases/products_v1.db", throwingCallback)
                .setDriver(getDriver())
                .build()
        assertThrows<RuntimeException> { db1.getProductDao().countProducts() }
        db1.close()

        assertThat(throwingCallback.openPrepackagedDatabaseCount).isEqualTo(0)
        val db2 =
            Room.databaseBuilder<ProductsDatabase>(context, "products.db")
                .createFromAsset("databases/products_v1.db", callback)
                .setDriver(getDriver())
                .build()
        assertThat(callback.openPrepackagedDatabaseCount).isEqualTo(0)
        db2.getProductDao().countProducts()
        assertThat(callback.openPrepackagedDatabaseCount).isEqualTo(1)
        db2.close()
    }

    @Entity(tableName = "products")
    data class Product(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

    @Dao
    interface ProductDao {
        @Query("SELECT COUNT(*) FROM products") fun countProducts(): Int

        @Query("INSERT INTO products (name) VALUES (:name)") fun insert(name: String)

        @Query("SELECT * FROM products WHERE id = :id") fun getProductById(id: Long): Product
    }

    @Database(entities = [Product::class], version = 1, exportSchema = false)
    internal abstract class ProductsDatabase : RoomDatabase() {
        abstract fun getProductDao(): ProductDao
    }

    @Database(entities = [Product::class], version = 2, exportSchema = false)
    internal abstract class ProductsDatabase_v2 : RoomDatabase() {
        abstract fun getProductDao(): ProductDao
    }

    private fun getDriver(): SQLiteDriver {
        return when (useDriver) {
            UseDriver.ANDROID -> AndroidSQLiteDriver()
            UseDriver.BUNDLED -> BundledSQLiteDriver()
        }
    }

    private fun copyAsset(input: InputStream, outputFile: File) {
        val output: OutputStream = FileOutputStream(outputFile)
        copyStream(input, output)
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        try {
            var length: Int
            val buffer = ByteArray(1024 * 4)
            while (input.read(buffer).also { length = it } > 0) {
                output.write(buffer, 0, length)
            }
        } finally {
            input.close()
            output.close()
        }
    }

    open class TestPrepackagedDatabaseCallback : RoomDatabase.PrepackagedDatabaseCallback() {
        var openPrepackagedDatabaseCount = 0

        override fun onOpenPrepackagedDatabase(connection: SQLiteConnection) {
            openPrepackagedDatabaseCount++
        }
    }
}
