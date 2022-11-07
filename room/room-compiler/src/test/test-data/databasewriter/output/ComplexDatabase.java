package foo.bar;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ComplexDatabase_Impl extends ComplexDatabase {
    private volatile ComplexDao _complexDao;

    @Override
    @NonNull
    protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
        final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new ComplexDatabase_Impl_OpenHelperDelegate(this), "12b646c55443feeefb567521e2bece85", "2f1dbf49584f5d6c91cb44f8a6ecfee2");
        final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
        final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
        return _helper;
    }

    @Override
    @NonNull
    protected InvalidationTracker createInvalidationTracker() {
        final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
        final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(1);
        final HashSet<String> _tables = new HashSet<String>(1);
        _tables.add("User");
        _viewTables.put("usersummary", _tables);
        return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "User","Child1","Child2");
    }

    @Override
    public void clearAllTables() {
        super.assertNotMainThread();
        final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
        try {
            super.beginTransaction();
            _db.execSQL("DELETE FROM `User`");
            _db.execSQL("DELETE FROM `Child1`");
            _db.execSQL("DELETE FROM `Child2`");
            super.setTransactionSuccessful();
        } finally {
            super.endTransaction();
            _db.query("PRAGMA wal_checkpoint(FULL)").close();
            if (!_db.inTransaction()) {
                _db.execSQL("VACUUM");
            }
        }
    }

    @Override
    @NonNull
    protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
        final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
        _typeConvertersMap.put(ComplexDao.class, ComplexDao_Impl.getRequiredConverters());
        return _typeConvertersMap;
    }

    @Override
    @NonNull
    public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
        final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
        return _autoMigrationSpecsSet;
    }

    @Override
    @NonNull
    public List<Migration> getAutoMigrations(
            @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
        final List<Migration> _autoMigrations = new ArrayList<Migration>();
        return _autoMigrations;
    }

    @Override
    ComplexDao getComplexDao() {
        if (_complexDao != null) {
            return _complexDao;
        } else {
            synchronized(this) {
                if(_complexDao == null) {
                    _complexDao = new ComplexDao_Impl(this);
                }
                return _complexDao;
            }
        }
    }
}