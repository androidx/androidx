import android.database.Cursor
import androidx.room.AmbiguousColumnResolver
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndex
import androidx.room.util.query
import androidx.room.util.wrapMappedColumns
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
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
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

    public override fun getUserCommentMapWithoutQueryVerification(): Map<User, List<Comment>> {
        val _sql: String = "SELECT * FROM User JOIN Comment ON User.id = Comment.userId"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, false, null)
        try {
            val _cursorIndices: Array<IntArray> =
                AmbiguousColumnResolver.resolve(_cursor.getColumnNames(), arrayOf(arrayOf("id", "name"),
                    arrayOf("id", "userId", "text")))
            val _wrappedCursor: Cursor = wrapMappedColumns(_cursor, arrayOf("id", "name"),
                intArrayOf(_cursorIndices[0][0], _cursorIndices[0][1]))
            val _wrappedCursor_1: Cursor = wrapMappedColumns(_cursor, arrayOf("id", "userId", "text"),
                intArrayOf(_cursorIndices[1][0], _cursorIndices[1][1], _cursorIndices[1][2]))
            val _result: MutableMap<User, MutableList<Comment>> =
                LinkedHashMap<User, MutableList<Comment>>()
            while (_cursor.moveToNext()) {
                val _key: User
                _key = __entityCursorConverter_User(_wrappedCursor)
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
                _value = __entityCursorConverter_Comment(_wrappedCursor_1)
                _values.add(_value)
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    private fun __entityCursorConverter_User(cursor: Cursor): User {
        val _entity: User
        val _cursorIndexOfId: Int = getColumnIndex(cursor, "id")
        val _cursorIndexOfName: Int = getColumnIndex(cursor, "name")
        val _tmpId: Int
        if (_cursorIndexOfId == -1) {
            _tmpId = 0
        } else {
            _tmpId = cursor.getInt(_cursorIndexOfId)
        }
        val _tmpName: String
        if (_cursorIndexOfName == -1) {
            error("Missing value for a NON-NULL column 'name', found NULL value instead.")
        } else {
            _tmpName = cursor.getString(_cursorIndexOfName)
        }
        _entity = User(_tmpId,_tmpName)
        return _entity
    }

    private fun __entityCursorConverter_Comment(cursor: Cursor): Comment {
        val _entity: Comment
        val _cursorIndexOfId: Int = getColumnIndex(cursor, "id")
        val _cursorIndexOfUserId: Int = getColumnIndex(cursor, "userId")
        val _cursorIndexOfText: Int = getColumnIndex(cursor, "text")
        val _tmpId: Int
        if (_cursorIndexOfId == -1) {
            _tmpId = 0
        } else {
            _tmpId = cursor.getInt(_cursorIndexOfId)
        }
        val _tmpUserId: Int
        if (_cursorIndexOfUserId == -1) {
            _tmpUserId = 0
        } else {
            _tmpUserId = cursor.getInt(_cursorIndexOfUserId)
        }
        val _tmpText: String
        if (_cursorIndexOfText == -1) {
            error("Missing value for a NON-NULL column 'text', found NULL value instead.")
        } else {
            _tmpText = cursor.getString(_cursorIndexOfText)
        }
        _entity = Comment(_tmpId,_tmpUserId,_tmpText)
        return _entity
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}