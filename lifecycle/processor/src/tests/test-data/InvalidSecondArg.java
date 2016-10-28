package foo;
import com.android.support.lifecycle.OnState;
import static com.android.support.lifecycle.Lifecycle.STARTED;
import static com.android.support.lifecycle.Lifecycle.STOPPED;
import com.android.support.lifecycle.LifecycleProvider;

public class InvalidSecondArg {
    @OnState(STOPPED)
    public void onStop(LifecycleProvider provider, Object prevState){}
}