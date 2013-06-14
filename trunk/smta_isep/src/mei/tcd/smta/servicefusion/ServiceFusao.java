package mei.tcd.smta.servicefusion;

import java.util.ArrayList;

import mei.tcd.smta.gps.Gps;
import mei.tcd.smta.gps.Gps.InterfaceGps;
import mei.tcd.smta.servicefusion.ServiceActivity.InsGpsReceiver;
import android.app.IntentService;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
// O IntentService permite-me usar uma thread diferente da thread main.
// Creates a default worker thread that executes all intents delivered to onStartCommand() separate from your application's main thread.
// Leitura:
// http://mobile.tutsplus.com/tutorials/android/android-fundamentals-intentservice-basics/
// http://thinkandroid.wordpress.com/2010/02/02/custom-intents-and-broadcasting-with-receivers/
// http://www.edumobile.org/android/android-programming-tutorials/use-of-services/
// http://developer.android.com/guide/components/services.html
public class ServiceFusao extends IntentService implements InterfaceGps  {
	Gps gpsListener;
	// Bundle para envio pelo broadcast
	Bundle mBundleLocation = new Bundle();
	public static final String LATITUDE = "";
	public static final String LONGITUDE = "";
	public static final String BROADCAST=
	    "mei.tcd.smta.servicefusion.BROADCAST";
	private static Intent broadcast=new Intent();
	
	
	 // Dá excepção quando coloco string name. 
	// You service needs to have a public no-args constructor. Otherwize, Android will not be able to instantiate it.
	public ServiceFusao() {
		super("Nome");
		// TODO Auto-generated constructor stub
		Log.d("Service", "Construcotr");
	}
	// Equivalente ao onStartCommand (mas aqui trata uma thread apenas)
	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		Log.d("Service", "HandleIntent");
	}
	// Equivalente ao onStart agora deprecated
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	  //  Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
		// Começar o listener do gps
		gpsListener = new Gps(getApplicationContext(),this);
		gpsListener.startGps();
		Log.d("Service", "StartCommand");
	    return super.onStartCommand(intent,flags,startId);
	    
	}
	@Override
	public void onCreate() {
		super.onCreate();
		// Instancia do meu listener gps
		Log.d("Service", "OnCrete");
	}
	
	
	
	// Métodos do Gps Listener e GpsStatus Listener ---------------------------------------
	@Override
	public void onLocationChanged(Location location) {
		
		mBundleLocation.putDouble(ServiceFusao.LATITUDE, location.getLatitude());
		mBundleLocation.putDouble(ServiceFusao.LONGITUDE, location.getLongitude());
		broadcast.setAction(InsGpsReceiver.ACTION_RESP);
		broadcast.addCategory(Intent.CATEGORY_DEFAULT);
		broadcast.putExtras(mBundleLocation);
		sendBroadcast(broadcast);
		
	}
	@Override
	public void onGpsStatusChanged(int event) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void sateliteStatus(ArrayList<GpsSatellite> arraySatelites) {
		// TODO Auto-generated method stub
		
	}
	//Métodos do Gps Listener e GpsStatus Listener ---------------------------------------

}
