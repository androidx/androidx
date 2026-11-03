/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawn
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.EventListener
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlin.math.roundToInt

/**
 * Approximates the image search UI that regressed memory with shared element transitions enabled
 * (b/502677590): a staggered grid of image results and an image viewer pager. Each viewer page
 * shows the image, details, and a list of related results. Grid items, viewer pages, and related
 * items are connected with [SharedTransitionScope.sharedBounds].
 *
 * [SharedElementsEnabled] only toggles [SharedTransitionScope.SharedContentConfig.isEnabled], so
 * the same [SharedTransitionLayout] and modifiers are composed in both configurations.
 *
 * Images are loaded with Coil from [ImageBaseUrl] (a mock server started by the benchmark). Without
 * it, items show placeholder colors only.
 *
 * The content description of the content view reports the UI state for the benchmark: the visible
 * screen ([GridState] or [ViewerStatePrefix] + viewer depth) once no transition is running and all
 * requested images are loaded, [BusyState] before that, and [ImageLoadFailedState] if an image
 * failed to load.
 */
class SharedElementTransitionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedElementsEnabled = intent.getBooleanExtra(SharedElementsEnabled, true)
        val imageUrls = ImageUrls(intent.getStringExtra(ImageBaseUrl))
        val loadTracker = ImageLoadTracker()
        // Without a disk cache, every activity loads its images from the server.
        val imageLoader =
            ImageLoader.Builder(this).diskCache(null).eventListener(loadTracker).build()
        val contentView = findViewById<View>(android.R.id.content)

        setContent {
            CompositionLocalProvider(
                LocalImageLoader provides imageLoader,
                LocalImageUrls provides imageUrls,
            ) {
                MaterialTheme {
                    Surface(Modifier.fillMaxSize()) {
                        SharedElementTransitionApp(
                            sharedElementsEnabled = sharedElementsEnabled,
                            loadTracker = loadTracker,
                            contentView = contentView,
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val SharedElementsEnabled = "SHARED_ELEMENTS_ENABLED"
        const val ImageBaseUrl = "IMAGE_BASE_URL"
    }
}

@Composable
private fun SharedElementTransitionApp(
    sharedElementsEnabled: Boolean,
    loadTracker: ImageLoadTracker,
    contentView: View,
) {
    val config = remember(sharedElementsEnabled) { SharedElementsConfig(sharedElementsEnabled) }
    var viewerStack by remember { mutableStateOf(emptyList<ViewerInstance>()) }
    val openViewer = { results: ResultList, index: Int ->
        viewerStack = viewerStack + ViewerInstance(results, index, depth = viewerStack.size + 1)
    }

    BackHandler(enabled = viewerStack.isNotEmpty()) { viewerStack = viewerStack.dropLast(1) }

    SharedTransitionLayout(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        val transition = updateTransition(viewerStack.lastOrNull(), label = "viewer")
        // Read in composition, so that this scope recomposes and runs the SideEffect when the state
        // changes.
        val appState =
            remember(transition, loadTracker) {
                    derivedStateOf {
                        when {
                            loadTracker.failedCount > 0 -> ImageLoadFailedState
                            transition.isRunning ||
                                transition.currentState != transition.targetState ||
                                loadTracker.isLoading -> BusyState
                            else ->
                                transition.currentState?.let { "$ViewerStatePrefix${it.depth}" }
                                    ?: GridState
                        }
                    }
                }
                .value
        SideEffect {
            if (contentView.contentDescription != appState) {
                contentView.contentDescription = appState
            }
        }
        // Fully drawn once the grid is shown with all of its images loaded.
        if (appState == GridState) {
            ReportDrawn()
        }

        transition.AnimatedContent(
            // Matches the image search viewer stack.
            transitionSpec = {
                fadeIn(tween(TransitionDurationMillis)) togetherWith
                    fadeOut(tween(TransitionDurationMillis))
            }
        ) { viewer ->
            if (viewer == null) {
                ResultsGrid(config, this, onItemClick = openViewer)
            } else {
                ImageViewer(viewer, config, this, onRelatedClick = openViewer)
            }
        }
    }
}

@Composable
private fun SharedTransitionScope.ResultsGrid(
    config: SharedTransitionScope.SharedContentConfig,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onItemClick: (ResultList, Int) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().semantics { contentDescription = ResultsGridDescription },
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        items(GridResults.size, key = { GridResults.idAt(it) }) { index ->
            ResultCard(
                itemId = GridResults.idAt(index),
                config = config,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier.semantics { contentDescription = "item-$index" },
                onClick = { onItemClick(GridResults, index) },
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.ResultCard(
    itemId: Long,
    config: SharedTransitionScope.SharedContentConfig,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val item = remember(itemId) { ResultItem(itemId) }
    Column(
        modifier
            .sharedBounds(
                rememberSharedContentState(SharedKey(itemId, ElementType.Bounds), config),
                animatedVisibilityScope = animatedVisibilityScope,
                // Intentional: matches the resize mode of the image search grid item.
                resizeMode = RemeasureToBounds,
            )
            // Like the image search grid item, only the image is clipped. Clipping the card would
            // not apply to the shared image and publisher, which render in the overlay.
            .background(MaterialTheme.colorScheme.surfaceVariant, CardShape)
            .clickable(onClick = onClick)
    ) {
        ItemImage(
            item = item,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.fillMaxWidth()
                    .aspectRatio(item.aspectRatio)
                    .sharedBounds(
                        rememberSharedContentState(SharedKey(itemId, ElementType.Image), config),
                        animatedVisibilityScope = animatedVisibilityScope,
                        resizeMode = RemeasureToBounds,
                    )
                    .clip(CardShape),
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp, top = 6.dp, end = 8.dp),
        )
        if (item.price != null) {
            Price(
                item,
                config,
                animatedVisibilityScope,
                Modifier.padding(start = 8.dp, top = 2.dp, end = 8.dp),
            )
        }
        Publisher(
            item,
            config,
            animatedVisibilityScope,
            resizeMode = RemeasureToBounds,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp, end = 8.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun SharedTransitionScope.Publisher(
    item: ResultItem,
    config: SharedTransitionScope.SharedContentConfig,
    animatedVisibilityScope: AnimatedVisibilityScope,
    resizeMode: SharedTransitionScope.ResizeMode,
    modifier: Modifier,
) {
    Row(
        modifier.sharedBounds(
            rememberSharedContentState(SharedKey(item.id, ElementType.Publisher), config),
            animatedVisibilityScope = animatedVisibilityScope,
            resizeMode = resizeMode,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PlaceholderColors[item.imageId % PlaceholderColors.size])
        )
        Text(
            text = item.publisher,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun SharedTransitionScope.Price(
    item: ResultItem,
    config: SharedTransitionScope.SharedContentConfig,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier,
) {
    Text(
        text = item.price.orEmpty(),
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            modifier.sharedBounds(
                rememberSharedContentState(SharedKey(item.id, ElementType.Shopping), config),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = scaleToBounds(),
            ),
    )
}

@Composable
private fun SharedTransitionScope.ImageViewer(
    viewer: ViewerInstance,
    config: SharedTransitionScope.SharedContentConfig,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onRelatedClick: (ResultList, Int) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = viewer.initialPage) { viewer.results.size }
    HorizontalPager(
        state = pagerState,
        modifier =
            Modifier.fillMaxSize().semantics {
                contentDescription = "$ViewerDescriptionPrefix${viewer.depth}"
            },
        key = { viewer.results.idAt(it) },
    ) { page ->
        ViewerPage(viewer.results.idAt(page), config, animatedVisibilityScope, onRelatedClick)
    }
}

@Composable
private fun SharedTransitionScope.ViewerPage(
    itemId: Long,
    config: SharedTransitionScope.SharedContentConfig,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onRelatedClick: (ResultList, Int) -> Unit,
) {
    val item = remember(itemId) { ResultItem(itemId) }
    val related = remember(itemId) { ResultList(parentId = itemId, size = RelatedItemCount) }
    LazyColumn(
        modifier =
            Modifier.fillMaxSize()
                .sharedBounds(
                    rememberSharedContentState(SharedKey(itemId, ElementType.Bounds), config),
                    animatedVisibilityScope = animatedVisibilityScope,
                    exit = fadeOut(tween(TransitionDurationMillis / 2)),
                    resizeMode = scaleToBounds(),
                )
                .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item(contentType = "image") { ViewerImage(item, config, animatedVisibilityScope) }
        item(contentType = "details") { ViewerDetails(item, config, animatedVisibilityScope) }
        items(RelatedItemCount / 2, contentType = { "related" }) { rowIndex ->
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(2) { column ->
                    val index = rowIndex * 2 + column
                    ResultCard(
                        itemId = related.idAt(index),
                        config = config,
                        animatedVisibilityScope = animatedVisibilityScope,
                        modifier =
                            Modifier.weight(1f).semantics { contentDescription = "related-$index" },
                        onClick = { onRelatedClick(related, index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedTransitionScope.ViewerImage(
    item: ResultItem,
    config: SharedTransitionScope.SharedContentConfig,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    // Like the image search viewer: the image keeps its aspect ratio, limited to a fraction of the
    // screen height, and the details below move with its height.
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val maxImageHeight = with(LocalDensity.current) { (windowHeight * MaxImageHeightRatio).toDp() }
    Box(
        Modifier.sharedBounds(
            rememberSharedContentState(SharedKey(item.id, ElementType.Image), config),
            animatedVisibilityScope = animatedVisibilityScope,
            resizeMode = RemeasureToBounds,
        )
    ) {
        ItemImage(
            item = item,
            contentScale = ContentScale.FillWidth,
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(max = maxImageHeight)
                    .aspectRatio(item.aspectRatio, matchHeightConstraintsFirst = true),
        )
    }
}

@Composable
private fun SharedTransitionScope.ViewerDetails(
    item: ResultItem,
    config: SharedTransitionScope.SharedContentConfig,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Publisher(
            item,
            config,
            animatedVisibilityScope,
            resizeMode = scaleToBounds(),
            modifier = Modifier,
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (item.price != null) {
            Price(item, config, animatedVisibilityScope, Modifier.padding(top = 4.dp))
        }
        Text(
            text = Description,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Tags.forEach { tag ->
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelMedium,
                    modifier =
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ItemImage(
    item: ResultItem,
    contentScale: ContentScale,
    modifier: Modifier,
) {
    AsyncImage(
        model = LocalImageUrls.current[item.imageId],
        contentDescription = null,
        imageLoader = LocalImageLoader.current,
        modifier = modifier,
        placeholder = item.placeholder,
        error = item.placeholder,
        fallback = item.placeholder,
        contentScale = contentScale,
    )
}

private const val GridItemCount = 200
private const val RelatedItemCount = 8
private const val TransitionDurationMillis = 150

/**
 * Number of distinct images served by the mock server. The image sizes must match the images that
 * SharedElementTransitionBenchmark pre-generates.
 */
private const val ImageCount = 48
private const val ImageWidthPx = 480

private val AspectRatios = floatArrayOf(0.6f, 0.75f, 0.8f, 1f, 1.25f, 1.5f)

private val PlaceholderColors =
    arrayOf(
        Color(0xFF8D6E63),
        Color(0xFF5C6BC0),
        Color(0xFF26A69A),
        Color(0xFFFFA726),
        Color(0xFF78909C),
        Color(0xFFAB47BC),
    )

private val Titles =
    arrayOf(
        "Mountain lake at sunset with a wooden pier",
        "City skyline at night from the harbor",
        "Autumn forest trail",
        "Desert dunes under a clear sky with long shadows",
        "Mountain village in a valley",
        "Coastal cliffs and lighthouse in the morning fog",
    )

private val Publishers = arrayOf("travel", "photos", "outdoors", "news")

private const val Description =
    "A wide view of the landscape with warm light and soft shadows. The photo was taken in the " +
        "early evening and shows the surrounding hills, the water, and a few buildings."

private val Tags = arrayOf("Landscape", "Travel", "Nature")

private val CardShape = RoundedCornerShape(12.dp)

/**
 * Maximum viewer image height as a fraction of the screen height, as in the image search viewer.
 */
private const val MaxImageHeightRatio = 0.65f

private class SharedElementsConfig(private val enabled: Boolean) :
    SharedTransitionScope.SharedContentConfig {
    override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
        get() = enabled
}

private enum class ElementType {
    Bounds,
    Image,
    Publisher,
    Shopping,
}

private data class SharedKey(val itemId: Long, val type: ElementType)

/**
 * A list of result ids: the top-level grid when [parentId] is null, or the related results of
 * [parentId]. Ids are unique within a viewer stack, so shared element keys never collide.
 */
private class ResultList(private val parentId: Long?, val size: Int) {
    fun idAt(index: Int): Long =
        if (parentId == null) index.toLong()
        else GridItemCount + parentId * RelatedItemCount + index
}

private val GridResults = ResultList(parentId = null, size = GridItemCount)

/** An entry in the viewer stack. Opening a related result pushes a new instance. */
private class ViewerInstance(val results: ResultList, val initialPage: Int, val depth: Int)

/**
 * Fake metadata for a result. Like real results, the text differs between results, so text layout
 * and glyph caches are not reused across items.
 */
private class ResultItem(val id: Long) {
    val imageId: Int = Math.floorMod(id * 31 + 7, ImageCount.toLong()).toInt()
    val aspectRatio: Float = AspectRatios[imageId % AspectRatios.size]
    val placeholder: ColorPainter =
        ColorPainter(PlaceholderColors[imageId % PlaceholderColors.size])
    val title: String = "${Titles[imageId % Titles.size]} $id"
    val publisher: String = "${Publishers[imageId % Publishers.size]}$id.example.com"
    /** Only some results are products with shopping metadata. */
    val price: String? = if (imageId % 3 == 0) "\$${5 + id % 200}.${10 + id % 90}" else null
}

private val LocalImageLoader = staticCompositionLocalOf<ImageLoader> { error("No ImageLoader") }

private val LocalImageUrls = staticCompositionLocalOf { ImageUrls(baseUrl = null) }

/** Image URLs on the benchmark's mock server, built once per image to avoid per-item strings. */
private class ImageUrls(baseUrl: String?) {
    private val urls: Array<String>? = baseUrl?.let { base ->
        Array(ImageCount) { imageId ->
            val height = (ImageWidthPx / AspectRatios[imageId % AspectRatios.size]).roundToInt()
            "${base}image/$imageId/${ImageWidthPx}x$height.png"
        }
    }

    operator fun get(imageId: Int): String? = urls?.get(imageId)
}

/**
 * Tracks image requests in snapshot state, so the reported UI state waits for images and reports
 * failed loads. Coil calls these listener methods on the main thread.
 */
private class ImageLoadTracker : EventListener() {
    private var inFlight by mutableIntStateOf(0)

    var failedCount by mutableIntStateOf(0)
        private set

    val isLoading: Boolean
        get() = inFlight > 0

    override fun onStart(request: ImageRequest) {
        inFlight++
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        onFinish()
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        onFinish()
        failedCount++
    }

    override fun onCancel(request: ImageRequest) {
        onFinish()
    }

    private fun onFinish() {
        inFlight = (inFlight - 1).coerceAtLeast(0)
    }
}

private const val ResultsGridDescription = "results-grid"
private const val ViewerDescriptionPrefix = "viewer-"
private const val GridState = "state-grid"
private const val ViewerStatePrefix = "state-viewer-"
private const val BusyState = "state-busy"
private const val ImageLoadFailedState = "state-image-load-failed"
