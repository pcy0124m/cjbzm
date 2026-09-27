package com.car.floatpanel;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppSelectHelper {
    private final PackageManager pm;
    public AppSelectHelper(Context ctx) {
        pm = ctx.getPackageManager();
    }
    public List<AppItem> getInstallAppList() {
        List<AppItem> list = new ArrayList<>();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo info : apps) {
            // 只列出能启动的应用（有 LaunchIntent）
            if (pm.getLaunchIntentForPackage(info.packageName) != null) {
                AppItem item = new AppItem();
                item.packageName = info.packageName;
                item.appName = pm.getApplicationLabel(info).toString();
                item.icon = pm.getApplicationIcon(info);
                list.add(item);
            }
        }
        Collections.sort(list, (a, b) -> a.appName.compareToIgnoreCase(b.appName));
        return list;
    }
    public Drawable getAppIcon(String pkgName) {
        try {
            return pm.getApplicationIcon(pkgName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
    public static class AppItem {
        public String packageName;
        public String appName;
        public Drawable icon;
    }
}
