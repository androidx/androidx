package foo;
import com.android.support.lifecycle.OnState;
import static com.android.support.lifecycle.Lifecycle.STARTED;
import static com.android.support.lifecycle.Lifecycle.STOPPED;
import com.android.support.lifecycle.LifecycleProvider;

public class TooManyArgs {
    @OnState(STOPPED)
    public void onStop(LifecycleProvider provider, int prevstate, int x){}
}