package tnl.objcanvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.os.Parcelable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Linh Ta on 1/8/2018.
 */

public class CanvasNewPolyEditor extends CanvasEditor {
	public static final String TAG = "CanvasNewPolyEditor";



	public static final float DEGREE_EQUALITY_EPS = 0.5f;
	public static final float DUPLICATED_POINT_EPS = 1f;

	public static final int BUTTON_LIST_MARGIN_RIGHT = 8;

	public static final int DEFAULT_FILLCOLOR = Color.argb(100, 0, 148, 255);

	public static final int DEFAULT_LINECOLOR = Color.BLUE;
	public static final int DEFAULT_LINEWIDTH = 20;
	public static final PathEffect DEFAULT_LINEEFFECT = new DashPathEffect(new float[] {50, 50}, 0);

	public static final int DEFAULT_VERTEXCOLOR = Color.WHITE;
	public static final int DEFAULT_VERTEXSIZE = 40;
	public static final int DEFAULT_LASTVERTEX_COLOR = Color.RED;

	public static final int DEFAULT_VERTEX_SHADOWCOLOR = Color.GRAY;
	public static final int DEFAULT_HIGHLIGHT_COLOR = Color.rgb(255, 193, 0);



	public int fillColor = DEFAULT_FILLCOLOR;

	public int lineColor = DEFAULT_LINECOLOR;
	public int lineWidth = DEFAULT_LINEWIDTH;
	public PathEffect lineEffect = DEFAULT_LINEEFFECT;

	public int vertexColor = DEFAULT_VERTEXCOLOR;
	public int vertexSize = DEFAULT_VERTEXSIZE;

	public boolean highlightActiveRightAngle = false;
	public boolean highlightActiveParallel = false;

	private LinearButtonListLayout linBtnListCommands;

	private boolean isPolygon;
	private boolean mustbeConvex;
	private int maxVertexCount;
	private int minVertexCount;
	private ArrayList<PointF> vertices = new ArrayList<PointF>();

	private MyOnButtonClickListener onButtonClickListener = new MyOnButtonClickListener();



	// initData: PolyEditorData
	@Override
	public int startByCanvas(
			ObjectBasedCanvas objectBasedCanvas,
			Object initData,
			OnCancelListener cancelListener
	) {
		if (super.startByCanvas(objectBasedCanvas, initData, cancelListener) == ALREADY_STARTED) {
			return ALREADY_STARTED;
		}

		PolyEditorData initDataCasted;

		if (!(initData instanceof PolyEditorData)) {
			return INVALID_INIT_DATA;
		}

		initDataCasted = (PolyEditorData)initData;

		vertices.clear();

		if (initDataCasted == null) {
			isPolygon = PolyEditorData.DEFAULT_ISPOLYGON;
			mustbeConvex = PolyEditorData.DEFAULT_MUSTBE_CONVEX;

			minVertexCount = PolyEditorData.VERTEX_COUNT_NOLIMIT;
			maxVertexCount = PolyEditorData.VERTEX_COUNT_NOLIMIT;

		} else {
			isPolygon = initDataCasted.isPolygon;
			mustbeConvex = initDataCasted.mustbeConvex;

			minVertexCount = initDataCasted.minVertexCount;
			maxVertexCount = initDataCasted.maxVertexCount;

			if (initDataCasted.vertices != null) {
				GeometryUtil.addListToListPointF(initDataCasted.vertices, vertices);
			}

		}

		prepareCommandControl();

		return STARTED_SUCCESS;
	}


	@Override
	public void endByCanvas() {
		linBtnListCommands.setOnButtonClickListener(null);
		objectBasedCanvas.removeView(linBtnListCommands);

		super.endByCanvas();
	}


