package com.google.zxing.client.android.camera;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by j17420 on 2017/7/13.
 */

public class DecodeHandler extends Handler {

	private CameraManager cameraManager;
	private MultiFormatReader reader;
	private OnCodeReadListener onCodeReadListener;
	private boolean isRunning;
	private final String TAG = DecodeHandler.class.getSimpleName();
	private Map<DecodeHintType,Object> map;

	public static DecodeHandler getInstance(CameraManager cameraManager) {
		DecodeThread decodeThread = new DecodeThread(cameraManager);
		decodeThread.start();
		return decodeThread.getHandler();
	}

	private DecodeHandler(CameraManager cameraManager) {
		super();
		isRunning = true;
		this.cameraManager = cameraManager;
		reader = new MultiFormatReader();
		map = new HashMap<>();
		setDefaultHints();
		reader.setHints(map);
	}

	private void setDefaultHints() {
		List<BarcodeFormat> formatList = new ArrayList<>();
		formatList.addAll(DecodeFormatManager.QR_CODE_FORMATS);
		formatList.addAll(DecodeFormatManager.PRODUCT_FORMATS);
		formatList.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
		map.put(DecodeHintType.POSSIBLE_FORMATS , formatList);
	}

	public void setOnCodeReadListener(OnCodeReadListener onCodeReadListener) {
		this.onCodeReadListener = onCodeReadListener;
	}

	public void justQrCodeEnable() {
		map.clear();
		List<BarcodeFormat> formatList = new ArrayList<>();
		formatList.addAll(DecodeFormatManager.QR_CODE_FORMATS);
		map.put(DecodeHintType.POSSIBLE_FORMATS , formatList);
		reader.setHints(map);
	}

	public void requestOneShotFrame() {
		if (isStop()) {
			return;
		}
		cameraManager.requestOneShotFrame(this);
	}

	public synchronized void stop() {
		if (isStop()) {
			return;
		}
		isRunning = false;
		sendEmptyMessage(MessageState.QUIT);
	}

	public boolean isStop() {
		return !isRunning;
	}

	@Override
	public void handleMessage(Message msg) {
		if (isStop()) {
			return;
		}
		switch (msg.what) {
			case MessageState.DECODE:
				//decode
				String result = decode((byte[]) msg.obj);

				if (TextUtils.isEmpty(result)) {
					requestOneShotFrame();
					return;
				}

				if (onCodeReadListener != null) {
					onCodeReadListener.onCodeRead(result);
				}

				break;
			case MessageState.ERROR:
				requestOneShotFrame();
				break;
			case MessageState.QUIT:
				removeCallbacksAndMessages(null);
				Looper.myLooper().quit();
				break;
		}
	}

	private String decode(final byte[] frameData) {
		Log.d("Decode","----begin");
		int width = cameraManager.getPreviewSize().x;
		int height = cameraManager.getPreviewSize().y;
		byte[] data = frameData;

		// portrait
		if (width < height) {
			byte[] rotatedData = new byte[data.length];
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++)
					rotatedData[y * width + width - x - 1] = data[y + x * height];
			}
			data = rotatedData;
		}

		PlanarYUVLuminanceSource source = cameraManager.buildLuminanceSource(data, width, height);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			Result result = reader.decodeWithState(bitmap);
			return result.getText();
		} catch (ReaderException re) {
			Log.d(TAG, "ReaderException");
		} finally {
			reader.reset();
			Log.d("Decode","----end");
		}
		return null;
	}

	public interface OnCodeReadListener {

		void onCodeRead(String text);
	}

	private static class DecodeThread extends Thread {

		DecodeHandler decodeHandler;
		CountDownLatch countDownLatch;
		CameraManager cameraManager;

		public DecodeThread(CameraManager cameraManager) {
			this.cameraManager = cameraManager;
			countDownLatch = new CountDownLatch(1);
		}

		@Override
		public void run() {
			Looper.prepare();
			decodeHandler = new DecodeHandler(cameraManager);
			countDownLatch.countDown();
			Looper.loop();
		}

		DecodeHandler getHandler() {
			try {
				countDownLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			return decodeHandler;
		}
	}
}
