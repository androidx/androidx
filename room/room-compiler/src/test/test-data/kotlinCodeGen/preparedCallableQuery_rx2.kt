import androidx.room.RoomDatabase
import androidx.room.SharedSQLiteStatement
import androidx.sqlite.db.SupportSQLiteStatement
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.lang.Class
import java.lang.Void
import java.util.concurrent.Callable
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

    private val __preparedStmtOfInsertPublisherSingle: SharedSQLiteStatement
    init {
        this.__db = __db
        this.__preparedStmtOfInsertPublisherSingle = object : SharedSQLiteStatement(__db) {
            public override fun createQuery(): String {
                val _query: String = "INSERT INTO MyEntity (pk, other) VALUES (?, ?)"
                return _query
            }
        }
    }

    public override fun insertPublisherSingle(id: String, name: String): Single<Long> =
        Single.fromCallable(object : Callable<Long?> {
            public override fun call(): Long? {
                val _stmt: SupportSQLiteStatement = __preparedStmtOfInsertPublisherSingle.acquire()
                var _argIndex: Int = 1
                _stmt.bindString(_argIndex, id)
                _argIndex = 2
                _stmt.bindString(_argIndex, name)
                try {
                    __db.beginTransaction()
                    try {
                        val _result: Long? = _stmt.executeInsert()
                        __db.setTransactionSuccessful()
                        return _result
                    } finally {
                        __db.endTransaction()
                    }
                } finally {
                    __preparedStmtOfInsertPublisherSingle.release(_stmt)
                }
            }
        })

    public override fun insertPublisherMaybe(id: String, name: String): Maybe<Long> =
        Maybe.fromCallable(object : Callable<Long?> {
            public override fun call(): Long? {
                val _stmt: SupportSQLiteStatement = __preparedStmtOfInsertPublisherSingle.acquire()
                var _argIndex: Int = 1
                _stmt.bindString(_argIndex, id)
                _argIndex = 2
                _stmt.bindString(_argIndex, name)
                try {
                    __db.beginTransaction()
                    try {
                        val _result: Long? = _stmt.executeInsert()
                        __db.setTransactionSuccessful()
                        return _result
                    } finally {
                        __db.endTransaction()
                    }
                } finally {
                    __preparedStmtOfInsertPublisherSingle.release(_stmt)
                }
            }
        })

    public override fun insertPublisherCompletable(id: String, name: String): Completable =
        Completable.fromCallable(object : Callable<Void?> {
            public override fun call(): Void? {
                val _stmt: SupportSQLiteStatement = __preparedStmtOfInsertPublisherSingle.acquire()
                var _argIndex: Int = 1
                _stmt.bindString(_argIndex, id)
                _argIndex = 2
                _stmt.bindString(_argIndex, name)
                try {
                    __db.beginTransaction()
                    try {
                        _stmt.executeInsert()
                        __db.setTransactionSuccessful()
                        return null
                    } finally {
                        __db.endTransaction()
                    }
                } finally {
                    __preparedStmtOfInsertPublisherSingle.release(_stmt)
                }
            }
        })

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}