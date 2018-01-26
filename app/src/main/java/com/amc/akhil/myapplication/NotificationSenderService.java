package com.amc.akhil.myapplication;

import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.StrictMode;

import com.amc.akhil.myapplication.db.GPSData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by srinivas on 03-12-2017.
 */

public class NotificationSenderService {

    public static final float RADIUS_IN_METERS = 500;

    public void sendNotification(Map<String, GPSData> gpsDataMap, String assistance){
        for(String key: gpsDataMap.keySet()){
            if(isLocationProximity(gpsDataMap.get(key)) && !isSelf(gpsDataMap.get(key)) && !isBVI(gpsDataMap.get(key))){
                if(assistance.equalsIgnoreCase("dropoff") && gpsDataMap.get(key).getSpeed() > 10){
                    sendNotification(gpsDataMap.get(key));
                }
                else if(assistance.equalsIgnoreCase("assistance")){
                    sendNotification(gpsDataMap.get(key));
                }
            }
        }
    }
    public void sendNotification(GPSData gpsData)
    {
        final String user_id = gpsData.getUserId();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                int SDK_INT = android.os.Build.VERSION.SDK_INT;
                if (SDK_INT > 8) {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                            .permitAll().build();
                    StrictMode.setThreadPolicy(policy);

                    //This is a Simple Logic to Send Notification different Device Programmatically....
                    try {
                        String jsonResponse;

                        URL url = new URL("https://onesignal.com/api/v1/notifications");
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setUseCaches(false);
                        con.setDoOutput(true);
                        con.setDoInput(true);

                        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        con.setRequestProperty("Authorization", "Basic NmJlZTgyODktMTllMy00MTM0LTllMmMtNGNmNWFkNDg4ZGRk");
                        con.setRequestMethod("POST");

                        String strJsonBody = "{"
                                + "\"app_id\": \"40fdc78f-635d-4a59-9fd9-f8241852267e\","

                                + "\"filters\": [{\"field\": \"tag\", \"key\": \"User_id\", \"relation\": \"=\", \"value\": \"" + user_id + "\"}],"
                                //+  "\"included_segments\": [\"Active Users\"],"
                                + "\"data\": {\"foo\": \"bar\"},"
                                + "\"contents\": {\"en\": \"You have received assistance request!\"}"
                                + "}";


                        System.out.println("strJsonBody:\n" + strJsonBody);

                        byte[] sendBytes = strJsonBody.getBytes("UTF-8");
                        con.setFixedLengthStreamingMode(sendBytes.length);

                        OutputStream outputStream = con.getOutputStream();
                        outputStream.write(sendBytes);

                        int httpResponse = con.getResponseCode();
                        System.out.println("httpResponse: " + httpResponse);

                        if (httpResponse >= HttpURLConnection.HTTP_OK
                                && httpResponse < HttpURLConnection.HTTP_BAD_REQUEST) {
                            Scanner scanner = new Scanner(con.getInputStream(), "UTF-8");
                            jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                            scanner.close();
                        } else {
                            Scanner scanner = new Scanner(con.getErrorStream(), "UTF-8");
                            jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                            scanner.close();
                        }
                        System.out.println("jsonResponse:\n" + jsonResponse);

                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        });
    }

    public boolean isLocationProximity(GPSData gpsData){
        Location vLocation = new Location(LocationManager.GPS_PROVIDER);
        vLocation.setLatitude(gpsData.getCurrentLatitude());
        vLocation.setLongitude(gpsData.getCurrentLongitude());
        float distance = MainActivity.currentLocation.distanceTo(vLocation);
        if(distance < RADIUS_IN_METERS){
            return true;
        }
        return false;
    }

    //checks if the coordinates belongs to the BVI requestor
    public boolean isSelf(GPSData gpsData){
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = gpsData.getUserId();
        if(firebaseUser.getUid().equalsIgnoreCase(userId)){
            return true;
        }
        return false;
    }

    public boolean isBVI(GPSData gpsData){
        return gpsData.isBVICoordinates();
    }
}
