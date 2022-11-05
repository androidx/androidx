package foo.bar;

import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.RoomOpenHelper.ValidationResult;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.room.util.TableInfo.Column;
import androidx.room.util.TableInfo.ForeignKey;
import androidx.room.util.TableInfo.Index;
import androidx.room.util.ViewInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;
import java.util.HashSet;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
final class ComplexDatabase_Impl_OpenHelperDelegate extends RoomOpenHelper.Delegate {
    private final RoomDatabase roomDb;

    ComplexDatabase_Impl_OpenHelperDelegate(RoomDatabase roomDb) {
        super(1923);
        this.roomDb = roomDb;
    }

    @Override
    public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `User` (`uid` INTEGER NOT NULL, `name` TEXT, `lastName` TEXT, `ageColumn` INTEGER NOT NULL, PRIMARY KEY(`uid`))");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `Child1` (`id` INTEGER NOT NULL, `name` TEXT, `serial` INTEGER, `code` TEXT, PRIMARY KEY(`id`))");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `Child2` (`id` INTEGER NOT NULL, `name` TEXT, `serial` INTEGER, `code` TEXT, PRIMARY KEY(`id`))");
        _db.execSQL("CREATE VIEW `UserSummary` AS SELECT uid, name FROM User");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '12b646c55443feeefb567521e2bece85')");
    }

    @Override
    public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `User`");
        _db.execSQL("DROP TABLE IF EXISTS `Child1`");
        _db.execSQL("DROP TABLE IF EXISTS `Child2`");
        _db.execSQL("DROP VIEW IF EXISTS `UserSummary`");
        if (roomDb.getCallbacks() != null) {
            for (int _i = 0, _size = roomDb.getCallbacks().size(); _i < _size; _i++) {
                roomDb.getCallbacks().get(_i).onDestructiveMigration(_db);
            }
        }
    }

    @Override
    public void onCreate(SupportSQLiteDatabase _db) {
        if (roomDb.getCallbacks() != null) {
            for (int _i = 0, _size = roomDb.getCallbacks().size(); _i < _size; _i++) {
                roomDb.getCallbacks().get(_i).onCreate(_db);
            }
        }
    }

    @Override
    public void onOpen(SupportSQLiteDatabase _db) {
        roomDb.setDatabase(_db);
        roomDb.internalInitInvalidationTracker(_db);
        if (roomDb.getCallbacks() != null) {
            for (int _i = 0, _size = roomDb.getCallbacks().size(); _i < _size; _i++) {
                roomDb.getCallbacks().get(_i).onOpen(_db);
            }
        }
    }

    @Override
    public void onPreMigrate(SupportSQLiteDatabase _db) {
        DBUtil.dropFtsSyncTriggers(_db);
    }

    @Override
    public void onPostMigrate(SupportSQLiteDatabase _db) {
    }

    @Override
    public RoomOpenHelper.ValidationResult onValidateSchema(SupportSQLiteDatabase _db) {
        final HashMap<String, TableInfo.Column> _columnsUser = new HashMap<String, TableInfo.Column>(4);
        _columnsUser.put("uid", new TableInfo.Column("uid", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("lastName", new TableInfo.Column("lastName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUser.put("ageColumn", new TableInfo.Column("ageColumn", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUser = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUser = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUser = new TableInfo("User", _columnsUser, _foreignKeysUser, _indicesUser);
        final TableInfo _existingUser = TableInfo.read(_db, "User");
        if (! _infoUser.equals(_existingUser)) {
            return new RoomOpenHelper.ValidationResult(false, "User(foo.bar.User).\n"
                    + " Expected:\n" + _infoUser + "\n"
                    + " Found:\n" + _existingUser);
        }
        final HashMap<String, TableInfo.Column> _columnsChild1 = new HashMap<String, TableInfo.Column>(4);
        _columnsChild1.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChild1.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChild1.put("serial", new TableInfo.Column("serial", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChild1.put("code", new TableInfo.Column("code", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChild1 = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChild1 = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChild1 = new TableInfo("Child1", _columnsChild1, _foreignKeysChild1, _indicesChild1);
        final TableInfo _existingChild1 = TableInfo.read(_db, "Child1");
        if (! _infoChild1.equals(_existingChild1)) {
            return new RoomOpenHelper.ValidationResult(false, "Child1(foo.bar.Child1).\n"
                    + " Expected:\n" + _infoChild1 + "\n"
                    + " Found:\n" + _existingChild1);
        }
        final HashMap<String, TableInfo.Column> _columnsChild2 = new HashMap<String, TableInfo.Column>(4);
        _columnsChild2.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChild2.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChild2.put("serial", new TableInfo.Column("serial", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChild2.put("code", new TableInfo.Column("code", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChild2 = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChild2 = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChild2 = new TableInfo("Child2", _columnsChild2, _foreignKeysChild2, _indicesChild2);
        final TableInfo _existingChild2 = TableInfo.read(_db, "Child2");
        if (! _infoChild2.equals(_existingChild2)) {
            return new RoomOpenHelper.ValidationResult(false, "Child2(foo.bar.Child2).\n"
                    + " Expected:\n" + _infoChild2 + "\n"
                    + " Found:\n" + _existingChild2);
        }
        final ViewInfo _infoUserSummary = new ViewInfo("UserSummary", "CREATE VIEW `UserSummary` AS SELECT uid, name FROM User");
        final ViewInfo _existingUserSummary = ViewInfo.read(_db, "UserSummary");
        if (! _infoUserSummary.equals(_existingUserSummary)) {
            return new RoomOpenHelper.ValidationResult(false, "UserSummary(foo.bar.UserSummary).\n"
                    + " Expected:\n" + _infoUserSummary + "\n"
                    + " Found:\n" + _existingUserSummary);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
    }
}