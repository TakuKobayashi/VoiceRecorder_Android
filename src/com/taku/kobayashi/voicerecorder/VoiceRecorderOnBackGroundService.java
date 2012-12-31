package com.taku.kobayashi.voicerecorder;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class VoiceRecorderOnBackGroundService extends IntentService {

	private final static String TAG = "VoiceRecorder_VoiceRecorderOnBackGroundService";
	private VoiceRecorder m_VoiceRecorder;
	//private boolean m_bRecording;

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public VoiceRecorderOnBackGroundService(String name) {
		super(name);
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public VoiceRecorderOnBackGroundService() {
		super("VoiceRecorderOnBackGroundService");
		//ファイル保存容量の限界、ファイル保存時間の限界等が訪れた時呼ばれる。
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	/*
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO 自動生成されたメソッド・スタブ
		return super.onStartCommand(intent, flags, startId);
	}
	*/

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	public void onCreate() {
		super.onCreate();
		m_VoiceRecorder = new VoiceRecorder(this);
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	protected void onHandleIntent(Intent intent) {
		MediaRecorder mr = new MediaRecorder();
		m_VoiceRecorder.setRecord(mr,intent.getIntExtra(getResources().getString(R.string.IntentAudioSourceKey), MediaRecorder.AudioSource.MIC),intent.getIntExtra(getResources().getString(R.string.IntentDurationKey), Config.DEFAULT_MAXDURATION));
		mr.setOnInfoListener(m_MediaRecorderInfoListener);

		Log.d(TAG, "ServiceStart");
		//バックグラウンド処理で録音中、無限ループさせて録音させ続ける
		boolean bRecord = m_VoiceRecorder.StartRecord();
		while(bRecord == true) {
			//録音中のxmlを削除した場合falseになり処理を抜ける
			bRecord = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.RecordingKey),false);
		}
		m_VoiceRecorder.StopRecord();
		Log.d(TAG, "ServiceStop");
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "ServiceDestroy");
		m_VoiceRecorder.release();
	}

	private OnInfoListener m_MediaRecorderInfoListener = new OnInfoListener() {
		@Override
		public void onInfo(MediaRecorder mr, int what, int extra) {
			Log.d(TAG,"what"+what+" extra:"+extra);
			if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED){
				m_VoiceRecorder.BackgroundStopRecord();
				Tools.showToast(VoiceRecorderOnBackGroundService.this, VoiceRecorderOnBackGroundService.this.getResources().getString(R.string.NoAvailableSpaceMessage));
			}else if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
				m_VoiceRecorder.BackgroundStopRecord();
				Tools.showToast(VoiceRecorderOnBackGroundService.this, VoiceRecorderOnBackGroundService.this.getResources().getString(R.string.TimeoutMessage));
			}
		}
	};

}