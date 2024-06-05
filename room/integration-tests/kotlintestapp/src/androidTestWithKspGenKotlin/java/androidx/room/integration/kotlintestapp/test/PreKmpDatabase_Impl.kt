@file:Suppress(
    "UNCHECKED_CAST",
    "UNSAFE_CALL",
    "DEPRECATION",
    "DEPRECATION_ERROR",
    "OVERRIDE_DEPRECATION",
    "REDUNDANT_PROJECTION",
    "RestrictedApiAndroidX"
) // Generated code

package androidx.room.integration.kotlintestapp.test

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomOpenHelper
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.lang.Class
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import javax.`annotation`.processing.Generated
import kotlin.Any
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.Set

@Generated(value = ["androidx.room.RoomProcessor"])
public class PreKmpDatabase_Impl : PreKmpDatabase() {
    private val _preKmpDatabase: Lazy<PreKmpDatabase.TheDao> = lazy {
        PreKmpDatabase_TheDao_Impl(this)
    }

    protected override fun createOpenHelper(
        config: DatabaseConfiguration
    ): SupportSQLiteOpenHelper {
        val _openCallback: SupportSQLiteOpenHelper.Callback =
            RoomOpenHelper(
                config,
                object : RoomOpenHelper.Delegate(2) {
                    public override fun createAllTables(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `TheEntity` (`id` INTEGER NOT NULL, `text` TEXT NOT NULL, `custom` BLOB NOT NULL, PRIMARY KEY(`id`))"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
                        )
                        db.execSQL(
                            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fbcbaa42ae194c6600f45bb10ebfcc1f')"
                        )
                    }

                    public override fun dropAllTables(db: SupportSQLiteDatabase) {
                        db.execSQL("DROP TABLE IF EXISTS `TheEntity`")
                        val _callbacks: List<RoomDatabase.Callback>? = mCallbacks
                        if (_callbacks != null) {
                            for (_callback: RoomDatabase.Callback in _callbacks) {
                                _callback.onDestructiveMigration(db)
                            }
                        }
                    }

                    public override fun onCreate(db: SupportSQLiteDatabase) {
                        val _callbacks: List<RoomDatabase.Callback>? = mCallbacks
                        if (_callbacks != null) {
                            for (_callback: RoomDatabase.Callback in _callbacks) {
                                _callback.onCreate(db)
                            }
                        }
                    }

                    public override fun onOpen(db: SupportSQLiteDatabase) {
                        mDatabase = db
                        internalInitInvalidationTracker(db)
                        val _callbacks: List<RoomDatabase.Callback>? = mCallbacks
                        if (_callbacks != null) {
                            for (_callback: RoomDatabase.Callback in _callbacks) {
                                _callback.onOpen(db)
                            }
                        }
                    }

                    public override fun onPreMigrate(db: SupportSQLiteDatabase) {
                        dropFtsSyncTriggers(db)
                    }

                    public override fun onPostMigrate(db: SupportSQLiteDatabase) {}

                    public override fun onValidateSchema(
                        db: SupportSQLiteDatabase
                    ): RoomOpenHelper.ValidationResult {
                        val _columnsTheEntity: HashMap<String, TableInfo.Column> =
                            HashMap<String, TableInfo.Column>(3)
                        _columnsTheEntity.put(
                            "id",
                            TableInfo.Column(
                                "id",
                                "INTEGER",
                                true,
                                1,
                                null,
                                TableInfo.CREATED_FROM_ENTITY
                            )
                        )
                        _columnsTheEntity.put(
                            "text",
                            TableInfo.Column(
                                "text",
                                "TEXT",
                                true,
                                0,
                                null,
                                TableInfo.CREATED_FROM_ENTITY
                            )
                        )
                        _columnsTheEntity.put(
                            "custom",
                            TableInfo.Column(
                                "custom",
                                "BLOB",
                                true,
                                0,
                                null,
                                TableInfo.CREATED_FROM_ENTITY
                            )
                        )
                        val _foreignKeysTheEntity: HashSet<TableInfo.ForeignKey> =
                            HashSet<TableInfo.ForeignKey>(0)
                        val _indicesTheEntity: HashSet<TableInfo.Index> =
                            HashSet<TableInfo.Index>(0)
                        val _infoTheEntity: TableInfo =
                            TableInfo(
                                "TheEntity",
                                _columnsTheEntity,
                                _foreignKeysTheEntity,
                                _indicesTheEntity
                            )
                        val _existingTheEntity: TableInfo = read(db, "TheEntity")
                        if (!_infoTheEntity.equals(_existingTheEntity)) {
                            return RoomOpenHelper.ValidationResult(
                                false,
                                """
              |TheEntity(androidx.room.integration.kotlintestapp.test.PreKmpDatabase.TheEntity).
              | Expected:
              |"""
                                    .trimMargin() +
                                    _infoTheEntity +
                                    """
              |
              | Found:
              |"""
                                        .trimMargin() +
                                    _existingTheEntity
                            )
                        }
                        return RoomOpenHelper.ValidationResult(true, null)
                    }
                },
                "fbcbaa42ae194c6600f45bb10ebfcc1f",
                "c5a810f0a0f12e6d217d248fa35d0d13"
            )
        val _sqliteConfig: SupportSQLiteOpenHelper.Configuration =
            SupportSQLiteOpenHelper.Configuration.builder(config.context)
                .name(config.name)
                .callback(_openCallback)
                .build()
        val _helper: SupportSQLiteOpenHelper = config.sqliteOpenHelperFactory.create(_sqliteConfig)
        return _helper
    }

    protected override fun createInvalidationTracker(): InvalidationTracker {
        val _shadowTablesMap: HashMap<String, String> = HashMap<String, String>(0)
        val _viewTables: HashMap<String, Set<String>> = HashMap<String, Set<String>>(0)
        return InvalidationTracker(this, _shadowTablesMap, _viewTables, "TheEntity")
    }

    public override fun clearAllTables() {
        super.assertNotMainThread()
        val _db: SupportSQLiteDatabase = super.openHelper.writableDatabase
        try {
            super.beginTransaction()
            _db.execSQL("DELETE FROM `TheEntity`")
            super.setTransactionSuccessful()
        } finally {
            super.endTransaction()
            _db.query("PRAGMA wal_checkpoint(FULL)").close()
            if (!_db.inTransaction()) {
                _db.execSQL("VACUUM")
            }
        }
    }

    protected override fun getRequiredTypeConverters(): Map<Class<out Any>, List<Class<out Any>>> {
        val _typeConvertersMap: HashMap<Class<out Any>, List<Class<out Any>>> =
            HashMap<Class<out Any>, List<Class<out Any>>>()
        _typeConvertersMap.put(
            PreKmpDatabase.TheDao::class.java,
            PreKmpDatabase_TheDao_Impl.getRequiredConverters()
        )
        return _typeConvertersMap
    }

    public override fun getRequiredAutoMigrationSpecs(): Set<Class<out AutoMigrationSpec>> {
        val _autoMigrationSpecsSet: HashSet<Class<out AutoMigrationSpec>> =
            HashSet<Class<out AutoMigrationSpec>>()
        _autoMigrationSpecsSet.add(PreKmpDatabase.MigrationSpec1To2::class.java)
        return _autoMigrationSpecsSet
    }

    public override fun getAutoMigrations(
        autoMigrationSpecs: Map<Class<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration> {
        val _autoMigrations: MutableList<Migration> = ArrayList<Migration>()
        _autoMigrations.add(
            PreKmpDatabase_AutoMigration_1_2_Impl(
                autoMigrationSpecs.getValue(PreKmpDatabase.MigrationSpec1To2::class.java)
            )
        )
        return _autoMigrations
    }

    public override fun getTheDao(): PreKmpDatabase.TheDao = _preKmpDatabase.value
}
