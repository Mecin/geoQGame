package pl.dmcs.mecin.geoqgame;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class DiscoverActivity extends FragmentActivity implements OnMapReadyCallback, HttpAsyncTask.AsyncResponse, LocationListener {

    private static int MAP_ZOOM = 16;
    private static int DISTANCE_TOLERANCE = 5;
    private static Double EARTH_RADIUS = 6378.41;
    private static int MIN_TIME_INTERVAL = 1000;
    private static int MIN_DISTANCE = 1;
    private static int NO_OF_QUESTS = 5;
    private static int RETRY_INTERVAL = 5000;
    private static int MAX_NO_OF_TRY = 3;
    private static String RETRY_BUTTON = "Retry";
    private static String STOP_BUTTON = "Stop";
    private static String START_BUTTON = "Start!";

    private int noOfTry = 0;
    private Map<Integer, Double[]> questMap = new HashMap<Integer, Double[]>();
    private int currentQuestPointer = -1;
    private boolean gameStarted = false;
    private long startTime;
    private long durationTime;
    private Double measuredDistance;

    private Marker myPositionMarker;
    private Polyline currQuestTrace;
    private Marker[] questMarkers = new Marker[NO_OF_QUESTS];

    private HttpAsyncTask myHttpAsyncTask = new HttpAsyncTask();
    private Timer timer = new Timer();
    private MyTimerTask myTask = new MyTimerTask();
    private GoogleMap mMap;
    private TextView latLngField;
    private Button startButton;
    private Button zoomInButton;
    private Button zoomOutButton;
    private TextView displayTextView;

    private LocationManager locationManager;
    private String provider;
    Criteria criteria;
    Location myLocation;

    private String myLatitude = "";
    private String myLongitude = "";

    DatabaseHandler db = new DatabaseHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        myHttpAsyncTask.delegate = this;

        displayTextView = (TextView) this.findViewById(R.id.display);

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

                String startButtonText = startButton.getText().toString();

                if (startButtonText.equals(RETRY_BUTTON)) {
                    startButton.setText(START_BUTTON);
                    noOfTry = 0;
                    notifyFailedOperation("");
                    return;
                }

                if (startButtonText.equals(STOP_BUTTON)) {
                    stopGame();
                    displayTextView.setText("Stopped");
                    return;
                }

                Log.d("before start", "questmap: " + questMap.isEmpty() + " currentqp: " + currentQuestPointer);
                if (questMap.isEmpty() && currentQuestPointer == -1) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Quests not found!", Toast.LENGTH_LONG);
                    toast.show();

                    return;
                }

                if (gameStarted) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Game already started!", Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }

                if (startButtonText.equals(START_BUTTON)) {
                    startGame();
                    return;
                }
            }
        });

        latLngField = (TextView) this.findViewById(R.id.my_coords);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(false);

        provider = locationManager.getBestProvider(criteria, true);

        Log.d("bestProvider", provider);

        myReqLocationUp();

        timer.schedule(myTask, 2 * MIN_TIME_INTERVAL);
    }

    @Override
    public void onLocationChanged(Location location) {
        myLocation = location;
        // Called when a new location is found by the network location provider.
        myLatitude = String.format("%.4f", myLocation.getLatitude());
        myLongitude = String.format("%.4f", myLocation.getLongitude());
        latLngField.setText("(" + myLatitude + "," + myLongitude + ")");

        LatLng myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());

        if (myPositionMarker != null) myPositionMarker.setPosition(myLatLng);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLatLng, MAP_ZOOM);

        mMap.animateCamera(cameraUpdate);

        if (gameStarted) {
            Log.d("game", "calc distance between me and " + currentQuestPointer + " quest.");
            updateGameStatus();
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng meStart;
        if (myLongitude.equals("") && myLongitude.equals("")) {
            meStart = new LatLng(-34, 151);
        } else {
            meStart = new LatLng(Double.valueOf(myLatitude), Double.valueOf(myLongitude));
        }
        //myPositionMarker = mMap.addMarker(new MarkerOptions().position(meStart).title("Me").icon(BitmapDescriptorFactory.fromResource(R.drawable.me_marker)));
        myPositionMarker = mMap.addMarker(new MarkerOptions().position(meStart).title("Me"));

    }

    @Override
    public void getQuests(JSONObject quests) {
        noOfTry = 0;

        if (!questMap.isEmpty() || currentQuestPointer != -1 || gameStarted) {
            Log.d("getQuests", "Quests not empty! Current quest: " + currentQuestPointer + ".");
            Toast.makeText(getApplicationContext(), "Error, game in progress.", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject tmpJQuest;
        //questMap.clear();
        //currentQuestPointer = -1;

        try {
            for (int i = 1; i < NO_OF_QUESTS + 1; i++) {
                Log.d("JSON " + i + " ", quests.getJSONObject(String.valueOf(i)).toString());
                tmpJQuest = quests.getJSONObject(String.valueOf(i));
                Log.d("tmpJQuest", tmpJQuest.getString("lat") + " " + tmpJQuest.getString("lng"));
                Log.d("double", Double.valueOf(tmpJQuest.getString("lat")) + " " + Double.valueOf(tmpJQuest.getString("lng")));
                questMap.put(i, new Double[]{Double.valueOf(tmpJQuest.getString("lat")), Double.valueOf(tmpJQuest.getString("lng"))});
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Toast.makeText(getApplicationContext(), "Sector found. Quests downloaded.", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void notifyFailedOperation(String msg) {
        noOfTry++;
        if (noOfTry < MAX_NO_OF_TRY) {
            Toast.makeText(getApplicationContext(), msg + " Retrying in " + (RETRY_INTERVAL / 1000) + " s (" + noOfTry + "/" + MAX_NO_OF_TRY + ").", Toast.LENGTH_SHORT).show();
            reScheduleTimer(RETRY_INTERVAL);
        } else {
            Toast.makeText(getApplicationContext(), msg + " (" + noOfTry + "/" + MAX_NO_OF_TRY + ").", Toast.LENGTH_SHORT).show();
            if (startButton != null) {
                startButton.setText("Retry");
            }
        }
    }

    private void myReqLocationUp() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast toast = Toast.makeText(getApplicationContext(), "Permissions for GPS denied!", Toast.LENGTH_LONG);
            toast.show();
        } else {

            Boolean isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            Boolean isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                Toast.makeText(getApplicationContext(), "Permissions for GPS and Network denied!", Toast.LENGTH_LONG).show();
            } else {
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_INTERVAL,
                            MIN_DISTANCE, this);

                    provider = LocationManager.NETWORK_PROVIDER;

                    Log.d("Network", "Network Enabled");

                    if (locationManager != null) {
                        myLocation = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (myLocation != null) {
                            Log.d("Network", "last location not null");
                            //onLocationChanged(myLocation);
                        }
                    }
                }

                if (isGPSEnabled) {
                    if (myLocation == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_INTERVAL,
                                MIN_DISTANCE, this);

                        provider = LocationManager.GPS_PROVIDER;

                        Log.d("GPS", "GPS Enabled");

                        if (locationManager != null) {
                            myLocation = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (myLocation != null) {
                                Log.d("GPS", "last location not null");
                                //onLocationChanged(myLocation);
                            }
                        }
                    }
                }
            }

            //locationManager.requestLocationUpdates(provider, MIN_TIME_INTERVAL, MIN_DISTANCE, this);
        }
    }

    private void startGame() {
        gameStarted = true;

        startTime = 0;

        measuredDistance = 0.0;

        startTime = System.currentTimeMillis();

        startButton.setText(STOP_BUTTON);

        currentQuestPointer = 0;

        LatLng questCoords;

        questCoords = new LatLng(questMap.get(1)[0], questMap.get(1)[1]);
        questMarkers[0] = mMap.addMarker(new MarkerOptions().position(questCoords).title("Quest 1.").icon(BitmapDescriptorFactory.fromResource(R.drawable.q1)));

        questCoords = new LatLng(questMap.get(2)[0], questMap.get(2)[1]);
        questMarkers[1] = mMap.addMarker(new MarkerOptions().position(questCoords).title("Quest 2.").icon(BitmapDescriptorFactory.fromResource(R.drawable.q2)));

        questCoords = new LatLng(questMap.get(3)[0], questMap.get(3)[1]);
        questMarkers[2] = mMap.addMarker(new MarkerOptions().position(questCoords).title("Quest 3.").icon(BitmapDescriptorFactory.fromResource(R.drawable.q3)));

        questCoords = new LatLng(questMap.get(4)[0], questMap.get(4)[1]);
        questMarkers[3] = mMap.addMarker(new MarkerOptions().position(questCoords).title("Quest 4.").icon(BitmapDescriptorFactory.fromResource(R.drawable.q4)));

        questCoords = new LatLng(questMap.get(5)[0], questMap.get(5)[1]);
        questMarkers[4] = mMap.addMarker(new MarkerOptions().position(questCoords).title("Quest 5.").icon(BitmapDescriptorFactory.fromResource(R.drawable.q5)));

        currQuestTrace = mMap.addPolyline(new PolylineOptions().add(myPositionMarker.getPosition(), questMarkers[currentQuestPointer].getPosition()).width(3).color(Color.RED));

        updateGameStatus();
    }

    private void stopGame() {
        for (int i = 0; i < NO_OF_QUESTS; i++) {
            questMarkers[i].remove();
        }

        currQuestTrace.remove();

        questMap.clear();

        startButton.setText(RETRY_BUTTON);

        currentQuestPointer = -1;

        durationTime = (System.currentTimeMillis() - startTime) / 1000;

        Log.d("stopGame", "duration: " + durationTime + " s, distance: " + measuredDistance + " m.");

        db.addHistoryEntry(measuredDistance, durationTime);

        gameStarted = false;
    }

    private void updateGameStatus() {

        for (int i = 0; i < currentQuestPointer; i++) {
            questMarkers[i].setIcon(BitmapDescriptorFactory.fromResource(R.drawable.qv));
        }

        LatLng myPos = myPositionMarker.getPosition();
        LatLng curQPos = questMarkers[currentQuestPointer].getPosition();
        Double distance = getDistance(myPos.latitude, myPos.longitude, curQPos.latitude, curQPos.longitude, EARTH_RADIUS);
        distance *= 1000;
        distance = Double.valueOf(Math.round(distance));

        measuredDistance += distance;

        Log.d("updateGame", "Distance between me nad quest " + currentQuestPointer + " is " + distance + " meters.");

        displayTextView.setText(distance + " m.");

        //currQuestTrace = mMap.addPolyline(new PolylineOptions().add(myPositionMarker.getPosition(),questMarkers[0].getPosition()).width(5).color(Color.RED));

        if (distance <= DISTANCE_TOLERANCE) {
            Log.d("updateGame", "Reached quest point " + currentQuestPointer + "!");
            if (currentQuestPointer == NO_OF_QUESTS - 1) {
                questMarkers[currentQuestPointer].setIcon(BitmapDescriptorFactory.fromResource(R.drawable.qv));
                // finish game
                stopGame();
                displayTextView.setText("WON!");
                Toast.makeText(getApplicationContext(), "Congratulation, You WON!", Toast.LENGTH_LONG).show();
                return;
            }
            //recreate polyline and increment currentQuestPointer
            Toast.makeText(getApplicationContext(), "Checkpoint reached! (" + (currentQuestPointer + 1) + "/" + NO_OF_QUESTS + ")", Toast.LENGTH_SHORT).show();
            currentQuestPointer++;
            updateGameStatus();
        }

        List<LatLng> tracePolylineCoords = new ArrayList<LatLng>();
        tracePolylineCoords.add(myPositionMarker.getPosition());
        tracePolylineCoords.add(questMarkers[currentQuestPointer].getPosition());

        currQuestTrace.setPoints(tracePolylineCoords);

    }

    private Double getDistance(Double lat1, Double lng1, Double lat2, Double lng2, Double earthRadius) {
        return acos(sin(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(Math.toRadians(lng1) - Math.toRadians(lng2))) * earthRadius;
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

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast toast = Toast.makeText(getApplicationContext(), "Permissions for GPS denied!", Toast.LENGTH_LONG);
            toast.show();
        } else {
            locationManager.removeUpdates(this);
        }
        timer.cancel();
        myTask.cancel();
        if (!gameStarted) {
            startButton.setText(RETRY_BUTTON);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        myReqLocationUp();
    }


    public class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            //Create and send JSON with position and vehicle number (if exists)
            JSONObject myPositionJsonObject = new JSONObject();

            if (myLocation != null) {
                myLatitude = String.valueOf(myLocation.getLatitude());
                myLongitude = String.valueOf(myLocation.getLongitude());
            }

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
