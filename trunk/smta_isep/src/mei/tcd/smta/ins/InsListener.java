package mei.tcd.smta.ins;

import java.util.Timer;
import java.util.TimerTask;

import mei.tcd.smta.R;
import mei.tcd.smta.util.Ops;
import mei.tcd.smta.util.SensorWriterSmta;
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
	public enum tipoRetorno {inicializar,orientacao, velocidade, posicao};// Tipo de retorno do callback implementado
	private static final float NS2S = 1.0f / 1000000000.0f; //Para calcular segundos de nanosegundos (multiplicar este valor pelo tempo)
	private static final float MSTOKM = 0.277777778f; //MSTOKM - Metros por segundo para KM/H 
	private SensorManager sensorManager = null;// sensorManager - Instancia do sensorManager
	private SharedPreferences prefs;// Gestor de preferencias
	private Resources resources;// Aceder aos resource (Strings, etc...)
	private boolean mInicioIns;// Vai informar se o INS j� come��u ou n�o
	//Preferencias
	private int mRateSensor;// Frequencia dos sensores definida nas preferencias e sugeridas nos listeners dos sensores.
	private String mTipoOrientacao;// Tipo orienta��o definida nas preferencias e vai definir se Acc+Mag ou RotVet ou Acc+Mag+Gyro Fus�o
	private float mCoeficienteThreshold;// Coeficiente a somar ao threshold calculado para detec��o de movimento. Pode ser definido em prefs.
	private boolean mControlDesacelera;// Experimental para testar o controlo de desacelera��o instatanea, logo ap�s acelera��o. Tem tambem de se definir um n�mero de registos posteriores � desacelera��o.
	private int mControlDesaceleraThreshold = 3; //N�mero de registos a serem descartados ap�s primeira desacelera��o. (Valores negativos)
	private boolean mEfectuaCalibracao;// se a calibra��o efectuada no sensor de acelera��o � para ser usada ou n�o (preferecias)
	private int filtro; //1- High pass, 2- Low pass, 3- Nenhum
	private float filtroAlpha;
	//---------------------------
	private int mSemaforo; // devolve um valor a sinalizar se est� em acelera��o, parado ou a desacelerar
	private float mVelocidade; // Velocidade em Km ((float) ins.getVelocidadeTotal() / MSTOKM;) 
	// dadosAcc, dadosGyro,dadosMag,dadosRotVet - Dados dos sensores em arrays float
	private float[] mCalibVetor = new float[3]; // Vai guardar os valores obtidos da calibra��o do acelerometro e usa-los na obten��o de valores Acc melhorados.
	private float[] mDadosAcc = new float[3]; // Vetor acelera��o obtida do Acelerometro nos 3 eixos (X, Y e Z)
	private float[] mDadosAccFiltro = new float[3]; // Vetor acelera��o obtida do Acelerometro nos 3 eixos (X, Y e Z)
	private float[] mDadosAccLinearFiltro = new float[3]; // Vetor acelera��o obtida do Acelerometro nos 3 eixos (X, Y e Z)
	private float[] mDadosGyro = new float[3]; // Vetor rota�ao obtida do Giroscopio  nos 3 eixos (X, Y e Z)
	private float[] mDadosMag = new float[3]; // Vetor Micro Teslas obtida do magnetometro  nos 3 eixos (X, Y e Z)
	private float[] mDadosMinhaAccLin = new float[3]; // Acelerometro linear retirando a gravidade com DCM na classe InsClass
	private float[] mDadosLinear = new float[3]; // Acelerometro linear do android
	private float[] mDadosMinhaAccLinFiltro = new float[3]; // Acelerometro linear retirando a gravidade com DCM na classe InsClass
	private float[] mDadosRotVet = new float[3]; // Vetor rota��o obtido do sensor virtual Rotation Vector.
	private float[] mAziPitRol = new float[3]; // Azimuth pith e roll de retorno
	private float[] globalCoords = new float[3]; //Vetor temporario para coordenadas globais
	private long eventTime = 0;// eventTime - Tempo em nanosegundos quando o evento aconteceu
	private boolean mRecord,mStartRecord=false;
	// anteriorEventTimeAcc, anteriorEventTimeGyro,anteriorEventTimeMag,anteriorEventTimeRotVet - Tempo em nanosegundos do evento anterior dos tr�s sensores
	private long anteriorEventTimeAcc = 0, anteriorEventTimeGyro = 0,anteriorEventTimeMag = 0, anteriorEventTimeRotVet = 0;
	private int mContadorDes = 0;// contadorDes - Para ser usado em conjunto com o controlo de desacelera��o
	private InsClass ins;// ins - Instancia classe INS
	private Ops operacoes;// ops - Instancia da classe de opera��es gen�ricas
	// Handlers 
	// Com os handler podemos enviar e processar mensagens no message Queue (fila de mensagens) na thread
	private Handler startHandler = new Handler();  // Obriga a come�ar quando 10s para certificar que a orienta��o est� fixa e que o threshold da acelera��o foi calculada
	private Handler posicaoHandler = new Handler();  // Corre un runnable para enviar a posi��o de X em X tempo definido em posicaoTimer

	private Timer posicaoTimer = new Timer();//vou colocar um timer para posicionar na view de x em x tempo
	// Interface para comunicar com quem instancia e enviar os retornos quando prontos.
	private OnInsChanged<tipoRetorno> listener;
	// Debug----------------------
	private SensorWriterSmta acelerometro = new SensorWriterSmta();
	private SensorWriterSmta acelerometroGlobais = new SensorWriterSmta();// Acelera��o  coordenadas globais
	private SensorWriterSmta acelerometroFiltro = new SensorWriterSmta();
	private SensorWriterSmta AccMeuLinear = new SensorWriterSmta();
	private SensorWriterSmta AccMeuLinearGlobais = new SensorWriterSmta();// Acelera��o linear coordenadas globais
	private SensorWriterSmta AccMeuLinearFiltro = new SensorWriterSmta();
	private SensorWriterSmta AccLinear = new SensorWriterSmta();
	private SensorWriterSmta AccLinearGlobais = new SensorWriterSmta();
	private SensorWriterSmta AccLinearFiltro = new SensorWriterSmta();
	private SensorWriterSmta magnetometro = new SensorWriterSmta();
	private SensorWriterSmta giroscopio = new SensorWriterSmta();

	// Variaveis de reotrnos

	// Getters e setters

	/**
	 * Retorna a velocidade calculada da seguinte forma:
	 * Velocidade em Km ((float) ins.getVelocidadeTotal() / MSTOKM;) 
	 * 
	 * @return velocidade em km
	 * 
	 */
	public float getVelocidade(){
		return mVelocidade;

	}
	/**
	 * Altera o valor da velocidade.
	 * 
	 * 
	 * @param velocidade em km
	 * 
	 */
	public void setVelocidade(float _velocidade){
		mVelocidade = _velocidade;

	}
	/**
	 * Coloca a velocidade em Zero, fazendo com que a posi��o n�o aumente.
	 * 
	 */
	public void setVelocidadeZero(){
		mVelocidade = 0.0f;

	}
	/**
	 * Retorna o semaforo sobre o modo de acelera��o.
	 * 1 - Parado, 2- a desacelerar, 3 - a acelerar
	 *  
	 * @return semaforo
	 * 
	 */
	public int getSemaforo(){
		return mSemaforo;

	}
	/**
	 * Coloca o valor do semaforo conforme a acelera��o.
	 * 1 - Parado, 2- a desacelerar, 3 - a acelerar
	 *  
	 */
	private void setSemaforo(int _semaforo){
		mSemaforo = _semaforo;

	}
	/**
	 * Retorna o Azimuth, pitch e roll. 
	 * Azimute magn�tico - Medida horizontal em graus sobre o norte magn�tico. Medido sobre o Z
	 * Pitch - Rota��o sobre o eixo do X
	 * Roll - Rota��o sobre o eixo do Y
	 *  
	 * @return AziPitRoll array float[] valor[0] - Azimute, Valor[1] - Pitch, Valor[2] - Roll (Valores em radianos)
	 * 
	 */
	public float[] getAziPitRoll(){
		return mAziPitRol;

	}
	/**
	 * Define o valor Azimuth, pitch e roll. 
	 * Azimute magn�tico - Medida horizontal em graus sobre o norte magn�tico. Medido sobre o Z
	 * Pitch - Rota��o sobre o eixo do X
	 * Roll - Rota��o sobre o eixo do Y
	 * 
	 * Aqui vai activar um evento sobre o interface entretanto inicializado na classe que o instancia.
	 *  
	 */
	private void setAziPitRoll(float[] _AziPitRol){
		mAziPitRol = _AziPitRol;
		listener.onInsEvent(tipoRetorno.orientacao); //Se j� tiver o Azimuth, o pitch e rool ent�o envio informa��o pelo callback

	}
	/**
	 * Verifica se o sistema j� inicializou aquando do startHandler ap�s 10 segundos.
	 *  
	 * @return inicioIns boolean true ou false
	 */
	public boolean getInicio()
	{
		return mInicioIns;
	}
	/**
	 * Altera a variavel mInicioIns conforme o estado do Ins.
	 *  
	 */
	private void setInicio(boolean _inicioIns)
	{
		mInicioIns = _inicioIns;
	}
	/**
	 * Retorna a posi��o ECEF do Ins. � uma posi��o inicializada com o vetor PosicaoInicialINS provenientes da primeira leitura do GPS.
	 * Deixa de ser ENU quando tem uma origem predefinida pelo GPS.
	 * 
	 * @return array float[] valor[0] - x , velor[1] - Y, valor[2] - z em metros.
	 *  
	 */
	public float[] getPosicao()
	{
		return  ins.getPosicao();
	}
	/**
	 * Vai definir a posi��o inicial do vetor posicao do INS.
	 * 
	 * @param _posicaoInicial vetor array double[] ECEF (Earth Centered, Earth Fixed)
	 *  
	 */
	public void setPosicaoInicial(double[] _posicaoInicial){
		ins.setPosicaoInicial(_posicaoInicial);
	}
	/**
	 * Vai definir se � para efectuar grava��o em ficheiro ou n�o
	 * 
	 * @param _posicaoInicial vetor array double[] ECEF (Earth Centered, Earth Fixed)
	 *  
	 */
	public void setRecord(boolean _record){
		mRecord = _record;
	}
	/**
	 * Vai iniciar a grava��o
	 * 
	 * @param _posicaoInicial vetor array double[] ECEF (Earth Centered, Earth Fixed)
	 *  
	 */
	public void setStartRec(boolean _startRec){
		mStartRecord = _startRec;
	}

	/**
	 * M�todo contrutor.
	 * 
	 * @param context interface com a informa��o global sobre a aplica��o.
	 * @param OnInsChanged interface do tipo OnInsChanged para comunicar os eventos. Esta interface deve estar implementada na actividade que o chama.
	 *  
	 */
	public InsListener(Context context,OnInsChanged onInsChanged){
		this.setVelocidadeZero(); // Inicializar a velocidade a zero.
		resources = context.getResources();// Tenho de ir buscar os resources, caso contrario devolve nullpointer
		// Referenciar o sensorManager
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);// Para referencia o servi�o de sistema para os sensores
		ins = new InsClass(); // instnacio a classe INS
		operacoes = new Ops(); // Instancio a classe Ops
		prefs = PreferenceManager.getDefaultSharedPreferences(context); //instancio as preferencias de modo static , sem new()
		listener = onInsChanged; // O listener referencia � o que vem por referencia quando inicializo o InsListener.
		// Colcoar os valores do vetor de calibra��o a zero.
		mCalibVetor[0] = 0;
		mCalibVetor[1] = 0;
		mCalibVetor[2] = 0;
		mDadosAcc[0] = 0;
		mDadosAcc[1] = 0;
		mDadosAcc[2] = 0;
		mDadosLinear[0] = 0;
		mDadosLinear[1] = 0;
		mDadosLinear[2] = 0;
		mDadosAccFiltro[0] = 0;
		mDadosAccFiltro[1] = 0;
		mDadosAccFiltro[2] = 0;

	}
	/**
	 * Inicializa as defini��es para os sensores. Carrega preferencias e timers para 
	 * executar a primeira actualiza��o  e actualiza��es de posicao em cada x tempo.
	 * 
	 * Apenas provicendia acesso ao m�todo:
	 * {@link #carregaDefinicoes()}
	 */
	public void start() {
		carregaDefinicoes(); // Para carregar prefs e inicializar os listeners dos sensores

	}
	/**
	 * Retira os listeners dos sensores e coloca o inicioIns=false.
	 * 
	 */
	public void stop(){
		if(sensorManager!=null)
			sensorManager.unregisterListener(this); // Retiro os listeners dos sensores
		//sensorWriter.fechaFicheiro();
		this.setInicio(false);// Coloco o inicio igual a false.
		if(acelerometro.ficheiro!=null && acelerometroFiltro.ficheiro!=null && AccMeuLinear.ficheiro!=null && magnetometro.ficheiro!=null && giroscopio.ficheiro!=null){
			acelerometro.fechaFicheiro();
			acelerometroFiltro.fechaFicheiro();
			AccMeuLinear.fechaFicheiro();
			AccLinear.fechaFicheiro();
			AccLinearGlobais.fechaFicheiro();
			AccMeuLinearFiltro.fechaFicheiro();
			AccLinearFiltro.fechaFicheiro();
			acelerometroGlobais.fechaFicheiro();
			//acelerometroFiltroGlobais.fechaFicheiro();
			AccMeuLinearGlobais.fechaFicheiro();
			//AccMeuLinearFiltroGlobais.fechaFicheiro();
			magnetometro.fechaFicheiro();
			giroscopio.fechaFicheiro();
		}
	}
	/**
	 * Callback chamado pela thread que registou o callback.
	 * Tipos de sensores lidos:
	 * Acelerometro - TYPE_ACCELEROMETER
	 * Magnet�metro - TYPE_MAGNETIC_FIELD
	 * Girosc�pio - TYPE_GYROSCOPE
	 * Vetor rota��o - TYPE_ROTATION_VECTOR
	 * 
	 * Calcula a velocidade baseado nos valores do acelerometro e parametros definidos para o threshold de detec��o de movimento (acelera��o)
	 * Calcula a velocidade linear com os m�todos da classe InsClass, retirando o peso da gravidade dos eixos do aceleromtro.
	 * Vai actualizar a velocidade e posi��o com os m�todos da classe InsClass.
	 * Actualiza ainda a posicao e velocidade de acordo com os valores da taxa de rota��o do gisrocopio. for�a centripeta?.
	 * Rota��o da terra 15,04�/h
	 * 
	 * Actualiza a orienta��o de acordo com a escolha do m�tod de obten��o da matriz DCM rota��o.
	 * 1- Acc+Mag
	 * 2- Vetor rota��o Sensor virtual
	 * 3- Acc+Mag+Gyro - Calculo da fus�o atrav�s de um filtro complementar
	 * 
	 * 
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		int tipoEvento = event.sensor.getType();// guarda o tipo de sensor que gerou o evento
		eventTime = event.timestamp; // Guarda o tempo em nanosegundos quando o evento aconteceu
		float dt = 0; // Intervalo de tempo entre o evento actual e o anterior (posterior passagem para segundos)
		switch (tipoEvento) {
		case Sensor.TYPE_ACCELEROMETER:
			// High Pass
			if(filtro==1){
				mDadosAccFiltro[0] = ins.highPass(event.values[0], mDadosAcc[0], mDadosAccFiltro[0],filtroAlpha);
				mDadosAccFiltro[1] = ins.highPass(event.values[1], mDadosAcc[1], mDadosAccFiltro[1],filtroAlpha);
				mDadosAccFiltro[2] = ins.highPass(event.values[2], mDadosAcc[2], mDadosAccFiltro[2],filtroAlpha);
			}
			// Low Pass
			else if(filtro==2){
				mDadosAccFiltro[0] = ins.lowPass(event.values[0], mDadosAccFiltro[0],filtroAlpha);
				mDadosAccFiltro[1] = ins.lowPass(event.values[1], mDadosAccFiltro[1],filtroAlpha);
				mDadosAccFiltro[2] = ins.lowPass(event.values[2], mDadosAccFiltro[2],filtroAlpha);
			}
			mDadosAcc[0] = event.values[0] + mCalibVetor[0];
			mDadosAcc[1] = event.values[1] + mCalibVetor[1];
			mDadosAcc[2] = event.values[2] + mCalibVetor[2];

			if (anteriorEventTimeAcc != 0)
				dt = (eventTime - anteriorEventTimeAcc) * NS2S;
			anteriorEventTimeAcc = eventTime;
			if(mInicioIns && mStartRecord){
				globalCoords = ins.getWorldVector(mDadosAcc);
				acelerometro.escreveIsto(eventTime + "," + dt + "," + mDadosAcc[0] + "," + mDadosAcc[1] + "," + mDadosAcc[2] + "\n");
				acelerometroGlobais.escreveIsto(eventTime + "," + dt + "," + globalCoords[0] + "," + globalCoords[1] + "," + globalCoords[2] + "\n");
				acelerometroFiltro.escreveIsto(eventTime + "," + dt + "," + mDadosAccFiltro[0] + "," + mDadosAccFiltro[1] + "," + mDadosAccFiltro[2]+ ","+ filtro + "," + filtroAlpha + "\n");
				
			}
			break;
		case Sensor.TYPE_GYROSCOPE:
			mDadosGyro[0] = event.values[0];
			mDadosGyro[1] = event.values[1];
			mDadosGyro[2] = event.values[2];
			if (anteriorEventTimeGyro != 0)
				dt = (eventTime - anteriorEventTimeGyro) * NS2S;
			anteriorEventTimeGyro = eventTime;
			if(mInicioIns && mStartRecord){
				giroscopio.escreveIsto(eventTime + "," + dt + "," + mDadosGyro[0] + "," + mDadosGyro[1] + "," + mDadosGyro[2]+ "\n");
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			mDadosMag[0] = event.values[0];
			mDadosMag[1] = event.values[1];
			mDadosMag[2] = event.values[2];
			if (anteriorEventTimeMag != 0)
				dt = (eventTime - anteriorEventTimeMag) * NS2S;
			anteriorEventTimeMag = eventTime;
			if(mInicioIns && mStartRecord){
				magnetometro.escreveIsto(eventTime + "," + dt + "," + mDadosMag[0] + "," + mDadosMag[1] + "," + mDadosMag[2]+ "\n");
			}
			break;
		case Sensor.TYPE_ROTATION_VECTOR:
			mDadosRotVet[0] = event.values[0];
			mDadosRotVet[1] = event.values[1];
			mDadosRotVet[2] = event.values[2];
			if (anteriorEventTimeRotVet != 0)
				dt = (eventTime - anteriorEventTimeRotVet) * NS2S;
			anteriorEventTimeRotVet = eventTime;
			break;

		case Sensor.TYPE_LINEAR_ACCELERATION:
			// High Pass
			if(filtro==1){
				mDadosAccLinearFiltro[0] = ins.highPass(event.values[0], mDadosLinear[0], mDadosAccLinearFiltro[0],filtroAlpha);
				mDadosAccLinearFiltro[1] = ins.highPass(event.values[1], mDadosLinear[1], mDadosAccLinearFiltro[1],filtroAlpha);
				mDadosAccLinearFiltro[2] = ins.highPass(event.values[2], mDadosLinear[2], mDadosAccLinearFiltro[2],filtroAlpha);
			}
			// Low Pass
			else if(filtro==2){
				mDadosAccLinearFiltro[0] = ins.lowPass(event.values[0], mDadosAccLinearFiltro[0],filtroAlpha);
				mDadosAccLinearFiltro[1] = ins.lowPass(event.values[1], mDadosAccLinearFiltro[1],filtroAlpha);
				mDadosAccLinearFiltro[2] = ins.lowPass(event.values[2], mDadosAccLinearFiltro[2],filtroAlpha);
			}
			mDadosLinear[0] = event.values[0];
			mDadosLinear[1] = event.values[1];
			mDadosLinear[2] = event.values[2];
			if (anteriorEventTimeRotVet != 0)
				dt = (eventTime - anteriorEventTimeRotVet) * NS2S;
			anteriorEventTimeRotVet = eventTime;
			if(mInicioIns && mStartRecord){
				globalCoords = ins.getWorldVector(mDadosLinear);
				AccLinearGlobais.escreveIsto(eventTime + "," + dt + "," + globalCoords[0] + "," + globalCoords[1] + "," + globalCoords[2]+ "\n");
				AccLinear.escreveIsto(eventTime + "," + dt + "," + mDadosLinear[0] + "," + mDadosLinear[1] + "," + mDadosLinear[2]+ "\n");
				AccLinearFiltro.escreveIsto(eventTime + "," + dt + "," + mDadosAccLinearFiltro[0] + "," + mDadosAccLinearFiltro[1] + "," + mDadosAccLinearFiltro[2]+ "\n");
			}
			break;
		}
		// ----------------------------------- Velocidade -----------------------------------------
		// Temos de certificar-nos que o dispositivo apanhou uma orienta��o +- estavel para retirar a gravidade da acelera��o
		// Podemos mais tarde colocar um timer
		if(operacoes.getMagnitude(ins.getAprAccMag())!=0 || operacoes.getMagnitude(ins.getAprFusao())!=0 || operacoes.getMagnitude(ins.getAprRotVet())!=0 && mInicioIns)
		{
			if (tipoEvento == Sensor.TYPE_ACCELEROMETER) {


				// Para a frente +
				// Para tras -
				// Calculo a minha velocidade linear baseada na orienta��o e acelerometro
				mDadosMinhaAccLin = ins.getAccLinear(mDadosAcc);
				if(mStartRecord){
					globalCoords = ins.getWorldVector(mDadosMinhaAccLin);
					AccMeuLinear.escreveIsto(eventTime + "," + dt + "," + mDadosMinhaAccLin[0] + "," + mDadosMinhaAccLin[1] + "," + mDadosMinhaAccLin[2]+ "\n");
					AccMeuLinearGlobais.escreveIsto(eventTime + "," + dt + "," + globalCoords[0] + "," + globalCoords[1] + "," + globalCoords[2]+ "\n");
					
				}

				// Calculo a minha m�dia de amostras iniciais para usar em conjunto
				// com o valor de threshold
				ins.stopDetection.adicionaAcc(mDadosMinhaAccLin);
				// Calcular apenas para Y
				if (!ins.jaCalculouThreshold ) {
					// TODO: Mostrar mensagem progress bar a avisar a calibrar

				} else {
					if (tipoEvento == Sensor.TYPE_ACCELEROMETER) {
						// dadosMinhaAccLin = ins.getAccLinear(dadosAcc); 
						//Apenas para Y
						if ((mDadosMinhaAccLin[1]) > (mCoeficienteThreshold + ins.stopDetection.mThresholdAccelY)) {
							this.setSemaforo(3);// a acelerar

							ins.actualiza_Velocidade(mDadosMinhaAccLin, dt);// Actualiza a velocidade com acelera��o linear
							mContadorDes = 0;
							// TODO: Corrigir o facto de depois da acelera��o feita,
							// este desacelerar bruscamente.
						} else if ((mDadosMinhaAccLin[1]) < (ins.stopDetection.mThresholdAccelY - mCoeficienteThreshold)
								&& mControlDesacelera) {
							if (mContadorDes > mControlDesaceleraThreshold) {
								this.setSemaforo(2);// a abrandar
								ins.actualiza_Velocidade(mDadosMinhaAccLin, dt);
								//ins.actualiza_Posicao(dt);
							}
							mContadorDes++;
						} else {
							this.setSemaforo(1);// Parado
						}
						// Actualizo a minha posi��o independentemente de estar ou n�o a ganhar velocidade
						//ins.actualiza_Velocidade(new float[]{0,0,0}, dt);
						ins.actualiza_Posicao(dt); // Actualiza a posicao
						this.setVelocidade((float) ins.getVelocidadeTotal() / MSTOKM);// A velocidade total apenas mede Y + Z quando telefone na diagonal

						listener.onInsEvent(tipoRetorno.velocidade);


					}
				}
			}
			if (tipoEvento==Sensor.TYPE_GYROSCOPE) { //Actualizar velocidade e posicao

				//ins.actualiza_VelocidadeGyro(mDadosGyro, dt);
				//ins.actualiza_PosicaoGyro(mDadosGyro, dt);
			}
		}

		// ----------------------------------- Orienta��o -----------------------------------------
		if (mTipoOrientacao.equals(resources.getString(R.string.AccMag))) {
			ins.calculaAccMagOrientacao(mDadosMag, mDadosAcc);
			this.setAziPitRoll(ins.getAprAccMag());

		} else if (mTipoOrientacao.equals(resources
				.getString(R.string.RotationVector))) {
			if (tipoEvento==Sensor.TYPE_ROTATION_VECTOR) { 
				ins.calculaRotVetOrientacao(mDadosRotVet);
				this.setAziPitRoll(ins.getAprRotVet());
			}

		} else if (mTipoOrientacao.equals(resources.getString(R.string.Fusao))) {
			if (tipoEvento == Sensor.TYPE_ACCELEROMETER) {
				ins.calculaAccMagOrientacao(mDadosMag, mDadosAcc);

			}
			if (tipoEvento == Sensor.TYPE_GYROSCOPE) {
				ins.calculaFusaoOrientacao(mDadosGyro, dt);
			}
			this.setAziPitRoll(ins.getAprFusao());


		}


	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}


	/**
	 * Determinar quais os sensores que dever�o estar activos. Respeitando o ciclo de vida de uma activadade no android, 
	 * corre sempre que o estado da actividade entra em onResume()
	 * Carregar defini��es das prefs e efectar os posts dos handlers.
	 */
	private void carregaDefinicoes() {

		// Limpa registers anteriores, apesar de a mudan�a ser efectuada na
		// configura��o e esta ser renovada no onPause e onResume
		this.setInicio(false);
		//sensorManager.unregisterListener(this);
		// Vai buscar as preferencioas
		mRateSensor = Integer.parseInt(prefs.getString("rate", "0"));
		mTipoOrientacao = prefs.getString("orientacao", "rotvet");
		mCoeficienteThreshold = Float.parseFloat(prefs.getString("thresholdArranque", "0.4f"));
		mControlDesacelera = prefs.getBoolean("controlaParagem", false);
		mControlDesaceleraThreshold = Integer.parseInt(prefs.getString(
				"controlaParagemContador", "3"));
		mEfectuaCalibracao = prefs.getBoolean("efectuaCalibracao", false);
		filtro = Integer.parseInt(prefs.getString("filtro", "3"));
		filtroAlpha = Float.parseFloat(prefs.getString("thresholdFiltro", "0.3f"));
		// Se efectua calibra��o, ent�o carrego o vetor calibra��o dos valores calibrados para aproxima��o
		if(mEfectuaCalibracao && prefs.contains("AccCalibX"))
		{

			mCalibVetor[0] = prefs.getFloat("AccCalibX", 0.0f);
			mCalibVetor[1] = prefs.getFloat("AccCalibY", 0.0f);
			mCalibVetor[2] = prefs.getFloat("AccCalibZ", 0.0f);

		}
		//---------------------			// Estes sao para retirar � so para gravar valores quando escolho este tipo de orienta��o e n�o faz sentido estar aqui.
		//TODO Retirar este quando final
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
				mRateSensor);
		//--------------------------------------------------------------------
		// O acelerometro fica sempre activo
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				mRateSensor);
		if (mTipoOrientacao.equals(resources.getString(R.string.AccMag))) {
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
					mRateSensor);
			// Este � para retirar � so para gravar valores quando escolho este tipo de orienta��o e n�o faz sentido estar aqui.
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
					mRateSensor);
		} else if (mTipoOrientacao.equals(resources
				.getString(R.string.RotationVector))) {
			sensorManager
			.registerListener(this, sensorManager
					.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
					mRateSensor);
			//TODO Retirar estes seguintes
			//---------------------			// Estes sao para retirar � so para gravar valores quando escolho este tipo de orienta��o e n�o faz sentido estar aqui.
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
					mRateSensor);

			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
					mRateSensor);
			//--------------------------------------------------------------------			
		} else if (mTipoOrientacao.equals(resources.getString(R.string.Fusao))) {
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
					mRateSensor);
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
					mRateSensor);
		}
		// informo que est� a inicializar
		listener.onInsEvent(tipoRetorno.inicializar);
		// So liberto o ecr� depois de 10 s
		startHandler.postDelayed(new Runnable() 
		{ 
			public void run() { 
				if(mRecord) //se for para gravar entao criar ficheiros
				{
					acelerometro.criaFicheiro("smta_isep","acc");
					acelerometroGlobais.criaFicheiro("smta_isep","accglobais");
					acelerometroFiltro.criaFicheiro("smta_isep","accfiltro");
					AccLinear.criaFicheiro("smta_isep","acclinear");
					AccLinearGlobais.criaFicheiro("smta_isep","acclinearglobais");
					AccLinearFiltro.criaFicheiro("smta_isep","acclinearfiltro");
					AccMeuLinearFiltro.criaFicheiro("smta_isep","accmeulinearfiltro");
					AccMeuLinear.criaFicheiro("smta_isep","accmeulinear");
					AccMeuLinearGlobais.criaFicheiro("smta_isep","accmeulinearglobais");
					magnetometro.criaFicheiro("smta_isep","mag");
					giroscopio.criaFicheiro("smta_isep","gyr");
										
				}
				setInicio(true);
				// Para verificar que est� pronto
				listener.onInsEvent(tipoRetorno.inicializar);

			} 
		}, 10000);
		// Actualizo a view de posi��o
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
		posicaoTimer.schedule(timerTask, 7000,2000);
	}
	/**
	 * Interface para comunicar com a classe que implmenta esta interface e assim aceder aos callbacks efectuados
	 * O Enum � apenas para enumerar os diversos tipos de callback que podem existir.
	 * 
	 * @param <E>
	 */
	public interface OnInsChanged<E extends Enum<E>> {
		public void onInsEvent(E value);
	}
}
