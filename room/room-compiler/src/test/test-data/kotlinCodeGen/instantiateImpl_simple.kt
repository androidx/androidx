import kotlin.reflect.KClass

internal fun KClass<MyDatabase>.instantiateImpl(): MyDatabase = MyDatabase_Impl()