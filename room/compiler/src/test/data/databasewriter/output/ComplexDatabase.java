
package foo.bar;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.db.SupportSQLiteOpenHelper.Callback;
import com.android.support.db.SupportSQLiteOpenHelper.Configuration;
import com.android.support.room.DatabaseConfiguration;
import com.android.support.room.InvalidationTracker;
import com.android.support.room.RoomOpenHelper;
import com.android.support.room.RoomOpenHelper.Delegate;
import java.lang.Override;

public class ComplexDatabase_Impl extends ComplexDatabase {
    private volatile ComplexDao _complexDao;

    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
        final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate() {
            public void createAllTables(SupportSQLiteDatabase _db) {
                _db.execSQL("CREATE TABLE IF NOT EXISTS `User` (`uid` INTEGER, `name` TEXT, `lastName` TEXT, `ageColumn` INTEGER, PRIMARY KEY(`uid`))");
                _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
                _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, \"d4b1d59e1344d0db40fe2cd3fe64d02f\")");
            }

            public void dropAllTables(SupportSQLiteDatabase _db) {
                _db.execSQL("DROP TABLE IF EXISTS `User`");
            }

            public void onOpen(SupportSQLiteDatabase _db) {
                mDatabase = _db;
                internalInitInvalidationTracker(_db);
            }

            protected boolean validateMigration(SupportSQLiteDatabase _db) {
                return true;
            }
        }, "d4b1d59e1344d0db40fe2cd3fe64d02f");
        final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
                .name(configuration.name)
                .version(1923)
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