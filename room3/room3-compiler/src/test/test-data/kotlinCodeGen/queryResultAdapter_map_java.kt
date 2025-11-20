import androidx.room3.RoomDatabase
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun getSongsWithArtist(): MutableMap<Song?, Artist?>? {
    val _sql: String = "SELECT * FROM Song JOIN Artist ON Song.artistKey = Artist.artistId"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _columnIndexOfArtistKey: Int = getColumnIndexOrThrow(_stmt, "artistKey")
        val _columnIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _result: MutableMap<Song?, Artist?> = LinkedHashMap<Song?, Artist?>()
        while (_stmt.step()) {
          val _key: Song?
          val _tmpSongId: String
          _tmpSongId = _stmt.getText(_columnIndexOfSongId)
          val _tmpArtistKey: String
          _tmpArtistKey = _stmt.getText(_columnIndexOfArtistKey)
          _key = Song(_tmpSongId,_tmpArtistKey)
          if (_stmt.isNull(_columnIndexOfArtistId)) {
            _result.put(_key, null)
            continue
          }
          val _value: Artist?
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_columnIndexOfArtistId)
          _value = Artist(_tmpArtistId)
          if (!_result.containsKey(_key)) {
            _result.put(_key, _value)
          }
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getArtistWithSongs(): MutableMap<Artist?, MutableList<Song?>?>? {
    val _sql: String = "SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _columnIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _columnIndexOfArtistKey: Int = getColumnIndexOrThrow(_stmt, "artistKey")
        val _result: MutableMap<Artist?, MutableList<Song?>?> = LinkedHashMap<Artist?, MutableList<Song?>?>()
        while (_stmt.step()) {
          val _key: Artist?
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_columnIndexOfArtistId)
          _key = Artist(_tmpArtistId)
          val _values: MutableList<Song?>?
          if (_result.containsKey(_key)) {
            _values = _result.getValue(_key)
          } else {
            _values = mutableListOf()
            _result.put(_key, _values)
          }
          if (_stmt.isNull(_columnIndexOfSongId) && _stmt.isNull(_columnIndexOfArtistKey)) {
            continue
          }
          val _value: Song?
          val _tmpSongId: String
          _tmpSongId = _stmt.getText(_columnIndexOfSongId)
          val _tmpArtistKey: String
          _tmpArtistKey = _stmt.getText(_columnIndexOfArtistKey)
          _value = Song(_tmpSongId,_tmpArtistKey)
          _values?.add(_value)
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
