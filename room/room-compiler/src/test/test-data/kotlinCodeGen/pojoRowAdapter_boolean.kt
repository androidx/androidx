package androidx.room.temp

import MyEntity
import android.database.Cursor
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Suppress

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["unchecked", "deprecation"])
public class PojoRowAdapter_1427165205 {
    public fun readFromCursor(
        _cursor: Cursor,
        _cursorIndexOfPk: Int,
        _cursorIndexOfBoolean: Int,
        _cursorIndexOfNullableBoolean: Int,
    ): MyEntity {
        val _result: MyEntity
        val _tmpPk: Int
        _tmpPk = _cursor.getInt(_cursorIndexOfPk)
        val _tmpBoolean: Boolean
        val _tmp: Int
        _tmp = _cursor.getInt(_cursorIndexOfBoolean)
        _tmpBoolean = _tmp != 0
        val _tmpNullableBoolean: Boolean?
        val _tmp_1: Int?
        if (_cursor.isNull(_cursorIndexOfNullableBoolean)) {
            _tmp_1 = null
        } else {
            _tmp_1 = _cursor.getInt(_cursorIndexOfNullableBoolean)
        }
        _tmpNullableBoolean = _tmp_1?.let { it != 0 }
        _result = MyEntity(_tmpPk,_tmpBoolean,_tmpNullableBoolean)
        return _result
    }
}