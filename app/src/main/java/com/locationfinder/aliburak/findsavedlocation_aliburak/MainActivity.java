package com.locationfinder.aliburak.findsavedlocation_aliburak;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    static ArrayList<String> names = new ArrayList<>();
    static ArrayList<LatLng> locations = new ArrayList<>();
    static SwipeMenuListView listView;
    static ArrayAdapter arrayAdapter;
    FirebaseDatabase firebaseDatabase;
    String nameFromFirebase;
    String latituteFromFirebase;
    String longituteFromFirebase;
    Double latituteDouble;
    Double longituteDouble;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.add_place,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() ==R.id.add_place){
            Intent intent = new Intent(getApplicationContext(),MapsActivity.class);
            intent.putExtra("info","new");
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isConnected(this)==false){
            buildDialog(this).show();
        }else {
            setContentView(R.layout.activity_main);


        firebaseDatabase = FirebaseDatabase.getInstance();

        listView = (SwipeMenuListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1,names);

        listView.setAdapter(arrayAdapter);
        getDataFromFirebase();
        System.out.println("Gelen isimler"+names);
        //arrayAdapter.notifyDataSetChanged();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),MapsActivity.class);
                intent.putExtra("info","old");
                intent.putExtra("position",position);
                startActivity(intent);
            }
        });
        swipeListView();
        }
    }
    public boolean isConnected(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo newtinfo = cm.getActiveNetworkInfo();
        if (newtinfo != null && newtinfo.isConnectedOrConnecting()) {
            NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (mobile != null && mobile.isConnectedOrConnecting() || wifi !=null && wifi.isConnectedOrConnecting()){
                return true;
            }else {
                return false;
            }
        }else{
            return false;

        }
    }
    public AlertDialog.Builder buildDialog(Context c){
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("No Internet Connection");
        builder.setMessage("Please check your Internet connection. Press Ok to continue.");
        builder.setCancelable(false);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        return builder;

    }

    public void swipeListView(){

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                // create "open" item
                SwipeMenuItem openItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                //openItem.setBackground(new ColorDrawable(Color.rgb(0x00, 0x66, 0xff)));
                // set item width
                //openItem.setWidth(170);
                // set item title
                //openItem.setTitle("Delete");
                // set item title fontsize
                //openItem.setTitleSize(18);
                // set item title font color
                //openItem.setTitleColor(Color.WHITE);
                // add to menu
               // menu.addMenuItem(openItem);

                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                        0x3F, 0x25)));
                // set item width
                deleteItem.setWidth(170);
                // set a icon
                deleteItem.setIcon(R.drawable.ic_delete);
                // add to menu
                menu.addMenuItem(deleteItem);
            }
        };

        listView.setMenuCreator(creator);

        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0: // this part delete item.
                        System.out.println("onMenuItemClick: clicked item 0 " + position);
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
                        Query removeQuery = ref.child("Locations").orderByChild("name").equalTo(names.get(position));
                        removeQuery.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()){
                                    ds.getRef().removeValue();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(getApplicationContext(),"Hata"+databaseError.toException(),Toast.LENGTH_LONG).show();
                            }
                        });
                        break;
                    case 1:
                        //when you add edited part you can upgrade there.
                        System.out.println("onMenuItemClick: clicked item " + index);
                        break;
                }
                // false : close the menu; true : not close the menu
                return false;
            }
        });
    }





    public void getDataFromFirebase(){


           DatabaseReference newReference = firebaseDatabase.getReference("Locations");
           newReference.addValueEventListener(new ValueEventListener() {
               @Override
               public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                   //System.out.println("Veriler :" +dataSnapshot.getChildren());
                   //System.out.println("Veriler 2 :"+dataSnapshot.getValue());
                   names.clear();
                   for (DataSnapshot ds : dataSnapshot.getChildren()){
                       HashMap<String,String> hashMap = (HashMap<String,String >) ds.getValue();
                       System.out.println("Liste:"+hashMap.get("name"));

                            nameFromFirebase = hashMap.get("name");
                            latituteFromFirebase =  hashMap.get("latitute");
                            longituteFromFirebase = hashMap.get("longitute");
                            if (latituteFromFirebase != null && longituteFromFirebase != null){
                                latituteDouble = Double.parseDouble(latituteFromFirebase);
                                longituteDouble = Double.parseDouble(longituteFromFirebase);
                                LatLng locationLatLngFromFirebase = new LatLng(latituteDouble,longituteDouble);

                                names.add(nameFromFirebase);
                                System.out.println("isimler1:"+names);
                                locations.add(locationLatLngFromFirebase);
                                arrayAdapter.notifyDataSetChanged();
                            }



                   }

               }

               @Override
               public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(getApplicationContext(),databaseError.toString(),Toast.LENGTH_LONG).show();
               }
           });


    }
}
