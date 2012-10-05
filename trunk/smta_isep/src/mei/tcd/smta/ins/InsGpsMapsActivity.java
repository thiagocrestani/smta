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
import mei.tcd.smta.gps.TrajectoOverlay;
import mei.tcd.smta.ins.InsActivity.mTipoRetorno;
import mei.tcd.smta.ins.InsListener.OnInsChanged;
import mei.tcd.smta.util.WGS84;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.location.GpsSatellite;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class InsGpsMapsActivity extends MapActivity implements OnInsChanged,InterfaceGps{
	//private Coordinates coordenadas;
	private InsListener insListener;
	private ProgressDialog dialog;
	private Button startBtn;
	private Button stopBtn;
	private Button nextBtn, previousBtn;
	private ViewSwitcher mViewSwitcher;
	private boolean mComecou;
	// Coordenadas
	private double[] posinicialEcef = new double[3];
	private double[] posinicialENU = new double[3];
	private float[] posEcef = new float[3];
	private double[] poswgs84 = new double[3];
	private float[] posEnu = new float[3];
	private static final float RAD2GRAUS = 57.2957795f;
	//private View mapaView;
	private static TextView gpsInformacao, gpsSatelites, gpsFix;
	// Mapa
	// Google Maps
	private MapView mapView;
	private boolean initialZoomSet;
	// Instancia do listener 
	private Gps gpsListener;
	private TrajectoOverlay trajectoOverlay;
	// inicialização GPS
	double latE6,  longE6, altE6; 
	double[] wgsOrigin = new double[3];
	double[] ecefOrigin = new double[3];
	double latE63 = 0, longE63 = 0;
	float[] mPosicaoAnterior = new float[3];
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		//		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		//				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.gpsinfo);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Progress dialog para inicializar e certificar que possuo uma orientação definida
		dialog = new ProgressDialog(this);
		// Mapa google
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
		// Drawable do oberlayItem.
		Drawable inicioDrawable = getResources().getDrawable(R.drawable.dot_icon);
		// Um overlay é uma lista de Items de  overlay . Parametros é o drawable (balão) e o mapa.
		trajectoOverlay = new TrajectoOverlay(inicioDrawable, mapView);
		// Acede à lista de overlays, neste caso adiciona a nossa trajectoOverlay.
		mapView.getOverlays().add(trajectoOverlay);
		// Instancia do meu listener gps
		gpsListener = new Gps(getApplicationContext(),this);
		latE6=0;longE6=0; altE6=0; 
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
	// Começa a acção. Evento definido no XML
	public void onStartClick(View view)
	{

		if (!mComecou)
		{
			
			startBtn.setEnabled(false);
			stopBtn.setEnabled(true);
			
			gpsListener.startGps();

			dialog.setCancelable(false);
			dialog.setMessage("A inicializar...a tentar obter localização do GPS!");
			dialog.show();

			mComecou = true;
		}
	}

	public void onStopClick(View view)
	{
		if (mComecou)
		{
			//insListener.stop();
			gpsListener.stopGps();
			insListener.stop();
			mComecou = false;
			// altero estado dos botões
			startBtn.setEnabled(true);
			stopBtn.setEnabled(false);

		}
	}
	@Override
	public void onResume() {
		super.onResume();
		// altero estado dos botões
		startBtn.setEnabled(!mComecou);
		stopBtn.setEnabled(mComecou);
		// Instancio o meu listener passando o contexto e de onde
		insListener = new InsListener(getApplicationContext(),this);
	}
	@Override
	public void onStop() {
		super.onStop();
		insListener.stop();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		insListener.stop();
	}

	@Override
	protected void onPause() {
		super.onPause();
		insListener.stop();
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
			//actualizaOrientacao(insListener.getAziPitRoll());
		}
		if(value.name()==mTipoRetorno.velocidade.name())
		{
			//actualizaVelocidadeView(insListener.getVelocidade(),insListener.getSemaforo());
		}
		if(value.name()==mTipoRetorno.posicao.name())
		{
			float[] posActual;
			double[] novaCoordenada = new double[2];
			posActual = insListener.getPosicao();


			if(WGS84.getLastDistance(mPosicaoAnterior,posActual)>0)
			{

				novaCoordenada = WGS84.FindPointAtDistanceFrom(latE6, longE6,insListener.getAziPitRoll()[0], WGS84.getLastDistance(mPosicaoAnterior,posActual));
				poswgs84[0] = novaCoordenada[0];
				poswgs84[1] = novaCoordenada[1];
				poswgs84[2] = altE6;
				latE6 = (Math.toDegrees(poswgs84[0]));
				longE6 = (Math.toDegrees(poswgs84[1]));


			}
			//Actualizo o mapa
			trajectoOverlay.adicionaPonto(latE6,
					longE6);
			//Log.d("INS LatLong:","Latitude: " + poswgs84[0] + "Longitude: " + poswgs84[1]);

//			gpsInformacao.setText("\nLatitude InsGps: " + posEcef[0] + "\nLongitude: " + posEcef[1]);
//			gpsInformacao.append("\nLatitude wgs84: " + poswgs84[0]+ "\nLongitude: " + poswgs84[1]);
			gpsInformacao.append("\nVelocidade: " +insListener.getVelocidade() );

			int latE62 = (int) (latE6 * 1E6);
			int longE62 = (int) (longE6 * 1E6);
			mapView.getController().animateTo(new GeoPoint(latE62, longE62));

			if (!initialZoomSet)
			{
				mapView.getController().setZoom(17);
				initialZoomSet = true;
			}
			mPosicaoAnterior = posActual;

		}
		if(value.name()==mTipoRetorno.inicializar.name())
		{
			if(insListener.getInicio())
			{
				dialog.dismiss();
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
		// Localização inicial para o posicionamento INS
//		double bearing = 0;

		latE6 =  (location.getLatitude() );
		longE6 =  (location.getLongitude());
		altE6 =  (location.getAltitude());
		

		posinicialENU[0] = 0;posinicialENU[1] = 0;posinicialENU[2] = 0;
//		Log.d("ENU Inicial:","East: " + posinicialEcef[0] + " North: " + posinicialEcef[1] + " Up: " + posinicialEcef[2] );
//		Log.d("WGS Inicial:","Latitude: " + wgsOrigin[0] + " Longitude: " + wgsOrigin[1] + " Altura: " + wgsOrigin[2] );
		insListener.setPosicaoInicial(posinicialEcef);
		dialog.dismiss();
		dialog.setCancelable(false);
		dialog.setMessage("Localização GPS obtida. A inicializar o INS...");
		// passo a posicao inicial para o INS
//		latE63 =wgsOrigin[0] ;
//		longE63 =wgsOrigin[1];
		dialog.show();
		insListener.start();

		stopBtn.performClick();
		
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
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.config:
			Intent smtaPrefs = new Intent(this, SmtaPreferences.class);
			startActivity(smtaPrefs);
			return true;
		case R.id.reset:
			//ins.velocidade.zero();
			return true;
	/*	case R.id.calibrar:
			Intent smtaCalib = new Intent(this, CalibracaoActivity.class);
			startActivity(smtaCalib);
			return true;*/

		case R.id.about:

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}
}
