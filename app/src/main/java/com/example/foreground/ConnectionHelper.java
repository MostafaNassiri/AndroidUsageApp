package com.example.foreground;

import android.annotation.SuppressLint;
import android.os.StrictMode;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Locale;

public class ConnectionHelper {
    Connection connection;
    String uname, pass, ip, port, database;
    @SuppressLint("NewApi")
    public Connection connectionClass(){
        ip = "185.165.116.47"; // آدرس سرور دیتابیس
        port = "1439"; // پورت سرور دیتابیس
        database ="muntodac_androidReports"; // نام دیتابیس
        uname = "android_reports"; // یوزرنیم
        pass="AndroidReports@2022"; // پسورد
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Connection connection = null;
        String connectionUrl = null;
        try{
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            connectionUrl = "jdbc:jtds:sqlserver://"
                    + ip + ":" + port + ";"
                    + "databasename=" + database + ";"
                    + "user=" + uname + ";"
                    + "password=" + pass + ";";
            connection = DriverManager.getConnection(connectionUrl);
        }
        catch (Exception ex){
            Log.e("Error: ", ex.getMessage());
        }
        return connection;
    }
}