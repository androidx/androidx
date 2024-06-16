import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.mutableListOf
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

  public override fun getSongsWithArtist(): Map<Song, Artist> {
    val _sql: String = "SELECT * FROM Song JOIN Artist ON Song.artistKey = Artist.artistId"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _cursorIndexOfArtistKey: Int = getColumnIndexOrThrow(_stmt, "artistKey")
        val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _result: MutableMap<Song, Artist> = LinkedHashMap<Song, Artist>()
        while (_stmt.step()) {
          val _key: Song
          val _tmpSongId: String
          _tmpSongId = _stmt.getText(_cursorIndexOfSongId)
          val _tmpArtistKey: String
          _tmpArtistKey = _stmt.getText(_cursorIndexOfArtistKey)
          _key = Song(_tmpSongId,_tmpArtistKey)
          if (_stmt.isNull(_cursorIndexOfArtistId)) {
            error("The column(s) of the map value object of type 'Artist' are NULL but the map's value type argument expect it to be NON-NULL")
          }
          val _value: Artist
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_cursorIndexOfArtistId)
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

  public override fun getArtistWithSongs(): Map<Artist, List<Song>> {
    val _sql: String = "SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _cursorIndexOfArtistKey: Int = getColumnIndexOrThrow(_stmt, "artistKey")
        val _result: MutableMap<Artist, MutableList<Song>> =
            LinkedHashMap<Artist, MutableList<Song>>()
        while (_stmt.step()) {
          val _key: Artist
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_cursorIndexOfArtistId)
          _key = Artist(_tmpArtistId)
          val _values: MutableList<Song>
          if (_result.containsKey(_key)) {
            _values = _result.getValue(_key)
          } else {
            _values = mutableListOf()
            _result.put(_key, _values)
          }
          if (_stmt.isNull(_cursorIndexOfSongId) && _stmt.isNull(_cursorIndexOfArtistKey)) {
            continue
          }
          val _value: Song
          val _tmpSongId: String
          _tmpSongId = _stmt.getText(_cursorIndexOfSongId)
          val _tmpArtistKey: String
          _tmpArtistKey = _stmt.getText(_cursorIndexOfArtistKey)
          _value = Song(_tmpSongId,_tmpArtistKey)
          _values.add(_value)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getArtistSongCount(): Map<Artist, Int> {
    val _sql: String =
        "SELECT Artist.*, COUNT(songId) as songCount FROM Artist JOIN Song ON Artist.artistId = Song.artistKey GROUP BY artistId"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _columnIndexOfSongCount: Int = getColumnIndexOrThrow(_stmt, "songCount")
        val _result: MutableMap<Artist, Int> = LinkedHashMap<Artist, Int>()
        while (_stmt.step()) {
          val _key: Artist
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_cursorIndexOfArtistId)
          _key = Artist(_tmpArtistId)
          if (_stmt.isNull(_columnIndexOfSongCount)) {
            error("The column(s) of the map value object of type 'Int' are NULL but the map's value type argument expect it to be NON-NULL")
          }
          val _value: Int
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSongCount).toInt()
          _value = _tmp
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

  public override fun getArtistWithSongIds(): Map<Artist, List<String>> {
    val _sql: String = "SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _columnIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _result: MutableMap<Artist, MutableList<String>> =
            LinkedHashMap<Artist, MutableList<String>>()
        while (_stmt.step()) {
          val _key: Artist
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_cursorIndexOfArtistId)
          _key = Artist(_tmpArtistId)
          val _values: MutableList<String>
          if (_result.containsKey(_key)) {
            _values = _result.getValue(_key)
          } else {
            _values = mutableListOf()
            _result.put(_key, _values)
          }
          if (_stmt.isNull(_columnIndexOfSongId)) {
            continue
          }
          val _value: String
          _value = _stmt.getText(_columnIndexOfSongId)
          _values.add(_value)
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
