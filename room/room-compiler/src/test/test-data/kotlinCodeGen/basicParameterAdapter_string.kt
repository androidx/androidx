import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performReadBlocking
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun stringParam(arg: String): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity WHERE string = ?"
    return performReadBlocking(__db, _sql) { _stmt ->
      var _argIndex: Int = 1
      _stmt.bindText(_argIndex, arg)
      val _cursorIndexOfString: Int = getColumnIndexOrThrow(_stmt, "string")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpString: String
        _tmpString = _stmt.getText(_cursorIndexOfString)
        _result = MyEntity(_tmpString)
      } else {
        error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
      }
      _result
    }
  }

  public override fun nullableStringParam(arg: String?): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity WHERE string = ?"
    return performReadBlocking(__db, _sql) { _stmt ->
      var _argIndex: Int = 1
      if (arg == null) {
        _stmt.bindNull(_argIndex)
      } else {
        _stmt.bindText(_argIndex, arg)
      }
      val _cursorIndexOfString: Int = getColumnIndexOrThrow(_stmt, "string")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpString: String
        _tmpString = _stmt.getText(_cursorIndexOfString)
        _result = MyEntity(_tmpString)
      } else {
        error("The query result was empty, but expected a single row to return a NON-NULL object of type <MyEntity>.")
      }
      _result
    }
  }

  public companion object {
    @JvmStatic
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
