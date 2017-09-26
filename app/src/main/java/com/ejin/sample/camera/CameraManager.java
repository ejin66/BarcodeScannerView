/*
 * Copyright (C) 2008 ZXing authors
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

package com.ejin.sample.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import com.ejin.sample.camera.open.OpenCamera;
import com.ejin.sample.camera.open.OpenCameraInterface;
import com.google.zxing.PlanarYUVLuminanceSource;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

	private static final String TAG = CameraManager.class.getSimpleName();

	private final CameraConfigurationManager configManager;
	private OpenCamera openCamera;
	private AutoFocusManager autoFocusManager;
	private boolean initialized;
	private boolean previewing;
	private int displayOrientation = 0;
	private Rect framingRect, framingRectInPreview;

	// PreviewCallback references are also removed from original ZXING authors work,
	// since we're using our own interface.
	// FramingRects references are also removed from original ZXING authors work,
	// since We're using all view size while detecting QR-Codes.
	private int requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA;
	private long autofocusIntervalInMs = AutoFocusManager.DEFAULT_AUTO_FOCUS_INTERVAL_MS;

	/**
	 * 扫描的类型
	 * bar code:长方形
	 * qr code:正方形
	 */
	public static final int SCANNER_BAR_CODE = 1;
	public static final int SCANNER_QR_CODE = 2;
	private int scannerType = SCANNER_QR_CODE;


	public CameraManager(Context context) {
		this.configManager = new CameraConfigurationManager(context);
	}

	public synchronized void requestOneShotFrame(Camera.PreviewCallback callback) {
		if (isOpen()) {
			openCamera.getCamera().setOneShotPreviewCallback(callback);
		}
	}

	public synchronized void setPreviewCallback(Camera.PreviewCallback callback) {
		if (isOpen()) {
			openCamera.getCamera().setPreviewCallback(callback);
		}
	}

	public void setDisplayOrientation(int degrees) {
		this.displayOrientation = degrees;

		if (isOpen()) {
			openCamera.getCamera().setDisplayOrientation(degrees);
		}
	}

	public void setAutofocusInterval(long autofocusIntervalInMs) {
		this.autofocusIntervalInMs = autofocusIntervalInMs;
		if (autoFocusManager != null) {
			autoFocusManager.setAutofocusInterval(autofocusIntervalInMs);
		}
	}

	public void forceAutoFocus() {
		if (autoFocusManager != null) {
			autoFocusManager.start();
		}
	}

	public Point getPreviewSize() {
		return configManager.getPreviewSizeOnScreen();
	}

	/**
	 * Opens the camera driver and initializes the hardware parameters.
	 *
	 * @param holder The surface object which the camera will draw preview frames into.
	 * @param height @throws IOException Indicates the camera driver failed to open.
	 */
	public synchronized void openDriver(SurfaceHolder holder, int width, int height)
			throws IOException {
		OpenCamera theCamera = openCamera;
		if (!isOpen()) {
			theCamera = OpenCameraInterface.open(requestedCameraId);
			if (theCamera == null || theCamera.getCamera() == null) {
				throw new IOException("Camera.open() failed to return object from driver");
			}
			openCamera = theCamera;
		}
		theCamera.getCamera().setPreviewDisplay(holder);
		theCamera.getCamera().setDisplayOrientation(displayOrientation);

		if (!initialized) {
			initialized = true;
			configManager.initFromCameraParameters(theCamera, width, height);
		}

		Camera cameraObject = theCamera.getCamera();
		Camera.Parameters parameters = cameraObject.getParameters();
		String parametersFlattened =
				parameters == null ? null : parameters.flatten(); // Save these, temporarily
		try {
			configManager.setDesiredCameraParameters(theCamera, false);
		} catch (RuntimeException re) {
			// Driver failed
			Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
			Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
			// Reset:
			if (parametersFlattened != null) {
				parameters = cameraObject.getParameters();
				parameters.unflatten(parametersFlattened);
				try {
					cameraObject.setParameters(parameters);
					configManager.setDesiredCameraParameters(theCamera, true);
				} catch (RuntimeException re2) {
					// Well, darn. Give up
					Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
				}
			}
		}
		cameraObject.setPreviewDisplay(holder);
	}

	/**
	 * Allows third party apps to specify the camera ID, rather than determine
	 * it automatically based on available cameras and their orientation.
	 *
	 * @param cameraId camera ID of the camera to use. A negative value means "no preference".
	 */
	public synchronized void setPreviewCameraId(int cameraId) {
		requestedCameraId = cameraId;
	}

	public int getPreviewCameraId() {
		return requestedCameraId;
	}

	/**
	 * @param enabled if {@code true}, light should be turned on if currently off. And vice versa.
	 */
	public synchronized void setTorchEnabled(boolean enabled) {
		OpenCamera theCamera = openCamera;
		if (theCamera != null && enabled != configManager.getTorchState(theCamera.getCamera())) {
			boolean wasAutoFocusManager = autoFocusManager != null;
			if (wasAutoFocusManager) {
				autoFocusManager.stop();
				autoFocusManager = null;
			}
			configManager.setTorchEnabled(theCamera.getCamera(), enabled);
			if (wasAutoFocusManager) {
				autoFocusManager = new AutoFocusManager(theCamera.getCamera());
				autoFocusManager.start();
			}
		}
	}

	public synchronized boolean isOpen() {
		return openCamera != null && openCamera.getCamera() != null;
	}

	/**
	 * Closes the camera driver if still in use.
	 */
	public synchronized void closeDriver() {
		if (isOpen()) {
			openCamera.getCamera().release();
			openCamera = null;
			// Make sure to clear these each time we close the camera, so that any scanning rect
			// requested by intent is forgotten.
			// framingRect = null;
			// framingRectInPreview = null;
		}
	}

	/**
	 * Asks the camera hardware to begin drawing preview frames to the screen.
	 */
	public synchronized void startPreview() {
		OpenCamera theCamera = openCamera;
		if (theCamera != null && !previewing) {
			theCamera.getCamera().startPreview();
			previewing = true;
			autoFocusManager = new AutoFocusManager(theCamera.getCamera());
			autoFocusManager.setAutofocusInterval(200);
		}
	}

	/**
	 * Tells the camera to stop drawing preview frames.
	 */
	public synchronized void stopPreview() {
		if (autoFocusManager != null) {
			autoFocusManager.stop();
			autoFocusManager = null;
		}
		if (openCamera != null && previewing) {
			openCamera.getCamera().stopPreview();
			previewing = false;
		}
	}

	/**
	 * A factory method to build the appropriate LuminanceSource object based on the format
	 * of the preview buffers, as described by Camera.Parameters.
	 *
	 * @param data   A preview frame.
	 * @param width  The width of the image.
	 * @param height The height of the image.
	 * @return A PlanarYUVLuminanceSource instance.
	 */
	public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
		Rect rect = getFramingRectInPreview();
		return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
				rect.width(), rect.height(), false);
