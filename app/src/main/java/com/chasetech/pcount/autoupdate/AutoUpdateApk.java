//
//	Copyright (c) 2012 lenik terenin
//
//	Licensed under the Apache License, Version 2.0 (the "License");
//	you may not use this file except in compliance with the License.
//	You may obtain a copy of the License at
//
//		http://www.apache.org/licenses/LICENSE-2.0
//
//	Unless required by applicable law or agreed to in writing, software
//	distributed under the License is distributed on an "AS IS" BASIS,
//	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//	See the License for the specific language governing permissions and
//	limitations under the License.

package com.chasetech.pcount.autoupdate;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.chasetech.pcount.library.MainLibrary;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Observable;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class AutoUpdateApk extends Observable {

	// this class is supposed to be instantiated in any of your activities or,
	// better yet, in Application subclass. Something along the lines of:
	//
	//	private AutoUpdateApk aua;	<-- you need to add this line of code
	//
	//	public void onCreate(Bundle savedInstanceState) {
	//		super.onCreate(savedInstanceState);
	//		setContentView(R.layout.main);
	//
	//		aua = new AutoUpdateApk(getApplicationContext());	<-- and add this line too
	//

	AlertDialog mAlertDialog;
	ProgressDialog progressDialog;

	public AutoUpdateApk(Context ctx) {
		setupVariables(ctx);
		mAlertDialog = new AlertDialog.Builder(ctx).create();
	}

	// set icon for notification popup (ic_default = application icon)
	//
	public static void setIcon( int icon ) {
		appIcon = icon;
	}

	// set name to display in notification popup (ic_default = application label)
	//
	public static void setName( String name ) {
		appName = name;
	}

	// set Notification flags (ic_default = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_NO_CLEAR)
	//
	public static void setNotificationFlags( int flags ) {
		NOTIFICATION_FLAGS = flags;
	}

	// set update interval (in milliseconds)
	//
	// there are nice constants in this file: MINUTES, HOURS, DAYS
	// you may use them to specify update interval like: 5 * DAYS
	//
	// please, don't specify update interval below 1 hour, this might
	// be considered annoying behaviour and result in service suspension
	//
	public void setUpdateInterval(long interval) {
		if( interval > 60 * MINUTES ) {
			UPDATE_INTERVAL = interval;
		} else {
			Log_e(TAG, "update interval is too short (less than 1 hour)");
		}
	}

	// software updates will use WiFi/Ethernet only (ic_default mode)
	//
	public static void disableMobileUpdates() {
		mobile_updates = false;
	}

	// software updates will use any internet connection, including mobile
	// might be a good idea to have 'unlimited' plan on your 3.75G connection
	//
	public static void enableMobileUpdates() {
		mobile_updates = true;
	}

	// call this if you want to perform update on demand
	// (checking for updates more often than once an hour is not recommended
	// and polling server every few minutes might be a reason for suspension)
	//
	public void checkUpdatesManually() {
		checkUpdates(true);		// force update check
	}

	public static final String AUTOUPDATE_CHECKING = "autoupdate_checking";
	public static final String AUTOUPDATE_NO_UPDATE = "autoupdate_no_update";
	public static final String AUTOUPDATE_GOT_UPDATE = "autoupdate_got_update";
	public static final String AUTOUPDATE_HAVE_UPDATE = "autoupdate_have_update";

	public void clearSchedule() {
		schedule.clear();
	}

	public void addSchedule(int start, int end) {
		schedule.add(new ScheduleEntry(start,end));
	}
//
// ---------- everything below this line is private and does not belong to the public API ----------
//
	protected final static String TAG = "AutoUpdateApk";



	public final static String ANDROID_PACKAGE = "application/vnd.android.package-archive";

/*	public final static String API_URL_APK = "http://ulp-projectsos.com/api/check";
	public final static String API_URL_APK_BETA = "http://ulp-projectsos.com/api/betacheck";
	public final static String API_URL_APK_CHECK = "http://ulp-projectsos.com/api/verify";
	public final static String API_URL_APK_BETACHECK = "http://ulp-projectsos.com/api/betaverify";*/

	protected static Context context = null;
	protected static SharedPreferences preferences;
	private final static String LAST_UPDATE_KEY = "last_update";
	private static long last_update = 0;

	private static int appIcon = android.R.drawable.ic_popup_reminder;
	private static int versionCode = 0;		// as low as it gets
	private static String packageName;
	private static String appName;
	private static int device_id;

	public static final long MINUTES = 60 * 1000;
	public static final long HOURS = 60 * MINUTES;
	public static final long DAYS = 24 * HOURS;

	// 3-4 hours in dev.mode, 1-2 days for stable releases
	//private static long UPDATE_INTERVAL = 3 * HOURS;	// how often to check
	private static long UPDATE_INTERVAL = 50 * 1000;	// how often to check

	private static boolean mobile_updates = false;		// download updates over wifi only

	private final static Handler updateHandler = new Handler();
	protected final static String UPDATE_FILE = "update_file";
	protected final static String SILENT_FAILED = "silent_failed";
	private final static String MD5_TIME = "md5_time";
	public final static String MD5_KEY = "md5";

	private static int NOTIFICATION_ID = 0xDEADBEEF;
	private static int NOTIFICATION_FLAGS = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_NO_CLEAR;
	//private static long WAKEUP_INTERVAL = 15 * MINUTES;
	private static long WAKEUP_INTERVAL = 50 * 1000;
    NotificationManager notificationManager;

	private class ScheduleEntry {
		public int start;
		public int end;

		public ScheduleEntry(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}

	private static ArrayList<ScheduleEntry> schedule = new ArrayList<ScheduleEntry>();

	private Runnable periodicUpdate = new Runnable() {
		@Override
		public void run() {
			checkUpdates(false);
			updateHandler.removeCallbacks(periodicUpdate);	// remove whatever others may have posted
			updateHandler.postDelayed(this, WAKEUP_INTERVAL);
		}
	};

	private BroadcastReceiver connectivity_receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			NetworkInfo currentNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			// do application-specific task(s) based on the current network state, such 
			// as enabling queuing of HTTP requests when currentNetworkInfo is connected etc.
			boolean not_mobile = !currentNetworkInfo.getTypeName().equalsIgnoreCase("MOBILE");
			if(currentNetworkInfo.isConnectedOrConnecting() && (mobile_updates || not_mobile)) {
				checkUpdates(false);
				updateHandler.postDelayed(periodicUpdate, UPDATE_INTERVAL);
			} else {
				updateHandler.removeCallbacks(periodicUpdate);	// no network anyway
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
			}
		}
	};

	private void setupVariables(Context ctx) {
		context = ctx;

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		packageName = context.getPackageName();
		preferences = context.getSharedPreferences( packageName + "_" + TAG, Context.MODE_PRIVATE);
		device_id = crc32(Secure.getString( context.getContentResolver(), Secure.ANDROID_ID));
		last_update = preferences.getLong("last_update", 0);
		NOTIFICATION_ID += crc32(packageName);
//		schedule.add(new ScheduleEntry(0,24));

		ApplicationInfo appinfo = context.getApplicationInfo();
		if( appinfo.icon != 0 ) {
			appIcon = appinfo.icon;
		} else {
			Log_w(TAG, "unable to find application icon");
		}
		if( appinfo.labelRes != 0 ) {
			appName = context.getString(appinfo.labelRes);
		} else {
			Log_w(TAG, "unable to find application label");
		}

		if( new File(appinfo.sourceDir).lastModified() > preferences.getLong(MD5_TIME, 0) ) {
			preferences.edit().putString( MD5_KEY, MD5Hex(appinfo.sourceDir)).apply();
			preferences.edit().putLong( MD5_TIME, System.currentTimeMillis()).apply();

			String update_file = preferences.getString(UPDATE_FILE, "");
			if( update_file.length() > 0 ) {
				if( new File( context.getFilesDir().getAbsolutePath() + "/" + update_file ).delete() ) {
					preferences.edit().remove(UPDATE_FILE).remove(SILENT_FAILED).apply();
				}
			}
		}
		//raise_notification();

        enableMobileUpdates();

		// checking updates
		if( haveInternetPermissions() ) {
			context.registerReceiver( connectivity_receiver,
					new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		}
	}

	private boolean checkSchedule() {
		if( schedule.size() == 0 ) return true;	// empty schedule always fits

		int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		for( ScheduleEntry e : schedule ) {
			if( now >= e.start && now < e.end ) return true;
		}
		return false;
	}

	private class checkUpdateTask extends AsyncTask<Void, String, String[]> {
		private DefaultHttpClient httpclient = new DefaultHttpClient();
        String statusMsg = "";
        String errmsg = "";
        byte dataArray[] = new byte[1024];

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            progressDialog.setMessage(values[0]);
        }

        protected String[] doInBackground(Void... v) {
			long start = System.currentTimeMillis();

			String urlCheck = AutoUpdate.API_URL_APK;
			if(MainLibrary.BETA) {
				urlCheck = AutoUpdate.API_URL_APK_BETA;
			}

			HttpPost httpPost = new HttpPost(urlCheck);

			HttpParams httpParameters = new BasicHttpParams();
			// set the timeout in milliseconds until a connection is established
			// the ic_default value is zero, that means the timeout is not used
			int timeoutConnection = 10000;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			// set the ic_default socket timeout (SO_TIMEOUT) in milliseconds
			// which is the timeout for waiting for data
			int timeoutSocket = 8000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

			httpclient.setParams(httpParameters);

			try {
                String strParams = "pkgname=" + packageName + "&version=" + versionCode +
                        "&md5=" + preferences.getString( MD5_KEY, "0") +
                        "&id=" + String.format("%08x", device_id);
				StringEntity params = new StringEntity(strParams);
				httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
				httpPost.setEntity(params);
                HttpResponse httpResponse = httpclient.execute( httpPost );
				String response = EntityUtils.toString( httpResponse.getEntity(), "UTF-8" );

                statusMsg = "got a reply from update server";
				Log_e(TAG, statusMsg);
                publishProgress(statusMsg);
                String[] result = response.split("\n");
				if( result.length > 1 && result[0].equalsIgnoreCase("have update") ) {
					HttpGet get = new HttpGet(result[1]);
                    statusMsg = "Downloading new apk.";
                    publishProgress(statusMsg);
					HttpEntity entity = httpclient.execute( get ).getEntity();
					Log_e(TAG, statusMsg);

					if( entity.getContentType().getValue().equalsIgnoreCase(ANDROID_PACKAGE)) {
						String fname = result[1].substring(result[1].lastIndexOf('/')+1);

						File fApk = new File("file://" + context.getFilesDir().getAbsolutePath() + "/" + fname);
						if(fApk.exists()) return result;

						FileOutputStream fos = context.openFileOutput(fname, Context.MODE_WORLD_READABLE);
						entity.writeTo(fos);
						fos.close();
						result[1] = fname;
					}
					setChanged();
					notifyObservers(AUTOUPDATE_GOT_UPDATE);
				} else {
					setChanged();
					notifyObservers(AUTOUPDATE_NO_UPDATE);
                    statusMsg = "no update available";
					Log_e(TAG, statusMsg);
                    publishProgress(statusMsg);
				}
				return result;
            } catch (ParseException e) {
                final String msg = e.getMessage();
                Log_e(TAG, msg != null ? msg : "Parse Exception");
            } catch (ClientProtocolException e) {
                final String msg = e.getMessage();
                Log_e(TAG, msg != null ? msg : "Client Protocol Exception");
            } catch (IOException e) {
                errmsg = e.getMessage() != null ? e.getMessage() : "Slow or unstable internet connection";
                Log_e(TAG, errmsg);
            }
            finally {
				httpclient.getConnectionManager().shutdown();
				long elapsed = System.currentTimeMillis() - start;
				Log_e(TAG, "update check finished in " + elapsed + "ms");
			}
			return null;
		}

		protected void onPreExecute()
		{
            progressDialog = ProgressDialog.show(context, "", "Checking internet connection.");
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Checking new updates.");
			// show progress bar or something
			Log_v(TAG, "checking if there's update on the server");
		}

		protected void onPostExecute(String[] result) {
			// kill progress bar here
            progressDialog.dismiss();
            if(!errmsg.trim().equals("")) {
                Toast.makeText(context, errmsg, Toast.LENGTH_LONG).show();
                return;
            }

			if( result != null ) {
				if( result[0].equalsIgnoreCase("have update") ) {
					preferences.edit().putString(UPDATE_FILE, result[1]).apply();

					String update_file_path = context.getFilesDir().getAbsolutePath() + "/" + result[1];
					preferences.edit().putString( MD5_KEY, MD5Hex(update_file_path)).apply();
					preferences.edit().putLong( MD5_TIME, System.currentTimeMillis()).apply();
				}
				raise_notification();
			} else {
                Toast.makeText(context, "No new updates.", Toast.LENGTH_SHORT).show();
				Log_v(TAG, "no reply from update server");
			}
		}
	}


	private void checkUpdates(boolean forced) {
		long now = System.currentTimeMillis();
    		if( forced || (last_update + UPDATE_INTERVAL) < now && checkSchedule() ) {
			new checkUpdateTask().execute();
			last_update = System.currentTimeMillis();
			preferences.edit().putLong( LAST_UPDATE_KEY, last_update).apply();

			this.setChanged();
			this.notifyObservers(AUTOUPDATE_CHECKING);
		}
	}

	protected void raise_notification() {

		String update_file = preferences.getString(UPDATE_FILE, "");
        notificationManager.cancelAll();
        if( update_file.length() > 0 ) {
            setChanged();

            // raise notification
//            Notification notification = new Notification(
//                    appIcon, appName + " update", System.currentTimeMillis());
//            notification.flags |= NOTIFICATION_FLAGS;

            CharSequence contentTitle = appName + " update available";
            CharSequence contentText = "Click this to install..";

            final Intent notificationIntent = new Intent(Intent.ACTION_VIEW );
			String apkFileUri = "file://" + context.getFilesDir().getAbsolutePath() + "/" + update_file;
            notificationIntent.setDataAndType(
                    Uri.parse(apkFileUri),
                    ANDROID_PACKAGE);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

			ShowNotif(contentTitle, contentText, contentIntent, notificationManager);

			try {
				mAlertDialog.setCancelable(false);
				mAlertDialog.setTitle("Existing update");
				mAlertDialog.setMessage("There's an " + appName + " update available. Tap UPDATE to install.");
				mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "UPDATE", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mAlertDialog.dismiss();
						notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(notificationIntent);
						notifyObservers(AUTOUPDATE_HAVE_UPDATE);
					}
				});
				mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mAlertDialog.dismiss();
						((Activity) context).finish();
					}
				});
				mAlertDialog.show();
			}
			catch (Exception e) {
				String err = e.getMessage() != null ? e.getMessage() : "Can't display alertdialog.";
				Log.e(TAG, err);
			}

        }
        else {
            notificationManager.cancel( NOTIFICATION_ID );
        }



