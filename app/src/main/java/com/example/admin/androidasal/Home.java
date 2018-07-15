package com.example.admin.androidasal;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.admin.androidasal.Common.Common;
import com.example.admin.androidasal.Model.User2;
import com.example.admin.androidasal.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{

    SupportMapFragment mapFragment;

    //Location
    private GoogleMap mMap;
    private static final int MY_PERMISSION_REQUEST_CODE = 280197;//Ngay sinh nhat :3
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 10919997;//CRUSH @@

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private static int UPDATE_INTERVAL = 5000;//5 SECS
    private static int FATEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference ref;
    GeoFire geoFire;

    Marker mUserMarker;

    boolean isSignFound = false;
    String signId = "";
    int radius = 1; //1 km

    double distance = 0.2;//200 m

    int kt = 1;


    //Car animation
    private List<LatLng> polyLineList;

    private float v;
    private double lat, lng;
    private Handler handler;
    private LatLng startPostion, endPosition, currentPosition;
    private int index, next;
    private Button btnGo;
    private EditText edtPlace;
    private String destination;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private Polyline blackPolyline, greyPolyLine;

    private IGoogleAPI mService;


    Runnable drawPathRunnable = new Runnable() {
        @Override
        public void run() {
            if(index < polyLineList.size() - 1){
                index++;
                next = index+1;
                if (index < polyLineList.size() - 1){
                    startPostion = polyLineList.get(index);
                    endPosition = polyLineList.get(next);
                }

                ValueAnimator valueAnimator = ValueAnimator.ofFloat(0,1);
                valueAnimator.setDuration(3000);
                valueAnimator.setInterpolator(new LinearInterpolator());
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        v = valueAnimator.getAnimatedFraction();
                        lng = v*endPosition.longitude+(1-v)*startPostion.longitude;
                        lat = v*endPosition.latitude+(1-v)*startPostion.latitude;
                        LatLng newPos = new LatLng(lat,lng);
                        mUserMarker.setPosition(newPos);
                        mUserMarker.setAnchor(0.5f, 0.5f);
                        mUserMarker.setRotation(getBearing(startPostion, newPos));
                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder()
                                        .target(newPos)
                                        .zoom(15.5f)
                                        .build()
                        ));
                    }
                });
                valueAnimator.start();
                handler.postDelayed(this, 3000);
            }
        }
    };

    private float getBearing(LatLng startPostion, LatLng endPosition) {
        double lat = Math.abs(startPostion.latitude - endPosition.latitude);
        double lng = Math.abs(startPostion.longitude - endPosition.longitude);

        if(startPostion.latitude < endPosition.latitude && startPostion.longitude < endPosition.longitude)
            return (float)(Math.toDegrees(Math.atan(lng/lat)));
        else if (startPostion.latitude >= endPosition.latitude && startPostion.longitude < endPosition.longitude)
            return (float)((90 - Math.toDegrees(Math.atan(lng/lat))) + 90);
        else if (startPostion.latitude >= endPosition.latitude && startPostion.longitude >= endPosition.longitude)
            return (float)(Math.toDegrees(Math.atan(lng/lat))+180);
        else if(startPostion.latitude < endPosition.latitude && startPostion.longitude >= endPosition.longitude)
            return (float)((Math.toDegrees(Math.atan(lng/lat)))+270);
        return -1;
    } // Deo can dung den -.-

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Maps
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        polyLineList = new ArrayList<>();
        btnGo = (Button)findViewById(R.id.btnGo);
        edtPlace = (EditText)findViewById(R.id.edtPlace);

        btnGo.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                destination = edtPlace.getText().toString();
                destination = destination.replace(" ", "+");
                Log.d("TAD",destination);

                getDirection();
            }
        });

