package androidx.room.temp

import MyEntity
import android.database.Cursor
import javax.`annotation`.processing.Generated
import kotlin.Byte
import kotlin.Char
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.Short
import kotlin.Suppress

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["unchecked", "deprecation"])
public class PojoRowAdapter_1427165205 {
    public fun readFromCursor(
        _cursor: Cursor,
        _cursorIndexOfInt: Int,
        _cursorIndexOfShort: Int,
        _cursorIndexOfByte: Int,
        _cursorIndexOfLong: Int,
        _cursorIndexOfChar: Int,
        _cursorIndexOfFloat: Int,
        _cursorIndexOfDouble: Int,
    ): MyEntity {
        val _result: MyEntity
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
        return _result
    }
}