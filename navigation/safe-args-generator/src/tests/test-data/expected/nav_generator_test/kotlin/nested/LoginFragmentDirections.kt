package foo.flavor.account

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import foo.LoginDirections
import foo.R

class LoginFragmentDirections private constructor() {
    companion object {
        fun register(): NavDirections = ActionOnlyNavDirections(R.id.register)

        fun actionDone(): NavDirections = LoginDirections.actionDone()
    }
}
