package androidx.compose.ui.draganddrop

import androidx.compose.ui.ExperimentalComposeUiApi
import org.w3c.dom.DataTransfer

/**
 * Definition for a type representing transferable data. It could be a remote URI,
 * rich text data on the clip board, a local file, or more.
 */
actual class DragAndDropTransferData @ExperimentalComposeUiApi constructor(
    internal val nativeTransferData: DataTransfer? = null
)

/**
 * While we'll definitely benefit from (even partial) commonization of transfer data,
 * it actually should be properly designed and coordinated throughout all teams, so now
 * the recommended way is to just pass/access native DataTransfer if needed
 */
@ExperimentalComposeUiApi
val DragAndDropTransferData.domDataTransferOrNull: DataTransfer?
    get() = nativeTransferData