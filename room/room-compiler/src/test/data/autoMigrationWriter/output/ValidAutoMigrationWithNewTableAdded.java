package foo.bar;

import androidx.annotation.NonNull;
import androidx.room.migration.AutoMigrationCallback;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.lang.Override;
import java.lang.SuppressWarnings;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
class ValidAutoMigrationWithNewTableAdded_Impl extends Migration implements AutoMigrationCallback {
    public ValidAutoMigrationWithNewTableAdded_Impl() {
        super(1, 2);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `Artist` (`artistId` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`artistId`))");
        database.execSQL("CREATE TABLE IF NOT EXISTS `Album` (`albumId` INTEGER NOT NULL, PRIMARY KEY(`albumId`))");
        database.execSQL("ALTER TABLE `Song` ADD COLUMN `songId` INTEGER DEFAULT NULL");
        onPostMigrate(database);
    }
}
