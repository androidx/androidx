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
package androidx.room.testing

import android.annotation.SuppressLint
import android.app.Instrumentation
import android.content.Context
import android.util.Log
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.room.DatabaseConfiguration
import androidx.room.Room
import androidx.room.Room.getGeneratedImplementation
import androidx.room.RoomDatabase
import androidx.room.RoomOpenHelper
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.migration.bundle.DatabaseViewBundle
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.FieldBundle
import androidx.room.migration.bundle.ForeignKeyBundle
import androidx.room.migration.bundle.FtsEntityBundle
import androidx.room.migration.bundle.IndexBundle
import androidx.room.migration.bundle.SchemaBundle
import androidx.room.migration.bundle.SchemaBundle.Companion.deserialize
import androidx.room.util.FtsTableInfo
import androidx.room.util.TableInfo
import androidx.room.util.ViewInfo
import androidx.room.util.useCursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.ref.WeakReference
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A class that can be used in your Instrumentation tests that can create the database in an
 * older schema.
 *
 * You must copy the schema json files (created by passing `room.schemaLocation` argument
 * into the annotation processor) into your test assets and pass in the path for that folder into
 * the constructor. This class will read the folder and extract the schemas from there.
 *
 * ```
 * android {
 *     defaultConfig {
 *         javaCompileOptions {
 *             annotationProcessorOptions {
 *                 arguments = ["room.schemaLocation": "$projectDir/schemas".toString()]
 *             }
 *         }
 *     }
 *     sourceSets {
 *         androidTest.assets.srcDirs += files("$projectDir/schemas".toString())
 *     }
 * }
 * ```
 */
open class MigrationTestHelper : TestWatcher {
    private val assetsFolder: String
    private val openFactory: SupportSQLiteOpenHelper.Factory
    private val managedDatabases = mutableListOf<WeakReference<SupportSQLiteDatabase>>()
    private val managedRoomDatabases = mutableListOf<WeakReference<RoomDatabase>>()
    private var testStarted = false
    private val instrumentation: Instrumentation
    private val specs: List<AutoMigrationSpec>
    private val databaseClass: Class<out RoomDatabase>?
    internal lateinit var databaseConfiguration: DatabaseConfiguration

    /**
     * Creates a new migration helper. It uses the Instrumentation context to load the schema
     * (falls back to the app resources) and the target context to create the database.
     *
     * @param instrumentation The instrumentation instance.
     * @param assetsFolder    The asset folder in the assets directory.
     * @param openFactory factory for creating an [SupportSQLiteOpenHelper]
     */
    @Deprecated(
        """
            Cannot be used to run migration tests involving [AutoMigration].
            To test [AutoMigration], you must use [MigrationTestHelper(Instrumentation, Class, List,
            SupportSQLiteOpenHelper.Factory)] for tests containing a
            [androidx.room.ProvidedAutoMigrationSpec], or use
            [MigrationTestHelper(Instrumentation, Class, List)] otherwise.
      """
    )
    @JvmOverloads
    constructor(
        instrumentation: Instrumentation,
        assetsFolder: String,
        openFactory: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory()
    ) {
        this.instrumentation = instrumentation
        this.assetsFolder = assetsFolder
        this.openFactory = openFactory
        databaseClass = null
        specs = mutableListOf()
    }

