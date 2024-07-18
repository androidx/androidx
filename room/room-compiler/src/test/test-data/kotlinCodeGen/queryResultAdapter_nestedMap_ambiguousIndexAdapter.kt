import android.database.Cursor
import androidx.room.AmbiguousColumnResolver
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.query
import java.lang.Class
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.LinkedHashMap
import javax.`annotation`.processing.Generated
import kotlin.Array
import kotlin.Int
import kotlin.IntArray
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.jvm.JvmStatic

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MyDao_Impl(
    __db: RoomDatabase,
) : MyDao {
    private val __db: RoomDatabase
    init {
        this.__db = __db
    }

    public override fun getLeftJoinUserNestedMap(): Map<User, Map<Avatar, List<Comment>>> {
        val _sql: String =
            "SELECT * FROM User JOIN Avatar ON User.id = Avatar.userId JOIN Comment ON Avatar.userId = Comment.userId"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndices: Array<IntArray> =
                AmbiguousColumnResolver.resolve(_cursor.getColumnNames(), arrayOf(arrayOf("id", "name"),
                    arrayOf("userId", "url", "data"), arrayOf("id", "userId", "text")))
            val _result: MutableMap<User, MutableMap<Avatar, MutableList<Comment>>> =
                LinkedHashMap<User, MutableMap<Avatar, MutableList<Comment>>>()
            while (_cursor.moveToNext()) {
                val _key: User
                val _tmpId: Int
                _tmpId = _cursor.getInt(_cursorIndices[0][0])
                val _tmpName: String
                _tmpName = _cursor.getString(_cursorIndices[0][1])
                _key = User(_tmpId,_tmpName)
                val _values: MutableMap<Avatar, MutableList<Comment>>
                if (_result.containsKey(_key)) {
                    _values = _result.getValue(_key)
                } else {
                    _values = LinkedHashMap<Avatar, MutableList<Comment>>()
                    _result.put(_key, _values)
                }
                if (_cursor.isNull(_cursorIndices[1][0]) && _cursor.isNull(_cursorIndices[1][1]) &&
                    _cursor.isNull(_cursorIndices[1][2])) {
                    continue
                }
                val _key_1: Avatar
                val _tmpUserId: Int
                _tmpUserId = _cursor.getInt(_cursorIndices[1][0])
                val _tmpUrl: String
                _tmpUrl = _cursor.getString(_cursorIndices[1][1])
                val _tmpData: ByteBuffer
                _tmpData = ByteBuffer.wrap(_cursor.getBlob(_cursorIndices[1][2]))
                _key_1 = Avatar(_tmpUserId,_tmpUrl,_tmpData)
                val _values_1: MutableList<Comment>
                if (_values.containsKey(_key_1)) {
                    _values_1 = _values.getValue(_key_1)
                } else {
                    _values_1 = ArrayList<Comment>()
                    _values.put(_key_1, _values_1)
                }
                if (_cursor.isNull(_cursorIndices[2][0]) && _cursor.isNull(_cursorIndices[2][1]) &&
                    _cursor.isNull(_cursorIndices[2][2])) {
                    continue
                }
                val _value: Comment
                val _tmpId_1: Int
                _tmpId_1 = _cursor.getInt(_cursorIndices[2][0])
                val _tmpUserId_1: Int
                _tmpUserId_1 = _cursor.getInt(_cursorIndices[2][1])
                val _tmpText: String
                _tmpText = _cursor.getString(_cursorIndices[2][2])
                _value = Comment(_tmpId_1,_tmpUserId_1,_tmpText)
                _values_1.add(_value)
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