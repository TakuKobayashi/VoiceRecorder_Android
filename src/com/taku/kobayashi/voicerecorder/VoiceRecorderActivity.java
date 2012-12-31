package com.taku.kobayashi.voicerecorder;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemSelectedListener;

public class VoiceRecorderActivity extends Activity {

	private static final String TAG ="VoiceRecorder_VoiceRecoderActivity";
	private static final int TIMER_MENU_ID = Menu.FIRST;
	private static final int SEARCH_MENU_ID = Menu.FIRST + 1;
	private static final int SETTING_MENU_ID = Menu.FIRST + 2;
//	private static final int DELETEFILES_MENU_ID = Menu.FIRST + 3;
	private VoiceRecorder m_VoiceRecorder;

	private ImageButton m_RecordButton;
	//private boolean m_bRecording;
	private int m_nFileNum = 0;
	private AudioFileListAdapter m_Adapter;
	//private VoiceRecordCounterView m_VoiceRecordCounterView;
	private EditText m_TimerText;
	private TextView m_RecordingTimeText;
	private LinearLayout m_RecordingLayout;
	private int m_nPosition;

	private Handler m_Handler;
	private Timer m_TimeCountTimer;
	private SharedPreferences m_SharedPreferences;

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.voicerecorderview);

		m_SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		m_RecordButton = (ImageButton) findViewById(R.id.VoiceRecordButton);
		m_RecordButton.setOnClickListener(m_RecodeListener);

		m_RecordingTimeText = (TextView) findViewById(R.id.RecordingTimeText);
		m_RecordingLayout = (LinearLayout) findViewById(R.id.RecordingStatusLayout);

		m_Adapter = new AudioFileListAdapter(this);
		ListView AudioFileList = (ListView) findViewById(R.id.AudioFileList);
		AudioFileList.setAdapter(m_Adapter);
		AudioFileList.setOnItemClickListener(m_AudioListListener);

		m_VoiceRecorder = new VoiceRecorder(this);
		m_nPosition = 1;
		m_TimeCountTimer = new Timer(true);
		m_Handler = new Handler();
		setMutableView(m_SharedPreferences.getBoolean(getResources().getString(R.string.RecordingKey), false));

	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private void setMutableView(boolean bRecording){
		if(bRecording == true){
			m_RecordButton.setImageResource(R.drawable.audiostop_icon);
			m_RecordingLayout.setVisibility(View.VISIBLE);
			settingCountTimer(bRecording);
		}else{
			m_RecordButton.setImageResource(R.drawable.recordstart_icon);
			m_Adapter.setFilter(null);
			m_RecordingLayout.setVisibility(View.INVISIBLE);
			settingCountTimer(bRecording);
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private void settingCountTimer(boolean bRecording){
		//Timer TimeCountTimer = new Timer(false);
		if(bRecording == true){
			if(m_TimeCountTimer == null){
				m_TimeCountTimer = new Timer(true);
			}
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					m_Handler.post(new Runnable() {
						@Override
						public void run() {
							//ここに時間が来たときに実行する処理を記入する
							if(m_SharedPreferences.getBoolean(getResources().getString(R.string.RecordingKey), true) == true){
								long Count = System.currentTimeMillis() - m_SharedPreferences.getLong(getResources().getString(R.string.RecordStartTimeKey), 0);
								m_RecordingTimeText.setText(Tools.ConversionTime(Count));
							}else{
								setMutableView(false);
							}
						}
					});
				}
			};
			//0.5秒ごとにカウント
			m_TimeCountTimer.schedule(task, 0, 500);
		}else{
			ResetTimer();
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private void ResetTimer(){
		m_RecordingTimeText.setText(Tools.ConversionTime(0));
		if(m_TimeCountTimer != null){
			m_TimeCountTimer.cancel();
			m_TimeCountTimer.purge();
			m_TimeCountTimer = null;
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private OnClickListener m_RecodeListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if(SDCardCtrl.checkSDCard(VoiceRecorderActivity.this) == true){
				//Start,Stopボタンを押さなくてもm_SharedPreferencesの内容が変化していることがあるので、ボタンを押すたびにその時の状態を取得
				boolean bRecording = m_SharedPreferences.getBoolean(getResources().getString(R.string.RecordingKey), false);
				if(bRecording == false){
					StartRecord(Config.DEFAULT_MAXDURATION);
				}else{
					StopRecord();
				}
			}
		}
	};

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private void StopRecord(){
		if(m_SharedPreferences.getBoolean(getResources().getString(R.string.BackgroundRecordKey), false) == false){
			m_VoiceRecorder.StopRecord();
		}else{
			m_VoiceRecorder.BackgroundStopRecord();
		}
		//マルチスレッド内で呼ばれるため、すでにm_SharedPreferencesが書き変わっている
		if(isFinishing() == false){
			setMutableView(m_SharedPreferences.getBoolean(getResources().getString(R.string.RecordingKey), false));
			ChangeFileNameDialog(VoiceRecorderActivity.this.getResources().getString(R.string.SaveFileNameTitle),VoiceRecorderActivity.this.getResources().getString(R.string.DialogSaveButton),VoiceRecorderActivity.this.getResources().getString(R.string.DialogDefaultSaveButton));
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private void StartRecord(int nDuration){
		MediaRecorder mr = new MediaRecorder();
		m_VoiceRecorder.setRecord(mr,MediaRecorder.AudioSource.MIC,nDuration);
		//ファイル保存容量の限界、ファイル保存時間の限界等が訪れた時呼ばれる。
		mr.setOnInfoListener(m_MediaRecorderInfoListener);

		//マルチスレッド内でm_SharedPreferencesが書き変わるため、マルチスレッドが立ち上がるタイムラグがあるので個別に設定
		boolean bRecord = true;
		if(m_SharedPreferences.getBoolean(getResources().getString(R.string.BackgroundRecordKey), false) == false){
			bRecord = m_VoiceRecorder.StartRecord();
		}else{
			Log.d(TAG, ""+nDuration);
			//bRecord = m_VoiceRecorder.BackgroundStartRecord();
			Intent intent = new Intent(this,VoiceRecorderOnBackGroundService.class);
			intent.putExtra(getResources().getString(R.string.IntentAudioSourceKey), MediaRecorder.AudioSource.MIC);
			intent.putExtra(getResources().getString(R.string.IntentDurationKey), nDuration);
			startService(intent);
		}
		if(isFinishing() == false){
			setMutableView(bRecord);
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private OnInfoListener m_MediaRecorderInfoListener = new OnInfoListener() {
		@Override
		public void onInfo(MediaRecorder mr, int what, int extra) {
			Log.d(TAG, "what:"+what+" extra:"+extra);
			if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED){
				StopRecord();
				Tools.showToast(VoiceRecorderActivity.this, VoiceRecorderActivity.this.getResources().getString(R.string.NoAvailableSpaceMessage));
			}else if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
				StopRecord();
				Tools.showToast(VoiceRecorderActivity.this, VoiceRecorderActivity.this.getResources().getString(R.string.TimeoutMessage));
			}
		}
	};

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private void ReservationRecordDialog(){
		LayoutInflater factory = LayoutInflater.from(this);
		View textEntryView = factory.inflate(R.layout.settingtimerdialog, null);
		Date now = new Date(System.currentTimeMillis());
		final DatePicker datePicker = (DatePicker) textEntryView.findViewById(R.id.datePicker);
		datePicker.updateDate(now.getYear(), now.getMonth(), now.getDate());
		final TimePicker timePicker = (TimePicker) textEntryView.findViewById(R.id.timePicker);
		timePicker.setIs24HourView(true);
		timePicker.setCurrentHour(now.getHours());
		timePicker.setCurrentMinute(now.getMinutes());

		m_TimerText = (EditText) textEntryView.findViewById(R.id.TimerDuration);
		final TextView UnitText = (TextView) textEntryView.findViewById(R.id.Unit);
		UnitText.setText(R.string.DialogSecondText);
		Spinner MaxDurationSpinner = (Spinner) textEntryView.findViewById(R.id.TimerSpinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.SettingTimerSpinner, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		MaxDurationSpinner.setAdapter(adapter);
		MaxDurationSpinner.setSelection(0,true);
		MaxDurationSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if(position == 0){
					UnitText.setText(R.string.DialogSecondText);
				}else if(position == 1){
					UnitText.setText(R.string.DialogMiniuteText);
				}else if(position == 2){
					UnitText.setText(R.string.DialogHourText);
				}
				m_nPosition = position + 1;
			}

			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(getResources().getString(R.string.TimerRecordTitle));
		alertDialogBuilder.setView(textEntryView);
		alertDialogBuilder.setPositiveButton(R.string.DialogSettingButton, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Date da = new Date(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), timePicker.getCurrentHour(), timePicker.getCurrentMinute(), 0);
				long SettingTimer = da.getTime();
				if(System.currentTimeMillis() < SettingTimer){
					RecordTimer((SettingTimer - System.currentTimeMillis()));
				}else{
					String strSetTimer = m_TimerText.getText().toString();
					if(strSetTimer.length() == 0){
						Tools.showToast(VoiceRecorderActivity.this,VoiceRecorderActivity.this.getResources().getString(R.string.EditTextEmptyMessage));
					}else{
						Tools.showToast(VoiceRecorderActivity.this, VoiceRecorderActivity.this.getResources().getString(R.string.SettingTimerErrorMessage));
					}
				}
			}
		});
		alertDialogBuilder.setNegativeButton(R.string.DialogCancelButton, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

			}
		});
		alertDialogBuilder.setCancelable(true);
		alertDialogBuilder.create().show();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private void RecordTimer(long time){
		//m_Handler = new Handler();
		Timer timer = new Timer(false);
		TimerTask ts = new TimerTask() {
			@Override
			public void run() {
				m_Handler.post(new Runnable() {
					@Override
					public void run() {
						//ここに時間が来たときに実行する処理を記入する

						//マルチスレッド対策:他のスレッドで録音中に時間が来た場合、録音しないための処理。
						boolean bRecording = m_SharedPreferences.getBoolean(VoiceRecorderActivity.this.getResources().getString(R.string.RecordingKey), false);
						if(bRecording == false){
							String strSetTimer = m_TimerText.getText().toString();
							if(strSetTimer.length() == 0){
								Tools.showToast(VoiceRecorderActivity.this,VoiceRecorderActivity.this.getResources().getString(R.string.EditTextEmptyMessage));
							}else{
								try{
									int num = Integer.parseInt(strSetTimer);
									if(num > 0){
									//ここにタイマーによる録音開始処理
										num = Tools.getMaxDuration(num, m_nPosition);
										StartRecord(num);
									//Log.d(TAG,VoiceRecorderActivity.this.isChild()+":"+VoiceRecorderActivity.this.isFinishing()+":"+VoiceRecorderActivity.this.isRestricted()+":"+VoiceRecorderActivity.this.isTaskRoot());
									}else{
										Tools.showToast(VoiceRecorderActivity.this,VoiceRecorderActivity.this.getResources().getString(R.string.EditTextUnder0Message));
									}
								} catch (NumberFormatException e) {
									Tools.showToast(VoiceRecorderActivity.this,VoiceRecorderActivity.this.getResources().getString(R.string.SettingNonNumberMessage));
								}
							}
						}else{
							Tools.showToast(VoiceRecorderActivity.this,VoiceRecorderActivity.this.getResources().getString(R.string.TimeUpWIthinRecordingMessage));
						}
					}
				});
			}

		};
		//一回だけ
		timer.schedule(ts,time);
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private AdapterView.OnItemClickListener m_AudioListListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> l, View v, int position,long id) {
			if(m_SharedPreferences.getBoolean(getResources().getString(R.string.RecordingKey), false) == false){
				m_nFileNum = position;
				ChoiceAudioFileDialog();
			}
		}
	};

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//バックボタンを押したときの処理
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(m_SharedPreferences.getBoolean(getResources().getString(R.string.RecordingKey), false) == true && m_SharedPreferences.getBoolean(getResources().getString(R.string.BackgroundRecordKey), false) == false){
				StopRecord();
			}else{
				finish();
			}
		}
		/*
		else if(keyCode == KeyEvent.KEYCODE_MENU && m_SharedPreferences.getBoolean(getResources().getString(R.string.RecordingKey), false) == false){
			//録音中はメニューキーをハンドリングさせない
			return true;
		}
		*/
		return false;

	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

