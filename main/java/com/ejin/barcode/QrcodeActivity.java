package com.ejin.barcode;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.zxing.client.android.camera.DecodeHandler;
import com.google.zxing.scanview.QRCodeReaderView;
import com.google.zxing.scanview.ScanView;

/**
 * Created by j17420 on 2017/7/11.
 */

public class QrcodeActivity extends AppCompatActivity implements DecodeHandler.OnQRCodeReadListener {

	ScanView scanView;

	public static void start(Context context) {
		Intent intent = new Intent(context, QrcodeActivity.class);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qrcode);
		initView();
	}

	private void initView() {
		scanView = (ScanView) findViewById(R.id.qrcoderiew);
		scanView.setOnQRCodeReadListener(this);
	}

	@Override
	public void onQRCodeRead(String text) {
		finish();
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		scanView.startCamera();
	}

	@Override
	protected void onPause() {
		super.onPause();
		scanView.stopCamera();
	}
}
