package tnl.objcanvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import java.util.Iterator;
import java.util.List;

/**
 * Created by tango on 2/26/2018.
 */

public class DrawingUtil {
	public static final String TAG = "DrawingUtil";



	public static final int NONECOLOR = Color.argb(0, 0, 0, 0);

	public static final float DEFAULT_LINEWIDTH = 15;
	public static final int DEFAULT_LINECOLOR = Color.BLUE;

	public static final float DEFAULT_POINTSIZE = 35;
	public static final int DEFAULT_POINTCOLOR = Color.WHITE;
	public static final int DEFAULT_POINT_SHADOWCOLOR = Color.GRAY;

	public static final int DEFAULT_FILLCOLOR = Color.argb(100, 0, 148, 255);

	public static final int DEFAULT_FIRSTVERTEX_MARKCOLOR = Color.rgb(76, 208, 0);
	public static final float DEFAULT_FIRSTVERTEX_MARKEXTRA_RATIO = 0.05f;



	public static void drawLines(
		float[] vertices,
		float lineWidth, int lineColor,
		Canvas canvas, Paint sharedPaint
	) {

		drawLines(
			vertices, 0, vertices.length,
			lineWidth, lineColor,
			canvas, sharedPaint
		);

	}


	public static void drawLines(
		float[] vertices, int startIndex, int length,
		float lineWidth, int lineColor,
		Canvas canvas, Paint sharedPaint
	) {

		if (startIndex < 0 || startIndex >= vertices.length) {
			return;
		}

		if (lineWidth <= 0 || Color.alpha(lineColor) == 0) {
			return;
		}

		preparePaintForLine(sharedPaint, lineWidth, lineColor);

		int endIndex = Math.min(vertices.length - 4, startIndex + length - 4);

		for (int i = startIndex; i <= endIndex; i += 2) {
			canvas.drawLine(
				vertices[i], vertices[i + 1], vertices[i + 2], vertices[i + 3],
				sharedPaint
			);

		}

	}


	public static void drawLine(
		float x1, float y1, float x2, float y2,
		float lineWidth, int lineColor,
		Canvas canvas, Paint sharedPaint
	) {
		preparePaintForLine(sharedPaint, lineWidth, lineColor);
		canvas.drawLine(x1, y1, x2, y2, sharedPaint);
	}


	public static void drawPoints(
		float[] vertices,
		float pointSize, int pointColor,
		Canvas canvas, Paint sharedPaint
	) {

		drawPoints(
			vertices, 0, vertices.length,
			pointSize, pointColor,
			canvas, sharedPaint
		);

	}


	public static void drawPoints(
		float[] vertices, int startIndex, int length,
		float pointSize, int pointColor,
		Canvas canvas, Paint sharedPaint
	) {

		if (startIndex < 0 || startIndex >= vertices.length) {
			return;
		}

		if (pointSize == 0 || length <= 0 || Color.alpha(pointColor) == 0) {
			return;
		}

		preparePaintForPoint(sharedPaint, pointSize, DEFAULT_POINT_SHADOWCOLOR);

		canvas.drawPoints(
			vertices, startIndex, Math.min(length, vertices.length - startIndex),
			sharedPaint
		);

		preparePaintForPoint(sharedPaint, pointSize - 2, pointColor);

		canvas.drawPoints(
			vertices, startIndex, Math.min(length, vertices.length - startIndex),
			sharedPaint
		);

	}


	public static void drawPoint(
		float x, float y,
		float pointSize,
		int pointColor,
		Canvas canvas, Paint sharedPaint
	) {
		preparePaintForPoint(sharedPaint, pointSize, DEFAULT_POINT_SHADOWCOLOR);
		canvas.drawPoint(x, y, sharedPaint);

		preparePaintForPoint(sharedPaint, pointSize - 2, pointColor);
		canvas.drawPoint(x, y, sharedPaint);
	}


	public static void fillPolygon(
		float[] vertices, int fillColor, Canvas canvas, Paint sharedPaint
	) {
		if (vertices.length < 6) {
			return;
		}

		Path path = new Path();

		path.moveTo(vertices[0], vertices[1]);

		int length = vertices.length - vertices.length % 2;

		for (int i = 2; i < length; i += 2) {
			path.lineTo(vertices[i], vertices[i + 1]);
		}

		path.close();

		sharedPaint.reset();
		sharedPaint.setColor(fillColor);
		sharedPaint.setStyle(Paint.Style.FILL);

		canvas.drawPath(path, sharedPaint);
	}


