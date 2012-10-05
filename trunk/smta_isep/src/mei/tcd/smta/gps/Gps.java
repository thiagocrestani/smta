package mei.tcd.smta.gps;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class Gps implements LocationListener, GpsStatus.Listener {
	private InterfaceGps iGps;
	private static final long DISTANCIA_MINIMA_ENTRE_UPDATES = 0; // Em metros  
	private static final long TEMPO_MINIMO_ENTRE_UPDATES = 0; // Em milisegundos  
	// Arrays de envio de informação satelite
	private  ArrayList<GpsSatellite> arraySatelites = new ArrayList<GpsSatellite>();
	// Location Manager providencia acesso aos serviços de localização
	private LocationManager locationManager;
	
	public Gps(Context context,InterfaceGps _iGps){
		this.iGps = _iGps;
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationManager.addGpsStatusListener(this);
		 
	}
	public void startGps()
	{
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,TEMPO_MINIMO_ENTRE_UPDATES, DISTANCIA_MINIMA_ENTRE_UPDATES, this);
	}
	public void stopGps()
	{
		locationManager.removeUpdates(this);
		locationManager.removeGpsStatusListener(this);
	}
	// métodos do listener do location
	@Override
	public void onLocationChanged(Location location) {
		this.iGps.onLocationChanged(location);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
/// Avisar que não está ligado
	}
	// Métodos do listener do statusGPs
	@Override
	public void onGpsStatusChanged(int event) {
		GpsStatus gpsStatus = locationManager.getGpsStatus(null);
		switch (event) {
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			
			break;
		case GpsStatus.GPS_EVENT_STARTED:
				
			break;
		case GpsStatus.GPS_EVENT_STOPPED:
				
			break;
		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			Iterable<GpsSatellite>satellites = gpsStatus.getSatellites();
            Iterator<GpsSatellite>satI = satellites.iterator();
            //String stGps = "Satelites:";
            if(!arraySatelites.isEmpty()) // Causa exfepção quando vazio ao limpar
            	arraySatelites.clear();
            while (satI.hasNext()) {
                GpsSatellite satellite = satI.next();
                arraySatelites.add(satellite);
//                // Os satelites são identificados pelo receptor pelo seu PRN.
//               stGps +=  "\n" + satellite.getPrn() + "," + satellite.usedInFix() + "," + satellite.getSnr() + "," + satellite.getAzimuth() + "," + satellite.getElevation(); 
//                // http://en.wikipedia.org/wiki/Global_Positioning_System: the almanac consists of coarse orbit and status information for each satellite
//                // http://en.wikipedia.org/wiki/Ephemeris: the positions of astronomical objects in the sky at a given time
//                // + "," + satellite.hasAlmanac() + "," + satellite.hasEphemeris());
               
            }
            this.iGps.sateliteStatus(arraySatelites);
            
		}
		this.iGps.onGpsStatusChanged(event);
	}
	
	// as actividade deverão implementar a seguinte interface. Envia apenas quando alteração de location (locationChange) ou gpsStatusChange.
	public interface InterfaceGps {
		// LocationListener
	    public void onLocationChanged(Location location);
	    // GpsStatus Listener
	    public void onGpsStatusChanged(int event);
	    public void sateliteStatus( ArrayList<GpsSatellite> arraySatelites);
	}
	
	
}
