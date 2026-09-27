package com.car.floatpanel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.app.PendingIntent;

公共 类 UsbPermissionReceiver 扩展 BroadcastReceiver
     static final  ACTION_USB_PERMISSION = "com.car.floatpanel.USB_PERMISSION";

    @Override
    public void onReceive(Context context, Intent intent) {
        字符串 action = intent.getAction();
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        如果 (UsbManager.ACTION_USB_DEVICE_ATTACHED.等于(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                Intent permissionIntent = new Intent(ACTION_USB_PERMISSION);
                permissionIntent.setPackage(context.getPackageName());
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                PendingIntent pi = PendingIntent.getBroadcast(context, 0, permissionIntent, flags);
                usbManager.requestPermission(device, pi);
            }
        } else if (ACTION_USB_PERMISSION.equals(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
            if (granted && device != null) {
                Intent svc = new Intent(context, FloatPanelService.class);
                svc.setAction("OPEN_TPMS");
                context.startService(svc);
            }
        }
    }
}

