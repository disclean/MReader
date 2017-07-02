package com.sean.mreader;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ChangeShortcut extends AppCompatActivity implements ShortcutItem.CommActivity{

	final static int REQUEST_CODE_GET_ONE_CONTACT = 0;

	private ShortcutItem mCurItem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		int i;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_change_shortcut);
		/*
		SharedPreferences pref = getSharedPreferences("shortcut", MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString("1", "01054782684");
		//editor.putString("1", "01040922684");
		editor.putString("2", "01093021485");
		editor.putString("11", "01040922684");
		editor.putString("12", "01022214537");
		editor.putString("21", "01034081575");editor.commit();
		*/
		ScrollView scrollView = new ScrollView(this);


		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);

		SharedPreferences pref = getSharedPreferences("shortcut", MODE_PRIVATE);
		//pref.getString("1", "");
		String itemNumber;
		ShortcutItem item;

		for (i=1 ; i<=100 ; i++) {
			itemNumber = pref.getString(Integer.toString(i), "");
			if (itemNumber.equals(""))    {
				// '등록' 버튼으로 생성
				item = new ShortcutItem(this, i);
				item.setAssigned(false);
			}
			else    {
				// 저장된 연락처로 표시
				//Log.i("MReader_ChangecutItem", "i="+i);
				item = new ShortcutItem(this, i, getNameByPhonenumber(itemNumber), itemNumber);
				item.setAssigned(true);
			}
			// activity 에 view 추가
			linearLayout.addView(item);
		}

		scrollView.addView(linearLayout);
		setContentView(scrollView);
		//setContentView(linearLayout);

		/*
		TextView view = new TextView(this);
		view.setText("Hello");
		view.setBackgroundResource(R.color.colorPrimary);
		*/
		//ViewGroup.LayoutParams param = new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

	}

	public void btnChangeShortcutHandler(View view) {

	}

	@Override
	public void AssignOneSC(ShortcutItem item) {
		Log.i("MReader_ChangeShortcut", "AssignOneSC() called");

		mCurItem = item;

		// PICKUP activity 띄워서 결과값 리턴
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
		startActivityForResult(intent, REQUEST_CODE_GET_ONE_CONTACT);

		Log.i("MReader_ChangeShortcut", "item.mIdxSC: " + item.mIdxSC);
		return;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		String nameSC, numberSC;
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_GET_ONE_CONTACT)    {
			// 수행을 제대로 한 경우
			if(resultCode == RESULT_OK && data != null)			{
				String result = data.getStringExtra("resultSetting");
				// 여기서 결과값을 SCItem 에게 던져주면 될듯.
				Cursor cursor = getContentResolver().query(data.getData(),
						new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
								ContactsContract.CommonDataKinds.Phone.NUMBER},
						null,
						null,
						null);
				cursor.moveToFirst();
				nameSC = cursor.getString(0);   //이름 얻어오기
				numberSC = cursor.getString(1); //번호 얻어오기
				//String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				cursor.close();

				Log.i("MReader_ChangeShortcut", "name: " + nameSC);
				Log.i("MReader_ChangeShortcut", "number: " + numberSC);

				mCurItem.setAssignedWithNameNumber(nameSC, numberSC);

				mCurItem = null;
				Toast.makeText(this, "등록했습니다", Toast.LENGTH_SHORT).show();
			}
			// 수행을 제대로 하지 못한 경우
			else if(resultCode == RESULT_CANCELED)			{

			}
		}

	}

	private String getNameByPhonenumber(String number) {
		String strName;
		Cursor curContact;
		Uri uriContact = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

		curContact = getContentResolver().query(
				uriContact,
				null,
				null,
				null,
				null);

		// check whether the phone number is saved or not
		if (curContact.getCount() > 0) {
			Log.i("MReader_ChangeShortcut", "연락처 있음");
			curContact.moveToNext();
			strName = curContact.getString(curContact.getColumnIndex("display_name"));
			Log.i("MReader_ChangeShortcut", "[getNameByPhonenumber] found " + number + ":" + strName);
			return strName;
		}
		Log.i("MReader_ChangeShortcut", "[getNameByPhonenumber] not found " + number);
		curContact.close();
		return null;

	}
}
