package com.quan404.dualcamerapreview;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.UVCCameraTextureView;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DualCameraPreview extends Activity implements CameraDialog.CameraDialogParent{

    // for debugging
    private static String TAG = "SingleCameraPreview";
    private static boolean DEBUG = true;

    // for thread pool
    private static final int CORE_POOL_SIZE = 1;		// initial/minimum threads
    private static final int MAX_POOL_SIZE = 4;			// maximum threads
    private static final int KEEP_ALIVE_TIME = 10;		// time periods while keep the idle thread
    protected static final ThreadPoolExecutor EXECUTER
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;

    private UVCCamera mCameraLeft = null;
    private UVCCameraTextureView mUVCCameraViewLeft;
    private Surface mPreviewSurfaceLeft;

    private UVCCamera mCameraRight = null;
    private UVCCameraTextureView mUVCCameraViewRight;
    private Surface mPreviewSurfaceRight;

    private int SELECTED_ID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dual_camera_preview);

        hideNavigationBar();

        mUVCCameraViewLeft = (UVCCameraTextureView) findViewById(R.id.cameraView01);
        mUVCCameraViewLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCameraLeft == null) {
                    CameraDialog.showDialog(DualCameraPreview.this);
                    SELECTED_ID = 0;
                } else {
                    releaseUVCCamera(0);
                }
            }
        });

        mUVCCameraViewRight = (UVCCameraTextureView) findViewById(R.id.cameraView02);
        mUVCCameraViewRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCameraRight == null) {
                    CameraDialog.showDialog(DualCameraPreview.this);
                    SELECTED_ID = 1;
                } else {
                    releaseUVCCamera(1);
                }
            }
        });


        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mUSBMonitor.register();
        if (mCameraLeft != null)
            mCameraLeft.startPreview();

        if (mCameraRight != null)
            mCameraRight.startPreview();
    }

    @Override
    protected void onPause() {
        mUSBMonitor.unregister();
        if (mCameraLeft != null)
            mCameraLeft.stopPreview();
        if (mCameraRight != null)
            mCameraRight.stopPreview();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(mUSBMonitor != null){
            mUSBMonitor.destroy();
        }
        if (mCameraLeft != null)
            mCameraLeft.destroy();

        if (mCameraRight != null)
            mCameraRight.destroy();

        releaseUVCCamera(2);

        super.onDestroy();
    }

    private USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onAttach:" + device);
            Toast.makeText(DualCameraPreview.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDettach(UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDetach:" + device);
            Toast.makeText(DualCameraPreview.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            if(mCameraLeft != null && mCameraRight != null) return;

            if (DEBUG) Log.v(TAG, "onConnect: " + device);

            final UVCCamera camera = new  UVCCamera();
            final int current_id = SELECTED_ID;
            SELECTED_ID = -1;
            EXECUTER.execute(new Runnable() {
                @Override
                public void run() {
                    // Open Camera
                    camera.open(ctrlBlock);


                    // Set Preview Mode
                    try {
                        if (DEBUG) Log.v(TAG, "MJPEG MODE");
                        camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG, 0.5f);
                    } catch (IllegalArgumentException e1) {
                        e1.printStackTrace();

                        if (DEBUG) Log.v(TAG, "PREVIEW MODE");
                        try {
                            camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE, 0.5f);
                        } catch (IllegalArgumentException e2) {
                            if (DEBUG) Log.v(TAG, "CAN NOT ENTER PREVIEW MODE");
                            camera.destroy();
                            e2.printStackTrace();
                        }
                    }

                    // Start Preview
                    if (mCameraLeft == null && current_id == 0) {
                        mCameraLeft = camera;
                        if (mPreviewSurfaceLeft != null) {
                            if (DEBUG) Log.v(TAG, "mPreviewSurface.release()");
                            mPreviewSurfaceLeft.release();
                            mPreviewSurfaceLeft = null;
                        }

                        final SurfaceTexture st = mUVCCameraViewLeft.getSurfaceTexture();
                        if (st != null) {
                            if (DEBUG) Log.v(TAG, "mPreviewSurface = new Surface(st);");
                            mPreviewSurfaceLeft = new Surface(st);
                        }

                        camera.setPreviewDisplay(mPreviewSurfaceLeft);
                        camera.startPreview();
                    }

                    if (mCameraRight == null  && current_id == 1) {
                        mCameraRight = camera;
                        if (mPreviewSurfaceRight != null) {
                            if (DEBUG) Log.v(TAG, "mPreviewSurface.release()");
                            mPreviewSurfaceRight.release();
                            mPreviewSurfaceRight = null;
                        }

                        final SurfaceTexture st = mUVCCameraViewRight.getSurfaceTexture();
                        if (st != null) {
                            if (DEBUG) Log.v(TAG, "mPreviewSurface = new Surface(st);");
                            mPreviewSurfaceRight = new Surface(st);
                        }

                        camera.setPreviewDisplay(mPreviewSurfaceRight);
                        camera.startPreview();
                    }


                }
            });

        }

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
            if(DEBUG) Log.v(TAG, "onDisconnect" + device);
            if(mCameraLeft != null && device.equals(mCameraLeft.getDevice())){
                releaseUVCCamera(0);
            }
            if(mCameraRight != null && device.equals(mCameraRight.getDevice())){
                releaseUVCCamera(1);
            }
        }

        @Override
        public void onCancel() {

        }
    };

    private void releaseUVCCamera(int id){
        if(DEBUG) Log.v(TAG, "releaseUVCCamera");

        if(id == 0 || id == 2){
            mCameraLeft.close();

            if (mPreviewSurfaceLeft != null){
                mPreviewSurfaceLeft.release();
                mPreviewSurfaceLeft = null;
            }
            mCameraLeft.destroy();
            mCameraLeft = null;
        }
        if(id == 1 || id == 2){
            mCameraRight.close();

            if (mPreviewSurfaceRight != null){
                mPreviewSurfaceRight.release();
                mPreviewSurfaceRight = null;
            }
            mCameraRight.destroy();
            mCameraRight = null;
        }
        SELECTED_ID = -1;
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }


    // for UI fullscreen

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(hasFocus)
            hideNavigationBar();
    }

    private void hideNavigationBar() {
        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setSystemUiVisibility(uiOptions);
    }
}
