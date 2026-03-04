import androidx.room3.RoomDatabase
import androidx.room3.rxjava3.RxDaoReturnTypeConverters
import androidx.room3.util.getLastInsertedRowId
import androidx.room3.util.performSuspending
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.prepare
import androidx.sqlite.step
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __rxDaoReturnTypeConverters: RxDaoReturnTypeConverters = RxDaoReturnTypeConverters()
  init {
    this.__db = __db
  }

  public override fun insertPublisherSingle(id: String, name: String): Single<Long> {
    val _sql: String = "INSERT INTO MyEntity (pk, other) VALUES (?, ?)"
    return __rxDaoReturnTypeConverters.convertSingle(__db) {
      performSuspending(__db, false, true) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_sql)
        try {
          var _argIndex: Int = 1
          _stmt.bindText(_argIndex, id)
          _argIndex = 2
          _stmt.bindText(_argIndex, name)
          _stmt.step()
          getLastInsertedRowId(_connection)
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun insertPublisherMaybe(id: String, name: String): Maybe<Long> {
    val _sql: String = "INSERT INTO MyEntity (pk, other) VALUES (?, ?)"
    return __rxDaoReturnTypeConverters.convertMaybe(__db) {
      performSuspending(__db, false, true) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_sql)
        try {
          var _argIndex: Int = 1
          _stmt.bindText(_argIndex, id)
          _argIndex = 2
          _stmt.bindText(_argIndex, name)
          _stmt.step()
          getLastInsertedRowId(_connection)
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun insertPublisherCompletable(id: String, name: String): Completable {
    val _sql: String = "INSERT INTO MyEntity (pk, other) VALUES (?, ?)"
    return __rxDaoReturnTypeConverters.convertCompletable(__db) {
      performSuspending(__db, false, true) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_sql)
        try {
          var _argIndex: Int = 1
          _stmt.bindText(_argIndex, id)
          _argIndex = 2
          _stmt.bindText(_argIndex, name)
          _stmt.step()
        } finally {
          _stmt.close()
        }
        kotlin.Unit
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