//        //Geo fire
//        ref = FirebaseDatabase.getInstance().getReference(Common.signs_tbl);
//        geoFire = new GeoFire(ref);

        System.out.println("TAD281");
        setUpLocation();



        mService = Common.getGoogleAPI();


    }

    private void getDirection(){
        currentPosition = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

        String requestApi = null;
        try{
            requestApi = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+currentPosition.latitude+","+currentPosition.longitude+"&"+
                    "destination="+destination+"&"+
                    "key="+getResources().getString(R.string.google_direction_api);
            Log.d("TAD", requestApi);//In ra UEL bi loi
            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());
                                JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                for(int i=0; i<jsonArray.length();i++){
                                    JSONObject route = jsonArray.getJSONObject(i);
                                    JSONObject poly = route.getJSONObject("overview_polyline");
                                    String polyline = poly.getString("points");
                                    polyLineList = decodePoly(polyline);
                                }
                                //Adjusting bounds
                                //if(!polyLineList.isEmpty()) {
                                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                    for (LatLng latLng : polyLineList)
                                        builder.include(latLng);

                                    LatLngBounds bounds = builder.build();
                                    CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,2);
                                    mMap.animateCamera(mCameraUpdate);
                                //}



                                polylineOptions = new PolylineOptions();
                                polylineOptions.color(Color.GRAY);
                                polylineOptions.width(5);
                                polylineOptions.startCap(new SquareCap());
                                polylineOptions.endCap(new SquareCap());
                                polylineOptions.jointType(JointType.ROUND);
                                polylineOptions.addAll(polyLineList);
                                greyPolyLine = mMap.addPolyline(polylineOptions);

                                blackPolylineOptions = new PolylineOptions();
                                blackPolylineOptions.color(Color.BLACK);
                                blackPolylineOptions.width(5);
                                blackPolylineOptions.startCap(new SquareCap());
                                blackPolylineOptions.endCap(new SquareCap());
                                blackPolylineOptions.jointType(JointType.ROUND);
                                blackPolyline = mMap.addPolyline(blackPolylineOptions);

                                mMap.addMarker(new MarkerOptions()
                                        .position(polyLineList.get(polyLineList.size()-1))
                                        .title("Chon vi tri"));

                                ValueAnimator polyLineAnimator = ValueAnimator.ofInt(0,100);
                                polyLineAnimator.setDuration(2000);
                                polyLineAnimator.setInterpolator(new LinearInterpolator());
                                polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                        List<LatLng> points = greyPolyLine.getPoints();
                                        int percenValue = (int)valueAnimator.getAnimatedValue();
                                        int size = points.size();
                                        int newPoints = (int)(size * (percenValue/100.0f));
                                        List<LatLng> p = points.subList(0, newPoints);
                                        blackPolyline.setPoints(p);
                                    }
                                });

                                polyLineAnimator.start();

                                mUserMarker = mMap.addMarker(new MarkerOptions().position(currentPosition)
                                        .flat(true)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                                handler = new Handler();
                                index=-1;
                                next = 1;
                                handler.postDelayed(drawPathRunnable, 3000);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(Home.this,""+t.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });
        }catch(Exception e){
            e.printStackTrace();
        }
    } // Deo can dung den -.-

    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    } // Deo can dung den -.-

    //Press Ctr+O


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    }

    private void setUpLocation() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            //Request runtime permission
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            },MY_PERMISSION_REQUEST_CODE);
        }
        else {
            if(checkPlayServices()){
                buildGoogleApiClient();
                createLocationRequest();
                //if(location_switch.isChecked())
                displayLocation();
            }
        }
    }

    private void displayLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation!=null){
            //if(location_switch.isChecked()){
            final double latitube = mLastLocation.getLatitude();
            final double longitube = mLastLocation.getLongitude();

//            final double latitube = 21.036511;
//            final double longitube = 105.787943;

//            final double latitube = 21.0361486;
//            final double longitube = 105.7824342;

//                //Update to Firebase
//                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitube, longitube), new GeoFire.CompletionListener() {
//                    @Override
//                    public void onComplete(String key, DatabaseError error) {
            //Add Maker

//                        ArrayMap<Double, Double> list = new ArrayMap<>();
//                        list.put(21.036481, 105.783290);
//                        list.put(21.036449, 105.783171);
//                        list.put(21.036495, 105.782710);
//            list.put(21.036508, 105.782617);
//            list.put(21.036503, 105.782537);
//            list.put(21.036503, 105.782482);
//            list.put(21.036502, 105.782445);
//            list.put(21.036500, 105.782410);
//            list.put(21.036504, 105.782388);
//            list.put(21.036500, 105.782362);
//            list.put(21.036502, 105.782338);
//            list.put(21.036503, 105.782307);
//            list.put(21.036504, 105.782285);
//            list.put(21.036503, 105.782278);
//            list.put(21.036507, 105.782260);
//            list.put(21.036508, 105.782238);
//            list.put(21.036508, 105.782216);
//            list.put(21.036511, 105.782190);
//            list.put(21.036511, 105.782163);
//            list.put(21.036509, 105.782140);
//            list.put(21.036506, 105.782116);
//            list.put(21.036506, 105.782091);
//            list.put(21.036506, 105.782091);
//            list.put(21.036506, 105.782047);
//            list.put(21.036508, 105.782022);
////            list.put();
//            list.put(21.036511, 105.782001);
//            list.put(21.036513, 105.781968);
//            list.put(21.036513, 105.781950);
//            list.put(21.036513, 105.781931);
//            list.put(21.036515, 105.781911);
//            list.put(21.036520, 105.781889);
//            list.put(21.036518, 105.781871);
//            list.put(21.036513, 105.781815);
//            list.put(21.036546, 105.781764);
//            list.put(21.036552, 105.781744);
//            list.put(21.036559, 105.781725);
//            list.put(21.036564, 105.781709);
//            list.put(21.036565, 105.781694);
//            list.put(21.036566, 105.781679);
//            list.put(21.036566, 105.781660);
//            list.put(21.036564, 105.781641);
//            list.put(21.036564, 105.781627);
//            list.put(21.036566, 105.781609);
//            list.put(21.036566, 105.781587);
//            list.put(21.036564, 105.781562);
//            list.put(21.036562, 105.781541);
//            list.put(21.036558, 105.781522);
//            list.put(21.036557, 105.781495);
//            list.put(21.036554, 105.781469);
//            list.put(21.036551, 105.781447);
//            list.put(21.036549, 105.781426);
//            list.put(21.036548, 105.781397);
//            list.put(21.036548, 105.781373);
//            list.put(21.036541, 105.781345);
//            list.put(21.036531, 105.781324);
//            list.put(21.036518, 105.781296);
//            list.put(21.036508, 105.781273);
//            list.put(21.036498, 105.781248);
//            list.put(21.036491, 105.781224);
//            list.put(21.036481, 105.781202);
//            list.put(21.036472, 105.781173);
//            list.put(21.036460, 105.781145);
//            list.put(21.036449, 105.781119);
//            list.put(21.036439, 105.781088);
//            list.put(21.036429, 105.781058);
//            list.put(21.036419, 105.781030);
//            list.put(21.036405, 105.780994);
//            list.put(21.036391, 105.780960);
//            list.put(21.036380, 105.780929);
//            list.put(21.036372, 105.780897);
//            list.put(21.036365, 105.780858);
//            list.put(21.036365, 105.780817);
//            list.put(21.036365, 105.780770);
//            list.put(21.036369, 105.780725);
//            list.put(21.036372, 105.780677);
//            list.put(21.036378, 105.780628);
//            list.put(21.036381, 105.780590);
//            list.put(21.036389, 105.780548);
//            list.put(21.036401, 105.780491);
//            list.put(21.036409, 105.780431);
//            list.put(21.036416, 105.780388);
//            list.put(21.036422, 105.780345);
//            list.put(21.036432, 105.780300);
//            list.put(21.036438, 105.780254);
//            list.put(21.036442, 105.780202);
//            list.put(21.036450, 105.780153);
//            list.put();
//            list.put();
//
            //for( int i = 0; i < list.size(); ++i ) {
//                latitube = list.keyAt(i);
//                longitube = list.valueAt(i);


//                latitube = list.keyAt(0);
//                longitube = list.valueAt(0);

            if (mUserMarker != null)
                mUserMarker.remove();// Remove already maker
            mUserMarker = mMap.addMarker(new MarkerOptions()
                    //.icon(BitmapDescriptorFactory.fromResource(R.drawable.traffic_sign))
                    .position(new LatLng(latitube, longitube))
                    .title("Bạn"));
            //Move camera to  this postion

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitube, longitube), 15.0f));

            //loadAllAvailableSign();
            // sleep
            //SystemClock.sleep(20000);
            //}

