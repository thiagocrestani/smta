package mei.tcd.smta.gps;

import com.google.android.maps.OverlayItem;

import com.google.android.maps.GeoPoint;
public class TrajectoOverlayItem extends OverlayItem {
	 
	    // Calsse construtora de overlayItem
	    public TrajectoOverlayItem(double latitude, double longitude)
	    { 
	    	// Lat, Log, Titulo e snippet
	        super(createGeoPoint(latitude, longitude),
	              String.format("(%f, %f)", latitude, longitude),
	              "");
	        
	    }
	 
	    private static GeoPoint createGeoPoint(double latitude, double longitude)
	    {
	        int e6Latitude = (int) (latitude * 1E6);
	        int e6Longitude = (int) (longitude * 1E6);
	        
	        return new GeoPoint(e6Latitude, e6Longitude);
	    }

	  
}
