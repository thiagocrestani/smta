package mei.tcd.smta.ins;

import mei.tcd.smta.util.Ops;

import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;
//import org.ejml.simple.SimpleMatrix;
import org.ejml.data.DenseMatrix64F;
import android.hardware.SensorManager;
import android.util.FloatMath;

// Leitura
// http://pt.scribd.com/doc/58403201/51/Direction-Cosine-Matrix-DCM-Representation
// http://www.flipcode.com/documents/matrfaq.html#Q36
// http://www.geometrictools.com/Documentation/EulerAngles.pdf 
//http://efficient-java-matrix-library.googlecode.com/svn-history/r307/javadoc/ver0.17/org/ejml/ops/NormOps.html
//http://efficient-java-matrix-library.googlecode.com/svn-history/r181/javadoc/ver0.14/org/ejml/ops/CommonOps.html

public class InsClass {
	// instancia algoritmo de detecção
	public StopDetection stopDetection = new StopDetection();
	// Operações
	private Ops opsins = new Ops();
	
	private static final float FILTRO_COEFICIENTE_GYRO = 0.98f; //O quanto queremos que os resultados se ajustema este
	// m_cosenos - m_cosenost (Guarda a matriz Direct Cosine Matrix e sua transposta (neste caso sempre igual à inversa))
	// accel - gyro (Valores para passar para DenseMatrix64F provenientes dos dados do vector aceleração e giroscopio)
	// posicao - velocidade - aceleracao - aceleracao_N (vetor poiscao,velocidade e aceleracao calculada)
	// m3identidade - (Matrix 3x3 Identidade)
	// rotacao - ( vetor rotacao do giroscopio)
	
	// Definição das matrizes e vetores
	private DenseMatrix64F mDcmeulerX=new DenseMatrix64F(3,3); //Matriz rotação para o eixo do X
	private DenseMatrix64F mDcmeulerY=new DenseMatrix64F(3,3); //Matriz rotação para o eixo do Y
	private DenseMatrix64F mDcmeulerZ=new DenseMatrix64F(3,3); //Matriz rotação para o eixo do Z
	
	
	// m_dcmGyro - Matrix DCM proveniente do calculo do giroscopio com inicialização do getrotationmatrix()
	private DenseMatrix64F m_dcmGyro=new DenseMatrix64F(3,3); // Matriz rotação DCM calculada com o giroscopio.
	private DenseMatrix64F mGyro=new DenseMatrix64F(3,1); //Guarda valores do giroscopio para o actualiza_VelocidadeGyro() e actualiza_Orientacao_Gyro()
	private DenseMatrix64F mPosicaoInicialGPS=new DenseMatrix64F(3,1); ///Vem da posição auferida pelo GPS(transfomação de wgs82->ecef->enu).
	private DenseMatrix64F mPosicao=new DenseMatrix64F(3,1); // Vetor posição no sistema de coordenadas global (ENU)
	private DenseMatrix64F mVelocidadeInicialGPS=new DenseMatrix64F(3,1); // Vetor posição anterior para medir distancia entre os ultimos pontos
	private DenseMatrix64F mVelocidade=new DenseMatrix64F(3,1);// Vetor velocidade no sistema de coordenadas global (ENU)
	private DenseMatrix64F mAceleracao=new DenseMatrix64F(3,1); // Vetor aceleração no sistema de coordenadas local (Dispositivo)
	private DenseMatrix64F mVelocidadeB=new DenseMatrix64F(3,1); // Vetor aceleração no sistema de coordenadas local (Dispositivo)
	private DenseMatrix64F m3identidade=new DenseMatrix64F(3,3); // Matriz identidade 3x3 para ser usado no calculo do DCM pelo giroscopio (GyroDcm())
	// Variaveis para calculo
	private DenseMatrix64F tempResult1=new DenseMatrix64F(3,3); // Resultado temporario
	private DenseMatrix64F tempResult2=new DenseMatrix64F(3,3); // Resultado temporario 2
	private DenseMatrix64F m_temp=new DenseMatrix64F(3,1); // array temporario
	private DenseMatrix64F m_tempdcm=new DenseMatrix64F(3,3); // DCM temporaria
	// Valor para ser usado como indicação nos filtros passa baixo e passa alto.
	
	private float[] m_dcm = new float[]{1,0,0,0,1,0,0,0,1}; // m_dcm - Matriz rotação usada para guardar valores do getrotationmatrix() dos Acc+Mag ou VetorRotacao
	private float[] m_dcmposicao = new float[]{1,0,0,0,1,0,0,0,1}; // m_dcm - Matriz rotação usada para guardar valores do getrotationmatrix() dos Acc+Mag ou VetorRotacao
	private float[] mAprAccMag=new float[3]; // Angulos Euler (Azimuth-yaw, Pitch, Roll) de Acelerometro e magnetometro
	private float[] mAprGyro=new float[]{0,0,0}; // Angulos Euler (Azimuth-yaw, Pitch, Roll) de giroscopio
	private float[] mAprRotVet=new float[]{0,0,0}; // Angulos Euler (Azimuth-yaw, Pitch, Roll) de giroscopio
	private float[] mAprFusao=new float[]{0,0,0}; // Angulos Euler (Azimuth-yaw, Pitch, Roll) da fusão (Filtro complementar)
	private float[] mAccLinear=new float[]{0,0,0};
		
