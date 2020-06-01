package g4rb4g3.at.ctsteststarter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import static g4rb4g3.at.ctsteststarter.KeyInterceptorService.PREFERENCES_NAME;

public class ApplicationAdapter extends ArrayAdapter<LaunchableApplicationInfo> {
  private List<LaunchableApplicationInfo> mAppsList;
  private Context mContext;
  private PackageManager mPackageManager;

  public ApplicationAdapter(Context context, int textViewResourceId, List<LaunchableApplicationInfo> appsList) {
    super(context, textViewResourceId, appsList);
    mContext = context;
    mAppsList = appsList;
    mPackageManager = context.getPackageManager();
  }

  @Override
  public int getCount() {
    return ((mAppsList != null) ? mAppsList.size() : 0);
  }

  @Override
  public LaunchableApplicationInfo getItem(int position) {
    return ((mAppsList != null) ? mAppsList.get(position) : null);
  }

  public void setKeyMapped(String packageName, boolean mapped) {
    for (LaunchableApplicationInfo info : mAppsList) {
      if (info.packageName.equals(packageName)) {
        info.isKeyMapped = mapped;
        notifyDataSetChanged();
        break;
      }
    }
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View view = convertView;
    if (null == view) {
      LayoutInflater layoutInflater = (LayoutInflater) mContext
          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      view = layoutInflater.inflate(R.layout.app_list_item, null);
    }

    LaunchableApplicationInfo applicationInfo = mAppsList.get(position);
    if (null != applicationInfo) {
      TextView appName = view.findViewById(R.id.app_name);
      TextView packageName = view.findViewById(R.id.app_paackage);
      ImageView iconview = view.findViewById(R.id.app_icon);
      CheckBox mapped = view.findViewById(R.id.cb_mapped);

      appName.setText(applicationInfo.name);
      packageName.setText(applicationInfo.packageName);
      iconview.setImageDrawable(applicationInfo.loadIcon(mPackageManager));

      mapped.setClickable(applicationInfo.isKeyMapped);
      mapped.setOnCheckedChangeListener((buttonView, isChecked) -> {
        if (!isChecked) {
          SharedPreferences sharedPreferences = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
          SharedPreferences.Editor editor = sharedPreferences.edit();
          Map<String, ?> preferences = sharedPreferences.getAll();
          for (Map.Entry<String, ?> entry : preferences.entrySet()) {
            if (applicationInfo.packageName.equals(entry.getValue().toString())) {
              editor.remove(entry.getKey());
            }
          }
          editor.commit();
          applicationInfo.isKeyMapped = false;
          mapped.setClickable(false);
        }
      });
      mapped.setChecked(applicationInfo.isKeyMapped);
    }
    return view;
  }
}
