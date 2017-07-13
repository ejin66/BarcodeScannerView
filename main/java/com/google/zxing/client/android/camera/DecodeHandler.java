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
	private MultiFormatReader multiFormatReader;
	private OnQRCodeReadListener onQRCodeReadListener;
	private boolean isRunning;
	private final String TAG = DecodeHandler.class.getSimpleName();

	public static DecodeHandler getInstance(CameraManager cameraManager) {
		DecodeThread decodeThread = new DecodeThread(cameraManager);
		decodeThread.start();
		return decodeThread.getHandler();
	}

	private DecodeHandler(CameraManager cameraManager) {
		super();
		isRunning = true;
		this.cameraManager = cameraManager;
		multiFormatReader = new MultiFormatReader();

		multiFormatReader.setHints(getDefaultHints());
	}

	private Map<DecodeHintType,Object> getDefaultHints() {
		Map<DecodeHintType,Object> map = new HashMap<DecodeHintType, Object>(20);

		List<BarcodeFormat> formatList = new ArrayList<>();
		formatList.addAll(DecodeFormatManager.PRODUCT_FORMATS);
		formatList.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
		formatList.addAll(DecodeFormatManager.QR_CODE_FORMATS);
		map.put(DecodeHintType.POSSIBLE_FORMATS , formatList);

		return map;
	}

	public void setOnQRCodeReadListener(OnQRCodeReadListener onQRCodeReadListener) {
		this.onQRCodeReadListener = onQRCodeReadListener;
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

				if (onQRCodeReadListener != null) {
					onQRCodeReadListener.onQRCodeRead(result);
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
		if (width < height) {
			// portrait
			byte[] rotatedData = new byte[data.length];
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++)
					rotatedData[y * width + width - x - 1] = data[y + x * height];
			}
			data = rotatedData;
		}
		final PlanarYUVLuminanceSource source = cameraManager.buildLuminanceSource(data, width, height);
		final HybridBinarizer hybBin = new HybridBinarizer(source);
		final BinaryBitmap bitmap = new BinaryBitmap(hybBin);
		try {
			Result result = multiFormatReader.decodeWithState(bitmap);
			return result.getText();
		} catch (ReaderException re) {
			Log.d(TAG, "ReaderException");
		} finally {
			multiFormatReader.reset();
			Log.d("Decode","----end");
		}
		return null;
	}

	public interface OnQRCodeReadListener {

		void onQRCodeRead(String text);
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
