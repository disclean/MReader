package com.sean.mreader;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.ExpandedMenuView;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewDebug;
import android.widget.TextView;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static android.R.attr.filter;

public class MReaderDialer extends AppCompatActivity {
	final static int PERMISSIONS_REQUEST_CODE_READ_CONTACTS = 1;
	final static int PERMISSIONS_REQUEST_CODE_CALL_PHONE = 2;
	final static int PERMISSIONS_REQUEST_CODE_READ_SMS = 3;
	final static int PERMISSIONS_REQUEST_CODE_READ_CALLLOG = 4;

	private TextView mTextNum;
	private TextView mTextName;
	private TextView mTextMsg;

	private Cursor mCursorMissedCall;
	private Cursor mCursorSMS;

	private Vibrator mVibe;

	private static TextToSpeech mTTS = null;
	private static boolean mInited = false;
	private boolean mIsValidShortcut = false;
	private String mCurrentPhonenumber;
	private String mCurrentName;
	private int mMsgIdx = 0;
	private int mMissCallIdx = 0;
	private BroadcastReceiver mBR;
	private Boolean mFolderOpened;
	private Thread.UncaughtExceptionHandler mUncaughtExceptionHandler;

	private PhoneStateListener mPhoneStateListener;
	private static boolean mRunningCall;

	private void Log(String str) {

		/*
		System.out.println("===========디버깅 시작했다~================");
		System.out.print("file:" + (new Throwable()).getStackTrace()[0].getClassName() + "  line");
		System.out.println((new Throwable()).getStackTrace()[0].getLineNumber());
		System.out.println(a);
		System.out.println("===========디버깅 끝났다~================");
		*/

		Throwable trow = new Throwable();
		String classname = trow.getStackTrace()[0].getClassName();
		int linenum = trow.getStackTrace()[0].getLineNumber();

		//Log.i(classname, String.format("%d: %s"), linenum, str);
		Log.i(classname, linenum + ": " + str);
	}

	UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
		@Override
		public void onStart(String utteranceId) {
			Log.d("MReader", "onStart ( utteranceId :" + utteranceId + " ) ");
		}

		@Override
		public void onError(String utteranceId) {
			Log.d("MReader", "onError ( utteranceId :" + utteranceId + " ) ");
		}

