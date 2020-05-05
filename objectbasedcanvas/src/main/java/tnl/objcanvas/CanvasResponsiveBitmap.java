package tnl.objcanvas;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.InputStream;



/**
 * Created by tango on 12/31/2017.
 */

public class CanvasResponsiveBitmap implements CanvasObject {
	public static final String TAG = "CanvasResponsiveBitmap";



	public static final int LOADING_BACKGROUNDCOLOR = Color.LTGRAY;

	public static final int MIN_VIEWwIDTH = 200;
	public static final int MIN_VIEWHEIGHT = 200;

	public static final int MIN_DECODED_IMG_DIMENSION = 200;

	// When the image has to be re-decoded because of out-of-bounding region,
	// this value determine how much "extra" region will also be re-decoded
	//
	// The higher this value, the less likely that the image will have to be re-decoded
	// Must be in (0, 1] range
	public static final float EXTRA_BOUNDING_RATIO = 0.9f;

	// DecodeImage * (2^logDecodeScale) = OriginalImage
	// OriginalImage * viewScale = ImageInView
	// => ImageInView = (2^logDecodeScale) * viewScale * DecodeImage
	//
	// This threshold determine when then image has to be re-decoded
	//      (2^logDecodeScale) * viewScale > SCALE_THRESHOLD => Re-decoded
	//
	// The higher this value, the less likely that the image will have to be re-decoded, but
	// the more ugly the ImageInView looks (as DecodeImage is scaled up too much)
	public static final float SCALE_THRESHOLD = 4f;
	// The higher this value, the less likely that the image has to be re-decoded
	// Must be >= 1
	public static final float EXTRA_SCALE_RATIO = 1.5f;



	private InputStream inputStream = null;
	private BitmapRegionDecoder decoder = null;
	private int imgWidth = 0;
	private int imgHeight = 0;

	private boolean ioError = false;

	// Relative to original coordinate system
	private Object posXYSyncObj = new Object();

	private float posX = 0;
	private float posY = 0;

	// decodeScale = 2^(-logDecodeScale)
	// 0 < decodeScale <= 1
	private int logDecodeScale = 0;
	private int maxLogDecodeScale = 0;

	private int regionFromX, regionFromY, regionToX, regionToY;

	private Object curBitmapSynObj = new Object();
	private Bitmap curBitmap = null;
	private Matrix curPositioningMatrix = new Matrix();



	public boolean setInputStream(InputStream input) {
		reset();

		try {
			decoder = BitmapRegionDecoder.newInstance(input, true);
		} catch (Exception e) {
			ioError = true;

			decoder = null;
			imgWidth = 0;
			imgHeight = 0;

			return false;
		}

		inputStream = input;

		imgWidth = decoder.getWidth();
		imgHeight = decoder.getHeight();

		maxLogDecodeScale = Math.max(
			0,
			(int)Math.floor(
				Math.log(Math.min(imgWidth, imgHeight) / MIN_DECODED_IMG_DIMENSION)
				/ Math.log(2)
			)

		);

		return true;
	}


	public boolean isIoError() {
		return ioError;
	}


	public boolean isNone() {
		return (inputStream == null);
	}


	public void reset() {
		ioError = false;
		inputStream = null;

		if (decoder != null) {
			decoder.recycle();
		}

	}


	public int getImageWidth() {
		return imgWidth;
	}


	public int getImageHeight() {
		return imgHeight;
	}


	public float getPositionX() {
		return posX;
	}


	public boolean setPositionX(float x) {
		boolean success = true;

		synchronized (posXYSyncObj) {
			if (Float.isNaN(x) || Float.isInfinite(x)) {
				this.posX = 0;

				success = false;
			}

			this.posX = x;
		}

		return success;
	}


	public float getPositionY() {
		return posY;
	}


	public boolean setPositionY(float y) {
		boolean success = true;

		synchronized (posXYSyncObj) {
			if (Float.isNaN(y) || Float.isInfinite(y)) {
				this.posY = 0;

				success = false;
			}

			this.posY = y;
		}

		return success;
	}


	@Override
	public void draw(
		Canvas canvas, ViewMatrixAnalyzer matViewAnalyzer,
		@Nullable Paint sharedPaint,
		boolean isOnAnimation
	) {
		if (isIoError() || isNone()) {
			return;
		}

		Bitmap curBitmapCpy;
		Matrix matCombined;
		Matrix matView = matViewAnalyzer.get();

		synchronized (curBitmapSynObj) {
			curBitmapCpy = curBitmap;

			matCombined = new Matrix(curPositioningMatrix);
		}

		if (curBitmapCpy == null) {
			return;
		}

		// Draw loading background first, to cover unloaded parts
		canvas.save();
		canvas.setMatrix(matView);

		sharedPaint.reset();
		sharedPaint.setColor(LOADING_BACKGROUNDCOLOR);
		canvas.drawRect(0, 0, imgWidth, imgHeight, sharedPaint);

		canvas.restore();

		// Draw bitmap afterward
		matCombined.postConcat(matView);

		canvas.drawBitmap(curBitmapCpy, matCombined, null);
	}


	@Override
	public boolean needLazyUpdate() {
		return true;
	}


