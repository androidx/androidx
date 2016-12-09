package foo.bar;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.db.SupportSQLiteOpenHelper.Callback;
import com.android.support.db.SupportSQLiteOpenHelper.Configuration;
import com.android.support.room.DatabaseConfiguration;

public class ComplexDatabase_Impl extends ComplexDatabase {
    public ComplexDatabase_Impl(DatabaseConfiguration configuration) {
        super(configuration);
    }

    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
        final SupportSQLiteOpenHelper.Callback _openCallback = new SupportSQLiteOpenHelper.Callback() {
            public void onCreate(SupportSQLiteDatabase _db) {
            }

            public void onUpgrade(SupportSQLiteDatabase _db, int _oldVersion, int _newVersion) {
            }

            public void onDowngrade(SupportSQLiteDatabase _db, int _oldVersion, int _newVersion) {
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
}