package com.google.zxing.client.android.camera;

import android.hardware.Camera;

/**
 * Created by j17420 on 2017/7/13.
 */

public class PreviewCallback implements Camera.PreviewCallback {

	private CameraConfigurationManager configurationManager;
	private DecodeHandler decodeHandler;
	private int message;

	public void setHandler(DecodeHandler handler, int message) {
		this.decodeHandler = handler;
		this.message = message;
	}

	PreviewCallback(CameraConfigurationManager configurationManager) {
		this.configurationManager = configurationManager;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (decodeHandler.isStop()) {
			return;
		}
		decodeHandler.obtainMessage(message, data).sendToTarget();
	}
}