	public boolean jaCalculouThreshold;
	public boolean jaAlinhou;
	public boolean jamudei = false;
	//#################### Para debug
	public DenseMatrix64F resultado=new DenseMatrix64F(3,1);
	public DenseMatrix64F mGravidade=new DenseMatrix64F(3,1);

	public InsClass()
	{
		this.jaCalculouThreshold = false;
		this.jaAlinhou = false;
		mGravidade.set(2,-SensorManager.GRAVITY_EARTH); //{0,0,-g} - Este vector servirá como base para calculo da variação do sistema de coordenadas disposito para navegação
		m3identidade = CommonOps.identity(3,3);
		//aceleracaoAnterior.zero();
		mAceleracao.zero();
		mVelocidade.zero();
		mPosicao.zero();// Aqui tenho de colocar a posicao inicial do GPS. Isso é efectuado com um setter
		mVelocidadeB.zero();
		m_dcmGyro = CommonOps.identity(3,3);
		mVelocidadeInicialGPS.zero();
	}
	/**
	 * ###################   Filtros passa baixo e passa alto   ####################
	 */
	/**
	 * Low Pass filter
	 *  alpha is calculated as t / (t + dT)
     *  with t, the low-pass filter's time-constant
     *  and dT, the event delivery rate
	 * Ingora grandes alterações no acelerometro
	 */
		public float lowPass(float current, float filtered, float alpha) {
			return alpha * current + (1.0f - alpha) * filtered;
		}
	/**
	 * High Pass filter
	 * Ingora o aceleromtro para pequenos valores
	 */
		float highPass(float current, float last, float filtered,  float alpha) {
			return alpha * (filtered + current - last);
				  //return current;
		}
	
	//----------------------------------- Inicio do processo de navegação inercial  ---------------------------------------
	
	/**
	 * Inicialização da posição do INS com uma posição do GPS. O GPS envia coordenadas LAT/LONG/ALT em WGS84.
	 * Tenho de transformar WGS84-> ECEF- > ENU (Plano tangente à terra)
	 * 
	 * @param _posicaoInicialGPS array Double proveniente da transformação WGS84-> ECEF- > ENU com classe estatica Coordenadas
	 */
	public void setPosicaoInicial(double[] _posicaoInicialGPS) {
		mPosicaoInicialGPS.set(0,_posicaoInicialGPS[0]);// Y-north-Latitude
		mPosicaoInicialGPS.set(1,_posicaoInicialGPS[1]);// X-east-Longitude
		mPosicaoInicialGPS.set(2,_posicaoInicialGPS[2]);
		mPosicao.set(mPosicaoInicialGPS);
	}
	/**
	 * Inicialização da posição do INS com uma posição do GPS. O GPS envia coordenadas LAT/LONG/ALT em WGS84.
	 * Tenho de transformar WGS84-> ECEF- > ENU (Plano tangente à terra)
	 * 
	 * @param _posicaoInicialGPS array Double proveniente da transformação WGS84-> ECEF- > ENU com classe estatica Coordenadas
	 */
	public void setVelocidadeInicial(double[] _VelocidadeInicialGPS) {
		mVelocidadeInicialGPS.set(0,_VelocidadeInicialGPS[0]);// X
		mVelocidadeInicialGPS.set(1,_VelocidadeInicialGPS[1]);// Y
		mVelocidadeInicialGPS.set(2,_VelocidadeInicialGPS[2]);// Z
		//mPosicao.set(mPosicaoInicialGPS);
	}
	/**
	 * Inicialização de matriz cosenos directores (m_dcm) para guardar valores provenientes do getRotationMatrix.
	 * Resulta numa matriz de 3x3 onde os valores representam os cosenos dos algulos entre os eixos do 
	 * referencial do dispositivo e do referencial de navegação (ENU).
	 * 
	 * @param dcm matrix resultante do método getRotationMatrix()
	 */
	public void set_dcm(float[] _m_dcm) {
		for (int i=0;i<9;i++) {
			m_dcm[i] = _m_dcm[i];
		}
	}
	
	
	/**
	 * Inicialização de matriz cosenos directores (m_dcmGyro) para guardar a matriz rotação calculada derivada da fusão do giroscopio com o ACC+MAG.
	 * Resulta numa matriz de 3x3 onde os valores representam os cosenos dos algulos entre os eixos do 
	 * referencial do dispositivo e do referencial de navegação.
	 * 
	 * @param dcm  Matriz rotação
	 */
	public void set_dcmGyro(float[] _m_dcmGyro) {
		for (int i=0;i<9;i++) {
			m_dcmGyro.set(i,_m_dcmGyro[i]);
		}
	}
	/**
	 * Retorna o vetor Azimuth, pitch  roll (Z, X, Y) referente ao getOrientation() obtido do acelerometro e magnetometro.
	 * 
	 * @return array float Azimuth, pitch  roll (Z, X, Y)
	 */
	public float[] getAprAccMag()
	{
		return  this.mAprAccMag;
	}
	/**
	 * Retorna o vetor Azimuth, pitch  roll (Z, X, Y) referente ao getOrientation() com o sensor Giroscopio.
	 * Vai ser usado para calcular o aprFusao.
	 * 
	 * @return array float Azimuth, pitch  roll (Z, X, Y)
	 */
	public float[] getAprGyro()
	{
		return  this.mAprGyro;
	}
	/**
	 * Retorna o vetor Azimuth, pitch  roll (Z, X, Y) referente ao getOrientation() com o sensor RotationVector e getRotationMatrixFromVetor().
	 * 
	 * 
	 * @return array float Azimuth, pitch  roll (Z, X, Y)
	 */
	public float[] getAprRotVet()
	{
		return  this.mAprRotVet;
	}
	/**
	 * Retorna o vetor Azimuth, pitch  roll (Z, X, Y) referente ao getOrientation() da fusão Acc+Mag+Gyro.
	 * 
	 * 
	 * @return array float Azimuth, pitch  roll (Z, X, Y)
	 */
	public float[] getAprFusao()
	{
		return  this.mAprFusao;
	}
	/**
	 * Retorna a posicao actual em ENU, pois a posição já não está no referencial inercial (Body)
	 * 
	 * @return array float posicao
	 */
	public float[] getPosicao()
	{
		return  new float[]{(float) mPosicao.get(0),(float) mPosicao.get(1),(float) mPosicao.get(2)};
	}
	/**
	 * O dispositivo pode ficar colocado da diagonal, logo tenho de devolver a velocidade como sendo o conjunto das duas componentes do vetor
	 * o Y e o Z, visto que o x está paralelo ao horizonte, ou ao eixo das rodas do veículo.
	 * 
	 * @return magnitude float da velocidade sobre os dois eixos. 
	 */
	public float getVelocidadeTotal()
	{
		return opsins.getMagnitude(new float[]{(float) mVelocidade.get(1),(float) mVelocidade.get(2)});
	}
	
