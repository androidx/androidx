package androidx.room.temp

import MyEntity
import android.database.Cursor
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["unchecked", "deprecation"])
public class PojoRowAdapter_1427165205 {
    public fun readFromCursor(
        _cursor: Cursor,
        _cursorIndexOfString: Int,
        _cursorIndexOfNullableString: Int,
    ): MyEntity {
        val _result: MyEntity
        val _tmpString: String
        _tmpString = _cursor.getString(_cursorIndexOfString)
        val _tmpNullableString: String?
        if (_cursor.isNull(_cursorIndexOfNullableString)) {
            _tmpNullableString = null
        } else {
            _tmpNullableString = _cursor.getString(_cursorIndexOfNullableString)
        }
        _result = MyEntity(_tmpString,_tmpNullableString)
        return _result
    }
}