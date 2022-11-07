import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.RoomOpenHelper
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
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
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["unchecked", "deprecation"])
public class MyDatabase_Impl : MyDatabase() {
    private val _myDao: Lazy<MyDao> = lazy { MyDao_Impl(this) }

    protected override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
        val _openCallback: SupportSQLiteOpenHelper.Callback = RoomOpenHelper(config,
            MyDatabase_Impl_OpenHelperDelegate(this), "195d7974660177325bd1a32d2c7b8b8c",
            "7458a901120796c5bbc554e2fefd262f")
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

    public override fun clearAllTables(): Unit {
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
        val _autoMigrations: List<Migration> = ArrayList<Migration>()
        return _autoMigrations
    }

    public override fun getDao(): MyDao = _myDao.value
}