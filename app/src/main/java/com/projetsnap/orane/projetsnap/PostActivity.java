package com.projetsnap.orane.projetsnap;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
    private Uri mImageUri=null;

    private static final int GALLERY_REQUEST=1;
    private StorageReference mStorage;
    private DatabaseReference mDB;
    private LocationManager lm;
    private Location location;
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
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        //location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        location=new Location(LocationManager.NETWORK_PROVIDER);

        mStorage= FirebaseStorage.getInstance().getReference();
        mDB= FirebaseDatabase.getInstance().getReference().child("Prjt_Snap");

        mSelectImage=(ImageButton) findViewById(R.id.imageButton);
        mAddLoc= (Button) findViewById(R.id.button_loc) ;
        mViewLoc=(TextView) findViewById(R.id.loc_view);
        mSend=(Button) findViewById(R.id.send_to_db);
        mProgress= new ProgressDialog(this);


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
                afficherAdresse();

        }});

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDB();
            }
        });
    }


 private void afficherAdresse() {
        Geocoder geo = new Geocoder(PostActivity.this);
        try {
            List<Address> adresses = geo.getFromLocation(location.getLatitude(),
                    location.getLongitude(),1);
            if(adresses != null && adresses.size() == 1){
                Address adresse = adresses.get(0);
                ((TextView)findViewById(R.id.loc_view)).setText(String.format("%s, %s %s",
                        adresse.getAddressLine(0),
                        adresse.getPostalCode(),
                        adresse.getLocality()));
            }
            else {
                ((TextView)findViewById(R.id.loc_view)).setText("Adresse introuvable");
            }
        } catch (IOException e) {
            e.printStackTrace();
            ((TextView)findViewById(R.id.loc_view)).setText("L'adresse n'a pu être déterminée.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==GALLERY_REQUEST&&resultCode==RESULT_OK){
            mImageUri=data.getData();
            mSelectImage.setImageURI(mImageUri);
        }
    }

    private void sendDB() {
        mProgress.setMessage("Sending to DB");
        mProgress.show();
        String loc_val=mViewLoc.getText().toString().trim();
        if(!TextUtils.isEmpty(loc_val)&&mImageUri!=null){
            StorageReference filepath=mStorage.child("Snaps").child(mImageUri.getLastPathSegment());
            filepath.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl=taskSnapshot.getDownloadUrl();
                    DatabaseReference newSnap=mDB.push();
                    newSnap.child("loc").setValue(mViewLoc.toString());
                    newSnap.child("image").setValue(downloadUrl.toString());

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
