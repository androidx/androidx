package androidx.room.guava;

import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Callable;

// fake GuavaRoom class for tests
public class GuavaRoom {

    @NonNull
    public static <T> ListenableFuture<T> createListenableFuture(
            final @NonNull RoomDatabase roomDatabase,
            final boolean inTransaction,
            final @NonNull Callable<T> callable,
            final @NonNull RoomSQLiteQuery query,
            final boolean releaseQuery,
            final @Nullable CancellationSignal cancellationSignal) {
        return null;
    }
}