	/**
	 *
	 * @param vertices
	 * @param fillColor             Set alpha channel to 0 to indicate not a polygon
	 * @param firstVertexMarkColor  Set alpha channel to 0 to indicate not marking first vertex
	 * @param firstVertexLineRatio
	 * @param lineWidth
	 * @param lineColor
	 * @param pointSize
	 * @param pointColor
	 * @param canvas
	 * @param sharedPaint
	 */
	public static void drawPoly(
		float[] vertices,
		int fillColor,
		float lineWidth, int lineColor, float pointSize, int pointColor,
		int firstVertexMarkColor, float firstVertexLineRatio,
		Canvas canvas, Paint sharedPaint
	) {
		if (vertices == null) {
			return;
		}

		if (vertices.length == 0) {
			return;
		}

		int verticesRealLength = vertices.length - vertices.length % 2;

		// If there are less than 3 vertices, then this can't be a polygon
		if (verticesRealLength < 6) {
			fillColor = NONECOLOR;
		}

		fillPolygon(vertices, fillColor, canvas, sharedPaint);

		if (Color.alpha(firstVertexMarkColor) == 0) {
			// Don't have to mark first vertex

			drawLines(vertices, lineWidth, lineColor, canvas, sharedPaint);

			if (Color.alpha(fillColor) != 0) {
				drawLine(
					vertices[verticesRealLength - 2],
					vertices[verticesRealLength - 1],
					vertices[0],
					vertices[1],
					lineWidth, lineColor,
					canvas, sharedPaint
				);

			}

			drawPoints(vertices, pointSize, pointColor, canvas, sharedPaint);

		} else if (verticesRealLength > 0) {
			// Have to mark first vertex. This is only possible if vertices has at least 1 point

			float firstX = vertices[0];
			float firstY = vertices[1];

			if (verticesRealLength > 1) {
				float extraFirstX =
					firstX
					+ (vertices[2] - vertices[0]) * firstVertexLineRatio;

				float extraFirstY =
					firstY
					+ (vertices[3] - vertices[1]) * firstVertexLineRatio;

				vertices[0] = extraFirstX;
				vertices[1] = extraFirstY;

				drawLines(vertices, lineWidth, lineColor, canvas, sharedPaint);

				// Draw the last-to-first line if fillColor is defined (meaning that
				// a polygon is intended to draw)
				if (Color.alpha(fillColor) != 0) {
					drawLine(
						vertices[verticesRealLength - 2],
						vertices[verticesRealLength - 1],
						firstX, firstY,
						lineWidth, lineColor,
						canvas, sharedPaint
					);

				}

				drawLine(
					firstX, firstY, extraFirstX, extraFirstY,
					lineWidth, firstVertexMarkColor,
					canvas, sharedPaint
				);

			}

			drawPoints(
				vertices,2, verticesRealLength - 2,
				pointSize, pointColor,
				canvas, sharedPaint
			);

			drawPoint(firstX, firstY, pointSize, firstVertexMarkColor, canvas, sharedPaint);
		}

	}


	public static void drawArrowHead(
		float headX, float headY,
		float directionX, float directionY,
		float openAngleDegree,
		float arrowEdgeLength,
		float lineWidth, int lineColor,
		Canvas canvas, Paint sharedPaint
	) {
		if (Color.alpha(lineColor) == 0 || arrowEdgeLength == 0) {
			return;
		}

		preparePaintForLine(sharedPaint, lineWidth, lineColor);

		float openAngleRad = (float)(openAngleDegree / 180 * Math.PI);

		float angleVecRad = (float)Math.atan2(directionY, directionX);
		float angleEdgeLeftRad = angleVecRad + openAngleRad;
		float angleEdgeRightRad = angleVecRad - openAngleRad;

		canvas.drawLine(
			headX,
			headY,
			headX + arrowEdgeLength * (float)Math.cos(angleEdgeLeftRad),
			headY + arrowEdgeLength * (float)Math.sin(angleEdgeLeftRad),
			sharedPaint
		);

		canvas.drawLine(
			headX,
			headY,
			headX + arrowEdgeLength * (float)Math.cos(angleEdgeRightRad),
			headY + arrowEdgeLength * (float)Math.sin(angleEdgeRightRad),
			sharedPaint
		);

	}


	public static void preparePaintForLine(Paint paint, float lineWidth, int lineColor) {
		paint.reset();
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeWidth(lineWidth);
		paint.setColor(lineColor);
	}


	public static void preparePaintForPoint(Paint paint, float pointSize, int pointColor) {
		paint.reset();
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setColor(pointColor);
		paint.setStrokeWidth(pointSize);
	}

}
