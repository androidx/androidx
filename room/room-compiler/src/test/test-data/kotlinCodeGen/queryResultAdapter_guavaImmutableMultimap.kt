import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableSetMultimap
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
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

  public override fun getArtistWithSongs(): ImmutableSetMultimap<Artist, Song> {
    val _sql: String = "SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _cursorIndexOfArtistKey: Int = getColumnIndexOrThrow(_stmt, "artistKey")
        val _mapBuilder: ImmutableSetMultimap.Builder<Artist, Song> = ImmutableSetMultimap.builder()
        while (_stmt.step()) {
          val _key: Artist
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_cursorIndexOfArtistId)
          _key = Artist(_tmpArtistId)
          if (_stmt.isNull(_cursorIndexOfSongId) && _stmt.isNull(_cursorIndexOfArtistKey)) {
            continue
          }
          val _value: Song
          val _tmpSongId: String
          _tmpSongId = _stmt.getText(_cursorIndexOfSongId)
          val _tmpArtistKey: String
          _tmpArtistKey = _stmt.getText(_cursorIndexOfArtistKey)
          _value = Song(_tmpSongId,_tmpArtistKey)
          _mapBuilder.put(_key, _value)
        }
        val _result: ImmutableSetMultimap<Artist, Song> = _mapBuilder.build()
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getArtistWithSongIds(): ImmutableListMultimap<Artist, Song> {
    val _sql: String = "SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _cursorIndexOfArtistKey: Int = getColumnIndexOrThrow(_stmt, "artistKey")
        val _mapBuilder: ImmutableListMultimap.Builder<Artist, Song> =
            ImmutableListMultimap.builder()
        while (_stmt.step()) {
          val _key: Artist
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_cursorIndexOfArtistId)
          _key = Artist(_tmpArtistId)
          if (_stmt.isNull(_cursorIndexOfSongId) && _stmt.isNull(_cursorIndexOfArtistKey)) {
            continue
          }
          val _value: Song
          val _tmpSongId: String
          _tmpSongId = _stmt.getText(_cursorIndexOfSongId)
          val _tmpArtistKey: String
          _tmpArtistKey = _stmt.getText(_cursorIndexOfArtistKey)
          _value = Song(_tmpSongId,_tmpArtistKey)
          _mapBuilder.put(_key, _value)
        }
        val _result: ImmutableListMultimap<Artist, Song> = _mapBuilder.build()
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
