package com.google.zxing.scanview;

import android.content.Context;
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
import android.widget.FrameLayout;

import com.google.zxing.client.android.camera.DecodeHandler;

/**
 * Created by j17420 on 2017/7/12.
 */

public class ScanView extends FrameLayout {

	QRCodeReaderView qrCodeView;
	private int maskColor = Color.parseColor("#60000000");
	private int cornerColor = Color.RED;

	/**
	 * 角的宽度
	 */
	private final int CORNER_WIDTH = 10;
	/**
	 * 角的长度
	 */
	private final int CORNER_LENGTH= 50;

	/**
	 * 扫描线移动的速度
	 */
	private final int SPEEND_DISTANCE = 5;

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

	/**
	 * 刷新界面的时间
	 */
	private static final long ANIMATION_DELAY = 10L;

	public ScanView(@NonNull Context context) {
		this(context, null);
	}

	public ScanView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ScanView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		addQRCoderView();
	}

	private void addQRCoderView() {
		qrCodeView = new QRCodeReaderView(getContext());
		qrCodeView.setBackCamera();
		addView(qrCodeView);
	}

	public QRCodeReaderView getQrCodeReaderView() {
		return qrCodeView;
	}

	public void setOnQRCodeReadListener(DecodeHandler.OnQRCodeReadListener onQRCodeReadListener) {
		qrCodeView.setOnQRCodeReadListener(onQRCodeReadListener);
	}

	public void startCamera() {
		qrCodeView.startCamera();
	}

	public void stopCamera() {
		qrCodeView.stopCamera();
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		Rect frame = qrCodeView.mCameraManager.getFramingRect();

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
		slideTop += SPEEND_DISTANCE;
		if (slideTop >= frame.bottom - CORNER_WIDTH || slideTop < frame.top + CORNER_WIDTH) {
			slideTop = frame.top + CORNER_WIDTH;
		}
		canvas.drawRect(frame.left + MIDDLE_LINE_PADDING, slideTop - MIDDLE_LINE_WIDTH / 2, frame.right - MIDDLE_LINE_PADDING, slideTop + MIDDLE_LINE_WIDTH / 2, paint);

		//只刷新扫描框的内容，其他地方不刷新
		postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
				frame.right, frame.bottom);
	}
}
