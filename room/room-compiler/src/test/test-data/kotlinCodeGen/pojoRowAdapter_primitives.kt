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
import kotlin.Byte
import kotlin.Char
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.Short
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.jvm.JvmStatic

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MyDao_Impl(
    __db: RoomDatabase,
) : MyDao {
    private val __db: RoomDatabase

    private val __insertionAdapterOfMyEntity: EntityInsertionAdapter<MyEntity>
    init {
        this.__db = __db
        this.__insertionAdapterOfMyEntity = object : EntityInsertionAdapter<MyEntity>(__db) {
            protected override fun createQuery(): String =
                "INSERT OR ABORT INTO `MyEntity` (`int`,`short`,`byte`,`long`,`char`,`float`,`double`) VALUES (?,?,?,?,?,?,?)"

            protected override fun bind(statement: SupportSQLiteStatement, entity: MyEntity) {
                statement.bindLong(1, entity.int.toLong())
                statement.bindLong(2, entity.short.toLong())
                statement.bindLong(3, entity.byte.toLong())
                statement.bindLong(4, entity.long)
                statement.bindLong(5, entity.char.toLong())
                statement.bindDouble(6, entity.float.toDouble())
                statement.bindDouble(7, entity.double)
            }
        }
    }

    public override fun addEntity(item: MyEntity) {
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
            val _cursorIndexOfInt: Int = getColumnIndexOrThrow(_cursor, "int")
            val _cursorIndexOfShort: Int = getColumnIndexOrThrow(_cursor, "short")
            val _cursorIndexOfByte: Int = getColumnIndexOrThrow(_cursor, "byte")
            val _cursorIndexOfLong: Int = getColumnIndexOrThrow(_cursor, "long")
            val _cursorIndexOfChar: Int = getColumnIndexOrThrow(_cursor, "char")
            val _cursorIndexOfFloat: Int = getColumnIndexOrThrow(_cursor, "float")
            val _cursorIndexOfDouble: Int = getColumnIndexOrThrow(_cursor, "double")
            val _result: MyEntity
            if (_cursor.moveToFirst()) {
                val _tmpInt: Int
                _tmpInt = _cursor.getInt(_cursorIndexOfInt)
                val _tmpShort: Short
                _tmpShort = _cursor.getShort(_cursorIndexOfShort)
                val _tmpByte: Byte
                _tmpByte = _cursor.getShort(_cursorIndexOfByte).toByte()
                val _tmpLong: Long
                _tmpLong = _cursor.getLong(_cursorIndexOfLong)
                val _tmpChar: Char
                _tmpChar = _cursor.getInt(_cursorIndexOfChar).toChar()
                val _tmpFloat: Float
                _tmpFloat = _cursor.getFloat(_cursorIndexOfFloat)
                val _tmpDouble: Double
                _tmpDouble = _cursor.getDouble(_cursorIndexOfDouble)
                _result = MyEntity(_tmpInt,_tmpShort,_tmpByte,_tmpLong,_tmpChar,_tmpFloat,_tmpDouble)
            } else {
                error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
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