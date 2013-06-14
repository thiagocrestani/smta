/**
 * Teste sobre o INS com mapa google e inicialização do vetor posicao com GPS.
 * 
 * Apenas para ver marcadores de posição do INS no mapa e verificar a orientação global.
 */
package mei.tcd.smta.ins;

import java.util.ArrayList;

import mei.tcd.smta.R;
import mei.tcd.smta.SmtaPreferences;
import mei.tcd.smta.gps.Gps.InterfaceGps;
import mei.tcd.smta.gps.Gps;
//import mei.tcd.smta.gps.TrajectoOverlay;
import mei.tcd.smta.ins.InsActivity.mTipoRetorno;
import mei.tcd.smta.ins.InsListener.OnInsChanged;
import mei.tcd.smta.util.SensorWriterSmta;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.GpsSatellite;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.support.v4.app.FragmentActivity;
import com.google.android.gms.maps.SupportMapFragment;

public class InsGpsLogger extends Activity  implements OnInsChanged,InterfaceGps{
	//private Coordinates coordenadas;
	private InsListener insListener;
	private ProgressDialog dialog;
	private Button startBtn;
	private Button stopBtn;
	private Button recBtn;
	private Button nextBtn, previousBtn;
	private ViewSwitcher mViewSwitcher;
	private boolean mComecou = false;//Começou o sistema
	private boolean mStartRec = false;; //Começou a gravação
	private boolean mComecouIns = false;
	private boolean mComecouGps = false;
	private boolean mLogGps = false;
	private boolean mLogIns = false;
	private SharedPreferences prefs;// Gestor de preferencias
	//private View mapaView;
	private static TextView gpsInformacao, gpsSatelites, gpsFix;
	// Mapa
	// Google Maps V2
	private GoogleMap mapView;
	private boolean initialZoomSet;
	// Instancia do listener 
	private Gps gpsListener;
	//private TrajectoOverlay trajectoOverlay;
	// inicialização GPS
	double latE6,  longE6, altE6; // localização actual
	double latE6_,  longE6_, altE6_; // localização anterior
	double[] wgsOrigin = new double[3];
	double[] ecefOrigin = new double[3];
	double velocidadeGps = 0;
	double latE63 = 0, longE63 = 0;
	float[] mPosicaoAnterior = new float[3];
	private boolean mostramsg = true;
	Circle circle ;
	//DEBUG
	private SensorWriterSmta gps = new SensorWriterSmta();
	private float acuracia;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		//		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		//				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.gpslogger);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//	    StrictMode.setThreadPolicy(policy);

		// Progress dialog para inicializar e certificar que possuo uma orientação definida
		dialog = new ProgressDialog(this);
		// Mapa google
		// referencia da instancia do mapa google maps agora fragment v2
		setUpMapIfNeeded();
		//mapView = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapview1))
		//        .getMap();
		//mapView = (MapView) findViewById(R.id.mapview1);
		startBtn = (Button) findViewById(R.id.start);
		stopBtn = (Button) findViewById(R.id.stop);
		nextBtn = (Button)  findViewById(R.id.maisinfo);
		nextBtn = (Button)  findViewById(R.id.paraTras);
		recBtn = (Button)  findViewById(R.id.rec);
		// +Inforação
		gpsInformacao = (TextView) findViewById(R.id.gpsInformacao);
		gpsSatelites = (TextView) findViewById(R.id.gpsSatelites);
		gpsFix = (TextView) findViewById(R.id.gpsFix);
		//Viewswitcher para slider
		mViewSwitcher = (ViewSwitcher)findViewById(R.id.viewSwitcher1);
		// Drawable do oberlayItem.
		Drawable inicioDrawable = getResources().getDrawable(R.drawable.dot_icon);
		// Um overlay é uma lista de Items de  overlay . Parametros é o drawable (balão) e o mapa.
		//trajectoOverlay = new TrajectoOverlay(inicioDrawable, mapView);
		// Acede à lista de overlays, neste caso adiciona a nossa trajectoOverlay.
		//mapView.getOverlays().add(trajectoOverlay);
		// Instancia do meu listener gps
		gpsListener = new Gps(getApplicationContext(),this);
		latE6=0;longE6=0; altE6=0; 
		latE6_=0;longE6_=0; altE6_=0; 
		prefs = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext()); //instancio as preferencias de modo static , sem new()
	}
	private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mapView == null) {
            // Try to obtain the map from the SupportMapFragment.
        	mapView = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapview1))
                    .getMap();
            // Verificar se foi obtido um mapa com sucesso.
