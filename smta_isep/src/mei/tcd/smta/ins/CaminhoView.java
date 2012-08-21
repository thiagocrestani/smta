package mei.tcd.smta.ins;

import java.text.DecimalFormat;
import java.util.ArrayList;

import mei.tcd.smta.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
//Leitura
//http://developer.android.com/reference/android/graphics/Canvas.html
//http://developer.android.com/guide/topics/graphics/2d-graphics.html
//http://jmsliu.com/199/android-canvas-example.html
//http://html5.litten.com/understanding-save-and-restore-for-the-canvas-context/
//http://bestsiteinthemultiverse.com/2008/11/android-graphics-example/
public class CaminhoView extends View {
	// Paints para desenhar
	private static int escala=3; // Para mais terde escalar
	private static int offset=20;
	private Paint marcasPaint;
	private Paint textPaint;
	private float[] posicao;
	private int index = 0;
	private int indexRestore =0;
	Bitmap ponto;
	//Bitmap seta;
	float velocidade;
	float ultimoX, ultimoY;
	boolean restore= false; // Só posso fazer o restore se existir translate, caso contrario o texto foge
	
	// pontos para o draw
	private ArrayList<Float> xArray, yArray;
	
	DecimalFormat graus = new DecimalFormat("0.0");;
	
	public void setPosicao(float[] _posicao) {
		xArray.add(_posicao[0]);
		yArray.add(_posicao[1]);
		velocidade = _posicao[2];
		posicao = _posicao;
	}
	public float[] getPosicao() {
		return posicao;
	}
	
	
	public CaminhoView(Context context) {
		super(context);
		initCaminhoView();
		
	}   

	public CaminhoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initCaminhoView();
	}

	public CaminhoView(Context context, AttributeSet attrs, int defaultStyle) {
		super(context, attrs, defaultStyle);
		initCaminhoView();
	}

	
	protected void initCaminhoView() {
		setFocusable(true);
		xArray = new ArrayList<Float>(); 
		yArray = new ArrayList<Float>();
		velocidade = 0;
		// referencias os resources
		Resources r = this.getResources();
		

		
		marcasPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		marcasPaint.setStrokeWidth(5);
		marcasPaint.setColor(r.getColor(R.color.marcas));

		textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint.setColor(r.getColor(R.color.text_color));
		textPaint.setTextSize(12);
		ponto =  BitmapFactory.decodeResource(getResources(),R.drawable.dot_icon);
		//seta =  BitmapFactory.decodeResource(getResources(),R.drawable.arrow_icon1);
		

	}
	@Override    
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) { 
		// O velocimetro é um circulo equivalente ao da orientacao
		// Vai ocupar o máximo espaço disponivel
		// A mais pequena medida vai assumir a dimensão
		
		int measuredWidth = measure(widthMeasureSpec);
		int measuredHeight = measure(heightMeasureSpec);

		int d = Math.min(measuredWidth, measuredHeight);
		// Quando override temos de chamar este método para gravar
		setMeasuredDimension(d, d);    
	}

	private int measure(int measureSpec) {
		int result = 0; 

		
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec); 

		if (specMode == MeasureSpec.UNSPECIFIED) {
			
			result = 200;
		} else {
			
			result = specSize;
		} 
		return result;
	}
	// Começa sempre 90 graus para a direita devido ao valor do cos(azimuth)
	@Override 
	protected void onDraw(Canvas canvas) {
		
		int px = getMeasuredWidth() / 2;
		int py = getMeasuredHeight() / 2;
		index = 0;
		indexRestore =0;
		//translate positivox vai para a direita o canvas 
		if(ultimoX+offset>px)
		{
			canvas.save();
	    	canvas.translate((px-(ultimoX+offset)-40), 0);
	    	restore = true;
	    	indexRestore ++;
		}
		if(ultimoX<-px+offset)
		{
			canvas.save();
	    	canvas.translate(-ultimoX-px+offset+40, 0);
	    	restore = true;
	    	indexRestore ++;
		}
		if(ultimoY+offset>py)
		{
			canvas.save();
	    	canvas.translate(0, (py-(ultimoY+offset)-40));
	    	restore = true;
	    	indexRestore ++;
		}
		if(ultimoY<-py+offset)
		{
			canvas.save();
	    	canvas.translate(0, -ultimoY-py+offset+40);
	    	restore = true;
	    	indexRestore ++;
		}
		
		while(index<xArray.size()){
                 
			
            //canvas.drawPoint(px+xArray.get(index),py+yArray.get(index),marcasPaint);
			canvas.drawBitmap(ponto, px+xArray.get(index), py+yArray.get(index), null);
            //invalidate();
            index++;
            
           
            
		}
		if(restore)
		{
			for(int i=0;i<indexRestore;i++)
				canvas.restore();
			restore=false;
		}
		
		if(index>0)
		{
			//canvas.restore();
			canvas.drawText("Posicao X:" + xArray.get(index-1), 1, 10, textPaint);
			canvas.drawText("Posicao Y:" + yArray.get(index-1), 1, 22, textPaint);
			ultimoX = xArray.get(index-1);
			ultimoY = yArray.get(index-1);


		}
		
	
		
	}


}	
