import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.RoomOpenHelper
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.FtsTableInfo
import androidx.room.util.TableInfo
import androidx.room.util.ViewInfo
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.lang.Class
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import javax.`annotation`.processing.Generated
import kotlin.Any
import kotlin.Boolean
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.Set
import androidx.room.util.FtsTableInfo.Companion.read as ftsTableInfoRead
import androidx.room.util.TableInfo.Companion.read as tableInfoRead
import androidx.room.util.ViewInfo.Companion.read as viewInfoRead

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
                db.execSQL("CREATE TABLE IF NOT EXISTS `MyParentEntity` (`parentKey` INTEGER NOT NULL, PRIMARY KEY(`parentKey`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `MyEntity` (`pk` INTEGER NOT NULL, `indexedCol` TEXT NOT NULL, PRIMARY KEY(`pk`), FOREIGN KEY(`indexedCol`) REFERENCES `MyParentEntity`(`parentKey`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_MyEntity_indexedCol` ON `MyEntity` (`indexedCol`)")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `MyFtsEntity` USING FTS4(`text` TEXT NOT NULL)")
                db.execSQL("CREATE VIEW `MyView` AS SELECT text FROM MyFtsEntity")
                db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '89ba16fb8b062b50acf0eb06c853efcb')")
            }

            public override fun dropAllTables(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `MyParentEntity`")
                db.execSQL("DROP TABLE IF EXISTS `MyEntity`")
                db.execSQL("DROP TABLE IF EXISTS `MyFtsEntity`")
                db.execSQL("DROP VIEW IF EXISTS `MyView`")
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
                db.execSQL("PRAGMA foreign_keys = ON")
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
                val _columnsMyParentEntity: HashMap<String, TableInfo.Column> =
                    HashMap<String, TableInfo.Column>(1)
                _columnsMyParentEntity.put("parentKey", TableInfo.Column("parentKey", "INTEGER", true, 1,
                    null, TableInfo.CREATED_FROM_ENTITY))
                val _foreignKeysMyParentEntity: HashSet<TableInfo.ForeignKey> =
                    HashSet<TableInfo.ForeignKey>(0)
                val _indicesMyParentEntity: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(0)
                val _infoMyParentEntity: TableInfo = TableInfo("MyParentEntity", _columnsMyParentEntity,
                    _foreignKeysMyParentEntity, _indicesMyParentEntity)
                val _existingMyParentEntity: TableInfo = tableInfoRead(db, "MyParentEntity")
                if (!_infoMyParentEntity.equals(_existingMyParentEntity)) {
                    return RoomOpenHelper.ValidationResult(false, """
              |MyParentEntity(MyParentEntity).
              | Expected:
              |""".trimMargin() + _infoMyParentEntity + """
              |
              | Found:
              |""".trimMargin() + _existingMyParentEntity)
                }
                val _columnsMyEntity: HashMap<String, TableInfo.Column> =
                    HashMap<String, TableInfo.Column>(2)
                _columnsMyEntity.put("pk", TableInfo.Column("pk", "INTEGER", true, 1, null,
                    TableInfo.CREATED_FROM_ENTITY))
                _columnsMyEntity.put("indexedCol", TableInfo.Column("indexedCol", "TEXT", true, 0, null,
                    TableInfo.CREATED_FROM_ENTITY))
                val _foreignKeysMyEntity: HashSet<TableInfo.ForeignKey> = HashSet<TableInfo.ForeignKey>(1)
                _foreignKeysMyEntity.add(TableInfo.ForeignKey("MyParentEntity", "CASCADE", "NO ACTION",
                    listOf("indexedCol"), listOf("parentKey")))
                val _indicesMyEntity: HashSet<TableInfo.Index> = HashSet<TableInfo.Index>(1)
                _indicesMyEntity.add(TableInfo.Index("index_MyEntity_indexedCol", false,
                    listOf("indexedCol"), listOf("ASC")))
                val _infoMyEntity: TableInfo = TableInfo("MyEntity", _columnsMyEntity, _foreignKeysMyEntity,
                    _indicesMyEntity)
                val _existingMyEntity: TableInfo = tableInfoRead(db, "MyEntity")
                if (!_infoMyEntity.equals(_existingMyEntity)) {
                    return RoomOpenHelper.ValidationResult(false, """
              |MyEntity(MyEntity).
              | Expected:
              |""".trimMargin() + _infoMyEntity + """
              |
              | Found:
              |""".trimMargin() + _existingMyEntity)
                }
                val _columnsMyFtsEntity: HashSet<String> = HashSet<String>(2)
                _columnsMyFtsEntity.add("text")
                val _infoMyFtsEntity: FtsTableInfo = FtsTableInfo("MyFtsEntity", _columnsMyFtsEntity,
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `MyFtsEntity` USING FTS4(`text` TEXT NOT NULL)")
                val _existingMyFtsEntity: FtsTableInfo = ftsTableInfoRead(db, "MyFtsEntity")
                if (!_infoMyFtsEntity.equals(_existingMyFtsEntity)) {
                    return RoomOpenHelper.ValidationResult(false, """
              |MyFtsEntity(MyFtsEntity).
              | Expected:
              |""".trimMargin() + _infoMyFtsEntity + """
              |
              | Found:
              |""".trimMargin() + _existingMyFtsEntity)
                }
                val _infoMyView: ViewInfo = ViewInfo("MyView",
                    "CREATE VIEW `MyView` AS SELECT text FROM MyFtsEntity")
                val _existingMyView: ViewInfo = viewInfoRead(db, "MyView")
                if (!_infoMyView.equals(_existingMyView)) {
                    return RoomOpenHelper.ValidationResult(false, """
              |MyView(MyView).
              | Expected:
              |""".trimMargin() + _infoMyView + """
              |
              | Found:
              |""".trimMargin() + _existingMyView)
                }
                return RoomOpenHelper.ValidationResult(true, null)
            }
        }, "89ba16fb8b062b50acf0eb06c853efcb", "8a71a68e07bdd62aa8c8324d870cf804")
        val _sqliteConfig: SupportSQLiteOpenHelper.Configuration =
            SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build()
        val _helper: SupportSQLiteOpenHelper = config.sqliteOpenHelperFactory.create(_sqliteConfig)
        return _helper
    }

    protected override fun createInvalidationTracker(): InvalidationTracker {
        val _shadowTablesMap: HashMap<String, String> = HashMap<String, String>(1)
        _shadowTablesMap.put("MyFtsEntity", "MyFtsEntity_content")
        val _viewTables: HashMap<String, Set<String>> = HashMap<String, Set<String>>(1)
        val _tables: HashSet<String> = HashSet<String>(1)
        _tables.add("MyFtsEntity")
        _viewTables.put("myview", _tables)
        return InvalidationTracker(this, _shadowTablesMap, _viewTables,
            "MyParentEntity","MyEntity","MyFtsEntity")
    }

    public override fun clearAllTables() {
        super.assertNotMainThread()
        val _db: SupportSQLiteDatabase = super.openHelper.writableDatabase
        val _supportsDeferForeignKeys: Boolean = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.LOLLIPOP
        try {
            if (!_supportsDeferForeignKeys) {
                _db.execSQL("PRAGMA foreign_keys = FALSE")
            }
            super.beginTransaction()
            if (_supportsDeferForeignKeys) {
                _db.execSQL("PRAGMA defer_foreign_keys = TRUE")
            }
            _db.execSQL("DELETE FROM `MyParentEntity`")
            _db.execSQL("DELETE FROM `MyEntity`")
            _db.execSQL("DELETE FROM `MyFtsEntity`")
            super.setTransactionSuccessful()
        } finally {
            super.endTransaction()
            if (!_supportsDeferForeignKeys) {
                _db.execSQL("PRAGMA foreign_keys = TRUE")
            }
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