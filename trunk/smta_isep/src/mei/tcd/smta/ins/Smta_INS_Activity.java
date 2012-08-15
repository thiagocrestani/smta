/**
 * @author pessanha
 * 2012-08-11
 */
package mei.tcd.smta.ins;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import mei.tcd.smta.R;
import mei.tcd.smta.SmtaPreferences;
import mei.tcd.smta.util.Ops;
import mei.tcd.smta.util.SensorWriterSmta;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

public class Smta_INS_Activity extends Activity implements SensorEventListener {
	// NS2S - Para calcular segundos de nanosegundos (multiplicar este valor pelo tempo)
	// MSTOKM - Metros por segundo para KM/H
	// RAD2GRAUS - Auxiliar no calculo de Radianos para graus
	private static final float NS2S = 1.0f / 1000000000.0f;
	private static final float MSTOKM = 0.277777778f;
	private static final float RAD2GRAUS = 57.2957795f;
	// eventTime - Tempo em nanosegundos quando o evento aconteceu
	// anteriorEventTimeAcc, anteriorEventTimeGyro,anteriorEventTimeMag,anteriorEventTimeRotVet - Tempo em nanosegundos do evento anterior dos três sensores
	// contadorDes - Para ser usado em conjunto com o controlo de desaceleração
	private long eventTime = 0;
	private long anteriorEventTimeAcc = 0, anteriorEventTimeGyro = 0,anteriorEventTimeMag = 0, anteriorEventTimeRotVet = 0;
	private int contadorDes = 0;
	// Variaveis de preferencias
	// PV - Valor threshold para multiplicar pelas observações actuais e comparar com o calibrado
	// rateSensor - Guarda a velocidade do sensor. Eventos podem ser recebidos mais rapidamente ou não. Delay_Fastest = ~20ms
	// tipoOrientacao - 3 tipos de orientação seleccionaveis. Acc+Mag, RotVetor ou Acc+Mag+Gyr (filtro complementar)
	// semaforo - Envia sinalização para a view sobre se está a acelerar, abrandar ou parado.
	// controlDesacelera - Experimental para testar o controlo de desaceleração instatanea, logo após aceleração.
	// controlDesaceleraThreshold - Número de registos a serem descartados após primeira desaceleração. (Valores negativos)
	private float PV;
	private int rateSensor;
	private String tipoOrientacao;
	private int semaforo;
	private boolean controlDesacelera;
	private int controlDesaceleraThreshold = 3;
	private boolean inicioINS;
	private boolean efectuaCalibracao;


	// dadosAcc, dadosGyro,dadosMag,dadosRotVet - Dados dos sensores em arrays float

