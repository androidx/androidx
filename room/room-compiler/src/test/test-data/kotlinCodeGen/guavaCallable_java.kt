import android.database.Cursor
import android.os.CancellationSignal
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.guava.GuavaRoom
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.query
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.reflect.KClass
import kotlin.text.StringBuilder

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun getListenableFuture(arg: Array<String?>?): ListenableFuture<MyEntity?>? {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = if (arg == null) 1 else arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _argCount: Int = 0 + _inputSize
    val _statement: RoomSQLiteQuery = acquire(_sql, _argCount)
    var _argIndex: Int = 1
    if (arg == null) {
      _statement.bindNull(_argIndex)
    } else {
      for (_item: String? in arg) {
        if (_item == null) {
          _statement.bindNull(_argIndex)
        } else {
          _statement.bindString(_argIndex, _item)
        }
        _argIndex++
      }
    }
    val _cancellationSignal: CancellationSignal = CancellationSignal()
    return GuavaRoom.createListenableFuture(__db, false, object : Callable<MyEntity?> {
      public override fun call(): MyEntity? {
        val _cursor: Cursor = query(__db, _statement, false, _cancellationSignal)
        try {
          val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_cursor, "pk")
          val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_cursor, "other")
          val _result: MyEntity?
          if (_cursor.moveToFirst()) {
            val _tmpPk: Int
            _tmpPk = _cursor.getInt(_cursorIndexOfPk)
            val _tmpOther: String
            _tmpOther = _cursor.getString(_cursorIndexOfOther)
            _result = MyEntity(_tmpPk,_tmpOther)
          } else {
            _result = null
          }
          return _result
        } finally {
          _cursor.close()
        }
      }
    }, _statement, true, _cancellationSignal)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