	public void draw(
		Canvas canvas,
		ViewMatrixAnalyzer matViewAnalyzer, int viewWidth, int viewHeight,
		Paint sharedPaint,
		boolean isOnAnimation
	) {
		Path sharedPath = new Path();

		int vertexCount = vertices.size();
		float[] pviews = matViewAnalyzer.mapOriginalToViewPoint(vertices);

		PointF pointViewCenter = new PointF(viewWidth / 2f, viewHeight / 2f);

		PointF pview0 = (vertexCount > 0 ? new PointF(pviews[0], pviews[1]) : null);
		PointF pview1 = (vertexCount > 1 ? new PointF(pviews[2], pviews[3]) : null);

		PointF pviewLast0 = (
			vertexCount > 0
				? new PointF(pviews[2 * (vertexCount - 1)], pviews[2 * (vertexCount - 1) + 1])
				: null
		);

		PointF pviewLast1 = (
			vertexCount > 1
				? new PointF(pviews[2 * (vertexCount - 2)], pviews[2 * (vertexCount - 2) + 1])
				: null
		);

		PointF vecViewCenterLast = (
			vertexCount > 1
				? GeometryUtil.makeVector(pointViewCenter, pviewLast0)
				: null
		);

		PointF vecViewCenterZero = (
			isPolygon && vertexCount > 1
				? GeometryUtil.makeVector(pointViewCenter, pview0)
				: null
		);

		// For polygon: Make a fill
		if (isPolygon && vertexCount > 1) {
			sharedPath.reset();

			sharedPath.moveTo(pointViewCenter.x, pointViewCenter.y);

			for (int i = 0; i < pviews.length; i += 2) {
				sharedPath.lineTo(pviews[i], pviews[i + 1]);
			}

			sharedPath.close();

			sharedPaint.reset();
			sharedPaint.setStyle(Paint.Style.FILL);
			sharedPaint.setColor(fillColor);

			canvas.drawPath(sharedPath, sharedPaint);
		}

		// Draw lines

		// Prepare sharedPaint
		sharedPaint.reset();
		sharedPaint.setStyle(Paint.Style.STROKE);
		sharedPaint.setStrokeJoin(Paint.Join.ROUND);
		sharedPaint.setStrokeCap(Paint.Cap.ROUND);
		sharedPaint.setStrokeWidth(lineWidth);
		sharedPaint.setPathEffect(lineEffect);

		if (highlightActiveParallel && vertexCount >= 3) {
			sharedPath.reset();

			Path normalPath = new Path();

			boolean hasParallelCenterLast = false;
			boolean hasParallelCenterZero = false;

			for (int i = 0; i < pviews.length - 3; i += 2) {
				PointF vec = new PointF(
					pviews[i] - pviews[i + 2],
					pviews[i + 1] - pviews[i + 3]
				);

				boolean parallelWithCenterLast = (
					Math.abs(GeometryUtil.angleInDegreeBetweenLines(vec, vecViewCenterLast))
					< DEGREE_EQUALITY_EPS
				);

				boolean parallelWithCenterZero = (
					vecViewCenterZero != null
					&& Math.abs(GeometryUtil.angleInDegreeBetweenLines(vec, vecViewCenterZero))
						< DEGREE_EQUALITY_EPS
				);

				hasParallelCenterLast |= parallelWithCenterLast;
				hasParallelCenterZero |= parallelWithCenterZero;

				addLineToPathInHighlighting(
					pviews[i], pviews[i + 1], pviews[i + 2], pviews[i + 3],
					parallelWithCenterLast || parallelWithCenterZero ? sharedPath : normalPath
				);

			}

			if (
				vecViewCenterZero != null
				&&
				Math.abs(
					GeometryUtil.angleInDegreeBetweenLines(vecViewCenterLast, vecViewCenterZero)
				) < DEGREE_EQUALITY_EPS
			) {
				hasParallelCenterLast = true;
				hasParallelCenterZero = true;
			}

			addLineToPathInHighlighting(
				pviewLast0.x, pviewLast0.y, pointViewCenter.x, pointViewCenter.y,
				hasParallelCenterLast ? sharedPath : normalPath
			);

			sharedPaint.setColor(DEFAULT_HIGHLIGHT_COLOR);
			canvas.drawPath(sharedPath, sharedPaint);

			sharedPaint.setColor(lineColor);
			canvas.drawPath(normalPath, sharedPaint);

			if (hasParallelCenterZero) {
				sharedPath.reset();

				addLineToPathInHighlighting(
					pointViewCenter.x, pointViewCenter.y, pview0.x, pview0.y,
					sharedPath
				);

				sharedPaint.setColor(DEFAULT_HIGHLIGHT_COLOR);
				sharedPaint.setStrokeWidth(lineWidth * 0.5f);

				canvas.drawPath(sharedPath, sharedPaint);
			}

		} else if (vertexCount > 0) {
			sharedPath.reset();

			sharedPath.moveTo(pviews[0], pviews[1]);

			for (int i = 2; i < pviews.length; i += 2) {
				sharedPath.lineTo(pviews[i], pviews[i + 1]);
			}

			sharedPath.lineTo(pointViewCenter.x, pointViewCenter.y);

			sharedPaint.setColor(lineColor);
			canvas.drawPath(sharedPath, sharedPaint);
		}

		// Highlight right angle, if required
		if (highlightActiveRightAngle && vertexCount > 1) {
			checkAndHighlightRightAngle(
				pviewLast1, pviewLast0, pointViewCenter, canvas, sharedPaint, sharedPath
			);

			if (isPolygon) {
				checkAndHighlightRightAngle(
					pointViewCenter, pview0, pview1, canvas, sharedPaint, sharedPath
				);

				checkAndHighlightRightAngle(
					pviewLast0, pointViewCenter, pview0, canvas, sharedPaint, sharedPath
				);

			}

		}

		// Draw points
		sharedPaint.reset();
		sharedPaint.setStrokeCap(Paint.Cap.ROUND);
		sharedPaint.setStrokeJoin(Paint.Join.ROUND);

		for (int i = 0; i < pviews.length; i += 2) {
			sharedPaint.setColor(DEFAULT_VERTEX_SHADOWCOLOR);
			sharedPaint.setStrokeWidth(vertexSize);
			canvas.drawPoint(pviews[i], pviews[i + 1], sharedPaint);

			sharedPaint.setColor(vertexColor);
			sharedPaint.setStrokeWidth(vertexSize - 2);
			canvas.drawPoint(pviews[i], pviews[i + 1], sharedPaint);
		}

		sharedPaint.setColor(DEFAULT_VERTEX_SHADOWCOLOR);
		sharedPaint.setStrokeWidth(vertexSize);
		canvas.drawPoint(viewWidth / 2f, viewHeight / 2f, sharedPaint);

		sharedPaint.setColor(DEFAULT_LASTVERTEX_COLOR);
		sharedPaint.setStrokeWidth(vertexSize - 2);
		canvas.drawPoint(viewWidth / 2f, viewHeight / 2f, sharedPaint);
	}


