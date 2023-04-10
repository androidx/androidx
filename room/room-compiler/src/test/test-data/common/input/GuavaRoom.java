package androidx.room.guava;

import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

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

    @NonNull
    private static <T> ListenableFuture<T> createListenableFuture(
            final Executor executor,
            final Callable<T> callable,
            final RoomSQLiteQuery query,
            final boolean releaseQuery,
            final @Nullable CancellationSignal cancellationSignal) {
        return null;
    }

    @NonNull
    public static <T> ListenableFuture<T> createListenableFuture(
            final @NonNull RoomDatabase roomDatabase,
            final @NonNull Callable<T> callable) {
        return createListenableFuture(roomDatabase, false, callable);
    }

    @NonNull
    public static <T> ListenableFuture<T> createListenableFuture(
            final @NonNull RoomDatabase roomDatabase,
            final boolean inTransaction,
            final @NonNull Callable<T> callable) {
        return createListenableFuture(getExecutor(roomDatabase, inTransaction), callable);
    }

    @NonNull
    private static <T> ListenableFuture<T> createListenableFuture(
            final @NonNull Executor executor,
            final @NonNull Callable<T> callable) {
        return null;
    }

    private static Executor getExecutor(RoomDatabase database, boolean inTransaction) {
        return null;
    }
}