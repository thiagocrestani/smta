package mei.tcd.smta.filtros;

public class MediaFiltro {
	private float[] mediaVector = new float[4];
	private float[] somaVector = new float[4];
    private int index;
    private int contador;
    private int size;
    /**
     * Método construtor define-se o tamanho do array.
     * Inicializa os contadores.
     * 
     * @param size int
     */
    public MediaFiltro(int _size) {
    	size=_size;
        reset();
    }

    /**
     * Retorna a média actual
     * 
     * @param media float
     */
    
    public float[] getMediaVector() {
    	for(int i=0;i<4;i++)
    		mediaVector[i] = somaVector[i]/size;
        return mediaVector;
    }
   
    /**
     * Adiciona um valor.
     */
    
    public void adicionaVector(float[] vetor) {
    	if(index==size)
    		reset();
    	for (int i=0;i<4;i++)
    		somaVector[i] += vetor[i];
    	index ++;
    }

    /*
     * @see com.forusers.android.filter.Filter#reset()
     */
   
    public void reset() {
        contador = 0;
        index = 0;
        for (int i=0;i<4;i++)
    		somaVector[i] = 0;
        
    }

    public long getCount() {
        return index;
    }

    }
