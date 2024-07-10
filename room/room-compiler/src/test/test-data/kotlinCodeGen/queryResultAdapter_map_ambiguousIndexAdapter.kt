import androidx.room.AmbiguousColumnResolver
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndex
import androidx.room.util.performBlocking
import androidx.room.util.wrapMappedColumns
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Array
import kotlin.Int
import kotlin.IntArray
import kotlin.String
import kotlin.Suppress
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun getUserCommentMap(): Map<User, List<Comment>> {
    val _sql: String = "SELECT * FROM User JOIN Comment ON User.id = Comment.userId"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndices: Array<IntArray> =
            AmbiguousColumnResolver.resolve(_stmt.getColumnNames(), arrayOf(arrayOf("id", "name"),
            arrayOf("id", "userId", "text")))
        val _result: MutableMap<User, MutableList<Comment>> =
            LinkedHashMap<User, MutableList<Comment>>()
        while (_stmt.step()) {
          val _key: User
          val _tmpId: Int
          _tmpId = _stmt.getLong(_cursorIndices[0][0]).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_cursorIndices[0][1])
          _key = User(_tmpId,_tmpName)
          val _values: MutableList<Comment>
          if (_result.containsKey(_key)) {
            _values = _result.getValue(_key)
          } else {
            _values = mutableListOf()
            _result.put(_key, _values)
          }
          if (_stmt.isNull(_cursorIndices[1][0]) && _stmt.isNull(_cursorIndices[1][1]) &&
              _stmt.isNull(_cursorIndices[1][2])) {
            continue
          }
          val _value: Comment
          val _tmpId_1: Int
          _tmpId_1 = _stmt.getLong(_cursorIndices[1][0]).toInt()
          val _tmpUserId: Int
          _tmpUserId = _stmt.getLong(_cursorIndices[1][1]).toInt()
          val _tmpText: String
          _tmpText = _stmt.getText(_cursorIndices[1][2])
          _value = Comment(_tmpId_1,_tmpUserId,_tmpText)
          _values.add(_value)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getUserCommentMapWithoutStarProjection(): Map<User, List<Comment>> {
    val _sql: String =
        "SELECT User.id, name, Comment.id, userId, text FROM User JOIN Comment ON User.id = Comment.userId"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndices: Array<IntArray> = arrayOf(intArrayOf(0, 1), intArrayOf(2, 3, 4))
        val _result: MutableMap<User, MutableList<Comment>> =
            LinkedHashMap<User, MutableList<Comment>>()
        while (_stmt.step()) {
          val _key: User
          val _tmpId: Int
          _tmpId = _stmt.getLong(_cursorIndices[0][0]).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_cursorIndices[0][1])
          _key = User(_tmpId,_tmpName)
          val _values: MutableList<Comment>
          if (_result.containsKey(_key)) {
            _values = _result.getValue(_key)
          } else {
            _values = mutableListOf()
            _result.put(_key, _values)
          }
          if (_stmt.isNull(_cursorIndices[1][0]) && _stmt.isNull(_cursorIndices[1][1]) &&
              _stmt.isNull(_cursorIndices[1][2])) {
            continue
          }
          val _value: Comment
          val _tmpId_1: Int
          _tmpId_1 = _stmt.getLong(_cursorIndices[1][0]).toInt()
          val _tmpUserId: Int
          _tmpUserId = _stmt.getLong(_cursorIndices[1][1]).toInt()
          val _tmpText: String
          _tmpText = _stmt.getText(_cursorIndices[1][2])
          _value = Comment(_tmpId_1,_tmpUserId,_tmpText)
          _values.add(_value)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getUserCommentMapWithoutQueryVerification(): Map<User, List<Comment>> {
    val _sql: String = "SELECT * FROM User JOIN Comment ON User.id = Comment.userId"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndices: Array<IntArray> =
            AmbiguousColumnResolver.resolve(_stmt.getColumnNames(), arrayOf(arrayOf("id", "name"),
            arrayOf("id", "userId", "text")))
        val _wrappedStmt: SQLiteStatement = wrapMappedColumns(_stmt, arrayOf("id", "name"),
            intArrayOf(_cursorIndices[0][0], _cursorIndices[0][1]))
        val _wrappedStmt_1: SQLiteStatement = wrapMappedColumns(_stmt, arrayOf("id", "userId",
            "text"), intArrayOf(_cursorIndices[1][0], _cursorIndices[1][1], _cursorIndices[1][2]))
        val _result: MutableMap<User, MutableList<Comment>> =
            LinkedHashMap<User, MutableList<Comment>>()
        while (_stmt.step()) {
          val _key: User
          _key = __entityStatementConverter_User(_wrappedStmt)
          val _values: MutableList<Comment>
          if (_result.containsKey(_key)) {
            _values = _result.getValue(_key)
          } else {
            _values = mutableListOf()
            _result.put(_key, _values)
          }
          if (_stmt.isNull(_cursorIndices[1][0]) && _stmt.isNull(_cursorIndices[1][1]) &&
              _stmt.isNull(_cursorIndices[1][2])) {
            continue
          }
          val _value: Comment
          _value = __entityStatementConverter_Comment(_wrappedStmt_1)
          _values.add(_value)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __entityStatementConverter_User(statement: SQLiteStatement): User {
    val _entity: User
    val _cursorIndexOfId: Int = getColumnIndex(statement, "id")
    val _cursorIndexOfName: Int = getColumnIndex(statement, "name")
    val _tmpId: Int
    if (_cursorIndexOfId == -1) {
      _tmpId = 0
    } else {
      _tmpId = statement.getLong(_cursorIndexOfId).toInt()
    }
    val _tmpName: String
    if (_cursorIndexOfName == -1) {
      error("Missing value for a NON-NULL column 'name', found NULL value instead.")
    } else {
      _tmpName = statement.getText(_cursorIndexOfName)
    }
    _entity = User(_tmpId,_tmpName)
    return _entity
  }

  private fun __entityStatementConverter_Comment(statement: SQLiteStatement): Comment {
    val _entity: Comment
    val _cursorIndexOfId: Int = getColumnIndex(statement, "id")
    val _cursorIndexOfUserId: Int = getColumnIndex(statement, "userId")
    val _cursorIndexOfText: Int = getColumnIndex(statement, "text")
    val _tmpId: Int
    if (_cursorIndexOfId == -1) {
      _tmpId = 0
    } else {
      _tmpId = statement.getLong(_cursorIndexOfId).toInt()
    }
    val _tmpUserId: Int
    if (_cursorIndexOfUserId == -1) {
      _tmpUserId = 0
    } else {
      _tmpUserId = statement.getLong(_cursorIndexOfUserId).toInt()
    }
    val _tmpText: String
    if (_cursorIndexOfText == -1) {
      error("Missing value for a NON-NULL column 'text', found NULL value instead.")
    } else {
      _tmpText = statement.getText(_cursorIndexOfText)
    }
    _entity = Comment(_tmpId,_tmpUserId,_tmpText)
    return _entity
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
