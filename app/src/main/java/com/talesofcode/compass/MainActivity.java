package com.talesofcode.compass;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    protected static final int REQUEST_PERMISSION_FINE_LOCATION = 1;

    private SensorManager sensorManager;
    private FusedLocationProviderClient fusedLocationClient;

    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private float heading, oldHeading, longitude, latitude, altitude, magneticDeclination, trueHeading;

    private boolean isLocationRetrieved = false;

    private TextView textViewHeading, textViewTrueHeading, textViewMagneticDeclination;
    private ImageView imageViewCompass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewHeading = findViewById(R.id.text_view_heading);
        textViewTrueHeading = findViewById(R.id.text_view_true_heading);
        textViewMagneticDeclination = findViewById(R.id.text_view_magnetic_declination);
        imageViewCompass = findViewById(R.id.image_compass);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //check if we have permission to access location
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            //fine location permission already granted
            getLocation();
        } else {
            //if permission is not granted, request location permissions from user
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_FINE_LOCATION);
        }

        heading = oldHeading = longitude = latitude = altitude = magneticDeclination = trueHeading = 0;

        //Default value is N/A. If location was retrieved, then text will be updated to respective values
        textViewTrueHeading.setText(R.string.not_available);
        textViewMagneticDeclination.setText(R.string.not_available);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_GAME);
        }

        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //make sensor readings smoother using a low pass filter
            CompassHelper.lowPassFilter(event.values.clone(), accelerometerReading);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            //make sensor readings smoother using a low pass filter
            CompassHelper.lowPassFilter(event.values.clone(), magnetometerReading);
        }
        updateHeading();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void updateHeading() {
        //oldHeading required for image rotate animation
        oldHeading = heading;

        heading = CompassHelper.calculateHeading(accelerometerReading, magnetometerReading);
        heading = CompassHelper.convertRadtoDeg(heading);
        heading = CompassHelper.map180to360(heading);

        if (isLocationRetrieved) {
            trueHeading = heading + magneticDeclination;
            if (trueHeading > 360) { //if trueHeading was 362 degrees for example, it should be adjusted to be 2 degrees instead
                trueHeading = trueHeading - 360;
            }
            textViewTrueHeading.setText(getString(R.string.true_heading, (int) trueHeading));
        }

        textViewHeading.setText(getString(R.string.heading, (int) heading));

        RotateAnimation rotateAnimation = new RotateAnimation(-oldHeading, -heading, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(500);
        rotateAnimation.setFillAfter(true);
        imageViewCompass.startAnimation(rotateAnimation);
    }

    @SuppressLint("MissingPermission")
    //suppress warning since we have already checked for permissions before calling the function
    private void getLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            isLocationRetrieved = true;
                            latitude = (float) location.getLatitude();
                            longitude = (float) location.getLongitude();
                            altitude = (float) location.getAltitude();
                            magneticDeclination = CompassHelper.calculateMagneticDeclination(latitude, longitude, altitude);
                            textViewMagneticDeclination.setText(getString(R.string.magnetic_declination, magneticDeclination));
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_FINE_LOCATION) {
            //if request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission is granted
                getLocation();
            } else {
                //display Toast with error message
                Toast.makeText(this, R.string.location_error_msg, Toast.LENGTH_LONG).show();
            }
        }
    }
}