	/**
	 * Calculo da velocidade linear. Removo a gravidade da aceleração aproximadamente fazendo aceleracao += dcm_T x gravidade. 
	 * Posso usar mais, pois o vetor gravidade está com sinal menos. 
	 * Com esta transformação, passo o vetor gravidade do referencial da terra para 
	 * o referencial do dispositivo elaborando o calculo de dcm_T*g e assim tenho o peso da gravidade no dispositivo.
	 * 
	 * @param dados array float valores do acelerometro
	 * @return array float da velocidade linear.
	 * 
	 */
	public float[] getAccLinear(float[] dados)
	{
		m_temp.set(0,dados[0]); //x
		m_temp.set(1,dados[1]); //y
		m_temp.set(2,dados[2]); //z
		// Dcm em float, tenho de passar para densematrix64f
		for (int i = 0;i<9;i++)
			m_tempdcm.set(i,m_dcm[i]);
		//m_temp = m_temp + m_tempdcm * gravidade
		CommonOps.multAddTransA(m_tempdcm, mGravidade, m_temp);
		for(int i=0;i<3;i++)
			mAccLinear[i] = (float)m_temp.get(i);
		//return new float[]{(float) m_temp.get(0),(float) m_temp.get(1),(float) m_temp.get(2)};
			return mAccLinear;
	}
	/**
	 * Calculo da velocidade.Integração numerica regra dos trapézios não funciona pois os intervalos de tempo não são constantes....
	 * 
	 * @param dados array float dados do meu acelerometro linear
	 * @param dt intervalo de tempo entre registos
	 */
	public void actualiza_Velocidade(float[] dados, float dt)
	{
		//aceleracaoAnterior.set(aceleracao); // Servirá para determinar a velocidade como (a0+a1/2) * dt trapezios
		mAceleracao.set(0,dados[0]); //x - East
		mAceleracao.set(1,dados[1]); //y - North
		mAceleracao.set(2,dados[2]); //z - Up
		
		// v1=v0 + dt*ac
		
		//v = v0 + v velocidade inicial 
		//CommonOps.addEquals(mVelocidade,mVelocidadeInicialGPS);
		//v = v + dt * acc
		// velocidade = velocidade + dt*aceleracao
		CommonOps.addEquals(mVelocidade, dt,mAceleracao); // ---> Esta é a que funciona
		//c = dt * a + b 
		//CommonOps.add(dt,mVelocidadeInicialGPS,mAceleracao ,mVelocidade); //--> v(t) = V0 + aT
		// Tenho de passar imediatamente a aceleração para ENU global referencial e só altera direcção de acelerelação variar
//		for (int i = 0;i<9;i++)
//			m_tempdcm.set(i,m_dcm[i]);
//		CommonOps.multAdd(dt,m_tempdcm,mAceleracao,mVelocidade);
//		jamudei=true;
		
	}
	/**
	 * Calculo da velocidade com integração do giroscópio.Integração numerica . 
	 * Aqui tenho de calcular a velocidade juntamente com os angulos do giroscopio. Velocidade em curva.
	 * Aceleração CENTRIPETA?? Visto que tenho a velocidade em Coordenadas Globais, tenho de passaar para locais para fazer a conta?
	 * Visto que um vetor tem direcção e magnitude, qualquer alteração na direcção, existe uma alteração na velocidade. Esta força chama-se força centripeta.
	 * 
	 * Mais, não temos e ter em atenção o efeito aceleração coriolis do movimento da terra, pois os efeitos são pequenos sobre pequenas diatância ou intervalos de tempo.
	 * 
	 * 
	 * @param dados array float dados do meu acelerometro linear
	 * @param dt intervalo de tempo entra registos
	 */
	public void actualiza_VelocidadeGyro(float[] dados, float dt)
	{
		mGyro.set(0, dados[0]);
		mGyro.set(1, dados[1]);
		mGyro.set(2, dados[2]);
		tempResult1 = getAntiSimetrica(mVelocidade);
	   
		//velocidade = velocidade+dt*tempResult1*gyro
		CommonOps.multAdd(dt, tempResult1, mGyro, mVelocidade); 
		
		
	}
	/**
	 * Calculo da posição. Já no referencial global pois multiplico pela DCM os valores da velocidade. 
	 * 
	 * @param dt
	 */
	public void actualiza_Posicao(float dt) {
		// Tenho de passar para global referencial a velocidade pois à medida que aumna, tenho de orientar a direcção
		for (int i = 0;i<9;i++)
			m_tempdcm.set(i,m_dcm[i]);
		// c = c + alpha * a * b
		//CommonOps.multAdd(dt,m_tempdcm,mVelocidade,mPosicao);
		//CommonOps.addEquals(posicao,posicaoInicialGPS);
		// Colocar a nova direcção na velocidade
		//c = a * b (mult)
		//a = a + beta * b 
//		if(jamudei){
//			CommonOps.addEquals(mPosicao,dt,mVelocidade);
//			jamudei=false;
//		}
//		else{
		//mPosicaoAnterior = mPosicao;
			CommonOps.multAdd(dt,m_tempdcm,mVelocidade,mPosicao);
			
	//	}
		
		//c = c + aT * b (transposta)
		//CommonOps.mult(m_tempdcm,mVelocidade,m_temp);
		//a = a + Beta * b 
		//CommonOps.addEquals(mPosicao, dt, m_temp);
		
	}
	/**
	 * Calculo da posição com gyro. TENHO DUVIDAS SE DEVEREI PASSAR TAMBEM ESTE VECTOR PARA GLOBAL ENU
	 * CENTRIPETA?? tenho de efectuar o mesmo à posição?
	 * @param dt
	 */
	public void actualiza_PosicaoGyro(float[] dados,float dt) {
		mGyro.set(0, dados[0]);
		mGyro.set(1, dados[1]);
		mGyro.set(2, dados[2]);
		tempResult1 = getAntiSimetrica(mPosicao); //Para poder efectuat o produto vectorial
    	//Update for the rotation (Pos_b=Pos_b+cross(Pos_b,gyro)
		//skew(Pos_b, mx_a);
		CommonOps.multAdd(dt, tempResult1, mGyro, mPosicao);
		
	}
	/**
	 * Actualiza o Azimuth, pitch e roll de acordo com o getrotationmatrix() e Acc+Mag
	 * 
	 *  
	 * @param dMag array float vetor dados do magnetometro
	 * @param dAcc array float vetor dados do acelerometro
	 * 
	 */
	public void calculaAccMagOrientacao(float[] dMag, float[] dAcc)
	{
		//azimuth_anterior = aprAccMag[0]; // Para calculo XYVIEW
		if(dMag!=null && dAcc!=null)
		{
			if(SensorManager.getRotationMatrix(m_dcm, null, dAcc, dMag)) {
				SensorManager.getOrientation(m_dcm, this.mAprAccMag);
				
			}
		}
	}
	/**
	 * Actualiza o Azimuth, pitch e roll de acordo com o getrotationmatrix() e sensor RotationVector
	 * 
	 *  
	 * @param dRotVet  array float vetor dados do RotationVector
	 * 
	 */
	public void calculaRotVetOrientacao(float[] dRotVet) {
		//azimuth_anterior = aprRotVet[0]; // Para calculo XYVIEW
		SensorManager.getRotationMatrixFromVector(m_dcm, dRotVet);
		//SensorManager.remapCoordinateSystem(m_dcm, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, m_dcmposicao);
		SensorManager.getOrientation(m_dcm, this.mAprRotVet);
		// Tenho de calcular o offset do azimuth calcuado para que aponte sempre para Y o inicio.
		
		
	}
	/**
	 * Actualiza o Azimuth, pitch e roll de acordo com o filtro complementar Acc+Mag+Gyro
	 * 
	 *  
	 * @param dGyro array float vetor dados do giroscopio
	 * @param dt intervalo de tempo entre o ultimo evento e o anterior
	 * 
	 */
	public void calculaFusaoOrientacao(float[] dGyro, float dt)
	{
		//azimuth_anterior = aprGyro[0]; // Para calculo XYVIEW
		float[] copyGyro = new float[3];
		float dt2 = dt;
		copyGyro = dGyro.clone(); //copiar os valores
		// Usa método para actualizar a DCM a matriz rotação do Gyro
		this.actualiza_Orientacao_Gyro(copyGyro,dt2);
		SensorManager.getOrientation(getDcmFloat3x3(m_dcmGyro), mAprGyro);
		calculaFusao();
	}
	/**
	 * Implementa o algoritmo do filtro complementar. http://web.mit.edu/scolton/www/filter.pdf
	 * Pega no resultado do vetor fusão aprfusao e acha a matriz rotação através dos angulos de Euler para actualizar a matriz rotação do giroscopio.
	 * Crio a DCM com a nova orientação. Compenso o drift do giroscopio com a fusão
	 * 
	 * 
	 */
	private void calculaFusao()
	{
		//float[] deltaRotationMatrix = new float[9];
		float FILTRO_COEFICIENTE_ACCMAG = 1.0f - FILTRO_COEFICIENTE_GYRO;
		//	mFusao[0] =
		//		FILTRO_COEFICIENTE_GYRO * aprGyro[0]
		//        + FILTRO_COEFICIENTE_ACCMAG * aprAccMag[0];
		//
		//	mFusao[1] =
		//		FILTRO_COEFICIENTE_GYRO * aprGyro[1]
		//        + FILTRO_COEFICIENTE_ACCMAG * aprAccMag[1];
		//
		//	mFusao[2] =
		//		FILTRO_COEFICIENTE_GYRO * aprGyro[2]
		//        + FILTRO_COEFICIENTE_ACCMAG * aprAccMag[2];
		//
		// Corrige problema de diferença entre a 179º e -179º medido em tempos diferentes por cada sensor
		// azimuth
		if (this.mAprGyro[0] < -0.5 * Math.PI && this.mAprAccMag[0] > 0.0) {
			mAprFusao[0] = (float) (FILTRO_COEFICIENTE_GYRO * (this.mAprGyro[0] + 2.0 * Math.PI) + FILTRO_COEFICIENTE_ACCMAG * this.mAprAccMag[0]);
			mAprFusao[0] -= (mAprFusao[0] > Math.PI) ? 2.0 * Math.PI : 0;
		}
		else if (this.mAprAccMag[0] < -0.5 * Math.PI && this.mAprGyro[0] > 0.0) {
			mAprFusao[0] = (float) (FILTRO_COEFICIENTE_GYRO * this.mAprGyro[0] + FILTRO_COEFICIENTE_ACCMAG * (this.mAprAccMag[0] + 2.0 * Math.PI));
			mAprFusao[0] -= (mAprFusao[0] > Math.PI)? 2.0 * Math.PI : 0;
		}
		else {
			mAprFusao[0] = FILTRO_COEFICIENTE_GYRO * this.mAprGyro[0] + FILTRO_COEFICIENTE_ACCMAG * this.mAprAccMag[0];
		}

		// pitch
		if (this.mAprGyro[1] < -0.5 * Math.PI && this.mAprAccMag[1] > 0.0) {
			mAprFusao[1] = (float) (FILTRO_COEFICIENTE_GYRO * (this.mAprGyro[1] + 2.0 * Math.PI) + FILTRO_COEFICIENTE_ACCMAG * this.mAprAccMag[1]);
			mAprFusao[1] -= (mAprFusao[1] > Math.PI) ? 2.0 * Math.PI : 0;
		}
		else if (this.mAprAccMag[1] < -0.5 * Math.PI && this.mAprGyro[1] > 0.0) {
			mAprFusao[1] = (float) (FILTRO_COEFICIENTE_GYRO * this.mAprGyro[1] + FILTRO_COEFICIENTE_ACCMAG * (this.mAprAccMag[1] + 2.0 * Math.PI));
			mAprFusao[1] -= (mAprFusao[1] > Math.PI)? 2.0 * Math.PI : 0;
		}
		else {
			mAprFusao[1] = FILTRO_COEFICIENTE_GYRO * this.mAprGyro[1] + FILTRO_COEFICIENTE_ACCMAG * this.mAprAccMag[1];
		}

		// roll
		if (this.mAprGyro[2] < -0.5 * Math.PI && this.mAprAccMag[2] > 0.0) {
			mAprFusao[2] = (float) (FILTRO_COEFICIENTE_GYRO * (this.mAprGyro[2] + 2.0 * Math.PI) + FILTRO_COEFICIENTE_ACCMAG * this.mAprAccMag[2]);
			mAprFusao[2] -= (mAprFusao[2] > Math.PI) ? 2.0 * Math.PI : 0;
		}
		else if (this.mAprAccMag[2] < -0.5 * Math.PI && this.mAprGyro[2] > 0.0) {
			mAprFusao[2] = (float) (FILTRO_COEFICIENTE_GYRO * this.mAprGyro[2] + FILTRO_COEFICIENTE_ACCMAG * (this.mAprAccMag[2] + 2.0 * Math.PI));
			mAprFusao[2] -= (mAprFusao[2] > Math.PI)? 2.0 * Math.PI : 0;
		}
		else {
			mAprFusao[2] = FILTRO_COEFICIENTE_GYRO * this.mAprGyro[2] + FILTRO_COEFICIENTE_ACCMAG * this.mAprAccMag[2];
		}
		// Crio a DCM com a nova orientação
		// compenso o drift do giroscopio com o fused
		

		//deltaRotationMatrix = ins.getDcmFromOrientation(mFusao);
		//Matrix.setRotateEulerM(deltaRotationMatrix,0,aprFusao[0],aprFusao[1],aprFusao[2]); 
		
		// SensorManager.getRotationMatrixFromVector(deltaRotationMatrix,deltaRotationMatrix);
		
		//Matrix.setRotateEulerM(deltaRotationMatrix,0,aprFusao[0],aprFusao[1],aprFusao[2]); 
		// ins.set_dcm(deltaRotationMatrix);
		//SensorManager.getRotationMatrixFromVector(R, rotationVector)
		
		//SensorManager.getRotationMatrixFromVector(deltaRotationMatrix,aprFusao);
		//this.set_dcmGyro(getDcmFromEuler(aprFusao));
		m_dcmGyro.set(getDcmFromEuler(mAprFusao));
		System.arraycopy(mAprFusao, 0, this.mAprGyro, 0, 3); //copia de elementos de array
		//actualizaOrientacao(mFusao);
	}
	
