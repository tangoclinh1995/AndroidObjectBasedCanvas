package tnl.objcanvas;


import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by Linh Ta on 1/8/2018.
 */

public abstract class CanvasPoly implements CanvasObject {
	public float lineWidth = DrawingUtil.DEFAULT_LINEWIDTH;
	public int lineColor = DrawingUtil.DEFAULT_LINECOLOR;

	public float vertexPointSize = DrawingUtil.DEFAULT_POINTSIZE;
	public int vertexPointColor = DrawingUtil.DEFAULT_POINTCOLOR;

	public int firstVertexMarkColor = DrawingUtil.DEFAULT_FIRSTVERTEX_MARKCOLOR;
	public float firstVertexMarkExtraRatio = DrawingUtil.DEFAULT_FIRSTVERTEX_MARKEXTRA_RATIO;

	public ArrayList<PointF> vertices = new ArrayList<>();


	public boolean needLazyUpdate() {
		return false;
	}


	public void lazyUpdate(Matrix matView, int viewWidth, int viewHeight) {

	}

}
