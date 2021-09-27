package g4rb4g3.at.ctsteststarter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ServiceRestartBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, KeyInterceptorService.class));
        Toast.makeText(context, "AppManager service restarted.", Toast.LENGTH_SHORT).show();
    }
}