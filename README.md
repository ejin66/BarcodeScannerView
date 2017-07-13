
# Introduce
**1. include BarcodeScannerView to your layout**
```java 
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
              android:orientation="vertical"
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              android:gravity="center_horizontal">

    <com.google.zxing.scanview.BarcodeScannerView
        android:id="@+id/scanView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</LinearLayout>
```

**2. set Listener:**
```java
scanView.setOnCodeReadListener(new OnCodeReadListener() {
  	@Override
	public void onCodeRead(String text) {
		//...
	}
});
```

**and, it works!(Don't forget the CAMERA and VIBRATE permission!)**