//		menu.add(0, TIMER_MENU_ID, 0, getResources().getString(getResources().getIdentifier("MenuText"+TIMER_MENU_ID, "string", getPackageName()))).setIcon(iconRes);
//		menu.add(0, SEARCH_MENU_ID, 0, getResources().getString(getResources().getIdentifier("MenuText"+SEARCH_MENU_ID, "string", getPackageName()))).setIcon(iconRes);
//		menu.add(0, SETTING_MENU_ID, 0, getResources().getString(getResources().getIdentifier("MenuText"+SETTING_MENU_ID, "string", getPackageName()))).setIcon(iconRes);
//		menu.add(0, DELETEFILES_MENU_ID, 0, getResources().getString(getResources().getIdentifier("MenuText"+DELETEFILES_MENU_ID, "string", getPackageName()))).setIcon(iconRes);
		menu.add(0, TIMER_MENU_ID, 0, getResources().getString(getResources().getIdentifier("MenuText"+TIMER_MENU_ID, "string", getPackageName()))).setIcon(R.drawable.timer_icon);
		menu.add(0, SEARCH_MENU_ID, 0, getResources().getString(getResources().getIdentifier("MenuText"+SEARCH_MENU_ID, "string", getPackageName()))).setIcon(R.drawable.search_icon);
		menu.add(0, SETTING_MENU_ID, 0, getResources().getString(getResources().getIdentifier("MenuText"+SETTING_MENU_ID, "string", getPackageName()))).setIcon(R.drawable.setting_icon);
