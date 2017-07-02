package com.sean.mreader;

import android.database.Cursor;
import android.provider.ContactsContract;

/**
 * Created by Sean on 2017-01-01.
 */

public class Utils {
	public static String getContactNameByNumber(String number)
	{
		/*
		String nameSC;
		Cursor cursor = getContentResolver().query(data.getData(),
				new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
						ContactsContract.CommonDataKinds.Phone.NUMBER},
				null,
				null,
				null);
		if (cursor.getCount()<=0)
			return null;

		cursor.moveToFirst();
		nameSC = cursor.getString(0);   //이름 얻어오기
		cursor.close();
		return nameSC;
		*/
		return "";
	}
}
