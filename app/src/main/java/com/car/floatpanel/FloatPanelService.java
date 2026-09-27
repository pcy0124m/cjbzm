package com.car.floatpanel;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;

import java.util.List;

public class FloatPanelService extends Service {

    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private View floatView;

    private MapView mMapView;
    private AMap aMap;
    private TextView tvMusic, tvTpms;

    private ImageView[] shortcutIcons = new ImageView[7];
    private TextView[] shortcutLabels = new TextView[7];
    private String[] shortcutPkgs = new String[7];

    private final int[] iconIds = {R.id.icon_0, R.id.icon_1, R.id.icon_2, R.id.icon_3, R.id.icon_4, R.id.icon_5, R.id.icon_6};
    private final int[] labelIds = {R.id.label_0, R.id.label_1, R.id.label_2, R.id.label_3, R.id.label_4, R.id.label_5, R.id.label_6};
    private final int[] shortcutIds = {R.id.shortcut_0, R.id.shortcut_1, R.id.shortcut_2, R.id.shortcut_3, R.id.shortcut_4, R.id.shortcut_5, R.id.shortcut_6};

    private AppSelectHelper appSelectHelper;
    private AppConfigHelper appConfigHelper;
    private TpmsUsbReader tpmsReader;

    private Button btnZoomMinus, btnZoomPlus, btnClose;
    private float windowScale = 1.0f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 1.5f;

    private Handler memCheckHandler;
    private Runnable memCheckRunnable;
    private boolean mapHidden = false;
    private static final long MEM_CHECK_INTERVAL = 2000;
    private static final long MEM_THRESHOLD = 300 * 1024 * 1024;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        params.format = PixelFormat.TRANSLUCENT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.gravity = Gravity.CENTER;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;

        floatView = LayoutInflater.from(this).inflate(R.layout.float_panel, null);
        wm.addView(floatView, params);

        tvMusic = floatView.findViewById(R.id.tv_music_info);
        tvTpms = floatView.findViewById(R.id.tv_tpms_data);
        mMapView = floatView.findViewById(R.id.mapView);
        mMapView.onCreate(null);
        aMap = mMapView.getMap();
        aMap.getUiSettings().setScaleControlsEnabled(false);
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
        aMap.showBuildings(false);
        aMap.showTraffic(false);

        appSelectHelper = new AppSelectHelper(this);
        appConfigHelper = new AppConfigHelper(this);
        tpmsReader = new TpmsUsbReader(this, data -> tvTpms.post(() -> tvTpms.setText(data)));

        // 恢复历史配置
        String[] musicCfg = appConfigHelper.getMusicApp();
        tvMusic.setText(musicCfg[1]);

        String[] tpmsCfg = appConfigHelper.getTpmsApp();
        tvTpms.setText(tpmsCfg[1]);

        // 7 个快捷栏
        for (int i = 0; i < 7; i++) {
            shortcutIcons[i] = floatView.findViewById(iconIds[i]);
            shortcutLabels[i] = floatView.findViewById(labelIds[i]);
            View scLayout = floatView.findViewById(shortcutIds[i]);

            String[] scCfg = appConfigHelper.getShortcut(i);
            shortcutPkgs[i] = scCfg[0];
            shortcutLabels[i].setText(scCfg[1]);
            if (!shortcutPkgs[i].isEmpty()) {
                shortcutIcons[i].setImageDrawable(appSelectHelper.getAppIcon(shortcutPkgs[i]));
            }
            final int index = i;
            scLayout.setOnClickListener(v -> showAppSelectDialog((pkg, name) -> {
                shortcutPkgs[index] = pkg;
                shortcutLabels[index].setText(name);
                shortcutIcons[index].setImageDrawable(appSelectHelper.getAppIcon(pkg));
                appConfigHelper.saveShortcut(index, pkg, name);
                launchApp(pkg);
            }));
        }

        // 播放器区域
        floatView.findViewById(R.id.layout_player).setOnClickListener(v ->
                showAppSelectDialog((pkg, name) -> {
                    tvMusic.setText(name);
                    appConfigHelper.saveMusicApp(pkg, name);
                    launchApp(pkg);
                }));

