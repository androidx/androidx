import androidx.room.RoomDatabase
import androidx.room.rxjava3.createFlowable
import androidx.room.rxjava3.createMaybe
import androidx.room.rxjava3.createObservable
import androidx.room.rxjava3.createSingle
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.sqlite.SQLiteStatement
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import javax.`annotation`.processing.Generated
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

  public override fun getFlowable(vararg arg: String?): Flowable<MyEntity> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return createFlowable(__db, false, arrayOf("MyEntity")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String? in arg) {
          if (_item == null) {
            _stmt.bindNull(_argIndex)
          } else {
            _stmt.bindText(_argIndex, _item)
          }
          _argIndex++
        }
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
        val _result: MyEntity?
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpOther: String
          _tmpOther = _stmt.getText(_cursorIndexOfOther)
          _result = MyEntity(_tmpPk,_tmpOther)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getObservable(vararg arg: String?): Observable<MyEntity> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return createObservable(__db, false, arrayOf("MyEntity")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String? in arg) {
          if (_item == null) {
            _stmt.bindNull(_argIndex)
          } else {
            _stmt.bindText(_argIndex, _item)
          }
          _argIndex++
        }
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
        val _result: MyEntity?
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpOther: String
          _tmpOther = _stmt.getText(_cursorIndexOfOther)
          _result = MyEntity(_tmpPk,_tmpOther)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getSingle(vararg arg: String?): Single<MyEntity> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return createSingle(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String? in arg) {
          if (_item == null) {
            _stmt.bindNull(_argIndex)
          } else {
            _stmt.bindText(_argIndex, _item)
          }
          _argIndex++
        }
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
        val _result: MyEntity?
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpOther: String
          _tmpOther = _stmt.getText(_cursorIndexOfOther)
          _result = MyEntity(_tmpPk,_tmpOther)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getMaybe(vararg arg: String?): Maybe<MyEntity> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return createMaybe(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String? in arg) {
          if (_item == null) {
            _stmt.bindNull(_argIndex)
          } else {
            _stmt.bindText(_argIndex, _item)
          }
          _argIndex++
        }
        val _cursorIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
        val _cursorIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
        val _result: MyEntity?
        if (_stmt.step()) {
          val _tmpPk: Int
          _tmpPk = _stmt.getLong(_cursorIndexOfPk).toInt()
          val _tmpOther: String
          _tmpOther = _stmt.getText(_cursorIndexOfOther)
          _result = MyEntity(_tmpPk,_tmpOther)
        } else {
          _result = null
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
