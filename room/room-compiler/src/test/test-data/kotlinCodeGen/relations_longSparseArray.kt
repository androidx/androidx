import android.database.Cursor
import androidx.collection.LongSparseArray
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.newStringBuilder
import androidx.room.util.query
import androidx.room.util.recursiveFetchLongSparseArray
import java.lang.Class
import java.lang.StringBuilder
import java.util.ArrayList
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.jvm.JvmStatic

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
public class MyDao_Impl(
    __db: RoomDatabase,
) : MyDao {
    private val __db: RoomDatabase
    init {
        this.__db = __db
    }

    public override fun getSongsWithArtist(): SongWithArtist {
        val _sql: String = "SELECT * FROM Song"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, true, null)
        try {
            val _cursorIndexOfSongId: Int = getColumnIndexOrThrow(_cursor, "songId")
            val _cursorIndexOfArtistKey: Int = getColumnIndexOrThrow(_cursor, "artistKey")
            val _collectionArtist: LongSparseArray<Artist?> = LongSparseArray<Artist?>()
            while (_cursor.moveToNext()) {
                val _tmpKey: Long
                _tmpKey = _cursor.getLong(_cursorIndexOfArtistKey)
                _collectionArtist.put(_tmpKey, null)
            }
            _cursor.moveToPosition(-1)
            __fetchRelationshipArtistAsArtist(_collectionArtist)
            val _result: SongWithArtist
            if (_cursor.moveToFirst()) {
                val _tmpSong: Song
                val _tmpSongId: Long
                _tmpSongId = _cursor.getLong(_cursorIndexOfSongId)
                val _tmpArtistKey: Long
                _tmpArtistKey = _cursor.getLong(_cursorIndexOfArtistKey)
                _tmpSong = Song(_tmpSongId,_tmpArtistKey)
                val _tmpArtist: Artist?
                val _tmpKey_1: Long
                _tmpKey_1 = _cursor.getLong(_cursorIndexOfArtistKey)
                _tmpArtist = _collectionArtist.get(_tmpKey_1)
                if (_tmpArtist == null) {
                    error("Relationship item 'artist' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'artistKey' and entityColumn named 'artistId'.")
                }
                _result = SongWithArtist(_tmpSong,_tmpArtist)
            } else {
                error("The query result was empty, but expected a single row to return a NON-NULL object of type <SongWithArtist>.")
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun getArtistAndSongs(): ArtistAndSongs {
        val _sql: String = "SELECT * FROM Artist"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, true, null)
        try {
            val _cursorIndexOfArtistId: Int = getColumnIndexOrThrow(_cursor, "artistId")
            val _collectionSongs: LongSparseArray<ArrayList<Song>> = LongSparseArray<ArrayList<Song>>()
            while (_cursor.moveToNext()) {
                val _tmpKey: Long
                _tmpKey = _cursor.getLong(_cursorIndexOfArtistId)
                if (!_collectionSongs.containsKey(_tmpKey)) {
                    _collectionSongs.put(_tmpKey, ArrayList<Song>())
                }
            }
            _cursor.moveToPosition(-1)
            __fetchRelationshipSongAsSong(_collectionSongs)
            val _result: ArtistAndSongs
            if (_cursor.moveToFirst()) {
                val _tmpArtist: Artist
                val _tmpArtistId: Long
                _tmpArtistId = _cursor.getLong(_cursorIndexOfArtistId)
                _tmpArtist = Artist(_tmpArtistId)
                val _tmpSongsCollection: ArrayList<Song>
                val _tmpKey_1: Long
                _tmpKey_1 = _cursor.getLong(_cursorIndexOfArtistId)
                _tmpSongsCollection = checkNotNull(_collectionSongs.get(_tmpKey_1))
                _result = ArtistAndSongs(_tmpArtist,_tmpSongsCollection)
            } else {
                error("The query result was empty, but expected a single row to return a NON-NULL object of type <ArtistAndSongs>.")
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    public override fun getPlaylistAndSongs(): PlaylistAndSongs {
        val _sql: String = "SELECT * FROM Playlist"
        val _statement: RoomSQLiteQuery = acquire(_sql, 0)
        __db.assertNotSuspendingTransaction()
        val _cursor: Cursor = query(__db, _statement, true, null)
        try {
            val _cursorIndexOfPlaylistId: Int = getColumnIndexOrThrow(_cursor, "playlistId")
            val _collectionSongs: LongSparseArray<ArrayList<Song>> = LongSparseArray<ArrayList<Song>>()
            while (_cursor.moveToNext()) {
                val _tmpKey: Long
                _tmpKey = _cursor.getLong(_cursorIndexOfPlaylistId)
                if (!_collectionSongs.containsKey(_tmpKey)) {
                    _collectionSongs.put(_tmpKey, ArrayList<Song>())
                }
            }
            _cursor.moveToPosition(-1)
            __fetchRelationshipSongAsSong_1(_collectionSongs)
            val _result: PlaylistAndSongs
            if (_cursor.moveToFirst()) {
                val _tmpPlaylist: Playlist
                val _tmpPlaylistId: Long
                _tmpPlaylistId = _cursor.getLong(_cursorIndexOfPlaylistId)
                _tmpPlaylist = Playlist(_tmpPlaylistId)
                val _tmpSongsCollection: ArrayList<Song>
                val _tmpKey_1: Long
                _tmpKey_1 = _cursor.getLong(_cursorIndexOfPlaylistId)
                _tmpSongsCollection = checkNotNull(_collectionSongs.get(_tmpKey_1))
                _result = PlaylistAndSongs(_tmpPlaylist,_tmpSongsCollection)
            } else {
                error("The query result was empty, but expected a single row to return a NON-NULL object of type <PlaylistAndSongs>.")
            }
            return _result
        } finally {
            _cursor.close()
            _statement.release()
        }
    }

    private fun __fetchRelationshipArtistAsArtist(_map: LongSparseArray<Artist?>) {
        if (_map.isEmpty()) {
            return
        }
        if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
            recursiveFetchLongSparseArray(_map, false) {
                __fetchRelationshipArtistAsArtist(it)
            }
            return
        }
        val _stringBuilder: StringBuilder = newStringBuilder()
        _stringBuilder.append("SELECT `artistId` FROM `Artist` WHERE `artistId` IN (")
        val _inputSize: Int = _map.size()
        appendPlaceholders(_stringBuilder, _inputSize)
        _stringBuilder.append(")")
        val _sql: String = _stringBuilder.toString()
        val _argCount: Int = 0 + _inputSize
        val _stmt: RoomSQLiteQuery = acquire(_sql, _argCount)
        var _argIndex: Int = 1
        for (i in 0 until _map.size()) {
            val _item: Long = _map.keyAt(i)
            _stmt.bindLong(_argIndex, _item)
            _argIndex++
        }
        val _cursor: Cursor = query(__db, _stmt, false, null)
        try {
            val _itemKeyIndex: Int = getColumnIndex(_cursor, "artistId")
            if (_itemKeyIndex == -1) {
                return
            }
            val _cursorIndexOfArtistId: Int = 0
            while (_cursor.moveToNext()) {
                val _tmpKey: Long
                _tmpKey = _cursor.getLong(_itemKeyIndex)
                if (_map.containsKey(_tmpKey)) {
                    val _item_1: Artist
                    val _tmpArtistId: Long
                    _tmpArtistId = _cursor.getLong(_cursorIndexOfArtistId)
                    _item_1 = Artist(_tmpArtistId)
                    _map.put(_tmpKey, _item_1)
                }
            }
        } finally {
            _cursor.close()
        }
    }

    private fun __fetchRelationshipSongAsSong(_map: LongSparseArray<ArrayList<Song>>) {
        if (_map.isEmpty()) {
            return
        }
        if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
            recursiveFetchLongSparseArray(_map, true) {
                __fetchRelationshipSongAsSong(it)
            }
            return
        }
        val _stringBuilder: StringBuilder = newStringBuilder()
        _stringBuilder.append("SELECT `songId`,`artistKey` FROM `Song` WHERE `artistKey` IN (")
        val _inputSize: Int = _map.size()
        appendPlaceholders(_stringBuilder, _inputSize)
        _stringBuilder.append(")")
        val _sql: String = _stringBuilder.toString()
        val _argCount: Int = 0 + _inputSize
        val _stmt: RoomSQLiteQuery = acquire(_sql, _argCount)
        var _argIndex: Int = 1
        for (i in 0 until _map.size()) {
            val _item: Long = _map.keyAt(i)
            _stmt.bindLong(_argIndex, _item)
            _argIndex++
        }
        val _cursor: Cursor = query(__db, _stmt, false, null)
        try {
            val _itemKeyIndex: Int = getColumnIndex(_cursor, "artistKey")
            if (_itemKeyIndex == -1) {
                return
            }
            val _cursorIndexOfSongId: Int = 0
            val _cursorIndexOfArtistKey: Int = 1
            while (_cursor.moveToNext()) {
                val _tmpKey: Long
                _tmpKey = _cursor.getLong(_itemKeyIndex)
                val _tmpRelation: ArrayList<Song>? = _map.get(_tmpKey)
                if (_tmpRelation != null) {
                    val _item_1: Song
                    val _tmpSongId: Long
                    _tmpSongId = _cursor.getLong(_cursorIndexOfSongId)
                    val _tmpArtistKey: Long
                    _tmpArtistKey = _cursor.getLong(_cursorIndexOfArtistKey)
                    _item_1 = Song(_tmpSongId,_tmpArtistKey)
                    _tmpRelation.add(_item_1)
                }
            }
        } finally {
            _cursor.close()
        }
    }

    private fun __fetchRelationshipSongAsSong_1(_map: LongSparseArray<ArrayList<Song>>) {
        if (_map.isEmpty()) {
            return
        }
        if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
            recursiveFetchLongSparseArray(_map, true) {
                __fetchRelationshipSongAsSong_1(it)
            }
            return
        }
        val _stringBuilder: StringBuilder = newStringBuilder()
        _stringBuilder.append("SELECT `Song`.`songId` AS `songId`,`Song`.`artistKey` AS `artistKey`,_junction.`playlistKey` FROM `PlaylistSongXRef` AS _junction INNER JOIN `Song` ON (_junction.`songKey` = `Song`.`songId`) WHERE _junction.`playlistKey` IN (")
        val _inputSize: Int = _map.size()
        appendPlaceholders(_stringBuilder, _inputSize)
        _stringBuilder.append(")")
        val _sql: String = _stringBuilder.toString()
        val _argCount: Int = 0 + _inputSize
        val _stmt: RoomSQLiteQuery = acquire(_sql, _argCount)
        var _argIndex: Int = 1
        for (i in 0 until _map.size()) {
            val _item: Long = _map.keyAt(i)
            _stmt.bindLong(_argIndex, _item)
            _argIndex++
        }
        val _cursor: Cursor = query(__db, _stmt, false, null)
        try {
            // _junction.playlistKey
            val _itemKeyIndex: Int = 2
            if (_itemKeyIndex == -1) {
                return
            }
            val _cursorIndexOfSongId: Int = 0
            val _cursorIndexOfArtistKey: Int = 1
            while (_cursor.moveToNext()) {
                val _tmpKey: Long
                _tmpKey = _cursor.getLong(_itemKeyIndex)
                val _tmpRelation: ArrayList<Song>? = _map.get(_tmpKey)
                if (_tmpRelation != null) {
                    val _item_1: Song
                    val _tmpSongId: Long
                    _tmpSongId = _cursor.getLong(_cursorIndexOfSongId)
                    val _tmpArtistKey: Long
                    _tmpArtistKey = _cursor.getLong(_cursorIndexOfArtistKey)
                    _item_1 = Song(_tmpSongId,_tmpArtistKey)
                    _tmpRelation.add(_item_1)
                }
            }
        } finally {
            _cursor.close()
        }
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}