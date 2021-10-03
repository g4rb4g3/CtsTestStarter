package g4rb4g3.at.ctsteststarter;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.widget.Toast;

import com.lge.ivi.IKeyInterceptor;
import com.lge.ivi.IKeyService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import androidx.core.app.NotificationCompat;

public class KeyInterceptorService extends Service {
    public static KeyInterceptorService self;

    public static final String PREFERENCES_NAME = "preferences";

    public static final int SHOW_MESSAGE = 1;
    public static final int MAPPED_APP = 2;
    public static final int UNMAPPED_APP = 3;

    public static final int DBL_TIMEOUT = 300; // ms
    public static final int DBL_TIMEOUT_EV = 2000; // ms

    public static final int KEYCODE_EV = 65535;

    private final IBinder mBinder = new KeyInterceptorBinder();
    private final List<Handler> mRegisteredHandlers = new ArrayList<>();
    private SharedPreferences mSharedPreferences;
    private ApplicationInfo mNextAppMappingApplicationInfo;
    private boolean mClearKeyMapping = false;
    private boolean mActivityVisible = false;
    private boolean mMapBackKey = false;
    private boolean mMapRecentApps = false;

    // Double keypress
    private static int sCalls = 0;
    private static long sLastCall = 0;
    private static int sLastKey = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runnable = () -> {
        if (sLastKey != 0) {
            // Resend the key if it was not a double keypress
            injectKeyEvent(sLastKey);
        }
    };

    private boolean _clearDbl() {
        handler.removeCallbacks(runnable);
        sLastKey = 0;
        sCalls = 0;
        sLastCall = 0;
        return (mNextAppMappingApplicationInfo != null || mClearKeyMapping || mMapBackKey || mMapRecentApps);
    }

