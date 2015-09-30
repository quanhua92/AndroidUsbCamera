package com.quan404.singlecamerapreview;

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

public class SingleCameraPreview extends Activity implements CameraDialog.CameraDialogParent {

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
    private UVCCamera mCamera = null;
    private UVCCameraTextureView mUVCCameraView;
    private Surface mPreviewSurface;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_camera_preview);

        mUVCCameraView = (UVCCameraTextureView) findViewById(R.id.UVCCameraTextureView);
        mUVCCameraView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH * 1.0f / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mUVCCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCamera == null) {
                    CameraDialog.showDialog(SingleCameraPreview.this);
                } else {
                    releaseUVCCamera();
                }
            }
        });
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mUSBMonitor.register();
        if (mCamera != null)
            mCamera.startPreview();
    }

    @Override
    protected void onPause() {
        mUSBMonitor.unregister();
        if (mCamera != null)
            mCamera.stopPreview();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(mUSBMonitor != null){
            mUSBMonitor.destroy();
        }
        if (mCamera != null)
            mCamera.destroy();


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
        public void onConnect(UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            if(mCamera != null) return;

            if (DEBUG) Log.v(TAG, "onConnect: " + device);

            final UVCCamera camera = new  UVCCamera();

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
//                            camera.destroy();
                            releaseUVCCamera();
                            e2.printStackTrace();
                        }
                    }

                    // Start Preview
                    if (mCamera == null) {
                        mCamera = camera;
                        if (mPreviewSurface != null) {
                            if (DEBUG) Log.v(TAG, "mPreviewSurface.release()");
                            mPreviewSurface.release();
                            mPreviewSurface = null;
                        }

                        final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                        if (st != null) {
                            if (DEBUG) Log.v(TAG, "mPreviewSurface = new Surface(st);");
                            mPreviewSurface = new Surface(st);
                        }

                        camera.setPreviewDisplay(mPreviewSurface);
                        camera.startPreview();
                    }
                }
            });

        }

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
            if(DEBUG) Log.v(TAG, "onDisconnect" + device);
            if(mCamera != null && device.equals(mCamera.getDevice())){
                releaseUVCCamera();
            }
        }

        @Override
        public void onCancel() {

        }
    };

    private void releaseUVCCamera(){
        if(DEBUG) Log.v(TAG, "releaseUVCCamera");
        mCamera.close();

        if (mPreviewSurface != null){
            mPreviewSurface.release();
            mPreviewSurface = null;
        }
        mCamera.destroy();
        mCamera = null;
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }
}
