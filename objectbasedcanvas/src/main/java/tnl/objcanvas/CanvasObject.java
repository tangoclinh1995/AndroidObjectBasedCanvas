package tnl.objcanvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;


/**
 * Created by Linh Ta on 1/8/2018.
 */
public interface CanvasObject {
	void draw(
		Canvas canvas, ViewMatrixAnalyzer matAnalyzer, @Nullable Paint sharedPaint,
		boolean isOnAnimation
	);

	boolean needLazyUpdate();
	void lazyUpdate(ViewMatrixAnalyzer matAnalyzer, int viewWidth, int viewHeight);
}
