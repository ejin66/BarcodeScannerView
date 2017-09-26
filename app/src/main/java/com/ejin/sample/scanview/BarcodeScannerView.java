package com.ejin.sample.scanview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.ejin.sample.R;
import com.ejin.sample.camera.DecodeListener;


/**
 * Created by j17420 on 2017/7/12.
 */

public class BarcodeScannerView extends FrameLayout {

	QRCodeReaderView qrCodeView;
	private int maskColor = Color.parseColor("#60000000");
	private int cornerColor;
	private int scannerLineColor;
	private float scannerSpeed;
	private boolean isTorchEnable;

	private Rect lastRect, currentRect, finalRect;

	private static final int STATUS_1 = 1;
	private static final int STATUS_2 = 2;
	private int status = STATUS_1;

	/**
	 * 角的宽度
	 */
	private final int CORNER_WIDTH = 10;
	/**
	 * 角的长度
	 */
	private final int CORNER_LENGTH = 50;

	/**
	 * 扫描线移动的速度
	 */
	private int MAX_SPEED_DISTANCE = 10;

	/**
	 * 扫描框中的中间线的宽度
	 */
	private static final int MIDDLE_LINE_WIDTH = 5;

	/**
	 * 扫描框中的中间线的与扫描框左右的间隙
	 */
	private static final int MIDDLE_LINE_PADDING = 15;

	/**
	 * 扫描线距离top的高度
	 */
	private int slideTop;

	private boolean isShowScannerLine = true;

	/**
	 * 刷新界面的时间
	 */
	private static final long ANIMATION_DELAY = 20L;

	/**
	 * 自动变换的间隔时间
	 */
	private static final long TOGGLE_INTERVAL_TIME = 2500;

	/**
	 * 变换动画的时间
	 */
	private static final long ANIMATION_DURATION = 1000;

	private Runnable toggleRunnable = new Runnable() {
		@Override
		public void run() {
			toggle();
			qrCodeView.postDelayed(this, TOGGLE_INTERVAL_TIME);
		}
	};

	private ValueAnimator valueAnimator;

	public BarcodeScannerView(@NonNull Context context) {
		this(context, null);
	}