	/**
	 * Calcula a matriz do cosenos (matriz rotação) a partir dos angulos de Euler.
	 * http://www.geometrictools.com/Documentation/EulerAngles.pdf
	 * Algoritmo simplificado de conversão ordem de multiplicação Y-X-Z por causa da getOrientation (ao contrario)
	 * http://www.flipcode.com/documents/matrfaq.html#Q36
	 * 
	 * @param apr vetor dos angulos de euler (Azimuth, Pitch e Roll
	 * @return DenseMatrix64F matriz rotação
	 */
	  private DenseMatrix64F getDcmFromEuler(float[] apr) {
			     
	        float sinX = FloatMath.sin(apr[1]);
	        float cosX = FloatMath.cos(apr[1]);
	        float sinY = FloatMath.sin(apr[2]);
	        float cosY = FloatMath.cos(apr[2]);
	        float sinZ = FloatMath.sin(apr[0]);
	        float cosZ = FloatMath.cos(apr[0]);
	        // Rotação ao longo do X
	        mDcmeulerX.zero();
	        mDcmeulerX.set(0,1.0f);
	        mDcmeulerX.set(4,cosX);
	        mDcmeulerX.set(5,sinX);
	        mDcmeulerX.set(7,-sinX);
	        mDcmeulerX.set(8,cosX);
	        // Rotação ao longo do Y
	        mDcmeulerY.zero();
	        mDcmeulerY.set(0,cosY);
	        mDcmeulerY.set(2,sinY);
	        mDcmeulerY.set(4,1.0f);
	        mDcmeulerY.set(6,-sinY);
	        mDcmeulerY.set(8,cosY);
	        // Rotação ao longo do Z
	        mDcmeulerZ.zero();
	        mDcmeulerZ.set(0,cosZ);
	        mDcmeulerZ.set(1,sinZ);
	        mDcmeulerZ.set(3,-sinZ);
	        mDcmeulerZ.set(4,cosZ);
	        mDcmeulerZ.set(8,1.0f);
	       
	        CommonOps.mult(mDcmeulerX, mDcmeulerY, tempResult1);
	        CommonOps.mult(mDcmeulerZ,tempResult1, tempResult2);
	        
	        return tempResult2;

	    }
	    
	   
	/**
	 * Actualiza a orientação de acordo com a DCM em
	 * http://pt.scribd.com/doc/58403201/51/Direction-Cosine-Matrix-DCM-Representation
	 *  
	 * @param dados dados provenientes do giroscopio
	 * @param dt intervalo de tempo entre obervações
	 * 
	 */
	public void actualiza_Orientacao_Gyro(float[] dadosG, float dt) {
		mGyro.set(0,dadosG[0]*dt);
		mGyro.set(1,dadosG[1]*dt);
		mGyro.set(2,dadosG[2]*dt);
		gyroDcm(mGyro); //calculo nova DCM
		
	}
	
