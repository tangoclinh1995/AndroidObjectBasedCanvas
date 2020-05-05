package tnl.objcanvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.NonNull;

import java.util.Iterator;
import java.util.List;

/**
 * Created by Linh Ta on 1/8/2018.
 */

public class CanvasPolygon extends CanvasPoly {
	public int fillColor = DrawingUtil.DEFAULT_FILLCOLOR;



	public void draw(
		Canvas canvas, ViewMatrixAnalyzer matViewAnalyzer, Paint sharedPaint,
		boolean isOnAnimation
	) {
		int appliedFillColor = fillColor;

		DrawingUtil.drawPoly(
			matViewAnalyzer.mapOriginalToViewPoint(vertices),
			appliedFillColor,
			lineWidth, lineColor, vertexPointSize, vertexPointColor,
			firstVertexMarkColor, firstVertexMarkExtraRatio,
			canvas, sharedPaint
		);

	}


	public void lazyUpdate(
		ViewMatrixAnalyzer matAnalyzer, int viewWidth, int viewHeight
	) {

	}

}
