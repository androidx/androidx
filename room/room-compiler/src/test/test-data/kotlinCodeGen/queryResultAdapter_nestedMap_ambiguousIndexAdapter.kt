import androidx.room.AmbiguousColumnResolver
import androidx.room.RoomDatabase
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import java.nio.ByteBuffer
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

  public override fun getLeftJoinUserNestedMap(): Map<User, Map<Avatar, List<Comment>>> {
    val _sql: String =
        "SELECT * FROM User JOIN Avatar ON User.id = Avatar.userId JOIN Comment ON Avatar.userId = Comment.userId"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndices: Array<IntArray> =
            AmbiguousColumnResolver.resolve(_stmt.getColumnNames(), arrayOf(arrayOf("id", "name"),
            arrayOf("userId", "url", "data"), arrayOf("id", "userId", "text")))
        val _result: MutableMap<User, MutableMap<Avatar, MutableList<Comment>>> =
            LinkedHashMap<User, MutableMap<Avatar, MutableList<Comment>>>()
        while (_stmt.step()) {
          val _key: User
          val _tmpId: Int
          _tmpId = _stmt.getLong(_cursorIndices[0][0]).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_cursorIndices[0][1])
          _key = User(_tmpId,_tmpName)
          val _values: MutableMap<Avatar, MutableList<Comment>>
          if (_result.containsKey(_key)) {
            _values = _result.getValue(_key)
          } else {
            _values = LinkedHashMap<Avatar, MutableList<Comment>>()
            _result.put(_key, _values)
          }
          if (_stmt.isNull(_cursorIndices[1][0]) && _stmt.isNull(_cursorIndices[1][1]) &&
              _stmt.isNull(_cursorIndices[1][2])) {
            continue
          }
          val _key_1: Avatar
          val _tmpUserId: Int
          _tmpUserId = _stmt.getLong(_cursorIndices[1][0]).toInt()
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_cursorIndices[1][1])
          val _tmpData: ByteBuffer
          _tmpData = ByteBuffer.wrap(_stmt.getBlob(_cursorIndices[1][2]))
          _key_1 = Avatar(_tmpUserId,_tmpUrl,_tmpData)
          val _values_1: MutableList<Comment>
          if (_values.containsKey(_key_1)) {
            _values_1 = _values.getValue(_key_1)
          } else {
            _values_1 = mutableListOf()
            _values.put(_key_1, _values_1)
          }
          if (_stmt.isNull(_cursorIndices[2][0]) && _stmt.isNull(_cursorIndices[2][1]) &&
              _stmt.isNull(_cursorIndices[2][2])) {
            continue
          }
          val _value: Comment
          val _tmpId_1: Int
          _tmpId_1 = _stmt.getLong(_cursorIndices[2][0]).toInt()
          val _tmpUserId_1: Int
          _tmpUserId_1 = _stmt.getLong(_cursorIndices[2][1]).toInt()
          val _tmpText: String
          _tmpText = _stmt.getText(_cursorIndices[2][2])
          _value = Comment(_tmpId_1,_tmpUserId_1,_tmpText)
          _values_1.add(_value)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