	public Parcelable getDataInProgress() {
		PolyEditorData res = new PolyEditorData();

		res.isPolygon = isPolygon;
		res.mustbeConvex = mustbeConvex;
		res.minVertexCount = minVertexCount;
		res.maxVertexCount = maxVertexCount;
		res.vertices = new ArrayList<>(vertices);

		return res;
	}


	public void cancel() {
		// It can be immplemented differently, such as displaying a dialogue box asking
		// user whether to save or not. Right now, just implement the simplest way

		cancelImmediately();
	}


	public void cancelImmediately() {
		if (cancelListener == null) {
			return;
		}

		ArrayList<PointF> result = new ArrayList<>(vertices);
		result.add(getCenterViewPointInOriginal());

		int finalVertexCount = result.size();

		int state = CANCEL_IMMEDIATE_REQUEST_USEABLE;

		if (
			(isPolygon && finalVertexCount < 3)
				|| (!isPolygon && finalVertexCount < 2)
				|| (minVertexCount > 0 && finalVertexCount < minVertexCount)
				|| (maxVertexCount > 0 && finalVertexCount > maxVertexCount)
			) {
			state = CANCEL_IMMEDIATE_REQUEST_UNUSEABLE;
		}

		cancelListener.onCancel(state, result);
	}


	private void prepareCommandControl() {
		linBtnListCommands = new LinearButtonListLayout(objectBasedCanvas.getContext());

		linBtnListCommands.setOrientation(LinearLayout.VERTICAL);
		linBtnListCommands.setButtons(
				new String[] {"Add vertex", "Remove last", "Save", "Cancel"},
				new int[] {
					R.drawable.ic_add,
					R.drawable.ic_delete,
					R.drawable.ic_check,
					R.drawable.ic_clear
				}
		);

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);

		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

