package com.taku.kobayashi.voicerecorder;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Timer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public class VoiceRecorder{

	private static final String TAG = "VoiceRecorder_VoiceRecorder";
	private Context m_Context;
	private MediaRecorder m_Recorder = null;
	private boolean m_bRecording;
	//private AudioRecord m_AudioRecord;
	private SharedPreferences m_SharedPreferences;
	private Thread m_Thread;
	//private Timer m_FinishTimer = null;
	private Handler m_Handler;
	private RecordStopListener RecordStopListener = null;
	private PreferenceFinishRecordListener PreferenceFinishListener = null;

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public VoiceRecorder(Context context){
		m_Context = context;
		m_bRecording = false;
		m_SharedPreferences = PreferenceManager.getDefaultSharedPreferences(m_Context);
		m_Handler = new Handler();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public void setRecord(MediaRecorder mr,int AudioSource,int nDurationTime){
		m_Recorder = mr;
		String format = m_SharedPreferences.getString(m_Context.getResources().getString(R.string.SettingFormatKey), ".wav");
		int nOutput = Tools.getOutputFormat(format);
		m_Recorder.setAudioSource(AudioSource);
		//オーディオファイルの出力フォーマット
		m_Recorder.setOutputFormat(nOutput);
		//高音質処理
		int nRate = getSamplingRate();
		if(nRate > Config.MIN_AUDIOSAMPLINGRATE){
			m_Recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		}else{
			m_Recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		}
		//m_Recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		Log.d(TAG,String.valueOf(nRate));
		m_Recorder.setAudioSamplingRate(nRate);

		int nMaxDuration = nDurationTime;
		if(nMaxDuration == Config.DEFAULT_MAXDURATION){
			nMaxDuration = m_SharedPreferences.getInt(m_Context.getResources().getString(R.string.SettingMaxDurationKey), Config.DEFAULT_MAXDURATION);
		}
		//Timer == -1 で無限大
		if(nMaxDuration > 0 && AudioSource == MediaRecorder.AudioSource.MIC){
			m_Recorder.setMaxDuration(nMaxDuration);
		}
		//オーディオファイルの出力先のパス
		String strSaveFilePath = Tools.getFilePath(format);
		m_Recorder.setOutputFile(strSaveFilePath);
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public boolean StartRecord(){
		if(m_bRecording == false){
			m_bRecording = true;
			EditRecordingStatus(m_bRecording);
			try {
				m_Recorder.prepare();
			} catch (IllegalStateException e) {
				Log.d(TAG, "ERROR:IllegalStateException");
				e.printStackTrace();
			} catch (IOException e) {
				Log.d(TAG, "ERROR:IOException");
				e.printStackTrace();
			}
			m_Recorder.start();
			//Thread(Runnable)は処理部分のマルチスレッド,m_Handler.postは表示に反映させるために必要
			m_Handler.post(new Runnable() {
				@Override
				public void run() {
					Tools.showToast(m_Context, m_Context.getResources().getString(R.string.RecordStartMessage));
				}
			});
		}
		return true;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	/*
	private void showToastMessage(final Integer nMessageStringID){
		boolean bBackGround = m_SharedPreferences.getBoolean(m_Context.getResources().getString(R.string.BackgroundRecordKey), false);
		if(bBackGround == true){
			m_Handler.post(new Runnable() {
				@Override
				public void run() {
					Tools.showToast(m_Context, m_Context.getResources().getString(nMessageStringID));
				}
			});
		}else{
			Tools.showToast(m_Context, m_Context.getResources().getString(nMessageStringID));
		}
	}
	*/

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public boolean StopRecord(){
		if(m_Recorder != null){
			m_bRecording = false;
			EditRecordingStatus(m_bRecording);
			m_Recorder.stop();
			m_Recorder.reset();
			Log.d(TAG,"RecordRelease");
		}else{
			m_bRecording = false;
			EditRecordingStatus(m_bRecording);
		}
		if(RecordStopListener != null){
			RecordStopListener.RecordStop();
		}
		//バックグラウンドであってもToastを表示させるため
		m_Handler.post(new Runnable() {
			@Override
			public void run() {
				Tools.showToast(m_Context, m_Context.getResources().getString(R.string.RecordStopMessage));
			}
		});
		return false;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private void EditRecordingStatus(boolean Recording){
		SharedPreferences setting = PreferenceManager.getDefaultSharedPreferences(m_Context);
		if(setting.getBoolean(m_Context.getResources().getString(R.string.RecordingKey), false) == true && Recording == false){
			if(PreferenceFinishListener != null){
				PreferenceFinishListener.RecordingFinish();
			}
		}
		SharedPreferences.Editor editor = setting.edit();
		//Booleanの変化のリスナーはLongの後に取得したいため、順番を変えてはだめ
		editor.putLong(m_Context.getResources().getString(R.string.RecordStartTimeKey), System.currentTimeMillis());
		editor.putBoolean(m_Context.getResources().getString(R.string.RecordingKey), Recording);
		editor.commit();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private int getSamplingRate(){
		int nRate = Config.MIN_AUDIOSAMPLINGRATE;
		if(Build.VERSION.SDK_INT >= 10){
			String SamplingRate = m_SharedPreferences.getString(m_Context.getResources().getString(R.string.SettingAudioSamplingRateKey), String.valueOf(Config.MIN_AUDIOSAMPLINGRATE));
			try{
				nRate = Integer.parseInt(SamplingRate);
			} catch (NumberFormatException e) {

			}
		}
		return nRate;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//バックグラウンド録音はスタートボタンを押したときのみ実行される
	public boolean BackgroundStartRecord(){
		m_Handler = new Handler();
		m_Thread = new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "Startback");

				long RecordStartTime = System.currentTimeMillis();
				StartRecord();
				while(m_bRecording == true) {
					//録音中のxmlを削除した場合falseになり処理を抜ける
					m_bRecording = m_SharedPreferences.getBoolean(m_Context.getResources().getString(R.string.RecordingKey),false);
				}
				StopRecord();
				Log.d(TAG, "Stoptback");
				long RecordedTime = System.currentTimeMillis() - RecordStartTime;
				if(RecordStopListener != null){
					RecordStopListener.RecordStop();
				}
				//releaseしないと再度マルチスレッドで録音を行ったときにMediaRecorderが残っていて、例外処理が発生する
				release();
				//m_Thread.stop();
			}
		});
		//突然スレッドが終了した時のハンドラ
		m_Thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(){
			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				Log.d(TAG, "UncaughtException!!!!");
				StopRecord();
			}
		});
		m_Thread.setDaemon(true);
		m_Thread.start();
		return true;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public boolean BackgroundStopRecord(){
		m_bRecording = false;
		EditRecordingStatus(m_bRecording);
		return false;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public void release(){
		if(m_Recorder != null){
			Log.d(TAG,"RecordRelease");
			if(m_bRecording == true){
				m_Recorder.stop();
				m_Recorder.reset();
				EditRecordingStatus(false);
			}
			m_Recorder.release();
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	/**
	 * リスナーを追加する
	 */
	public void setOnRecordStopListener(RecordStopListener listener){
		this.RecordStopListener = listener;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	/**
	 * リスナーを削除する
	 */
	public void RecoedStopRemoveListener(){
		this.RecordStopListener = null;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//検出処理が終わったことを通知する独自のリスナーを作成
	public interface RecordStopListener extends EventListener {
		/**
		 * 録音が終了したことを通知する
		 */
		public void RecordStop();

	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	/**
	 * リスナーを追加する
	 */
	public void setOnPreferenceFinishListener(PreferenceFinishRecordListener listener){
		this.PreferenceFinishListener = listener;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	/**
	 * リスナーを削除する
	 */
	public void PreferenceRemoveListener(){
		this.PreferenceFinishListener = null;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//検出処理が終わったことを通知する独自のリスナーを作成
	public interface PreferenceFinishRecordListener extends EventListener {
		/**
		 * 録音が終了したことを通知する
		 */
		public void RecordingFinish();

	}

}