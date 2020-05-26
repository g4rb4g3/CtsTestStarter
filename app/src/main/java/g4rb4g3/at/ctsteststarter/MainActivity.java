package g4rb4g3.at.ctsteststarter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static g4rb4g3.at.ctsteststarter.KeyInterceptorService.SHOW_MESSAGE;

public class MainActivity extends Activity {
  private boolean mBound = false;
  private KeyInterceptorService mService;
  private PackageManager mPackageManager = null;
  private List<ApplicationInfo> mApplist = null;
  private ApplicationAdapter mListAdapter = null;
  private ListView mLvAppList;
  private AlertDialog mAlertDialog;

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

    mPackageManager = getPackageManager();

    mLvAppList = findViewById(R.id.lv_all_apps);
    mLvAppList.setOnItemClickListener((parent, view, position, id) -> {
      ApplicationInfo applicationInfo = mApplist.get(position);
      String appLabel = applicationInfo.loadLabel(mPackageManager).toString();
      applicationInfo.name = appLabel;
      mAlertDialog = new AlertDialog.Builder(this)
          .setTitle(R.string.next_step)
          .setMessage(getString(R.string.long_press_to_map_app, appLabel))
          .setNegativeButton(R.string.cancel, (dialog, which) -> mService.cancel())
          .setCancelable(false)
          .create();
      mAlertDialog.show();
      mService.mapAppToKey(applicationInfo);
    });

    mLvAppList.setOnItemLongClickListener((parent, view, position, id) -> {
      String packageName = mApplist.get(position).packageName;
      startActivity(mPackageManager.getLaunchIntentForPackage(packageName));
      return true;
    });
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
      case R.id.mi_howto:
        new AlertDialog.Builder(this)
            .setTitle(R.string.how_to)
            .setMessage(R.string.app_description)
            .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
            .show();
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

  private class LoadApplications extends AsyncTask<Void, Void, Void> {
    private ProgressDialog progress = null;

    @Override
    protected Void doInBackground(Void... params) {
      mApplist = checkForLaunchIntent(mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA));
      mListAdapter = new ApplicationAdapter(MainActivity.this,
          R.layout.app_list_item, mApplist);

      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      mLvAppList.setAdapter(mListAdapter);
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

    private List<ApplicationInfo> checkForLaunchIntent(List<ApplicationInfo> list) {
      String ownPackageName = getPackageName();
      ArrayList<ApplicationInfo> applist = new ArrayList<>();
      for (ApplicationInfo info : list) {
        try {
          if (!ownPackageName.equals(info.packageName) && mPackageManager.getLaunchIntentForPackage(info.packageName) != null) {
            applist.add(info);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      return applist;
    }
  }
}
