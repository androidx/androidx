import androidx.room3.RoomDatabase
import androidx.room3.util.performInTransactionBlocking
import androidx.room3.util.performInTransactionSuspending
import javax.`annotation`.processing.Generated
import kotlin.Long
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class MyDao_Impl(
  __db: RoomDatabase,
) : MyDao() {
  private val __db: RoomDatabase
  init {
    this.__db = __db
  }

  public override fun baseConcrete(): Unit = performInTransactionBlocking(__db) {
    super@MyDao_Impl.baseConcrete()
  }

  public override suspend fun baseSuspendConcrete(): Unit = performInTransactionSuspending(__db) {
    super@MyDao_Impl.baseSuspendConcrete()
  }

  public override fun concrete(): Unit = performInTransactionBlocking(__db) {
    super@MyDao_Impl.concrete()
  }

  internal override fun concreteInternal(): Unit = performInTransactionBlocking(__db) {
    super@MyDao_Impl.concreteInternal()
  }

  public override suspend fun suspendConcrete(): Unit = performInTransactionSuspending(__db) {
    super@MyDao_Impl.suspendConcrete()
  }

  internal override fun concreteInternalWithReturn(): Long = performInTransactionBlocking(__db) {
    super@MyDao_Impl.concreteInternalWithReturn()
  }

  public override suspend fun suspendConcreteWithReturn(): Long = performInTransactionSuspending(__db) {
    super@MyDao_Impl.suspendConcreteWithReturn()
  }

  public override fun concreteWithVararg(vararg arr: Long): Unit = performInTransactionBlocking(__db) {
    super@MyDao_Impl.concreteWithVararg(*arr)
  }

  public override suspend fun suspendConcreteWithVararg(vararg arr: Long): Unit = performInTransactionSuspending(__db) {
    super@MyDao_Impl.suspendConcreteWithVararg(*arr)
  }

  public override fun <R> concreteWithTypeParam(): Unit = performInTransactionBlocking(__db) {
    super@MyDao_Impl.concreteWithTypeParam<R>()
  }

  public override suspend fun <R> suspendConcreteWithTypeParam(): Unit = performInTransactionSuspending(__db) {
    super@MyDao_Impl.suspendConcreteWithTypeParam<R>()
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