		@Override
		public void onDone(String utteranceId) {
			Log.d("MReader", "onDone ( utteranceId :" + utteranceId + " ) ");
		}
		/*
		@Override
		public void onError(String utteranceId, int errorCode) {
			Log.d("MReader", "onError ( utteranceId :"+utteranceId+" ) ");
		}

		@Override
		public void onStop(String utteranceId, boolean interrupted)
		{
			Log.d("MReader", "onStop ( utteranceId :"+utteranceId+" ) ");
		}
		*/
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mreader_dialer);

		Log.i("MReader_Lifecycle", "onCreate 1439 ");

		mUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandlerApplication());

		deb("mTextNum is null?" + mTextNum);

		mTextNum = (TextView) findViewById(R.id.txtNum);
		mTextName = (TextView) findViewById(R.id.txtName);
		mTextMsg = (TextView) findViewById(R.id.txtMsg);

		mCurrentPhonenumber = "";
		mCurrentName = "";

		if (mInited == false) {
			Log.i("MReader_Dialer", "DIALER will init");

			mTTS = new TextToSpeech(this,
					new TextToSpeech.OnInitListener() {
						@Override
						public void onInit(int status) {
							deb("[onInit] status=" + status);
							if (status == TextToSpeech.SUCCESS) {
								deb("TextToSpeech.SUCCESS");
							} else {
								deb("mTTS status is not SUCCESS (" + status + ")");
							}
							mTTS.setOnUtteranceProgressListener(utteranceProgressListener);
						}
					});




			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			filter.addAction(Intent.ACTION_SCREEN_ON);

			if (mBR != null)    {
				Log.i("MReader_Dialer", "\n\n\nmBR is not null!!!!\n\n\n");
				unregisterReceiver(mBR);
				mBR = null;
			}

			mBR = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
						deb("mBR: SCREEN ON");
						mFolderOpened = true;
						Intent intentScreenOn = new Intent(context, MReaderDialer.class);
						intentScreenOn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intentScreenOn.putExtra("action", "SCREEN_ON");
						//context.startActivity(intentScreenOn);
						PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentScreenOn, 0);
						try						{
							pendingIntent.send();
						}
						catch(PendingIntent.CanceledException e)						{
							e.printStackTrace();
						}
					}
					else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
						deb("mBR: SCREEN ON");
						mFolderOpened = false;
						// speaking 동작 중지
						Intent intentScreenOff = new Intent(context, MReaderDialer.class);
						//intentScreenOff.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intentScreenOff.putExtra("action", "SCREEN_OFF");
						context.startActivity(intentScreenOff);
					}
				}
			};
			registerReceiver(mBR, filter);
			deb("mBR registered");
			deb("initializing done");
			mInited = true;
		} else {
			deb("skip initializing");
		}

		mPhoneStateListener = new PhoneStateListener()
		{
			public void onCallStateChanged(int state, String incomingNumber)
			{
				deb("state = " + state);
				if (state==TelephonyManager.CALL_STATE_RINGING ||
							state==TelephonyManager.CALL_STATE_OFFHOOK) {
					deb("mRunningCall is true");
					mRunningCall = true;
				}
				else {
					deb("mRunningCall is false");
					mRunningCall = false;
				}
			};
		};
		TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		manager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);


		mTextNum.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				deb("called");
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				deb("called");
			}

			@Override
			public void afterTextChanged(Editable editable) {
				deb("called");
				InputNumberChanged(editable.toString());
			}
		});

		// MReaderService 한테 intent 하나 보내자
		Intent intent = new Intent(this, com.sean.mreader.MReaderService.class);
		//intent = new Intent()
		startService(intent);

		if (getIntent() != null && getIntent().getAction() != null) {
			deb("Dialer got intent:" + getIntent().getAction().toString());

			String action = getIntent().getStringExtra("action");
			if (action != null) {
				if (action.equals("SCREEN_ON")) {
					deb("Got intent action SCREEN_ON");
//					mMsgIdx = 0;
				} else if (action.equals("SCREEN_OFF")) {
					deb("Got intent action SCREEN_OFF");
				}
			}
		}


		mFolderOpened = false;

		//InitShortcuts();

		//Toast.makeText(this, "Hi", Toast.LENGTH_LONG).show();
		mTextName.setText("");
		mVibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		//mTTS.speak("윤석준123", TextToSpeech.QUEUE_FLUSH, null);

		//MReaderTalker talker = new MReaderTalker();
		//Log.i("MReader_Dialer", "dialer send first intent");
		//startService(new Intent(this, MReaderTalker.class));
		//Log.i("MReader_Dialer", "dialer send second intent");
		//startService(new Intent(this, MReaderTalker.class));

	}

	private void deb(String message) {
		if (message == null) return;
		try {
			//Log.i("MReader_Dialer", "");
			//System.out.println("["+ this.getClass().getName()+"]" +message );
			//new Throwable().getStackTrace()[0].getLineNumber()
			Log.i("MReader_Dialer", "[" + new Throwable().getStackTrace()[1].getMethodName() +
					":" + new Throwable().getStackTrace()[1].getLineNumber() + "] " + message);

//					"file " + new Throwable().getStackTrace()[1].getFileName() +
//					" class " + new Throwable().getStackTrace()[1].getClassName() +
//					" method " + new Throwable().getStackTrace()[1].getMethodName() +
//					" line " + new Throwable().getStackTrace()[1].getLineNumber());


		} catch (NullPointerException n) {
			System.out.println("error!!....null!!" + this.getClass().getName());
		}
	}


	private void PrintCallLog(Cursor cur) {
		if (cur == null)
			return;

		int i = 1;
		cur.moveToFirst();
		do {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초.", Locale.KOREA);
			Date currentTime = new Date(Long.parseLong(
					cur.getString(cur.getColumnIndex(CallLog.Calls.DATE))));
			String dTime = formatter.format(currentTime);
			//				deb("[checkMissCall] query result:");
			Log.i("MReader_Dialer", i++ + ",\t" +
					dTime + ",\t" +
					cur.getString(cur.getColumnIndex(CallLog.Calls.NUMBER)) + ",\t" +
					cur.getString(cur.getColumnIndex(CallLog.Calls.TYPE)) + ",\t" +
					cur.getString(cur.getColumnIndex(CallLog.Calls.NEW)) + ",\t" +
//					cur.getString(cur.getColumnIndex(CallLog.Calls.CONTENT_ITEM_TYPE)) + ",\t" +
//					cur.getString(cur.getColumnIndex(CallLog.Calls.CONTENT_TYPE)) + ",\t" +
					cur.getString(cur.getColumnIndex(CallLog.Calls.IS_READ)));
		} while (cur.moveToNext());

	}

	private void PrintSMSLog(Cursor cur) {
		if (cur == null)
			return;

		int i = 1;
		cur.moveToFirst();
		do {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초.", Locale.KOREA);
			Date currentTime = new Date(Long.parseLong(
					cur.getString(cur.getColumnIndex(Telephony.TextBasedSmsColumns.DATE))));
			String dTime = formatter.format(currentTime);
			//				deb("[checkMissCall] query result:");

			Log.i("MReader_Dialer", i++ + ",\t" +
					dTime + ",\t" +
					cur.getString(cur.getColumnIndex(Telephony.TextBasedSmsColumns.THREAD_ID)) + ",\t" +
					cur.getString(cur.getColumnIndex("_id")) + ",\t" +
					cur.getString(cur.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS)) + ",\t" +
					cur.getString(cur.getColumnIndex(Telephony.TextBasedSmsColumns.PERSON)) + ",\t" +
					cur.getString(cur.getColumnIndex(Telephony.TextBasedSmsColumns.READ)) + ",\t" +
					cur.getString(cur.getColumnIndex(Telephony.TextBasedSmsColumns.SEEN)) + ",\t" +
					cur.getString(cur.getColumnIndex(Telephony.TextBasedSmsColumns.BODY))
			);
//					cur.getString(cur.getColumnIndex(CallLog.Calls.TYPE)) + ",\t" +
//					cur.getString(cur.getColumnIndex(CallLog.Calls.NEW)) + ",\t" +
//					cur.getString(cur.getColumnIndex(CallLog.Calls.IS_READ)
		} while (cur.moveToNext());

		//Telephony.Sms.DATE;
		//Telephony.Mms.DATE;

		//android.provider.CallLog.Calls.

	}

	private void InputNumberChanged(String num) {
		String str, phonenum, name;

		deb(num);
		// 단축키에 해당하는 연락처를 검색
		phonenum = getPhonenumByShortcut(num);
		if (phonenum == null) {
			phonenum = num;
		}
		deb("전화번호=" + phonenum);
		mCurrentPhonenumber = phonenum;

		name = getNameByPhonenumber(phonenum);
		deb("name=" + name);

		// 그 이름을 mTextName 에 업데이트
		if (name != null) {
			mTextName.setText(name);
			mTTS.speak(name, TextToSpeech.QUEUE_ADD, null);
			mCurrentName = name;
		} else {
			mTextName.setText("");
			mCurrentName = "\"" + mCurrentPhonenumber + "\"";
		}
	}

	private void checkUnreadSMS() {
		int cntMissCall;

		deb("called");
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
			deb("[checkUnreadSMS] CALL_LOG 권한이 없어 종료합니다");
			return;
		}
		Cursor cursor = getContentResolver().query(
				Uri.parse("content://sms"),
				null, // projection (all column)
				Telephony.TextBasedSmsColumns.READ + "=?", //selection (where)
				//null, // selection (where)
				new String[]{"0"}, // selection args
				//null, // selection args
				"date DESC"); // sort order
		/*
		Cursor cursor = getContentResolver().query(
				CallLog.Calls.CONTENT_URI,  // uri
				null, // projection (all columns)
				CallLog.Calls.TYPE + " = ? AND " + CallLog.Calls.IS_READ + " = ? ", // selection (where)
				//CallLog.Calls.TYPE + " = ? AND " + CallLog.Calls.NEW + " = ? AND " + CallLog.Calls.IS_READ + " = ? ", // selection (where)
				new String[] { Integer.toString(CallLog.Calls.MISSED_TYPE), "0" }, // selection args
				//new String[] { "0"}, // selection args
				CallLog.Calls.DATE + " DESC limit 100"); // sort order
		*/

		cntMissCall = cursor.getCount();
		deb("[checkUnreadSMS] Unread msg: " + cntMissCall + "개");
		if (cntMissCall > 0) {
			deb("[checkUnreadSMS] 한번 봅시다..");
			PrintSMSLog(cursor);
			// 부재중 전화 갯수를 말한다
			speakTilEnd(ordinal(cntMissCall) + " 개의 새로운 메세지가 있습니다.",
					TextToSpeech.QUEUE_FLUSH);
			// 다섯 개의 부재중 전화가 있습니다

		}
	}


	// 부재중 전화 갯수를 확인해서 1개 이상이면 갯수를 읽는다.
	private void checkMissCall() {
		int cntMissCall;

		deb("called");
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
			deb("[checkMissCall] CALL_LOG 권한이 없어 종료합니다");
			return;
		}
		Cursor cursor = getContentResolver().query(
				CallLog.Calls.CONTENT_URI,  // uri
				null, // projection (all columns)
				CallLog.Calls.TYPE + " = ? AND " + CallLog.Calls.IS_READ + " = ? ", // selection (where)
				//CallLog.Calls.TYPE + " = ? AND " + CallLog.Calls.NEW + " = ? AND " + CallLog.Calls.IS_READ + " = ? ", // selection (where)
				new String[]{Integer.toString(CallLog.Calls.MISSED_TYPE), "0"}, // selection args
				//new String[] { "0"}, // selection args
				CallLog.Calls.DATE + " DESC limit 100"); // sort order
		cntMissCall = cursor.getCount();
		deb("[checkMissCall] Unread misscall: " + cntMissCall + "개");
		if (cntMissCall > 0) {
			PrintCallLog(cursor);
			// 부재중 전화 갯수를 말한다
			speakTilEnd(ordinal(cntMissCall) + " 개의 부재중 전화가 있습니다.",
					TextToSpeech.QUEUE_FLUSH);
			// 다섯 개의 부재중 전화가 있습니다

		}
	}

	private String getLatestCallNum() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
			deb("[checkMissCall] CALL_LOG 권한이 없어 종료합니다");
			return null;
		}

		Cursor cursor = getContentResolver().query(
				CallLog.Calls.CONTENT_URI,  // uri
				null, // projection (all columns)
				CallLog.Calls.TYPE + " = ? OR " + CallLog.Calls.TYPE + " = ?", // selection (where)
				new String[]{"1", "2"}, // selection args
				CallLog.Calls.DATE + " DESC limit 99"); // sort order

		if (cursor.getCount() == 0) {
			return null;
		}
		PrintCallLog(cursor);
		cursor.moveToFirst();
		deb("latest NUMBER = " + cursor.getString(cursor.getColumnIndex("NUMBER")));
		return cursor.getString(cursor.getColumnIndex("NUMBER"));

	}

	void InitShortcuts() {
		deb("called");
		SharedPreferences pref = getSharedPreferences("shortcut", MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString("1", "01071453625");
		editor.putString("2", "01042581485");
		editor.putString("3", "01093021485");
		editor.putString("4", "01048533625");
		editor.putString("5", "01033481046");
		editor.putString("6", "01035817938");
		editor.putString("7", "01082901288");
		editor.putString("8", "01035631046");
		editor.putString("9", "0517543033");
		editor.putString("10", "01076764784");
		editor.putString("11", "0517551485");
		editor.putString("12", "01073373892");
		editor.putString("13", "01085863260");
		editor.putString("14", "01031342295");
		editor.putString("15", "0163602980");
		editor.putString("16", "01077334398");
		editor.putString("17", "01051909935");
		editor.putString("18", "01035759928");
		editor.putString("19", "01054782684");
		editor.putString("20", "01035697938");
		editor.putString("21", "01047135118");
		editor.putString("22", "0515838000");
		editor.putString("23", "01051938900");
		editor.putString("24", "01029457677");
		editor.putString("25", "01086206146");
		editor.putString("26", "01093326220");
		editor.putString("27", "01025735530");
		editor.putString("28", "01031210789");
		editor.putString("29", "01045802196");
		editor.putString("30", "0517511724");
		editor.putString("31", "01038453490");
		editor.putString("32", "01044975667");
		editor.putString("33", "01045544831");
		editor.putString("34", "01023911176");
		editor.putString("35", "01050431484");
		editor.putString("36", "0517317578");
		editor.putString("37", "01071427581");
		editor.commit();
	}

	@Override
	public boolean onKeyLongPress(int keycode, KeyEvent event) {
		switch (keycode) {
			case KeyEvent.KEYCODE_STAR:
				// 매너모드 설정,해제
				deb("Long press KEYCODE_STAR");
				AudioManager aManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				int curRingerMode = aManager.getRingerMode();
				if (curRingerMode == AudioManager.RINGER_MODE_VIBRATE) {
					// '매너모드가 해제됐습니다' 라고 읽자
					aManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
					if (false) {
						mTTS.speak("매너모드가 해제됐습니다", TextToSpeech.QUEUE_FLUSH, null);
					} else {
						// 띠리링 사운드 재생
						Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
						Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
						r.play();
					}
				} else {
					// 진동을 울리자
					mVibe.vibrate(500);
					aManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
				}

				//aManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
				//aManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

				return true;
			case KeyEvent.KEYCODE_POUND:
				deb("Long press KEYCODE_POUND");
				return true;
		}

		return super.onKeyLongPress(keycode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if ((event.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0) {
			if (keyCode == KeyEvent.KEYCODE_STAR) {
				deb("Short press KEYCODE_STAR");
				mMissCallIdx++;
				speakOneMissCall();
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_POUND) {
				deb("Short press KEYCODE_POUND");
				mMsgIdx++;
				speakOneSMS();
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		String str, ret;
		int num;

		/*
		// temp
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
			//keyCode = KeyEvent.KEYCODE_1;
			keyCode = KeyEvent.KEYCODE_STAR;
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
			keyCode = KeyEvent.KEYCODE_POUND;
			//keyCode = KeyEvent.KEYCODE_CALL;
		*/

		// 키패드 숫자 처리 읽기
		if (KeyEvent.KEYCODE_0 <= keyCode && keyCode <= KeyEvent.KEYCODE_9) {
			num = keyCode - 7;
			speakTilEnd(Integer.toString(num), TextToSpeech.QUEUE_FLUSH);
			handleNumKeycode(num);
			return true;
		}

		// 키패드 숫자를 제외한 KeyEvent 처리
		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				//int a = 10 / 0;
				break;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				break;
			case KeyEvent.KEYCODE_BACK:
				if (mTextNum.length() > 0) {
					mTextNum.setText(mTextNum.getText().subSequence(0, mTextNum.length() - 1).toString());
				}
				return true;
			//break;

			case KeyEvent.KEYCODE_STAR:
			case KeyEvent.KEYCODE_POUND:
				event.startTracking();
				return true;

			case KeyEvent.KEYCODE_CALL:
				// 전화걸기
				if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
					deb("전화걸기 권한이 필요합니다");

					if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
						deb("전화걸기 권한의 필요성을 설명합니다");
						// 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
						// 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다
						ActivityCompat.requestPermissions(this,
								new String[]{Manifest.permission.CALL_PHONE},
								PERMISSIONS_REQUEST_CODE_CALL_PHONE);
						// 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
						deb("requestPermission 했는데...2");

					} else {
						deb("전화걸기 권한의 필요성 설명이 필요없습니다. requestPermisstion 합니다");
						ActivityCompat.requestPermissions(this,
								new String[]{Manifest.permission.CALL_PHONE},
								PERMISSIONS_REQUEST_CODE_CALL_PHONE);
						// 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
						deb("requestPermission 했는데...");
					}
				} else {
					deb("전화걸기 권한이 있습니다.");
					deb("mCurrentPhonenumber=" + mCurrentPhonenumber);
					deb("mCurrentName=" + mCurrentName);

					if (mCurrentPhonenumber.equals("")) {
						String latestCall = getLatestCallNum();
						if (latestCall == null) {
							deb("최근에 전화걸었던 기록이 없습니다.");
							return true;
						}
						mTextNum.setText(latestCall);
						mCurrentPhonenumber = latestCall;
						mCurrentName = getNameByPhonenumber(latestCall);
						if (mCurrentName==null) {
							mCurrentName = latestCall;
						}
						//InputNumberChanged(latestCall);
						return true;
					}
					speakTilEnd(mCurrentName + " 에게 전화를 겁니다", TextToSpeech.QUEUE_FLUSH);
					/*
					mTTS.speak(mCurrentName + " 에게 전화를 겁니다", TextToSpeech.QUEUE_FLUSH, null);
					while (mTTS.isSpeaking()) {
						deb("wait until speaking is done");
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					*/
					startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mCurrentPhonenumber)));

					/*
					if (mCurrentPhonenumber != null) {
						if (mCurrentPhonenumber.equals("")) {
							deb("전화번호가 없어서 발신할 수 없습니다. (" + mCurrentPhonenumber + ")");
							return true;
						}
						// 단축키로 지정한 번호로 전화걸기

					} else {
						String number = mTextNum.getText().toString();
						String strContactName = getNameByPhonenumber(number);
						deb("strContactName=" + strContactName);

						if (number.equals("")) {
							String latestCall = getLatestCallNum();
							if (latestCall == null) {
								deb("전화번호가 전혀 없어서 발신할 수 없습니다.");
								return true;
							}
							mTextNum.setText(latestCall);
							InputNumberChanged(latestCall);
							return true;
						}

						if (strContactName == null) {
							// 날번호
							String tmp = number + " 에게 전화를 겁니다";
							deb(tmp);
							mTTS.speak(tmp, TextToSpeech.QUEUE_FLUSH, null);

						} else {
							// 단축키로 지정 안된 주소록에 저장된 번호
							String tmp = strContactName + " 에게 전화를 겁니다";
							deb(tmp);
							mTTS.speak(tmp, TextToSpeech.QUEUE_FLUSH, null);
						}
						while (mTTS.isSpeaking()) {
							deb("wait until speaking is done");
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mTextNum.getText().toString())));
					}
					*/
				}

				return true;
//				break;

		}

		return super.onKeyDown(keyCode, event);
	}

	void speakOneSMS() {
		int maxCount = 0;
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
			deb("문자읽기 권한이 필요합니다");

			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)) {
				deb("문자읽기 권한의 필요성을 설명합니다");
				// 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
				// 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.READ_SMS},
						PERMISSIONS_REQUEST_CODE_READ_SMS);
				// 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
				deb("requestPermission 했는데...2");

			} else {
				deb("문자읽기 권한의 필요성 설명이 필요없습니다. requestPermisstion 합니다");
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.READ_SMS},
						PERMISSIONS_REQUEST_CODE_READ_SMS);
				// 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
				deb("requestPermission 했는데...");
			}
		} else {
			deb("문자읽기 권한이 있습니다.");

			if (mMsgIdx <= 0) {
				deb("이전 메세지 없음");
				mTTS.speak("이전 메세지가 없습니다", TextToSpeech.QUEUE_FLUSH, null);
				mMsgIdx = 0;
				return;
			}

			Uri uri = Uri.parse("content://sms");
			int idxMove;
			String clause = "type=?";
			String order = "";
			String[] args = {""};
			//args[0] = "0";
			args[0] = "1";
			//Cursor cur = getContentResolver().query(uri, null, "read = 0", null, null);
//			Cursor cur = getContentResolver().query(Uri.parse("content://sms"), null, clause, args, null);
			Cursor cur = getContentResolver().query(
					Uri.parse("content://sms"),
					null,
					null,
					null,
					"date DESC limit 499");
			maxCount = cur.getCount();

			deb("쿼리 완료(" + maxCount + ")");

			if (mMsgIdx > maxCount) {
				deb("다음 메세지 없음");
				mTTS.speak("다음 메세지가 없습니다", TextToSpeech.QUEUE_FLUSH, null);
				mMsgIdx = maxCount;
				return;
			}

			deb("읽을 준비 완료");
			deb("mMsgIdx=" + mMsgIdx);

			cur.moveToFirst();
			idxMove = mMsgIdx;
			while (idxMove-- > 1) {
				cur.moveToNext();
			}

			String strToSpeak;
			String strFrom;
			String _id = cur.getString(cur.getColumnIndex("_id"));

			deb("total: " + cur.getCount());
			int i = 1;
			int max_msg = 1;
			//0 개의 새 메세지가 있습니다.
			//Log.i("MReader", "New Message: "+cur.getCount());
			//mTTS.speak(ordinal(cur.getCount()) + " 개의 새로은 메세지가 있습니다.", TextToSpeech.QUEUE_FLUSH, null);

			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "100");

			Log.i("MReader_Dialer", "_id=" + cur.getString(cur.getColumnIndex("_id")));
			Log.i("MReader_Dialer", "thread_id=" + cur.getString(cur.getColumnIndex("thread_id")));
			Log.i("MReader_Dialer", "address=" + cur.getString(cur.getColumnIndex("address")));
			Log.i("MReader_Dialer", "person=" + cur.getString(cur.getColumnIndex("address")));
			Log.i("MReader_Dialer", "date=" + cur.getString(cur.getColumnIndex("date")));
			Log.i("MReader_Dialer", "body=" + cur.getString(cur.getColumnIndex("body")));

			strFrom = getNameByPhonenumber(cur.getString(cur.getColumnIndex("address")));
			Log.i("MReader_Dialer", "strFrom=" + strFrom);
			if (strFrom == null) {
				strFrom = cur.getString(cur.getColumnIndex("address"));
			}
			Log.i("MReader_Dialer", "strFrom=" + strFrom);
