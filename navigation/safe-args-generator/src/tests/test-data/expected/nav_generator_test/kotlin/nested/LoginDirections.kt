package foo

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int

class LoginDirections private constructor() {
    private object ActionDone : NavDirections {
        override fun getActionId(): Int = foo.R.id.action_done

        override fun getArguments(): Bundle {
            val result = Bundle()
            return result
        }
    }

    companion object {
        fun actionDone(): NavDirections = ActionDone
    }
}
