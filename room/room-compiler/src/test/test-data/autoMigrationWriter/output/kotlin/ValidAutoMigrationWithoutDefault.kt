package foo.bar

import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class MyDatabase_AutoMigration_1_2_Impl : Migration {
    private val callback: AutoMigrationSpec = ValidAutoMigrationWithoutDefault()

    public constructor() : super(1, 2)

    public override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `Song` ADD COLUMN `artistId` INTEGER DEFAULT NULL")
        callback.onPostMigrate(connection)
    }
}