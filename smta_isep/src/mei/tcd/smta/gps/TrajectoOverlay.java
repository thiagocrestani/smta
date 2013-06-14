package mei.tcd.smta.gps;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.maps.ItemizedOverlay;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;

import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
// Classe base de overlay consiste na lista de items (listadepontos) a serem mostrados no mapa.
public class TrajectoOverlay extends ItemizedOverlay<OverlayItem> {
	// Lista de pontos a serem mostrados no mapa. Utiliza array de OverlayItem
	private List<TrajectoOverlayItem> listaDePontos = new ArrayList<TrajectoOverlayItem>();
	private Paint trajectoPaint;
	private Paint tracoPaint;
	private Paint fillPaint;
	private GoogleMap mapView;
	private boolean inicio = true;

	public TrajectoOverlay(Drawable defaultMarker, GoogleMap mapView2)
	{
		// Interessante objecto que permite ajustar o centro do drawable para marcar no inicio. Neste caso vai ser um ponto.
		super(boundCenterBottom(defaultMarker));
// Ok, esta linha é estranha e é apenas por causa de um bug em 
		//http://code.google.com/p/android/issues/detail?id=2035
		populate();
		trajectoPaint = new Paint();
		trajectoPaint.setColor(Color.RED);
		trajectoPaint.setStrokeWidth(7);

		tracoPaint = new Paint();
		tracoPaint.setColor(Color.BLUE);
		tracoPaint.setStrokeWidth(2);
		tracoPaint.setStyle(Paint.Style.STROKE);

		fillPaint = new Paint();
		fillPaint.setColor(Color.BLUE);
		fillPaint.setStyle(Style.FILL);
		fillPaint.setAlpha(32);

		this.mapView = mapView2;
	}

	@Override
	protected OverlayItem createItem(int i)
	{
		return listaDePontos.get(i);
	}
// Retorna o numero de items no overlay
	@Override
	public int size()
	{
		return listaDePontos.size();
	}
// Adiciona ponto a lista
	public void adicionaPonto(double latitude, double longitude)
	{
		listaDePontos.add(new TrajectoOverlayItem(latitude,
				longitude));
		populate();
// Para chamar o onDraw
		//mapView.invalidate();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow)
	{
		super.draw(canvas, mapView, shadow);

		// Se a lista estiver vazia então não faz nada
		if (!listaDePontos.isEmpty())
		{
			TrajectoOverlayItem anterior = null;
			// PAra cada ponto
			for (TrajectoOverlayItem pontoOverlay : listaDePontos)
			{
				if (anterior != null)
				{
					Projection projection = mapView.getProjection();
					// marca de inicio
					if(inicio)
					{
						canvas.drawCircle(projection.toPixels(anterior.getPoint(), null).x,
								projection.toPixels(anterior.getPoint(), null).y,
								10,
	                              tracoPaint);
						inicio = false;
					}
					// Projection traduz entre o sistema de coordenadas XY e pixeis no ecra.
					
					// Ponto anterior  (Point é um conjunto de 2 coordenadas inteiros)
					// Guarda o ponto anteior e o actual e desenha uma linha entre o dois
					android.graphics.Point pontoAnterior =
						projection.toPixels(anterior.getPoint(), null);

					android.graphics.Point pontoActual =
						projection.toPixels(pontoOverlay.getPoint(), null);

					canvas.drawLine(pontoAnterior.x,
							pontoAnterior.y,
							pontoActual.x,
							pontoActual.y,
							trajectoPaint);
				}

				anterior = pontoOverlay;
			}

			
		}
	}

}
