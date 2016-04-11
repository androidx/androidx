package android.support.v17.leanback.supportleanbackshowcase.app.page;

import android.app.Activity;
import android.os.Bundle;
import android.support.v17.leanback.supportleanbackshowcase.R;

/**
 * Activity showcasing the use of {@link android.support.v17.leanback.widget.PageRow} and
 * {@link android.support.v17.leanback.widget.ListRow}.
 */
public class PageAndListRowActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_list_row);
    }
}
