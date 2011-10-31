
package org.zakky.nfcforeverybody;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;

/**
 * タブレットと電話で別のActivityを起動できるようにするための、
 * 中継用の Activity です。
 */
public class UsbAccessoryActivity extends Activity {
    static final String TAG = UsbAccessoryActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = createIntent(this);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private static Intent createIntent(Activity activity) {
        final Display display = activity.getWindowManager().getDefaultDisplay();
        int maxExtent = Math.max(display.getWidth(), display.getHeight());

        Intent intent;
        if (1200 < maxExtent) {
            Log.i(TAG, "starting tablet ui");
            intent = new Intent(activity, NfcAccessoryTabletActivity.class);
        } else {
            Log.i(TAG, "starting phone ui");
            intent = new Intent(activity, NfcAccessoryPhoneActivity.class);
        }
        return intent;
    }
}