/*
			Log.i("MReader", i+": " +
							strFrom+", "+
							mCursorMsg.getString(mCursorMsg.getColumnIndex("body"))+", "+
							mCursorMsg.getInt(mCursorMsg.getColumnIndex("read"))+", "
			);
			*/



			/*
			SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초", Locale.KOREA);
			String originDate = mDateFormat.format(cur.getString(cur.getColumnIndex("date")));
			mTextMsg.setText(originDate);
			*/

			strToSpeak = ordinal2(mMsgIdx) + " 메세지. (";
			strToSpeak += strFrom + ") 으로부터";
			strToSpeak += cur.getString(cur.getColumnIndex("body")) + ".";

			deb("!date = " + cur.getString(cur.getColumnIndex("date")));


			String curTime = new SimpleDateFormat("M월 d일 a h시 m분. ", Locale.KOREA).format(
					Long.parseLong(cur.getString(cur.getColumnIndex("date"))));
			strToSpeak += curTime;

			StringBuilder sb = new StringBuilder();
			sb.append(String.format("%d. From:%s, \n%s\n%s", mMsgIdx, strFrom, cur.getString(cur.getColumnIndex("body")), curTime));
			mTextMsg.setText(sb.toString());
/*
			SimpleDateFormat formatter = new SimpleDateFormat ( "yyyy년 MM월 dd일 HH시 mm분 ss초.", Locale.KOREA );
			Date currentTime = new Date(Long.parseLong(
					cur.getString(cur.getColumnIndex(CallLog.Calls.DATE))));
			String dTime = formatter.format ( currentTime );
			//				deb("[checkMissCall] query result:");

			*/

			mTTS.speak(strToSpeak, TextToSpeech.QUEUE_FLUSH, null);
