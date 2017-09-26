package com.ejin.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import com.ejin.sample.camera.DecodeListener;
import com.ejin.sample.scanview.BarcodeScannerView;

/**
 * Created by j17420 on 2017/7/11.
 */

public class QrcodeActivity extends AppCompatActivity implements DecodeListener {

	BarcodeScannerView scannerView;

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
		scannerView = (BarcodeScannerView) findViewById(R.id.qrcoderiew);
		scannerView.setOnCodeReadListener(this);
	}



	@Override
	protected void onResume() {
		super.onResume();
		scannerView.startCamera();
//		scannerView.autoSwitchScaner();
	}

	@Override
	protected void onPause() {
		super.onPause();
		scannerView.stopCamera();
	}

	@Override
	public void onRead(final String s) {
		scannerView.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(QrcodeActivity.this, s, Toast.LENGTH_SHORT).show();
			}
		});
		finish();
	}
}
