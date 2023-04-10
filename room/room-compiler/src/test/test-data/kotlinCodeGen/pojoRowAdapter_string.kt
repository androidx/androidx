import android.database.Cursor
import androidx.room.EntityInsertionAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.Class
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.jvm.JvmStatic

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION"])
public class MyDao_Impl(
    __db: RoomDatabase,
) : MyDao {
    private val __db: RoomDatabase

    private val __insertionAdapterOfMyEntity: EntityInsertionAdapter<MyEntity>
    init {
        this.__db = __db
        this.__insertionAdapterOfMyEntity = object : EntityInsertionAdapter<MyEntity>(__db) {
            public override fun createQuery(): String =
                "INSERT OR ABORT INTO `MyEntity` (`string`,`nullableString`) VALUES (?,?)"

            public override fun bind(statement: SupportSQLiteStatement, entity: MyEntity): Unit {
                statement.bindString(1, entity.string)
                if (entity.nullableString == null) {
                    statement.bindNull(2)
                } else {
                    statement.bindString(2, entity.nullableString)
                }
            }
        }
    }

    public override fun addEntity(item: MyEntity): Unit {
        __db.assertNotSuspendingTransaction()
        __db.beginTransaction()
        try {
            __insertionAdapterOfMyEntity.insert(item)
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override fun getEntity(): MyEntity {
        val _sql: String = "SELECT * FROM MyEntity"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndexOfString: Int = getColumnIndexOrThrow(_cursor, "string")
            val _cursorIndexOfNullableString: Int = getColumnIndexOrThrow(_cursor, "nullableString")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpString: String
                _tmpString = _cursor.getString(_cursorIndexOfString)
                val _tmpNullableString: String?
                if (_cursor.isNull(_cursorIndexOfNullableString)) {
                    _tmpNullableString = null
                } else {
                    _tmpNullableString = _cursor.getString(_cursorIndexOfNullableString)
                }
                _result = MyEntity(_tmpString,_tmpNullableString)
            } else {
                error("Cursor was empty, but expected a single item.")
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}