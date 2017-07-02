package com.sean.mreader;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class MReaderBR extends BroadcastReceiver {
	public MReaderBR() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("MReader", "!!onReceive:" + intent.getAction().toString());

		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			Intent intentBootComplete = new Intent(context, MReaderDialer.class);
			intentBootComplete.putExtra("action", "run first");
			//intent.putExtra(“text”,String.valueOf(editText.getText()));
			//context.startActivity(intentBootComplete);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentBootComplete, 0);


			Intent intentStartMReaderService = new Intent(context, MReaderService.class);
			intentStartMReaderService.putExtra("action", "run first");
			//intent.putExtra(“text”,String.valueOf(editText.getText()));
			//context.startActivity(intentBootComplete);
			PendingIntent pendingIntent2 = PendingIntent.getActivity(context, 0, intentStartMReaderService, 0);
			try {
				pendingIntent.send();
				pendingIntent2.send();
			}
			catch(PendingIntent.CanceledException e)    {
				e.printStackTrace();
			}
		}
		// 아래 intent 들은 receive 하지 않을 것이야...
		else if (intent.getAction().equals("android.intent.action.SCREEN_ON")) {
			Log.i("MReader", "Do something for SCRREN_ON");

		}
		else if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
			Log.i("MReader", "Do something for SCRREN_OFF");
		}
	}
}
