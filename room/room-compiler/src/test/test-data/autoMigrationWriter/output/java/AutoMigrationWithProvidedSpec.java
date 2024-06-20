package foo.bar;

import androidx.annotation.NonNull;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.sqlite.SQLite;
import androidx.sqlite.SQLiteConnection;
import java.lang.Override;
import java.lang.SuppressWarnings;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
final class MyDatabase_AutoMigration_1_2_Impl extends Migration {
    private final AutoMigrationSpec callback;

    public MyDatabase_AutoMigration_1_2_Impl(@NonNull final AutoMigrationSpec callback) {
        super(1, 2);
        this.callback = callback;
    }

    @Override
    public void migrate(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "ALTER TABLE `Song` ADD COLUMN `artistId` INTEGER DEFAULT NULL");
        callback.onPostMigrate(connection);
    }
}