//            latitube = list.keyAt(1);
//            longitube = list.valueAt(1);
//
//            if (mUserMarker != null)
//                mUserMarker.remove();// Remove already maker
//            mUserMarker = mMap.addMarker(new MarkerOptions()
//                    //.icon(BitmapDescriptorFactory.fromResource(R.drawable.traffic_sign))
//                    .position(new LatLng(latitube, longitube))
//                    .title("Bạn"));
//            //Move camera to  this postion
//
//            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitube, longitube), 15.0f));

            loadAllAvailableSign();

//
//                        //rotateMarker(mCurrent, -360, mMap);
//                    }
//                });
            Log.d("TAD", String.format("Dia chi cua ban da bi thay doi: %f / %f", latitube, longitube));
            // }
        }
        else {
            Log.d("Loi", "Khong the tim toi vi tri cua Bien bao");
        }
    }


    private void loadAllAvailableSign() {



        //Load all avaiable Sign in distance 3 km
        DatabaseReference signLocation = FirebaseDatabase.getInstance().getReference(Common.signs_tbl);
        GeoFire gf = new GeoFire(signLocation);

        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), distance);
        geoQuery.removeAllListeners();



        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {
                //Use key to get email from table Users
                //Table Users is table when drive register account and update information
                //Just open your Sign to check this table name
                FirebaseDatabase.getInstance().getReference(Common.users_tbl)
                        .child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                //Because User2 and User model is same properties
                                // So we can use User2 model to get User here
                                User2 user2 = dataSnapshot.getValue(User2.class);

                                //System.out.println(location.latitude+" - " +location.longitude);
                                if(user2!=null) {

                                    //Add sign to map

                                    String lc = user2.getEmail();
//
                                    if (lc.compareTo("rephai@gmail.com") == 0) {
                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(location.latitude, location.longitude))
//                                        .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                                                .flat(true)
                                                .title("Bien re phai")
                                                //.title(user2.getName())
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.rephai)));
                                    }

                                    else
                                        if (lc.compareTo("retrai@gmail.com") == 0) {
                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(location.latitude, location.longitude))