//		if( update_file.length() > 0 ) {
//			setChanged();
//			notifyObservers(AUTOUPDATE_HAVE_UPDATE);
//
//			// raise notification
///*			Notification notification = new Notification(
//					appIcon, appName + " update", System.currentTimeMillis());*/
//            Notification.Builder notifBuilder = new Notification.Builder(context);
//			//notification.flags |= NOTIFICATION_FLAGS;
//
//			CharSequence contentTitle = appName + " update available";
//			CharSequence contentText = "Select to install";
//			Intent notificationIntent = new Intent(Intent.ACTION_VIEW );
//			Uri uriFile = Uri.parse("file://" + context.getFilesDir().getAbsolutePath() + "/" + update_file);
//			notificationIntent.setDataAndType(uriFile, ANDROID_PACKAGE);
//			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
//
//            notifBuilder.setContentTitle(contentTitle);
//            notifBuilder.setContentText(contentText);
//            notifBuilder.setContentIntent(contentIntent);
//            notifBuilder.setSubText("Update available.");
//            notifBuilder.build();
//
//            Notification notification = notifBuilder.build();
//            notification.flags |= NOTIFICATION_FLAGS;
//
//            notificationManager.notify(NOTIFICATION_ID, notification);
//
//			//notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
//			//notificationManager.notify( NOTIFICATION_ID, notification);
//		} else {
//			notificationManager.cancel( NOTIFICATION_ID );
//		}
	}

	private void ShowNotif(CharSequence contentTitle, CharSequence contentText, PendingIntent contentIntent, NotificationManager nm) {
		Notification.Builder builder = new Notification.Builder(context);

		builder.setAutoCancel(false);
		builder.setTicker("An update is available.");
		builder.setContentTitle(contentTitle);
		builder.setContentText(contentText);
		builder.setSmallIcon(appIcon);
		builder.setContentIntent(contentIntent);
		builder.setOngoing(true);
		builder.setSubText(appName + " update");   //API level 16
		builder.setNumber(100);
        builder.setAutoCancel(true);
        builder.setProgress(0,0, true);
		nm.notify(NOTIFICATION_ID, builder.build());
	}

	private String MD5Hex(String filename)
	{
		final int BUFFER_SIZE = 8192;
		byte[] buf = new byte[BUFFER_SIZE];
		int length;
		try {
			FileInputStream fis = new FileInputStream( filename );
			BufferedInputStream bis = new BufferedInputStream(fis);
			MessageDigest md = MessageDigest.getInstance("MD5");
			while( (length = bis.read(buf)) != -1 ) {
				md.update(buf, 0, length);
			}

			byte[] array = md.digest();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
			}
			Log_v(TAG, "md5sum: " + sb.toString());
			return sb.toString();
		} catch (Exception e) {
//			e.printStackTrace();
			Log_e(TAG, e.getMessage());
		}
		return "md5bad";
	}

	private boolean haveInternetPermissions() {
		Set<String> required_perms = new HashSet<String>();
		required_perms.add("android.permission.INTERNET");
		required_perms.add("android.permission.ACCESS_WIFI_STATE");
		required_perms.add("android.permission.ACCESS_NETWORK_STATE");

		PackageManager pm = context.getPackageManager();
		String packageName = context.getPackageName();
		int flags = PackageManager.GET_PERMISSIONS;
		PackageInfo packageInfo = null;

		try {
			packageInfo = pm.getPackageInfo(packageName, flags);
			versionCode = packageInfo.versionCode;
		} catch (NameNotFoundException e) {
//			e.printStackTrace();
			Log_e(TAG, e.getMessage());
		}
		if( packageInfo.requestedPermissions != null ) {
			for( String p : packageInfo.requestedPermissions ) {
				//Log_v(TAG, "permission: " + p.toString());
				required_perms.remove(p);
			}
			if( required_perms.size() == 0 ) {
				return true;	// permissions are in order
			}
			// something is missing
			for( String p : required_perms ) {
				Log_e(TAG, "required permission missing: " + p);
			}
		}
		Log_e(TAG, "INTERNET/WIFI access required, but no permissions are found in Manifest.xml");
		return false;
	}

	public static int crc32(String str) {
		byte bytes[] = str.getBytes();
		Checksum checksum = new CRC32();
		checksum.update(bytes,0,bytes.length);
		return (int) checksum.getValue();
	}

	// logging facilities to enable easy overriding. thanks, Dan!
	//
	protected void Log_v(String tag, String message) {Log_v(tag, message, null);}
	protected void Log_v(String tag, String message, Throwable e) {log("v", tag, message, e);}
	protected void Log_d(String tag, String message) {Log_d(tag, message, null);}
	protected void Log_d(String tag, String message, Throwable e) {log("d", tag, message, e);}
	protected void Log_i(String tag, String message) {Log_d(tag, message, null);}
	protected void Log_i(String tag, String message, Throwable e) {log("i", tag, message, e);}
	protected void Log_w(String tag, String message) {Log_w(tag, message, null);}
	protected void Log_w(String tag, String message, Throwable e) {log("w", tag, message, e);}
	protected void Log_e(String tag, String message) {Log_e(tag, message, null);}
	protected void Log_e(String tag, String message, Throwable e) {log("e", tag, message, e);}

	protected void log(String level, String tag, String message, Throwable e) {
		if(level.equalsIgnoreCase("v")) {
			if(e == null) android.util.Log.v(tag, message);
			else android.util.Log.v(tag, message, e);
		} else if(level.equalsIgnoreCase("d")) {
			if(e == null) android.util.Log.d(tag, message);
			else android.util.Log.d(tag, message, e);
		} else if(level.equalsIgnoreCase("i")) {
			if(e == null) android.util.Log.i(tag, message);
			else android.util.Log.i(tag, message, e);
		} else if(level.equalsIgnoreCase("w")) {
			if(e == null) android.util.Log.w(tag, message);
			else android.util.Log.w(tag, message, e);
		} else {
			if(e == null) android.util.Log.e(tag, message);
			else android.util.Log.e(tag, message, e);
		}
	}

}
