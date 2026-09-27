package com.car.floatpanel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        字符串 action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                意图 svc = new 意图(上下文, FloatPanelService.类);
                context.startService(svc);
            }, 3000);
        }
    }
}
