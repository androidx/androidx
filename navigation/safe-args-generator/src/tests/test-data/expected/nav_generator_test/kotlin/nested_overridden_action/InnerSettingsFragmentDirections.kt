package foo.flavor

import androidx.navigation.NavDirections
import kotlin.Int

class InnerSettingsFragmentDirections {
    companion object {
        fun exit(exitReason: Int): NavDirections = InnerSettingsDirections.exit(exitReason)
    }
}
