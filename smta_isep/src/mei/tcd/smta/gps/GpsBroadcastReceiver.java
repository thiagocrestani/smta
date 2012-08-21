package mei.tcd.smta.gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
//Leitura
//http://thinkandroid.wordpress.com/2009/12/28/relationship-between-receivers-intents-and-activitiesservices/


// Esta classse extende o broadcast receiver  para verificar quais o tipo de notificações que o broadcast está a receber.
public class GpsBroadcastReceiver extends BroadcastReceiver {

	 private static final String TAG = "BroadcastReceiver";
	 ////Intent { act=mei.tcd.smta.gps.GPSIN (has extras) }
	    @Override
	    public void onReceive(Context context, Intent intent)
	    {
	    	// O receiver recebeu qualquer coisa
	        Log.d(TAG, "Recebi Intent");
	       // Será que é de mudança de localização que vem do pendingIntent
	        if (intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED))
	        {
	            Log.d(TAG, "LocationManager.KEY_LOCATION_CHANGED");
	            
	            Location location = (Location) intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
	            onLocationChanged(context, location);
	        }
	        // Será que é do provider alteraado (não deve ser pois o wifi não é usado. Mas a primeira pode ser, e logo temos de saber que ele está enable.
	        else if (intent.hasExtra(LocationManager.KEY_PROVIDER_ENABLED))
	        {
	            Log.d(TAG, "LocationManager.KEY_PROVIDER_ENABLED");
	            
	            if (intent.getExtras().getBoolean(LocationManager.KEY_PROVIDER_ENABLED))
	            {
	                onProviderEnabled(null);
	            }
	            else
	            {
	                onProviderDisabled(null);
	            }
	        }
	        // Se usarmos sensor de proximidade.
	        else if (intent.hasExtra(LocationManager.KEY_PROXIMITY_ENTERING))
	        {
	            Log.d(TAG, "Received KEY_PROXIMITY_ENTERING");
	            
	            if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false))
	            {
	                onEnteringProximity(context);
	            }
	            else
	            {
	                onExitingProximity(context);
	            }
	        }
	        // Aqui posso verificar o numero de satelites
	        else if (intent.hasExtra(LocationManager.KEY_STATUS_CHANGED))
	        {
	            
	        }
	    }
	    
	    public void onLocationChanged(Context context, Location location) {}
	    public void onProviderEnabled(String provider) {}
	    public void onProviderDisabled(String provider) {}
	    public void onEnteringProximity(Context context) {}
	    public void onExitingProximity(Context context) {}
	    public void onStatusChanged() {}

}
