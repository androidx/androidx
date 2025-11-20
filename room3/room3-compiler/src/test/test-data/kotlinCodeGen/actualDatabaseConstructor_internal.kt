import androidx.room3.RoomDatabaseConstructor

internal actual object MyDatabaseCtor : RoomDatabaseConstructor<MyDatabase> {
    override fun initialize(): MyDatabase = MyDatabase_Impl()
}