//            if (mapView != null) {
//                setUpMap();
//            }
        }
    }
//	 private void setUpMap() {
//		 mapView.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
//	    }
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
	// Começa a acção. Evento definido no XML
	public void onStartClick(View view)
	{

		if (!mComecou)
		{
			startBtn.setEnabled(false);
			stopBtn.setEnabled(true);
			recBtn.setEnabled(false);
			dialog.setCancelable(false);
			dialog.setMessage("A inicializar / calibrar o GPS e sensores do Ins...!");
			dialog.show();
			//mostramsg = true;
			if(mLogGps)
			{
				
				gps.criaFicheiro("smta_isep","gps");
				gpsListener.startGps();
			}
			if(mLogIns)
			{
				insListener.setRecord(true);
				insListener.start();
			}
			mComecou = true;
		}
	}

	public void onStopClick(View view)
	{
		if (mComecou)
		{
			insListener.stop();
			gpsListener.stopGps();
			mComecou = false;
			mStartRec = false;
			mostramsg = false;
			mStartRec = false;
			// altero estado dos botões
			startBtn.setEnabled(true);
			stopBtn.setEnabled(false);
			recBtn.setEnabled(false);

		}
	}
	public void onRecClick(View view)
	{

		if (mStartRec)
		{
			mStartRec = false;
			insListener.setStartRec(mStartRec);
			recBtn.setText("Rec");
			stopBtn.setEnabled(true);
			
		}
		else 
		{
			mStartRec = true;
			insListener.setStartRec(mStartRec);
			recBtn.setText("StopRec");
			stopBtn.setEnabled(false);
		}
	}
	@Override
	public void onResume() {
		super.onResume();
		//Vou buscar as preferencia de log
		mLogGps = prefs.getBoolean("logGps", false);
		mLogIns = prefs.getBoolean("logSensor", false);
		// altero estado dos botões
		startBtn.setEnabled(!mComecou);
		stopBtn.setEnabled(mComecou);
		recBtn.setEnabled(mComecou);
		// Instancio o meu listener passando o contexto e de onde
		insListener = new InsListener(getApplicationContext(),this);
	}
	@Override
	public void onStop() {
		super.onStop();
		insListener.stop();
		gpsListener.stopGps();
		if(gps.ficheiro!=null)
			gps.fechaFicheiro();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		insListener.stop();
		gpsListener.stopGps();
		if(gps.ficheiro!=null)
			gps.fechaFicheiro();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(insListener!=null)
			insListener.stop();
		if(gpsListener!=null)
			gpsListener.stopGps();
		if(gps.ficheiro!=null)
			gps.fechaFicheiro();
	}
	//Obrigatoriedade Google para o servidor saber se o serviço esta a ser usado para tracking
