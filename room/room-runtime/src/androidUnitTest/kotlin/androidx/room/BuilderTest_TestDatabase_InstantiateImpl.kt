import androidx.room.BuilderTest
import androidx.room.BuilderTest_TestDatabase_Impl
import kotlin.reflect.KClass

internal fun KClass<BuilderTest.TestDatabase>.instantiateImpl(): BuilderTest.TestDatabase =
    BuilderTest_TestDatabase_Impl()
