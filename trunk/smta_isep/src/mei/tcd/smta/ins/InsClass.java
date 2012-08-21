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
	// instancia algoritmo de detec��o
	public StopDetection stopDetection = new StopDetection();
	// Opera��es
	private Ops opsins = new Ops();
	// Defini��o das matrizes e vetores
	private static final float FILTRO_COEFICIENTE_GYRO = 0.98f; //O quanto queremos que os resultados se ajustema este
	// m_cosenos - m_cosenost (Guarda a matriz Direct Cosine Matrix e sua transposta (neste caso sempre igual � inversa))
	// accel - gyro (Valores para passar para DenseMatrix64F provenientes dos dados do vector acelera��o e giroscopio)
	// posicao - velocidade - aceleracao - aceleracao_N (vetor poiscao,velocidade e aceleracao calculada)
	// m3identidade - (Matrix 3x3 Identidade)
	// rotacao - ( vetor rotacao do giroscopio)
	private DenseMatrix64F dcmeulerX=new DenseMatrix64F(3,3); //9
	private DenseMatrix64F dcmeulerY=new DenseMatrix64F(3,3); //9
	private DenseMatrix64F dcmeulerZ=new DenseMatrix64F(3,3); //9
	// Variaveis para calculo
	private DenseMatrix64F tempResult1=new DenseMatrix64F(3,3); //
	private DenseMatrix64F tempResult2=new DenseMatrix64F(3,3); //
	
	// m_dcmGyro - Matrix DCM proveniente do calculo do giroscopio com inicializa��o do getrotationmatrix
	public DenseMatrix64F m_dcmGyro=new DenseMatrix64F(3,3); //9
	public DenseMatrix64F accel=new DenseMatrix64F(3,1);
	public DenseMatrix64F gyro=new DenseMatrix64F(3,1);
	public DenseMatrix64F posicao=new DenseMatrix64F(3,1);
	public DenseMatrix64F posicao_n=new DenseMatrix64F(3,1); //vetor transformado para referencia de navega�ao
	public DenseMatrix64F posicaoAnterior=new DenseMatrix64F(3,1);
	public DenseMatrix64F velocidade=new DenseMatrix64F(3,1);//vetor transformado para referencia de navega�ao
	public DenseMatrix64F velocidade_n=new DenseMatrix64F(3,1);
	//public DenseMatrix64F velocidadeAnterior=new DenseMatrix64F(3,1); //m�todo dos trapezios n�o funciona em intervalos de tempo inconstantes
	public DenseMatrix64F aceleracao=new DenseMatrix64F(3,1);
	//public DenseMatrix64F aceleracaoAnterior=new DenseMatrix64F(3,1); //m�todo dos trapezios n�o funciona em intervalos de tempo inconstantes
	
	public DenseMatrix64F m3identidade=new DenseMatrix64F(3,3);
	public DenseMatrix64F m_rotacao=new DenseMatrix64F(3,1); // Para ser usado no calculo do giroscopio
	public DenseMatrix64F m_temp=new DenseMatrix64F(3,1);
	public DenseMatrix64F m_tempdcm=new DenseMatrix64F(3,3);
	// m_dcm - Matriz rota��o usada para guardar valores do getrotationmatrix() dos Acc+Mag ou VetorRotacao
	public float[] m_dcm = new float[]{1,0,0,0,1,0,0,0,1};
	public float[] aprAccMag=new float[3]; // Angulos Euler (Azimuth-yaw, Pitch, Roll) de Acelerometro e magnetometro
	public float[] aprGyro=new float[]{0,0,0};; // Angulos Euler (Azimuth-yaw, Pitch, Roll) de giroscopio
	public float[] aprRotVet=new float[]{0,0,0};; // Angulos Euler (Azimuth-yaw, Pitch, Roll) de giroscopio
	public float[] aprFusao=new float[]{0,0,0};; // Angulos Euler (Azimuth-yaw, Pitch, Roll) da fus�o (Filtro complementar)
	 // Para guardar valores anteriores do calculo do x e y para plot
	public float[] arrayXYVew = new float[]{0,0,0}; // x, y, azimuth
	public float azimuth_anterior ;
	
	public boolean jaCalculouThreshold;
	public boolean jaAlinhou;
	
	//#################### Para debug
	public DenseMatrix64F resultado=new DenseMatrix64F(3,1);
	public DenseMatrix64F gravidade=new DenseMatrix64F(3,1);

	public InsClass()
	{
		this.jaCalculouThreshold = false;
		this.jaAlinhou = false;
		gravidade.set(2,-SensorManager.GRAVITY_EARTH); //{0,0,g} - Este vector servir� como base para calculo da varia��o do sistema de coordenadas disposito para navega��o
		m3identidade = CommonOps.identity(3,3);
		//aceleracaoAnterior.zero();
		aceleracao.zero();
		velocidade.zero();
		
		azimuth_anterior = 0;
		posicao.zero();
		m_dcmGyro = CommonOps.identity(3,3);
	}

	
	//----------------------------------- Inicio do processo de navega��o inercial  ---------------------------------------
	/**
	 * Inicializa��o de matriz cosenos directores (m_dcm) para guardar valores dos getRotationMatrix
	 * Resulta numa matriz de 3x3 onde os valores representam os cosenos dos algulos entre os eixos do 
	 * referencial do dispositivo e do referencial de navega��o.
	 * 
	 * @param dcm matrix resultante do m�todo getRotationMatrix()
	 */
	public void set_dcm(float[] dcm) {
		for (int i=0;i<9;i++) {
			m_dcm[i] = dcm[i];
		}
	}
	
	
	/**
	 * Inicializa��o de matriz cosenos directores (m_dcmGyro) para guardar a matriz rota��o calculada derivada do giroscopio
	 * Resulta numa matriz de 3x3 onde os valores representam os cosenos dos algulos entre os eixos do 
	 * referencial do dispositivo e do referencial de navega��o.
	 * 
	 * @param dcm  Matriz rota��o
	 */
	public void set_dcmGyro(float[] dcm) {
		for (int i=0;i<9;i++) {
			m_dcmGyro.set(i,dcm[i]);
		}
	}
	// N�o Uso ainda-----------------------------------------------------------
	/**
	 * vai buscar o vetor posi��o no referencial de navega��o
	 * a m_dcm � sempre a actual, seja que tipo de orienta��o estiver a ser usado, mesmo a fusa� pois usa os dois m_dcm e mdcmGyro
	 * Pnavegacao = DCM * Pinercial
	 * 
	 */
	public float[] getPosicao_N()
	{
		CommonOps.mult(getFloatToDense3x3(m_dcm),posicao,posicao_n);
		
		return  new float[]{(float) posicao_n.get(0),(float) posicao_n.get(1),(float) posicao_n.get(2)};
	}
