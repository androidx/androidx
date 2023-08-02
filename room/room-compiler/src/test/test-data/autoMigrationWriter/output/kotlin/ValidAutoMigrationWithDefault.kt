package foo.bar

import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION"])
internal class MyDatabase_AutoMigration_1_2_Impl : Migration {
    private val callback: AutoMigrationSpec = ValidAutoMigrationWithDefault()

    public constructor() : super(1, 2)

    public override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `Song` ADD COLUMN `artistId` INTEGER NOT NULL DEFAULT 0")
        callback.onPostMigrate(db)
    }
}