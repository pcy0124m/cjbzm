package com.car.floatpanel;
import android.content.Context;
import android.content.SharedPreferences;

public class AppConfigHelper {
    private static final String SP_NAME = "float_panel_config";
    private final SharedPreferences sp;

    public AppConfigHelper(Context ctx) {
        sp = ctx.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    public void saveMusicApp(String pkg, String name) {
        sp.edit().putString("music_pkg", pkg).putString("music_name", name).apply();
    }

    public String[] getMusicApp() {
        return new String[]{
                sp.getString("music_pkg", ""),
                sp.getString("music_name", "播放器")
        };
    }

    public void saveTpmsApp(String pkg, String name) {
        sp.edit().putString("tpms_pkg", pkg).putString("tpms_name", name).apply();
    }

    public String[] getTpmsApp() {
        return new String[]{
                sp.getString("tpms_pkg", ""),
                sp.getString("tpms_name", "胎压")
        };
    }

    public void saveShortcut(int index, String pkg, String name) {
        sp.edit()
                .putString("sc_pkg_" + index, pkg)
                .putString("sc_name_" + index, name)
                .apply();
    }

    public String[] getShortcut(int index) {
        return new String[]{
                sp.getString("sc_pkg_" + index, ""),
                sp.getString("sc_name_" + index, "快捷" + (index + 1))
        };
    }

    public void saveScale(float scale) {
        sp.edit().putFloat("window_scale", scale).apply();
    }

    public float getScale() {
        return sp.getFloat("window_scale", 1.0f);
    }
}