	float[] dadosAcc = new float[3]; // Acelerometro
	float[] dadosGyro = new float[3]; // Giroscopio
	float[] dadosMag = new float[3]; // Magnetometro
	float[] dadosMinhaAccLin = new float[3]; // Acelerometro linear retirando a gravidade com DCM
	float[] dadosRotVet = new float[3]; // Rotation Vector
	float[] calibVetor = new float[3]; // se usar processo de calibração em preferencia. A minha gravidade apontava 9.3 no máximo no eixo do Z.
	// sensorManager - Instancia do sensorManager
	// ins - Instancia classe INS
	// orientacaoView - Instancia da minha view para orientação
	// velocidadeView - Instancia da minha view para velocidade
	// resources - Aceder aos resource (Strings, etc...)
	// prefs - Gestor de preferencias
	private SensorManager sensorManager;
	private Smta_INS ins;
	OrientacaoView orientacaoView;
	VelocidadeView velocidadeView;
	CaminhoView caminhoView;
	Resources resources;
	SharedPreferences prefs;
	Ops operacoes;
	Handler startHandler = new Handler();  // Obriga a começar quando 10s
	Handler viewHandler = new Handler(); // refresca a view de x em x tempo
	ProgressDialog dialog;
	//vou colocar um timer para posicionar na view de x em x tempo
	private Timer viewTimer = new Timer();
	// ---------------------------------------- PARA DEBUG DE INFORMAÇAO -----------------------------------
	// referenciar as views
	TextView debugView;
	// Formatar numeros
	DecimalFormat graus;
	public SensorWriterSmta sensorWriter = new SensorWriterSmta();
	// ---------------------------------------- FIM DEBUG DE INFORMAÇAO -----------------------------------
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Coloca em fullScreen e deixa ecrã sempre ligado
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.ins);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// Referenciar o sensorManager
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);// Para
		ins = new Smta_INS();
		operacoes = new Ops();
		// instancio as preferencias de modo static
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// instancio as minha views
		orientacaoView = (OrientacaoView) this
		.findViewById(R.id.orientacaoView);
		velocidadeView = (VelocidadeView) this
		.findViewById(R.id.velocidadeView);
		caminhoView = (CaminhoView) this
		.findViewById(R.id.caminhoView);
		//debugView = (TextView) findViewById(R.id.debug);
		// Actualizo a orientação a zero
		actualizaOrientacao(new float[] { 0, 0, 0 });
		// Vou buscar as dimensões do ecra e redimensiona as views
		Display display = getWindowManager().getDefaultDisplay(); 
		int width = display.getWidth();  // método deprecated
		//int height = display.getHeight();  // método deprecated
		orientacaoView.getLayoutParams().width = width/2-20;
		velocidadeView.getLayoutParams().width = width/2-20;
		// DEBUG
		//		sensorWriter.criaFicheiro("smta_dados_isep");
		//		sensorWriter.escreveIsto("Azimuth | Pitch | Roll | VelocidadeX | VelocidadeY | VelocidadeZ | VelocidadeToral | PosicaoX | PosicaoY \n");
		dialog = new ProgressDialog(this);

	}
	@Override
	public void onResume() {
		super.onResume();
		// Actualiza sensores e preferencias sempre que em pause
		actualizaSensores();
		// Certificar que não alinhou sempre que entra em pause
		ins.jaAlinhou = false;
		dialog.setCancelable(false);

		dialog.setMessage("A inicializar...");

		dialog.show();
	}

	/**
	 * Actualizo a view de acordo com os parametros Azimuth, Pitch e Roll passados em array
	 * @param apr array Azimuth, pitch e roll
	 */
	private void actualizaOrientacao(float[] apr) {
		float[] copiaApr = new float[3];
		copiaApr = apr.clone();
		// Para passar em graus
		copiaApr[0] *= RAD2GRAUS;
		copiaApr[1] *= RAD2GRAUS;
		copiaApr[2] *= RAD2GRAUS;
		if (orientacaoView != null) {
			orientacaoView.setAzimuth(copiaApr[0]);
			orientacaoView.setPitch(copiaApr[1]);
			orientacaoView.setRoll(copiaApr[2]);
			// Obriga a chamar o onDraw
			orientacaoView.invalidate();
		}

	}
	/**
	 * Actualizo a view velocidade (velocimetro) de acordo com os parametros enviados. 
	 * @param vel A velocidade do objecto
	 * @param semaforo Semaforo que indica se vai a acelarar, abrandar ou parado.
	 */
	private void actualizaVelocidadeView(float vel, int semaforo) {
		if (velocidadeView != null) {
			velocidadeView.setVelocidade(vel);
			velocidadeView.setSemaforo(semaforo);
			velocidadeView.invalidate();
		}
	}
	/**
	 * Actualizo a view velocidade (velocimetro) de acordo com os parametros enviados. 
	 * @param vel A velocidade do objecto
	 * @param semaforo Semaforo que indica se vai a acelarar, abrandar ou parado.
	 */
	private void actualizaPosicaoView(float[] pos) {
		if (caminhoView != null) {
			caminhoView.setPosicao(pos);

			caminhoView.invalidate();
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		int tipoEvento = event.sensor.getType();// guarda o tipo de sensor que gerou o evento
		eventTime = event.timestamp; // Guarda o tempo em nanosegundos quando o evento aconteceu
		float dt = 0; // Intervalo de tempo entre o evento actual e o anterior (posterior passagem para segundos)
		switch (tipoEvento) {
		case Sensor.TYPE_ACCELEROMETER:
			dadosAcc[0] = event.values[0] + calibVetor[0];
			dadosAcc[1] = event.values[1] + calibVetor[1];
			dadosAcc[2] = event.values[2] + calibVetor[2];
			if (anteriorEventTimeAcc != 0)
				dt = (eventTime - anteriorEventTimeAcc) * NS2S;
			anteriorEventTimeAcc = eventTime;
			break;
		case Sensor.TYPE_GYROSCOPE:
			dadosGyro[0] = event.values[0];
			dadosGyro[1] = event.values[1];
			dadosGyro[2] = event.values[2];
			if (anteriorEventTimeGyro != 0)
				dt = (eventTime - anteriorEventTimeGyro) * NS2S;
			anteriorEventTimeGyro = eventTime;
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			dadosMag[0] = event.values[0];
			dadosMag[1] = event.values[1];
			dadosMag[2] = event.values[2];
			if (anteriorEventTimeMag != 0)
				dt = (eventTime - anteriorEventTimeMag) * NS2S;
			anteriorEventTimeMag = eventTime;
			break;
		case Sensor.TYPE_ROTATION_VECTOR:
			dadosRotVet[0] = event.values[0];
			dadosRotVet[1] = event.values[1];
			dadosRotVet[2] = event.values[2];
			if (anteriorEventTimeRotVet != 0)
				dt = (eventTime - anteriorEventTimeRotVet) * NS2S;
			anteriorEventTimeRotVet = eventTime;
			break;
		}
		// ----------------------------------- Velocidade -----------------------------------------
		// Temos de certificar-nos que o dispositivo apanhou uma orientação +- estavel para retirar a gravidade da aceleração
		// Podemos mais tarde colocar um timer
		if(operacoes.getMagnitude(ins.aprAccMag)!=0 || operacoes.getMagnitude(ins.aprFusao)!=0 || operacoes.getMagnitude(ins.aprRotVet)!=0 && inicioINS)
		{
			if (tipoEvento == Sensor.TYPE_ACCELEROMETER) {
				// Para a frente +
				// Para tras -
				// Calculo a minha velocidade linear baseada na orientação e acelerometro
				dadosMinhaAccLin = ins.getAccLinear(dadosAcc);
				// Calculo a minha média de amostras iniciais para usar em conjunto
				// com o valor de threshold
				ins.stopDetection.adicionaAcc(dadosMinhaAccLin);
				// Calcular apenas para Y
				if (!ins.jaCalculouThreshold ) {
					// TODO: Mostrar mensagem progress bar a avisar a calibrar

				} else {
					if (tipoEvento == Sensor.TYPE_ACCELEROMETER) {
						// dadosMinhaAccLin = ins.getAccLinear(dadosAcc); 
						//Apenas para Y
						if ((dadosMinhaAccLin[1]) > (PV + ins.stopDetection.thresholdAccelY)) {
							semaforo = 3;// a acelerar
							ins.actualiza_Velocidade(dadosMinhaAccLin, dt);// Actualiza a velocidade com aceleração linear
							contadorDes = 0;
							// TODO: Corrigir o facto de depois da aceleração feita,
							// este desacelerar bruscamente.
						} else if ((dadosMinhaAccLin[1]) < (ins.stopDetection.thresholdAccelY - PV)
								&& controlDesacelera) {
							if (contadorDes > controlDesaceleraThreshold) {
								semaforo = 2;// a abrandar
								ins.actualiza_Velocidade(dadosMinhaAccLin, dt);
								//ins.actualiza_Posicao(dt);
							}
							contadorDes++;
						} else {
							semaforo = 1;// Parado
						}
						// Actualizo a minha posição independentemente de estar ou não a ganhar velocidade
						ins.actualiza_Posicao(dt); // Actualiza a posicao
						actualizaVelocidadeView((float) ins.getVelocidadeTotal() / MSTOKM,	semaforo);
						// esta é para tirar
						// ins.actualiza_Velocidade(dadosMinhaAccLin, dt);
						// Se a velocidade for menor que zero, então mostra zero
						//						if (ins.velocidade.get(1) < 0)
						//							ins.velocidade.set(1, 0);
						//------------------ DEBUG ------------------------------------------
						//						debugView.setText("Km/h"
						//								+ operacoes.getMagnitude(ins.getVelocidade_N()) / MSTOKM);
						//Log.d("acc",Double.toString(ins.velocidade.get(1)) +"   "+ Double.toString(ins.getVelocidadeTotal()));
						//					Log.d("Acc",
						//							ins.aceleracao.get(0) + "   "
						//							+ ins.aceleracao.get(1) + "    "
						//							+ ins.aceleracao.get(2));

					}
				}
			}
		}

		// ----------------------------------- Orientação -----------------------------------------
		if (tipoOrientacao.equals(resources.getString(R.string.AccMag))) {
			ins.calculaAccMagOrientacao(dadosMag, dadosAcc);
			actualizaOrientacao(ins.aprAccMag);
		} else if (tipoOrientacao.equals(resources
				.getString(R.string.RotationVector))) {
			ins.calculaRotVetOrientacao(dadosRotVet);
			actualizaOrientacao(ins.aprRotVet);
		} else if (tipoOrientacao.equals(resources.getString(R.string.Fusao))) {
			if (tipoEvento == Sensor.TYPE_ACCELEROMETER) {
				ins.calculaAccMagOrientacao(dadosMag, dadosAcc);
				// ins.calculaFusao();
			}
			if (tipoEvento == Sensor.TYPE_GYROSCOPE) {
				ins.calculaFusaoOrientacao(dadosGyro, dt);
			}
			actualizaOrientacao(ins.aprFusao);
		}
		// RotVet 0-Azimuth, 1- Pitch, 2- Roll
		//		sensorWriter.escreveIsto(dadosRotVet[0] + "|" +dadosRotVet[1] + "|" +dadosRotVet[2] + "|" +
		//				ins.velocidade.get(0) + "|" +ins.velocidade.get(1) + "|" +ins.velocidade.get(2) + "|" + 
		//				(float) operacoes.getMagnitude(ins.getDcmFloat3x1(ins.velocidade)) + "|" + 
		//				(float) operacoes.getMagnitude(ins.getDcmFloat3x1(ins.velocidade))*Math.cos(dadosRotVet[0]) +"|" + (float) operacoes.getMagnitude(ins.getDcmFloat3x1(ins.velocidade))*Math.sin(dadosRotVet[0])  + "\n"); 
	}


	/**
	 * Determinar quais os sensores que deverão estar activos. Respeitando o ciclo de vida de uma activadade no android, 
	 * corre sempre que o estado da actividade entra em onResume()
	 */
	private void actualizaSensores() {
		// Tenho de ir buscar os resources, caso contrario devolve nullpointer
		resources = getResources();
		// Limpa registers anteriores, apesar de a mudança ser efectuada na
		// configuração e esta ser renovada no onPause e onResume
		inicioINS = false;
		sensorManager.unregisterListener(this);
		// Vai buscar as preferencioas
		rateSensor = Integer.parseInt(prefs.getString("rate", "0"));
		tipoOrientacao = prefs.getString("orientacao", "rotvet");
		PV = Float.parseFloat(prefs.getString("thresholdArranque", "0.4f"));
		controlDesacelera = prefs.getBoolean("controlaParagem", false);
		controlDesaceleraThreshold = Integer.parseInt(prefs.getString(
				"controlaParagemContador", "3"));
		efectuaCalibracao = prefs.getBoolean("efectuaCalibracao", false);
		// Se efectua calibração, então carrego o vetor calibração dos valores calibrados para aproximação
		if(efectuaCalibracao && prefs.contains("AccCalibX"))
		{

			calibVetor[0] = prefs.getFloat("AccCalibX", 0.0f);
			calibVetor[1] = prefs.getFloat("AccCalibY", 0.0f);
			calibVetor[2] = prefs.getFloat("AccCalibZ", 0.0f);

		}
		// O acelerometro fica sempre activo
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				rateSensor);
		if (tipoOrientacao.equals(resources.getString(R.string.AccMag))) {
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
					rateSensor);
		} else if (tipoOrientacao.equals(resources
				.getString(R.string.RotationVector))) {
			sensorManager
			.registerListener(this, sensorManager
					.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
					rateSensor);
		} else if (tipoOrientacao.equals(resources.getString(R.string.Fusao))) {
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
					rateSensor);
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
					rateSensor);
		}
		// So liberto o ecrã depois de 10 s
		startHandler.postDelayed(new Runnable() 
		{ 
			public void run() { 
				inicioINS =true;
				dialog.dismiss();
				
			} 
		}, 10000);
		// Actualizo a view de posição
		TimerTask timerTask = new TimerTask() {		
			@Override
			public void run() {
				viewHandler.post(new Runnable() 
				{ 
					public void run() { 
						actualizaPosicaoView(ins.getXYView());
						}		
				});
			}
		}; 
		viewTimer.schedule(timerTask, 10000,500);
		

	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStop() {
		super.onStop();
		sensorManager.unregisterListener(this);
		//sensorWriter.fechaFicheiro();
		inicioINS =false;

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		sensorManager.unregisterListener(this);
		//sensorWriter.fechaFicheiro();
		inicioINS =false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Tenho sempre de efetuar o unRegister para prevenir o gasto da bateria
		// device's battery.
		sensorManager.unregisterListener(this);
		//sensorWriter.fechaFicheiro();
		inicioINS =false;
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
			ins.velocidade.zero();
			return true;
		case R.id.calibrar:
			Intent smtaCalib = new Intent(this, CalibracaoActivity.class);
			startActivity(smtaCalib);
			return true;

		case R.id.about:

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

}