	/**
	 * Processo de discretização para actualização da DCM
	 * http://pt.scribd.com/doc/58403201/51/Direction-Cosine-Matrix-DCM-Representation
	 * 
	 * 1º Calcular a matriz anti-simetrica para facilitar o calculo do produto vetorial ou soma A-1 = AT do vetor rotação
	 * 2º Determinar a magnitude do vetor (SQRT(m_rotacao[0]*m_rotacao[0] + m_rotacao[1]*m_rotacao[1]+m_rotacao[2]*m_rotacao[2]))
	 * 3º Calcular os coeficientes da series
	 * 4º Actualizar a matriz DCM
	 * 
	 * @param rotacao vetor integrado no tempo
	 * 
	 */
	private void gyroDcm(DenseMatrix64F rotacao) {
		DenseMatrix64F tempResult=new DenseMatrix64F(3,3);
		DenseMatrix64F tempSimetrica=new DenseMatrix64F(3,3);
		DenseMatrix64F CalcB=new DenseMatrix64F(3,3);
		DenseMatrix64F CalcA=new DenseMatrix64F(3,3);
		tempSimetrica = getAntiSimetrica(rotacao);
		double rot_norm=NormOps.fastNormF(rotacao);
		// I + (serie_Alpha x  tempSimetrica) + (serie_Beta x  tempSimetrica^2)
		// I +              calcA			  +            CalcB  
		// I + 						calcA
		// tempResult
		// mcosenos x tempresult
		// Se a magnitude do vetor for maior que zero, então efectuar a operação
		if (rot_norm>0) {
			// Determinar os coeficientes das series
			double serie_Alpha=Math.sin(rot_norm)/rot_norm;
			double serie_Beta=(1-Math.cos(rot_norm))/(rot_norm*rot_norm);
			// CalcB = tempSimetrica^2;
			CommonOps.mult(tempSimetrica,tempSimetrica,CalcB);
			// CalcB = B x CalcB;
			CommonOps.scale(serie_Beta, CalcB, CalcB);
			// CalcA = A x tempSimetrica;
			CommonOps.scale(serie_Alpha, tempSimetrica, CalcA);
			// CalcA = CalcA + CalcB
			CommonOps.addEquals(CalcA,CalcB);
			// tempResult = CalaA
			CommonOps.add(m3identidade, CalcA,tempResult);
			// TempDCM = m_dcmGyro x tempResult
			CommonOps.mult(m_dcmGyro, tempResult, m_tempdcm);
			m_dcmGyro.set(m_tempdcm);
			
			
		}
		
	}
		
