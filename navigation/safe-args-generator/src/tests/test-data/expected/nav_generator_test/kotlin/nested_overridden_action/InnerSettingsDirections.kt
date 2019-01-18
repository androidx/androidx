package foo

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int

class InnerSettingsDirections {
    private data class Exit(val exitReason: Int) : NavDirections {
        override fun getActionId(): Int = foo.R.id.exit

        override fun getArguments(): Bundle {
            val result = Bundle()
            result.putInt("exitReason", this.exitReason)
            return result
        }
    }

    companion object {
        fun exit(exitReason: Int): NavDirections = Exit(exitReason)
    }
}