//			i++;
			Log.i("MReader_Dialer", "strToSpeak: " + strToSpeak);


			Cursor curTmp = getContentResolver().query(
					Uri.parse("content://sms"),
					null,
					"_id=?",
					new String[]{_id}, // selection args
					"date DESC");

			Log.i("MReader_Dialer", "왜 update 가 fail 되는지 봅시다");
			PrintSMSLog(curTmp);
			curTmp.close();
			curTmp = null;


			// READ를 1로 update
			ContentValues newValue = new ContentValues();
			//newValue.put(Telephony.TextBasedSmsColumns.READ, "1");
			newValue.put("read", 1);
			//newValue.put(CallLog.Calls.NEW, "0");
			//String where = "_id="+_id;
			String where = "read=0";

			int result = getContentResolver().update(
					Uri.parse("content://sms"), newValue, where, null);
			if (result == 0) {
				Log.i("MReader_Dialer", "read update failed.");
			}

		}

	}

	void speakOneMissCall() {
		int maxCount = 0;
		int idxMove = 0;
		boolean isValid = false;

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
			Log.i("MReader_Dialer", "콜로그 읽기 권한이 필요합니다");

			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALL_LOG)) {
				Log.i("MReader_Dialer", "콜로그 읽기 권한의 필요성을 설명합니다");
				// 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
				// 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.READ_CALL_LOG},
						PERMISSIONS_REQUEST_CODE_READ_CALLLOG);
				// 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
				Log.i("MReader_Dialer", "requestPermission 했는데...3");

			} else {
				Log.i("MReader_Dialer", "콜로그 읽기 권한의 필요성 설명이 필요없습니다. requestPermisstion 합니다");
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.READ_CALL_LOG},
						PERMISSIONS_REQUEST_CODE_READ_SMS);
				// 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
				Log.i("MReader_Dialer", "requestPermission 했는데...");
			}
			return;
		}
		Log.i("MReader_Dialer", "콜로그 읽기 권한이 있습니다.");

		// null 로 체크해야 되나 .isClosed()로 체크해야되나?
		if (mCursorMissedCall == null) {
			mCursorMissedCall = getContentResolver().query(
					Uri.parse("content://call_log/calls"),  //CallLog.Calls.CONTENT_URI,
					null,
					CallLog.Calls.TYPE + " = ?",
					new String[]{Integer.toString(CallLog.Calls.MISSED_TYPE)},
					CallLog.Calls.DATE + " DESC limit 100");
			maxCount = mCursorMissedCall.getCount();
			Log.i("MReader_Dialer", "부재 전화 쿼리 완료(" + maxCount + ")");

			PrintCallLog(mCursorMissedCall);
			isValid = mCursorMissedCall.moveToFirst();
		} else {
			isValid = mCursorMissedCall.moveToNext();
		}

		if (!isValid) {
			// row 가 없음
			mTTS.speak("이전 부재전화가 없습니다", TextToSpeech.QUEUE_FLUSH, null);
			return;
		} else {
			String strToSpeak;
			String strFrom;
			int i = 1;
			int max_msg = 1;

			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "100");
