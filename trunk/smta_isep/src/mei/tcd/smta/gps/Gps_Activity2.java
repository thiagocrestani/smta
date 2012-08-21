package mei.tcd.smta.gps;

import java.util.Iterator;

import mei.tcd.smta.R;

import com.google.android.maps.MapActivity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationManager;
import android.location.GpsStatus; // Para verificar se temos GPs ou não
import android.os.Bundle;
import android.text.Layout;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
public class Gps_Activity2 extends MapActivity implements GpsStatus.Listener{
	
		
		private static final int REQUEST_CODE = 0;
		// Para o getAction do intent. A acção personalizada deve estar num namespace
		private static final String ACCAO_GPS =	"mei.tcd.smta.gps.GPSIN";
		// locationManager - Classe que providencia acesso aos serviços de localização
		// mapView - A view que disopnibiliza o mapa
		// trajectoOverlay - Classe que extende a classe ItemizedOverlay que é uma lista de items a serem colocados em overlay do mapa
		// pendingIntent - Descreve um intent e uma acção. O objecto retornado é entregue a aplicações para que possam tomar as acções que o mesmo argumenta mais tarde.
		private LocationManager locationManager;
		private MapView mapView;
		private TrajectoOverlay trajectoOverlay;
		private PendingIntent pendingIntent;
		private Button startBtn;
		private Button stopBtn;
		private Button nextBtn, previousBtn;
		private ViewSwitcher mViewSwitcher;
		//private View mapaView;
		private static TextView gpsInformacao;
		private boolean mComecou;
		
		//  private PointDatabaseManager pointDatabaseManager;
		private ActualizaMapaBroadcastReceiver actualizaMapaBroadcasReceiver;

		@Override
		protected void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			setContentView(R.layout.gpsinfo);
			// Retorna o handle de um serviço. A classe rerotnada  varia de acordo com o nome requisitado.
			locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			// Ok, adicionamos um listener, o proprio para verificar o estado do GPS impossivel de verificar via Broadcast
			locationManager.addGpsStatusListener(this);
			// referencia da instancia do mapa google maps
			mapView = (MapView) findViewById(R.id.mapview1);
			startBtn = (Button) findViewById(R.id.start);
			stopBtn = (Button) findViewById(R.id.stop);
			nextBtn = (Button)  findViewById(R.id.maisinfo);
			nextBtn = (Button)  findViewById(R.id.paraTras);
			gpsInformacao = (TextView) findViewById(R.id.gpsInformacao);
			mViewSwitcher = (ViewSwitcher)findViewById(R.id.viewSwitcher1);
			//mapaView = (View) findViewById(R.id.mapaView);
			// Habilitar controlos de zoom
			mapView.setBuiltInZoomControls(true);
			// Drawable do oberlayItem.
			Drawable inicioDrawable =
				getResources().getDrawable(R.drawable.dot_icon);
			// Um overlay é uma lista de Items de  overlay . Parametros é o drawable (balão) e o mapa.
			trajectoOverlay = new TrajectoOverlay(inicioDrawable, mapView);
			// Acede à lista de overlays, neste caso adiciona a nossa trajectoOverlay.
			mapView.getOverlays().add(trajectoOverlay);
			//Por defeito o modo de acesso as preferencias é 0 ou MODE_PRIVATE conforme em baixo na classe context.		
			// Preferencia para mais tarde
			//@see #MODE_WORLD_READABLE
			//@see #MODE_WORLD_WRITEABLE
			//preferences = getPreferences(0);
			// Instancia do broadcastReceiver de actualização de pontos no mapa.
			actualizaMapaBroadcasReceiver =	new ActualizaMapaBroadcastReceiver(trajectoOverlay, mapView);
//			Display display = getWindowManager().getDefaultDisplay(); 
//			int height = display.getHeight();  // método deprecated
//			mapaView.getLayoutParams().height = height-(height/4);
			
			
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
		// Do GpsStatusListener implementado para verificar ligação
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
                String stGps = "Satelites:";
                while (satI.hasNext()) {
                    GpsSatellite satellite = satI.next();
                    // Os satelites são identificados pelo receptor pelo seu PRN.
                   stGps +=  "\n" + satellite.getPrn() + "," + satellite.usedInFix() + "," + satellite.getSnr() + "," + satellite.getAzimuth() + "," + satellite.getElevation(); 
                    // http://en.wikipedia.org/wiki/Global_Positioning_System: the almanac consists of coarse orbit and status information for each satellite
                    // http://en.wikipedia.org/wiki/Ephemeris: the positions of astronomical objects in the sky at a given time
                    // + "," + satellite.hasAlmanac() + "," + satellite.hasEphemeris());
                   
                }
                gpsInformacao.setText(stGps);
			}
		}
		@Override
		protected void onResume()
		{
			super.onResume();
			// Registar um broadcast receiver para correr nesta thread. Se o broadcast for igual ao filtro, a actualizaMapaBroadcasReceiver vai ser chamado.
			registerReceiver(actualizaMapaBroadcasReceiver,
					new IntentFilter(ACCAO_GPS));

			startBtn.setEnabled(!mComecou);
			stopBtn.setEnabled(mComecou);

//			
		}

		@Override
		protected void onPause()
		{
			super.onPause();
			// unregister tal como nos listeners
			unregisterReceiver(actualizaMapaBroadcasReceiver);
			
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
				// Criar novo pendingIntent
				pendingIntent = createPendingIntent();
				// Aqui podemos definit criterios para usar um ou outro sensor de localização visto que o WIFI faz parte do mesmo LocationManager.
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_COARSE);
				// Dependendo do criterios, vai chamar os providers e efecruar pedidos de localização aos mesmos.
				// TODO: Retirar este processo pois vai haver apenas um provider
				for (String provider : locationManager.getProviders(criteria, true))
				{
					// Regista a aactividade para ser notificada periodicamente.O 0,0 quer dizer sempre que tiver.
					locationManager.requestLocationUpdates(provider,
							0,
							0,
							pendingIntent);
				}

				mComecou = true;
			}
		}

		public void onStopClick(View view)
		{
			if (mComecou)
			{
				if (pendingIntent == null)
				{
					pendingIntent = createPendingIntent();
				}
				// Remove qualquer registo para updates.
				locationManager.removeUpdates(pendingIntent);

				startBtn.setEnabled(true);
				stopBtn.setEnabled(false);

				mComecou = false;


			}
		}
		// M+etodo para criar o pending intent.
		private PendingIntent createPendingIntent()
		{
			Intent intent = new Intent(ACCAO_GPS);
			return PendingIntent.getBroadcast(getApplicationContext(),
					REQUEST_CODE,
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
		}

		private static class ActualizaMapaBroadcastReceiver
		extends GpsBroadcastReceiver
		{
			private TrajectoOverlay trajectoLocationOverlay;
			private MapView mapView;
			private boolean initialZoomSet;

			public ActualizaMapaBroadcastReceiver(TrajectoOverlay overlay,
					MapView mapView)
			{
				this.trajectoLocationOverlay = overlay;
				this.mapView = mapView;
			}
			 public void onLocationChanged(Context context, Location location) 
			 {
				 // Tenhoque adicionar os pontos para construir o path
				 trajectoLocationOverlay.adicionaPonto(location.getLatitude(),
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
			
		}

	}
