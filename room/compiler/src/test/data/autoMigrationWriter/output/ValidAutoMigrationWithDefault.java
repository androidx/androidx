package foo.bar;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.lang.Override;
import java.lang.SuppressWarnings;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
class ValidAutoMigrationWithDefault_Impl extends Migration {
    public ValidAutoMigrationWithDefault_Impl() {
        super(1, 2);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE 'Song' ADD COLUMN `artistId` INTEGER NOT NULL DEFAULT '0'");
    }
}
