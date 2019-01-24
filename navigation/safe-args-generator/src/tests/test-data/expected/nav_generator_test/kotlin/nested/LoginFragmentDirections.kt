package foo.flavor.account

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import foo.LoginDirections

class LoginFragmentDirections private constructor() {
    companion object {
        fun register(): NavDirections = ActionOnlyNavDirections(foo.R.id.register)

        fun actionDone(): NavDirections = LoginDirections.actionDone()
    }
}