    /**
     * Creates a new migration helper. It uses the Instrumentation context to load the schema
     * (falls back to the app resources) and the target context to create the database.
     *
     * An instance of a class annotated with [androidx.room.ProvidedAutoMigrationSpec] has
     * to be provided to Room using this constructor. MigrationTestHelper will map auto migration
     * spec classes to their provided instances before running and validating the Migrations.
     *
     * @param instrumentation The instrumentation instance.
     * @param databaseClass   The Database class to be tested.
     */
    constructor(
        instrumentation: Instrumentation,
        databaseClass: Class<out RoomDatabase>
    ) : this(
        instrumentation, databaseClass, emptyList(), FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Creates a new migration helper. It uses the Instrumentation context to load the schema
     * (falls back to the app resources) and the target context to create the database.
     *
     *
     * An instance of a class annotated with [androidx.room.ProvidedAutoMigrationSpec] has
     * to be provided to Room using this constructor. MigrationTestHelper will map auto migration
     * spec classes to their provided instances before running and validating the Migrations.
     *
     * @param instrumentation The instrumentation instance.
     * @param databaseClass   The Database class to be tested.
     * @param specs           The list of available auto migration specs that will be provided to
     * Room at runtime.
     * @param openFactory factory for creating an [SupportSQLiteOpenHelper]
     */
    @JvmOverloads
    constructor(
        instrumentation: Instrumentation,
        databaseClass: Class<out RoomDatabase>,
        specs: List<AutoMigrationSpec>,
        openFactory: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory()
    ) {
        this.assetsFolder = checkNotNull(databaseClass.canonicalName).let {
            if (it.endsWith("/")) {
                it.substring(0, databaseClass.canonicalName!!.length - 1)
            } else {
                it
            }
        }
        this.instrumentation = instrumentation
        this.openFactory = openFactory
        this.databaseClass = databaseClass
        this.specs = specs
    }

    override fun starting(description: Description?) {
        super.starting(description)
        testStarted = true
    }

    /**
     * Creates the database in the given version.
     * If the database file already exists, it tries to delete it first. If delete fails, throws
     * an exception.
     *
     * @param name    The name of the database.
     * @param version The version in which the database should be created.
     * @return A database connection which has the schema in the requested version.
     */
    @SuppressLint("RestrictedApi")
    @Throws(IOException::class)
    open fun createDatabase(name: String, version: Int): SupportSQLiteDatabase {
        val dbPath: File = instrumentation.targetContext.getDatabasePath(name)
        if (dbPath.exists()) {
            Log.d(TAG, "deleting database file $name")
            check(dbPath.delete()) {
                "There is a database file and I could not delete" +
                    " it. Make sure you don't have any open connections to that database" +
                    " before calling this method."
            }
        }
        val schemaBundle = loadSchema(version)
        val container: RoomDatabase.MigrationContainer = RoomDatabase.MigrationContainer()
        val configuration = DatabaseConfiguration(
            context = instrumentation.targetContext,
            name = name,
            sqliteOpenHelperFactory = openFactory,
            migrationContainer = container,
            callbacks = null,
            allowMainThreadQueries = true,
            journalMode = RoomDatabase.JournalMode.TRUNCATE,
            queryExecutor = ArchTaskExecutor.getIOThreadExecutor(),
            transactionExecutor = ArchTaskExecutor.getIOThreadExecutor(),
            multiInstanceInvalidationServiceIntent = null,
            requireMigration = true,
            allowDestructiveMigrationOnDowngrade = false,
            migrationNotRequiredFrom = emptySet(),
            copyFromAssetPath = null,
            copyFromFile = null,
            copyFromInputStream = null,
            prepackagedDatabaseCallback = null,
            typeConverters = emptyList(),
            autoMigrationSpecs = emptyList()
        )
        val roomOpenHelper = RoomOpenHelper(
            configuration = configuration,
            delegate = CreatingDelegate(schemaBundle.database),
            identityHash = schemaBundle.database.identityHash,
            // we pass the same hash twice since an old schema does not necessarily have
            // a legacy hash and we would not even persist it.
            legacyHash = schemaBundle.database.identityHash
        )
        return openDatabase(name, roomOpenHelper)
    }

    /**
     * Runs the given set of migrations on the provided database.
     *
     * It uses the same algorithm that Room uses to choose migrations so the migrations instances
     * that are provided to this method must be sufficient to bring the database from current
     * version to the desired version.
     *
     * After the migration, the method validates the database schema to ensure that migration
     * result matches the expected schema. Handling of dropped tables depends on the
     * `validateDroppedTables` argument. If set to true, the verification will fail if it
     * finds a table that is not registered in the Database. If set to false, extra tables in the
     * database will be ignored (this is the runtime library behavior).
     *
     * @param name                  The database name. You must first create this database via
     * [createDatabase].
     * @param version               The final version after applying the migrations.
     * @param validateDroppedTables If set to true, validation will fail if the database has
     * unknown tables.
     * @param migrations            The list of available migrations.
     * @throws IllegalArgumentException If the schema validation fails.
     */
    @SuppressLint("RestrictedApi")
    open fun runMigrationsAndValidate(
        name: String,
        version: Int,
        validateDroppedTables: Boolean,
        vararg migrations: Migration
    ): SupportSQLiteDatabase {
        val dbPath = instrumentation.targetContext.getDatabasePath(name)
        check(dbPath.exists()) {
            "Cannot find the database file for $name. " +
                "Before calling runMigrations, you must first create the database via " +
                "createDatabase."
        }
        val schemaBundle = loadSchema(version)
        val container = RoomDatabase.MigrationContainer()
        container.addMigrations(*migrations)
        val autoMigrations = getAutoMigrations(specs)
        autoMigrations.forEach { autoMigration ->
            val migrationExists = container.contains(
                autoMigration.startVersion,
                autoMigration.endVersion
            )
            if (!migrationExists) {
                container.addMigrations(autoMigration)
            }
        }
        databaseConfiguration = DatabaseConfiguration(
            context = instrumentation.targetContext,
            name = name,
            sqliteOpenHelperFactory = openFactory,
            migrationContainer = container,
            callbacks = null,
            allowMainThreadQueries = true,
            journalMode = RoomDatabase.JournalMode.TRUNCATE,
            queryExecutor = ArchTaskExecutor.getIOThreadExecutor(),
            transactionExecutor = ArchTaskExecutor.getIOThreadExecutor(),
            multiInstanceInvalidationServiceIntent = null,
            requireMigration = true,
            allowDestructiveMigrationOnDowngrade = false,
            migrationNotRequiredFrom = emptySet(),
            copyFromAssetPath = null,
            copyFromFile = null,
            copyFromInputStream = null,
            prepackagedDatabaseCallback = null,
            typeConverters = emptyList(),
            autoMigrationSpecs = emptyList()
        )
        val roomOpenHelper = RoomOpenHelper(
            configuration = databaseConfiguration,
            delegate = MigratingDelegate(
                databaseBundle = schemaBundle.database,
                // we pass the same hash twice since an old schema does not necessarily have
                // a legacy hash and we would not even persist it.
                mVerifyDroppedTables = validateDroppedTables
            ),
            identityHash = schemaBundle.database.identityHash,
            legacyHash = schemaBundle.database.identityHash
        )
        return openDatabase(name, roomOpenHelper)
    }

    /**
     * Returns a list of [Migration] of a database that has been generated using
     * [androidx.room.AutoMigration].
     */
    private fun getAutoMigrations(userProvidedSpecs: List<AutoMigrationSpec>): List<Migration> {
        if (databaseClass == null) {
            return if (userProvidedSpecs.isEmpty()) {
                // TODO: Detect that there are auto migrations to test when a deprecated
                //  constructor is used.
                Log.e(
                    TAG, "If you have any AutoMigrations in your implementation, you must use " +
                        "a non-deprecated MigrationTestHelper constructor to provide the " +
                        "Database class in order to test them. If you do not have any " +
                        "AutoMigrations to test, you may ignore this warning."
                )
                mutableListOf()
            } else {
                error(
                    "You must provide the database class in the " +
                        "MigrationTestHelper constructor in order to test auto migrations."
                )
            }
        }
        val db: RoomDatabase = getGeneratedImplementation(
            databaseClass, "_Impl"
        )
        val requiredAutoMigrationSpecs = db.getRequiredAutoMigrationSpecs()
        return db.getAutoMigrations(
            createAutoMigrationSpecMap(requiredAutoMigrationSpecs, userProvidedSpecs)
        )
    }

    /**
     * Maps auto migration spec classes to their provided instance.
     */
    private fun createAutoMigrationSpecMap(
        requiredAutoMigrationSpecs: Set<Class<out AutoMigrationSpec>>,
        userProvidedSpecs: List<AutoMigrationSpec>
    ): Map<Class<out AutoMigrationSpec>, AutoMigrationSpec> {
        if (requiredAutoMigrationSpecs.isEmpty()) {
            return emptyMap()
        }
        return buildMap {
            requiredAutoMigrationSpecs.forEach { spec ->
                val match = userProvidedSpecs.firstOrNull { provided ->
                    spec.isAssignableFrom(provided.javaClass)
                }
                require(match != null) {
                    "A required auto migration spec (${spec.canonicalName}) has not been provided."
                }
                put(spec, match)
            }
        }
    }

    private fun openDatabase(name: String, roomOpenHelper: RoomOpenHelper): SupportSQLiteDatabase {
        val config = SupportSQLiteOpenHelper.Configuration.builder(instrumentation.targetContext)
            .callback(roomOpenHelper)
            .name(name)
            .build()
        val db = openFactory.create(config).writableDatabase
        managedDatabases.add(WeakReference(db))
        return db
    }

    override fun finished(description: Description?) {
        super.finished(description)
        managedDatabases.forEach { dbRef ->
            val db = dbRef.get()
            if (db != null && db.isOpen) {
                try {
                    db.close()
                } catch (ignored: Throwable) {
                }
            }
        }
        managedRoomDatabases.forEach { dbRef ->
            val roomDatabase = dbRef.get()
            roomDatabase?.close()
        }
    }

    /**
     * Registers a database connection to be automatically closed when the test finishes.
     *
     * This only works if `MigrationTestHelper` is registered as a Junit test rule via
     * [Rule][org.junit.Rule] annotation.
     *
     * @param db The database connection that should be closed after the test finishes.
     */
    open fun closeWhenFinished(db: SupportSQLiteDatabase) {
        check(testStarted) {
            "You cannot register a database to be closed before" +
                " the test starts. Maybe you forgot to annotate MigrationTestHelper as a" +
                " test rule? (@Rule)"
        }
        managedDatabases.add(WeakReference(db))
    }

    /**
     * Registers a database connection to be automatically closed when the test finishes.
     *
     * This only works if `MigrationTestHelper` is registered as a Junit test rule via
     * [Rule][org.junit.Rule] annotation.
     *
     * @param db The RoomDatabase instance which holds the database.
     */
    open fun closeWhenFinished(db: RoomDatabase) {
        check(testStarted) {
            "You cannot register a database to be closed before" +
                " the test starts. Maybe you forgot to annotate MigrationTestHelper as a" +
                " test rule? (@Rule)"
        }
        managedRoomDatabases.add(WeakReference(db))
    }

    private fun loadSchema(version: Int): SchemaBundle {
        return try {
            loadSchema(instrumentation.context, version)
        } catch (testAssetsIOExceptions: FileNotFoundException) {
            Log.w(
                TAG, "Could not find the schema file in the test assets. Checking the" +
                    " application assets"
            )
            try {
                loadSchema(instrumentation.targetContext, version)
            } catch (appAssetsException: FileNotFoundException) {
                // throw the test assets exception instead
                throw FileNotFoundException(
                    "Cannot find the schema file in the assets folder. " +
                        "Make sure to include the exported json schemas in your test assert " +
                        "inputs. See " +
                        "https://developer.android.com/training/data-storage/room/" +
                        "migrating-db-versions#export-schema for details. Missing file: " +
                        testAssetsIOExceptions.message
                )
            }
        }
    }

    private fun loadSchema(context: Context, version: Int): SchemaBundle {
        val input = context.assets.open("$assetsFolder/$version.json")
        return deserialize(input)
    }

    internal class MigratingDelegate(
        databaseBundle: DatabaseBundle,
        private val mVerifyDroppedTables: Boolean
    ) : RoomOpenHelperDelegate(databaseBundle) {
        override fun createAllTables(db: SupportSQLiteDatabase) {
            throw UnsupportedOperationException(
                "Was expecting to migrate but received create." +
                    "Make sure you have created the database first."
            )
        }

        override fun onValidateSchema(
            db: SupportSQLiteDatabase
        ): RoomOpenHelper.ValidationResult {
            val tables = mDatabaseBundle.entitiesByTableName
            tables.values.forEach { entity ->
                if (entity is FtsEntityBundle) {
                    val expected = toFtsTableInfo(entity)
                    val found = FtsTableInfo.read(db, entity.tableName)
                    if (expected != found) {
                        return RoomOpenHelper.ValidationResult(
                            false,
                            """
                                ${expected.name}
                                Expected: $expected
                                Found: $found
                            """.trimIndent()
                        )
                    }
                } else {
                    val expected = toTableInfo(entity)
                    val found = TableInfo.read(db, entity.tableName)
                    if (expected != found) {
                        return RoomOpenHelper.ValidationResult(
                            false,
                            """
                                ${expected.name}
                                Expected: $expected
                                found: $found
                            """.trimIndent()
                        )
                    }
                }
            }
            mDatabaseBundle.views.forEach { view ->
                val expected = toViewInfo(view)
                val found = ViewInfo.read(db, view.viewName)
                if (expected != found) {
                    return RoomOpenHelper.ValidationResult(
                        false,
                            """
                                ${expected.name}
                                Expected: $expected
                                Found: $found
                            """.trimIndent()
                    )
                }
            }
            if (mVerifyDroppedTables) {
                // now ensure tables that should be removed are removed.
                val expectedTables = buildSet {
                    tables.values.forEach { entity ->
                        add(entity.tableName)
                        if (entity is FtsEntityBundle) {
                            addAll(entity.shadowTableNames)
                        }
                    }
                }
                db.query(
                    "SELECT name FROM sqlite_master WHERE type='table'" +
                        " AND name NOT IN(?, ?, ?)",
                    arrayOf(
                        Room.MASTER_TABLE_NAME, "android_metadata",
                        "sqlite_sequence"
                    )
                ).useCursor { cursor ->
                    while (cursor.moveToNext()) {
                        val tableName = cursor.getString(0)
                        if (!expectedTables.contains(tableName)) {
                            return RoomOpenHelper.ValidationResult(
                                false, "Unexpected table $tableName"
                            )
                        }
                    }
                }
            }
            return RoomOpenHelper.ValidationResult(true, null)
        }
    }

    internal class CreatingDelegate(
        databaseBundle: DatabaseBundle
    ) : RoomOpenHelperDelegate(databaseBundle) {
        override fun createAllTables(db: SupportSQLiteDatabase) {
            mDatabaseBundle.buildCreateQueries().forEach { query ->
                db.execSQL(query)
            }
        }

        override fun onValidateSchema(
            db: SupportSQLiteDatabase
        ): RoomOpenHelper.ValidationResult {
            throw UnsupportedOperationException(
                "This open helper just creates the database but it received a migration request."
            )
        }
    }

    internal abstract class RoomOpenHelperDelegate(
        val mDatabaseBundle: DatabaseBundle
    ) : RoomOpenHelper.Delegate(
            mDatabaseBundle.version
        ) {
        override fun dropAllTables(db: SupportSQLiteDatabase) {
            throw UnsupportedOperationException("cannot drop all tables in the test")
        }

        override fun onCreate(db: SupportSQLiteDatabase) {}
        override fun onOpen(db: SupportSQLiteDatabase) {}
    }

    internal companion object {
        private const val TAG = "MigrationTestHelper"
        @JvmStatic
        internal fun toTableInfo(entityBundle: EntityBundle): TableInfo {
            return TableInfo(
                name = entityBundle.tableName,
                columns = toColumnMap(entityBundle),
                foreignKeys = toForeignKeys(entityBundle.foreignKeys),
                indices = toIndices(entityBundle.indices)
            )
        }

        @JvmStatic
        internal fun toFtsTableInfo(ftsEntityBundle: FtsEntityBundle): FtsTableInfo {
            return FtsTableInfo(
                name = ftsEntityBundle.tableName,
                columns = toColumnNamesSet(ftsEntityBundle),
                createSql = ftsEntityBundle.createSql
            )
        }

        @JvmStatic
        internal fun toViewInfo(viewBundle: DatabaseViewBundle): ViewInfo {
            return ViewInfo(
                name = viewBundle.viewName,
                sql = viewBundle.createView()
            )
        }

        @JvmStatic
        internal fun toIndices(indices: List<IndexBundle>?): Set<TableInfo.Index> {
            if (indices == null) {
                return emptySet()
            }
            val result = indices.map { bundle ->
                TableInfo.Index(
                    name = bundle.name,
                    unique = bundle.isUnique,
                    columns = bundle.columnNames!!,
                    orders = bundle.orders!!
                )
            }.toSet()
            return result
        }

        @JvmStatic
        internal fun toForeignKeys(
            bundles: List<ForeignKeyBundle>?
        ): Set<TableInfo.ForeignKey> {
            if (bundles == null) {
                return emptySet()
            }
            val result = bundles.map { bundle ->
                TableInfo.ForeignKey(
                    referenceTable = bundle.table,
                    onDelete = bundle.onDelete,
                    onUpdate = bundle.onUpdate,
                    columnNames = bundle.columns,
                    referenceColumnNames = bundle.referencedColumns
                )
            }.toSet()
            return result
        }

        @JvmStatic
        internal fun toColumnNamesSet(entity: EntityBundle): Set<String> {
            val result = entity.fields.map { field ->
                field.columnName
            }.toSet()
            return result
        }

        @JvmStatic
        internal fun toColumnMap(entity: EntityBundle): Map<String, TableInfo.Column> {
            val result: MutableMap<String, TableInfo.Column> = HashMap()
            entity.fields.associateBy { bundle ->
                val column = toColumn(entity, bundle)
                result[column.name] = column
            }
            return result
        }

        @JvmStatic
        internal fun toColumn(entity: EntityBundle, field: FieldBundle): TableInfo.Column {
            return TableInfo.Column(
                name = field.columnName,
                type = field.affinity,
                notNull = field.isNonNull,
                primaryKeyPosition = findPrimaryKeyPosition(entity, field),
                defaultValue = field.defaultValue,
                createdFrom = TableInfo.CREATED_FROM_ENTITY
            )
        }

        @JvmStatic
        internal fun findPrimaryKeyPosition(entity: EntityBundle, field: FieldBundle): Int {
            return entity.primaryKey.columnNames.indexOfFirst { columnName ->
                field.columnName.equals(columnName, ignoreCase = true)
            } + 1 // Shift by 1 to get primary key position
        }
    }
}
