package foo.flavor

import android.os.Bundle
import androidx.navigation.NavDirections
import foo.R
import kotlin.Int
import kotlin.String

class SettingsFragmentDirections private constructor() {
    private data class Exit(val exitReason: String = "DIFFERENT") : NavDirections {
        override fun getActionId(): Int = R.id.exit

        override fun getArguments(): Bundle {
            val result = Bundle()
            result.putString("exitReason", this.exitReason)
            return result
        }
    }

    companion object {
        fun exit(exitReason: String = "DIFFERENT"): NavDirections = Exit(exitReason)
    }
}
