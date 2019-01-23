package foo.flavor

import androidx.navigation.NavDirections
import foo.InnerSettingsDirections
import kotlin.Int

class InnerSettingsFragmentDirections private constructor() {
    companion object {
        fun exit(exitReason: Int): NavDirections = InnerSettingsDirections.exit(exitReason)
    }
}
