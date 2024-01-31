import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performReadSuspending
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

  public override suspend fun getEntity(): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity"
    return performReadSuspending(__db, _sql) { _stmt ->
      val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpPk: Int
        _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
        _result = MyEntity(_tmpPk)
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
