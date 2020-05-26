package g4rb4g3.at.ctsteststarter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ApplicationAdapter extends ArrayAdapter<ApplicationInfo> {
  private List<ApplicationInfo> mAppsList = null;
  private Context mContext;
  private PackageManager mPackageManager;

  public ApplicationAdapter(Context context, int textViewResourceId, List<ApplicationInfo> appsList) {
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
  public ApplicationInfo getItem(int position) {
    return ((mAppsList != null) ? mAppsList.get(position) : null);
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

    ApplicationInfo applicationInfo = mAppsList.get(position);
    if (null != applicationInfo) {
      TextView appName = view.findViewById(R.id.app_name);
      TextView packageName = view.findViewById(R.id.app_paackage);
      ImageView iconview = view.findViewById(R.id.app_icon);

      appName.setText(applicationInfo.name);
      packageName.setText(applicationInfo.packageName);
      iconview.setImageDrawable(applicationInfo.loadIcon(mPackageManager));
    }
    return view;
  }
}
