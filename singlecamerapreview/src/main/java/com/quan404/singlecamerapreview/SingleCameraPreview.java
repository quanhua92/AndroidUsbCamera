package com.quan404.singlecamerapreview;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;

public class SingleCameraPreview extends Activity implements CameraDialog.CameraDialogParent {

    // for debugging
    private static String TAG = "SingleCameraPreview";
    private static boolean DEBUG = true;

    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_camera_preview);

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mUSBMonitor.register();
    }

    @Override
    protected void onPause() {
        mUSBMonitor.unregister();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(mUSBMonitor != null){
            mUSBMonitor.destroy();
        }


        super.onDestroy();
    }

    private USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onAttach:" + device);
            Toast.makeText(SingleCameraPreview.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDettach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDetach:" + device);
            Toast.makeText(SingleCameraPreview.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {

        }

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {

        }

        @Override
        public void onCancel() {

        }
    };

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }
}
