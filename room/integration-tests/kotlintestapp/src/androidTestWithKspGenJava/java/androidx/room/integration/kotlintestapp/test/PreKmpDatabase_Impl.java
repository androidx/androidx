package androidx.room.integration.kotlintestapp.test;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
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
@SuppressWarnings({"unchecked", "deprecation", "RestrictedApiAndroidX", "UnknownNullness"})
public final class PreKmpDatabase_Impl extends PreKmpDatabase {
    private volatile PreKmpDatabase.TheDao _preKmpDatabase;

    @Override
    @NonNull
    protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
        final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
            @Override
            public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `TheEntity` (`id` INTEGER NOT NULL, `text` TEXT NOT NULL, `custom` BLOB NOT NULL, PRIMARY KEY(`id`))");
                db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
                db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fbcbaa42ae194c6600f45bb10ebfcc1f')");
            }

            @Override
            public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
                db.execSQL("DROP TABLE IF EXISTS `TheEntity`");
                final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
                if (_callbacks != null) {
                    for (RoomDatabase.Callback _callback : _callbacks) {
                        _callback.onDestructiveMigration(db);
                    }
                }
            }

            @Override
            public void onCreate(@NonNull final SupportSQLiteDatabase db) {
                final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
                if (_callbacks != null) {
                    for (RoomDatabase.Callback _callback : _callbacks) {
                        _callback.onCreate(db);
                    }
                }
            }

            @Override
            public void onOpen(@NonNull final SupportSQLiteDatabase db) {
                mDatabase = db;
                internalInitInvalidationTracker(db);
                final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
                if (_callbacks != null) {
                    for (RoomDatabase.Callback _callback : _callbacks) {
                        _callback.onOpen(db);
                    }
                }
            }

            @Override
            public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
                DBUtil.dropFtsSyncTriggers(db);
            }

            @Override
            public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
            }

            @Override
            @NonNull
            public RoomOpenHelper.ValidationResult onValidateSchema(
                    @NonNull final SupportSQLiteDatabase db) {
                final HashMap<String, TableInfo.Column> _columnsTheEntity = new HashMap<String, TableInfo.Column>(3);
                _columnsTheEntity.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
                _columnsTheEntity.put("text", new TableInfo.Column("text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
                _columnsTheEntity.put("custom", new TableInfo.Column("custom", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
                final HashSet<TableInfo.ForeignKey> _foreignKeysTheEntity = new HashSet<TableInfo.ForeignKey>(0);
                final HashSet<TableInfo.Index> _indicesTheEntity = new HashSet<TableInfo.Index>(0);
                final TableInfo _infoTheEntity = new TableInfo("TheEntity", _columnsTheEntity, _foreignKeysTheEntity, _indicesTheEntity);
                final TableInfo _existingTheEntity = TableInfo.read(db, "TheEntity");
                if (!_infoTheEntity.equals(_existingTheEntity)) {
                    return new RoomOpenHelper.ValidationResult(false, "TheEntity(androidx.room.integration.kotlintestapp.test.PreKmpDatabase.TheEntity).\n"
                            + " Expected:\n" + _infoTheEntity + "\n"
                            + " Found:\n" + _existingTheEntity);
                }
                return new RoomOpenHelper.ValidationResult(true, null);
            }
        }, "fbcbaa42ae194c6600f45bb10ebfcc1f", "c5a810f0a0f12e6d217d248fa35d0d13");
        final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
        final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
        return _helper;
    }

    @Override
    @NonNull
    protected InvalidationTracker createInvalidationTracker() {
        final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
        final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
        return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "TheEntity");
    }

    @Override
    public void clearAllTables() {
        super.assertNotMainThread();
        final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
        try {
            super.beginTransaction();
            _db.execSQL("DELETE FROM `TheEntity`");
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
        _typeConvertersMap.put(PreKmpDatabase.TheDao.class, PreKmpDatabase_TheDao_Impl.getRequiredConverters());
        return _typeConvertersMap;
    }

    @Override
    @NonNull
    public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
        final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
        _autoMigrationSpecsSet.add(PreKmpDatabase.MigrationSpec1To2.class);
        return _autoMigrationSpecsSet;
    }

    @Override
    @NonNull
    public List<Migration> getAutoMigrations(
            @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
        final List<Migration> _autoMigrations = new ArrayList<Migration>();
        _autoMigrations.add(new PreKmpDatabase_AutoMigration_1_2_Impl(autoMigrationSpecs.get(PreKmpDatabase.MigrationSpec1To2.class)));
        return _autoMigrations;
    }

    @Override
    public PreKmpDatabase.TheDao getTheDao() {
        if (_preKmpDatabase != null) {
            return _preKmpDatabase;
        } else {
            synchronized(this) {
                if(_preKmpDatabase == null) {
                    _preKmpDatabase = new PreKmpDatabase_TheDao_Impl(this);
                }
                return _preKmpDatabase;
            }
        }
    }
}