	public BarcodeScannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BarcodeScannerView);
		cornerColor = typedArray.getColor(R.styleable.BarcodeScannerView_corner_color, Color.RED);
		scannerLineColor = typedArray.getColor(R.styleable.BarcodeScannerView_scanner_line_color, Color.RED);
		scannerSpeed = typedArray.getFloat(R.styleable.BarcodeScannerView_scanner_speed, 0.5f);
		if (scannerSpeed > 1) {
			scannerSpeed = 1;
		}
		if (scannerSpeed < 0.2) {
			scannerSpeed = 0.2f;
		}
		typedArray.recycle();
	}

	public BarcodeScannerView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		addQRCoderView();
	}

	private void addQRCoderView() {
		qrCodeView = new QRCodeReaderView(getContext());
		qrCodeView.setBackCamera();
		addView(qrCodeView);

		MaskView maskView = new MaskView(getContext());
		addView(maskView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}

	public void justEnableQrcode() {
		qrCodeView.justEnableQrcode();
	}

	public QRCodeReaderView getQrCodeReaderView() {
		return qrCodeView;
	}

	public void setOnCodeReadListener(DecodeListener listener) {
		qrCodeView.setOnCodeReadListener(listener);
	}

	public void startCamera() {
		qrCodeView.startCamera();
	}

	public void stopCamera() {
		qrCodeView.stopCamera();
		qrCodeView.removeCallbacks(toggleRunnable);
	}

	public void setTorchEnabled(boolean enable) {
		isTorchEnable = enable;
		qrCodeView.setTorchEnabled(enable);
	}

	public void toggleTorch() {
		setTorchEnabled(!isTorchEnable);
	}

	public void setScannerSpeed(float speed) {
		scannerSpeed = speed;
	}

	public void setQrCodeScanner() {
		qrCodeView.setQrCodeScanner();
	}

	public void setBarCodeScanner() {
		qrCodeView.setBarCodeScanner();
	}

	public void autoSwitchScaner() {
		showScannerLine(false);
		qrCodeView.postDelayed(toggleRunnable, 1000);
	}

	public void showScannerLine(boolean show) {
		isShowScannerLine = show;
	}

	private void toggle() {
		lastRect = qrCodeView.mCameraManager.getFramingRect();
		switch (status) {
			case STATUS_1:
				status = STATUS_2;
				setBarCodeScanner();
				break;
			case STATUS_2:
				status = STATUS_1;
				setQrCodeScanner();
				break;
		}
		finalRect = qrCodeView.mCameraManager.getFramingRect();
		if (valueAnimator == null) {
			valueAnimator = ValueAnimator.ofFloat(0, 1);
			valueAnimator.setDuration(ANIMATION_DURATION);
			valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					float value = (float) animation.getAnimatedValue();
					calculateRect(value);
				}
			});
		}
		valueAnimator.start();
	}

	private void calculateRect(float f) {
		if (lastRect == null || currentRect == null || finalRect == null) {
			return;
		}
		int left = (int) (lastRect.left + (finalRect.left - lastRect.left) * f);
		int top = (int) (lastRect.top + (finalRect.top - lastRect.top) * f);
		int right = (int) (lastRect.right + (finalRect.right - lastRect.right) * f);
		int bottom = (int) (lastRect.bottom + (finalRect.bottom - lastRect.bottom) * f);
		currentRect.set(left, top, right, bottom);
	}

	private Rect getDrawRect() {
		if (currentRect == null) {
			currentRect = new Rect();
			currentRect.set(qrCodeView.mCameraManager.getFramingRect());
		}
		return currentRect;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		//小米手机在这里绘制的时候，画面并不显示
	}

	public class MaskView extends View {

		public MaskView(Context context) {
			super(context);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			Rect frame = getDrawRect();

			if (frame == null) {
				invalidate();
				return;
			}

			//画背景
			canvas.drawColor(maskColor);
			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
			canvas.drawRect(frame, paint);


			//画扫描框边上的角，总共8个部分
			paint.setXfermode(null);
			paint.setColor(cornerColor);
			canvas.drawRect(frame.left, frame.top, frame.left + CORNER_LENGTH,
					frame.top + CORNER_WIDTH, paint);
			canvas.drawRect(frame.left, frame.top, frame.left + CORNER_WIDTH, frame.top
					+ CORNER_LENGTH, paint);
			canvas.drawRect(frame.right - CORNER_LENGTH, frame.top, frame.right,
					frame.top + CORNER_WIDTH, paint);
			canvas.drawRect(frame.right - CORNER_WIDTH, frame.top, frame.right, frame.top
					+ CORNER_LENGTH, paint);
			canvas.drawRect(frame.left, frame.bottom - CORNER_WIDTH, frame.left
					+ CORNER_LENGTH, frame.bottom, paint);
			canvas.drawRect(frame.left, frame.bottom - CORNER_LENGTH,
					frame.left + CORNER_WIDTH, frame.bottom, paint);
			canvas.drawRect(frame.right - CORNER_LENGTH, frame.bottom - CORNER_WIDTH,
					frame.right, frame.bottom, paint);
			canvas.drawRect(frame.right - CORNER_WIDTH, frame.bottom - CORNER_LENGTH,
					frame.right, frame.bottom, paint);

			//绘制中间的线,每次刷新界面，中间的线往下移动SPEEN_DISTANCE
			if (isShowScannerLine) {
				slideTop += MAX_SPEED_DISTANCE * scannerSpeed;
				if (slideTop >= frame.bottom - CORNER_WIDTH || slideTop < frame.top + CORNER_WIDTH) {
					slideTop = frame.top + CORNER_WIDTH;
				}
				paint.setColor(scannerLineColor);
				canvas.drawRect(frame.left + MIDDLE_LINE_PADDING, slideTop - MIDDLE_LINE_WIDTH / 2, frame.right - MIDDLE_LINE_PADDING, slideTop + MIDDLE_LINE_WIDTH / 2, paint);

				//只刷新扫描框的内容，其他地方不刷新
				postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
						frame.right, frame.bottom);
			}

			postInvalidateDelayed(ANIMATION_DELAY);
		}
	}

}
