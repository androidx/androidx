package foo.bar;

import androidx.annotation.NonNull;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenDelegate;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.room.util.ViewInfo;
import androidx.sqlite.SQLite;
import androidx.sqlite.SQLiteConnection;
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
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class ComplexDatabase_Impl extends ComplexDatabase {
    private volatile ComplexDao _complexDao;

    @Override
    @NonNull
    protected RoomOpenDelegate createOpenDelegate() {
        final RoomOpenDelegate _openDelegate = new RoomOpenDelegate(1923, "12b646c55443feeefb567521e2bece85", "2f1dbf49584f5d6c91cb44f8a6ecfee2") {
            @Override
            public void createAllTables(@NonNull final SQLiteConnection connection) {
                SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `User` (`uid` INTEGER NOT NULL, `name` TEXT, `lastName` TEXT, `ageColumn` INTEGER NOT NULL, PRIMARY KEY(`uid`))");
                SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `Child1` (`id` INTEGER NOT NULL, `name` TEXT, `serial` INTEGER, `code` TEXT, PRIMARY KEY(`id`))");
                SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `Child2` (`id` INTEGER NOT NULL, `name` TEXT, `serial` INTEGER, `code` TEXT, PRIMARY KEY(`id`))");
                SQLite.execSQL(connection, "CREATE VIEW `UserSummary` AS SELECT uid, name FROM User");
                SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
                SQLite.execSQL(connection, "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '12b646c55443feeefb567521e2bece85')");
            }

            @Override
            public void dropAllTables(@NonNull final SQLiteConnection connection) {
                SQLite.execSQL(connection, "DROP TABLE IF EXISTS `User`");
                SQLite.execSQL(connection, "DROP TABLE IF EXISTS `Child1`");
                SQLite.execSQL(connection, "DROP TABLE IF EXISTS `Child2`");
                SQLite.execSQL(connection, "DROP VIEW IF EXISTS `UserSummary`");
            }

            @Override
            public void onCreate(@NonNull final SQLiteConnection connection) {
            }

            @Override
            public void onOpen(@NonNull final SQLiteConnection connection) {
                internalInitInvalidationTracker(connection);
            }

            @Override
            public void onPreMigrate(@NonNull final SQLiteConnection connection) {
                DBUtil.dropFtsSyncTriggers(connection);
            }

            @Override
            public void onPostMigrate(@NonNull final SQLiteConnection connection) {
            }

            @Override
            @NonNull
            public RoomOpenDelegate.ValidationResult onValidateSchema(
                    @NonNull final SQLiteConnection connection) {
                final Map<String, TableInfo.Column> _columnsUser = new HashMap<String, TableInfo.Column>(4);
                _columnsUser.put("uid", new TableInfo.Column("uid", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
                _columnsUser.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
                _columnsUser.put("lastName", new TableInfo.Column("lastName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
                _columnsUser.put("ageColumn", new TableInfo.Column("ageColumn", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
                final Set<TableInfo.ForeignKey> _foreignKeysUser = new HashSet<TableInfo.ForeignKey>(0);
                final Set<TableInfo.Index> _indicesUser = new HashSet<TableInfo.Index>(0);
                final TableInfo _infoUser = new TableInfo("User", _columnsUser, _foreignKeysUser, _indicesUser);
                final TableInfo _existingUser = TableInfo.read(connection, "User");
                if (!_infoUser.equals(_existingUser)) {
                    return new RoomOpenDelegate.ValidationResult(false, "User(foo.bar.User).\n"
                            + " Expected:\n" + _infoUser + "\n"
                            + " Found:\n" + _existingUser);
                }
                final Map<String, TableInfo.Column> _columnsChild1 = new HashMap<String, TableInfo.Column>(4);
                _columnsChild1.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
                _columnsChild1.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
                _columnsChild1.put("serial", new TableInfo.Column("serial", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
                _columnsChild1.put("code", new TableInfo.Column("code", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
                final Set<TableInfo.ForeignKey> _foreignKeysChild1 = new HashSet<TableInfo.ForeignKey>(0);
                final Set<TableInfo.Index> _indicesChild1 = new HashSet<TableInfo.Index>(0);
                final TableInfo _infoChild1 = new TableInfo("Child1", _columnsChild1, _foreignKeysChild1, _indicesChild1);
                final TableInfo _existingChild1 = TableInfo.read(connection, "Child1");
                if (!_infoChild1.equals(_existingChild1)) {
                    return new RoomOpenDelegate.ValidationResult(false, "Child1(foo.bar.Child1).\n"
                            + " Expected:\n" + _infoChild1 + "\n"
                            + " Found:\n" + _existingChild1);
                }
                final Map<String, TableInfo.Column> _columnsChild2 = new HashMap<String, TableInfo.Column>(4);
                _columnsChild2.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
                _columnsChild2.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
                _columnsChild2.put("serial", new TableInfo.Column("serial", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
                _columnsChild2.put("code", new TableInfo.Column("code", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
                final Set<TableInfo.ForeignKey> _foreignKeysChild2 = new HashSet<TableInfo.ForeignKey>(0);
                final Set<TableInfo.Index> _indicesChild2 = new HashSet<TableInfo.Index>(0);
                final TableInfo _infoChild2 = new TableInfo("Child2", _columnsChild2, _foreignKeysChild2, _indicesChild2);
                final TableInfo _existingChild2 = TableInfo.read(connection, "Child2");
                if (!_infoChild2.equals(_existingChild2)) {
                    return new RoomOpenDelegate.ValidationResult(false, "Child2(foo.bar.Child2).\n"
                            + " Expected:\n" + _infoChild2 + "\n"
                            + " Found:\n" + _existingChild2);
                }
                final ViewInfo _infoUserSummary = new ViewInfo("UserSummary", "CREATE VIEW `UserSummary` AS SELECT uid, name FROM User");
                final ViewInfo _existingUserSummary = ViewInfo.read(connection, "UserSummary");
                if (!_infoUserSummary.equals(_existingUserSummary)) {
                    return new RoomOpenDelegate.ValidationResult(false, "UserSummary(foo.bar.UserSummary).\n"
                            + " Expected:\n" + _infoUserSummary + "\n"
                            + " Found:\n" + _existingUserSummary);
                }
                return new RoomOpenDelegate.ValidationResult(true, null);
            }
        };
        return _openDelegate;
    }

    @Override
    @NonNull
    protected InvalidationTracker createInvalidationTracker() {
        final Map<String, String> _shadowTablesMap = new HashMap<String, String>(0);
        final Map<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(1);
        final Set<String> _tables = new HashSet<String>(1);
        _tables.add("User");
        _viewTables.put("usersummary", _tables);
        return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "User", "Child1", "Child2");
    }

    @Override
    public void clearAllTables() {
        super.performClear(false, "User", "Child1", "Child2");
    }

    @Override
    @NonNull
    protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
        final Map<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
        _typeConvertersMap.put(ComplexDao.class, ComplexDao_Impl.getRequiredConverters());
        return _typeConvertersMap;
    }

    @Override
    @NonNull
    public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
        final Set<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
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