package foo.bar;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Callback;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Configuration;
import android.arch.persistence.room.DatabaseConfiguration;
import android.arch.persistence.room.InvalidationTracker;
import android.arch.persistence.room.RoomOpenHelper;
import android.arch.persistence.room.RoomOpenHelper.Delegate;
import android.arch.persistence.room.util.TableInfo;
import android.arch.persistence.room.util.TableInfo.Column;
import android.arch.persistence.room.util.TableInfo.ForeignKey;
import java.lang.IllegalStateException;
import java.lang.Override;
import java.lang.String;
import java.util.HashMap;
import java.util.HashSet;
import javax.annotation.Generated;

@Generated("android.arch.persistence.room.RoomProcessor")
public class ComplexDatabase_Impl extends ComplexDatabase {
    private volatile ComplexDao _complexDao;

    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
        final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(1923) {
            public void createAllTables(SupportSQLiteDatabase _db) {
                _db.execSQL("CREATE TABLE IF NOT EXISTS `User` (`uid` INTEGER NOT NULL, `name` TEXT, `lastName` TEXT, `ageColumn` INTEGER NOT NULL, PRIMARY KEY(`uid`))");
                _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
                _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, \"6773601c5bcf94c71ee4eb0de04f21a4\")");
            }

            public void dropAllTables(SupportSQLiteDatabase _db) {
                _db.execSQL("DROP TABLE IF EXISTS `User`");
            }

            protected void onCreate(SupportSQLiteDatabase _db) {
                if (mCallbacks != null) {
                    for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
                        mCallbacks.get(_i).onCreate(_db);
                    }
                }
            }

            public void onOpen(SupportSQLiteDatabase _db) {
                mDatabase = _db;
                internalInitInvalidationTracker(_db);
                if (mCallbacks != null) {
                    for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
                        mCallbacks.get(_i).onOpen(_db);
                    }
                }
            }

            protected void validateMigration(SupportSQLiteDatabase _db) {
                final HashMap<String, TableInfo.Column> _columnsUser = new HashMap<String, TableInfo.Column>(4);
                _columnsUser.put("uid", new TableInfo.Column("uid", "INTEGER", true, 1));
                _columnsUser.put("name", new TableInfo.Column("name", "TEXT", false, 0));
                _columnsUser.put("lastName", new TableInfo.Column("lastName", "TEXT", false, 0));
                _columnsUser.put("ageColumn", new TableInfo.Column("ageColumn", "INTEGER", true, 0));
                final HashSet<TableInfo.ForeignKey> _foreignKeysUser = new HashSet<TableInfo.ForeignKey>(0);
                final TableInfo _infoUser = new TableInfo("User", _columnsUser, _foreignKeysUser);
                final TableInfo _existingUser = TableInfo.read(_db, "User");
                if (! _infoUser.equals(_existingUser)) {
                    throw new IllegalStateException("Migration didn't properly handle User(foo.bar.User).\n"
                            + " Expected:\n" + _infoUser + "\n"
                            + " Found:\n" + _existingUser);
                }
            }
        }, "6773601c5bcf94c71ee4eb0de04f21a4");
        final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
                .name(configuration.name)
                .callback(_openCallback)
                .build();
        final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
        return _helper;
    }

    @Override
    protected InvalidationTracker createInvalidationTracker() {
        return new InvalidationTracker(this, "User");
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