	/**
	 * Constroi a matriz antisimétrica cujo A-1=A_T de um vetor 3x1 para facilitar o calculo com matrizes
	 *  	
	 * @param vetor
	 * @return DenseMatrix64F
	 */
	private DenseMatrix64F  getAntiSimetrica(DenseMatrix64F vetor) {
		DenseMatrix64F tempAntisimetrica=new DenseMatrix64F(3,3);
		tempAntisimetrica.zero();
		tempAntisimetrica.set(0,1,-vetor.get(2));
		tempAntisimetrica.set(0,2,vetor.get(1));
		tempAntisimetrica.set(1,0,vetor.get(2));
		tempAntisimetrica.set(1,2,-vetor.get(0));
		tempAntisimetrica.set(2,0,-vetor.get(1));
		tempAntisimetrica.set(2,1,vetor.get(0));
		return tempAntisimetrica;
	}
	//---------------------------------------- Funções Utilitarias e de DEBUG ---------------------------------------------
	/**
	 * Pega num vetor em float[] ,passa-o para DenseMatrix64F e transforma-o no referencial de navegação multiplicando pela matriz DCM.
	 * 
	 * @param vetor Float[]
	 * @return retval float[]
	 */
	public float[] getWorldVector(float[] vetor)
	{
		float[] retval = new float[3];
		DenseMatrix64F tempResult = new DenseMatrix64F(3,1);
		DenseMatrix64F tempVetor = new DenseMatrix64F(3,1);
		// Dcm em float, tenho de passar para densematrix64f
		for (int i = 0;i<9;i++)
			m_tempdcm.set(i,m_dcm[i]);
		//vetor em float, tenho de passar para densmatrix
		tempVetor=this.getFloatToDense3x1(vetor);
		//CommonOps.transpose(m_cosenos, m_cosenost);
		CommonOps.mult(m_tempdcm, tempVetor, tempResult);
		for(int i = 0;i<3;i++)
			retval[i] = (float) tempVetor.get(i);
		return retval;

	}
	/**
	 * Pega num vetor 3x3 em DenseMatrix64F e transforma-o em Float.
	 * 
	 * @param vetor DenseMatrix64F
	 * @return retval float[]
	 */
	public float[] getDcmFloat3x3(DenseMatrix64F vetor)
	{
		float[] retval = new float[9];
		for(int i=0;i<9;i++)
			retval[i]=(float)vetor.get(i);
		return retval;
	}
	/**
	 * Pega num vetor 3x1 em DenseMatrix64F e transforma-o em Float.
	 * 
	 * @param vetor DenseMatrix64F
	 * @return retval float[]
	 */
	public float[] getDcmFloat3x1(DenseMatrix64F vetor)
	{
		float[] retval = new float[3];
		for(int i=0;i<3;i++)
			retval[i]=(float)vetor.get(i);
		return retval;
	}
	/**
	 * Pega num vetor 3x3 em float[] e transforma-o em DenseMatrix64F.
	 * 
	 * @param vetor float[]
	 * @return retval DenseMatrix64F
	 */
	public DenseMatrix64F getFloatToDense3x3(float[] vetor)
	{
		
		for(int i=0;i<9;i++)
			m_tempdcm.set(i,vetor[i]);
		return m_tempdcm;
	}
	/**
	 * Pega num vetor 3x1 em float[] e transforma-o em DenseMatrix64F.
	 * 
	 * @param vetor float[]
	 * @return retval DenseMatrix64F
	 */
	public DenseMatrix64F getFloatToDense3x1(float[] vetor)
	{
		
		for(int i=0;i<3;i++)
			m_temp.set(i,vetor[i]);
		return m_temp;
	}
	
