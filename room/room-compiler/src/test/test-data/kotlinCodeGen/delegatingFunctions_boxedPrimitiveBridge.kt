import androidx.room.RoomDatabase
import androidx.room.SharedSQLiteStatement
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performReadBlocking
import androidx.sqlite.db.SupportSQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
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

  private val __preparedStmtOfInsertEntity: SharedSQLiteStatement
  init {
    this.__db = __db
    this.__preparedStmtOfInsertEntity = object : SharedSQLiteStatement(__db) {
      public override fun createQuery(): String {
        val _query: String = "INSERT INTO MyEntity (pk) VALUES (?)"
        return _query
      }
    }
  }

  public override fun insertEntity(id: Long): Long {
    __db.assertNotSuspendingTransaction()
    val _stmt: SupportSQLiteStatement = __preparedStmtOfInsertEntity.acquire()
    var _argIndex: Int = 1
    _stmt.bindLong(_argIndex, id)
    try {
      __db.beginTransaction()
      try {
        val _result: Long = _stmt.executeInsert()
        __db.setTransactionSuccessful()
        return _result
      } finally {
        __db.endTransaction()
      }
    } finally {
      __preparedStmtOfInsertEntity.release(_stmt)
    }
  }

  public override fun getEntity(id: Long): MyEntity {
    val _sql: String = "SELECT * FROM MyEntity WHERE pk = ?"
    return performReadBlocking(__db, _sql) { _stmt ->
      var _argIndex: Int = 1
      _stmt.bindLong(_argIndex, id)
      val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
      val _result: MyEntity
      if (_stmt.step()) {
        val _tmpPk: Long
        _tmpPk = _stmt.getLong(_cursorIndexOfPk)
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