//		return new PlanarYUVLuminanceSource(data, width, height, 0, 0,
//				width, height, false);
	}

	/**
	 * Calculates the framing rect which the UI should draw to show the user where to place the
	 * barcode. This target helps with alignment as well as forces the user to hold the device
	 * far enough away to ensure the image will be in focus.
	 *
	 * @return The rectangle to draw on screen in window coordinates.
	 */
	public Rect getFramingRect() {
		Point screenResolution = configManager.getScreenResolution();
		if (framingRect == null) {
			if (openCamera == null) {
				return null;
			}
			//正方形
			int width = Math.min(screenResolution.x, screenResolution.y) / 2;
			int height = width;
			//长方形，为条形码定制
			if (scannerType == SCANNER_BAR_CODE) {
				width = screenResolution.x * 2 / 3;
				height = width / 4;
			}
			int leftOffset = (screenResolution.x - width) / 2;
			int topOffset = (screenResolution.y - height) / 2;
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
			Log.d(TAG, "framingRect: " + framingRect);
		}
		return framingRect;
	}


	/**
	 * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
	 * not UI / screen.
	 *
	 * @return {@link Rect} expressing barcode scan area in terms of the preview size
	 */
	public synchronized Rect getFramingRectInPreview() {
		if (framingRectInPreview == null) {
			Rect framingRect = getFramingRect();
			if (framingRect == null) {
				return null;
			}
			Rect rect = new Rect(framingRect);
			Point previewSizeOnScreen = configManager.getPreviewSizeOnScreen();
			Point screenResolution = configManager.getScreenResolution();
			if (previewSizeOnScreen == null || screenResolution == null) {
				// Called early, before init even finished
				return null;
			}
			Log.d(TAG, "previewSizeOnScreen: " + previewSizeOnScreen);
			Log.d(TAG, "screenResolution: " + screenResolution);


			rect.left = rect.left * previewSizeOnScreen.x / screenResolution.x;
			rect.right = rect.right * previewSizeOnScreen.x / screenResolution.x;
			rect.top = rect.top * previewSizeOnScreen.y / screenResolution.y;
			rect.bottom = rect.bottom * previewSizeOnScreen.y / screenResolution.y;

			framingRectInPreview = rect;
			Log.d(TAG, "getFramingRectInPreview: " + framingRectInPreview);
		}

		return framingRectInPreview;
	}

	public void switchScannerType(int scannerType) {
		this.scannerType = scannerType;
		framingRectInPreview = null;
		framingRect = null;
	}
}