		layoutParams.setMargins(0, BUTTON_LIST_MARGIN_RIGHT, 0, 0);

		linBtnListCommands.setLayoutParams(layoutParams);

		linBtnListCommands.setOnButtonClickListener(onButtonClickListener);
		objectBasedCanvas.addView(linBtnListCommands);
	}


	private PointF getCenterViewPointInOriginal() {
		PointF res = new PointF();

		objectBasedCanvas.mapViewToOriginalPoint(
			objectBasedCanvas.getViewWidth() / 2f,
			objectBasedCanvas.getViewHeight() / 2f,
			res
		);

		return res;
	}


	private void checkAndHighlightRightAngle(
		PointF pviewA, PointF pviewB, PointF pviewC,
		Canvas canvas, Paint sharedPaint, Path sharedPath
	) {
		PointF vecViewBA = GeometryUtil.makeVector(pviewB, pviewA);
		PointF vecViewBC = GeometryUtil.makeVector(pviewB, pviewC);

		float lenBA = vecViewBA.length();
		float lenBC = vecViewBC.length();

		if (
			Math.abs(GeometryUtil.angleInDegreeBetweenLines(vecViewBA, vecViewBC) - 90)
			>= DEGREE_EQUALITY_EPS
		) {
			return;
		}

		float markLineLength = vertexSize * 2;

		PointF pmarkBA = new PointF(
			pviewB.x + vecViewBA.x * markLineLength / lenBA,
			pviewB.y + vecViewBA.y * markLineLength / lenBA
		);

		PointF pmarkBC = new PointF(
			pviewB.x + vecViewBC.x * markLineLength / lenBC,
			pviewB.y + vecViewBC.y * markLineLength / lenBC
		);

		PointF pmarkMid = new PointF(
			pmarkBA.x + vecViewBC.x * markLineLength / lenBC,
			pmarkBA.y + vecViewBC.y * markLineLength / lenBC
		);

		sharedPath.reset();
		sharedPath.moveTo(pmarkBA.x, pmarkBA.y);
		sharedPath.lineTo(pmarkMid.x, pmarkMid.y);
		sharedPath.lineTo(pmarkBC.x, pmarkBC.y);

		sharedPaint.reset();
		sharedPaint.setStyle(Paint.Style.STROKE);
		sharedPaint.setStrokeJoin(Paint.Join.ROUND);
		sharedPaint.setStrokeCap(Paint.Cap.ROUND);
		sharedPaint.setStrokeWidth(lineWidth * 0.3f);
		sharedPaint.setColor(DEFAULT_HIGHLIGHT_COLOR);

		canvas.drawPath(sharedPath, sharedPaint);
	}


	private void addLineToPathInHighlighting(
		float x1, float y1, float x2, float y2, Path path
	) {
		path.moveTo(x1, y1);
		path.lineTo(x2, y2);
	}


	/**
	 *
	 * @param newPoint
	 * @param vertices
	 * @return 0 if no duplication, 1 if duplicate with last point, -1 if duplicate with first point
	 */
	private int checkDuplicateFirstAndLastVertex(PointF newPoint, ArrayList<PointF> vertices) {
		if (vertices == null || newPoint == null) {
			return 0;
		}

		if (vertices.size() == 0) {
			return 0;
		}

		PointF adjPoint;

		adjPoint = vertices.get(vertices.size() - 1);

		if (
			GeometryUtil.distancePointToPoint(
				newPoint.x, newPoint.y, adjPoint.x, adjPoint.y
			) < DUPLICATED_POINT_EPS
		) {
			return 1;
		}

		adjPoint = vertices.get(0);

		if (
			GeometryUtil.distancePointToPoint(
				newPoint.x, newPoint.y, adjPoint.x, adjPoint.y
			) < DUPLICATED_POINT_EPS
		) {
			return -1;
		}

		return 0;
	}


	private void toastMessage(String message) {
		Toast.makeText(
			objectBasedCanvas.getContext(),
			message,
			Toast.LENGTH_SHORT
		).show();

	}






	private class MyOnButtonClickListener
	implements LinearButtonListLayout.OnButtonClickListener {

		@Override
		public void onClick(int index) {
			switch (index) {
				// Add vertex
				case 0:
					if (vertices.size() + 1 == maxVertexCount) {
						toastMessage(
							"You have reached vertex limit. "
							+ "Maximum number of vertices is "
							+ maxVertexCount
						);

						return;
					}

					PointF newPoint = getCenterViewPointInOriginal();

					// Check if users accidentally duplicate point
					if (vertices.size() > 0) {
						int dup = checkDuplicateFirstAndLastVertex(newPoint, vertices);

						if (dup == 1) {
							toastMessage("The new vertex is duplicated with the last one");
							return;
						}

						if (dup == -1) {
							toastMessage("The new vertex is duplicated with the first one");
							return;
						}

					}

					vertices.add(newPoint);

					objectBasedCanvas.invalidateCanvas();

					break;

				// Remove last vertex
				case 1:
					if (!vertices.isEmpty()) {
						vertices.remove(vertices.size() - 1);

						objectBasedCanvas.invalidateCanvas();
					}

					break;

				// Save.
				case 2:
					// NOTES: We regard (drawVertices + (Active point)) for final result
					int finalVertexCount = vertices.size() + 1;

					if (isPolygon && finalVertexCount < 3) {
						toastMessage("Polygon needs at least 3 vertices");
						break;
					}

					if (!isPolygon && finalVertexCount < 2) {
						toastMessage("Polyline needs at least 2 vertices");
						break;
					}

					if (minVertexCount > 0 && finalVertexCount < minVertexCount) {
						toastMessage(
							"You have not added enough vertices. "
							+ "Minimum number of vertices is "
							+ minVertexCount
						);
						break;
					}

					if (maxVertexCount > 0 && finalVertexCount > maxVertexCount) {
						toastMessage(
							"You have added too many vertices. "
							+ "Maximum number of vertices is "
							+ maxVertexCount
						);

						break;
					}

					ArrayList<PointF> result = new ArrayList<>(vertices);
					PointF lastPoint = getCenterViewPointInOriginal();

					// Check duplication
					int dup = checkDuplicateFirstAndLastVertex(lastPoint, result);

					if (dup == 1) {
						toastMessage("Two last vertices are duplicated");
						return;
					}

					if (dup == -1) {
						toastMessage("The first and the last vertex are duplicated");
						return;
					}

					result.add(lastPoint);

					if (
						isPolygon
						&& mustbeConvex
						&& !GeometryUtil.isConvexPolygon(result)
					) {
						toastMessage("Your polygon is not convex");

						break;
					}

					if (cancelListener == null) {
						break;
					}

					cancelListener.onCancel(SAVE_REQUESTED, result);

					break;

				// Cancel
				case 3:
					cancel();

					break;
			}

		}

	}

}