//                                        .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                                                .flat(true)
                                                .title("Bien re trai")
                                                //.title(user2.getName())
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.retrai)));
                                    }

                                    else if (lc.compareTo("denbao@gmail.com") == 0) {

                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(location.latitude, location.longitude))
//                                        .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                                                .flat(true)
                                                .title("Den bao")
                                                //.title(user2.getName())
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.denbao)));
                                    }
                                    else if (lc.compareTo("biendung@gmail.com") == 0) {

                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(location.latitude, location.longitude))
//                                        .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                                                .flat(true)
                                                .title("Bien dung")
                                                //.title(user2.getName())
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.biendung)));

                                    }
                                    else if (lc.compareTo("motchieu@gmail.com") == 0) {

                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(location.latitude, location.longitude))
//                                        .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                                                .flat(true)
                                                .title("Một chiều")
                                                //.title(user2.getName())
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.motchieu)));

                                    }
                                }
//                                mMap.addMarker(new MarkerOptions()
//                                        .position(new LatLng(location.latitude, location.longitude))
////                                        .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
//                                        .flat(true)
//                                        .title("Traffic")
//                                        //.title(user2.getName())
//                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.if_traffic_1054955)));
                                //System.out.println(location.latitude+ " "+ location.longitude);
                                //System.out.println(user2.getName());
                                // System.out.println("TAD!!!");
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
//
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(distance <= 0.2) { // distance just find for 3 km
                    distance++;
                    loadAllAvailableSign();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });


    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS){
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode,this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            else {
                Toast.makeText(this, "Day la dich vu khong duoc ho tro", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
//        //Add sample marker
//        googleMap.addMarker(new MarkerOptions()
//                        .position(new LatLng(37.7750,-122.4183))
//                        .title("TAD"));
//        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.7750,-122.4183),12));
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    private void startLocationUpdates() {

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
    }
}
