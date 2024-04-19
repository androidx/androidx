package androidx.room.integration.kotlintestapp.test;

import androidx.annotation.NonNull;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.lang.Override;
import java.lang.SuppressWarnings;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
final class PreKmpDatabase_AutoMigration_1_2_Impl extends Migration {
    private final AutoMigrationSpec callback;

    public PreKmpDatabase_AutoMigration_1_2_Impl(@NonNull final AutoMigrationSpec callback) {
        super(1, 2);
        this.callback = callback;
    }

    @Override
    public void migrate(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE `DeletedEntity`");
        callback.onPostMigrate(db);
    }
}
