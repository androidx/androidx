import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.lang.Void
import java.util.concurrent.Callable
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
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

  public override fun insertPublisherSingle(id: String, name: String): Single<Long> =
      Single.fromCallable(object : Callable<Long?> {
    public override fun call(): Long? {
      val _sql: String = "INSERT INTO MyEntity (pk, other) VALUES (?, ?)"
      val _stmt: SupportSQLiteStatement = __db.compileStatement(_sql)
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, id)
      _argIndex = 2
      _stmt.bindString(_argIndex, name)
      __db.beginTransaction()
      try {
        val _result: Long? = _stmt.executeInsert()
        __db.setTransactionSuccessful()
        return _result
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun insertPublisherMaybe(id: String, name: String): Maybe<Long> =
      Maybe.fromCallable(object : Callable<Long?> {
    public override fun call(): Long? {
      val _sql: String = "INSERT INTO MyEntity (pk, other) VALUES (?, ?)"
      val _stmt: SupportSQLiteStatement = __db.compileStatement(_sql)
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, id)
      _argIndex = 2
      _stmt.bindString(_argIndex, name)
      __db.beginTransaction()
      try {
        val _result: Long? = _stmt.executeInsert()
        __db.setTransactionSuccessful()
        return _result
      } finally {
        __db.endTransaction()
      }
    }
  })

  public override fun insertPublisherCompletable(id: String, name: String): Completable =
      Completable.fromCallable(object : Callable<Void?> {
    public override fun call(): Void? {
      val _sql: String = "INSERT INTO MyEntity (pk, other) VALUES (?, ?)"
      val _stmt: SupportSQLiteStatement = __db.compileStatement(_sql)
      var _argIndex: Int = 1
      _stmt.bindString(_argIndex, id)
      _argIndex = 2
      _stmt.bindString(_argIndex, name)
      __db.beginTransaction()
      try {
        _stmt.executeInsert()
        __db.setTransactionSuccessful()
        return null
      } finally {
        __db.endTransaction()
      }
    }
  })

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