        // 胎压区域
        floatView.findViewById(R.id.layout_tpms).setOnClickListener(v ->
                showAppSelectDialog((pkg, name) -> {
                    tvTpms.setText(name);
                    appConfigHelper.saveTpmsApp(pkg, name);
                    launchApp(pkg);
                }));

        // 缩放 / 关闭
        windowScale = appConfigHelper.getScale();
        btnZoomMinus = floatView.findViewById(R.id.btn_zoom_minus);
        btnZoomPlus = floatView.findViewById(R.id.btn_zoom_plus);
        btnClose = floatView.findViewById(R.id.btn_close);

        btnZoomMinus.setOnClickListener(v -> {
            windowScale = Math.max(MIN_SCALE, windowScale - 0.1f);
            applyWindowScale();
            appConfigHelper.saveScale(windowScale);
        });
        btnZoomPlus.setOnClickListener(v -> {
            windowScale = Math.min(MAX_SCALE, windowScale + 0.1f);
            applyWindowScale();
            appConfigHelper.saveScale(windowScale);
        });
        btnClose.setOnClickListener(v -> stopSelf());
        applyWindowScale();

        // 拖动
        floatView.setOnTouchListener(new View.OnTouchListener() {
            private int initX, initY;
            private float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = params.x;
                        initY = params.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initX + (int) (event.getRawX() - touchX);
                        params.y = initY + (int) (event.getRawY() - touchY);
                        wm.updateViewLayout(floatView, params);
                        return true;
                }
                return false;
            }
        });

        // 内存检测
        memCheckHandler = new Handler(Looper.getMainLooper());
        memCheckRunnable = new Runnable() {
            @Override
            public void run() {
                long avail = getAvailableMemory();
                if (avail < MEM_THRESHOLD && !mapHidden) {
                    mMapView.setVisibility(View.GONE);
                    mapHidden = true;
                } else if (avail >= MEM_THRESHOLD && mapHidden) {
                    mMapView.setVisibility(View.VISIBLE);
                    mapHidden = false;
                }
                memCheckHandler.postDelayed(this, MEM_CHECK_INTERVAL);
            }
        };
        memCheckHandler.post(memCheckRunnable);
    }

    private void showAppSelectDialog(AppSelectCallback callback) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_applist, null);
        ListView listView = dialogView.findViewById(R.id.lv_app);
        List<AppSelectHelper.AppItem> appList = appSelectHelper.getInstallAppList();

        ArrayAdapter<AppSelectHelper.AppItem> adapter = new ArrayAdapter<AppSelectHelper.AppItem>(
                this, android.R.layout.simple_list_item_1, appList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                AppSelectHelper.AppItem item = getItem(position);
                text.setText(item.appName);
                text.setCompoundDrawablesWithIntrinsicBounds(item.icon, null, null, null);
                text.setCompoundDrawablePadding(16);
                return view;
            }
        };
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择应用")
                .setView(dialogView)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            AppSelectHelper.AppItem selected = appList.get(position);
            callback.onSelect(selected.packageName, selected.appName);
            dialog.dismiss();
        });
        dialog.show();
    }

    private interface AppSelectCallback {
        void onSelect(String pkg, String name);
    }

    private void applyWindowScale() {
        floatView.setScaleX(windowScale);
        floatView.setScaleY(windowScale);
        wm.updateViewLayout(floatView, params);
    }

    private void launchApp(String pkg) {
        if (pkg == null || pkg.isEmpty()) return;
        Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
        if (intent != null) startActivity(intent);
    }

    private long getAvailableMemory() {
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
equals(intent.getAction())) {
            tpmsReader.打开();
        }
         START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        如果 (memCheckHandler 不等于 空) memCheckHandler.removeCallbacks(memCheckRunnable);
        如果 (floatView 不是 空) wm.移除视图(floatView);
如果 (mMapView 不是 空) mMapView.销毁();
        如果 (tpmsReader 不是 空) tpmsReader.关闭();
    }

    @Override
    公共 IBinder onBind(Intent intent) {
        return null;
    }
}
