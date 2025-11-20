import androidx.room3.RoomDatabase
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.performBlocking
import androidx.sqlite.SQLiteStatement
import com.google.common.collect.ImmutableMap
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.MutableMap
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

  public override fun getSongsWithArtist(): ImmutableMap<Song, Artist> {
    val _sql: String = "SELECT * FROM Song JOIN Artist ON Song.artistKey = Artist.artistId"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSongId: Int = getColumnIndexOrThrow(_stmt, "songId")
        val _columnIndexOfArtistKey: Int = getColumnIndexOrThrow(_stmt, "artistKey")
        val _columnIndexOfArtistId: Int = getColumnIndexOrThrow(_stmt, "artistId")
        val _mapResult: MutableMap<Song, Artist> = LinkedHashMap<Song, Artist>()
        while (_stmt.step()) {
          val _key: Song
          val _tmpSongId: String
          _tmpSongId = _stmt.getText(_columnIndexOfSongId)
          val _tmpArtistKey: String
          _tmpArtistKey = _stmt.getText(_columnIndexOfArtistKey)
          _key = Song(_tmpSongId,_tmpArtistKey)
          if (_stmt.isNull(_columnIndexOfArtistId)) {
            error("The column(s) of the map value object of type 'Artist' are NULL but the map's value type argument expect it to be NON-NULL")
          }
          val _value: Artist
          val _tmpArtistId: String
          _tmpArtistId = _stmt.getText(_columnIndexOfArtistId)
          _value = Artist(_tmpArtistId)
          if (!_mapResult.containsKey(_key)) {
            _mapResult.put(_key, _value)
          }
        }
        val _result: ImmutableMap<Song, Artist> = ImmutableMap.copyOf(_mapResult)
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
