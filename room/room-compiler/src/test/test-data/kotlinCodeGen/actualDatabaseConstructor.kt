import androidx.room.RoomDatabaseConstructor

public actual object MyDatabaseCtor : RoomDatabaseConstructor<MyDatabase> {
    override fun initialize(): MyDatabase = MyDatabase_Impl()
}