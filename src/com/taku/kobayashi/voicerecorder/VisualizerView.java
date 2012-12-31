package com.taku.kobayashi.voicerecorder;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class VisualizerView extends View{

	private byte[] m_Bytes;
	private float[] m_Points;
	//private Rect m_Rect = new Rect();
	private Paint m_ForePaint = new Paint();

	public VisualizerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		m_Bytes = null;
		m_ForePaint.setStrokeWidth(1f);
		m_ForePaint.setAntiAlias(true);
		//青
		m_ForePaint.setColor(Color.rgb(0, 128, 255));
	}

	public void updateVisualizer(byte[] bytes) {
		m_Bytes = bytes;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (m_Bytes == null) {
			return;
		}

		// 波形データの数にあわせて表示データを作成します。
		// 線分を引くために開始位置（X,Y)終了位置（X,Y）が必要なため4倍にしています。
		if (m_Points == null || m_Points.length < m_Bytes.length * 4) {
			m_Points = new float[m_Bytes.length * 4];
		}

		int width = getWidth();
		int height = getHeight();
		int Length = m_Bytes.length - 1;
		// 波形データは-128～127の範囲のデータであるため128を足して
		// 0からはじまるようにしています。
		for (int i = 0; i < Length; i++) {
			//x0
			m_Points[i * 4] = width * i / (m_Bytes.length - 1);
			//y0
			m_Points[i * 4 + 1] = height / 2 + ((byte) (m_Bytes[i] + 128)) * (height / 2) / 128;
			//x1
			m_Points[i * 4 + 2] = width * (i + 1) / (m_Bytes.length - 1);
			//y1
			m_Points[i * 4 + 3] = height / 2 + ((byte) (m_Bytes[i + 1] + 128)) * (height / 2) / 128;
		}

		canvas.drawLines(m_Points, m_ForePaint);
	}
}