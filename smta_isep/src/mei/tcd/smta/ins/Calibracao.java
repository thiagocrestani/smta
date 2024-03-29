package mei.tcd.smta.ins;

import android.hardware.SensorManager;
import mei.tcd.smta.util.Ops;

public class Calibracao{
	
	//private static final float GRAVIDADE = SensorManager.GRAVITY_EARTH;
	private float[] arrayX;
	private float[] arrayY;
	private float[] arrayZ;
	private final static float GRAVIDADE = SensorManager.GRAVITY_EARTH;
	private float[] dadosAccCalib = new float[3];
//	// Para mais tarde efectuarmos uma calibra��o a tr�s planos
//	private float[] gravidadePos1 = new float[]{0,0,0}; //valores quando deitado com ecra para cima (-)
////	private float[] gravidadePos2 = new float[]{0,GRAVIDADE,0}; //valores quando de p� encostado a uma parede por exemplo (|)
////	private float[] gravidadePos3 = new float[]{GRAVIDADE,0,0}; //valores quando perpendicular a uma superficie plana com o Y paralelo a superficie (--)
	private int obervacoes;
	private int indice = 0;
	Ops operacoes;
	
	public boolean jaCalibrou = false;
	
	public Calibracao(int _observacoes){
		operacoes = new Ops(); 
		arrayX = new float[_observacoes];
		arrayY = new float[_observacoes];
		arrayZ = new float[_observacoes];
		obervacoes = _observacoes;
		indice = 0;
		resetCalib();
	}
	
	/***
	 * Vai buscar a m�dia dos valores gravados em cada um dos eixos
	 * @return o vetor m�dia dos valores
	 */
	public float[] getCalibVetor()
	{
		return dadosAccCalib;
	}
	public void resetCalib()
	{
		for (int i = 0;i<3;i++)
			dadosAccCalib[i] = 0;
		indice = 0;
		
	}
	/***
	 * Adiciona o valor ao array de valores de cada eixo para quando finalizar, ent�o calcular a m�dia
	 * @param _xyz
	 */
	public void adicionadaXYZ(float[] _xyz)
	{
		if(indice==this.obervacoes-1)
		{
			jaCalibrou = true;
			dadosAccCalib[0] = operacoes.getMedia(arrayX);
			dadosAccCalib[1] = operacoes.getMedia(arrayY);
			dadosAccCalib[2] = operacoes.getMedia(arrayZ);
		}
		arrayX[indice] =  _xyz[0];
		arrayY[indice] =  _xyz[1];
		arrayZ[indice] =  _xyz[2];
		indice ++;
	}
}
