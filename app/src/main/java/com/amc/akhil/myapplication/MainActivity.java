package com.amc.akhil.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amc.akhil.myapplication.db.DatabaseHandler;
import com.amc.akhil.myapplication.db.GPSData;
import com.amc.akhil.myapplication.db.GPSInfo;
import com.amc.akhil.myapplication.db.User;
import com.amc.akhil.myapplication.db.UserInfo;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener, LocationListener, TextToSpeech.OnInitListener {

    private static final int TTS_CHECK_CODE = 0x122;
    private Button logoutButton;
    private Button needAssistanceButton;
    private Button emergencyButton;
    private Button exitButton;
    private AutoCompleteTextView userName;
    private EditText password;
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected Context context;
    TextView txtLat;
    String lat;
    String provider;
    protected String latitude, longitude;
    protected boolean gps_enabled, network_enabled;
    private DatabaseReference dbReference;
    private FirebaseAuth firebaseAuth;
    private NavigationView navigationView;
    public static Location currentLocation;
    public NotificationSenderService notificationSenderService;
    private TextToSpeech mTts;
    private FirebaseAnalytics firebaseAnalytics;
    private TextView titleTextView;
    private Button dropoffButton;

    private TTSServiceActivity ttsServiceActivity;

    private Map<String, GPSData> gpsDataMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .setNotificationOpenedHandler(new NotificationHandler())
                .init();
        initializeRefereces();
        checkLogin();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                speakOut("You are on Main screen");
            }
        },5000);
    }

    public void hideNavigationMenu() {
        navigationView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void checkLogin() {
        if(firebaseAuth.getCurrentUser() == null){
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        }
        //send tag to onesignal for push notification
        if(firebaseAuth.getCurrentUser() != null){
            OneSignal.sendTag("User_id",firebaseAuth.getCurrentUser().getUid());
            titleTextView.setText("Hello "+firebaseAuth.getCurrentUser().getDisplayName());
            startLocationInfoRetrieval();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if(view == needAssistanceButton){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    notificationSenderService.sendNotification(getGpsDataMap(), "assistance");
                }
            },4000);
            Intent intent = new Intent(this, AnimationActivity.class);
            intent.putExtra("request","assistance");
            startActivity(intent);
            speakOut("You have selected Need for assistance mode");
        }
        if(view == emergencyButton){
            speakOut("You selected Emergency mode");
            startActivity(new Intent(this, EmergencyActivity.class));
        }
        if(view == exitButton){
            speakOut("Closing the application");
            System.exit(0);
        }
        if(view == dropoffButton){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    notificationSenderService.sendNotification(getGpsDataMap(), "dropoff");
                }
            },4000);
            speakOut("You have requested for drop off");
            Intent intent = new Intent(this, AnimationActivity.class);
            intent.putExtra("request","dropoff");
            startActivity(intent);
        }
        if(view == logoutButton){
            speakOut("Signing out");
            firebaseAuth.signOut();
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        }
        return true;
    }


    @Override
    public void onClick(View view) {
        if(view == needAssistanceButton){
            speakOut("Need for assistance mode");
        }
        if(view == emergencyButton){
            speakOut("Emergency mode");
        }
        if(view == exitButton){
            speakOut("Exit the application");
        }
        if(view == dropoffButton){
            speakOut("Request for drop off");
        }
        if(view == logoutButton){
            speakOut("Sign out");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TTS_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                mTts = new TextToSpeech(MainActivity.this, this);
                mTts.setLanguage(Locale.US);
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }

    void startLocationInfoRetrieval() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
        } else {
            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
        }
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        retrieveGPSData();
    }

    @Override
    public void onLocationChanged(Location location) {
        writeGPSData(location);
        currentLocation = location;
        retrieveGPSData();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }

    public List<GPSData> retrieveGPSData(){
        dbReference.orderByValue().addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                GPSData gpsData = new GPSData((Map) dataSnapshot.getValue());
                getGpsDataMap().put(gpsData.getUserId(),gpsData);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                System.out.println("Data "+dataSnapshot.getValue());
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                System.out.println("Data "+dataSnapshot.getValue());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                System.out.println("Data "+dataSnapshot.getValue());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Data ");
            }
        });
        return null;
    }

    public void writeGPSData(Location location){
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if(firebaseUser != null){
            GPSData gpsData = new GPSData(location.getLatitude(), location.getLongitude(),
                                    location.getTime(), firebaseUser.getUid(), true);
            Map<String, Object> map = gpsData.toMap();
            dbReference.child("gps"+firebaseUser.getUid()).updateChildren(map);
        }
    }

    private void speakOut(String tts_text) {
        try {
            if(tts_text != null && mTts != null) {
                mTts.speak(tts_text, TextToSpeech.QUEUE_ADD, null);
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            System.out.println("Exception while TTS "+e.getMessage());
        }
    }

    public void initializeRefereces() {
        firebaseAuth = FirebaseAuth.getInstance();
        logoutButton = (Button) findViewById(R.id.logout);
        logoutButton.setOnClickListener(this);
        logoutButton.setOnLongClickListener(this);
        needAssistanceButton = (Button) findViewById(R.id.assistance);
        needAssistanceButton.setOnClickListener(this);
        needAssistanceButton.setOnLongClickListener(this);
        emergencyButton = (Button) findViewById(R.id.emergency);
        emergencyButton.setOnClickListener(this);
        emergencyButton.setOnLongClickListener(this);
        dropoffButton = (Button) findViewById(R.id.dropoff);
        dropoffButton.setOnClickListener(this);
        dropoffButton.setOnLongClickListener(this);
        exitButton = (Button) findViewById(R.id.exit);
        exitButton.setOnClickListener(this);
        exitButton.setOnLongClickListener(this);
        gpsDataMap = new HashMap<>();
        notificationSenderService = new NotificationSenderService();
        dbReference = FirebaseDatabase.getInstance().getReference("GPSInfo");
        ttsServiceActivity = new TTSServiceActivity();
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        titleTextView = (TextView) findViewById(R.id.title);
        checkForTTSPresent();
    }

    public void checkForTTSPresent() {
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, TTS_CHECK_CODE);
        /*try{
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            //do nothing
        }*/
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) {
            mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {
                    // Log.d("MainActivity", "TTS finished");
                }

                @Override
                public void onError(String utteranceId) {
                }

                @Override
                public void onStart(String utteranceId) {
                }
            });
        }
    }

    public TTSServiceActivity getTtsServiceActivity() {
        return ttsServiceActivity;
    }

    public class NotificationHandler implements OneSignal.NotificationOpenedHandler{

        @Override
        public void notificationOpened(OSNotificationOpenResult result) {
            startActivity(new Intent(getApplicationContext(),MapsActivity.class));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        speakOut("Closing the application");
        System.exit(0);
    }

    public Map<String, GPSData> getGpsDataMap() {
        return gpsDataMap;
    }

}
