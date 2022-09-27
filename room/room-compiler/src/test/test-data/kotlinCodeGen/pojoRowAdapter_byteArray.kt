package androidx.room.temp

import MyEntity
import android.database.Cursor
import javax.`annotation`.processing.Generated
import kotlin.ByteArray
import kotlin.Int
import kotlin.Suppress

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["unchecked", "deprecation"])
public class PojoRowAdapter_1427165205 {
    public fun readFromCursor(
        _cursor: Cursor,
        _cursorIndexOfPk: Int,
        _cursorIndexOfByteArray: Int,
        _cursorIndexOfNullableByteArray: Int,
    ): MyEntity {
        val _result: MyEntity
        val _tmpPk: Int
        _tmpPk = _cursor.getInt(_cursorIndexOfPk)
        val _tmpByteArray: ByteArray
        _tmpByteArray = _cursor.getBlob(_cursorIndexOfByteArray)
        val _tmpNullableByteArray: ByteArray?
        if (_cursor.isNull(_cursorIndexOfNullableByteArray)) {
            _tmpNullableByteArray = null
        } else {
            _tmpNullableByteArray = _cursor.getBlob(_cursorIndexOfNullableByteArray)
        }
        _result = MyEntity(_tmpPk,_tmpByteArray,_tmpNullableByteArray)
        return _result
    }
}