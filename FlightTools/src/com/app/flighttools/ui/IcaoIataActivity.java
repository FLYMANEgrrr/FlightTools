package com.app.flighttools.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.app.flighttools.database.IcaoIataDbHelper;
import com.app.flighttools.util.FileUtils;
import com.app.flighttools.view.R;

public class IcaoIataActivity extends Activity {
	private String dirPath = "Flight Tools/";
	private String proffesional = "Airport Code.xls";
	private EditText edit_in;
	private Button button_search;
	private TextView tv_title;
	private TextView tv_warning1;
	private TextView tv_warning2;
	private TextView tv_english;
	private TextView tv_three_four;
	private TextView tv_airport;
	private TextView tv_city;
	private ListView list_content;
	private ListView list_history;
	private ImageView image_back;
	private RelativeLayout layout_title;
	private ScrollView layout_content;
	private List<HashMap<String, String>> listContent;
	private List<HashMap<String, String>> listHistory;
	private SimpleAdapter spContent;
	private SimpleAdapter spHitory;
	private ProgressDialog searchDialog;
	private IcaoIataDbHelper icaoiataHelper;
	private SQLiteDatabase dbRead;
	private SQLiteDatabase dbWrite;
	public String in;
	public ArrayList<Cell[]> cells;
	private final static String TABLE_NAME = "IcaoIataHistory";
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_icao_iata);
		initResource();
	}

	public void initResource(){
		edit_in = (EditText) findViewById(R.id.edit_in);
		button_search = (Button) findViewById(R.id.button_search);
		tv_warning1 = (TextView) findViewById(R.id.tv_warning1);
		tv_warning2 = (TextView) findViewById(R.id.tv_warning2);
		tv_english = (TextView) findViewById(R.id.tv_english);
		tv_three_four = (TextView) findViewById(R.id.tv_three_four);
		tv_airport = (TextView) findViewById(R.id.tv_airport);
		tv_city = (TextView) findViewById(R.id.tv_city);
		tv_title = (TextView)findViewById(R.id.tv_title1); 
		TextPaint tp = tv_title.getPaint(); 
		tp.setFakeBoldText(true);
		list_content = (ListView) findViewById(R.id.list_content);
		list_history = (ListView) findViewById(R.id.list_history);
		image_back = (ImageView) findViewById(R.id.image_back);
		layout_title = (RelativeLayout) findViewById(R.id.layout_title);
		layout_content = (ScrollView) findViewById(R.id.layout_content);
		searchDialog = new ProgressDialog(IcaoIataActivity.this);
		searchDialog.setMessage("正在查询中，请稍后");
		listHistory = new ArrayList<HashMap<String,String>>();
		spHitory = new SimpleAdapter(this, listHistory, R.layout.item_history, new String[]{"Word", "Explain"}, new int[]{R.id.tv_history_word, R.id.tv_history_explain});
		list_history.setAdapter(spHitory);
		listContent = new ArrayList<HashMap<String,String>>();
		spContent = new SimpleAdapter(this, listContent, R.layout.item_icao_iata, new String[]{"Three", "Four", "Airport", "English"}, new int[]{R.id.tv_three, R.id.tv_four, R.id.tv_airport, R.id.tv_english});
		list_content.setAdapter(spContent);
		icaoiataHelper = new IcaoIataDbHelper(IcaoIataActivity.this, TABLE_NAME, null, 1);
		dbRead = icaoiataHelper.getReadableDatabase();
		dbWrite = icaoiataHelper.getWritableDatabase();
		icaoiataHelper.onCreate(dbRead);		
		Cursor cursor = dbRead.query(TABLE_NAME, new String[]{"TimeStamp","Word","Explain"}, null, null, null, null, "TimeStamp DESC");  
		int i = 0;
		while(cursor.moveToNext())
		{
			HashMap<String, String> list = new HashMap<String, String>();
			String word = cursor.getString(cursor.getColumnIndex("Word"));
			String explain = cursor.getString(cursor.getColumnIndex("Explain"));
			list.put("Word", word);
			list.put("Explain", explain);
			listHistory.add(list);
			i++;
			if(i > 20)
				break;
		}
		if(cursor.getCount() > 200)
		{
			cursor.moveToPosition(100);
			while(cursor.moveToNext())
			{
				dbWrite.delete(TABLE_NAME, "TimeStamp=" + cursor.getLong(cursor.getColumnIndex("TimeStamp")), null);
			}
		}
		spHitory.notifyDataSetChanged();
		cells = new ArrayList<Cell[]>();
		//搜索
		button_search.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(layout_title.getWindowToken(), 0);
				in = edit_in.getText().toString();
				if(in == null || in.equals("") || in.equals("机场") || in.equals("机") || in.equals("场"))
					return;
				searchDialog.show();
				searchAsyncTask searchAsyncTask = new searchAsyncTask();
				searchAsyncTask.execute();
			}
		});
		//返回上级页面
		image_back.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
		//点击历史记录重新搜索
		list_history.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				in = listHistory.get(position).get("Word");
				searchDialog.show();
				searchAsyncTask searchAsyncTask = new searchAsyncTask();
				searchAsyncTask.execute();
			}
		});
		//显示模糊查询的结果之一
		list_content.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				Cell[] cell = cells.get(position);
				tv_city.setText("所在城市: " + cell[0].getContents());
				tv_three_four.setText("三字码  " + cell[1].getContents() + "  四字码  " + cell[2].getContents());
				tv_airport.setText(cell[3].getContents());
				tv_english.setText(cell[4].getContents());
				list_content.setVisibility(View.GONE);
				layout_content.setVisibility(View.VISIBLE);
				tv_city.setVisibility(View.VISIBLE);
				tv_three_four.setVisibility(View.VISIBLE);
				tv_airport.setVisibility(View.VISIBLE);
				tv_english.setVisibility(View.VISIBLE);
			}		
		});
	}
	
	class searchAsyncTask extends AsyncTask<Integer, Integer, ArrayList<Cell[]>>
	{

		@Override
		protected ArrayList<Cell[]> doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			try {
				cells.clear();
				listContent.clear();
				Workbook book = Workbook.getWorkbook(new File(FileUtils.getSDPATH() + dirPath + proffesional));
				Sheet sheet=book.getSheet(0);
				int i;
				for(i=1;i<sheet.getRows();i++)
				{
					if(sheet.getCell(1, i).getContents().equalsIgnoreCase(in))
					{
						cells.add(sheet.getRow(i));
					}
					if(sheet.getCell(2, i).getContents().equalsIgnoreCase(in))
					{
						cells.add(sheet.getRow(i));
					}
					if(sheet.getCell(4, i).getContents().equalsIgnoreCase(in))
					{
						cells.add(sheet.getRow(i));
					}
					if(sheet.getCell(3, i).getContents().contains(in))
					{
						cells.add(sheet.getRow(i));
					}
				}
				return cells;
			} catch (BiffException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return cells;
		}
		
		@Override
		protected void onPostExecute(ArrayList<Cell[]> cells) {
            //判断是否已有该记录
			searchDialog.dismiss();
			for(int i=0;i<listHistory.size();i++)
			{
				if(listHistory.get(i).get("Word").equals(in))
				{
					listHistory.remove(i);
		            String[] args = {in};
					dbWrite.delete(TABLE_NAME, "Word=?", args);
				}
			}
			if(cells.size() == 0)
			{				
				tv_warning1.setVisibility(View.VISIBLE);
				tv_warning2.setVisibility(View.VISIBLE);
				list_content.setVisibility(View.GONE);
				tv_city.setVisibility(View.GONE);
				tv_three_four.setVisibility(View.GONE);
				tv_airport.setVisibility(View.GONE);
				tv_english.setVisibility(View.GONE);
				HashMap<String, String> list = new HashMap<String, String>();
				ContentValues values = new ContentValues(); 
	            values.put("TimeStamp", System.currentTimeMillis());
	            values.put("Word", in);
	            values.put("Explain", "无");
				dbWrite.insert(TABLE_NAME, null, values);
				list.put("Word", in);
				list.put("Explain", "无");
				if(listHistory.size() == 20)				
					listHistory.remove(listHistory.size()-1);
				listHistory.add(0, list);
				spHitory.notifyDataSetChanged();
				return;
			}
			else if(cells.size() == 1)
			{
				Cell[] cell = cells.get(0);
				ContentValues values = new ContentValues();  
				HashMap<String, String> list = new HashMap<String, String>();
	            values.put("TimeStamp", System.currentTimeMillis());
	            values.put("Word", in);
				list.put("Word", in);
	            //判断是否有汉字
	            int bytesLength = in.getBytes().length;  
	            int sLength = in.length();   
	            if(bytesLength - sLength > 0)
	            {  
	            	values.put("Explain", cell[4].getContents());
					list.put("Explain", cell[4].getContents());
	            } 
	            else
	            {
		            values.put("Explain", cell[3].getContents());
					list.put("Explain", cell[3].getContents());
	            }
				dbWrite.insert(TABLE_NAME, null, values);
				if(listHistory.size() == 20)				
					listHistory.remove(listHistory.size()-1);
				listHistory.add(0, list);
				spHitory.notifyDataSetChanged();
				tv_city.setText("所在城市: " + cell[0].getContents());
				tv_three_four.setText("三字码  " + cell[1].getContents() + "  四字码  " + cell[2].getContents());
				tv_airport.setText(cell[3].getContents());
				tv_english.setText(cell[4].getContents());
				tv_warning1.setVisibility(View.GONE);
				tv_warning2.setVisibility(View.GONE);
				list_content.setVisibility(View.GONE);
				layout_content.setVisibility(View.VISIBLE);
				tv_city.setVisibility(View.VISIBLE);
				tv_three_four.setVisibility(View.VISIBLE);
				tv_airport.setVisibility(View.VISIBLE);
				tv_english.setVisibility(View.VISIBLE);
			}
			else
			{
				StringBuilder history = new StringBuilder();
				for(int i=0;i<cells.size();i++)
				{
					Cell[] cell = cells.get(i);
					HashMap<String, String> list = new HashMap<String, String>();
					list.put("Three", cell[1].getContents());
					list.put("Four", cell[2].getContents());
					list.put("Airport", cell[3].getContents());
					list.put("English", cell[4].getContents());
					listContent.add(list);
		            history.append(cell[3].getContents() + ";");
				}
				ContentValues values = new ContentValues();  
				HashMap<String, String> list = new HashMap<String, String>();
	            values.put("TimeStamp", System.currentTimeMillis());
	            values.put("Word", in);
		        values.put("Explain", history.toString());
				dbWrite.insert(TABLE_NAME, null, values);
				list.put("Word", in);
				list.put("Explain", history.toString());
				if(listHistory.size() == 20)				
					listHistory.remove(listHistory.size()-1);
				listHistory.add(0, list);
				spHitory.notifyDataSetChanged();
				spContent.notifyDataSetChanged();
				tv_warning1.setVisibility(View.GONE);
				tv_warning2.setVisibility(View.GONE);
				layout_content.setVisibility(View.GONE);
				list_content.setVisibility(View.VISIBLE);
			}
		}
	}
}
