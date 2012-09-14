package mei.tcd.smta.util;

//Leitura
//http://www.movable-type.co.uk/scripts/latlong.html 
/**
 * 
 * 
 * 
 *
 * 
 */
public class WGS84 {
	public static double EarthRadius = 6378137.0;//6 378 km
	/**
	 * Encontra a coordenada seguinte com os valores da direcção (Bearing) e distancia (calcula entre a posição anterior e a actual)
	 * 
	 * @param _latitude
	 * @param _longitude
	 * @param _azimuth em radianos
	 * @param _distancia em metros
	 * @return
	 */
	public static double[] FindPointAtDistanceFrom(double _latitude, double _longitude, double _azimuth, double _distancia)
	{
		double[] retval = new double[2];
		double d = _distancia / EarthRadius;
        double lat1 = Math.toRadians(_latitude);
        double lon1 = Math.toRadians(_longitude);
        double lat = Math.asin((Math.sin(lat1) * Math.cos(d))
                        + (Math.cos(lat1) * Math.sin(d) * Math.cos((_azimuth))));

        double lon = lon1
                        + Math.atan2(Math.sin((_azimuth)) * Math.sin(d)
                                        * Math.cos(lat1),
                                        Math.cos(d) - Math.sin(lat1) * Math.sin(lat));
	    
		retval[0] = lat;
	    retval[1] = lon;
	  return retval;
	}
	/**
	 * Distancia entre dois pontos posição (vetores)
	 * 
	 * @param _posanterior
	 * @param _posactual
	 */
	public static double getLastDistance(float[] _posanterior, float[] _posactual){
		double[] distancia = new double[3];
		distancia[0] = _posanterior[0] - _posactual[0];
		distancia[1] = _posanterior[1] - _posactual[1];
		distancia[2] = _posanterior[2] - _posactual[2];
		return Math.sqrt(distancia[0]*distancia[0] + distancia[1]*distancia[1] + distancia[2]*distancia[2]);
	}
}

