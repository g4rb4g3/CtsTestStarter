package g4rb4g3.at.ctsteststarter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.view.KeyEvent;


public class EVButtonReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        KeyInterceptorService service = KeyInterceptorService.self;
        // Simulate keypress with an unknown code
        try {
            service.mKeyInterceptor.onKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyInterceptorService.KEYCODE_EV));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        abortBroadcast();
    }
}