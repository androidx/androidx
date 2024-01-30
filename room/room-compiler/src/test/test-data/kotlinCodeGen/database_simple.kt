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
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MyDatabase_Impl : MyDatabase() {
    private val _myDao: Lazy<MyDao> = lazy {
        MyDao_Impl(this)
    }

    protected override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
        val _openCallback: SupportSQLiteOpenHelper.Callback = RoomOpenHelper(config, object :
            RoomOpenHelper.Delegate(1) {
            public override fun createAllTables(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `MyEntity` (`pk` INTEGER NOT NULL, PRIMARY KEY(`pk`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '195d7974660177325bd1a32d2c7b8b8c')")
            }

            public override fun dropAllTables(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `MyEntity`")
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

            public override fun onPostMigrate(db: SupportSQLiteDatabase) {
            }

            public override fun onValidateSchema(db: SupportSQLiteDatabase):
                RoomOpenHelper.ValidationResult {
                val _columnsMyEntity: HashMap<String, TableInfo.Column> =
                    HashMap<String, TableInfo.Column>(1)
                _columnsMyEntity.put("pk", TableInfo.Column("pk", "INTEGER", true, 1, null,
                    TableInfo.CREATED_FROM_ENTITY))
                val _foreignKeysMyEntity: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(0)
                val _indicesMyEntity: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
                val _infoMyEntity: TableInfo = TableInfo("MyEntity", _columnsMyEntity, _foreignKeysMyEntity,
                    _indicesMyEntity)
                val _existingMyEntity: TableInfo = read(db, "MyEntity")
                if (!_infoMyEntity.equals(_existingMyEntity)) {
                    return RoomOpenHelper.ValidationResult(false, """
                  |MyEntity(MyEntity).
                  | Expected:
                  |""".trimMargin() + _infoMyEntity + """
                  |
                  | Found:
                  |""".trimMargin() + _existingMyEntity)
                }
                return RoomOpenHelper.ValidationResult(true, null)
            }
        }, "195d7974660177325bd1a32d2c7b8b8c", "7458a901120796c5bbc554e2fefd262f")
        val _sqliteConfig: SupportSQLiteOpenHelper.Configuration =
            SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build()
        val _helper: SupportSQLiteOpenHelper = config.sqliteOpenHelperFactory.create(_sqliteConfig)
        return _helper
    }

    protected override fun createInvalidationTracker(): InvalidationTracker {
        val _shadowTablesMap: HashMap<String, String> = HashMap<String, String>(0)
        val _viewTables: HashMap<String, Set<String>> = HashMap<String, Set<String>>(0)
        return InvalidationTracker(this, _shadowTablesMap, _viewTables, "MyEntity")
    }

    public override fun clearAllTables() {
        super.assertNotMainThread()
        val _db: SupportSQLiteDatabase = super.openHelper.writableDatabase
        try {
            super.beginTransaction()
            _db.execSQL("DELETE FROM `MyEntity`")
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
        _typeConvertersMap.put(MyDao::class.java, MyDao_Impl.getRequiredConverters())
        return _typeConvertersMap
    }

    public override fun getRequiredAutoMigrationSpecs(): Set<Class<out AutoMigrationSpec>> {
        val _autoMigrationSpecsSet: HashSet<Class<out AutoMigrationSpec>> =
            HashSet<Class<out AutoMigrationSpec>>()
        return _autoMigrationSpecsSet
    }

    public override
    fun getAutoMigrations(autoMigrationSpecs: Map<Class<out AutoMigrationSpec>, AutoMigrationSpec>):
        List<Migration> {
        val _autoMigrations: MutableList<Migration> = ArrayList<Migration>()
        return _autoMigrations
    }

    public override fun getDao(): MyDao = _myDao.value
}