//	/**
//	 * vai buscar o vetor posi��o no referencial de dispositivo
//	 * a m_dcm � sempre a actual, seja que tipo de orienta��o estiver a ser usado.
//	 * Pnavegacao = DCM * Pinercial
//	 * 
//	 */
//	public float[] getPosicao_D()
//	{
//		for(int i=0;i<3;i++)
//			m_temp_array3[i] = (float) posicao.get(i);
//		
//		return m_temp_array3;
//	}
	/**
	 * vai buscar o vetor velocidade no referencial de navega��o
	 * a m_dcm � sempre a actual, seja que tipo de orienta��o estiver a ser usado. Uso esta apenas para testar se o sinal da velocidade varia consoante a orienta��o global
	 * Vnavegacao = DCM * Vinercial
	 * 
	 */
	public float[] getVelocidade_N()
	{
		CommonOps.mult(getFloatToDense3x3(m_dcm),velocidade,velocidade_n);
		// para passar para float
		return  new float[]{(float) velocidade_n.get(0),(float) velocidade_n.get(1),(float) velocidade_n.get(2)};
	}
	/**
	 * O dispositivo pode ficar colocado da diagonal, logo tenho de devolver a velodidade como sendo o conjunto das duas compoenentes do vetor
	 * o Y e o Z, visto que o x est� paralelo ao horizonte.
	 */
	public float getVelocidadeTotal()
	{
		return opsins.getMagnitude(new float[]{(float) velocidade.get(1),(float) velocidade.get(2)});
	}
	/**
	 * Vai estipular o x e o y para efectuar o plot na view.
	 * O arrayXYVew guarda o valor anterior.
	 * Usa formula de x=vxcos(azimuth) e y=vxsin(azimuth).
	 */
	public float[] getXYView()
	{
		float[] retval = new float[3];
		retval[0] = (float) (arrayXYVew[0] +  ((getVelocidadeTotal() )*Math.cos(azimuth_anterior)));
		retval[1] = (float) (arrayXYVew[1] +  ((getVelocidadeTotal() )*Math.sin(azimuth_anterior)));
		retval[2] = getVelocidadeTotal(); // J� n�o precisa, mas fica para j�
		arrayXYVew[0] = retval[0];
		arrayXYVew[1] = retval[1];
		return retval;
	}
	/**
	 * Calculo da velocidade linear. Removo a gravidade da acelera��o aproximadamente fazendo aceleracao += dcm_T x gravidade
	 * Com esta transforma��o, passo o vetor gravidade do referencial da terra para 
	 * o referencial do dispositivo elaborando o calculo de dcm_T*g e assim tenho o peso da gravidade no dispositivo
	 * 
	 * @param dados valors do acelerometro
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
		
		CommonOps.multAddTransA(m_tempdcm, gravidade, m_temp);
		return new float[]{(float) m_temp.get(0),(float) m_temp.get(1),(float) m_temp.get(2)};
	}
	/**
	 * Calculo da velocidade.Integra��o numerica regra dos trap�zios n�o funciona pois os intervalos de tempo n�o s�o constantes....
	 * 
	 * @param dados dados do meu acelerometro linear
	 * @param dt intervalo de tempo entra registos
	 */
	public void actualiza_Velocidade(float[] dados, float dt)
	{
		//aceleracaoAnterior.set(aceleracao); // Servir� para determinar a velocidade como (a0+a1/2) * dt trapezios
		aceleracao.set(0,dados[0]); //x
		aceleracao.set(1,dados[1]); //y
		aceleracao.set(2,dados[2]); //z
		CommonOps.addEquals(velocidade, dt,aceleracao);
		
		
	}
	/**
	 * Calculo da posi��o. 
	 * @param dt
	 */
	public void actualiza_Posicao(float dt) {
		//Actualiza posicao com a velocidade (posicao=posicao+dt*velocidade)
		posicaoAnterior.set(posicao) ;
		CommonOps.addEquals(posicao, dt, velocidade);
		
	}
	/**
	 * Actualiza o Azimuth, pitch e roll de acordo com o getrotationmatrix() e Acc+Mag
	 * 
	 *  
	 * @param dMag vetor dados do magnetometro
	 * @param dAcc vetor dados do acelerometro
	 * 
	 */
	public void calculaAccMagOrientacao(float[] dMag, float[] dAcc)
	{
		azimuth_anterior = aprAccMag[0]; // Para calculo XYVIEW
		if(dMag!=null && dAcc!=null)
		{
			if(SensorManager.getRotationMatrix(m_dcm, null, dAcc, dMag)) {
				SensorManager.getOrientation(m_dcm, this.aprAccMag);
			}
		}
	}
	/**
	 * Actualiza o Azimuth, pitch e roll de acordo com o getrotationmatrix() e sensor RotationVector
	 * 
	 *  
	 * @param dRotVet vetor dados do RotationVector
	 * 
	 */
	public void calculaRotVetOrientacao(float[] dRotVet) {
		azimuth_anterior = aprRotVet[0]; // Para calculo XYVIEW
		SensorManager.getRotationMatrixFromVector(m_dcm, dRotVet);
		SensorManager.getOrientation(m_dcm, this.aprRotVet);
	}
	/**
	 * Actualiza o Azimuth, pitch e roll de acordo com o nosso filtro complementar Acc+Mag+Gyro
	 * 
	 *  
	 * @param dGyro vetor dados do giroscopio
	 * @param dt intervalo de tempo entre o ultimo evento e o anterior
	 * 
	 */
	public void calculaFusaoOrientacao(float[] dGyro, float dt)
	{
		azimuth_anterior = aprGyro[0]; // Para calculo XYVIEW
		float[] copyGyro = new float[3];
		float dt2 = dt;
		copyGyro = dGyro.clone();
		// Usa m�todo para actualizar a DCM a matriz rota��o do Gyro
		this.actualiza_Orientacao_Gyro(copyGyro,dt2);
		SensorManager.getOrientation(getDcmFloat3x3(m_dcmGyro), aprGyro);
		calculaFusao();
	}
	/**
	 * Implementa o algoritmo do filtro complementar. http://web.mit.edu/scolton/www/filter.pdf
	 * Pega no resultado do vetor fus�o aprfusao e acha a matriz rota��o para actualizar a matriz rota��o do giroscopio.
	 * Crio a DCM com a nova orienta��o. Compenso o drift do giroscopio com o fused
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
		// Corrige problema de diferen�a entre a 179� e -179� medido em tempos diferentes por cada sensor
		// azimuth
		if (this.aprGyro[0] < -0.5 * Math.PI && this.aprAccMag[0] > 0.0) {
			aprFusao[0] = (float) (FILTRO_COEFICIENTE_GYRO * (this.aprGyro[0] + 2.0 * Math.PI) + FILTRO_COEFICIENTE_ACCMAG * this.aprAccMag[0]);
			aprFusao[0] -= (aprFusao[0] > Math.PI) ? 2.0 * Math.PI : 0;
		}
		else if (this.aprAccMag[0] < -0.5 * Math.PI && this.aprGyro[0] > 0.0) {
			aprFusao[0] = (float) (FILTRO_COEFICIENTE_GYRO * this.aprGyro[0] + FILTRO_COEFICIENTE_ACCMAG * (this.aprAccMag[0] + 2.0 * Math.PI));
			aprFusao[0] -= (aprFusao[0] > Math.PI)? 2.0 * Math.PI : 0;
		}
		else {
			aprFusao[0] = FILTRO_COEFICIENTE_GYRO * this.aprGyro[0] + FILTRO_COEFICIENTE_ACCMAG * this.aprAccMag[0];
		}

		// pitch
		if (this.aprGyro[1] < -0.5 * Math.PI && this.aprAccMag[1] > 0.0) {
			aprFusao[1] = (float) (FILTRO_COEFICIENTE_GYRO * (this.aprGyro[1] + 2.0 * Math.PI) + FILTRO_COEFICIENTE_ACCMAG * this.aprAccMag[1]);
			aprFusao[1] -= (aprFusao[1] > Math.PI) ? 2.0 * Math.PI : 0;
		}
		else if (this.aprAccMag[1] < -0.5 * Math.PI && this.aprGyro[1] > 0.0) {
			aprFusao[1] = (float) (FILTRO_COEFICIENTE_GYRO * this.aprGyro[1] + FILTRO_COEFICIENTE_ACCMAG * (this.aprAccMag[1] + 2.0 * Math.PI));
			aprFusao[1] -= (aprFusao[1] > Math.PI)? 2.0 * Math.PI : 0;
		}
		else {
			aprFusao[1] = FILTRO_COEFICIENTE_GYRO * this.aprGyro[1] + FILTRO_COEFICIENTE_ACCMAG * this.aprAccMag[1];
		}

		// roll
		if (this.aprGyro[2] < -0.5 * Math.PI && this.aprAccMag[2] > 0.0) {
			aprFusao[2] = (float) (FILTRO_COEFICIENTE_GYRO * (this.aprGyro[2] + 2.0 * Math.PI) + FILTRO_COEFICIENTE_ACCMAG * this.aprAccMag[2]);
			aprFusao[2] -= (aprFusao[2] > Math.PI) ? 2.0 * Math.PI : 0;
		}
		else if (this.aprAccMag[2] < -0.5 * Math.PI && this.aprGyro[2] > 0.0) {
			aprFusao[2] = (float) (FILTRO_COEFICIENTE_GYRO * this.aprGyro[2] + FILTRO_COEFICIENTE_ACCMAG * (this.aprAccMag[2] + 2.0 * Math.PI));
			aprFusao[2] -= (aprFusao[2] > Math.PI)? 2.0 * Math.PI : 0;
		}
		else {
			aprFusao[2] = FILTRO_COEFICIENTE_GYRO * this.aprGyro[2] + FILTRO_COEFICIENTE_ACCMAG * this.aprAccMag[2];
		}
		// Crio a DCM com a nova orienta��o
		// compenso o drift do giroscopio com o fused
		

		//deltaRotationMatrix = ins.getDcmFromOrientation(mFusao);
		//Matrix.setRotateEulerM(deltaRotationMatrix,0,aprFusao[0],aprFusao[1],aprFusao[2]); 
		
		// SensorManager.getRotationMatrixFromVector(deltaRotationMatrix,deltaRotationMatrix);
		
		//Matrix.setRotateEulerM(deltaRotationMatrix,0,aprFusao[0],aprFusao[1],aprFusao[2]); 
		// ins.set_dcm(deltaRotationMatrix);
		//SensorManager.getRotationMatrixFromVector(R, rotationVector)
		
		//SensorManager.getRotationMatrixFromVector(deltaRotationMatrix,aprFusao);
		//this.set_dcmGyro(getDcmFromEuler(aprFusao));
		m_dcmGyro.set(getDcmFromEuler(aprFusao));
		System.arraycopy(aprFusao, 0, this.aprGyro, 0, 3);
		//actualizaOrientacao(mFusao);
	}
	
	 // Algoritmo simplificado de convers�o ordem de multiplica��o y,x,z
	 // http://www.flipcode.com/documents/matrfaq.html#Q36
	// http://www.geometrictools.com/Documentation/EulerAngles.pdf
	/**
	 * Calcula a matriz do cosenos (matriz rota��o) a partir dos angulos de Euler.
	 * http://www.geometrictools.com/Documentation/EulerAngles.pdf
	 * Algoritmo simplificado de convers�o ordem de multiplica��o Y-X-Z por causa da getOrientation (ao contrario)
	 * http://www.flipcode.com/documents/matrfaq.html#Q36
	 * 
	 * @param apr vetor dos angulos de euler (Azimuth, Pitch e Roll
	 */
	  private DenseMatrix64F getDcmFromEuler(float[] apr) {
			     
	        float sinX = FloatMath.sin(apr[1]);
	        float cosX = FloatMath.cos(apr[1]);
	        float sinY = FloatMath.sin(apr[2]);
	        float cosY = FloatMath.cos(apr[2]);
	        float sinZ = FloatMath.sin(apr[0]);
	        float cosZ = FloatMath.cos(apr[0]);
	        // Rota��o ao longo do X
	        dcmeulerX.zero();
	        dcmeulerX.set(0,1.0f);
	        dcmeulerX.set(4,cosX);
	        dcmeulerX.set(5,sinX);
	        dcmeulerX.set(7,-sinX);
	        dcmeulerX.set(8,cosX);
	        // Rota��o ao longo do Y
	        dcmeulerY.zero();
	        dcmeulerY.set(0,cosY);
	        dcmeulerY.set(2,sinY);
	        dcmeulerY.set(4,1.0f);
	        dcmeulerY.set(6,-sinY);
	        dcmeulerY.set(8,cosY);
	        // Rota��o ao longo do Z
	        dcmeulerZ.zero();
	        dcmeulerZ.set(0,cosZ);
	        dcmeulerZ.set(1,sinZ);
	        dcmeulerZ.set(3,-sinZ);
	        dcmeulerZ.set(4,cosZ);
	        dcmeulerZ.set(8,1.0f);
	       
	        CommonOps.mult(dcmeulerX, dcmeulerY, tempResult1);
	        CommonOps.mult(dcmeulerZ,tempResult1, tempResult2);
	        
	        return tempResult2;

	    }
	    
	   
	/**
	 * Actualiza a orienta��o de acordo com a actualiza��o DCM em
	 * http://pt.scribd.com/doc/58403201/51/Direction-Cosine-Matrix-DCM-Representation
	 *  
	 * @param dados dados provenientes do giroscopio
	 * @param dt intervalo de tempo entre oberva��es
	 * 
	 */
	public void actualiza_Orientacao_Gyro(float[] dadosG, float dt) {
		m_rotacao.set(0,dadosG[0]*dt);
		m_rotacao.set(1,dadosG[1]*dt);
		m_rotacao.set(2,dadosG[2]*dt);
		gyroDcm(m_rotacao); //calculo nova DCM
		
	}
	
	/**
	 * Processo de discretiza��o
	 * http://pt.scribd.com/doc/58403201/51/Direction-Cosine-Matrix-DCM-Representation
	 * 
	 * 1� Calcular a matriz anti-simetrica para facilitar o calculo do produto vetorial ou soma A-1 = AT do vetor rota��o
	 * 2� Determinar a magnitude do vetor (SQRT(m_rotacao[0]*m_rotacao[0] + m_rotacao[1]*m_rotacao[1]+m_rotacao[2]*m_rotacao[2]))
	 * 3� Calcular os coeficientes da series
	 * 4� Actualizar a matriz DCM
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
		// Se a magnitude do vetor for maior que zero, ent�o efectuar a opera��o
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
	 * Constroi a matriz antisim�trica cujo A-1=A_T de um vetor 3x1 para facilitar o calculo com matrizes
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
	//---------------------------------------- Fun��es Utilitarias ---------------------------------------------
	/**
	 * Pega num vetor em float[] ,passa-o para DenseMatrix64F e transforma-o no referencial de navega��o multiplicando pela matriz DCM.
	 * 
	 * @param vetor Float[]
	 * @return retval float[]
	 */
	public float[] getWorldVector(float[] vetor)
	{
		float[] retval = new float[3];
		DenseMatrix64F tempResult = new DenseMatrix64F(3,1);
		DenseMatrix64F tempVetor = new DenseMatrix64F(3,1);
		for(int i = 0;i<3;i++)
			tempVetor.set(i,vetor[i]);
		//CommonOps.transpose(m_cosenos, m_cosenost);
		CommonOps.mult(m_dcmGyro, tempVetor, tempResult);
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
		// Arrays tempor�rios para guardar amostras de velores para se efctuar o calculo da variancia
		// tendo em conta que em m�dia ser�o efectuadas leituras a 30 hz, ent�o 30 leituras ser�o suficientes para calcular a variancia e a variancia nova
		
		private static final int NAMOSTRAS = 5;
		private float[] tempAccelX=new float[NAMOSTRAS];
		private float[] tempAccelY=new float[NAMOSTRAS];
		private float[] tempAccelZ=new float[NAMOSTRAS];
		private int tempIndice = 0;
		
		// Variavel claculada pela variancia de valores
		public float thresholdAccelX =0;
		public float thresholdAccelY =0;
		public float thresholdAccelZ =0;
		public float thresholdAccelNewX =0;
		public float thresholdAccelNewY =0;
		public float thresholdAccelNewZ =0;
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
					this.thresholdAccelX = opsins.getDesvioPadrao(opsins.getVariancia(tempAccelX));
					this.thresholdAccelY = opsins.getDesvioPadrao(opsins.getVariancia(tempAccelY));
					this.thresholdAccelZ = opsins.getDesvioPadrao(opsins.getVariancia(tempAccelZ));
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
					this.thresholdAccelNewX = opsins.getDesvioPadrao(opsins.getVariancia(tempAccelX));
					this.thresholdAccelNewY = opsins.getDesvioPadrao(opsins.getVariancia(tempAccelY));
					this.thresholdAccelNewZ = opsins.getDesvioPadrao(opsins.getVariancia(tempAccelZ));
					
				}
				this.limparArrays();
			}

		}
		
	}
	
		
	
}