/*
		Log.i("MReader_Dialer", "_id="+cur.getString(cur.getColumnIndex("_id")));
		Log.i("MReader_Dialer", "thread_id="+cur.getString(cur.getColumnIndex("thread_id")));
		Log.i("MReader_Dialer", "address="+cur.getString(cur.getColumnIndex("address")));
		Log.i("MReader_Dialer", "person="+cur.getString(cur.getColumnIndex("address")));
		Log.i("MReader_Dialer", "date="+cur.getString(cur.getColumnIndex("date")));
		Log.i("MReader_Dialer", "body="+cur.getString(cur.getColumnIndex("body")));
*/

			String phoneNumber = mCursorMissedCall.getString(
					mCursorMissedCall.getColumnIndex(CallLog.Calls.NUMBER));
			strFrom = getNameByPhonenumber(phoneNumber);
			Log.i("MReader_Dialer", "strFrom=" + strFrom);
			if (strFrom == null) {
				strFrom = phoneNumber;
			}
			Log.i("MReader_Dialer", "strFrom=" + strFrom);

		/*
		SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초", Locale.KOREA);
		String originDate = mDateFormat.format(cur.getString(cur.getColumnIndex("date")));
		mTextMsg.setText(originDate);
		*/

			strToSpeak = ordinal2(mMissCallIdx) + " 부재전화. '";
			strToSpeak += strFrom + "' . ";
			SimpleDateFormat formatter = new SimpleDateFormat("M월 d일 a h시 m분.", Locale.KOREA);
			Date currentTime = new Date(Long.parseLong(
					mCursorMissedCall.getString(
							mCursorMissedCall.getColumnIndex(
									CallLog.Calls.DATE))));
			String dTime = formatter.format(currentTime);
			Log.i("MReader_Dialer", "Date : " + dTime);
			strToSpeak += dTime;

			StringBuilder sb = new StringBuilder();
			sb.append(String.format("From:%s.\n%s", strFrom, dTime));
			mTextMsg.setText(sb.toString());

