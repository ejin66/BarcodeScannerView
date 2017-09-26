package com.ejin.sample.camera;

import android.hardware.Camera;
import android.text.TextUtils;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by j17420 on 2017/8/25.
 */

public class DecodeHelper {

	private static final String TAG = "DecodeHelper";

	private static final int MAX_THREAD_COUNT = 15;
	private CameraManager mCameraManager;
	private MultiFormatReader reader;
	private Map<DecodeHintType, Object> map;
	private DecodeListener onCodeReadListener;
	private ExecutorService threadPool;
	private boolean isDecodeSuccess;
	private boolean isStopped;
	private AtomicInteger currentThreadCount;

	public DecodeHelper(CameraManager manager) {
		mCameraManager = manager;
		currentThreadCount = new AtomicInteger();
		threadPool = Executors.newFixedThreadPool(MAX_THREAD_COUNT);

		reader = new MultiFormatReader();
		map = new HashMap<>();
		setDefaultHints();
		reader.setHints(map);
	}

	public void setCodeReadListener(DecodeListener listener) {
		onCodeReadListener = listener;
	}

	public void startShotFrame() {
		threadPool.submit(new Runnable() {
			@Override
			public void run() {
				while (!(isDecodeSuccess || isStopped)) {
					if (currentThreadCount.get() < MAX_THREAD_COUNT + 5) {
						requestOneShotFrame();
					}
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	private void requestOneShotFrame() {
		mCameraManager.requestOneShotFrame(new Camera.PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				decode(data);
			}
		});
	}

	private void decode(final byte[] frameData) {
		if (isDecodeSuccess || isStopped || onCodeReadListener == null) {
			return;
		}
		currentThreadCount.incrementAndGet();
		threadPool.submit(new Runnable() {
			@Override
			public void run() {
				String result = decodeData(frameData);
				if (!TextUtils.isEmpty(result)) {
					callback(result);
				}
				currentThreadCount.decrementAndGet();
			}
		});
	}

	private void callback(String s) {
		if (isDecodeSuccess || isStopped) {
			return;
		}
		isDecodeSuccess = true;
		onCodeReadListener.onRead(s);
		stop();
	}

	public void stop() {
		if (isStopped) {
			return;
		}
		isStopped = true;
		threadPool.shutdownNow();
		System.gc();
	}

	private void setDefaultHints() {
		List<BarcodeFormat> formatList = new ArrayList<>();
		formatList.addAll(DecodeFormatManager.QR_CODE_FORMATS);
		formatList.addAll(DecodeFormatManager.PRODUCT_FORMATS);
		formatList.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
		map.put(DecodeHintType.POSSIBLE_FORMATS, formatList);
	}

	private String decodeData(final byte[] frameData) {
//		Log.i(TAG, "decode begin");
		int width = mCameraManager.getPreviewSize().x;
		int height = mCameraManager.getPreviewSize().y;
		byte[] data = frameData;

		//portrait
		if (width < height) {
			byte[] rotatedData = new byte[data.length];
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++)
					rotatedData[y * width + width - x - 1] = data[y + x * height];
			}
			data = rotatedData;
		}

		PlanarYUVLuminanceSource source = mCameraManager.buildLuminanceSource(data, width, height);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			Result result = reader.decodeWithState(bitmap);
			return result.getText();
		} catch (ReaderException re) {
//			Log.i(TAG, "decode exception");
		} finally {
			reader.reset();
//			Log.i(TAG, "decode end");
		}
		return null;
	}

}
