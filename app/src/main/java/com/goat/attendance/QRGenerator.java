package com.goat.attendance;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRGenerator {
	
	public Bitmap generateQRCode(String uid, String name) {
		
		try {
			
			int qrSize = 512;
			
			QRCodeWriter writer = new QRCodeWriter();
			
			BitMatrix bitMatrix = writer.encode( uid, BarcodeFormat.QR_CODE, qrSize, qrSize );
			
			Bitmap qrBitmap = Bitmap.createBitmap( qrSize, qrSize, Bitmap.Config.RGB_565 );
			
			for (int x = 0; x < qrSize; x++) {
				for (int y = 0; y < qrSize; y++) {
					qrBitmap.setPixel(
					x,
					y,
					bitMatrix.get(x, y)
					? Color.BLACK
					: Color.WHITE
					);
				}
			}
			
			// Text settings
			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setColor(Color.BLACK);
			paint.setTextSize(40);
			paint.setTypeface(Typeface.DEFAULT_BOLD);
			paint.setTextAlign(Paint.Align.CENTER);
			
			String text = "Name: " + name;
			
			Paint.FontMetrics fm = paint.getFontMetrics();
			int textHeight = (int)(fm.bottom - fm.top);
			
			int padding = 30;
			
			// Final bitmap size
			Bitmap finalBitmap = Bitmap.createBitmap( qrSize, qrSize + textHeight + (padding * 2), Bitmap.Config.ARGB_8888 );
			
			Canvas canvas = new Canvas(finalBitmap);
			canvas.drawColor(Color.WHITE);
			
			// Draw QR
			canvas.drawBitmap(qrBitmap, 0, 0, null);
			
			// Draw Name
			float textY = qrSize + padding - fm.top; canvas.drawText(text, qrSize / 2f, textY, paint);
			
			return finalBitmap;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
