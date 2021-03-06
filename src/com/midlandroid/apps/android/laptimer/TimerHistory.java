package com.midlandroid.apps.android.laptimer;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.midlandroid.apps.android.laptimer.util.SimpleFileAccess;
import com.midlandroid.apps.android.laptimer.util.TextUtil;
import com.midlandroid.apps.android.laptimer.util.TimerHistoryDbRecord;
import com.midlandroid.apps.android.laptimer.util.UIUtil;
import com.midlandroid.apps.android.laptimer.util.db.OpenDatabaseHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;


public class TimerHistory extends Activity {
	@SuppressWarnings("unused")
	private static final String LOG_TAG = TimerHistory.class.getSimpleName();
	
	private ListView historyList;
	private List<TimerHistoryDbRecord> timerHistory;
	private ArrayAdapter<String> adapter;
	private ArrayList<String> listItems;
	
	private NumberFormat numFormat;
	
	private OpenDatabaseHelper dbHelper;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.timer_history);

		// Create the number formatter that will be used later
		numFormat = NumberFormat.getInstance();
        numFormat.setMinimumIntegerDigits(2);
        numFormat.setMaximumIntegerDigits(2);
        numFormat.setParseIntegerOnly(true);
		
		// Create the adapter that will be used to populate the history list
		listItems = new ArrayList<String>();
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
		
		// timer history list
		historyList = (ListView) findViewById(R.id.history_list);
		historyList.setAdapter(adapter);
		
		historyList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				_showHistoryDialog(timerHistory.get(pos));
			}
		});
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// get a connection to the database
		dbHelper = new OpenDatabaseHelper(this);
		
		_refreshHistoryList();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
    	// Disconnect from the database
    	dbHelper.close();
    	dbHelper = null;
	}
	
	
    ////////////////////////////////////
    // Options Menu
    ////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	getMenuInflater().inflate(R.menu.history_menu, menu);
    	   	
		return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.mi_write_all_to_sdcard:
    		_writeAllHistoryToSdCard();
    		return true;
    	case R.id.mi_delete_all_saved_history:
    		_deleteAllSavedHistory();
    		return true;
    	}
    	
    	return false;
    }
	
	
    ////////////////////////////////////
    // Private Methods	
    ////////////////////////////////////
    /**
     * Export a select saved timer timer history to the SD card.
     */
    private void _writeHistoryItemToSdCard(final TimerHistoryDbRecord record) {
    	final String ExportDirectoryPath = Environment.getExternalStorageDirectory() + "/MidlandAndroid.LapTimer/";
    	// Make sure the external storage device is actually mounted before continuing.
    	String storageState = Environment.getExternalStorageState();
    	if (Environment.MEDIA_MOUNTED.equals(storageState)) {
    		// Create the directory the lap timer exports will be written to
    		// if it does not already exist.
    		File outDir = new File(ExportDirectoryPath);
    		if (!outDir.exists()) {
    			outDir.mkdir();
    		}
    		
	    	// Export the timer history to the SD card
			new SimpleFileAccess().showOutFileAlertPromptAndWriteTo(this,
					ExportDirectoryPath + 
					"laptimer_"+DateFormat.format("yyyyMMdd-kkmmss",record.getStartedAt()) + ".txt",
					record.getHistory());
    	}
    }
    
    
    /**
     * Export all of the saved timer histories to the SD card
     */
	private void _writeAllHistoryToSdCard() {
        // Local reference to the timer history list
        List<TimerHistoryDbRecord> tmpHistory = timerHistory;
        
        // Detect whether or not the application will be able to write to the storage device.
        String storageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(storageState)) {
			for (TimerHistoryDbRecord record : tmpHistory) {
				_writeHistoryItemToSdCard(record);
			}
        } else {
        	// Create the alert that will prompt the user
        	final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        	alert.setTitle("File Selection");
        	alert.setMessage("SD card is not mounted, unable to save timer history.");

        	// Create the click listeners for user response
        	alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    			}
    		});

        	// Display the alert
        	alert.show();
        }
	}
	
	
	/**
	 * Delete all saved histories 
	 */
	private void _deleteAllSavedHistory() {
		UIUtil.showAlertPrompt(TimerHistory.this,
				"Delete All Saved History Items",
				"Are you sure you want to delete all saved history items?",
				"Yes", "Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInf, int which) {
						if (DialogInterface.BUTTON_POSITIVE == which) {
							for (TimerHistoryDbRecord record : timerHistory) {
					    		dbHelper.deleteTimerHistoryById(record.getId());
							}
							_refreshHistoryList();
						} else if (DialogInterface.BUTTON_NEGATIVE == which) {
							// Cancel, nothing to do.
						}
					}
				});
	}
	

	/**
	 * Using the values returned by querying for
	 * saved timer histories, build the history list.
	 */
	private void _populateListWithTimerHistory() {      
        // Local reference to the timer history list
        List<TimerHistoryDbRecord> tmpHistory = timerHistory;
        
        // clear the existing contents of the list
        listItems.clear();
		
        // Get the values from each element in the results list
		for (TimerHistoryDbRecord result : tmpHistory) {
			String str = new String(
					TextUtil.formatDateToString(result.getStartedAt()) + "\n" +
					result.getDesc());
			listItems.add(str);
		}
		adapter.notifyDataSetChanged();
	}
	
	
	/**
	 * Displays a popup dialog displaying the selected timer history's history
	 * @param record
	 */
	private void _showHistoryDialog(final TimerHistoryDbRecord record) {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.history_dialog_layout);
		dialog.setTitle(new String(
				"Duration: " + TextUtil.formatDateToString(record.getDuration(), numFormat)));
		
		final EditText desc = (EditText) dialog.findViewById(R.id.history_item_desc_txt);
		desc.setText(record.getDesc());
		
		TextView text = (TextView) dialog.findViewById(R.id.history_dialog_text);
		text.setText(record.getHistory());

		Button deleteBtn = (Button) dialog.findViewById(R.id.history_item_delete_btn);
		deleteBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				UIUtil.showAlertPrompt(TimerHistory.this,
						"Delete Saved History Item",
						"Are you sure you want to delete this saved history item?",
						"Yes", "Cancel",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInf, int which) {
								if (DialogInterface.BUTTON_POSITIVE == which) {
						    		dbHelper.deleteTimerHistoryById(record.getId());
						    		_refreshHistoryList();
						    		
						    		dialog.dismiss();
								} else if (DialogInterface.BUTTON_NEGATIVE == which) {
									// Cancel, nothing to do.
								}
							}
						});
			}
		});
		Button exportBtn = (Button) dialog.findViewById(R.id.history_item_export_btn);
		exportBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				_writeHistoryItemToSdCard(record);
			}
		});
		Button closeBtn  = (Button) dialog.findViewById(R.id.history_item_close_btn);
		closeBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				record.setDesc(desc.getText().toString());
				dbHelper.updateTimerHistory(record);
				
	    		_refreshHistoryList();
	    		dialog.dismiss();
			}
		});
		
		dialog.show();
	}

	
	/**
	 * Clears the current timer history list and refreshes it with the
	 * data from the database
	 */
	private void _refreshHistoryList() {
		timerHistory = dbHelper.selectAllTimerHistories();
		_populateListWithTimerHistory();
	}
}
