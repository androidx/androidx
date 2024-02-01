package androidx.room.integration.kotlintestapp.test;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "BanSynchronizedMethods", "RestrictedApiAndroidX", "UnknownNullness"})
public final class PreKmpDatabase_TheDao_Impl implements PreKmpDatabase.TheDao {
    private final RoomDatabase __db;

    private final EntityInsertionAdapter<PreKmpDatabase.TheEntity> __insertionAdapterOfTheEntity;

    private PreKmpDatabase.TheConverter __theConverter;

    public PreKmpDatabase_TheDao_Impl(@NonNull final RoomDatabase __db) {
        this.__db = __db;
        this.__insertionAdapterOfTheEntity = new EntityInsertionAdapter<PreKmpDatabase.TheEntity>(__db) {
            @Override
            @NonNull
            protected String createQuery() {
                return "INSERT OR ABORT INTO `TheEntity` (`id`,`text`,`custom`) VALUES (?,?,?)";
            }

            @Override
            protected void bind(@NonNull final SupportSQLiteStatement statement,
                    @NonNull final PreKmpDatabase.TheEntity entity) {
                statement.bindLong(1, entity.getId());
                statement.bindString(2, entity.getText());
                final byte[] _tmp = __theConverter().fromCustomData(entity.getCustom());
                statement.bindBlob(3, _tmp);
            }
        };
    }

    @Override
    public void insert(final PreKmpDatabase.TheEntity it) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            __insertionAdapterOfTheEntity.insert(it);
            __db.setTransactionSuccessful();
        } finally {
            __db.endTransaction();
        }
    }

    @Override
    public List<PreKmpDatabase.TheEntity> query() {
        final String _sql = "SELECT * FROM TheEntity";
        final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
        __db.assertNotSuspendingTransaction();
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
            final int _cursorIndexOfCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "custom");
            final List<PreKmpDatabase.TheEntity> _result = new ArrayList<PreKmpDatabase.TheEntity>(_cursor.getCount());
            while (_cursor.moveToNext()) {
                final PreKmpDatabase.TheEntity _item;
                final long _tmpId;
                _tmpId = _cursor.getLong(_cursorIndexOfId);
                final String _tmpText;
                _tmpText = _cursor.getString(_cursorIndexOfText);
                final PreKmpDatabase.CustomData _tmpCustom;
                final byte[] _tmp;
                _tmp = _cursor.getBlob(_cursorIndexOfCustom);
                _tmpCustom = __theConverter().toCustomData(_tmp);
                _item = new PreKmpDatabase.TheEntity(_tmpId,_tmpText,_tmpCustom);
                _result.add(_item);
            }
            return _result;
        } finally {
            _cursor.close();
            _statement.release();
        }
    }

    @NonNull
    public static List<Class<?>> getRequiredConverters() {
        return Arrays.asList(PreKmpDatabase.TheConverter.class);
    }

    private synchronized PreKmpDatabase.TheConverter __theConverter() {
        if (__theConverter == null) {
            __theConverter = __db.getTypeConverter(PreKmpDatabase.TheConverter.class);
        }
        return __theConverter;
    }
}
