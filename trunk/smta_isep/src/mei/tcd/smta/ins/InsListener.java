package mei.tcd.smta.ins;

import java.util.Timer;
import java.util.TimerTask;

import mei.tcd.smta.R;
import mei.tcd.smta.util.Ops;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.preference.PreferenceManager;

public class InsListener implements SensorEventListener {
	// Tipo de retorno do callback implementado
	public enum tipoRetorno {inicializar,orientacao, velocidade, posicao};
	// NS2S - Para calcular segundos de nanosegundos (multiplicar este valor pelo tempo)
	// MSTOKM - Metros por segundo para KM/H 
	private static final float NS2S = 1.0f / 1000000000.0f;
	private static final float MSTOKM = 0.277777778f;
	// sensorManager - Instancia do sensorManager
	// prefs - Gestor de preferencias
	// resources - Aceder aos resource (Strings, etc...)
	// inicioINS - Vai devolver se o INS já começõu ou não
	// rateSensor - Frequencia dos sensores definida nas preferencias.
	// tipoOrientação - Tipo orientação definida nas preferencias
	// PV - threshold para detecção de movimento
	// controlDesacelera - Experimental para testar o controlo de desaceleração instatanea, logo após aceleração.
	// controlDesaceleraThreshold - Número de registos a serem descartados após primeira desaceleração. (Valores negativos)
	// efectuaCalibracao - se a calibração efectuada no sensor de aceleração é para ser usada ou não (preferecias)
	// Semaforo - devolve um valor a sinalizar se está em aceleração, parado ou a desacelerar
	private SensorManager sensorManager = null;
	private SharedPreferences prefs;
	private Resources resources;
	private boolean inicioINS;
	private int rateSensor;
	private String tipoOrientacao;
	private float PV;
	private boolean controlDesacelera;
	private int controlDesaceleraThreshold = 3;
	private boolean efectuaCalibracao;
	private int semaforo;
	private float velocidade;
	// dadosAcc, dadosGyro,dadosMag,dadosRotVet - Dados dos sensores em arrays float
	private float[] calibVetor = new float[3]; 
	private float[] dadosAcc = new float[3]; // Acelerometro
	private float[] dadosGyro = new float[3]; // Giroscopio
	private float[] dadosMag = new float[3]; // Magnetometro
	private float[] dadosMinhaAccLin = new float[3]; // Acelerometro linear retirando a gravidade com DCM
	private float[] dadosRotVet = new float[3]; // Rotation Vector
	private float[] AziPitRol = new float[3]; // Azimuth pith e roll de retorno
	// eventTime - Tempo em nanosegundos quando o evento aconteceu
	// anteriorEventTimeAcc, anteriorEventTimeGyro,anteriorEventTimeMag,anteriorEventTimeRotVet - Tempo em nanosegundos do evento anterior dos três sensores
	// contadorDes - Para ser usado em conjunto com o controlo de desaceleração
	private long eventTime = 0;
	private long anteriorEventTimeAcc = 0, anteriorEventTimeGyro = 0,anteriorEventTimeMag = 0, anteriorEventTimeRotVet = 0;
	private int contadorDes = 0;
	// ins - Instancia classe INS
	// ops - Instancia da classe de operações genéricas
	private InsClass ins;
	private Ops operacoes;
	// Handlers 
	private Handler startHandler = new Handler();  // Obriga a começar quando 10s
	private Handler posicaoHandler = new Handler();  // Obriga a começar quando 10s
	//vou colocar um timer para posicionar na view de x em x tempo
	private Timer posicaoTimer = new Timer();
	// Vai sr notificado quando detectado
	private OnInsChanged<tipoRetorno> listener;
	// Variaveis de reotrnos
		
	// Getters e setters
	public float getVelocidade(){
		return velocidade;
		
	}
	public void setVelocidadeZero(){
		velocidade = 0.0f;
		
	}
	public int getSemaforo(){
		return semaforo;
		
	}
	public float[] getAziPitRoll(){
		return AziPitRol;
		
	}
	private void setAziPitRoll(float[] apr){
		AziPitRol = apr;
		listener.onInsEvent(tipoRetorno.orientacao);
		
	}
	public boolean getInicio()
	{
		return inicioINS;
	}
	private void setInicio(boolean _inicio)
	{
		inicioINS = _inicio;
	}
	public float[] getPosicao()
	{
		return ins.getXYView();
	}
	// Método contrutor
	public InsListener(Context context,OnInsChanged onInsChanged){
		this.velocidade = 0.0f;
		// Tenho de ir buscar os resources, caso contrario devolve nullpointer
		resources = context.getResources();
		// Referenciar o sensorManager
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);// Para
		ins = new InsClass();
		operacoes = new Ops();
		// instancio as preferencias de modo static
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		listener = onInsChanged;
		
	}
	// Métodos para começar e parar
	public void start() {
		actualizaSensores();
		
    }
	public void stop(){
		sensorManager.unregisterListener(this);
		//sensorWriter.fechaFicheiro();
		this.setInicio(false);
		
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
						velocidade = (float) ins.getVelocidadeTotal() / MSTOKM;
						listener.onInsEvent(tipoRetorno.velocidade);
						

					}
				}
			}
		}

		// ----------------------------------- Orientação -----------------------------------------
		if (tipoOrientacao.equals(resources.getString(R.string.AccMag))) {
			ins.calculaAccMagOrientacao(dadosMag, dadosAcc);
			this.setAziPitRoll(ins.aprAccMag);
			
		} else if (tipoOrientacao.equals(resources
				.getString(R.string.RotationVector))) {
			ins.calculaRotVetOrientacao(dadosRotVet);
			this.setAziPitRoll(ins.aprRotVet);
			
		} else if (tipoOrientacao.equals(resources.getString(R.string.Fusao))) {
			if (tipoEvento == Sensor.TYPE_ACCELEROMETER) {
				ins.calculaAccMagOrientacao(dadosMag, dadosAcc);
			
			}
			if (tipoEvento == Sensor.TYPE_GYROSCOPE) {
				ins.calculaFusaoOrientacao(dadosGyro, dt);
			}
			this.setAziPitRoll(ins.aprFusao);
			AziPitRol = ins.aprFusao;
			
		}
		

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	
	/**
	 * Determinar quais os sensores que deverão estar activos. Respeitando o ciclo de vida de uma activadade no android, 
	 * corre sempre que o estado da actividade entra em onResume()
	 */
	private void actualizaSensores() {
		
		// Limpa registers anteriores, apesar de a mudança ser efectuada na
		// configuração e esta ser renovada no onPause e onResume
		this.setInicio(false);
		//sensorManager.unregisterListener(this);
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
		// informo que está a inicializar
		listener.onInsEvent(tipoRetorno.inicializar);
		// So liberto o ecrã depois de 10 s
		startHandler.postDelayed(new Runnable() 
		{ 
			public void run() { 
				setInicio(true);
				// Para verificar que está pronto
				listener.onInsEvent(tipoRetorno.inicializar);
				
			} 
		}, 10000);
		// Actualizo a view de posição
		TimerTask timerTask = new TimerTask() {		
			@Override
			public void run() {
				posicaoHandler.post(new Runnable() 
				{ 
					public void run() { 
						listener.onInsEvent(tipoRetorno.posicao);
						}		
				});
			}
		}; 
		posicaoTimer.schedule(timerTask, 10000,500);
	}
	// Mando apenas as que interessam
	public interface OnInsChanged<E extends Enum<E>> {
        public void onInsEvent(E value);
    }
}