//		menu.add(0, DELETEFILES_MENU_ID, 0, getResources().getString(getResources().getIdentifier("MenuText"+DELETEFILES_MENU_ID, "string", getPackageName()))).setIcon(R.drawable.delete_icon);

		return true;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		//録音中はmenuバーを表示させない
		if(m_SharedPreferences.getBoolean(getResources().getString(R.string.RecordingKey), false) == false){
			return true;
		}else{
			//return falseでメニューバーが表示されなくなる
			return false;
		}

	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case TIMER_MENU_ID:
				ReservationRecordDialog();
				return true;

			case SEARCH_MENU_ID:
				SearchFileDialog();
				return true;

			case SETTING_MENU_ID:
				Intent intent = new Intent(this,SettingActivity.class);
				startActivity(intent);
				return true;

			/*
			case DELETEFILES_MENU_ID:
				return true;
			*/
		}
		return super.onOptionsItemSelected(item);
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private void ChoiceAudioFileDialog(){
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(getResources().getString(R.string.ChoiceRecordFileListTitle));
		alertDialogBuilder.setItems(R.array.ChioceAudioFile, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(SDCardCtrl.checkSDCard(VoiceRecorderActivity.this) == true){
					if(which == 0){
						Intent intent = new Intent(VoiceRecorderActivity.this,MeidaPlayerActivity.class);
						intent.putExtra(VoiceRecorderActivity.this.getResources().getString(R.string.IntentAudioFilePathKey), m_Adapter.getFile(m_nFileNum).getPath());
						startActivity(intent);
					}else if(which == 1){
						Intent audioIntent = new Intent(Intent.ACTION_SEND);
						//音声を添付するために必要
						audioIntent.setType("audio/*");
						//音声へのパス
						audioIntent.putExtra(Intent.EXTRA_STREAM,Uri.fromFile(m_Adapter.getFile(m_nFileNum)));
						startActivity(audioIntent);
					}else if(which == 2){
						ChangeFileNameDialog(VoiceRecorderActivity.this.getResources().getString(R.string.ChangeFileNameTitle),VoiceRecorderActivity.this.getResources().getString(R.string.DialogChangeButton),VoiceRecorderActivity.this.getResources().getString(R.string.DialogCancelButton));
					}else if(which == 3){
						CheckDeleteFileDialog();
					}
				}
			}
		});
		alertDialogBuilder.setCancelable(true);
		alertDialogBuilder.create().show();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private void ChangeFileNameDialog(String Title,String PositiveButton,String NegativeButton){
		LayoutInflater factory = LayoutInflater.from(this);
		final View textEntryView = factory.inflate(R.layout.edittextdialog, null);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(Title);
		alertDialogBuilder.setView(textEntryView);
		alertDialogBuilder.setPositiveButton(PositiveButton, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if(SDCardCtrl.checkSDCard(VoiceRecorderActivity.this) == true){
					EditText text = (EditText) textEntryView.findViewById(R.id.InputText);
					String str = text.getText().toString();
					boolean sucess = Tools.RenameFile(m_Adapter.getFile(m_nFileNum),str);
					if(sucess == false){
						Tools.showToast(VoiceRecorderActivity.this, VoiceRecorderActivity.this.getResources().getString(R.string.AudioFileRenameFailedMessage));
					}
				}
			}
		});
		alertDialogBuilder.setNegativeButton(NegativeButton, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

			}
		});
		alertDialogBuilder.setCancelable(true);
		alertDialogBuilder.create().show();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private void CheckDeleteFileDialog(){
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setMessage(R.string.CheckFileDeleteMessage);
		alertDialogBuilder.setPositiveButton(R.string.DialogOKButton, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				m_Adapter.deleteFile(m_nFileNum);
			}
		});
		alertDialogBuilder.setNegativeButton(R.string.DialogCancelButton, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

			}
		});
		alertDialogBuilder.setCancelable(true);
		alertDialogBuilder.create().show();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private void SearchFileDialog(){
		LayoutInflater factory = LayoutInflater.from(this);
		final View textEntryView = factory.inflate(R.layout.edittextdialog, null);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(getResources().getString(R.string.DialogSearchButton));
		alertDialogBuilder.setView(textEntryView);
		alertDialogBuilder.setPositiveButton(R.string.DialogSearchButton, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if(SDCardCtrl.checkSDCard(VoiceRecorderActivity.this) == true){
					EditText text = (EditText) textEntryView.findViewById(R.id.InputText);
					String str = text.getText().toString();
					m_Adapter.setFilter(str);
				}
			}
		});
		alertDialogBuilder.setNegativeButton(R.string.DialogCancelButton, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

			}
		});
		alertDialogBuilder.setCancelable(true);
		alertDialogBuilder.create().show();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG,"onStart");
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		if(SDCardCtrl.checkSDCard(this) == true){
			m_Adapter.setFilter(null);
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG,"onStop");
	}

	//--------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG,"onDestroy");
		if(m_SharedPreferences.getBoolean(getResources().getString(R.string.BackgroundRecordKey), false) == false || m_SharedPreferences.getBoolean(getResources().getString(R.string.RecordingKey),false) == false){
			m_VoiceRecorder.release();
			ResetTimer();
		}
		m_Adapter.release();
		Tools.releaseImageView(m_RecordButton);
		System.gc();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

}