	@Override
	public void lazyUpdate(
		ViewMatrixAnalyzer matViewAnalyzer, int viewWidth, int viewHeight
	) {
		// WARNING: These local variables have SAME names with class variables
		float posX, posY;

		synchronized (posXYSyncObj) {
			posX = this.posX;
			posY = this.posY;
		}

		viewWidth = Math.max(viewWidth, MIN_VIEWwIDTH);
		viewHeight = Math.max(viewHeight, MIN_VIEWHEIGHT);

		float viewScale = matViewAnalyzer.getScale();

		// Calculate potentially new image bound
		PointF viewTopLeft = new PointF(0, 0);
		matViewAnalyzer.mapViewToOriginalPoint(
			viewTopLeft.x, viewTopLeft.y, viewTopLeft
		);

		PointF viewTopRight = new PointF(viewWidth, 0);
		matViewAnalyzer.mapViewToOriginalPoint(
			viewTopRight.x, viewTopRight.y, viewTopRight
		);

		PointF viewBottomLeft = new PointF(0, viewHeight);
		matViewAnalyzer.mapViewToOriginalPoint(
			viewBottomLeft.x, viewBottomLeft.y, viewBottomLeft
		);

		PointF viewBottomRight = new PointF(viewWidth, viewHeight);
		matViewAnalyzer.mapViewToOriginalPoint(
			viewBottomRight.x, viewBottomRight.y, viewBottomRight
		);

		float fromX = viewTopLeft.x,
			  toX = viewTopLeft.x,
			  fromY = viewTopLeft.y,
			  toY = viewTopLeft.y;

		fromX = Math.min(fromX, viewTopRight.x);
		fromX = Math.min(fromX, viewBottomLeft.x);
		fromX = Math.min(fromX, viewBottomRight.x);

		toX = Math.max(toX, viewTopRight.x);
		toX = Math.max(toX, viewBottomLeft.x);
		toX = Math.max(toX, viewBottomRight.x);

		fromY = Math.min(fromY, viewTopRight.y);
		fromY = Math.min(fromY, viewBottomLeft.y);
		fromY = Math.min(fromY, viewBottomRight.y);

		toY = Math.max(toY, viewTopRight.y);
		toY = Math.max(toY, viewBottomLeft.y);
		toY = Math.max(toY, viewBottomRight.y);

		// Potentially new region to decode is bounded by the image
		fromX = Math.max(fromX, posX);
		toX = Math.min(toX, posX + imgWidth);
		fromY = Math.max(fromY, posY);
		toY = Math.min(toY, posY + imgHeight);

		// outOfBound = Whether the view is still in the image bound previously decoded
		boolean outOfBound = (
			fromX < posX + regionFromX
			|| toX > posX + regionToX
			|| fromY < posY + regionFromY
			|| toY > posY + regionToY
		);

		// scaleNotEnough = Whether the image bound previously decoded can support the current
		// view if sufficient quality
		boolean scaleNotEnough = ((2 << logDecodeScale) * viewScale > SCALE_THRESHOLD);

		if (!outOfBound && !scaleNotEnough) {
			return;
		}

		int newRegionFromX = (int)Math.floor((fromX - posX) * EXTRA_BOUNDING_RATIO);
		int newRegionFromY = (int)Math.floor((fromY - posY) * EXTRA_BOUNDING_RATIO);

		int newRegionToX = (int)Math.floor(
			toX + (posX + imgWidth - toX) * (1 - EXTRA_BOUNDING_RATIO) - posX
		);
		int newRegionToY = (int)Math.floor(
			toY + (posY + imgHeight - toY) * (1 - EXTRA_BOUNDING_RATIO) - posY
		);

		//  Find new logDecodeScale. Must be a power of 2
		double newLogDecodeScale = Math.floor(
			Math.log(SCALE_THRESHOLD / EXTRA_SCALE_RATIO / viewScale) / Math.log(2)
		);

		// logDecodeScale must be in range [0, maxLogDecodeScale]
		if (newLogDecodeScale < 0) {
			newLogDecodeScale = 0;
		} else if (newLogDecodeScale > maxLogDecodeScale) {
			newLogDecodeScale = maxLogDecodeScale;
		}

		this.logDecodeScale = (int)newLogDecodeScale;

		synchronized (curBitmapSynObj) {
			this.regionFromX = newRegionFromX;
			this.regionFromY = newRegionFromY;
			this.regionToX = newRegionToX;
			this.regionToY = newRegionToY;

			float decodeScale = (float)Math.pow(2, logDecodeScale);

			this.curPositioningMatrix.reset();
			this.curPositioningMatrix.postScale(decodeScale, decodeScale);
			this.curPositioningMatrix.postTranslate(
				posX + this.regionFromX,
				posY + this.regionFromY
			);

			BitmapFactory.Options ops = new BitmapFactory.Options();
			ops.inSampleSize = (int)Math.pow(2, logDecodeScale);

			this.curBitmap = decoder.decodeRegion(
				new Rect(regionFromX, regionFromY, regionToX, regionToY),
				ops
			);

		}

	}


	private PointF transformPoint(float x, float y, Matrix mat) {
		float[] res = new float[] {x, y};
		mat.mapPoints(res);

		return new PointF(res[0], res[1]);
	}


	private PointF transformVector(float x, float y, Matrix mat) {
		float[] res = new float[] {x, y};
		mat.mapVectors(res);

		return new PointF(res[0], res[1]);
	}


	private void projectOn(PointF a, PointF b, PointF vec) {
		float x = (vec.x * a.x + vec.y * a.y) / a.length();
		float y = (vec.x * b.x + vec.y * b.y) / b.length();

		vec.set(x, y);
	}

}
