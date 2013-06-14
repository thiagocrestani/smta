package mei.tcd.smta.gps;

import java.util.ArrayList;
import mei.tcd.smta.R;
import mei.tcd.smta.gps.Gps.InterfaceGps;
import com.google.android.maps.MapActivity;
import android.graphics.drawable.Drawable;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.GpsStatus; // Para verificar se temos GPs ou não
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
// As actividade terão de implementar o interface para comunicarem com a classe GPS
public class Gps_Activity extends MapActivity implements InterfaceGps{

	// Google Maps
	private MapView mapView;
	private boolean initialZoomSet;
	
	private TrajectoOverlay trajectoOverlay;
	
	
	private Button startBtn;
	private Button stopBtn;
	private Button nextBtn, previousBtn;
	private ViewSwitcher mViewSwitcher;
	//private View mapaView;
	private static TextView gpsInformacao, gpsSatelites, gpsFix;
	private boolean mComecou;
	// Instancia do listener 
	Gps gpsListener;
	//  private PointDatabaseManager pointDatabaseManager;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.gpsinfo);

		// referencia da instancia do mapa google maps
		mapView = (MapView) findViewById(R.id.mapview1);
		startBtn = (Button) findViewById(R.id.start);
		stopBtn = (Button) findViewById(R.id.stop);
		nextBtn = (Button)  findViewById(R.id.maisinfo);
		nextBtn = (Button)  findViewById(R.id.paraTras);
		gpsInformacao = (TextView) findViewById(R.id.gpsInformacao);
		gpsSatelites = (TextView) findViewById(R.id.gpsSatelites);
		gpsFix = (TextView) findViewById(R.id.gpsFix);
		mViewSwitcher = (ViewSwitcher)findViewById(R.id.viewSwitcher1);
		
		// Habilitar controlos de zoom
		mapView.setBuiltInZoomControls(true);
		// Drawable do oberlayItem.
		Drawable inicioDrawable = getResources().getDrawable(R.drawable.dot_icon);
		// Um overlay é uma lista de Items de  overlay . Parametros é o drawable (balão) e o mapa.
		//trajectoOverlay = new TrajectoOverlay(inicioDrawable, mapView);
		// Acede à lista de overlays, neste caso adiciona a nossa trajectoOverlay.
		mapView.getOverlays().add(trajectoOverlay);
		// Instancia do meu listener gps
		gpsListener = new Gps(getApplicationContext(),this);

	}
	// clicklistener do botão next view (switchviewer)
	public void onNextClick(View view)
	{
		mViewSwitcher.showNext();
	}
	// clicklistener do botão previous view (switchviewer)
	public void onPreviousClick(View view)
	{
		mViewSwitcher.showPrevious();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		startBtn.setEnabled(!mComecou);
		stopBtn.setEnabled(mComecou);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		// unregister tal como nos listeners


	}
	//Obrigatoriedade Google para o servidor saber se o serviço esta a ser usado para tracking
	@Override
	protected boolean isRouteDisplayed()
	{
		return true;
	}
	//Obrigatoriedade Google para o servidor saber se o serviço esta a ser usado para localização
	@Override
	protected boolean isLocationDisplayed()
	{
		return true;
	}
	// Começa a acção. Evento definido no XML
	public void onStartClick(View view)
	{

		if (!mComecou)
		{
			startBtn.setEnabled(false);
			stopBtn.setEnabled(true);
			gpsListener.startGps();

			mComecou = true;
		}
	}

	public void onStopClick(View view)
	{
		if (mComecou)
		{

			gpsListener.stopGps();
			mComecou = false;
			startBtn.setEnabled(true);
			stopBtn.setEnabled(false);

		}
	}
	// Calbacks do Interface InterfaceGps (definida em GPs.java)
	@Override
	public void onLocationChanged(Location location) {
		trajectoOverlay.adicionaPonto(location.getLatitude(),
				location.getLongitude());

		int latE6 = (int) (location.getLatitude() * 1E6);
		int longE6 = (int) (location.getLongitude() * 1E6);
		gpsInformacao.setText(Float.toString(location.getSpeed()));
		mapView.getController().animateTo(new GeoPoint(latE6, longE6));

		if (!initialZoomSet)
		{
			mapView.getController().setZoom(15);
			initialZoomSet = true;
		}

	}
	@Override
	public void onGpsStatusChanged(int event) {
		switch (event) {
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			gpsSatelites.append("\nSolução de posicionamento adquirida!");
			break;
		case GpsStatus.GPS_EVENT_STARTED:
			gpsSatelites.append("Gps started!");	
			break;
		case GpsStatus.GPS_EVENT_STOPPED:
			gpsSatelites.append("\nGps stopped!");		
			break;
		}
		

	}
	@Override
	public void sateliteStatus(ArrayList<GpsSatellite> arraySatelites) {
		gpsFix.setText("\nPrn  |   Snr  |  Usado");
		for(int i =0;i<arraySatelites.size();i++)
			gpsFix.append("\n"+arraySatelites.get(i).getPrn() + " | " + 
								arraySatelites.get(i).getSnr() + " | " + 
								arraySatelites.get(i).usedInFix());		

	}

}
