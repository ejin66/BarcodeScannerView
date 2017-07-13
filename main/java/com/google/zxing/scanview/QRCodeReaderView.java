/*
 * Copyright 2014 David Lázaro Esparcia.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.zxing.scanview;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.zxing.client.android.camera.BeepManager;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.camera.DecodeHandler;

import java.io.IOException;

import static android.hardware.Camera.getCameraInfo;

/**
 * QRCodeReaderView Class which uses ZXING lib and let you easily integrate a QR decoder view.
 * Take some classes and made some modifications in the original ZXING - Barcode Scanner project.
 *
 * @author David Lázaro
 */
public class QRCodeReaderView extends SurfaceView
		implements SurfaceHolder.Callback {


	private static final String TAG = QRCodeReaderView.class.getName();
	protected CameraManager mCameraManager;
	private BeepManager beepManager;
	private DecodeHandler decodeHandler;
	private DecodeHandler.OnQRCodeReadListener onQRCodeReadListener;

	protected QRCodeReaderView(Context context) {
		this(context, null);
	}

	protected QRCodeReaderView(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (isInEditMode()) {
			return;
		}

		if (checkCameraHardware()) {
			mCameraManager = new CameraManager(getContext());
			beepManager = new BeepManager(context, false, true);
			getHolder().addCallback(this);
			setBackCamera();
		} else {
			throw new RuntimeException("Error: Camera not found");
		}
	}

	/**
	 * Set the callback to return decoding result
	 *
	 * @param onQRCodeReadListener the listener
	 */
	public void setOnQRCodeReadListener(final DecodeHandler.OnQRCodeReadListener onQRCodeReadListener) {
		this.onQRCodeReadListener = new DecodeHandler.OnQRCodeReadListener() {
			@Override
			public void onQRCodeRead(String text) {
				beepManager.playBeepSoundAndVibrate();
				onQRCodeReadListener.onQRCodeRead(text);
			}
		};
	}

	/**
	 * Starts camera preview and decoding
	 */
	public void startCamera() {
		mCameraManager.startPreview();
		beepManager.init();
		if (decodeHandler == null || decodeHandler.isStop()) {
			decodeHandler = DecodeHandler.getInstance(mCameraManager);
			decodeHandler.setOnQRCodeReadListener(onQRCodeReadListener);
		}
		Log.d(TAG, "startCamera");
	}

	/**
	 * Stop camera preview and decoding
	 */
	public void stopCamera() {
		mCameraManager.stopPreview();
		beepManager.close();
		if (decodeHandler != null) {
			decodeHandler.stop();
		}
		Log.d(TAG, "stopCamera");
	}

	/**
	 * Set Camera autofocus interval value
	 * default value is 5000 ms.
	 *
	 * @param autofocusIntervalInMs autofocus interval value
	 */
	public void setAutofocusInterval(long autofocusIntervalInMs) {
		if (mCameraManager != null) {
			mCameraManager.setAutofocusInterval(autofocusIntervalInMs);
		}
	}

	/**
	 * Trigger an auto focus
	 */
	public void forceAutoFocus() {
		if (mCameraManager != null) {
			mCameraManager.forceAutoFocus();
		}
	}

	/**
	 * Set Torch enabled/disabled.
	 * default value is false
	 *
	 * @param enabled torch enabled/disabled.
	 */
	public void setTorchEnabled(boolean enabled) {
		if (mCameraManager != null) {
			mCameraManager.setTorchEnabled(enabled);
		}
	}

	/**
	 * Allows user to specify the camera ID, rather than determine
	 * it automatically based on available cameras and their orientation.
	 *
	 * @param cameraId camera ID of the camera to use. A negative value means "no preference".
	 */
	public void setPreviewCameraId(int cameraId) {
		mCameraManager.setPreviewCameraId(cameraId);
	}

	/**
	 * Camera preview from device back camera
	 */
	public void setBackCamera() {
		setPreviewCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
	}

	/**
	 * Camera preview from device front camera
	 */
	public void setFrontCamera() {
		setPreviewCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}

	/****************************************************
	 * SurfaceHolder.Callback,Camera.PreviewCallback
	 ****************************************************/

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated");

		try {
			// Indicate camera, our View dimensions
			mCameraManager.openDriver(holder, this.getWidth(), this.getHeight());
		} catch (IOException e) {
			Log.w(TAG, "Can not openDriver: " + e.getMessage());
			mCameraManager.closeDriver();
		}

		try {
//      mQRCodeReader = new QRCodeReader();

			mCameraManager.startPreview();
		} catch (Exception e) {
			Log.e(TAG, "Exception: " + e.getMessage());
			mCameraManager.closeDriver();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(TAG, "surfaceChanged");

		if (holder.getSurface() == null) {
			Log.e(TAG, "Error: preview surface does not exist");
			return;
		}

		if (mCameraManager.getPreviewSize() == null) {
			Log.e(TAG, "Error: preview size does not exist");
			return;
		}

		mCameraManager.stopPreview();

		// Fix the camera sensor rotation

		mCameraManager.setDisplayOrientation(getCameraDisplayOrientation());

		mCameraManager.startPreview();
		if (decodeHandler != null) {
			decodeHandler.requestOneShotFrame();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");

		mCameraManager.stopPreview();
		mCameraManager.closeDriver();
	}

	/**
	 * Check if this device has a camera
	 */
	private boolean checkCameraHardware() {
		if (getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else if (getContext().getPackageManager()
				.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
			// this device has a front camera
			return true;
		} else {
			// this device has any camera
			return getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
		}
	}

	/**
	 * Fix for the camera Sensor on some devices (ex.: Nexus 5x)
	 */
	@SuppressWarnings("deprecation")
	private int getCameraDisplayOrientation() {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.GINGERBREAD) {
			return 90;
		}

		Camera.CameraInfo info = new Camera.CameraInfo();
		getCameraInfo(mCameraManager.getPreviewCameraId(), info);
		WindowManager windowManager =
				(WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		int rotation = windowManager.getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
			default:
				break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		return result;
	}

}
