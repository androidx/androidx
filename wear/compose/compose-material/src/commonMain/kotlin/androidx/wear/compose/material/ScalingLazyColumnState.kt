package androidx.wear.compose.material

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Creates a [ScalingLazyColumnState] that is remembered across compositions.
 */
@Composable
public fun rememberScalingLazyColumnState(): ScalingLazyColumnState {
    return rememberSaveable(saver = ScalingLazyColumnState.Saver) {
        ScalingLazyColumnState()
    }
}

/**
 * A state object that can be hoisted to control and observe scrolling.
 * TODO (b/193792848): Add scrolling and snap support.
 *
 * In most cases, this will be created via [rememberScalingLazyColumnState].
 */
@Stable
public class ScalingLazyColumnState {

    internal var lazyListState: LazyListState = LazyListState(0, 0)

    companion object {
        /**
         * The default [Saver] implementation for [ScalingLazyColumnState].
         */
        val Saver: Saver<ScalingLazyColumnState, *> = listSaver(
            save = {
                listOf(
                    it.lazyListState.firstVisibleItemIndex,
                    it.lazyListState.firstVisibleItemScrollOffset
                )
            },
            restore = {
                var scalingLazyColumnState = ScalingLazyColumnState()
                scalingLazyColumnState.lazyListState = LazyListState(
                    firstVisibleItemIndex = it[0],
                    firstVisibleItemScrollOffset = it[1]
                )
                scalingLazyColumnState
            }
        )
    }
}
