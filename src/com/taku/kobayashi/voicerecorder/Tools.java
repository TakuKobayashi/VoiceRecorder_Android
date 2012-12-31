package com.taku.kobayashi.voicerecorder;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

public class Tools {

	private final static String TAG = "VoiceRecoderTools";

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------
	//ImageViewを使用したときのメモリリーク対策
	public static void releaseImageView(ImageView imageView){
		if (imageView != null) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable)(imageView.getDrawable());
			if (bitmapDrawable != null) {
				bitmapDrawable.setCallback(null);
			}
			imageView.setImageBitmap(null);
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static void releaseWebView(WebView webview){
		webview.stopLoading();
		webview.setWebChromeClient(null);
		webview.setWebViewClient(null);
		webview.destroy();
		webview = null;
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static String getFilePath(String file_extention) {

		String strFilePath = new String();
		String strTempName = new String();
		int i = 0;
		while (true) {
			// 外部ストレージ(SDカード)がある場所の情報を取ってくる
			String strExtDir = Environment.getExternalStorageDirectory().getPath();
			String strPrefix = "VoiceRecoder";
			// 今の時間をミリ秒で返す(PHPのstrtotimeと同じ)
			long dateTaken = System.currentTimeMillis();

			if (i == 0) {
				// 年-月-日_時.分.秒 + 拡張子 または 年-月-日_時.分.秒 +_番号 + 拡張子
				strTempName = DateFormat.format("yyyy-MM-dd_kk_mm_ss", dateTaken).toString() + file_extention;
			} else {
				strTempName = DateFormat.format("yyyy-MM-dd_kk_mm_ss", dateTaken).toString() + "_" + i + file_extention;
			}
			strFilePath = strExtDir + "/" + com.taku.kobayashi.voicerecorder.Config.DIRECTORY_NAME_TO_SAVE + "/" + strPrefix + strTempName;
			File file = new File(strFilePath);
			// 他にファイルがないか調べる
			if (file.exists() == false){
				break;
			}
			i++;
		}
		return strFilePath;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------
	public static void showToast(Context con, String message) {
		Toast toast = Toast.makeText(con, message, Toast.LENGTH_LONG);
		toast.show();
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static void memoryInfo(Context con,String mes) {
		ActivityManager activityManager = (ActivityManager)con.getSystemService(Activity.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(memoryInfo);

		/*
		//使えるメモリ容量全体
		Log.d(TAG, "memoryInfo.availMem[MB] = " + (int)(memoryInfo.availMem/1024/1024));
		//backgroundで実行しているものを次々とkillしていくしきい値
		Log.d(TAG, "memoryInfo.threshold[MB] = " + (int)(memoryInfo.threshold/1024/1024));
		//
		Log.d(TAG, "memoryInfo.lowMemory = " + memoryInfo.lowMemory);
		*/
		/*
		// 確保しているヒープサイズ
		Log.d(TAG, "NativeHeapSize = " + android.os.Debug.getNativeHeapSize()/1024);
		// 空きヒープサイズ
		Log.d(TAG, "NativeHeapFreeSize = " + android.os.Debug.getNativeHeapFreeSize()/1024);
		// 使用中ピープサイズ
		Log.d(TAG, "NativeHeapAllocatedSize = " + android.os.Debug.getNativeHeapAllocatedSize()/1024);
		*/

		// 確保しているヒープサイズ
		Log.d(TAG, mes + ":NativeHeapSize = " + android.os.Debug.getNativeHeapSize());
		// 空きヒープサイズ
		Log.d(TAG, mes + ":NativeHeapFreeSize = " + android.os.Debug.getNativeHeapFreeSize());
		// 使用中ピープサイズ
		Log.d(TAG, mes + ":NativeHeapAllocatedSize = " + android.os.Debug.getNativeHeapAllocatedSize());

		/*
		// アプリのメモリ情報を取得
		Runtime runtime = Runtime.getRuntime();
		// トータルメモリ
		Log.v(TAG, "totalMemory[KB] = " + (int)(runtime.totalMemory()/1024));
		// 空きメモリ
		Log.v(TAG, "freeMemory[KB] = " + (int)(runtime.freeMemory()/1024));
		//現在使用しているメモリ
		Log.v(TAG, "usedMemory[KB] = " + (int)( (runtime.totalMemory() - runtime.freeMemory())/1024) );
		// Dalvikで使用できる最大メモリ
		Log.v(TAG, "maxMemory[KB] = " + (int)(runtime.maxMemory()/1024));
		*/
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static String getRecodeFileExtension(int MediaRecoderNumber){
		String strExtension = null;
		switch(MediaRecoderNumber){
			case MediaRecorder.OutputFormat.DEFAULT:
				strExtension = ".wav";
				break;
			case MediaRecorder.OutputFormat.THREE_GPP:
				strExtension = ".3gp";
				break;
			case MediaRecorder.OutputFormat.MPEG_4:
				strExtension = ".mp4";
				break;
		}
		return strExtension;
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static ArrayList<File> FileStringFilter(String FileDirPath,String FilterStr){
		File file = new File(FileDirPath);
		ArrayList<File> FileList = new ArrayList<File>();
		//String 引数がnullだった場合は制限をかけない。
		if(file.exists()){
			//listFiles()はSDカード内の該当ディレクトリを検索しているため処理が遅くなる
			File[] files = file.listFiles();
			//処理回数削減(処理速度向上)のため
			int nLength = files.length;
			for(int i = 0;i < nLength;i++){
				boolean bFilter = true;
				if(FilterStr != null){
					//正規表現による検索
					if(!files[i].getName().matches(".*" + FilterStr + ".*")){
						bFilter = false;
					}
				}
				if(files[i].getName().endsWith(".mp3") == false && files[i].getName().endsWith(".mp4") == false && files[i].getName().endsWith(".3gp") == false && files[i].getName().endsWith(".wav") == false){
					bFilter = false;
				}
				if(bFilter){
					FileList.add(files[i]);
				}
			}
		}
		return FileList;
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	//拡張子はそのままでファイル名を変える
	public static boolean RenameFile(File file,String NewName){
		String path = file.getPath();
		String OldName = file.getName();
		//正規表現であるので\\をつける
		String[] strExtentions = OldName.split("\\.");
		int nLast = strExtentions.length - 1;
		String extention = strExtentions[nLast];
		//空欄ならfile名を変更しない
		if(NewName.isEmpty() == true){
			for(int i = 0;i < nLast;i++){
				NewName += strExtentions[i];
			}
		}
		String newFilePath = path.replace(OldName, NewName + "." + extention);
		File newFile = new File(newFilePath);
		boolean renameSuccess = file.renameTo(newFile);
		return renameSuccess;
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static String ConversionTime(long milliseconds){
		int second = (int) (milliseconds / 1000);
		int miniute = second / 60;
		int hour = miniute / 60;
		miniute = miniute - (hour * 60);
		second = second - (hour * 60 * 60) - (miniute * 60);
		String time = new String();
		if(hour < 10){
			time += "0";
		}
		time += hour + ":";
		if(miniute < 10){
			time += "0";
		}
		time += miniute + ":";
		if(second < 10){
			time += "0";
		}
		time += second;
		return time;
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private static void setVolume(Context context,AudioManager am,int Volume){
		am.setStreamVolume(AudioManager.STREAM_MUSIC, Volume, AudioManager.FLAG_SHOW_UI);
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(context.getResources().getString(R.string.RecordVolumeKey), Volume);
		editor.commit();
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	private static void setVolumeChange(Context context,AudioManager am,int Volume){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		if(settings.getBoolean(context.getResources().getString(R.string.VolumeAutoSetKey), false) == true){
			setVolume(context, am, Volume);
		}else{
			am.setStreamVolume(AudioManager.STREAM_MUSIC, Volume, AudioManager.FLAG_SHOW_UI);
		}
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static void setVolumeUp(Context context){
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		int nVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		nVolume = nVolume + 1;
		int nMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		if(nMaxVolume < nVolume){
			nVolume = nMaxVolume;
		}
		setVolumeChange(context,am,nVolume);
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static void setVolumeDown(Context context){
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		int nVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		nVolume = nVolume - 1;
		if(0 >= nVolume){
			nVolume = 0;
			Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
			//200ミリ秒バイブレーション起動。
			vibrator.vibrate(Config.VIBRATING_TIME);
		}
		setVolumeChange(context,am,nVolume);
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static void setRecoedVolume(Context context){
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		int nBefore = am.getStreamVolume(AudioManager.STREAM_MUSIC);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(context.getResources().getString(R.string.RecordBeforeVolumeKey), nBefore);
		editor.commit();

		if((settings.getBoolean(context.getResources().getString(R.string.VolumeAutoSetKey), false) == true)){
			int nRecordVolume = settings.getInt(context.getResources().getString(R.string.RecordVolumeKey), 0);
			Log.d(TAG, "n:"+nRecordVolume);
			setVolume(context,am,nRecordVolume);
		}

	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static void setBeforeVolume(Context context){
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		int nBefore = settings.getInt(context.getResources().getString(R.string.RecordBeforeVolumeKey), 0);

		if((settings.getBoolean(context.getResources().getString(R.string.VolumeAutoSetKey), false) == true)){
			am.setStreamVolume(AudioManager.STREAM_MUSIC, nBefore, AudioManager.FLAG_SHOW_UI);
		}
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static int getOutputFormat(String FileExtention) {
		int format = 0;
		if(FileExtention.equals(".wav")){
			format = MediaRecorder.OutputFormat.DEFAULT;
		}else if(FileExtention.equals(".3gp")){
			format = MediaRecorder.OutputFormat.THREE_GPP;
		}else if(FileExtention.equals(".mp4")){
			format = MediaRecorder.OutputFormat.MPEG_4;
		}
		return format;
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static int getMaxDuration(int Time,int position){
		int nTimeMillisecond = -1;
		if(position == 0){
			nTimeMillisecond = -1;
		}else if(position == 1){
			//秒
			nTimeMillisecond = Time * 1000;
		}else if(position == 2){
			//分
			nTimeMillisecond = Time * 1000 * 60;
		}else if(position == 3){
			//時間
			nTimeMillisecond = Time * 1000 * 60 * 60;
		}
		return nTimeMillisecond;
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static Bitmap getBitmap(Context con, Integer rsc) {
		//BitmapFactory.Options options = new BitmapFactory.Options();
		//int nInSampleSize = 1;
		//nInSampleSize = (int)Math.floor(Math.max(getImageSize(con, rsc).width / getDisplaySize(con).width,getImageSize(con, rsc).height / getDisplaySize(con).height));
		//options.inSampleSize = nInSampleSize;
		return BitmapFactory.decodeResource(con.getResources(), rsc);
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

	public static void releaseSeekBar(SeekBar sb){
		if(sb != null){
			sb.setProgressDrawable(null);
			sb.setThumb(null);
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------------------------------------

}
