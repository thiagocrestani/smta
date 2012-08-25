package mei.tcd.smta.util;

import org.ejml.simple.SimpleMatrix;

//Leitura
//http://www.ordnancesurvey.co.uk/oswebsite/gps/docs/A_Guide_to_Coordinate_Systems_in_Great_Britain.pdf
//http://en.wikipedia.org/wiki/File:ECEF.png
//http://home.online.no/~sigurdhu/WGS84_Eng.html
/**
 * 
 * Utilit�rios de convers�es de sistemas de coordenadas globais
 * 
 * Baseado em 2012 David Herges, david@spektrakel.de
 *
 * 
 */
public class WGS84 {
	/**
     * Constante: Semi-Major Axis a
     */
    public static final double WGS84_A = 6378137.0;

    /**
     * Constant: Semi-minor Axis b
     */
    public static final double WGS84_B = 6356752.3142;

    /**
     * Constant: First eccentricity = e =
     */
    public static final double WGS84_E = 0.0818191908426;

    /**
     * Constant: Flattening = f =
     */
    public static final double WGS84_F = (WGS84_A - WGS84_B) / WGS84_A;

    /**
     * Converte uma coordenada WGS84 coordinate para uma coordenada ECEF. Algoritmo do livro
     *  "Integrated Navigation System"  Jan Wendel
     * 
     * @param latitude
     * @param longitude
     * @param altitude
     * @return Coordenata ECEF como array de double com valor[0]: x-coordenada
     *         valor[1]: y-coordenada valor[2]: z-coordenada
     */
    public static final double[] wgs2ecef(double latitude, double longitude, double altitude) {
        // converter para radianos
        latitude = Math.toRadians(latitude);
        longitude = Math.toRadians(longitude);

        // Variaveis utilit�rias
        double e2 = WGS84_E * WGS84_E;
        double N = WGS84_A / Math.sqrt(1 - e2 * Math.sin(latitude) * Math.sin(latitude));

        // Calcular coordenadas cartesianas
        double[] ecef = new double[3];
        ecef[0] = (N + altitude) * Math.cos(latitude) * Math.cos(longitude);
        ecef[1] = (N + altitude) * Math.cos(latitude) * Math.sin(longitude);
        ecef[2] = (N * (1 - e2) + altitude) * Math.sin(latitude);

        return ecef;
    }

    /**
     * Converte coordenadas ECEF em coordenadas WGS84. 
     * 
     * @param x em metros
     * @param y em metros
     * @param z em metros
     * @return coordenadas WGS84 como array de double com velor[0]: latitude em graus
     *          velor[1]: longitude em graus velor[2]: altitude em
     *         metros sobre a elipsoide
     */
    public static final double[] ecef2wgs(double x, double y, double z) {
       
        double[] wgs = new double[3];
        if (x == 0 && y == 0) {
            wgs[1] = 0.0;
            if (z > 0) {
                wgs[0] = 90.0;
                wgs[2] = z - WGS84_B;
            } else {
                wgs[0] = -90.0;
                wgs[2] = z + WGS84_B;
            }
            return wgs;
        }

        // Variaveis
        double e2 = WGS84_E * WGS84_E;
        double es2 = (WGS84_A * WGS84_A - WGS84_B * WGS84_B) / (WGS84_B * WGS84_B);
        double p = Math.sqrt((x * x) + (y * y));
        double theta = Math.atan((z * WGS84_A) / (p * WGS84_B));

        // calcular latitude e longitude
        wgs[0] = Math.atan((z + (es2 * WGS84_B * Math.pow(Math.sin(theta), 3)))
                / (p - (e2 * WGS84_A * Math.pow(Math.cos(theta), 3))));
        wgs[1] = Math.atan2(y, x);

        // calcular altitude
        double N = WGS84_A / Math.sqrt(1 - e2 * Math.sin(wgs[0]) * Math.sin(wgs[0]));
        wgs[2] = (p / Math.cos(wgs[0])) - N;

        // converter para graus
        wgs[0] = Math.toDegrees(wgs[0]);
        wgs[1] = Math.toDegrees(wgs[1]);

        return wgs;
    }

    /**
     * Convert coordenadas locais ENU (Earth-North-Up) com origem numa coordenada WGS84 para coordenadas ECEF
     *  
     * @param east valor em metros
     * @param north valor em metros
     * @param up valor em metros
     * @param latitude latitude das coordenadas locais originarias em graus
     * @param longitude longitude das coordenadas locais originarias em graus
     * @return coordenadas ECEF como array double com valor[0]: x-coordenada
     *         valor[1]: y-coordenada valor[2]: z-coordenada
     */
    public static final double[] enu2ecef_wgs(double east, double north, double up,
            double latitude, double longitude) {
        // converter para radianos
        latitude = Math.toRadians(latitude);
        longitude = Math.toRadians(longitude);

        
        final double sin_lat = Math.sin(latitude);
        final double cos_lat = Math.cos(latitude);
        final double sin_lon = Math.sin(longitude);
        final double cos_lon = Math.cos(longitude);

        
        double[] ecef = new double[3];
        ecef[0] = (-1.0 * sin_lon * east) - (sin_lat * cos_lon * north) + (cos_lat * cos_lon * up);
        ecef[1] = (cos_lon * east) - (sin_lat * sin_lon * north) + (cos_lat * sin_lon * up);
        ecef[2] = (cos_lat * north) + (sin_lat * up);

        return ecef;
    }

    
}
