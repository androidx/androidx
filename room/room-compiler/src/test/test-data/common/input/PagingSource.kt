package androidx.paging

import androidx.paging.PagingState

public abstract class PagingSource<K : Any, T : Any> {
    public sealed class LoadParams<K : Any> constructor()

    public sealed class LoadResult<K : Any, T : Any> {
        public class Invalid<K : Any, T : Any> : LoadResult<K, T>()
    }

    public abstract fun getRefreshKey(state: PagingState<K, T>): K?

    public abstract suspend fun load(params: LoadParams<K>): LoadResult<K, T>
}