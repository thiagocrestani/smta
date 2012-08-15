package mei.tcd.smta.util;

public class Ops {
	
	/**
	 * Retorna verdadeiro ou false se magnitude > ou < 0
	 * 
	 * @param dados array vetor float
	 * @return true ou false
	 */
	public boolean hasMagnitude(float[] dados)
	{
		if( (float) (this.getMagnitude(dados))>0){
			return true;
		}
		else return false;
	}
	
	
	/**
	 * Obtenção da média de arrays float
	 * 
	 * @param dados array vetor float
	 * @param n numero de elementos
	 * @return média 
	 */
	public float getMedia(float[] dados)
	{
		float retval = 0;
		for(int i =0;i<dados.length; i++)
			retval +=  (dados[i]);
		return retval/dados.length;
	}
	
	
	
	/**
	 * Obtem a magnitude de um vetor array float
	 * 
	 * @param dados array vetor float
	 * @return magnitude
	 */
	public float getMagnitude(float[] dados)
	{
		float SomaQuadrada = 0;
		for(int i=0;i<dados.length;i++){
			SomaQuadrada +=Math.abs(dados[i])*Math.abs(dados[i]);

		}
		return (float) Math.sqrt(SomaQuadrada);

	}
	
	/**
	 * Obtem variancia de um conjunto de valores
	 * 
	 * @param dados array dos dados
	 * @return variancia
	 */
	public float getVariancia(float[] dados)
	{
		float retval = 0;
		float media = 0;
		media = this.getMedia(dados);
		for(int i = 0; i<dados.length;i++)
		{
			// Variancia - SOMA (xi – Média)2 / (n – 1)
			retval += (dados[i] - media) * (dados[i] - media);
		}
		return (retval / dados.length);
		
	}
	
	
	/**
	 * Obtem desvio padrão de um conjunto de valores
	 * 
	 * @param variancia variancia calculada
	 * @return desvio padrao
	 */
	public float getDesvioPadrao(float variancia)
	{
		return (float) Math.sqrt(variancia);
	}
}
