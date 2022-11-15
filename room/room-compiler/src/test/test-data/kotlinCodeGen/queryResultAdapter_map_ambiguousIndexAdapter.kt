import android.database.Cursor
import androidx.room.AmbiguousColumnResolver
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.query
import java.lang.Class
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
@Suppress(names = ["unchecked", "deprecation"])
public class MyDao_Impl(
    __db: RoomDatabase,
) : MyDao {
    private val __db: RoomDatabase
    init {
        this.__db = __db
    }

    public override fun getUserCommentMap(): Map<User, List<Comment>> {
        val _sql: String = "SELECT * FROM User JOIN Comment ON User.id = Comment.userId"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndices: Array<IntArray> =
                AmbiguousColumnResolver.resolve(_cursor.getColumnNames(), arrayOf(arrayOf("id", "name"),
                    arrayOf("id", "userId", "text")))
            val _result: MutableMap<User, MutableList<Comment>> =
                LinkedHashMap<User, MutableList<Comment>>()
            while (_cursor.moveToNext()) {
                val _key: User
                val _tmpId: Int
                _tmpId = _cursor.getInt(_cursorIndices[0][0])
                val _tmpName: String
                _tmpName = _cursor.getString(_cursorIndices[0][1])
                _key = User(_tmpId,_tmpName)
                val _values: MutableList<Comment>
                if (_result.containsKey(_key)) {
                    _values = _result.getValue(_key)
                } else {
                    _values = ArrayList<Comment>()
                    _result.put(_key, _values)
                }
                if (_cursor.isNull(_cursorIndices[1][0]) && _cursor.isNull(_cursorIndices[1][1]) &&
                    _cursor.isNull(_cursorIndices[1][2])) {
                    continue
                }
                val _value: Comment
                val _tmpId_1: Int
                _tmpId_1 = _cursor.getInt(_cursorIndices[1][0])
                val _tmpUserId: Int
                _tmpUserId = _cursor.getInt(_cursorIndices[1][1])
                val _tmpText: String
                _tmpText = _cursor.getString(_cursorIndices[1][2])
                _value = Comment(_tmpId_1,_tmpUserId,_tmpText)
                _values.add(_value)
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun getUserCommentMapWithoutStarProjection(): Map<User, List<Comment>> {
        val _sql: String =
            "SELECT User.id, name, Comment.id, userId, text FROM User JOIN Comment ON User.id = Comment.userId"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndices: Array<IntArray> = arrayOf(intArrayOf(0, 1), intArrayOf(2, 3, 4))
            val _result: MutableMap<User, MutableList<Comment>> =
                LinkedHashMap<User, MutableList<Comment>>()
            while (_cursor.moveToNext()) {
                val _key: User
                val _tmpId: Int
                _tmpId = _cursor.getInt(_cursorIndices[0][0])
                val _tmpName: String
                _tmpName = _cursor.getString(_cursorIndices[0][1])
                _key = User(_tmpId,_tmpName)
                val _values: MutableList<Comment>
                if (_result.containsKey(_key)) {
                    _values = _result.getValue(_key)
                } else {
                    _values = ArrayList<Comment>()
                    _result.put(_key, _values)
                }
                if (_cursor.isNull(_cursorIndices[1][0]) && _cursor.isNull(_cursorIndices[1][1]) &&
                    _cursor.isNull(_cursorIndices[1][2])) {
                    continue
                }
                val _value: Comment
                val _tmpId_1: Int
                _tmpId_1 = _cursor.getInt(_cursorIndices[1][0])
                val _tmpUserId: Int
                _tmpUserId = _cursor.getInt(_cursorIndices[1][1])
                val _tmpText: String
                _tmpText = _cursor.getString(_cursorIndices[1][2])
                _value = Comment(_tmpId_1,_tmpUserId,_tmpText)
                _values.add(_value)
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