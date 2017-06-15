package com.projetsnap.orane.projetsnap;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class PostActivity extends AppCompatActivity {

    private ImageButton mSelectImage;
    private Button mAddLoc;
    private TextView mViewLoc;
    private Button mSend;
    private EditText mTitle;
    private TextView mDate;
    private Uri mImageUri=null;

    double longitude, latitude;
    String date;
    TextView mLat, mLong;

    private static final int GALLERY_REQUEST=1;
    private StorageReference mStorage;
    private DatabaseReference mDB;
    LocationManager locationManager;
    private ProgressDialog mProgress;


    private static final String TAG = "PostActivity";


    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String FCM_MESSAGE_URL = " https://fcm.googleapis.com/fcm/send";


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        FirebaseMessaging.getInstance().subscribeToTopic("photoadded");
        Log.v(TAG, "subscribed= photoadded");

        setContentView(R.layout.activity_post);
        locationManager= (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        mStorage= FirebaseStorage.getInstance().getReference();
        mDB= FirebaseDatabase.getInstance().getReference().child("Prjt_Snap");

        mSelectImage=(ImageButton) findViewById(R.id.imageButton);
        mAddLoc= (Button) findViewById(R.id.button_loc) ;
        mViewLoc=(TextView) findViewById(R.id.loc_view);
        mSend=(Button) findViewById(R.id.send_to_db);
        mProgress= new ProgressDialog(this);
        mLat=(TextView) findViewById(R.id.Lat) ;
        mLong=(TextView) findViewById(R.id.Long) ;
        mTitle= (EditText) findViewById(R.id.titre) ;
        mDate=(TextView) findViewById(R.id.date_vue);
        date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        mDate.setText(date);


        mSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent=new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_REQUEST);
            }
        });
        mAddLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocation();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2 * 60 * 1000, 10, locationListenerGPS);

        }});

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDB();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==GALLERY_REQUEST&&resultCode==RESULT_OK){
            mImageUri=data.getData();
            mSelectImage.setImageURI(mImageUri);
        }
    }

    private boolean checkLocation() {
        if(!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }
    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
    }

    private final LocationListener locationListenerGPS= new LocationListener() {
        public void onLocationChanged(Location location) {

            longitude = location.getLongitude();
            latitude = location.getLatitude();
            mLat.setText(latitude+"");
            mLong.setText(longitude+"");
            mViewLoc.setText("Latitude: "+latitude+"\nLongitude: "+longitude);
            mProgress.dismiss();
        }
        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }
        @Override
        public void onProviderEnabled(String s) {

        }
        @Override
        public void onProviderDisabled(String s) {
        }
    };

    private void sendDB() {
        mProgress.setMessage("Sending to DB");
        mProgress.show();
        final String loc_val=mViewLoc.getText().toString().trim();
        final String titre_val=mTitle.getText().toString().trim();
        final String date_val=mDate.getText().toString().trim();
        if(!TextUtils.isEmpty(loc_val)&&mImageUri!=null){
            StorageReference filepath=mStorage.child("Snaps").child(mImageUri.getLastPathSegment());
            filepath.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl=taskSnapshot.getDownloadUrl();
                    DatabaseReference newSnap=mDB.push();
                    newSnap.child("loc").setValue(loc_val);
                    newSnap.child("title").setValue(titre_val);
                    newSnap.child("image").setValue(downloadUrl.toString());
                    newSnap.child("date").setValue(date_val);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //Code exécuté dans le nouveau thread
                            String retour = post(FCM_MESSAGE_URL);
                            Log.v(TAG, "index=" + retour);
                        }
                    }).start();

                    mProgress.dismiss();
                    startActivity(new Intent(PostActivity.this,MainActivity.class));

                }

            });

        }

    }

    public String post(String url) {


        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        JSONObject dataJson = new JSONObject();

        try {
            json.put("to", "/topics/photoadded");
            dataJson.put("title", "Nouvelle photo");
            dataJson.put("body", "une photo a été ajouté a la bibliotheque");
            json.put("notification", dataJson);
            RequestBody body = RequestBody.create(JSON, json.toString());
            Request request = new Request.Builder()
                    .header("Authorization", "key=AAAAlXAQb5k:APA91bHSuXbRLKtrRWpz7Xj82PkBMn9pNSBaLDN0Gys0X6V6QEtA9nKwpqPmvN67WIpIuXzLyedbrWNBxlBHLT5e9LgdH9AnNFjFL81t_kK6Xd7OXagXAkt0RfhcvxwpXS6XrCzA8_T8")
                    .url(url)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            return response.body().string();



        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";

    }


}
