package com.example.formapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    private static final int DELAY_TIME = 3000;

    EditText farmer, insurer, ins_amt, ins_no, t_period, start, end, location_txt;

    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();

    private final static int ALL_PERMISSIONS_RESULT = 101;
    LocationTrack locationTrack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissions.add(ACCESS_FINE_LOCATION);
        permissions.add(ACCESS_COARSE_LOCATION);

        permissionsToRequest = findUnAskedPermissions(permissions);
        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (permissionsToRequest.size() > 0)
                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        }


        farmer = findViewById(R.id.farmer_name);
        insurer = findViewById(R.id.insurer_name);
        ins_amt = findViewById(R.id.ins_amount);
        ins_no = findViewById(R.id.ins_number);
        t_period = findViewById(R.id.time_period);
        start = findViewById(R.id.start_date);
        end = findViewById(R.id.end_date);
        location_txt = findViewById(R.id.location_txt);



        ImageButton photoButton = findViewById(R.id.camera_btn);
        ImageButton locationButton = findViewById(R.id.location_btn);

        Button Upload = findViewById(R.id.submit_form);

        Upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity outerClass = new MainActivity();
                asyncUpload task = outerClass.new asyncUpload(MainActivity.this, "Uploading", "Uploading Insurance details");
                task.execute();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {

                        farmer.setText("");
                        insurer.setText("");
                        ins_amt.setText("");
                        ins_no.setText("");
                        t_period.setText("");
                        start.setText("");
                        end.setText("");
                        location_txt.setText("");

                        Toast.makeText(MainActivity.this, "Insurance uploaded for Review", Toast.LENGTH_SHORT).show();

                        Intent myIntent = new Intent(MainActivity.this, WebViewDashboard.class);
                        startActivity(myIntent);

                    }
                }, DELAY_TIME);
            }
        });

        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                }
                else
                {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });

        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationTrack = new LocationTrack(MainActivity.this);


                if (locationTrack.canGetLocation()) {


                    double longitude = locationTrack.getLongitude();
                    double latitude = locationTrack.getLatitude();

                    location_txt.setText("Longitude:" + Double.toString(longitude) + ",Latitude:" + Double.toString(latitude));

                    // Toast.makeText(getApplicationContext(), "Longitude:" + Double.toString(longitude) + "\nLatitude:" + Double.toString(latitude), Toast.LENGTH_SHORT).show();



                } else {

                    locationTrack.showSettingsAlert();
                }
            }
        });

    }

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<String>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case ALL_PERMISSIONS_RESULT:
                for (String perms : permissionsToRequest) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }

                }

                break;
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationTrack.stopListener();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {


            MainActivity outerClass = new MainActivity(); //Outer class
            asyncUpload task = outerClass.new asyncUpload(MainActivity.this, "Extracting Image Data", "Fetching text from your image...");
            task.execute();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    setTextFields();
                }
            }, DELAY_TIME);

        }

    }


    private class asyncUpload extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        Activity activity;
        String title, message;

        public asyncUpload(MainActivity activity, String title, String message) {
            this.activity = activity;
            this.title = title;
            this.message = message;

            dialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            dialog.setTitle(this.title);
            dialog.setMessage(this.message);
            dialog.setCancelable(false);

            dialog.show();
        }
        @Override
        protected Void doInBackground(Void... args) {

            try {
                Thread.sleep(DELAY_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            // do UI work here
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    private void setTextFields() {

        farmer.setText("anuj singh");
        insurer.setText("babloo halwaai");
        ins_amt.setText("2 lakh");
        ins_no.setText("123456789");
        t_period.setText("12 months");
        start.setText("aaj se");
        end.setText("kal tak");

    }


}
