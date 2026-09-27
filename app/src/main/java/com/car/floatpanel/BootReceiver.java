package com.car.floatpanel;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent svc = new Intent(context, FloatPanelService.class);
                context.startService(svc);
            }, 3000);
        }
    }
}
