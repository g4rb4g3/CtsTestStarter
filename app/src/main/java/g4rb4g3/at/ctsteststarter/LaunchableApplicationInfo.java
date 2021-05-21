package g4rb4g3.at.ctsteststarter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class LaunchableApplicationInfo extends ApplicationInfo {
  public boolean isLaunchable;
  public boolean isSystemApp;
  public boolean isKeyMapped;
  public String version;

  public LaunchableApplicationInfo(ApplicationInfo applicationInfo, boolean isLaunchable, Context context) {
    super(applicationInfo);
    this.isLaunchable = isLaunchable;
    this.isSystemApp = (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    try {
      PackageInfo packageInfo = context.getPackageManager().getPackageInfo(applicationInfo.packageName, 0);
      this.version = packageInfo.versionName;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
  }
}
