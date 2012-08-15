package mei.tcd.smta.ins;

import java.text.DecimalFormat;

import mei.tcd.smta.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
//Leitura
//http://developer.android.com/reference/android/graphics/Canvas.html
//http://developer.android.com/guide/topics/graphics/2d-graphics.html
//http://jmsliu.com/199/android-canvas-example.html
//http://html5.litten.com/understanding-save-and-restore-for-the-canvas-context/
public class VelocidadeView extends View {
	// Paints para desenhar
	private Paint marcasPaint;
	private Paint textPaint;
	private Paint textPaintkm;
	private Paint circuloForaPaint;
	private Paint verde;
	private Paint amarelo;
	private Paint vermelho;
	private float textWidthKm;
	private String kmText;
	private int textHeight;
	private float velocidade;
	private int semaforo;
	DecimalFormat graus = new DecimalFormat("0.0");;
	
	public void setVelocidade(float _velocidade) {
		
		velocidade = _velocidade;
	}
	public float getVelocidade() {
		return velocidade;
	}
	public void setSemaforo(int _semaforo) {
		
		semaforo = _semaforo;
	}
	public float getSemaforo() {
		return semaforo;
	}

	

	
	public VelocidadeView(Context context) {
		super(context);
		initVelocidadeView();
		
	}   

	public VelocidadeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initVelocidadeView();
	}

	public VelocidadeView(Context context, AttributeSet attrs, int defaultStyle) {
		super(context, attrs, defaultStyle);
		initVelocidadeView();
	}

	
	protected void initVelocidadeView() {
		setFocusable(true);
		// referencias os resources
		Resources r = this.getResources();
		

		// criar os paints
		circuloForaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circuloForaPaint.setColor(r.getColor(R.color.circulos_grandes));
		circuloForaPaint.setStrokeWidth(3);
		circuloForaPaint.setStyle(Paint.Style.STROKE);

		marcasPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		marcasPaint.setStrokeWidth(2);
		marcasPaint.setColor(r.getColor(R.color.marcas));

		textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint.setColor(r.getColor(R.color.text_color));
		textPaint.setTextSize(12);
		
		textPaintkm = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaintkm.setColor(r.getColor(R.color.text_color));
		textPaintkm.setTextSize(24);
		
		vermelho = new Paint(Paint.ANTI_ALIAS_FLAG);
		vermelho.setColor(Color.RED);
		vermelho.setStrokeWidth(3);
		vermelho.setStyle(Paint.Style.FILL_AND_STROKE);
		
		amarelo = new Paint(Paint.ANTI_ALIAS_FLAG);
		amarelo.setColor(Color.YELLOW);
		amarelo.setStrokeWidth(3);
		amarelo.setStyle(Paint.Style.FILL_AND_STROKE);
		
		verde = new Paint(Paint.ANTI_ALIAS_FLAG);
		verde.setColor(Color.GREEN);
		verde.setStrokeWidth(3);
		verde.setStyle(Paint.Style.FILL_AND_STROKE);

		textHeight = (int)textPaint.measureText("yY");

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
	@Override 
	protected void onDraw(Canvas canvas) {
		
		int px = getMeasuredWidth() / 2;
		int py = getMeasuredHeight() / 2;

		int radius = Math.min(px, py);
		
		canvas.drawCircle(px, py, radius, circuloForaPaint);
		// Velocidade máxima 250
		// 250/360º = 0,69444444444444444444444444444444
		// Graus para radianos = 0.012042772
		canvas.drawLine(px, py, (float)(radius - Math.cos(0.012042772*velocidade)*(radius-20)), (float)(radius - Math.sin(0.012042772*velocidade)*(radius-20)), 		marcasPaint);
		// Rotate our perspective so that the 'top' is facing the 		current bearing.
		canvas.save();
		//canvas.rotate(-bearing, px, py);
		int textWidth = (int)textPaint.measureText("W");
		int cardinalX = px-radius+30-textWidth/2;
		int cardinalY = py;

		// Draw the marker every 15 degrees and a text every 45.
		for (int i = 0; i < 13; i++) {
			// Draw a marker.
			canvas.drawLine(px-radius, py, px-radius+10, py, 		marcasPaint);

			canvas.save();
			canvas.translate(0, textHeight);

			// Pinta os números
			//canvas.save(); /// Sem o save para poder rodar apenas o que vai à frente
			canvas.rotate(-90, cardinalX, cardinalY);
			
			
				switch (i) {
				case(0)  : {
					canvas.drawText("0", cardinalX, 	cardinalY, textPaint);	break;
				}
				case(3)  : canvas.drawText("65", cardinalX, 	cardinalY, textPaint); break;
				case(6) : canvas.drawText("125", cardinalX, 	cardinalY, textPaint); break;
				case(9) : canvas.drawText("185", cardinalX, 	cardinalY, textPaint); break;
				case(12) : canvas.drawText("250", cardinalX, 	cardinalY, textPaint); break;
				}

				//canvas.drawText(dirString, cardinalX, 	cardinalY, textPaint);
			
			canvas.restore();
			canvas.rotate(15, px, py);
		}
		
		
		
		canvas.restore();
		if(getSemaforo()==1)// parado
		{
		canvas.drawCircle(px, py-(py/3), radius/8, vermelho);
		}
		else if(getSemaforo()==2)// abrandar
		{
			canvas.drawCircle(px, py-(py/3), radius/8, amarelo);
		}else //acelerar
		{
			canvas.drawCircle(px, py-(py/3), radius/8, verde);
		}
		kmText = graus.format(velocidade)+ " Km/h";
		textWidthKm = textPaintkm.measureText(kmText);
		canvas.drawText(graus.format(velocidade)+ " Km/h",px-(textWidthKm/2),py+(py/2),textPaintkm);
		
		
	}


}	