	//-----------------------------  Algoritmo para calculo threshold comparativo  ---------------------------------
	 
	class StopDetection
	{
		// Arrays temporários para guardar amostras de velores para se efctuar o calculo da variancia
		// tendo em conta que em média serão efectuadas leituras a 30 hz, então 30 leituras serão suficientes para calcular a variancia e a variancia nova
		
		private static final int NAMOSTRAS = 5;
		private float[] tempAccelX=new float[NAMOSTRAS];
		private float[] tempAccelY=new float[NAMOSTRAS];
		private float[] tempAccelZ=new float[NAMOSTRAS];
		private int tempIndice = 0;
		
		// Variavel claculada pela variancia de valores
		public float mThresholdAccelX =0;
		public float mThresholdAccelY =0;
		public float mThresholdAccelZ =0;
		public float mThresholdAccelNewX =0;
		public float mThresholdAccelNewY =0;
		public float mThresholdAccelNewZ =0;
		public boolean velPositivaY = true;

		// Limpar arrays
		public void limparArrays() { 
			for(int j=0;j<StopDetection.NAMOSTRAS;j++)
			{
				this.tempAccelX[j] = 0;
				this.tempAccelY[j] = 0;
				this.tempAccelZ[j] = 0;
			}
			this.tempIndice = 0;
			
		}
		// Adiciona para o array de calculo da variancia
		public void adicionaAcc(float[] dat)
		{
			if(this.tempIndice<StopDetection.NAMOSTRAS)
			{
				this.tempAccelX[tempIndice] = dat[0];
				this.tempAccelY[tempIndice] = dat[1];
				this.tempAccelZ[tempIndice] = dat[2];
				this.tempIndice ++;

			}else
				// calcula a variancia
			{

				if(!jaCalculouThreshold)
				{
					//this.thresholdAccel = opsins.getVariancia(tempMagAccel);
					this.mThresholdAccelX = opsins.getDesvioPadrao(opsins.getVariancia(tempAccelX));
					this.mThresholdAccelY = opsins.getDesvioPadrao(opsins.getVariancia(tempAccelY));
					this.mThresholdAccelZ = opsins.getDesvioPadrao(opsins.getVariancia(tempAccelZ));
					jaCalculouThreshold = true;
				}
				else
				{
					if(opsins.getMedia(tempAccelY)>0)
					{
						velPositivaY = true;
					}
					else
					{
						velPositivaY = false;
					}
					//this.thresholdAccelNew = opsins.getVariancia(tempMagAccel);
					this.mThresholdAccelNewX = opsins.getDesvioPadrao(opsins.getVariancia(tempAccelX));
					this.mThresholdAccelNewY = opsins.getDesvioPadrao(opsins.getVariancia(tempAccelY));
					this.mThresholdAccelNewZ = opsins.getDesvioPadrao(opsins.getVariancia(tempAccelZ));
					
				}
				this.limparArrays();
			}

		}
		
	}
	
		
	
}
