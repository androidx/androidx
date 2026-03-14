import androidx.lifecycle.LiveData
import androidx.room3.RoomDatabase
import androidx.room3.livedata.LiveDataDaoReturnTypeConverter
import androidx.room3.util.appendPlaceholders
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performSuspending
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.prepare
import androidx.sqlite.step
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.reflect.KClass
import kotlin.text.StringBuilder

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase

  private val __liveDataDaoReturnTypeConverter: LiveDataDaoReturnTypeConverter =
      LiveDataDaoReturnTypeConverter()
  init {
    this.__db = __db
  }

  public override fun getLiveData(vararg arg: String?): LiveData<MyEntity> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return __liveDataDaoReturnTypeConverter.convert(__db, arrayOf("MyEntity")) {
      performSuspending(__db, true, false) { _connection ->
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
          val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
          val _columnIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
          val _result: MyEntity
          if (_stmt.step()) {
            val _tmpPk: Int
            _tmpPk = _stmt.getLong(_columnIndexOfPk).toInt()
            val _tmpOther: String
            _tmpOther = _stmt.getText(_columnIndexOfOther)
            _result = MyEntity(_tmpPk,_tmpOther)
          } else {
            error("The query result was empty, but expected a single row to return a NON-NULL object of type 'MyEntity'.")
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun getLiveDataNullable(vararg arg: String?): LiveData<MyEntity?> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM MyEntity WHERE pk IN (")
    val _inputSize: Int = arg.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return __liveDataDaoReturnTypeConverter.convert(__db, arrayOf("MyEntity")) {
      performSuspending(__db, true, false) { _connection ->
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
          val _columnIndexOfPk: Int = getColumnIndexOrThrow(_stmt, "pk")
          val _columnIndexOfOther: Int = getColumnIndexOrThrow(_stmt, "other")
          val _result: MyEntity?
          if (_stmt.step()) {
            val _tmpPk: Int
            _tmpPk = _stmt.getLong(_columnIndexOfPk).toInt()
            val _tmpOther: String
            _tmpOther = _stmt.getText(_columnIndexOfOther)
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
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
