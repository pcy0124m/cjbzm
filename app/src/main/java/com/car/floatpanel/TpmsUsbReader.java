package com.car.floatpanel;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

/**
 * USB 串口胎压读取（CH340 / CP2102）。
 * 注意：当前使用 bulkTransfer 做简易读取，实际协议需按你的 TPMS 硬件报文修改 parse 逻辑。
 */
public class TpmsUsbReader {
    private final Context ctx;
    private UsbDeviceConnection connection;
    private UsbInterface usbInterface;
    private UsbEndpoint epIn;
    private volatile boolean running = false;
    private final OnDataListener listener;

    public interface OnDataListener {
        void onReceive(String data);
    }

    public TpmsUsbReader(Context ctx, OnDataListener l) {
        this.ctx = ctx;
        this.listener = l;
    }

    public boolean open() {
        UsbManager usbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) return false;

        for (UsbDevice device : usbManager.getDeviceList().values()) {
            UsbDeviceConnection conn = usbManager.openDevice(device);
            if (conn == null) continue;
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface intf = device.getInterface(i);
                if (conn.claimInterface(intf, true)) {
                    for (int j = 0; j < intf.getEndpointCount(); j++) {
                        UsbEndpoint ep = intf.getEndpoint(j);
                        if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                                && ep.getDirection() == UsbConstants.USB_DIR_IN) {
                            epIn = ep;
                            usbInterface = intf;
                            connection = conn;
                            startReadLoop();
                            return true;
                        }
                    }
                }
            }
            conn.close();
        }
        return false;
    }

    private void startReadLoop() {
        running = true;
        new Thread(() -> {
            byte[] buf = new byte[64];
            while (running && connection != null) {
                try {
                    int len = connection.bulkTransfer(epIn, buf, buf.length, 200);
                    如果 (长度 > 0 && 监听器 != 空) {
                        String raw = new String(buf, 0, len).trim();
                        listener.onReceive(parse(raw));
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }, "tpms-read").start();
    }

    // TODO: 根据你的 TPMS 协议修改解析逻辑，这里原样透传
    private String parse(String raw) {
        return "TPMS: " + raw;
    }

    public void close() {
        running = false;
        if (connection != null) {
            if (usbInterface != null) connection.releaseInterface(usbInterface);
            connection.close();
            connection = null;
        }
    }
}
