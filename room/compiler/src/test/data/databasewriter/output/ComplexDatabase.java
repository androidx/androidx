package foo.bar;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.db.SupportSQLiteOpenHelper.Callback;
import com.android.support.db.SupportSQLiteOpenHelper.Configuration;
import com.android.support.room.DatabaseConfiguration;
import java.lang.Override;

public class ComplexDatabase_Impl extends ComplexDatabase {
    private volatile ComplexDao _complexDao;

    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
        final SupportSQLiteOpenHelper.Callback _openCallback = new SupportSQLiteOpenHelper.Callback() {
            public void onCreate(SupportSQLiteDatabase _db) {
                _db.execSQL("CREATE TABLE IF NOT EXISTS `User` (`uid` INTEGER, `name` TEXT,"
                        + " `lastName` TEXT, `ageColumn` INTEGER, PRIMARY KEY(`uid`))");
            }

            public void onUpgrade(SupportSQLiteDatabase _db, int _oldVersion, int _newVersion) {
                _db.execSQL("DROP TABLE IF EXISTS `User`");
                _db.execSQL("CREATE TABLE IF NOT EXISTS `User` (`uid` INTEGER, `name` TEXT,"
                        + " `lastName` TEXT, `ageColumn` INTEGER, PRIMARY KEY(`uid`))");
            }

            public void onDowngrade(SupportSQLiteDatabase _db, int _oldVersion, int _newVersion) {
                onUpgrade(_db, _oldVersion, _newVersion);
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
