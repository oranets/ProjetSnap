package com.projetsnap.orane.projetsnap;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {
private RecyclerView mSnapList;
    private DatabaseReference mDataB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDataB=FirebaseDatabase.getInstance().getReference().child("Prjt_Snap");

        mSnapList=(RecyclerView) findViewById(R.id.snap_list);
        mSnapList.setHasFixedSize(true);
        mSnapList.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseRecyclerAdapter<SnapClass,SnapViewHolder> firebaseRecyclerAdapter= new FirebaseRecyclerAdapter<SnapClass, SnapViewHolder>(
                SnapClass.class,
                R.layout.snap_row,
                SnapViewHolder.class,
                mDataB

        ) {
            @Override
            protected void populateViewHolder(SnapViewHolder viewHolder, SnapClass model, int position) {
                viewHolder.setLoca(model.getLoc());
                viewHolder.setImage(getApplicationContext(),model.getImage());

            }
        };
        mSnapList.setAdapter(firebaseRecyclerAdapter);

    }
    public static class SnapViewHolder extends RecyclerView.ViewHolder{
        View mView;
        public SnapViewHolder(View itemView) {
            super(itemView);
            mView=itemView;
        }
        public void setLoca(String loca){
            TextView post_loc=(TextView) mView.findViewById(R.id.post_loc);
            post_loc.setText(loca);
        }
        public void setImage(Context ctx, String img){
            ImageView post_image=(ImageView) mView.findViewById(R.id.post_image);
            Picasso.with(ctx).load(img).into(post_image);

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()== R.id.action_add){
            startActivity(new Intent(MainActivity.this, PostActivity.class));

        }
        return super.onOptionsItemSelected(item);
    }
}
