package com.sean.mreader;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

/**
 * TODO: document your custom view class.
 */
public class ShortcutItem extends LinearLayout {

	final static int STATE_REG=0;
	final static int STATE_UNREG=1;
	private String mExampleString; // TODO: use a default from R.string...
	private int mExampleColor = Color.RED; // TODO: use a default from R.color...
	private float mExampleDimension = 0; // TODO: use a default from R.dimen...
	private Drawable mExampleDrawable;
	public int mIdxSC;
	private String mNameSC;
	private String mNumberSC;
	private int mState;
	private Context mContext;

	private TextView mViewID;
	private TextView mViewName;
	private TextView mViewNumber;
	private TextView mViewBtnReg;
	//private TextView mViewBtnUnreg;

	private TextPaint mTextPaint;
	private float mTextWidth;
	private float mTextHeight;

	CommActivity mCommListener;

	public ShortcutItem(Context context) {
		super(context);
		init(null, 0);
		mContext = context;
		initViews();


		Activity activity = (Activity)context;
		if(activity instanceof CommActivity)		{
			mCommListener = (CommActivity)activity;
		}
		else		{
			throw new ClassCastException();
		}

	}

	public ShortcutItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
		mContext = context;
		initViews();


		Activity activity = (Activity)context;
		if(activity instanceof CommActivity)		{
			mCommListener = (CommActivity)activity;
		}
		else		{
			throw new ClassCastException();
		}
	}

	public ShortcutItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
		mContext = context;
		initViews();


		Activity activity = (Activity)context;
		if(activity instanceof CommActivity)		{
			mCommListener = (CommActivity)activity;
		}
		else		{
			throw new ClassCastException();
		}
	}

	public ShortcutItem(Context context, int idxSC) {
		super(context);
		mIdxSC = idxSC;
		mContext = context;
		initViews();

		setAssigned(false);
		//init(attrs, defStyle);

		Activity activity = (Activity)context;
		if(activity instanceof CommActivity)		{
			mCommListener = (CommActivity)activity;
		}
		else		{
			throw new ClassCastException();
		}
	}
	public ShortcutItem(Context context, int idxSC, String name, String number) {
		super(context);


		mIdxSC = idxSC;
		mNameSC = name;
		mNumberSC = number;
		mContext = context;

		initViews();

		setAssigned(true);

//		mViewID.setText(Integer.toString(mIdxSC));
		//mViewName.setText(mNameSC);
		//mViewNumber.setText(mNumberSC);

		//init(attrs, defStyle);
	}

	private void deb(String message) {
		if(message == null) return ;
		try
		{
			Log.i("MReader_ShortcutItem", "[" + new Throwable().getStackTrace()[1].getMethodName() +
					":" + new Throwable().getStackTrace()[1].getLineNumber() + "] " + message);
		}catch(NullPointerException n)
		{
			System.out.println("error!!....null!!" + this.getClass().getName());
		}
	}

	interface CommActivity
	{
		public void AssignOneSC(ShortcutItem item);
	}

	public void PickupOneContact(ShortcutItem item)
	{
		mCommListener.AssignOneSC(item);
	}

	public void setAssignedWithNameNumber(String name, String number)
	{
		mNameSC = name;
		mNumberSC = number;

		setAssigned(true);

		/*
		SharedPreferences pref = getContext().getSharedPreferences("shortcut", MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString(Integer.toString(mIdxSC), number);
		editor.commit();
		*/
	}

	private OnClickListener onclickReg = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			//Intent intent = new Intent(Intent.ACTION_PICK);
			//intent.setData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
			//getContext().startActivityFor

			PickupOneContact((ShortcutItem)view.getParent().getParent());


			/*
			Log.i("MReader_ShortcutItem", "view.getClass(): " + view.getClass().toString());
			Log.i("MReader_ShortcutItem", "getClass(): " + getClass().toString());

			Log.i("MReader_ShortcutItem", "view.getParent(): " + view.getParent().getClass().toString());
			Log.i("MReader_ShortcutItem", "view.getParent().getParent(): " + view.getParent().getParent().getClass().toString());
			Log.i("MReader_ShortcutItem", "getParent(): " + getParent().getClass().toString());

			01-01 12:38:50.686 11830-11830/com.sean.mreader I/MReader_ShortcutItem: view.getClass(): class android.widget.TextView
			01-01 12:38:50.686 11830-11830/com.sean.mreader I/MReader_ShortcutItem: getClass(): class com.sean.mreader.ShortcutItem$1

			01-01 12:38:50.686 11830-11830/com.sean.mreader I/MReader_ShortcutItem: view.getParent(): class android.widget.LinearLayout
			01-01 12:38:50.686 11830-11830/com.sean.mreader I/MReader_ShortcutItem: view.getParent().getParent(): class com.sean.mreader.ShortcutItem
			01-01 12:38:50.686 11830-11830/com.sean.mreader I/MReader_ShortcutItem: getParent(): class android.widget.LinearLayout
			 */
		}
	};

	private OnClickListener onclickUnReg = new OnClickListener() {
		@Override
		public void onClick(View view) {

			ShortcutItem item = (ShortcutItem)view.getParent().getParent();
			item.setAssigned(false);
			Toast.makeText(getContext(), "해제했습니다", Toast.LENGTH_SHORT).show();
		}
	};

	public void setAssigned(boolean value)
	{
		SharedPreferences pref = getContext().getSharedPreferences("shortcut", MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();

		if (value)  {
			mState = STATE_REG;
			mViewBtnReg.setText("해제");
			mViewBtnReg.setOnClickListener(onclickUnReg);
			//mViewBtnUnreg.setVisibility(VISIBLE);

			mViewName.setText(mNameSC);
			mViewNumber.setText(mNumberSC);

			editor.putString(Integer.toString(mIdxSC), mNumberSC);

		}
		else    {
			mState = STATE_UNREG;
			//mViewBtnReg.setVisibility(VISIBLE);
			mViewBtnReg.setText("등록");
			mViewBtnReg.setOnClickListener(onclickReg);
			//mViewBtnUnreg.setVisibility(INVISIBLE);

			mViewName.setText("");
			mViewNumber.setText("");

			editor.putString(Integer.toString(mIdxSC), "");
		}

		editor.commit();
	}

	public void initViews()
	{

		Log.i("MReader_ShortcutItem", "initViews() called");
		setOrientation(LinearLayout.HORIZONTAL);
		setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		//setGravity(Gravity.CENTER);

		mViewName = new TextView(getContext());
		mViewNumber = new TextView(getContext());
		mViewID = new TextView(getContext());
		mViewBtnReg = new TextView(getContext());
		//mViewBtnUnreg = new TextView(getContext());

		Log.i("MReader_ShortcutItem", "nNameSC="+mNameSC);
		Log.i("MReader_ShortcutItem", "mNumberSC="+mNumberSC);
		Log.i("MReader_ShortcutItem", "mIdxSC="+mIdxSC);

		mViewName.setText(mNameSC);
		mViewNumber.setText(mNumberSC);
		mViewID.setText(Integer.toString(mIdxSC));
		mViewBtnReg.setText("기본값");

		/*
		//LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(1, 90);
		param.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
		param.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());;
		param.weight = 1;

		Log.i("MReader_ShortcutItem", "@@@@ width="+param.width+", height="+param.height);
		param.gravity = Gravity.CENTER;
		*/

		mViewID.setGravity(Gravity.CENTER);
		mViewID.setWidth((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics()));
		mViewID.setBackgroundResource(R.color.colorAccent);
		addView(mViewID);

		LinearLayout layout1 = new LinearLayout(getContext());
		layout1.setOrientation(LinearLayout.VERTICAL);

		/*
		LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		param2.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
		param2.gravity = Gravity.CENTER;
		*/
		mViewName.setWidth((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 220, getResources().getDisplayMetrics()));
		mViewName.setBackgroundResource(R.color.colorPrimaryDark);
		mViewName.setTextSize((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
		layout1.addView(mViewName);

		/*
		LinearLayout.LayoutParams param3 = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		param3.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
		param3.gravity = Gravity.CENTER;
		*/
		mViewNumber.setBackgroundResource(R.color.colorPrimary);
		mViewNumber.setTextSize((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8, getResources().getDisplayMetrics()));
		layout1.addView(mViewNumber);

		addView(layout1);

		LinearLayout layout2 = new LinearLayout(getContext());
		layout2.setOrientation(LinearLayout.HORIZONTAL);
		layout2.setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		layout2.setGravity(Gravity.CENTER);

		// layout2 에 버튼 추가하고
		/*
		LinearLayout.LayoutParams param4 = new LinearLayout.LayoutParams(45, 45);
		param4.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
		param4.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());;
		param4.weight = 1;
		*/
//		setAssigned(true);
//		mViewBtnReg.setText("등록");
//		mViewBtnReg.setVisibility(INVISIBLE);
		layout2.addView(mViewBtnReg);
		
		//mViewBtnUnreg.setText("해제");
		//mViewBtnUnreg.setVisibility(INVISIBLE);
		//layout2.addView(mViewBtnUnreg);

		addView(layout2);

		Log.i("MReader_ShortcutItem", "initViews() done");

	}

	private void init(AttributeSet attrs, int defStyle) {
		Log.i("MReader_ShortcutItem", "init() called");

		/*
		// Load attributes
		final TypedArray a = getContext().obtainStyledAttributes(
				attrs, R.styleable.ShortcutItem, defStyle, 0);

		mExampleString = a.getString(
				R.styleable.ShortcutItem_exampleString);
		mExampleColor = a.getColor(
				R.styleable.ShortcutItem_exampleColor,
				mExampleColor);
		// Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
		// values that should fall on pixel boundaries.
		mExampleDimension = a.getDimension(
				R.styleable.ShortcutItem_exampleDimension,
				mExampleDimension);

		if (a.hasValue(R.styleable.ShortcutItem_exampleDrawable)) {
			mExampleDrawable = a.getDrawable(
					R.styleable.ShortcutItem_exampleDrawable);
			mExampleDrawable.setCallback(this);
		}

		a.recycle();

		// Set up a default TextPaint object
		mTextPaint = new TextPaint();
		mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setTextAlign(Paint.Align.LEFT);

		// Update TextPaint and text measurements from attributes
		invalidateTextPaintAndMeasurements();
*/


		/*
		String infService = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li;
		li = (LayoutInflater) getContext().getSystemService(infService);
		li.inflate(R.layout.activity_edit_shortcut, this, true);

		mViewNumber = (TextView)findViewById(R.id.txtNum);
		mViewNumber.setText("010-5478-2684");
		*/

		Log.i("MReader_ShortcutItem", "init() done");
	}

	private void invalidateTextPaintAndMeasurements() {
		mTextPaint.setTextSize(mExampleDimension);
		mTextPaint.setColor(mExampleColor);
		mTextWidth = mTextPaint.measureText(mExampleString);

		Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
		mTextHeight = fontMetrics.bottom;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// TODO: consider storing these as member variables to reduce
		// allocations per draw cycle.
		int paddingLeft = getPaddingLeft();
		int paddingTop = getPaddingTop();
		int paddingRight = getPaddingRight();
		int paddingBottom = getPaddingBottom();

		int contentWidth = getWidth() - paddingLeft - paddingRight;
		int contentHeight = getHeight() - paddingTop - paddingBottom;

		// Draw the text.
		canvas.drawText(mExampleString,
				paddingLeft + (contentWidth - mTextWidth) / 2,
				paddingTop + (contentHeight + mTextHeight) / 2,
				mTextPaint);

		// Draw the example drawable on top of the text.
		if (mExampleDrawable != null) {
			mExampleDrawable.setBounds(paddingLeft, paddingTop,
	paddingLeft + contentWidth, paddingTop + contentHeight);
	mExampleDrawable.draw(canvas);
}
}

/**
 * Gets the example string attribute value.
 *
 * @return The example string attribute value.
 */
	public String getExampleString() {
		return mExampleString;
	}

	/**
	 * Sets the view's example string attribute value. In the example view, this string
	 * is the text to draw.
	 *
	 * @param exampleString The example string attribute value to use.
	 */
	public void setExampleString(String exampleString) {
		mExampleString = exampleString;
		invalidateTextPaintAndMeasurements();
	}

	/**
	 * Gets the example color attribute value.
	 *
	 * @return The example color attribute value.
	 */
	public int getExampleColor() {
		return mExampleColor;
	}

	/**
	 * Sets the view's example color attribute value. In the example view, this color
	 * is the font color.
	 *
	 * @param exampleColor The example color attribute value to use.
	 */
	public void setExampleColor(int exampleColor) {
		mExampleColor = exampleColor;
		invalidateTextPaintAndMeasurements();
	}

	/**
	 * Gets the example dimension attribute value.
	 *
	 * @return The example dimension attribute value.
	 */
	public float getExampleDimension() {
		return mExampleDimension;
	}

	/**
	 * Sets the view's example dimension attribute value. In the example view, this dimension
	 * is the font size.
	 *
	 * @param exampleDimension The example dimension attribute value to use.
	 */
	public void setExampleDimension(float exampleDimension) {
		mExampleDimension = exampleDimension;
		invalidateTextPaintAndMeasurements();
	}

	/**
	 * Gets the example drawable attribute value.
	 *
	 * @return The example drawable attribute value.
	 */
	public Drawable getExampleDrawable() {
		return mExampleDrawable;
	}

	/**
	 * Sets the view's example drawable attribute value. In the example view, this drawable is
	 * drawn above the text.
	 *
	 * @param exampleDrawable The example drawable attribute value to use.
	 */
	public void setExampleDrawable(Drawable exampleDrawable) {
		mExampleDrawable = exampleDrawable;
	}
}
