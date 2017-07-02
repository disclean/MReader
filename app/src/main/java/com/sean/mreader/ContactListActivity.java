package com.sean.mreader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import static com.sean.mreader.MReaderDialer.PERMISSIONS_REQUEST_CODE_READ_CONTACTS;
import static com.sean.mreader.MReaderDialer.PERMISSIONS_REQUEST_CODE_READ_SMS;

public class ContactListActivity extends AppCompatActivity {
	private ListView mListView;
	private ArrayAdapter<String> mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_list);

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
			Log.i("MReader_contactList", "연락처 읽기 권한이 필요합니다");

			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
				Log.i("MReader_contactList", "연락처 읽기 권한의 필요성을 설명합니다");
				// 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
				// 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.READ_CONTACTS},
						PERMISSIONS_REQUEST_CODE_READ_CONTACTS);
				// 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
				Log.i("MReader_contactList", "requestPermission 했는데...5548");

			} else {
				Log.i("MReader_contactList", "연락처 읽기 권한의 필요성 설명이 필요없습니다. requestPermisstion 합니다");
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.READ_CONTACTS},
						PERMISSIONS_REQUEST_CODE_READ_CONTACTS);
				// 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다
				Log.i("MReader_contactList", "requestPermission 했는데...5548");
			}
			Log.i("MReader_contactList", "what's going on...");
			return;
		}
		String sss[] = {
				ContactsContract.Contacts._ID,
				ContactsContract.Contacts.DISPLAY_NAME,
				};


		Cursor cur = getContentResolver().query(
				ContactsContract.Contacts.CONTENT_URI,
				null,   // 모든 열을 가져옴
				null,   // where 절 없음,, "_id=?"
				null,   // ? 에 해당하는 배열값,, new String[] {23]
				"DISPLAY_NAME ASC");

        mAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_2);

        // Xml에서 추가한 ListView 연결
        mListView = (ListView) findViewById(R.id.contactList);

        // ListView에 어댑터 연결
        mListView.setAdapter(mAdapter);

        // ListView 아이템 터치 시 이벤트 추가
        mListView.setOnItemClickListener(onClickListItem);

		//ContactsContract.CommonDataKinds.Phone.NUMBER
		//ContactsContract.CommonDataKinds.Phone.NUMBER

		cur.moveToFirst();
		while (cur.moveToNext())    {
			mAdapter.add(cur.getString(cur.getColumnIndex("DISPLAY_NAME"))+ " : " +
					cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
			Log.i("MReader_contactList", "_id="+cur.getString(cur.getColumnIndex("_id")));
		}
        // ListView에 아이템 추가

        mAdapter.add("디아블로");

	}

	// 아이템 터치 이벤트
	private AdapterView.OnItemClickListener onClickListItem = new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			// 이벤트 발생 시 해당 아이템 위치의 텍스트를 출력
			//Toast.makeText(getApplicationContext(), m_Adapter.getItem(arg2), Toast.LENGTH_SHORT).show();
		}
	};
}