    public IKeyInterceptor.Stub mKeyInterceptor = new IKeyInterceptor.Stub() {
        @Override
        public boolean onKeyEvent(KeyEvent keyEvent) {
            String key;

            if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            int to = keyEvent.getKeyCode() == KEYCODE_EV ? DBL_TIMEOUT_EV : DBL_TIMEOUT; // More time if EV button

            if (keyEvent.isLongPress()) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_SETTINGS) {
                    if (!mActivityVisible) {
                        startActivity(new Intent(getApplicationContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } else if (mMapBackKey || mClearKeyMapping || mNextAppMappingApplicationInfo != null) {
                        notifyHandlers(SHOW_MESSAGE, getString(R.string.settings_cannot_be_mapped));
                        cancel();
                    }
                    return true;
                }

                key = String.valueOf(keyEvent.getKeyCode());
            }
            else {
                key = keyEvent.getKeyCode() + "_dbl";

                // It should be a resended key event
                if (sLastCall > 0 && System.currentTimeMillis() - sLastCall >= to) {
                    return _clearDbl();
                }

                // If not mapping check if we have a registered application
                if (mNextAppMappingApplicationInfo == null && !mClearKeyMapping && !mMapBackKey
                        && !mMapRecentApps) {
                    String packageName = mSharedPreferences.getString(key, null);
                    // If there is no such app, we can send keycode immediately (no delay)
                    if (packageName == null) return _clearDbl();
                }

                // If the new key is not the same as the previous, then send it immediately
                if (sLastKey != 0 && sLastKey != keyEvent.getKeyCode()) return _clearDbl();

                sLastKey = keyEvent.getKeyCode();
                sLastCall = System.currentTimeMillis();
                // Resend keykode later to make single presses works as well though a little delayed
                handler.postDelayed(runnable, to);

                // Count clicks
                sCalls++;
                if (sCalls == 2) {
                    // Go back to app manager if it was EV button
                    if (sLastKey == KEYCODE_EV) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // You need this if starting
                        // the activity from a service
                        intent.setAction(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        startActivity(intent);
                    }
                    // It is a double click
                    _clearDbl();
                }
                // No process keypress further until we know it is a single or double keypress
                else return true;
            }

            // If it is nor double nor long keypress
            if (key.length() == 0) return false;

            // Here we know if it is a double or single keypress

            // Mapping in progress a
            if (mNextAppMappingApplicationInfo != null) {
                mSharedPreferences.edit().putString(key, mNextAppMappingApplicationInfo.packageName).apply();
                notifyHandlers(SHOW_MESSAGE, getString(R.string.mapping_app_completed, key, mNextAppMappingApplicationInfo.name));
                notifyHandlers(MAPPED_APP, mNextAppMappingApplicationInfo.packageName);
                mNextAppMappingApplicationInfo = null;
                return true;
            }

            if (mClearKeyMapping) {
                String packageName = mSharedPreferences.getString(key, null);
                if (packageName != null) {
                    notifyHandlers(UNMAPPED_APP, packageName);
                    mSharedPreferences.edit().remove(key).apply();
                    notifyHandlers(SHOW_MESSAGE, getString(R.string.mapping_cleared, key));
                }
                mClearKeyMapping = false;
                return true;
            }

            if (mMapBackKey) {
                mSharedPreferences.edit().putString(key, "back_key").apply();
                notifyHandlers(SHOW_MESSAGE, getString(R.string.mapping_back_completed, key));
                mMapBackKey = false;
                return true;
            }

            if (mMapRecentApps) {
                mSharedPreferences.edit().putString(key, "recent_apps").apply();
                notifyHandlers(SHOW_MESSAGE, getString(R.string.mapping_recent_completed, key));
                mMapRecentApps = false;
                return true;
            }

            String packageName = mSharedPreferences.getString(key, null);
            if (packageName == null) {
                return false;
            }
            switch (packageName) {
                case "back_key":
                    injectKeyEvent(KeyEvent.KEYCODE_BACK);
                    break;
                case "recent_apps":
                    openRecentApps();
                    break;
                default:
                    Context ctx = getApplicationContext();
                    ctx.startActivity(ctx.getPackageManager().getLaunchIntentForPackage(packageName));
            }

            return true;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        self = this;  // Maybe this is not the most beautiful solution

        Notification notification = new NotificationCompat.Builder(this, "CtsTestStarterKeyInterceptorService")
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build();
        startForeground(1, notification);

        mSharedPreferences = getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        try {
            IKeyService keyService = getKeyService();
            assert keyService != null;
            keyService.setKeyInterceptor(mKeyInterceptor);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;  // Try to make the OS restart the service
    }

    @Override
    public void onDestroy() {
        self = null;
        super.onDestroy();
        // Try to restart immediately if OS kills it
        Intent broadcastIntent = new Intent(this, ServiceRestartBroadcastReceiver.class);
        sendBroadcast(broadcastIntent);
    }

    private IKeyService getKeyService() {
        try {
            Method method = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
            IBinder binder = (IBinder) method.invoke(null, "com.lge.ivi.server.Key");
            if (binder != null) {
                return IKeyService.Stub.asInterface(binder);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void registerHandler(Handler handler) {
        mRegisteredHandlers.add(handler);
    }

    public void unregisterHandler(Handler handler) {
        mRegisteredHandlers.remove(handler);
    }

    public void mapAppToKey(ApplicationInfo applicationInfo) {
        mNextAppMappingApplicationInfo = applicationInfo;
    }

    public void mapBackKey() {
        mMapBackKey = true;
    }

    public void clearKeyMapping() {
        mClearKeyMapping = true;
    }

    public void mapRecentApps() {
        mMapRecentApps = true;
    }

    public void cancel() {
        mNextAppMappingApplicationInfo = null;
        mMapBackKey = false;
        mClearKeyMapping = false;
        mMapRecentApps = false;
    }

    public void setActivityVisible(boolean visible) {
        mActivityVisible = visible;
    }

    private void notifyHandlers(int what, Object obj) {
        Message msg = new Message();
        msg.what = what;
        msg.obj = obj;
        for (Handler handler : mRegisteredHandlers) {
            handler.sendMessage(msg);
        }
    }

    public static void injectKeyEvent(int keyCode) {
        try {
            String keyCommand = "input keyevent " + keyCode + " > /dev/null 2> /dev/null < /dev/null &";
            ProcessExecutor.executeRootCommand(keyCommand);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void openRecentApps() {
        try {
            Class serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getService = serviceManagerClass.getMethod("getService", String.class);
            IBinder retbinder = (IBinder) getService.invoke(null, "statusbar");
            Class statusBarClass = Class.forName(retbinder.getInterfaceDescriptor());
            Object statusBarObject = statusBarClass.getClasses()[0]
                    .getMethod("asInterface", IBinder.class).invoke(null, retbinder);
            Method toggleRecentApps = statusBarClass.getMethod("toggleRecentApps");
            toggleRecentApps.setAccessible(true);
            toggleRecentApps.invoke(statusBarObject);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public class KeyInterceptorBinder extends Binder {
        public KeyInterceptorService getService() {
            return KeyInterceptorService.this;
        }
    }
}