//	@Override
//	protected boolean isRouteDisplayed()
//	{
//		return true;
//	}
//	//Obrigatoriedade Google para o servidor saber se o serviço esta a ser usado para localização
//	@Override
//	protected boolean isLocationDisplayed()
//	{
//		return true;
//	}
	/**
	 * Corro o callback definido no interface. Valido cada tipo de retorno ou acção despoletada de forma a actualizar as acções que necessito.
	 * Caso o retorno seja a posição, então efectuo as seguintes operações:
	 * 1- vou buscar a posição (em global coordinates)
	 * 2- transformo ECEF para WGS84
	 * 3- Adciono o ponto ao overlay
	 * 4- Calculo o int de cada wgs84 recebido de forma a mostrar no mapa (google obriga a INTs)
	 * 
	 * Se estiver a inicializar, então fecho a caixa de dialogo.
	 * 
	 * @param value tipo de retorno para comparativo (tipo evento)
	 */
	@Override
	public void onInsEvent(Enum value) {
		if(value.name()==mTipoRetorno.orientacao.name())
		{
			
		}
		if(value.name()==mTipoRetorno.velocidade.name())
		{
			
		}
		if(value.name()==mTipoRetorno.posicao.name())
		{
			
		}
		if(value.name()==mTipoRetorno.inicializar.name())
		{
			if(insListener.getInicio())
			{
				if(mLogIns && !mLogGps)
				{
					recBtn.setEnabled(true);
					dialog.dismiss();
				}
				mComecouIns = true;
				
				
				
			}
		}

	}
	/**
	 * Para calculo da posicao inicial do INS, deverei ir buscar uma coordenada ao GPS para me posicionar.
	 * Para isso vou buscar o objecto location (lat/long/alt)
	 * Passo para ECEF e envio para o INS definir a posicao inicial do meu INS.
	 * Altero a caixa de dialogo, faço start ao INS e perfomclick ao stopGPS.
	 */
	@Override
	public void onLocationChanged(Location location) {
		if(latE6!=0)
		{
			latE6_ = latE6;
			longE6_ = longE6;
			altE6_ = altE6;
		}
		acuracia = location.getAccuracy();
		latE6 = location.getLatitude();
		longE6 = location.getLongitude();
		altE6 = location.getAltitude();
		velocidadeGps = location.getSpeed();
		if(mStartRec)
		{
			
			gps.escreveIsto(location.getTime() + "," + latE6+ "," + longE6 + "," + altE6 + "," + location.getBearing()+ ","+location.getSpeed()+"\n");
			
		}
		if(latE6_!=0)
		{
			

		// Get back the mutable Circle
			if(circle==null)
			{
				CircleOptions circleOptions = new CircleOptions()
			    .center(new LatLng(latE6, longE6))
			    .radius(acuracia); // In meters
				circle = mapView.addCircle(circleOptions);
			}else{
				circle.setCenter(new LatLng(latE6, longE6));
				circle.setRadius(acuracia);
			}
				
			Polyline line = mapView.addPolyline(new PolylineOptions().add( 
	                new LatLng(latE6_, longE6_), new LatLng(latE6, longE6))
	                .width(5)
	                .color(Color.BLUE));
		}
		//trajectoOverlay.adicionaPonto(latE6,
		//		longE6);
//		Polyline line = mapView.addPolyline(new PolylineOptions().add( 
//                new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude, dest.longitude))
//                .width(2)
//                .color(Color.BLUE));
//		int latE62 = (int) (latE6 * 1E6);
//		int longE62 = (int) (longE6 * 1E6);
		LatLng coordinates = new LatLng(latE6, longE6);	
		//mapView.getController().animateTo(new GeoPoint(latE62, longE62));
		mapView.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 17));

		if (!initialZoomSet)
		{
			mapView.animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );    
			//mapView.getController().setZoom(17);
			initialZoomSet = true;
		}
		gpsInformacao.setText("Velocidade (Km/h): " +velocidadeGps*(18/5) );
		mComecouGps = true;
		if(mComecouGps && mComecouIns){
			recBtn.setEnabled(true);
			dialog.dismiss();
		}
		



	}
	@Override
	public void onGpsStatusChanged(int event) {
		// TODO Auto-generated method stub

	}
	@Override
	public void sateliteStatus(ArrayList<GpsSatellite> arraySatelites) {
		// TODO Auto-generated method stub

	}
	// ------------------------------------------------------------------------
	// Menu de opções
	// Configuração - Configuraçao das preferencias
	// Reset - Reset à velocidade
	// About - Espaço para colocar Acerca
	// ------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.smta_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.config) {
			Intent smtaPrefs = new Intent(this, SmtaPreferences.class);
			startActivity(smtaPrefs);
			return true;
		} else if (item.getItemId() == R.id.reset) {
			//ins.velocidade.zero();
			return true;
		} else if (item.getItemId() == R.id.calibrar) {
			Intent smtaCalib = new Intent(this, CalibracaoActivity.class);
			startActivity(smtaCalib);
			return true;
		} else if (item.getItemId() == R.id.about) {
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}

	}
}

