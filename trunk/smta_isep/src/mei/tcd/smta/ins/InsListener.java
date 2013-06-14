package mei.tcd.smta.ins;

import java.util.Timer;
import java.util.TimerTask;

import mei.tcd.smta.R;
import mei.tcd.smta.filtros.HighLowPassFilter;
import mei.tcd.smta.filtros.MediaFiltro;
import mei.tcd.smta.filtros.MovingAverage;
import mei.tcd.smta.filtros.SimpleMovingAverage;
import mei.tcd.smta.util.CalibracaoAcc;
import mei.tcd.smta.util.Ops;
import mei.tcd.smta.util.SensorWriterSmta;
import mei.tcd.smta.util.ServidorUdp;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public class InsListener implements SensorEventListener {
	public enum tipoRetorno {inicializar,orientacao, velocidade, posicao};// Tipo de retorno do callback implementado
	private static final float NS2S = 1.0f / 1000000000.0f; //Para calcular segundos de nanosegundos (multiplicar este valor pelo tempo)
	private static final float MSTOKM = 0.277777778f; //MSTOKM - Metros por segundo para KM/H 
	private SensorManager sensorManager = null;// sensorManager - Instancia do sensorManager
	private SharedPreferences prefs;// Gestor de preferencias
	private Resources resources;// Aceder aos resource (Strings, etc...)
	private boolean mInicioIns;// Vai informar se o INS já começõu ou não
	private Context context;
	//Preferencias
	private int mRateSensor;// Frequencia dos sensores definida nas preferencias e sugeridas nos listeners dos sensores.
	private String mTipoOrientacao;// Tipo orientação definida nas preferencias e vai definir se Acc+Mag ou RotVet ou Acc+Mag+Gyro Fusão
	private float mCoeficienteThreshold;// Coeficiente a somar ao threshold calculado para detecção de movimento. Pode ser definido em prefs.
	private boolean mControlDesacelera;// Experimental para testar o controlo de desaceleração instatanea, logo após aceleração. Tem tambem de se definir um número de registos posteriores à desaceleração.
	private int mControlDesaceleraThreshold = 3; //Número de registos a serem descartados após primeira desaceleração. (Valores negativos)
	private boolean mEfectuaCalibracao;// se a calibração efectuada no sensor de aceleração é para ser usada ou não (preferecias)
	private int filtro; //1- High pass, 2- Low pass, 3- Nenhum
	private int mRegistosCalibracao =0; // Número de registo para calibração do acelerometro antes de cda corrida
	private float filtroAlpha;
	private int mPortaIP;
	private String mEnderecoIP;
	private boolean mUdpsend;
	private boolean enviaTodosOrientacao=false;
	//---------------------------
	private int mSemaforo; // devolve um valor a sinalizar se está em aceleração, parado ou a desacelerar
	private float mVelocidade; // Velocidade em Km ((float) ins.getVelocidadeTotal() / MSTOKM;) 
	// dadosAcc, dadosGyro,dadosMag,dadosRotVet - Dados dos sensores em arrays float
	private float[] mCalibVetor_scale = new float[3]; // Vai guardar os valores obtidos da calibração do acelerometro e usa-los na obtenção de valores Acc melhorados.
	private float[] mCalibVetor_bias = new float[3]; // Vai guardar os valores obtidos da calibração do acelerometro e usa-los na obtenção de valores Acc melhorados.
	private float[] mDadosAcc = new float[3]; // Vetor aceleração obtida do Acelerometro nos 3 eixos (X, Y e Z)
	private float[] mDadosGravidade = new float[3]; // Vetor aceleração de gravidade nos 3 eixos (X, Y e Z)
	private float[] mDadosAccCalib = new float[3]; // Vetor aceleração com calibração
	private float[] mDadosAccFiltro = new float[3]; // Vetor aceleração obtida do Acelerometro nos 3 eixos (X, Y e Z)
	private float[] mDadosAccLinearFiltro = new float[3]; // Vetor aceleração obtida do Acelerometro nos 3 eixos (X, Y e Z)
	private float[] mDadosGyro = new float[3]; // Vetor rotaçao obtida do Giroscopio  nos 3 eixos (X, Y e Z)
	private float[] mDadosMag = new float[3]; // Vetor Micro Teslas obtida do magnetometro  nos 3 eixos (X, Y e Z)
	private float[] mDadosMinhaAccLin = new float[3]; // Acelerometro linear retirando a gravidade com DCM na classe InsClass
	private float[] mDadosLinear = new float[3]; // Acelerometro linear do android
	private float[] mDadosMediaAcc = new float[3]; // Acelerometro linear retirando a gravidade com DCM na classe InsClass
	private float[] mDadosMediaAccLinear = new float[3]; // Acelerometro linear retirando a gravidade com DCM na classe InsClass
	private float[] mDadosRotVet = new float[3]; // Vetor rotação obtido do sensor virtual Rotation Vector.
	private float[] mAziPitRol = new float[3]; // Azimuth pith e roll de retorno
	private float[] globalCoords = new float[3]; //Vetor temporario para coordenadas globais
	private float[] mDadosCalibradosMedia = new float[3];
	
	//Filtro
	// Filtro SMA
	private boolean mUsaSma; 
	private int mSizeSma;
//	private static final int MEDIA_FILTRO_SIZE=5;
//	private static final int SMA_SIZE=5;
	private MovingAverage mSmaX; //new MovingAverage(SMA_SIZE);
	private MovingAverage mSmaY; //= new MovingAverage(SMA_SIZE);
	private MovingAverage mSmaZ;// = new MovingAverage(SMA_SIZE);
	private SimpleMovingAverage mSma2X; //= new SimpleMovingAverage(SMA_SIZE);
	private SimpleMovingAverage mSma2Y; //= new SimpleMovingAverage(SMA_SIZE);
	private SimpleMovingAverage mSma2Z; //= new SimpleMovingAverage(SMA_SIZE);
	private HighLowPassFilter highlowpassfilter;
//	private MediaFiltro mFiltroAcc = new MediaFiltro(MEDIA_FILTRO_SIZE);
//	private MediaFiltro mFiltroAccLinear = new MediaFiltro(MEDIA_FILTRO_SIZE);
	private long eventTime = 0;// eventTime - Tempo em nanosegundos quando o evento aconteceu
	private boolean mRecord,mStartRecord=false;
	// anteriorEventTimeAcc, anteriorEventTimeGyro,anteriorEventTimeMag,anteriorEventTimeRotVet - Tempo em nanosegundos do evento anterior dos três sensores
	private long anteriorEventTimeGravidade = 0,anteriorEventTimeAcc = 0, anteriorEventTimeGyro = 0,anteriorEventTimeMag = 0, anteriorEventTimeRotVet = 0,anteriorEventAccLinear = 0;
	private int mContadorDes = 0;// contadorDes - Para ser usado em conjunto com o controlo de desaceleração
	private InsClass ins;// ins - Instancia classe INS
	private Ops operacoes;// ops - Instancia da classe de operações genéricas
	private CalibracaoAcc calibAcc;
	// Handlers 
	// Com os handler podemos enviar e processar mensagens no message Queue (fila de mensagens) na thread
	private Handler startHandler = new Handler();  // Obriga a começar quando 10s para certificar que a orientação está fixa e que o threshold da aceleração foi calculada
	private Handler posicaoHandler = new Handler();  // Corre un runnable para enviar a posição de X em X tempo definido em posicaoTimer

	private Timer posicaoTimer = new Timer();//vou colocar um timer para posicionar na view de x em x tempo
	// referenciair a classe de enio de pacotes udp
	//private ServidorUdp enviarUdp;
	// String de sensores a enviar para ficheiro ou udp
	private String dadosSensores="";
	private String dadosSensoresUdp ="";
	TimerTask udpTask;
	private Handler handlerUdp = new Handler();
	private int accControlaUdp;
	private int gyrControlaUdp;
	private int graControlaUdp;
	private int accLinearControlaUdp;
//	private Sender sd;
	// Interface para comunicar com quem instancia e enviar os retornos quando prontos.
	private OnInsChanged<tipoRetorno> listener;
	// Debug----------------------
	// Acc_Filtro_Globais - Guarda registos de Aceleração, aceleração filtrada e aceleração para coordenadas globais ENU
	// [1] - Timestamp - Tempo em nanosegundos
	// [2] - Dt - Tempo entre eventos
	// [3] - Aceleração Eixo X
	// [4] - Aceleração Eixo Y
	// [5] - Aceleração Eixo Z
	// [6] - Aceleração Filtro Eixo X
	// [7] - Aceleração Filtro Eixo Y
	// [8] - Aceleração Filtro Eixo Z
	// [9] - Tipo de filtro
	// [10] - Valor do filtro
	// [11] - Aceleração Globais Eixo X
	// [12] - Aceleração Globais Eixo Y
	// [13] - Aceleração Globais Eixo Z
	//
	// [14] - Aceleração sma Eixo X
	// [15] - Aceleração sma Eixo Y
	// [16] - Aceleração sma Eixo Z
	// [17] - Calibração Scale Eixo X
	// [18] - Calibração Scale Eixo Y
	// [19] - Calibração Scale Eixo Z
	// [20] - Calibração Bias Eixo X
	// [21] - Calibração Bias Eixo Y
	// [22] - Calibração Bias Eixo X
	// [23] - Aceleração X + Calibração Eixo X
	// [24] - Aceleração Y + Calibração Eixo Y
	// [25] - Aceleração Z + Calibração Eixo X
	// [26] - SMA2 Aceleração X 
	// [27] - SMA2 Aceleração Y 
	// [28] - SMA2 Aceleração Z 
	// [29] - SMA pontos
	// AccLinear_Filtro_Globais - Guarda registos de Aceleração Linear, aceleração linear filtrada e aceleração linear para coordenadas globais ENU
	// [1] - Timestamp - Tempo em nanosegundos
	// [2] - Dt - Tempo entre eventos
	// [3] - Aceleração Eixo X
	// [4] - Aceleração Eixo Y
	// [5] - Aceleração Eixo Z
	// [6] - Aceleração Filtro Eixo X
	// [7] - Aceleração Filtro Eixo Y
	// [8] - Aceleração Filtro Eixo Z
	// [9] - Tipo de filtro
	// [10] - Valor do filtro
	// [11] - Aceleração Globais Eixo X
	// [12] - Aceleração Globais Eixo Y
	// [13] - Aceleração Globais Eixo Z
	private SensorWriterSmta Acc_Filtro_Globais = new SensorWriterSmta(); //
	private SensorWriterSmta AccMediaFiltro = new SensorWriterSmta();
	private SensorWriterSmta AccLinear_Filtro_Globais = new SensorWriterSmta();
	private SensorWriterSmta AccLinearMediaFiltro = new SensorWriterSmta();
	private SensorWriterSmta AccMeuLinear_Globais = new SensorWriterSmta();
	private SensorWriterSmta magnetometro = new SensorWriterSmta();
	private SensorWriterSmta giroscopio = new SensorWriterSmta();
	private SensorWriterSmta gravidade = new SensorWriterSmta();
	private SensorWriterSmta accmagapr = new SensorWriterSmta();
	private SensorWriterSmta rvapr = new SensorWriterSmta();
	private SensorWriterSmta fusaoapr = new SensorWriterSmta();

	/*NOVO FILTRO*/
	private boolean initState = true;
	// rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];
    private float timestamp;
 // angular speeds from gyro
    private float[] gyro = new float[3];
 // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
// DEBUG
	private float[] clobalcoo = new float[3];
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
	 * Coloca a velocidade em Zero, fazendo com que a posição não aumente.
	 * 
	 */
	public void setVelocidadeZero(){
		mVelocidade = 0.0f;

	}
	/**
	 * Retorna o semaforo sobre o modo de aceleração.
	 * 1 - Parado, 2- a desacelerar, 3 - a acelerar
	 *  
	 * @return semaforo
	 * 
	 */
	public int getSemaforo(){
		return mSemaforo;

	}
	/**
	 * Coloca o valor do semaforo conforme a aceleração.
	 * 1 - Parado, 2- a desacelerar, 3 - a acelerar
	 *  
	 */
	private void setSemaforo(int _semaforo){
		mSemaforo = _semaforo;

	}
	/**
	 * Retorna o Azimuth, pitch e roll. 
	 * Azimute magnético - Medida horizontal em graus sobre o norte magnético. Medido sobre o Z
	 * Pitch - Rotação sobre o eixo do X
	 * Roll - Rotação sobre o eixo do Y
	 *  
	 * @return AziPitRoll array float[] valor[0] - Azimute, Valor[1] - Pitch, Valor[2] - Roll (Valores em radianos)
	 * 
	 */
	public float[] getAziPitRoll(){
		return mAziPitRol;

	}
	/**
	 * Define o valor Azimuth, pitch e roll. 
	 * Azimute magnético - Medida horizontal em graus sobre o norte magnético. Medido sobre o Z
	 * Pitch - Rotação sobre o eixo do X
	 * Roll - Rotação sobre o eixo do Y
	 * 
	 * Aqui vai activar um evento sobre o interface entretanto inicializado na classe que o instancia.
	 *  
	 */
	private void setAziPitRoll(float[] _AziPitRol){
		mAziPitRol = _AziPitRol;
		listener.onInsEvent(tipoRetorno.orientacao); //Se já tiver o Azimuth, o pitch e rool então envio informação pelo callback

	}
	/**
	 * Verifica se o sistema já inicializou aquando do startHandler após 10 segundos.
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
	 * Retorna a posição ECEF do Ins. É uma posição inicializada com o vetor PosicaoInicialINS provenientes da primeira leitura do GPS.
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
	 * Vai definir a posição inicial do vetor posicao do INS.
	 * 
	 * @param _posicaoInicial vetor array double[] ECEF (Earth Centered, Earth Fixed)
	 *  
	 */
	public void setPosicaoInicial(double[] _posicaoInicial){
		ins.setPosicaoInicial(_posicaoInicial);
	}
	/**
	 * Vai definir se é para efectuar gravação em ficheiro ou não
	 * 
	 * @param _posicaoInicial vetor array double[] ECEF (Earth Centered, Earth Fixed)
	 *  
	 */
	public void setRecord(boolean _record){
		mRecord = _record;
	}
	/**
	 * Vai iniciar a gravação
	 * 
	 * @param _posicaoInicial vetor array double[] ECEF (Earth Centered, Earth Fixed)
	 *  
	 */
	public void setStartRec(boolean _startRec){
		mStartRecord = _startRec;
		accControlaUdp=accLinearControlaUdp=gyrControlaUdp=0;
		 
	}

	/**
	 * Método contrutor.
	 * 
	 * @param context interface com a informação global sobre a aplicação.
	 * @param OnInsChanged interface do tipo OnInsChanged para comunicar os eventos. Esta interface deve estar implementada na actividade que o chama.
	 *  
	 */
	public InsListener(Context _context,OnInsChanged<tipoRetorno> onInsChanged){
		this.context = _context;
		this.setVelocidadeZero(); // Inicializar a velocidade a zero.
		resources = context.getResources();// Tenho de ir buscar os resources, caso contrario devolve nullpointer
		// Referenciar o sensorManager
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);// Para referencia o serviço de sistema para os sensores
		ins = new InsClass(); // instnacio a classe INS
		operacoes = new Ops(); // Instancio a classe Ops
		prefs = PreferenceManager.getDefaultSharedPreferences(context); //instancio as preferencias de modo static , sem new()
		listener = onInsChanged; // O listener referencia é o que vem por referencia quando inicializo o InsListener.
		// Colcoar os valores do vetor de calibração a zero.
		mCalibVetor_scale[0] = 1;
		mCalibVetor_scale[1] = 1;
		mCalibVetor_scale[2] = 1;
		mCalibVetor_bias[0] = 0;
		mCalibVetor_bias[1] = 0;
		mCalibVetor_bias[2] = 0;
		
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
	 * Inicializa as definições para os sensores. Carrega preferencias e timers para 
	 * executar a primeira actualização  e actualizações de posicao em cada x tempo.
	 * 
	 * Apenas provicendia acesso ao método:
	 * {@link #carregaDefinicoes()}
	 */
	public void start() {
		carregaDefinicoes(); // Para carregar prefs e inicializar os listeners dos sensores
		accControlaUdp=gyrControlaUdp=0;
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
		if(AccMediaFiltro.ficheiro!=null && Acc_Filtro_Globais.ficheiro!=null && AccMeuLinear_Globais.ficheiro!=null && magnetometro.ficheiro!=null && giroscopio.ficheiro!=null){
			Acc_Filtro_Globais.fechaFicheiro();
			AccMediaFiltro.fechaFicheiro();
			AccLinear_Filtro_Globais.fechaFicheiro();
			AccLinearMediaFiltro.fechaFicheiro();
			AccMeuLinear_Globais.fechaFicheiro();
			magnetometro.fechaFicheiro();
			giroscopio.fechaFicheiro();
			gravidade.fechaFicheiro();
			accmagapr.fechaFicheiro();
			rvapr.fechaFicheiro();
			fusaoapr.fechaFicheiro();
			
		}
//		if(sd != null)
//		{
//			sd.stopThread();
//		}
		
		 if( udpTask != null )   
	        {  
			 udpTask.cancel();  
	        }  
		//enviarUdp.CloseUdp();
	}
	/**
	 * Callback chamado pela thread que registou o callback.
	 * Tipos de sensores lidos:
	 * Acelerometro - TYPE_ACCELEROMETER
	 * Magnetómetro - TYPE_MAGNETIC_FIELD
	 * Giroscópio - TYPE_GYROSCOPE
	 * Vetor rotação - TYPE_ROTATION_VECTOR
	 * 
	 * Calcula a velocidade baseado nos valores do acelerometro e parametros definidos para o threshold de detecção de movimento (aceleração)
	 * Calcula a velocidade linear com os métodos da classe InsClass, retirando o peso da gravidade dos eixos do aceleromtro.
	 * Vai actualizar a velocidade e posição com os métodos da classe InsClass.
	 * Actualiza ainda a posicao e velocidade de acordo com os valores da taxa de rotação do gisrocopio. força centripeta?.
	 * Rotação da terra 15,04º/h
	 * 
	 * Actualiza a orientação de acordo com a escolha do métod de obtenção da matriz DCM rotação.
	 * 1- Acc+Mag
	 * 2- Vetor rotação Sensor virtual
	 * 3- Acc+Mag+Gyro - Calculo da fusão através de um filtro complementar
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
			// Calcula do Dt tempo entre eventos
			if (anteriorEventTimeAcc != 0)
				dt = (eventTime - anteriorEventTimeAcc) * NS2S;
			anteriorEventTimeAcc = eventTime;
			accControlaUdp ++; // Para controla os pacotes udp que por vezes vão repetidos.
			// Filtro High Pass
			if(filtro==1){
				mDadosAccFiltro = highlowpassfilter.highPass(mDadosAcc, event.values.clone(), mDadosAccFiltro);
			}
			// Filtro Low Pass
			else if(filtro==2){
				mDadosAccFiltro = highlowpassfilter.lowPass(mDadosAccCalib, mDadosAccFiltro);
			}
			mDadosAcc = event.values.clone();
			mDadosAccCalib = vetorCalibrado(mDadosAcc.clone(),mDadosAccCalib);
			// SMA2 do livro
			mSma2X.pushValue(mDadosAccCalib[0]);
			mSma2Y.pushValue(mDadosAccCalib[1]);
			mSma2Z.pushValue(mDadosAccCalib[2]);
			mDadosCalibradosMedia[0]=mSma2X.getValue();
			mDadosCalibradosMedia[1]=mSma2Y.getValue();
			mDadosCalibradosMedia[2]=mSma2Z.getValue();
			// Verifica se já iniciou o INS e se é para guardar registos
			if(mInicioIns && mStartRecord){
				// Vai buscar as coordenadas globias AccW = Acc * DCM
				clobalcoo[0] = mSma2X.getValue();
				clobalcoo[1] = mSma2Y.getValue();
				clobalcoo[2] = mSma2Z.getValue();
				globalCoords = ins.getWorldVector(clobalcoo);
				
	    		
	    		Log.d("Aceleração Navegação", globalCoords[0] + "-" + globalCoords[1]+"-" + globalCoords[2]);
				
				//mSmaX.compute(mDadosAcc[0]);
				dadosSensores=eventTime + "," + dt + "," + mDadosAcc[0] + "," + mDadosAcc[1] + "," + mDadosAcc[2] + "," +
						mDadosAccFiltro[0] + "," + mDadosAccFiltro[1] + "," + mDadosAccFiltro[2]+ ","+ filtro + "," + filtroAlpha + "," +
						globalCoords[0] + "," + globalCoords[1] + "," + globalCoords[2] + "," +
						mSmaX.compute(mDadosAccCalib[0]) + "," +mSmaY.compute(mDadosAccCalib[1]) + "," + mSmaZ.compute(mDadosAccCalib[2]) + "," +
						mCalibVetor_scale[0] + "," + mCalibVetor_scale[1] + "," + mCalibVetor_scale[2] + "," +
						mCalibVetor_bias[0] + "," + mCalibVetor_bias[1] + "," + mCalibVetor_bias[2] + "," +
						mDadosAccCalib[0] + "," + mDadosAccCalib[1] + "," + mDadosAccCalib[2] + "," + 
						mSma2X.getValue() + "," + mSma2Y.getValue() + "," + mSma2Z.getValue()  + "\n";
				
				
				if(mUdpsend){
					//dadosSensoresUdp=dadosSensoresUdp + accControlaUdp+","+"acc"+","+(dadosSensores)+"\r\n";
					/***
					 * [1] - Controla UDP
					 * [2] - String Controlo e.g. 'acc'
					 * [3] - Event time
					 * [4] - dt
					 * [5] - Acc raw x
					 * [6] - Acc raw y 
					 * [7] - Acc raw z
					 * [8] - Acc filtro x
					 * [9] - Acc filtro y
					 * [10] - Acc filtro z
					 * [11] - Acc filtro Alpha
					 * [12] - Acc SMA x
					 * [13] - Acc SMA y
					 * [14] - Acc SMA z
					 * [15] - Acc SMA pontos
					 * [16] - Acc Calibrado x
					 * [17] - Acc Calibrado y
					 * [18] - Acc Calibrado z
					 * [19] - Acc Global x
					 * [20] - Acc Global y
					 * [21] - Acc Global z
					 */
//					dadosSensoresUdp=dadosSensoresUdp + accControlaUdp+","+"acc"+","+eventTime + "," + dt + "," + 
//										mDadosAcc[0] + "," + mDadosAcc[1] + "," + mDadosAcc[2] + "," +
//										mDadosAccFiltro[0] + "," + mDadosAccFiltro[1] + "," + mDadosAccFiltro[2]+  "," + filtroAlpha + "," +
//										mSma2X.getValue() + "," + mSma2Y.getValue() + "," + mSma2Z.getValue() + "," + mSizeSma +"," +
//										mDadosAccCalib[0] + "," + mDadosAccCalib[1] + "," + mDadosAccCalib[2] +"," +
//										globalCoords[0] + "," + globalCoords[1] + "," + globalCoords[2] + "," +"\r\n";
					dadosSensoresUdp=dadosSensoresUdp + accControlaUdp+","+"acc"+","+eventTime + "," + dt + "," + 
										
				}
				Acc_Filtro_Globais.escreveIsto(dadosSensores);
				//Log.d("StringUdp",dadosSensoresUdp.length() + "#####" + dadosSensoresUdp);
				
				// Outro filtro de média
//				mFiltroAcc.adicionaVector(new float[]{dt,mDadosAcc[0],mDadosAcc[1],mDadosAcc[2]});
//				// Média de X valores
//				if(mFiltroAcc.getCount()==MEDIA_FILTRO_SIZE){
//					mDadosMediaAcc = mFiltroAcc.getMediaVector();
//					AccMediaFiltro.escreveIsto(eventTime + "," + mDadosMediaAcc[0] + "," + mDadosMediaAcc[1] + "," + mDadosMediaAcc[2] + "," + mDadosMediaAcc[3] + "\n");
//				}
				
				
			}
			break;
		case Sensor.TYPE_GYROSCOPE:
			gyrControlaUdp ++;
			mDadosGyro = event.values.clone();
			
			if (anteriorEventTimeGyro != 0)
				dt = (eventTime - anteriorEventTimeGyro) * NS2S;
			anteriorEventTimeGyro = eventTime;
			if(mInicioIns && mStartRecord){
				if(mUdpsend){
					dadosSensoresUdp=dadosSensoresUdp + gyrControlaUdp+","+"gyr"+","+eventTime + "," + dt + "," + mDadosGyro[0] + "," + mDadosGyro[1] + "," + mDadosGyro[2]+"\r\n";
				}
				giroscopio.escreveIsto(eventTime + "," + dt + "," + mDadosGyro[0] + "," + mDadosGyro[1] + "," + mDadosGyro[2]+ "\n");
				
			}
			break;
		case Sensor.TYPE_GRAVITY:
			mDadosGravidade = event.values.clone();
			graControlaUdp ++;
			if (anteriorEventTimeGyro != 0)
				dt = (eventTime - anteriorEventTimeGravidade) * NS2S;
			anteriorEventTimeGravidade = eventTime;
			if(mInicioIns && mStartRecord){
//				if(mUdpsend){
//					dadosSensoresUdp=dadosSensoresUdp + graControlaUdp+","+"gra"+","+eventTime + "," + dt + "," + mDadosGravidade[0] + "," + mDadosGravidade[1] + "," + mDadosGravidade[2]+"\r\n";
//				}				gravidade.escreveIsto(eventTime + "," + dt + "," + mDadosGravidade[0] + "," + mDadosGravidade[1] + "," + mDadosGravidade[2]+ "\n");
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			mDadosMag = event.values.clone();
			
			if (anteriorEventTimeMag != 0)
				dt = (eventTime - anteriorEventTimeMag) * NS2S;
			anteriorEventTimeMag = eventTime;
			if(mInicioIns && mStartRecord){
				magnetometro.escreveIsto(eventTime + "," + dt + "," + mDadosMag[0] + "," + mDadosMag[1] + "," + mDadosMag[2]+ "\n");
			}
			break;
		case Sensor.TYPE_ROTATION_VECTOR:
			mDadosRotVet = event.values.clone();
			
			if (anteriorEventTimeRotVet != 0)
				dt = (eventTime - anteriorEventTimeRotVet) * NS2S;
			anteriorEventTimeRotVet = eventTime;
			Log.d("Vetor rotacao", mDadosRotVet[0] + "-" + mDadosRotVet[1]+"-" + mDadosRotVet[2]);
			break;

		case Sensor.TYPE_LINEAR_ACCELERATION:
			// High Pass
			if(filtro==1){
				mDadosAccLinearFiltro = highlowpassfilter.highPass(mDadosLinear.clone(), event.values.clone(), mDadosAccLinearFiltro);
				
			}
			// Low Pass
			else if(filtro==2){
				mDadosAccLinearFiltro = highlowpassfilter.lowPass(mDadosLinear.clone(), mDadosAccLinearFiltro);
			
			}
			mDadosLinear = event.values.clone();
			
			if (anteriorEventAccLinear != 0)
				dt = (eventTime - anteriorEventAccLinear) * NS2S;
			anteriorEventAccLinear = eventTime;
			if(mInicioIns && mStartRecord){
				globalCoords = ins.getWorldVector(mDadosLinear.clone());
				AccLinear_Filtro_Globais.escreveIsto(eventTime + "," + dt + "," + mDadosLinear[0] + "," + mDadosLinear[1] + "," + mDadosLinear[2]+"," +
						mDadosAccLinearFiltro[0] + "," + mDadosAccLinearFiltro[1] + "," + mDadosAccLinearFiltro[2]+ "," + filtro + "," + filtroAlpha + "," +
						globalCoords[0] + "," + globalCoords[1] + "," + globalCoords[2]+ "\n");
				
//				mFiltroAccLinear.adicionaVector(new float[]{dt,mDadosLinear[0],mDadosLinear[1],mDadosLinear[2]});
//				if(mFiltroAccLinear.getCount()==MEDIA_FILTRO_SIZE){
//					mDadosMediaAccLinear = mFiltroAccLinear.getMediaVector();
//					AccLinearMediaFiltro.escreveIsto(eventTime + "," + mDadosMediaAccLinear[0] + "," + mDadosMediaAccLinear[1] + "," + mDadosMediaAccLinear[2] + "," + mDadosMediaAccLinear[3] + "\n");
//				}
			}
			break;
		}
		// ----------------------------------- Velocidade -----------------------------------------
		// Temos de certificar-nos que o dispositivo apanhou uma orientação +- estavel para retirar a gravidade da aceleração
		// Podemos mais tarde colocar um timer
		if(operacoes.getMagnitude(ins.getAprAccMag())!=0 || operacoes.getMagnitude(ins.getAprFusao())!=0 || operacoes.getMagnitude(ins.getAprRotVet())!=0 && mInicioIns)
		{
			// alterei evento de acelerometro para gravidade
			if (tipoEvento == Sensor.TYPE_ACCELEROMETER) {

				
				// Para a frente +
				// Para tras -
				// Calculo a minha velocidade linear baseada na orientação e acelerometro calibrado
				mDadosMinhaAccLin = ins.getAccLinear(mDadosAccCalib);
				if(mStartRecord){
					globalCoords = ins.getWorldVector(mDadosMinhaAccLin);
					AccMeuLinear_Globais.escreveIsto(eventTime + "," + dt + "," + mDadosMinhaAccLin[0] + "," + mDadosMinhaAccLin[1] + "," + mDadosMinhaAccLin[2]+ "," +
							globalCoords[0] + "," + globalCoords[1] + "," + globalCoords[2]+ "\n");
					accLinearControlaUdp ++;
					if(mUdpsend){
						dadosSensoresUdp=dadosSensoresUdp + accLinearControlaUdp+","+"acclin"+","+eventTime + "," + dt + "," + mDadosMinhaAccLin[0] + "," + mDadosMinhaAccLin[1] + "," + mDadosMinhaAccLin[2]+"\r\n";
					}
				}

				// Calculo a minha média de amostras iniciais para usar em conjunto
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

							ins.actualiza_Velocidade(mDadosMinhaAccLin, dt);// Actualiza a velocidade com aceleração linear
							mContadorDes = 0;
							// TODO: Corrigir o facto de depois da aceleração feita,
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
						// Actualizo a minha posição independentemente de estar ou não a ganhar velocidade
						//ins.actualiza_Velocidade(new float[]{0,0,0}, dt);
						ins.actualiza_Posicao(dt); // Actualiza a posicao
						this.setVelocidade(ins.getVelocidadeTotal() / MSTOKM);// A velocidade total apenas mede Y + Z quando telefone na diagonal

						listener.onInsEvent(tipoRetorno.velocidade);


					}
				}
			}
			if (tipoEvento==Sensor.TYPE_GYROSCOPE) { //Actualizar velocidade e posicao

				//ins.actualiza_VelocidadeGyro(mDadosGyro, dt);
				//ins.actualiza_PosicaoGyro(mDadosGyro, dt);
			}
		}

		// ----------------------------------- Orientação -----------------------------------------
		// Se for para gravar então gravar os três para comparativo
		if (mTipoOrientacao.equals(resources.
				getString(R.string.AccMag)) || enviaTodosOrientacao) {
			if (tipoEvento == Sensor.TYPE_ACCELEROMETER || tipoEvento == Sensor.TYPE_MAGNETIC_FIELD) {
				// Alterar o vetor aceleração pelo gravidade
				ins.calculaAccMagOrientacao(mDadosMag, mDadosAccCalib);
				this.setAziPitRoll(ins.getAprAccMag());
				if(mInicioIns && mStartRecord){
					accmagapr.escreveIsto(eventTime + "," + dt + "," + ins.getAprAccMag()[0] + "," + ins.getAprAccMag()[1] + ","+ ins.getAprAccMag()[2] + "\n");
				}
			}	

		}  if (mTipoOrientacao.equals(resources
				.getString(R.string.RotationVector)) || enviaTodosOrientacao) {
			if (tipoEvento==Sensor.TYPE_ROTATION_VECTOR) { 
				ins.calculaRotVetOrientacao(mDadosRotVet);
				this.setAziPitRoll(ins.getAprRotVet());
				if(mInicioIns && mStartRecord){
					rvapr.escreveIsto(eventTime + "," + dt + "," + ins.getAprRotVet()[0] + "," + ins.getAprRotVet()[1] + ","+ ins.getAprRotVet()[2] + "\n");
				}
			}

		}  if (mTipoOrientacao.equals(resources.
				getString(R.string.Fusao)) || enviaTodosOrientacao) {
			if (tipoEvento == Sensor.TYPE_ACCELEROMETER) {
				//ins.calculaAccMagOrientacao(mDadosMag, mDadosAccCalib);
				ins.calculaAccMagOrientacao(mDadosMag, mDadosAccCalib);
				this.setAziPitRoll(ins.getAprFusao());
				if(mInicioIns && mStartRecord){
					fusaoapr.escreveIsto(eventTime + "," + dt + "," + ins.getAprFusao()[0] + "," + ins.getAprFusao()[1] + ","+ ins.getAprFusao()[2] + "\n");
				}
			}
			if (tipoEvento == Sensor.TYPE_GYROSCOPE) {
				
				ins.gyroFunction(event);
				/*esta parte de codigo fica entre os dois tipos de evento*/
				this.setAziPitRoll(ins.getAprFusao());
				if(mInicioIns && mStartRecord){
					fusaoapr.escreveIsto(eventTime + "," + dt + "," + ins.getAprFusao()[0] + "," + ins.getAprFusao()[1] + ","+ ins.getAprFusao()[2] + "\n");
				}
				/*-------------------------------------------------------*/
				//ins.calculaFusaoOrientacao(mDadosGyro, dt);
				/*NOVO FILTRO*/
				
			}
			

		}


	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}
	
	
	/**
	 * Determinar quais os sensores que deverão estar activos. Respeitando o ciclo de vida de uma activadade no android, 
	 * corre sempre que o estado da actividade entra em onResume()
	 * Carregar definições das prefs e efectar os posts dos handlers.
	 */
	private void carregaDefinicoes() {

		// Limpa registers anteriores, apesar de a mudança ser efectuada na
		// configuração e esta ser renovada no onPause e onResume
		this.setInicio(false);
		// Vai buscar as preferencioas
		// -----------------------------------------------------------------------------------------------------------
		// Filtro SMA
		mUsaSma = prefs.getBoolean("usaSma", false);
		mSizeSma = Integer.parseInt(prefs.getString("thresholdSMA", "5"));
		// Filtro Low Pass ou HighPass
		filtro = Integer.parseInt(prefs.getString("filtro", "3"));
		filtroAlpha = Float.parseFloat(prefs.getString("thresholdFiltro", "0.3f"));
		highlowpassfilter = new HighLowPassFilter(filtroAlpha);
		// Rate sensor
		mRateSensor = Integer.parseInt(prefs.getString("rate", "0"));
		// Calibração
		mEfectuaCalibracao = prefs.getBoolean("efectuaCalibracao", false);
		mRegistosCalibracao = Integer.parseInt(prefs.getString("thresholdCalibracao", "500"));
		// Tipo orientação
		mTipoOrientacao = prefs.getString("orientacao", "rotvet");
		mCoeficienteThreshold = Float.parseFloat(prefs.getString("thresholdArranque", "0.4f"));
		// Controlar desaceleração
		mControlDesacelera = prefs.getBoolean("controlaParagem", false);
		mControlDesaceleraThreshold = Integer.parseInt(prefs.getString(
				"controlaParagemContador", "3"));
		// valores para configurar o servidor udp
		mPortaIP = Integer.parseInt(prefs.getString("udpporta", "49000"));
		mEnderecoIP = prefs.getString("udpip", "0.0.0.0");
		mUdpsend=prefs.getBoolean("udpsend", false);
		// Inicialização do Simple Moving Average
		mSmaX = new MovingAverage(mSizeSma);
		mSmaY = new MovingAverage(mSizeSma);
		mSmaZ = new MovingAverage(mSizeSma);
		mSma2X = new SimpleMovingAverage(mSizeSma);
		mSma2Y = new SimpleMovingAverage(mSizeSma);
		mSma2Z = new SimpleMovingAverage(mSizeSma);
		//---------------------------------------------------------------------------------------------------------------
		// Efectuar calibração
		// Se efectua calibração, então carrego o vetor calibração dos valores calibrados para aproximação
		if(mEfectuaCalibracao && prefs.contains("k_X"))
		{
			mCalibVetor_scale[0] = prefs.getFloat("k_X", 1.0f);
			mCalibVetor_scale[1] =prefs.getFloat("k_Y", 1.0f);
			mCalibVetor_scale[2] =prefs.getFloat("k_Z", 1.0f);
			mCalibVetor_bias[0] = prefs.getFloat("b_X", 0.0f);
			mCalibVetor_bias[1] = prefs.getFloat("b_Y", 0.0f);
			mCalibVetor_bias[2] = prefs.getFloat("b_Z", 0.0f);
		}
//		if(mUdpsend)
//		{
//			//enviarUdp = new ServidorUdp(mEnderecoIP, mPortaIP);
//			
//		}
		//---------------------			// Estes sao para retirar é so para gravar valores quando escolho este tipo de orientação e não faz sentido estar aqui.
		//TODO Retirar este quando final
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
				mRateSensor);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
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
			// Este é para retirar é so para gravar valores quando escolho este tipo de orientação e não faz sentido estar aqui.
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
					mRateSensor);
			/*Apenas para omparativo*/
			sensorManager
			.registerListener(this, sensorManager
					.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
					mRateSensor);
		} else if (mTipoOrientacao.equals(resources
				.getString(R.string.RotationVector))) {
			sensorManager
			.registerListener(this, sensorManager
					.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
					mRateSensor);
			//TODO Retirar estes seguintes
			//---------------------			// Estes sao para retirar é so para gravar valores quando escolho este tipo de orientação e não faz sentido estar aqui.
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
			/*Apenas para omparativo*/
			sensorManager
			.registerListener(this, sensorManager
					.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
					mRateSensor);
		}
		// informo que está a inicializar
		listener.onInsEvent(tipoRetorno.inicializar);
		// So liberto o ecrã depois de 10 s
		startHandler.postDelayed(new Runnable() 
		{ 
			@Override
			public void run() { 
				if(mRecord) //se for para gravar entao criar ficheiros
				{
					Acc_Filtro_Globais.criaFicheiro("smta_isep","AccFiltroGlobais");
					AccMediaFiltro.criaFicheiro("smta_isep","AccMediaFiltro");
					
					AccLinear_Filtro_Globais.criaFicheiro("smta_isep","AccLinearFiltroGlobais");
					AccLinearMediaFiltro.criaFicheiro("smta_isep","AccLinearMediaFiltro");
					
					AccMeuLinear_Globais.criaFicheiro("smta_isep","AccMeulinearGlobais");
					
					magnetometro.criaFicheiro("smta_isep","mag");
					giroscopio.criaFicheiro("smta_isep","gyr");
					gravidade.criaFicheiro("smta_isep","gravidade");
					accmagapr.criaFicheiro("smta_isep", "apr_accmag");
					rvapr.criaFicheiro("smta_isep", "apr_rv");
					fusaoapr.criaFicheiro("smta_isep", "apr_fusao");
										
				}
				
				// Set flasg para inicio
				setInicio(true); // Aqui digo que está tudo pronto
				// Para verificar que está pronto
				listener.onInsEvent(tipoRetorno.inicializar);

			} 
		}, 10000);
		// Actualizo a view de posição
