package com.goat.attendance;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.goat.attendance.databinding.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.zxing.client.android.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import org.json.*;

public class QrviewerActivity extends AppCompatActivity {
	
	private Timer _timer = new Timer();
	
	private QrviewerBinding binding;
	
	private TimerTask timerr;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = QrviewerBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		setSupportActionBar(binding.Toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		binding.Toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				onBackPressed();
			}
		});
	}
	
	private void initializeLogic() {
		QRGenerator qrGenerator = new QRGenerator();
		String qrText = getIntent().getStringExtra("qr_data");
		
		if (qrText == null) {
			qrText = "UserName: Unknown";
		}
		
		String name = qrText;
		
		if (qrText.startsWith("UserName:")) {
			name = qrText.substring("UserName:".length()).trim();
		}
		
		Bitmap qrBitmap = qrGenerator.generateQRCode(qrText, name);
		binding.qrimg.setImageBitmap(qrBitmap);
		
		String path = saveQRCode(qrBitmap, name);
		
		Toast.makeText(
		this,
		"Saved: " + path,
		Toast.LENGTH_LONG
		).show();
	}
	public static String saveQRCode(Bitmap bitmap, String name) {
		
		try {
			
			File file = new File(
			"/storage/emulated/0/Download/" + name + "_QR.png"
			);
			
			FileOutputStream out =
			new FileOutputStream(file);
			
			bitmap.compress(
			Bitmap.CompressFormat.PNG,
			100,
			out
			);
			
			out.flush();
			out.close();
			
			return file.getAbsolutePath();
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("QR_SAVE", e.toString());
		}
		
		return null;
		
	}
	
}