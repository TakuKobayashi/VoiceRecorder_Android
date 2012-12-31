package com.taku.kobayashi.voicerecorder;

import java.io.File;

import android.app.Activity;
import android.os.Environment;
import android.os.StatFs;

public class SDCardCtrl{
	private static String SDCardDir = Environment.getExternalStorageDirectory().toString();
	public static String AudioFileDir = SDCardDir + "/" + Config.DIRECTORY_NAME_TO_SAVE;

	//--------------------------------------------------------------------------------

	public static boolean CreateNewDirectoryOnSDcard(String strNewDirName) {
		File fileNewDir = new File(strNewDirName);
		if (fileNewDir.exists() && fileNewDir.isDirectory()) {
			return true;
		} else {
			if (fileNewDir.mkdirs()) {
				return true;
			} else {
				return false;
			}
		}
	}

	//--------------------------------------------------------------------------------


	private static boolean CheckSDcardMount(Activity act) {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) == false) {
			Tools.showToast(act, act.getResources().getString(R.string.SDCardMountRemovingMessage));
			act.finish();
			return false;
		}else{
			return true;
		}
	}

	//--------------------------------------------------------------------------------
	public static boolean checkSDCard(Activity act) {
		if (!CheckSDcardMount(act)) {
			return false;
		}
		if (!CheckSDcardAvailableSpace(act)) {
			return false;
		}
		if (!CreateNewDirectoryOnSDcard(Environment.getExternalStorageDirectory().toString() + "/" + Config.DIRECTORY_NAME_TO_SAVE + "/")) {
			return false;
		}
		return true;
	}

	//--------------------------------------------------------------------------------
	private static boolean CheckSDcardAvailableSpace(Activity act) {
		String strSDcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		StatFs statFs = new StatFs(strSDcardPath);

		double SDcardAvailableSpace = (double)statFs.getAvailableBlocks() * (double)statFs.getBlockSize() / 1024.0;

		if (SDcardAvailableSpace <= com.taku.kobayashi.voicerecorder.Config.LIMIT_MINIMAM_SPACE) {
			Tools.showToast(act, act.getResources().getString(R.string.SDCardNoAvailableSpaceMessage));
			act.finish();
			return false;
		}else{
			return true;
		}
	}

	//--------------------------------------------------------------------------------
}