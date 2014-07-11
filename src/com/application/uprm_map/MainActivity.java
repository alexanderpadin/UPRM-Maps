package com.application.uprm_map;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.application.uprm_map.R;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {
	private GoogleMap mMap;
	private static final LatLng UPRM = new LatLng(18.210885,-67.140884);
	String Buildings_Data, Exits_Data;
	ArrayList<String[]> DataList = new ArrayList<String[]>();
	ArrayList<String[]> ExitsList = new ArrayList<String[]>();
	boolean firstRun = false;
	Marker Mark, BuildingMarker, Entrance;
	int markerTime = 0;
	double user_latitude, user_longitude;
	ArrayList<Polyline> line = new ArrayList<Polyline>();
	String mapType = "satellite";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpMapIfNeeded();  
        
        if(isFirstRun()) {
        	if(isNetworkAvailable()){
        		ShowAbout();
        		getData obj = new getData();
        		Buildings_Data = obj.getAllData();
				try {
					Log.i("Data", Buildings_Data);
					createFile(Buildings_Data, "Data.txt");
				} catch (IOException e) {
					e.printStackTrace();
				} 
        	} else {
        		ConnectionWarning();
        		ShowAbout();
        		getData obj = new getData();
        		Buildings_Data = obj.getAllData();
				try {
					createFile(Buildings_Data, "Data.txt");
				} catch (IOException e) {
					e.printStackTrace();
				} 
        	}
        } else {
        	if(!isNetworkAvailable()){
        		ConnectionWarning();
        	}
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
		if (itemId == R.id.about) {
			ShowAbout();
			return true;
		} else if (itemId == R.id.type) {
			changeType();
			return true;
		} else if (itemId == R.id.location) {
			GPS_Location();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
    }
    
    @Override
	protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }
    

    private void createFile(String result, String filename) throws IOException {
  	    	FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
  	    	fos.write(result.getBytes());
  	    	fos.close();
  	    }
    
    private boolean isFirstRun() {
    	File file1 = getBaseContext().getFileStreamPath("Data.txt");
        File file2 = getBaseContext().getFileStreamPath("Exits.txt");
        
        if(file1.exists() && file2.exists()) {
        	return false;
        } else {
        	return true;
        }
    }
    
    public final boolean isNetworkAvailable() {
    	ConnectivityManager connec =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
    	if ( connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED ||
			connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTING ||
			connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTING ||
			connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED ) {
    		
    		return true;
    	} else if ( connec.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED ||  
				   connec.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED  ) {
    		return false;
    	} else{
    		return false;
    	}
    }
    
    public void ConnectionWarning() {
        final Dialog dlg = new Dialog(this);
    	dlg.setContentView(R.layout.close);
    	dlg.setTitle("No Network Connection");
    	TextView msg = (TextView) dlg.findViewById(R.id.message);
    	msg.setText("You are not connected to a network, Google Maps might not run properly.");
		
    	Button close = (Button) dlg.findViewById(R.id.closeThis);
    	close.setText("      Close      ");
    	close.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dlg.dismiss();
			}
		});
    	
    	Button Network = (Button) dlg.findViewById(R.id.openSettings);
    	Network.setText("     Settings    ");
    	Network.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
				dlg.dismiss();
			}
		});
    dlg.show();
    }
    
    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            if (mMap != null) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(UPRM, 17));
            }
        }
    }
  
    public void changeType() {
    	if(mapType.equals("satellite")) {
    		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    		mapType = "normal";
    	}
    	else if(mapType.equals("normal")) {
    		mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
    		mapType = "terrain";
    	}
    	else if (mapType.equals("terrain")) {
    		mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    		mapType = "satellite";
    	}
    }
    
    private void prepareData() {
    	Buildings_Data = readFileData("Data.txt");
    	Exits_Data = readFileData("Exits.txt");
    	Log.i("DATA", Buildings_Data);
    	try {
    		parseData("buildings");
    		parseData("exits");
    	} catch(Exception e) {
        	Toast.makeText(getApplicationContext(),	"Error Parsing Data", Toast.LENGTH_LONG).show();
        	File file1 = getBaseContext().getFileStreamPath("Data.txt");
            File file2 = getBaseContext().getFileStreamPath("Exits.txt");
            file1.delete();
            file2.delete();
            System.exit(1);
    	}
    }
    
    private String readFileData(String filename) {
    	String data;
    	try {
    	    BufferedReader inputReader = new BufferedReader(
    	    		new InputStreamReader(openFileInput(filename)));
    	    String inputString;
    	    StringBuffer stringBuffer = new StringBuffer();                
    	    while ((inputString = inputReader.readLine()) != null) {
    	        stringBuffer.append(inputString + "\n");
    	    }
    	    data = stringBuffer.toString();
    	} catch (IOException e) {
    	    e.printStackTrace();
    	    data = null;
    	}
    	return data;
    }
    
    private void parseData(String type) {
    	if(type.equals("buildings")) {
    		String[] Buildings = Buildings_Data.split("<br>");
    		for (int i = 0 ; i < Buildings.length ; i++) {
    			DataList.add(Buildings[i].split(","));
    		}
    	} else {
    		String[] Exits = Exits_Data.split("<br>");
    		for (int i = 0 ; i < Exits.length ; i++) {
    			ExitsList.add(Exits[i].split(","));
    		}	
    	}	
    }
    
    public void ShowAbout(){
    	final Dialog dlg = new Dialog(this);
      	dlg.setContentView(R.layout.activity_about);
      	dlg.setTitle("About");
  		
      	Button close = (Button) dlg.findViewById(R.id.close);

      	close.setOnClickListener(new OnClickListener() {
  			@Override
  			public void onClick(View v) {
  				dlg.dismiss();
  			}
  		});
      	dlg.show();
    }
    
    public void openDialog(View view) {
    	if(!firstRun) {
    		prepareData();
    		firstRun = true;
    	}
    	   	
    	final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.activity_dialog);
		dialog.setTitle("Find Building");
		dialog.setCanceledOnTouchOutside(true);
		
		final Spinner Buildings = (Spinner) dialog.findViewById(R.id.buildings);
		final EditText room = (EditText) dialog.findViewById(R.id.room);
		Button submit = (Button) dialog.findViewById(R.id.submit);
		Button cancel = (Button) dialog.findViewById(R.id.cancel);
		CheckBox makeRote = (CheckBox) dialog.findViewById(R.id.makeRoute);
		makeRote.setVisibility(View.GONE);
		
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, 
				R.array.building_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Buildings.setAdapter(adapter);
		
		Buildings.setOnItemSelectedListener(new OnItemSelectedListener() {
			
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					   int arg2, long arg3) {
				String input_building = Buildings.getSelectedItem().toString();
				if(!(input_building.equals("Select"))) {
					closeMarker();
					findBuilding(input_building);
					dialog.dismiss();
				}
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			
			}
		});
		
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
			}
		});
		
		submit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				closeMarker();
				String Room = new String(room.getText().toString());				
				Room = Room.replaceAll("[^A-Za-z]","");
				Room = Room.toLowerCase();
				creatMarker(searchBuildingByCode(Room));
				dialog.dismiss();
			}
		});
		dialog.show();
	}
    
    public void findBuilding(String building) {
    	creatMarker(searchBuilding(building));
    }
    
    public String[] searchBuilding(String building) {
    	for (String[] row : DataList) {
    		if(row[0].equals(building)) {
    			return row;
    		}
		}
    	return null;
    }
	
    public String[] searchBuildingByCode(String code) {
    	
    	if(code.equals("fa") || code.equals("fb") || code.equals("fc")) {
    		code = "f";
    	}
       	for (String[] row : DataList) {
    		if(row.length == 6 && row[5].equals(code)) {
    			return row;
    		}
		}
    	return null;
    }

    public void closeMarker() {
    	try {
        	BuildingMarker.remove();
        	Entrance.remove();
			for (Polyline l : line) {
				l.remove();
			}
		} catch (Exception e) { }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(UPRM, 17));
    }
    
    public void creatMarker(String[] row) {
    	if(!(row == null)){
    		LatLng location = new LatLng(Double.parseDouble(row[1]), Double.parseDouble(row[2]));
    		MarkerOptions Marker = new MarkerOptions()
    		.position(location)
    		.title(row[0])
    		.snippet("Press to view building");
    		mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
				public void onInfoWindowClick(com.google.android.gms.maps.model.Marker marker) {	
					String markerName = marker.getTitle();
					String id = "";
					for (String[] row : DataList) {
			    		if(row[0].equals(markerName)) {
			    			id = row[4];
			    		}
					}
					if(id != "") {
						createPhoto(id, markerName);
					} else {
						Toast.makeText(getApplicationContext(), "No photo available of "+ marker.getTitle(), Toast.LENGTH_LONG).show();
					}
				}
			});
    		
    		BuildingMarker = mMap.addMarker(Marker);
    		BuildingMarker.showInfoWindow();
    		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 18));
    	} else {
        	Toast.makeText(getApplicationContext(),	"Building Not Found.", Toast.LENGTH_LONG).show();
    	}
    }

    public void createExits() {
    	for(String[] Item : ExitsList) {
    		if(Item.length > 1){
				LatLng location = new LatLng(Double.parseDouble(Item[1]), Double.parseDouble(Item[2]));
				MarkerOptions Exit = new MarkerOptions().position(location).icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_dot));
				mMap.addMarker(Exit);
    		}
    	}
    }
    
    public void createPhoto(String id, String name) {	
    	final Dialog photo = new Dialog(this);
    	photo.setContentView(R.layout.picture);
    	photo.setTitle(name);
    	photo.setCanceledOnTouchOutside(true);
    	
    	ImageView buildingPhoto = (ImageView) photo.findViewById(R.id.buildingImage);
    	Uri uri = Uri.parse("android.resource://com.application.uprm_map/drawable/building_" +  id);
    	buildingPhoto.setImageURI(uri);
    	
    	DisplayMetrics metrics = this.getResources().getDisplayMetrics();
    	int width = metrics.widthPixels;
    	int height = metrics.heightPixels;
    	
    	buildingPhoto.getLayoutParams().height = height / 2;
    	buildingPhoto.getLayoutParams().width = width;
		
    	Button ok = (Button) photo.findViewById(R.id.close);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				photo.dismiss();
			}
		});
		Button Directions = (Button) photo.findViewById(R.id.dir);
		Directions.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					for (Polyline l : line) {
						l.remove();
					}
				} catch (Exception e) { }
				GetDirections();	
				photo.dismiss();
			}
		});		
		photo.show();
    }
   
    public void locationWarning() {
    	final Dialog dlg = new Dialog(this);
      	dlg.setContentView(R.layout.warning);
      	dlg.setTitle("Warning");
  		
      	Button close = (Button) dlg.findViewById(R.id.okok);

      	close.setOnClickListener(new OnClickListener() {
  			@Override
  			public void onClick(View v) {
  				dlg.dismiss();
  			}
  		});
      	dlg.show();
    }
    
    public void GetDirections() {
    	if(isGPS()) {
    		if(isNetworkAvailable()) {
    		GPS_Location();
    		
    		double start_lat;
    		double start_log;
    		
    		if(checkLocatio(user_latitude, user_longitude)) {
    			start_lat = user_latitude;
    			start_log = user_longitude;
    		} else {
    			start_lat = 18.211009;
    			start_log = -67.144905;
    			LatLng LLocation = new LatLng(start_lat, start_log);
    			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LLocation, 18));
    			MarkerOptions Marker = new MarkerOptions()
        		.position(LLocation)
        		.title("Campus Entrance")
        		.icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_dot));
    			Entrance = mMap.addMarker(Marker);
    			Entrance.showInfoWindow();
    			locationWarning();
    		}
    		
    		String directions_url = makeURL (start_lat, start_log, BuildingMarker.getPosition().latitude, BuildingMarker.getPosition().longitude);
    		new connectAsyncTask(directions_url).execute();
    		} else {
    			ConnectionWarning();
    		}
    	} else {
    		 final Dialog dlg = new Dialog(this);
          	dlg.setContentView(R.layout.close);
          	dlg.setTitle("No GPS enable");
          	TextView msg = (TextView) dlg.findViewById(R.id.message);
          	msg.setText("Your GPS seems to be disabled, do you want to enable it?");
      		
          	Button close = (Button) dlg.findViewById(R.id.closeThis);
          	close.setText("        NO       ");
          	close.setOnClickListener(new OnClickListener() {
      			@Override
      			public void onClick(View v) {
      				dlg.dismiss();
      			}
      		});
          	
          	Button Network = (Button) dlg.findViewById(R.id.openSettings);
          	Network.setText("        YES       ");
          	Network.setOnClickListener(new OnClickListener() {
      			
      			@Override
      			public void onClick(View v) {
      				startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
      				dlg.dismiss();
      			}
      		});
          dlg.show();
    	}
    }
    
    boolean isGPS(){
    	LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
    	if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
           return false;
        }
    	else {
    		return true;
    	}
    }
    
    public void GPS_Location() {
    	if(isGPS()){
     	    LocationManager locationManager = (LocationManager) 
     	            getSystemService(LOCATION_SERVICE);
     	    Criteria criteria = new Criteria();
     	    String bestProvider = locationManager.getBestProvider(criteria, false);
     	    Location location = locationManager.getLastKnownLocation(bestProvider);
     	    LocationListener loc_listener = new LocationListener() {

     	        @Override
				public void onLocationChanged(Location l) {
     	        	user_latitude = l.getLatitude();
          	        user_longitude = l.getLongitude();
          	        LatLng LLocation = new LatLng(user_latitude, user_longitude);
          	        Mark.setPosition(LLocation);
     	        }

     	        @Override
				public void onProviderEnabled(String p) {
     	        	Toast.makeText(getApplicationContext(),	"GPS signal recovered.", Toast.LENGTH_LONG).show();
     	        }

     	        @Override
				public void onProviderDisabled(String p) {
                	Toast.makeText(getApplicationContext(),	"GPS signal lost.", Toast.LENGTH_LONG).show();
     	        }

     	        @Override
				public void onStatusChanged(String p, int status, Bundle extras) {}
     	    };
     	    locationManager
     	            .requestLocationUpdates(bestProvider, 0, 0, loc_listener);
     	    location = locationManager.getLastKnownLocation(bestProvider);
     	    try {
     	        user_latitude = location.getLatitude();
     	        user_longitude = location.getLongitude();
     	        LatLng LLocation = new LatLng(user_latitude, user_longitude);
         		
     	        if(markerTime == 0) {
     	        MarkerOptions Marker = new MarkerOptions()
         		.position(LLocation).icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_dot)).title("My Location");

         		Mark = mMap.addMarker(Marker);
         		Mark.showInfoWindow();
         		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LLocation, 18));
         		markerTime = 1;
     	        } else {
     	        	Mark.setPosition(LLocation);
     	        	Mark.showInfoWindow();
     	        	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LLocation, 18));
     	        }
         		
     	    } catch (NullPointerException e) {
     	    	user_latitude = -1.0;
     	    	user_longitude = -1.0;
     	    }
         } else {
        	 final Dialog dlg = new Dialog(this);
         	dlg.setContentView(R.layout.close);
         	dlg.setTitle("No GPS enable");
         	TextView msg = (TextView) dlg.findViewById(R.id.message);
         	msg.setText("Your GPS seems to be disabled, do you want to enable it?");
     		
         	Button close = (Button) dlg.findViewById(R.id.closeThis);
         	close.setText("        NO       ");
         	close.setOnClickListener(new OnClickListener() {
     			@Override
     			public void onClick(View v) {
     				dlg.dismiss();
     			}
     		});
         	
         	Button Network = (Button) dlg.findViewById(R.id.openSettings);
         	Network.setText("        YES       ");
         	Network.setOnClickListener(new OnClickListener() {
     			
     			@Override
     			public void onClick(View v) {
     				startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
     				dlg.dismiss();
     			}
     		});
         dlg.show();
         }
    	
    }
    
    public boolean checkLocatio(double lat, double log) {
    	if((lat >= 18.184905 && lat <= 18.222249) && (log >= -67.161741 && log <= -67.11874)) {
    		return true;
    	} else {
    		return false;
    	}
    }
  
    public String makeURL (double sourcelat, double sourcelog, double destlat, double destlog ){
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString
                .append(Double.toString( sourcelog));
        urlString.append("&destination=");// to
        urlString
                .append(Double.toString( destlat));
        urlString.append(",");
        urlString.append(Double.toString( destlog));
        urlString.append("&sensor=false&mode=walking&alternatives=true");
        
        Log.i("URL", urlString.toString());
        
        return urlString.toString();
 }
       
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
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

            LatLng p = new LatLng( ((lat / 1E5)),
                     ((lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }
      
    public void drawPath(String  result) {
        try {
               final JSONObject json = new JSONObject(result);
               JSONArray routeArray = json.getJSONArray("routes");
               JSONObject routes = routeArray.getJSONObject(0);
               JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
               String encodedString = overviewPolylines.getString("points");
               List<LatLng> list = decodePoly(encodedString);

               for(int z = 0; z<list.size()-1;z++){
                    LatLng src= list.get(z);
                    LatLng dest= list.get(z+1);
                    line.add(mMap.addPolyline(new PolylineOptions()
                    .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude,   dest.longitude))
                    .width(7)
                    .color(Color.CYAN).geodesic(true)));
                   
                }
               
        } 
        catch (JSONException e) {

        }
    } 

    private class connectAsyncTask extends AsyncTask<Void, Void, String>{
        private ProgressDialog progressDialog;
        String url;
        connectAsyncTask(String urlPass){
            url = urlPass;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Fetching route, Please wait...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }
        @Override
        protected String doInBackground(Void... params) {
            JSONParser jParser = new JSONParser();
            String json = jParser.getJSONFromUrl(url);
            return json;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);   
            progressDialog.hide();        
            if(result!=null){
                drawPath(result);
            }
        }
    }
    
    public class JSONParser {
         InputStream is = null;
        JSONObject jObj = null;
         String json = "";
        // constructor
        public JSONParser() {
        }
        public String getJSONFromUrl(String url) {
            try {
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(url);

                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();
                is = httpEntity.getContent();           

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        is, "iso-8859-1"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }

                json = sb.toString();
                is.close();
            } catch (Exception e) {
                Log.e("Buffer Error", "Error converting result " + e.toString());
            }
            return json;

        }
    }
}



