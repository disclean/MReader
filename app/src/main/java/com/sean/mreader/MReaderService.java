package com.sean.mreader;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

import static android.app.ActivityManager.MOVE_TASK_WITH_HOME;
import static com.sean.mreader.R.id.info;

public class MReaderService extends Service {
	public MReaderService() {
		deb("MReaderService() called");
	}

	private void deb(String message) {
		if (message == null) return;
		try {
			//Log.i("MReader_Dialer", "");
			//System.out.println("["+ this.getClass().getName()+"]" +message );
			//new Throwable().getStackTrace()[0].getLineNumber()
			Log.i("MReader_Service", "[" + new Throwable().getStackTrace()[1].getMethodName() +
					":" + new Throwable().getStackTrace()[1].getLineNumber() + "] " + message);

//					"file " + new Throwable().getStackTrace()[1].getFileName() +
//					" class " + new Throwable().getStackTrace()[1].getClassName() +
//					" method " + new Throwable().getStackTrace()[1].getMethodName() +
//					" line " + new Throwable().getStackTrace()[1].getLineNumber());


		} catch (NullPointerException n) {
			System.out.println("error!!....null!!" + this.getClass().getName());
		}
	}

	BroadcastReceiver mServiceBR = new BroadcastReceiver() {
		public static final String ScreenOff = "android.intent.action.SCREEN_OFF";
		public static final String ScreenOn = "android.intent.action.SCREEN_ON";

		public void onReceive(Context context, Intent intent) {
			deb("mServiceBR received " + intent.getAction().toString());

			if (intent.getAction().equals(ScreenOff)) {
				deb("Do something for SCREEN_OFF");
			} else if (intent.getAction().equals(ScreenOn)) {
				deb("Do something for SCREEN_ON");
				/*
				ActivityManager.RunningTaskInfo running = info.get(0);
				ComponentName componentName = running.topActivity;
				return cls.getName().equals(componentName.getClassName());
				*/

				ActivityManager activityManager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
				List<ActivityManager.RunningTaskInfo> info;
				//info = activityManager.getAppTasks();
				info = activityManager.getRunningTasks(100);
				deb("MReader class name is "+MReaderDialer.class.toString()); //com.sean.mreader.MReaderDialer
				//deb("will list all task");
				boolean isRunningMReader = false;
				for (Iterator iterator = info.iterator(); iterator.hasNext(); ) {
					ActivityManager.RunningTaskInfo runningTaskInfo = (ActivityManager.RunningTaskInfo) iterator.next();
					deb(runningTaskInfo.topActivity.getClassName());
					//if (runningTaskInfo.topActivity.getClassName().equals(MReaderDialer.class.toString())) {
					if (runningTaskInfo.topActivity.getClassName().equals("com.sean.mreader.MReader" +
							"" +
							"Dialer")) {
						isRunningMReader = true;
						activityManager.moveTaskToFront(runningTaskInfo.id, 0);
						deb("isRunningMReader is true");
						break;
					}
				}

				if (!isRunningMReader) {
					Intent intentDialer = new Intent(context, com.sean.mreader.MReaderDialer.class);
					intentDialer.putExtra("action", "SCREEN_ON");
					PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentDialer, 0);
					try {
						pendingIntent.send();
						deb("Will send intent to MReaderDialer");
					} catch (Exception e) {
						deb(e.toString());
					}
				}
			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		deb("called");
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);
		registerReceiver(mServiceBR, intentFilter);
		deb("registerReceiver mServiceBR");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		deb("called");
		deb("return START_STICKY");
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		deb("called");

		if (mServiceBR != null) {
			unregisterReceiver(mServiceBR);
			deb("unregister mServiceBR");
		}
	}
}
