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
                val _tmpInt: Int? = entity.int
                if (_tmpInt == null) {
                    statement.bindNull(1)
                } else {
                    statement.bindLong(1, _tmpInt.toLong())
                }
                val _tmpShort: Short? = entity.short
                if (_tmpShort == null) {
                    statement.bindNull(2)
                } else {
                    statement.bindLong(2, _tmpShort.toLong())
                }
                val _tmpByte: Byte? = entity.byte
                if (_tmpByte == null) {
                    statement.bindNull(3)
                } else {
                    statement.bindLong(3, _tmpByte.toLong())
                }
                val _tmpLong: Long? = entity.long
                if (_tmpLong == null) {
                    statement.bindNull(4)
                } else {
                    statement.bindLong(4, _tmpLong)
                }
                val _tmpChar: Char? = entity.char
                if (_tmpChar == null) {
                    statement.bindNull(5)
                } else {
                    statement.bindLong(5, _tmpChar.toLong())
                }
                val _tmpFloat: Float? = entity.float
                if (_tmpFloat == null) {
                    statement.bindNull(6)
                } else {
                    statement.bindDouble(6, _tmpFloat.toDouble())
                }
                val _tmpDouble: Double? = entity.double
                if (_tmpDouble == null) {
                    statement.bindNull(7)
                } else {
                    statement.bindDouble(7, _tmpDouble)
                }
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
                val _tmpInt: Int?
                if (_cursor.isNull(_cursorIndexOfInt)) {
                    _tmpInt = null
                } else {
                    _tmpInt = _cursor.getInt(_cursorIndexOfInt)
                }
                val _tmpShort: Short?
                if (_cursor.isNull(_cursorIndexOfShort)) {
                    _tmpShort = null
                } else {
                    _tmpShort = _cursor.getShort(_cursorIndexOfShort)
                }
                val _tmpByte: Byte?
                if (_cursor.isNull(_cursorIndexOfByte)) {
                    _tmpByte = null
                } else {
                    _tmpByte = _cursor.getShort(_cursorIndexOfByte).toByte()
                }
                val _tmpLong: Long?
                if (_cursor.isNull(_cursorIndexOfLong)) {
                    _tmpLong = null
                } else {
                    _tmpLong = _cursor.getLong(_cursorIndexOfLong)
                }
                val _tmpChar: Char?
                if (_cursor.isNull(_cursorIndexOfChar)) {
                    _tmpChar = null
                } else {
                    _tmpChar = _cursor.getInt(_cursorIndexOfChar).toChar()
                }
                val _tmpFloat: Float?
                if (_cursor.isNull(_cursorIndexOfFloat)) {
                    _tmpFloat = null
                } else {
                    _tmpFloat = _cursor.getFloat(_cursorIndexOfFloat)
                }
                val _tmpDouble: Double?
                if (_cursor.isNull(_cursorIndexOfDouble)) {
                    _tmpDouble = null
                } else {
                    _tmpDouble = _cursor.getDouble(_cursorIndexOfDouble)
                }
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