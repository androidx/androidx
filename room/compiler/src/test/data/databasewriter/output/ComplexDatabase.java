package foo.bar;

import android.database.Cursor;
import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.db.SupportSQLiteOpenHelper.Callback;
import com.android.support.db.SupportSQLiteOpenHelper.Configuration;
import com.android.support.room.DatabaseConfiguration;
import com.android.support.room.InvalidationTracker;
import com.android.support.room.util.StringUtil;
import java.lang.IllegalStateException;
import java.lang.Override;

public class ComplexDatabase_Impl extends ComplexDatabase {
    private volatile ComplexDao _complexDao;

    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
        final SupportSQLiteOpenHelper.Callback _openCallback = new SupportSQLiteOpenHelper.Callback() {
            public void onCreate(SupportSQLiteDatabase _db) {
                _db.execSQL("CREATE TABLE IF NOT EXISTS `room_master_table`(id INTEGER PRIMARY KEY,identity_hash TEXT)");
                _db.execSQL("INSERT OR REPLACE INTO room_master_table VALUES(42,\"d4b1d59e1344d0db40fe2cd3fe64d02f\")");
                _db.execSQL("CREATE TABLE IF NOT EXISTS `User` (`uid` INTEGER, `name` TEXT, `lastName` TEXT, `ageColumn` INTEGER, PRIMARY KEY(`uid`))");
            }

            public void onUpgrade(SupportSQLiteDatabase _db, int _oldVersion, int _newVersion) {
                _db.execSQL("DROP TABLE IF EXISTS `User`");
                _db.execSQL("CREATE TABLE IF NOT EXISTS `room_master_table`(id INTEGER PRIMARY KEY,identity_hash TEXT)");
                _db.execSQL("INSERT OR REPLACE INTO room_master_table VALUES(42,\"d4b1d59e1344d0db40fe2cd3fe64d02f\")");
                _db.execSQL("CREATE TABLE IF NOT EXISTS `User` (`uid` INTEGER, `name` TEXT, `lastName` TEXT, `ageColumn` INTEGER, PRIMARY KEY(`uid`))");
            }

            public void onDowngrade(SupportSQLiteDatabase _db, int _oldVersion, int _newVersion) {
                onUpgrade(_db, _oldVersion, _newVersion);
            }

            public void onOpen(SupportSQLiteDatabase _db) {
                String identityHash = "";
                Cursor cursor = _db.rawQuery("SELECT identity_hash FROM room_master_table WHERE id = 42 LIMIT 1", StringUtil.EMPTY_STRING_ARRAY);
                try {
                    if (cursor.moveToFirst()) {
                        identityHash = cursor.getString(0);
                    }
                } finally {
                    cursor.close();
                }
                if(!"d4b1d59e1344d0db40fe2cd3fe64d02f".equals(identityHash)) {
                    throw new IllegalStateException("Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number. You can simply fix this by increasing the version number.");
                }
                mDatabase = _db;
                internalInitInvalidationTracker(_db);
            }
        };
        final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
                .name(configuration.name)
                .version(configuration.version)
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