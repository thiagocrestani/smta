package mei.tcd.smta.filtros;
/**
 * ###################   Filtros passa baixo e passa alto   ####################
 */
/**
 * Low Pass filter
 *  alpha is calculated as t / (t + dT)
 *  with t, the low-pass filter's time-constant
 *  and dT, the event delivery rate
 * Ingora grandes alterações no acelerometro
 * t= duração relativa do sinal sobre o qual irá actuar. Sinais maiores que a constante de tempo passam inalterados
 * dt = 0.02
 * 
 *   float dt = (float) (1.0 / sampleRate); 0.02
 *   float RC = (float) (1.0 / cutoffFrequency); 
 *   alpha = dt / (RC + dt);
 *   http://stackoverflow.com/questions/11812490/time-discrete-implementation-of-1st-order-rc-filter
 * alpha trabalha bem com cutoff de 0.15
 * There's a constant, alpha, which affects the 'weight' or 'momentum' -- basically how drastically does the new value affect the current smoothed value. 
 */
public class HighLowPassFilter {
	private float mAlpha;
	
	public HighLowPassFilter(float alpha_)
	{
		mAlpha = alpha_;
		
	}
	
		public float[] lowPass(float[] input, float[] output) {
			if ( output == null ) return input;
		     
		    for ( int i=0; i<input.length; i++ ) {
		    	output[i] = mAlpha * input[i] + (1.0f - mAlpha) * output[i];
		        
		    }
		    return output;
		    
			 
		}
	/**
	 * High Pass filter
	 * Ingora o aceleromtro para pequenos valores
	 */
		public float[] highPass(float[] input, float[] inputAnterior, float[] output) {
			if ( output == null ) return input;
		     
		    for ( int i=0; i<input.length; i++ ) {
		    	output[i] = mAlpha * (output[i] + input[i] - inputAnterior[i]);
		        
		    }
		    return output;
			//return alpha * (filtered + current - last);
				  //return current;
		}
	
}
