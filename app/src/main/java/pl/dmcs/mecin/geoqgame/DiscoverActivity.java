package pl.dmcs.mecin.geoqgame;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class DiscoverActivity extends FragmentActivity implements OnMapReadyCallback, HttpAsyncTask.AsyncResponse {

    private static int REPORTING_INTERVAL = 10000;
    private static int MAP_ZOOM = 20;
    private static int MIN_TIME_INTERVAL = 1000;
    private static int MIN_DISTANCE = 1;
    private static int NO_OF_QUESTS = 5;
    private static int RETRY_INTERVAL = 5000;
    private static int MAX_NO_OF_TRY = 3;
    private int noOfTry = 0;

    private Marker myPositionMarker;
    private HttpAsyncTask myHttpAsyncTask = new HttpAsyncTask();
    private Timer timer = new Timer();
    private MyTimerTask myTask = new MyTimerTask();
    private GoogleMap mMap;
    private TextView latLngField;
    private Button startButton;
    private Button zoomInButton;
    private Button zoomOutButton;
    private LocationManager locationManager;
    private String myLatitude = "";
    private String myLongitude = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        myHttpAsyncTask.delegate = this;

        zoomInButton = (Button) this.findViewById(R.id.zoom_in);

        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MAP_ZOOM += 1;

            }
        });

        zoomOutButton = (Button) this.findViewById(R.id.zoom_out);

        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MAP_ZOOM > 0) {
                    MAP_ZOOM -= 1;
                }
            }
        });

        startButton = (Button) this.findViewById(R.id.start_button);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(getApplicationContext(), "Quests not found!", Toast.LENGTH_LONG);
                toast.show();
            }
        });

        latLngField = (TextView) this.findViewById(R.id.my_coords);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                myLatitude = String.format("%.4f", location.getLatitude());
                myLongitude = String.format("%.4f", location.getLongitude());
                latLngField.setText("(" + myLatitude + "," + myLongitude + ")");

                LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                if(myPositionMarker != null) myPositionMarker.setPosition(myLatLng);

                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLatLng, MAP_ZOOM);

                mMap.animateCamera(cameraUpdate);

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };


        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Toast toast = Toast.makeText(getApplicationContext(), "Permissions for GPS denied!", Toast.LENGTH_LONG);
            toast.show();

            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_INTERVAL, MIN_DISTANCE, locationListener);

        timer.schedule(myTask, 2*MIN_TIME_INTERVAL);
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng meStart;
        if(myLongitude.equals("") && myLongitude.equals("")) {
            meStart = new LatLng(-34, 151);
        } else {
            meStart = new LatLng(Double.valueOf(myLatitude), Double.valueOf(myLongitude));
        }
        myPositionMarker = mMap.addMarker(new MarkerOptions().position(meStart).title("Me").icon(BitmapDescriptorFactory.fromResource(R.drawable.me_marker)));

    }

    @Override
    public void getQuests(JSONObject quests) {
        noOfTry = 0;
        //parse JSON and get quests
        Log.d("getQuests", quests.toString());
        JSONObject tmpJQuest;
        try {
            for(int i = 1; i < NO_OF_QUESTS+1; i++) {
                Log.d("JSON " + i + " ", quests.getJSONObject(String.valueOf(i)).toString());
                tmpJQuest = quests.getJSONObject(String.valueOf(i));
                Log.d("tmpJQuest", tmpJQuest.getString("lat") + " " + tmpJQuest.getString("lng") );
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void notifyFailedOperation(String msg) {
        noOfTry++;
        if (noOfTry < MAX_NO_OF_TRY) {
            Toast.makeText(getApplicationContext(), msg + " Retrying in "+(RETRY_INTERVAL/1000)+" s ("+noOfTry+"/"+MAX_NO_OF_TRY+").", Toast.LENGTH_LONG).show();
            reScheduleTimer(RETRY_INTERVAL);
        } else {
            Toast.makeText(getApplicationContext(), msg + " ("+noOfTry+"/"+MAX_NO_OF_TRY+").", Toast.LENGTH_LONG).show();
        }
    }

    private void reScheduleTimer(int interval) {
        timer.cancel();
        myTask.cancel();
        timer = new Timer();
        myTask = new MyTimerTask();
        myHttpAsyncTask = new HttpAsyncTask();
        myHttpAsyncTask.delegate = this;
        timer.schedule(myTask, interval);
    }

    public class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            //Create and send JSON with position and vehicle number (if exists)
            JSONObject myPositionJsonObject = new JSONObject();

            try {
                myPositionJsonObject.put(Tables.Position.LATITUDE, myLatitude);
                myPositionJsonObject.put(Tables.Position.LONGITUDE, myLongitude);

                //Send JSON
                myHttpAsyncTask.execute(myPositionJsonObject, Tables.API_SERVER + Tables.API_GET_QUEST);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            //myPositionMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(Double.valueOf(myLatitude),Double.valueOf(myLongitude))));

            System.out.println("LATITUDE ===> " + myLatitude);
            System.out.println("LONGITUDE ===> " + myLongitude);

        }
    }
}
