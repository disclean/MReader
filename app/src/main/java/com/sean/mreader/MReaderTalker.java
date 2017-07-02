package com.sean.mreader;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MReaderTalker extends IntentService {
	// IntentService 에서는 static 변수 사용을 추천하지 않는 듯
	// MReaderTalker 의 필요성 자체가 모호해진다. MReaderDialer 에서 전부 수행해도 될듯.
	static TextToSpeech mTTS = null;
	static boolean mInited = false;

	UtteranceProgressListener utteranceProgressListener=new UtteranceProgressListener() {
		@Override
		public void onStart(String utteranceId) {
			Log.d("MReader_Talker", "onStart ( utteranceId :"+utteranceId+" ) ");
		}

		@Override
		public void onError(String utteranceId) {
			Log.d("MReader_Talker", "onError ( utteranceId :"+utteranceId+" ) ");
		}

		@Override
		public void onDone(String utteranceId) {
			Log.d("MReader_Talker", "onDone ( utteranceId :"+utteranceId+" ) ");
		}
		/*
		@Override
		public void onError(String utteranceId, int errorCode) {
			Log.d("MReader_Talker", "onError ( utteranceId :"+utteranceId+" ) ");
		}

		@Override
		public void onStop(String utteranceId, boolean interrupted)
		{
			Log.d("MReader_Talker", "onStop ( utteranceId :"+utteranceId+" ) ");
		}
		*/
	};

	// TODO: Rename actions, choose action names that describe tasks that this
	// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
	private static final String ACTION_FOO = "com.sean.mreader.action.FOO";
	private static final String ACTION_BAZ = "com.sean.mreader.action.BAZ";

	// TODO: Rename parameters
	private static final String EXTRA_PARAM1 = "com.sean.mreader.extra.PARAM1";
	private static final String EXTRA_PARAM2 = "com.sean.mreader.extra.PARAM2";

	/**
	 * Starts this service to perform action Foo with the given parameters. If
	 * the service is already performing a task this action will be queued.
	 *
	 * @see IntentService
	 */
	// TODO: Customize helper method
	public static void startActionFoo(Context context, String param1, String param2) {
		Intent intent = new Intent(context, MReaderTalker.class);
		intent.setAction(ACTION_FOO);
		intent.putExtra(EXTRA_PARAM1, param1);
		intent.putExtra(EXTRA_PARAM2, param2);
		context.startService(intent);
	}

	/**
	 * Starts this service to perform action Baz with the given parameters. If
	 * the service is already performing a task this action will be queued.
	 *
	 * @see IntentService
	 */
	// TODO: Customize helper method
	public static void startActionBaz(Context context, String param1, String param2) {
		Intent intent = new Intent(context, MReaderTalker.class);
		intent.setAction(ACTION_BAZ);
		intent.putExtra(EXTRA_PARAM1, param1);
		intent.putExtra(EXTRA_PARAM2, param2);
		context.startService(intent);
	}

	public MReaderTalker() {
		super("MReaderTalker");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		/*
		if (intent != null) {
			final String action = intent.getAction();
			if (ACTION_FOO.equals(action)) {
				final String param1 = intent.getStringExtra(EXTRA_PARAM1);
				final String param2 = intent.getStringExtra(EXTRA_PARAM2);
				handleActionFoo(param1, param2);
			} else if (ACTION_BAZ.equals(action)) {
				final String param1 = intent.getStringExtra(EXTRA_PARAM1);
				final String param2 = intent.getStringExtra(EXTRA_PARAM2);
				handleActionBaz(param1, param2);
			}
		}
		*/

		Log.i("MReader_Talker", "onHandleIntent called");

		if (mTTS == null) {
			Log.i("MReader_Talker", "mTTS is null");
		}
		else    {
			Log.i("MReader_Talker", "mTTS is not null");
		}

		if (!mInited) {
			Log.i("MReader_Talker", "mInited is null");
		}
		else    {
			Log.i("MReader_Talker", "mInited is not null");
		}

		if (mInited==false) {
			Log.i("MReader_Talker", "Talker will init");

			mTTS = new TextToSpeech(this,
					new TextToSpeech.OnInitListener() {
				@Override
				public void onInit(int status) {
					Log.i("MReader_Talker", "[TALKER onInit] status="+status);
					if (status == TextToSpeech.SUCCESS) {
						Log.i("MReader_Talker", "TALKER TextToSpeech.SUCCESS");
					}
					mTTS.setOnUtteranceProgressListener(utteranceProgressListener);
				}
			});
			Log.i("MReader_Talker", "TALKER do some initializing");

			mInited = true;
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Log.i("MReader_Talker", "mInited="+mInited);

		mTTS.speak("부끄러워지네요", TextToSpeech.QUEUE_FLUSH, null);
	}

	/**
	 * Handle action Foo in the provided background thread with the provided
	 * parameters.
	 */
	private void handleActionFoo(String param1, String param2) {
		// TODO: Handle action Foo
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Handle action Baz in the provided background thread with the provided
	 * parameters.
	 */
	private void handleActionBaz(String param1, String param2) {
		// TODO: Handle action Baz
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
