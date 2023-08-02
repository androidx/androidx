import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.room.RoomSQLiteQuery
import androidx.room.RoomSQLiteQuery.Companion.acquire
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.newStringBuilder
import androidx.room.util.query
import androidx.room.util.recursiveFetchHashMap
import java.lang.Class
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.util.HashMap
import javax.`annotation`.processing.Generated
import kotlin.ByteArray
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Set
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
            val _collectionArtist: HashMap<ByteBuffer, Artist?> = HashMap<ByteBuffer, Artist?>()
            while (_cursor.moveToNext()) {
                val _tmpKey: ByteBuffer
                _tmpKey = ByteBuffer.wrap(_cursor.getBlob(_cursorIndexOfArtistKey))
                _collectionArtist.put(_tmpKey, null)
            }
            _cursor.moveToPosition(-1)
            __fetchRelationshipArtistAsArtist(_collectionArtist)
            val _result: SongWithArtist
            if (_cursor.moveToFirst()) {
                val _tmpSong: Song
                val _tmpSongId: Long
                _tmpSongId = _cursor.getLong(_cursorIndexOfSongId)
                val _tmpArtistKey: ByteArray
                _tmpArtistKey = _cursor.getBlob(_cursorIndexOfArtistKey)
                _tmpSong = Song(_tmpSongId,_tmpArtistKey)
                val _tmpArtist: Artist?
                val _tmpKey_1: ByteBuffer
                _tmpKey_1 = ByteBuffer.wrap(_cursor.getBlob(_cursorIndexOfArtistKey))
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

    private fun __fetchRelationshipArtistAsArtist(_map: HashMap<ByteBuffer, Artist?>) {
        val __mapKeySet: Set<ByteBuffer> = _map.keys
        if (__mapKeySet.isEmpty()) {
            return
        }
        if (_map.size > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
            recursiveFetchHashMap(_map, false) {
                __fetchRelationshipArtistAsArtist(it)
            }
            return
        }
        val _stringBuilder: StringBuilder = newStringBuilder()
        _stringBuilder.append("SELECT `artistId` FROM `Artist` WHERE `artistId` IN (")
        val _inputSize: Int = __mapKeySet.size
        appendPlaceholders(_stringBuilder, _inputSize)
        _stringBuilder.append(")")
        val _sql: String = _stringBuilder.toString()
        val _argCount: Int = 0 + _inputSize
        val _stmt: RoomSQLiteQuery = acquire(_sql, _argCount)
        var _argIndex: Int = 1
        for (_item: ByteBuffer in __mapKeySet) {
            _stmt.bindBlob(_argIndex, _item.array())
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
                val _tmpKey: ByteBuffer
                _tmpKey = ByteBuffer.wrap(_cursor.getBlob(_itemKeyIndex))
                if (_map.containsKey(_tmpKey)) {
                    val _item_1: Artist
                    val _tmpArtistId: ByteArray
                    _tmpArtistId = _cursor.getBlob(_cursorIndexOfArtistId)
                    _item_1 = Artist(_tmpArtistId)
                    _map.put(_tmpKey, _item_1)
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