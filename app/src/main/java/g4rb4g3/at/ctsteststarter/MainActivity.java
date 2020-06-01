package g4rb4g3.at.ctsteststarter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static g4rb4g3.at.ctsteststarter.KeyInterceptorService.MAPPED_APP;
import static g4rb4g3.at.ctsteststarter.KeyInterceptorService.PREFERENCES_NAME;
import static g4rb4g3.at.ctsteststarter.KeyInterceptorService.SHOW_MESSAGE;
import static g4rb4g3.at.ctsteststarter.KeyInterceptorService.UNMAPPED_APP;

public class MainActivity extends Activity {
  private boolean mBound = false;
  private KeyInterceptorService mService;
  private PackageManager mPackageManager = null;
  private ApplicationAdapter mListAdapter = null;
  private GridView mGvAppList;
  private AlertDialog mAlertDialog;
  private boolean mShowAllApps = false;

  private Map<String, Integer> contextOptions = new HashMap<>();

  private Handler mHandler = new Handler(Looper.getMainLooper()) {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case SHOW_MESSAGE:
          Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
          if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
          }
          break;
        case MAPPED_APP:
          mListAdapter.setKeyMapped(msg.obj.toString(), true);
          break;
        case UNMAPPED_APP:
          mListAdapter.setKeyMapped(msg.obj.toString(), false);
          break;
      }
    }
  };

  private ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      KeyInterceptorService.KeyInterceptorBinder binder = (KeyInterceptorService.KeyInterceptorBinder) service;
      mService = binder.getService();
      mBound = true;

      mService.registerHandler(mHandler);
      mService.isActivityVisible(true);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      mBound = false;
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setTitle(getString(R.string.app_name_version, BuildConfig.VERSION_NAME));

    mPackageManager = getPackageManager();

    enableDotsMenu();

    mGvAppList = findViewById(R.id.gv_all_apps);
    mGvAppList.setOnItemClickListener((parent, view, position, id) -> {
      LaunchableApplicationInfo info = mListAdapter.getItem(position);
      showAppOptions(info, position);
    });

    contextOptions.put(getString(R.string.launch), R.string.launch);
    contextOptions.put(getString(R.string.clear_cache), R.string.clear_cache);
    contextOptions.put(getString(R.string.clear_data), R.string.clear_data);
    contextOptions.put(getString(R.string.uninstall), R.string.uninstall);
    contextOptions.put(getString(R.string.force_stop), R.string.force_stop);
    contextOptions.put(getString(R.string.map_key), R.string.map_key);
  }

  private void enableDotsMenu() {
    try {
      ViewConfiguration config = ViewConfiguration.get(this);
      Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");

      if (menuKeyField != null) {
        menuKeyField.setAccessible(true);
        menuKeyField.setBoolean(config, false);
      }
    } catch (Exception e) {
      Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    bindService(new Intent(this, KeyInterceptorService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

    new LoadApplications().execute();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.mi_clear_mapping:
        mAlertDialog = new AlertDialog.Builder(this)
            .setTitle(R.string.next_step)
            .setMessage(R.string.long_press_clear_mapping)
            .setNegativeButton(R.string.cancel, (dialog, which) -> mService.cancel())
            .setCancelable(false)
            .create();
        mAlertDialog.show();
        mService.clearKeyMapping();
        break;
      case R.id.mi_map_back_key:
        mAlertDialog = new AlertDialog.Builder(this)
            .setTitle(R.string.next_step)
            .setMessage(R.string.long_press_map_back)
            .setNegativeButton(R.string.cancel, (dialog, which) -> mService.cancel())
            .setCancelable(false)
            .create();
        mAlertDialog.show();
        mService.mapBackKey();
        break;
      case R.id.mi_show_all_apps:
        item.setChecked(!item.isChecked());
        mShowAllApps = item.isChecked();
        new LoadApplications().execute();
        break;
      case R.id.mi_map_recents:
        mAlertDialog = new AlertDialog.Builder(this)
            .setTitle(R.string.next_step)
            .setMessage(R.string.long_press_map_recent)
            .setNegativeButton(R.string.cancel, (dialog, which) -> mService.cancel())
            .setCancelable(false)
            .create();
        mAlertDialog.show();
        mService.mapRecentApps();
        break;
    }
    return true;
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mBound) {
      mService.isActivityVisible(false);
      mService.cancel();
      mService.unregisterHandler(mHandler);
      unbindService(mServiceConnection);
    }
    if (mAlertDialog != null && mAlertDialog.isShowing()) {
      mAlertDialog.dismiss();
    }
  }

  public void mapAppToKey(final LaunchableApplicationInfo info) {
    if (!info.isLaunchable) {
      Toast.makeText(this, R.string.not_assignable, Toast.LENGTH_SHORT).show();
      return;
    }
    mAlertDialog = new AlertDialog.Builder(this)
        .setTitle(R.string.next_step)
        .setMessage(getString(R.string.long_press_to_map_app, info.name))
        .setNegativeButton(R.string.cancel, (dialog, which) -> mService.cancel())
        .setCancelable(false)
        .create();
    mAlertDialog.show();
    mService.mapAppToKey(info);
  }

  private void showAppOptions(final LaunchableApplicationInfo info, final int position) {
    final ArrayAdapter<String> items = new ArrayAdapter<>(this, android.R.layout.simple_selectable_list_item);
    if (info.isLaunchable) {
      if (!info.isKeyMapped) {
        items.add(getString(R.string.map_key));
      }
      items.add(getString(R.string.launch));
    }
    items.addAll(getString(R.string.clear_cache), getString(R.string.clear_data), getString(R.string.force_stop));
    if (!info.isSystemApp) {
      items.add(getString(R.string.uninstall));
    }
    new AlertDialog.Builder(this)
        .setTitle(info.name)
        .setIcon(info.loadIcon(mPackageManager))
        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
        .setAdapter(items, (dialog, which) -> {
          switch (contextOptions.get(items.getItem(which))) {
            case R.string.launch:
              startActivity(mPackageManager.getLaunchIntentForPackage(info.packageName));
              break;
            case R.string.uninstall:
              try {
                ProcessExecutor.executeRootCommand("pm uninstall " + info.packageName);
                mListAdapter.remove(info);
                mListAdapter.notifyDataSetChanged();

                SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
                Map<String, ?> allEntries = sharedPreferences.getAll();
                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                  if (info.packageName.equals(entry.getValue().toString())) {
                    sharedPreferences.edit().remove(entry.getKey()).commit();
                    break;
                  }
                }
              } catch (RemoteException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
              }
              break;
            case R.string.clear_cache:
              try {
                ProcessExecutor.executeRootCommand("rm -rf /data/data/" + info.packageName + "/cache/*");
              } catch (RemoteException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
              }
              break;
            case R.string.clear_data:
              try {
                killAppIfRunning(info.packageName);
                ProcessExecutor.executeRootCommand("rm -rf /data/data/" + info.packageName + "/*");
              } catch (RemoteException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
              }
              break;
            case R.string.force_stop:
              try {
                killAppIfRunning(info.packageName);
              } catch (RemoteException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
              }
              break;
            case R.string.map_key:
              mapAppToKey(info);
              break;
          }
        })
        .show();
  }

  private void killAppIfRunning(String packageName) throws RemoteException {
    String pid = ProcessExecutor.execute("/system/bin/sh", "-c", "ps | grep " + packageName + " | busybox awk '{print $2}'");
    if (pid != null) {
      ProcessExecutor.executeRootCommand("kill " + pid);
    }
  }

  private class LoadApplications extends AsyncTask<Void, Void, Void> {
    private ProgressDialog progress = null;

    @Override
    protected Void doInBackground(Void... params) {
      List<LaunchableApplicationInfo> appList = checkForLaunchIntent(mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA));
      mListAdapter = new ApplicationAdapter(MainActivity.this, R.layout.app_list_item, appList);
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      mGvAppList.setAdapter(mListAdapter);
      progress.dismiss();
      super.onPostExecute(result);
    }

    @Override
    protected void onPreExecute() {
      progress = ProgressDialog.show(MainActivity.this, null, getString(R.string.loading_applist));
      super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
      super.onProgressUpdate(values);
    }

    private List<LaunchableApplicationInfo> checkForLaunchIntent(List<ApplicationInfo> list) {
      List<String> mappedApps = new ArrayList<>();
      Map<String, ?> preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getAll();
      for (Map.Entry<String, ?> entry : preferences.entrySet()) {
        if (TextUtils.isDigitsOnly(entry.getKey())) {
          mappedApps.add(entry.getValue().toString());
        }
      }
      String ownPackageName = getPackageName();
      ArrayList<LaunchableApplicationInfo> applist = new ArrayList<>();
      for (ApplicationInfo info : list) {
        try {
          if (!ownPackageName.equals(info.packageName)) {
            LaunchableApplicationInfo applicationInfo = new LaunchableApplicationInfo(info, mPackageManager.getLaunchIntentForPackage(info.packageName) != null);
            if (!mShowAllApps && !applicationInfo.isLaunchable) {
              continue;
            }
            applicationInfo.isKeyMapped = mappedApps.contains(applicationInfo.packageName);
            String appLabel = applicationInfo.loadLabel(mPackageManager).toString();
            applicationInfo.name = appLabel == null ? "" : appLabel;
            applist.add(applicationInfo);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      Collections.sort(applist, (o1, o2) -> o1.name.toLowerCase().compareTo(o2.name.toLowerCase()));
      return applist;
    }
  }
}
