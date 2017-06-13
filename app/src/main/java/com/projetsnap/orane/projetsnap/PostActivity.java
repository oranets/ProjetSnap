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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
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
                    mProgress.dismiss();
                    startActivity(new Intent(PostActivity.this,MainActivity.class));
                }

            });
        }

    }


}
