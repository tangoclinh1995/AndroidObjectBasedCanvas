package tnl.objcanvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import java.util.Arrays;

/**
 * Created by Linh Ta on 1/8/2018.
 */

public class CanvasPolyline extends CanvasPoly {
	public void draw(
		Canvas canvas, ViewMatrixAnalyzer matViewAnalyzer, Paint sharedPaint,
		boolean isOnAnimation
	) {

		DrawingUtil.drawPoly(
			matViewAnalyzer.mapOriginalToViewPoint(vertices),
			DrawingUtil.NONECOLOR,
			lineWidth, lineColor,
			vertexPointSize, vertexPointColor,
			firstVertexMarkColor, firstVertexMarkExtraRatio,
			canvas, sharedPaint
		);

	}


	public void lazyUpdate(
		ViewMatrixAnalyzer matAnalyzer, int viewWidth, int viewHeight
	) {

	}

}