//		sd = new Sender((WifiManager) this.context.getSystemService(Context.WIFI_SERVICE),mEnderecoIP, mPortaIP);
//		sd.startThread();
		TimerTask timerTask = new TimerTask() {		
			@Override
			public void run() {
				posicaoHandler.post(new Runnable() 
				{ 
					@Override
					public void run() { 
						listener.onInsEvent(tipoRetorno.posicao);
					}		
				});
			}
		}; 
		posicaoTimer.schedule(timerTask, 7000,2000);
		
		
//		if(mUdpsend){
//			//enviarUdp = new ServidorUdp(mEnderecoIP, mPortaIP);
////			//enviarUdp = new ServidorUdp(mEnderecoIP, mPortaIP);
//			
//			 Runnable runnable = new Runnable() {
//				   @Override
//				   public void run() {
//				      /* do what you need to do */
//					   String dadoscopy = dadosSensoresUdp;
//						dadosSensoresUdp = "";
//						new ServidorUdp(mEnderecoIP, mPortaIP).send(dadoscopy);
//				      /* and here comes the "trick" */
//				      handlerUdp.postDelayed(this, 1000);
//				   }
//				};
//				handlerUdp.postDelayed(runnable, 1000);
		if(mUdpsend){
		Timer timer = new Timer();
		udpTask = new TimerTask() {	
			@Override
			public void run() {
				
				{
					String dadoscopy = dadosSensoresUdp;
					dadosSensoresUdp = "";
					new ServidorUdp(mEnderecoIP, mPortaIP).send(dadoscopy);
					
				}
			}
		};
		// Estava 350,350 quando apenas um texto era enviado
		timer.schedule(udpTask, 175,175);
		}
//		
	}
	private float[] vetorCalibrado(float[] dadosacc_,float[] output)
	{
		for(int i=0;i<dadosacc_.length;i++)
		{
			output[i] = dadosacc_[i] * mCalibVetor_scale[i]-mCalibVetor_bias[i];
		}
		return output;
	}
	/**
	 * Interface para comunicar com a classe que implmenta esta interface e assim aceder aos callbacks efectuados
	 * O Enum é apenas para enumerar os diversos tipos de callback que podem existir.
	 * 
	 * @param <E>
	 */
	public interface OnInsChanged<E extends Enum<E>> {
		public void onInsEvent(E value);
	}


}
