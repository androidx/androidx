package foo.bar;

import androidx.annotation.NonNull;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.lang.Override;
import java.lang.SuppressWarnings;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
class MyDatabase_AutoMigration_1_2_Impl extends Migration {
    private final AutoMigrationSpec callback = new MyAutoMigration();

    public MyDatabase_AutoMigration_1_2_Impl() {
        super(1, 2);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `_new_SongTable` (`id` INTEGER NOT NULL, `songTitle` TEXT NOT NULL, `songLength` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        database.execSQL("INSERT INTO `_new_SongTable` (`id`,`songTitle`,`songLength`) SELECT `id`,`title`,`length` FROM `Song`");
        database.execSQL("DROP TABLE `Song`");
        database.execSQL("ALTER TABLE `_new_SongTable` RENAME TO `SongTable`");
        callback.onPostMigrate(database);
    }
}