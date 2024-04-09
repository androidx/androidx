import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import java.util.Optional
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
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

  public override fun queryOfOptional(): Optional<MyEntity> {
    val _sql: String = "SELECT * FROM MyEntity"
    val _statement: RoomSQLiteQuery = acquire(_sql, 0)
    __db.assertNotSuspendingTransaction()
    val _cursor: Cursor = query(__db, _statement, false, null)
    try {
      val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_cursor, "pk")
      val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_cursor, "other")
      val _value: MyEntity?
      if (_cursor.moveToFirst()) {
        val _tmpPk: Int
        _tmpPk = _cursor.getInt(_cursorIndexOfPk)
        val _tmpOther: String
        _tmpOther = _cursor.getString(_cursorIndexOfOther)
        _value = MyEntity(_tmpPk,_tmpOther)
      } else {
        _value = null
      }
      val _result: Optional<MyEntity> = Optional.ofNullable(_value)
      return _result
    } finally {
      _cursor.close()
      _statement.release()
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