//		strToSpeak += cur.getString(cur.getColumnIndex("body")) + ".";
			if (mTTS != null)
				mTTS.speak(strToSpeak, TextToSpeech.QUEUE_FLUSH, null);
			else
				Log.i("MReader_Dialer", "\n\n\n mTTS is null!!\n\n\n");

			Log.i("MReader_Dialer", "strToSpeak: " + strToSpeak);

			// IS_READ를 1로 update 해야 하지 않을까
			ContentValues newValue = new ContentValues();
			newValue.put(CallLog.Calls.IS_READ, "1");
			//newValue.put(CallLog.Calls.NEW, "0");
			String where = "IS_READ=0";

			int result = getContentResolver().update(Uri.parse("content://call_log/calls"), newValue, where, null);
			if (result == 0) {
				Log.i("MReader_Dialer", "is_read update failed.");
			}


		}
		/*
		cur.moveToFirst();
		while (cur.moveToNext()) {
			Log.i("MReader_Dialer",
					"[NUMBER] " + cur.getString(cur.getColumnIndex(CallLog.Calls.NUMBER)));
			String callDate = cur.getString(cur.getColumnIndex(CallLog.Calls.DATE));
			Date callDayTime = new Date(Long.valueOf(callDate));
			Log.i("MReader_Dialer",
					"[DATE] " + callDayTime);
		}
		*/


	}

	public String ordinal(int num) {
		String strOne[] = {"", "한", "두", "세", "네", "다섯", "여섯", "일곱", "여덟", "아홉"};
		String strTen[] = {"", "열", "스물", "서른", "마흔", "쉰", "예순", "일흔", "여든", "아흔"};
		int ten, one;

		if (num >= 100) {
			return "여러";
		}

		ten = num / 10;
		one = num % 10;
		return strTen[ten] + ", " + strOne[one];
	}

	public String ordinal2(int num) {
		String strOne[] = {"", "한번째", "두번째", "세번째", "네번째", "다섯번째", "여섯번째", "일곱번째", "여덟번째", "아홉번째"};
		String strTen[] = {"", "열", "스물", "서른", "마흔", "쉰", "예순", "일흔", "여든", "아흔"};
		int ten, one;

		if (num >= 100) return "여러";
		if (num == 1) return "첫번째";
		if (num == 10) return "열번째";
		if (num == 20) return "스무번째";
		if (num == 30) return "서른번째";
		if (num == 40) return "마흔번째";
		if (num == 50) return "쉰번째";
		if (num == 60) return "예순번째";
		if (num == 70) return "일흔번째";
		if (num == 80) return "여든번째";
		if (num == 90) return "아흔번째";

		ten = num / 10;
		one = num % 10;
		return strTen[ten] + " " + strOne[one];
	}

	private void handleNumKeycode(int num) {
		String str, phonenum, name;
		str = mTextNum.getText().toString() + Integer.toString(num);
		mTextNum.setText(str);
	}

	private String getNameByPhonenumber(String number) {
		String strName;
		Cursor curContact;
		Uri uriContact = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

		if (number.equals("")) {
			deb("getNameByPhonenumber] number=" + number);
			return null;
		}

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
			deb("권한이 필요합니다");

			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
				deb("권한의 필요성을 설명합니다");
				// 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
				// 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.READ_CONTACTS},
						PERMISSIONS_REQUEST_CODE_READ_CONTACTS);
				// 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
				deb("requestPermission 했는데...2");

			} else {
				deb("권한의 필요성 설명이 필요없습니다. requestPermisstion 합니다");
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.READ_CONTACTS},
						PERMISSIONS_REQUEST_CODE_READ_CONTACTS);
				// 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
				deb("requestPermission 했는데...");
			}
		} else {
			deb("권한이 있습니다.");

			curContact = getContentResolver().query(
					uriContact,
					null,
					null,
					null,
					null);

			// check whether the phone number is saved or not
			if (curContact.getCount() > 0) {
				deb("연락처 있음");
				curContact.moveToNext();
/*
			for (int j = 0 ; j<22 ; j++)    {
				Log.i("MReader", "j="+j+": "+curContact.getColumnName(j) + "=" + curContact.getString(j));
			}
*/
				strName = curContact.getString(curContact.getColumnIndex("display_name"));
				deb("[getNameByPhonenumber] found " + number + ":" + strName);
				return strName;
			}
			deb("not found " + number);
			curContact.close();
		}


		return null;

	}

	/*
		boolean checkAndRequestPermission(String permission)
		{
			switch (permission) {
				case Manifest.permission.READ_CONTACTS:
					if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
						Log.i("MReader_Dialer", "권한이 필요합니다");

						if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_CONTACTS)) {
							Log.i("MReader_Dialer", "권한의 필요성을 설명합니다");
							// 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
							// 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다

						} else {
							Log.i("MReader_Dialer", "권한의 필요성 설명이 필요없습니다. requestPermisstion 합니다");
							ActivityCompat.requestPermissions(this,
									new String[]{Manifest.permission.READ_CONTACTS},
									PERMISSIONS_REQUEST_CODE_READ_CONTACTS);
							// 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
						}
					}
					else{
						Log.i("MReader_Dialer", "권한이 있습니다.");
						//Toast.makeText(this, "주소록 읽기 권한이 없어 프로그램을 종료합니다", Toast.LENGTH_LONG).show();
						//return false;
						return true;
					}
					break;
			}
			return false;

		}
	*/
	String getPhonenumByShortcut(String sNum) {
		int i;
		SharedPreferences pref = getSharedPreferences("shortcut", MODE_PRIVATE);
		String ret = pref.getString(sNum, null);

		if (pref == null) {
			deb("pref is null");
			return null;
		}

		if (ret == null) {
			deb("ret is null");
			return null;
		}

		if (ret.equals("")) {
			deb("ret is blank");
			return null;
		}
		deb("found " + ret);
		return ret;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       String permissions[], int[] grantResults) {
		Log.i("MReader_Dialer", "onRequestPermissionsResult 호출됨(" + requestCode + ")");

		switch (requestCode) {
			case PERMISSIONS_REQUEST_CODE_READ_CONTACTS:
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// 권한 허가
					// 해당 권한을 사용해서 작업을 진행할 수 있습니다
					Log.i("MReader_Dialer", "READ_CONTACTS 권한 획득");

				} else {
					// 권한 거부
					// 사용자가 해당권한을 거부했을때 해주어야 할 동작을 수행합니다
					Log.i("MReader_Dialer", "READ_CONTACTS 권한 획득 실패");
				}
				return;
		}
	}

	public void btnChangeShortcutHandler(View v) {
		/*
		//여기에다 할 일을 적어주세요.
		Log.i("MReader_Dialer", "btnChangeShortcutHandler 호출됨");
		//mTTS.speak(mCurrentName + " 에게 전화를 겁니다", TextToSpeech.QUEUE_FLUSH, null);
		//v.getContext();
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			Log.i("MReader_Dialer", "여기가 뭐시당가");
			return;
		}
		Log.i("MReader_Dialer", "전화걸어보자");
		v.getContext().startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:01040922684")));
		*/
		v.getContext().startActivity(new Intent(v.getContext(), ChangeShortcut.class));

		//v.getContext().startActivity(new Intent(v.getContext(), ContactListActivity.class));

		/*
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
		startActivityForResult(intent, 0);
		*/
	}

	private void dialWithNumber(String num) {
		// 주소록에 저장된 번호면 'XXX 에게 전화를 겁니다' 라고 말하고 전화검

		// 주소록에 저장안된 번호면 '010XXXX' 에게 전화를 겁니다.' 라고 말하고 전화검
	}

	private void speakTilEnd(String str, int queueMode) {
		mTTS.speak(str, queueMode, null);
		while (mTTS.isSpeaking()) {
			Log.i("MReader_Dialer", "wait until speaking is done");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void XXXX(int type, String content) {

	}

	@Override
	public void onStart() {
		Log.i("MReader_Lifecycle", "onStart");
		super.onStart();
		return;
	}

	@Override
	public void onResume() {
		Log.i("MReader_Lifecycle", "onResume");
		mCursorMissedCall = null;
		/*
		if (mFolderOpened) {

		} else {
			deb("onResume 이지만 폴더가 닫혀 있어서 무시함");
		}
		*/
		deb("멤버 변수를 초기화해봅니다");
		mCurrentName = "";
		mCurrentPhonenumber = "";
		mTextName.setText("");
		mTextNum.setText("");
		mTextMsg.setText("");
		mMsgIdx = 0;
		mMissCallIdx = 0;

		if (mRunningCall==false) {
			// 현재 시간을 읽는다.
			String curTime = new SimpleDateFormat("a h시 m분. ", Locale.KOREA).format(
					new Date(System.currentTimeMillis()));
			deb("현재시간: " + curTime);
			mTTS.speak(curTime, TextToSpeech.QUEUE_FLUSH, null);
		}
		else {
			deb("통화중이라 시간 안읽음");
		}

		//checkMissCall();
		//checkUnreadSMS();


		super.onResume();
		return;
	}

	@Override
	public void onPause() {
		Log.i("MReader_Lifecycle", "onPause");
		if (mTTS != null && mTTS.isSpeaking()) {
			Log.i("MReader_Dialer", "will stop speaking");
			mTTS.stop();
		}

		super.onPause();
		return;
	}

	@Override
	public void onDestroy() {
		Log.i("MReader_Lifecycle", "onDestroy");
		Log.i("MReader", "MReader_Dialer onDestroy");
		if (mBR != null)
			unregisterReceiver(mBR);
		else
			Log.i("MReader", "mBR is NULL, can't unregister it");
		if (mTTS != null) {
			mTTS.stop();
			mTTS.shutdown();
		}
		super.onDestroy();
	}


	private class UncaughtExceptionHandlerApplication implements Thread.UncaughtExceptionHandler {
		@Override
		public void uncaughtException(Thread thread, Throwable throwable) {
			//예외상황이 발행 되는 경우 작업
			Log.e("MREADER!!!", getStackTrace(throwable));
			//예외처리를 하지 않고 DefaultUncaughtException으로 넘긴다.
			mUncaughtExceptionHandler.uncaughtException(thread, throwable);
		}

		private String getStackTrace(Throwable th) {

			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);

			Throwable cause = th;
			while (cause != null) {
				cause.printStackTrace(printWriter);
				cause = cause.getCause();
			}
			final String stacktraceAsString = result.toString();
			printWriter.close();

			return stacktraceAsString;
		}
	}
}



