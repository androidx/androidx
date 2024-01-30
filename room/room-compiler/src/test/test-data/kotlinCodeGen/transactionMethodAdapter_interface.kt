import androidx.room.RoomDatabase
import androidx.room.withTransaction
import java.lang.Class
import javax.`annotation`.processing.Generated
import kotlin.Function0
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.coroutines.SuspendFunction0
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

    public override fun baseConcrete() {
        __db.beginTransaction()
        try {
            super@MyDao_Impl.baseConcrete()
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override suspend fun baseSuspendConcrete() {
        __db.withTransaction {
            super@MyDao_Impl.baseSuspendConcrete()
        }
    }

    public override fun concrete() {
        __db.beginTransaction()
        try {
            super@MyDao_Impl.concrete()
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override fun concreteWithReturn(): String {
        __db.beginTransaction()
        try {
            val _result: String
            _result = super@MyDao_Impl.concreteWithReturn()
            __db.setTransactionSuccessful()
            return _result
        } finally {
            __db.endTransaction()
        }
    }

    public override fun concreteWithParamsAndReturn(text: String, num: Long): String {
        __db.beginTransaction()
        try {
            val _result: String
            _result = super@MyDao_Impl.concreteWithParamsAndReturn(text, num)
            __db.setTransactionSuccessful()
            return _result
        } finally {
            __db.endTransaction()
        }
    }

    public override fun concreteWithFunctionalParam(block: Function0<Unit>) {
        __db.beginTransaction()
        try {
            super@MyDao_Impl.concreteWithFunctionalParam(block)
            __db.setTransactionSuccessful()
        } finally {
            __db.endTransaction()
        }
    }

    public override suspend fun suspendConcrete() {
        __db.withTransaction {
            super@MyDao_Impl.suspendConcrete()
        }
    }

    public override suspend fun suspendConcreteWithReturn(): String = __db.withTransaction {
        super@MyDao_Impl.suspendConcreteWithReturn()
    }

    public override suspend
    fun suspendConcreteWithSuspendFunctionalParam(block: SuspendFunction0<Unit>) {
        __db.withTransaction {
            super@MyDao_Impl.suspendConcreteWithSuspendFunctionalParam(block)
        }
    }

    public companion object {
        @JvmStatic
        public fun getRequiredConverters(): List<Class<*>> = emptyList()
    }
}