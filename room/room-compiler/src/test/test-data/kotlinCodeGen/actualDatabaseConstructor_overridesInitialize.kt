import androidx.room.RoomDatabaseConstructor

public actual object MyDatabaseCtor : RoomDatabaseConstructor<MyDatabase> {
    actual override fun initialize(): MyDatabase = MyDatabase_Impl()
}