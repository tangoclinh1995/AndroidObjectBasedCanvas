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
import java.util.Arrays;

/**
 * Created by LinhTa on 1/10/2018.
 */

public class CanvasEditPolyEditor extends CanvasEditor {
	public static final String TAG = "CanvasEditPolyEditor";



	public static final float DEGREE_EQUALITY_EPS = 0.5f;

	public static final int SELECT_POINT_PIXEL_THRESHOLD = 30;
	public static final int SELECT_LINE_PIXEL_THRESHOLD = 15;
	public static final float[] SEGMENT_INSIDE_RATIO_RANGE
		= new float[] {0.2f, 0.8f};

	public static final int BUTTON_LIST_MARGIN_RIGHT = 8;

	public static final int DEFAULT_FIRSTVERTEX_COLOR = Color.rgb(76, 208, 0);
	public static final float DEFAULT_FIRSTVERTEX_LINE_RATIO = 0.1f;

	public static final int DEFAULT_FILLCOLOR = Color.argb(100, 0, 148, 255);

	public static final int DEFAULT_LINECOLOR = Color.BLUE;
	public static final int DEFAULT_LINEWIDTH = 20;
	public static final PathEffect DEFAULT_LINEEFFECT
		= new DashPathEffect(new float[] {50, 50}, 0);

	public static final int DEFAULT_VERTEXCOLOR = Color.WHITE;
	public static final int DEFAULT_VERTEXSIZE = 40;

	public static final int DEFAULT_VERTEX_SHADOWCOLOR = Color.GRAY;
	public static final int DEFAULT_ACTIVE_COLOR = Color.RED;
	public static final int DEFAULT_HIGHLIGH_COLOR = Color.rgb(255, 193, 0);



	public int fillColor = DEFAULT_FILLCOLOR;

	public int lineColor = DEFAULT_LINECOLOR;
	public int lineWidth = DEFAULT_LINEWIDTH;
	public PathEffect lineEffect = DEFAULT_LINEEFFECT;

	public int vertexColor = DEFAULT_VERTEXCOLOR;
	public int vertexSize = DEFAULT_VERTEXSIZE;

	public int firstVertexMarkColor = DEFAULT_FIRSTVERTEX_COLOR;

	public boolean highlightActiveRightAngle = false;
	public boolean highlightActiveParallel = false;

	private LinearButtonListLayout linBtnListCommands;
	private CanvasEditPolyEditor.MyOnButtonClickListener onButtonClickListener
		= new CanvasEditPolyEditor.MyOnButtonClickListener();

	private boolean isPolygon;
	private boolean mustbeConvex;
	private int minVertexCount;
	private int maxVertexCount;
	private ArrayList<PointF> vertices = new ArrayList<>();

	// When both activeVertexId and activeVertexNextId is != -1
	// 		=> A line segment is selected
	// When only activeVertexId != -1
	//		=> A vertex is selected
	private int activeVertexId = -1;
	// If available: activeVertexNexId = (activeVertexId + 1) % vertices.size()
	private int activeVertexNextId = -1;



	public CanvasEditPolyEditor() {
		gestureListener = new MyOnCanvasGestureListener();
	}



	@Override
	public int startByCanvas(
			ObjectBasedCanvas objectBasedCanvas,
			Object initData,
			OnCancelListener cancelListener
	) {
		if (
			super.startByCanvas(objectBasedCanvas, initData, cancelListener)
				== ALREADY_STARTED
		) {
			return ALREADY_STARTED;
		}

		PolyEditorData initDataCasted;

		if (!(initData instanceof PolyEditorData)) {
			return INVALID_INIT_DATA;
		}

		initDataCasted = (PolyEditorData)initData;

		if (
			initDataCasted == null
			|| (initDataCasted != null && initDataCasted.vertices == null)
		) {
			return INVALID_INIT_DATA;
		}

		if (
			(initDataCasted.isPolygon && initDataCasted.vertices.size() < 3)
			|| (!initDataCasted.isPolygon && initDataCasted.vertices.size() < 2)
		) {
			return INVALID_INIT_DATA;
		}

		isPolygon = initDataCasted.isPolygon;
		mustbeConvex = initDataCasted.mustbeConvex;

		minVertexCount = initDataCasted.minVertexCount;
		maxVertexCount = initDataCasted.maxVertexCount;

		vertices.clear();
		GeometryUtil.addListToListPointF(initDataCasted.vertices, vertices);

		prepareCommandControl();

		return STARTED_SUCCESS;
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
		boolean markFirstVertex = (Color.alpha(firstVertexMarkColor) != 0);

		Path sharedPath = new Path();

		int vertexCount = vertices.size();
		float[] pviews = matViewAnalyzer.mapOriginalToViewPoint(vertices);

		// Prepare point that will be used for highlighting first vertex
		PointF pointFirstIndicator = new PointF(
			pviews[0] + (pviews[2] - pviews[0]) * DEFAULT_FIRSTVERTEX_LINE_RATIO,
			pviews[1] + (pviews[3] - pviews[1]) * DEFAULT_FIRSTVERTEX_LINE_RATIO
		);

		// Prepare points that will be used for highlighting parallel
		// and right angle
		int[] idActiveRanges = new int[5];
		PointF[] vertexActiveRanges = new PointF[5];

		PointF vecActiveNext = null;
		PointF vecActivePrev = null;

		Arrays.fill(idActiveRanges, -1);
		Arrays.fill(vertexActiveRanges, null);

		if (
			vertexCount >= 3
			&& activeVertexId != -1
			&& activeVertexNextId == -1
		) {
			for (int i = -2; i <= 2; ++i) {
				idActiveRanges[2 + i]
					= (activeVertexId + i + vertexCount) % vertexCount;

				vertexActiveRanges[2 + i] = new PointF(
					pviews[2 * idActiveRanges[2 + i]],
					pviews[2 * idActiveRanges[2 + i] + 1]
				);

			}

			vecActiveNext = GeometryUtil.makeVector(
				vertexActiveRanges[2], vertexActiveRanges[3]
			);
			vecActivePrev = GeometryUtil.makeVector(
				vertexActiveRanges[2], vertexActiveRanges[1]
			);

		}

		// For polygon: Make a fill
		if (isPolygon) {
			sharedPath.reset();

			sharedPath.moveTo(pviews[0], pviews[1]);

			for (int i = 2; i < pviews.length; i += 2) {
				sharedPath.lineTo(pviews[i], pviews[i + 1]);
			}

			sharedPath.close();

			sharedPaint.reset();
			sharedPaint.setStyle(Paint.Style.FILL);
			sharedPaint.setColor(fillColor);

			canvas.drawPath(sharedPath, sharedPaint);
		}

		// Draw lines & Highlight parallel edges

		// Prepare sharedPaint
		sharedPaint.reset();
		sharedPaint.setStyle(Paint.Style.STROKE);
		sharedPaint.setStrokeJoin(Paint.Join.ROUND);
		sharedPaint.setStrokeCap(Paint.Cap.ROUND);
		sharedPaint.setStrokeWidth(lineWidth);
		sharedPaint.setPathEffect(lineEffect);

		// Draw first vertex line indicator
		if (markFirstVertex) {
			sharedPath.reset();
			sharedPath.moveTo(pviews[0], pviews[1]);
			sharedPath.lineTo(pointFirstIndicator.x, pointFirstIndicator.y);

			sharedPaint.setColor(firstVertexMarkColor);
			canvas.drawPath(sharedPath, sharedPaint);
		}

		if (
			highlightActiveParallel
			&& vertices.size() > 3
			&& activeVertexId != -1 && activeVertexNextId == -1
		) {
			sharedPath.reset();

			boolean activeNextHasParallel = false;
			boolean activePrevHasParallel = false;

			Path normalPath = new Path();

			PointF pcur = new PointF();
			PointF pnext = new PointF();

			for (int i = 0; i < vertexCount; ++i) {
				if (i == idActiveRanges[1] || i == idActiveRanges[2]) {
					continue;
				}

				int nextI = i + 1;
				if (nextI == vertexCount) {
					nextI = 0;
				}

				if (nextI == 0 && !isPolygon) {
					break;
				}

				pcur.set(pviews[2 * i], pviews[2 * i + 1]);
				pnext.set(pviews[2 * nextI], pviews[2 * nextI + 1]);

				PointF vec = GeometryUtil.makeVector(pcur, pnext);

				boolean parallelWithNext = (
					Math.abs(
						GeometryUtil.angleInDegreeBetweenLines(vec, vecActiveNext)
					) < DEGREE_EQUALITY_EPS
				);

				boolean parallelWithPrev = (
					Math.abs(
						GeometryUtil.angleInDegreeBetweenLines(vec, vecActivePrev)
					) < DEGREE_EQUALITY_EPS
				);

				activeNextHasParallel |= parallelWithNext;
				activePrevHasParallel |= parallelWithPrev;

				addLineToPathInHighlighting(
					pcur, pnext,
					i,
					pointFirstIndicator,
					parallelWithNext || parallelWithPrev ? sharedPath : normalPath
				);

			}

			if (
				((idActiveRanges[2] != 0 && idActiveRanges[3] != 0) || isPolygon)
				&&
				Math.abs(
					GeometryUtil.angleInDegreeBetweenLines(vecActivePrev, vecActiveNext)
				) < DEGREE_EQUALITY_EPS
			) {
				activeNextHasParallel = true;
				activePrevHasParallel = true;
			}

			if (idActiveRanges[3] != 0 || isPolygon) {
				addLineToPathInHighlighting(
					vertexActiveRanges[2], vertexActiveRanges[3],
					idActiveRanges[2],
					pointFirstIndicator,
					activeNextHasParallel ? sharedPath : normalPath
				);

			}

			if (idActiveRanges[2] != 0 || isPolygon) {
				addLineToPathInHighlighting(
					vertexActiveRanges[1], vertexActiveRanges[2],
					idActiveRanges[1],
					pointFirstIndicator,
					activePrevHasParallel ? sharedPath : normalPath
				);

			}

			sharedPaint.setColor(DEFAULT_HIGHLIGH_COLOR);
			canvas.drawPath(sharedPath, sharedPaint);

			sharedPaint.setColor(lineColor);
			canvas.drawPath(normalPath, sharedPaint);

		} else {
			sharedPath.reset();

			if (markFirstVertex) {
				sharedPath.moveTo(pointFirstIndicator.x, pointFirstIndicator.y);
			} else {
				sharedPath.moveTo(pviews[0], pviews[1]);
			}

			for (int i = 2; i < pviews.length; i += 2) {
				if (2 * activeVertexNextId == i) {
					sharedPath.moveTo(pviews[i], pviews[i + 1]);
				} else {
					sharedPath.lineTo(pviews[i], pviews[i + 1]);
				}

			}

			if (isPolygon && activeVertexNextId != 0) {
				sharedPath.lineTo(pviews[0], pviews[1]);
			}

			sharedPaint.setColor(lineColor);
			canvas.drawPath(sharedPath, sharedPaint);

			if (activeVertexId != -1 && activeVertexNextId != -1 ) {
				sharedPath.reset();

				sharedPath.moveTo(
					pviews[2 * activeVertexNextId],
					pviews[2 * activeVertexNextId + 1]
				);

				if (activeVertexId == 0 && markFirstVertex) {
					sharedPath.lineTo(
						pointFirstIndicator.x,
						pointFirstIndicator.y
					);

				} else {
					sharedPath.lineTo(
						pviews[2 * activeVertexId],
						pviews[2 * activeVertexId + 1]
					);

				}

				sharedPaint.setColor(DEFAULT_ACTIVE_COLOR);
				canvas.drawPath(sharedPath, sharedPaint);
			}

		}

		// Highlight right angle, if required
		if (
			highlightActiveRightAngle
			&& vertexCount >= 3
			&& activeVertexId != -1
			&& activeVertexNextId == -1
		) {
			if (
				(idActiveRanges[1] != 0 && idActiveRanges[2] != 0)
				|| isPolygon
			) {
				checkAndHighlightRightAngle(
					vertexActiveRanges[0],
					vertexActiveRanges[1],
					vertexActiveRanges[2],
					canvas, sharedPaint, sharedPath
				);

			}

			if (
				(idActiveRanges[2] != 0 && idActiveRanges[3] != 0)
				|| isPolygon
			) {
				checkAndHighlightRightAngle(
					vertexActiveRanges[1],
					vertexActiveRanges[2],
					vertexActiveRanges[3],
					canvas, sharedPaint, sharedPath
				);

			}

			if (
				(idActiveRanges[3] != 0 && idActiveRanges[4] != 0)
				|| isPolygon
			) {
				checkAndHighlightRightAngle(
					vertexActiveRanges[2],
					vertexActiveRanges[3],
					vertexActiveRanges[4],
					canvas, sharedPaint, sharedPath
				);

			}

		}

		// Draw points
		sharedPaint.reset();
		sharedPaint.setStrokeCap(Paint.Cap.ROUND);
		sharedPaint.setStrokeJoin(Paint.Join.ROUND);

		for (int i = 0; i < pviews.length; i += 2) {
			int vertexId = i / 2;

			sharedPaint.setColor(DEFAULT_VERTEX_SHADOWCOLOR);
			sharedPaint.setStrokeWidth(vertexSize);
			canvas.drawPoint(pviews[i], pviews[i + 1], sharedPaint);

			if (vertexId == activeVertexId || vertexId == activeVertexNextId) {
				sharedPaint.setColor(DEFAULT_ACTIVE_COLOR);
			} else if (vertexId == 0 && markFirstVertex) {
				sharedPaint.setColor(firstVertexMarkColor);
			} else {
				sharedPaint.setColor(vertexColor);
			}

			sharedPaint.setStrokeWidth(vertexSize - 2);
			canvas.drawPoint(pviews[i], pviews[i + 1], sharedPaint);
		}

	}


	public void cancel() {
		// It can be immplemented differently,
		// such as displaying a dialogue box asking user whether to save or not.
		// Right now, just implement the simplest way
		cancelImmediately();
	}


	public void cancelImmediately() {
		if (cancelListener == null) {
			return;
		}

		ArrayList<PointF> result = new ArrayList<>(vertices);

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
		linBtnListCommands = new LinearButtonListLayout(
			objectBasedCanvas.getContext()
		);

		linBtnListCommands.setOrientation(LinearLayout.VERTICAL);
		linBtnListCommands.setButtons(
			new String[] {
				"Add vertex", "Remove vertex", "Save", "Cancel"
			},
			new int[] {
				R.drawable.ic_add,
				R.drawable.ic_delete,
				R.drawable.ic_check,
				R.drawable.ic_clear
			}
		);

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		);

		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

		layoutParams.setMargins(0, BUTTON_LIST_MARGIN_RIGHT, 0, 0);

		linBtnListCommands.setLayoutParams(layoutParams);

		linBtnListCommands.setOnButtonClickListener(onButtonClickListener);

		// Initially: Hide "Add vertex" and "Remove vertex" button
		linBtnListCommands.setButtonVisible(0, false);
		linBtnListCommands.setButtonVisible(1, false);

		objectBasedCanvas.addView(linBtnListCommands);
	}


	private void cleanActiveVertices() {
		activeVertexId = -1;
		activeVertexNextId = -1;
	}


	private void showHideControlButtons() {
		if (activeVertexId == -1 && activeVertexNextId == -1) {
			// No thing selected => Hide "Add vertex" and "Remove vertex" button
			linBtnListCommands.setButtonVisible(0, false);
			linBtnListCommands.setButtonVisible(1, false);

		} else if (activeVertexId != -1 && activeVertexNextId == -1) {
			// A vertex is selected => Show "Remove vertex" button
			linBtnListCommands.setButtonVisible(0, false);
			linBtnListCommands.setButtonVisible(1, true);

		} else if (activeVertexId != -1 && activeVertexNextId != -1) {
			// An edge is selected => Show "Add vertex" button
			linBtnListCommands.setButtonVisible(0, true);
			linBtnListCommands.setButtonVisible(1, false);
		}

	}


	private void addLineToPathInHighlighting(
		PointF start, PointF end,
		int startId,
		PointF firstVertexIndicator,
		Path path
	) {
		if (startId == 0 && Color.alpha(firstVertexMarkColor) != 0) {
			path.moveTo(firstVertexIndicator.x, firstVertexIndicator.y);
		} else {
			path.moveTo(start.x, start.y);
		}

		path.lineTo(end.x, end.y);
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
			Math.abs(
				GeometryUtil.angleInDegreeBetweenLines(vecViewBA, vecViewBC)
				- 90
			) >= DEGREE_EQUALITY_EPS
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
		sharedPaint.setColor(DEFAULT_HIGHLIGH_COLOR);

		canvas.drawPath(sharedPath, sharedPaint);
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
					if (activeVertexId == -1) {
						break;
					}

					if (
						isPolygon
						&&
						(activeVertexId + 1) % vertices.size()
							!= activeVertexNextId
					) {
						break;
					}

					if (
						!isPolygon
						&&
						activeVertexId + 1 != activeVertexNextId
					) {
						break;
					}

					if (maxVertexCount > 0 && vertices.size() > maxVertexCount) {
						toastMessage(
							"You have reached vertex limit. Maximum number of vertices is "
							+ maxVertexCount
						);

						break;
					}

					PointF pa = vertices.get(activeVertexId);
					PointF pb = vertices.get(activeVertexNextId);

					PointF newPoint = new PointF(
						(pa.x + pb.x) / 2,
						(pa.y + pb.y) /2
					);

					vertices.add(activeVertexId + 1, newPoint);

					cleanActiveVertices();
					showHideControlButtons();

					objectBasedCanvas.invalidateCanvas();

					break;

				// Remove vertex
				case 1:
					if (activeVertexId == -1 || activeVertexNextId != -1) {
						break;
					}

					int vertexCount = vertices.size();

					if (isPolygon && vertexCount == 3) {
						toastMessage(
							"Cannot remove because a polygon needs at least"
							+ " 3 vertices"
						);

						break;
					}

					if (!isPolygon && vertexCount == 2) {
						toastMessage(
							"Cannot remove because a polyline needs at least"
							+ " 2 points"
						);

						break;
					}

					vertices.remove(activeVertexId);

					cleanActiveVertices();
					showHideControlButtons();

					objectBasedCanvas.invalidateCanvas();

					break;

				// Save
				case 2:
					vertexCount = vertices.size();

					if (minVertexCount > 0 && vertexCount < minVertexCount) {
						toastMessage(
							"You have not added enough vertices."
							+ " Mininum number of vertices is "
							+ minVertexCount
						);

						break;
					}

					if (maxVertexCount > 0 && vertexCount > maxVertexCount) {
						toastMessage(
							"You have added too many vertices."
							+ " Maximum number of vertices is "
							+ maxVertexCount
						);

						break;
					}

					if (
						isPolygon
						&& mustbeConvex
						&& !GeometryUtil.isConvexPolygon(vertices)
					) {
						toastMessage("Your polygon is not convex");

						break;
					}

					if (cancelListener == null) {
						break;
					}

					cancelListener.onCancel(
						SAVE_REQUESTED,
						new ArrayList<>(vertices)
					);

					break;

				// Cancel
				case 3:
					cancel();

					break;
			}

		}

	}






	private class MyOnCanvasGestureListener implements OnCanvasGestureListener {

		@Override
		public boolean onClick(float x, float y) {
			if (!selectNewActive(x, y)) {
				cleanActiveVertices();
			}

			showHideControlButtons();

			objectBasedCanvas.invalidateCanvas();

			return true;
		}


		@Override
		public boolean onLongPress(float x, float y) {
			return false;
		}


		@Override
		public boolean onTouchMoveStart(float x, float y) {
			if (!selectNewActive(x, y)) {
				// When nothing new is selected:
				//      If a vertex is previously selected => Clear that selection
				//      If a segment is previously selected => Keep it
				if (activeVertexId != -1 && activeVertexNextId == -1) {
					cleanActiveVertices();
				}

			}

			showHideControlButtons();

			objectBasedCanvas.invalidateCanvas();

			return false;
		}


		@Override
		public boolean onTouchMove(float x, float y, float[] translateParams) {
			// A vertex is being selected
			if (activeVertexId != -1 && activeVertexNextId == -1) {
				objectBasedCanvas.mapViewToOriginalPoint(
					x, y, vertices.get(activeVertexId)
				);

				objectBasedCanvas.invalidateCanvas();

				return true;
			}

			return false;
		}


		@Override
		public boolean onTouchMoveEnd() {
			return false;
		}


		@Override
		public boolean onMultiTouchMoveStart() {
			// Clean selection of a vertex is currently selected
			if (activeVertexId != -1 && activeVertexNextId == -1) {
				cleanActiveVertices();

				showHideControlButtons();

				objectBasedCanvas.invalidateCanvas();

				return true;
			}

			return false;
		}


		@Override
		public boolean onMultiTouchMove(
			float[] translateParams, float[] scaleParams, float[] rotateParams
		) {
			return false;
		}


		@Override
		public boolean onMultiTouchMoveEnd() {
			return false;
		}


		private boolean selectNewActive(float x, float y) {
			ViewMatrixAnalyzer matViewAnalyzer
				= objectBasedCanvas.getViewMatrixAnalyzer();

			PointF p = new PointF();
			matViewAnalyzer.mapViewToOriginalPoint(x, y, p);

			int nearestVertexId = -1;
			// The distance value is in screen VIEW (not ORIGINAL)
			// coordinate system to make use of screen pixel metrics
			float distToNearestVertex = Float.POSITIVE_INFINITY;

			int nearestSegmentStartVertexId = -1;
			// The distance value is in screen VIEW (not ORIGINAL)
			// coordinate system to make use of screen pixel metrics
			float distToNearestSegment = Float.POSITIVE_INFINITY;

			int nVertices = vertices.size();

			PointF cp = new PointF();
			PointF np = new PointF();

			PointF va = new PointF();
			PointF vb = new PointF();

			for (int i = 0; i < nVertices; ++i) {
				cp.set(vertices.get(i));

				va.set(p.x - cp.x, p.y - cp.y);

				float d = va.length();

				if (d < distToNearestVertex) {
					nearestVertexId = i;
					distToNearestVertex = d;
				}

				if (!isPolygon && i == nVertices - 1) {
					break;
				}

				np.set(vertices.get((i + 1) % nVertices));


				vb.set(np.x - cp.x, np.y - cp.y);
				float magVb = vb.length();

				// Calculate the projection of vector (cp->p) to (cp->np)
				float proj = (va.x * vb.x + va.y * vb.y) / magVb;

				if (
					proj < magVb * SEGMENT_INSIDE_RATIO_RANGE[0]
					|| proj > magVb * SEGMENT_INSIDE_RATIO_RANGE[1]
				) {
					// This means the projection of p into the line (cp->np) is
					// NOT inside the "selectable" part of the line segment (cp->np)
					// => Ignore this line segment
					continue;
				}

				d = (float)Math.sqrt(Math.pow(va.length(), 2) - proj * proj);

				if (d < distToNearestSegment) {
					nearestSegmentStartVertexId = i;
					distToNearestSegment = d;
				}

			}

			// Convert from ORIGINAL to VIEW unit
			distToNearestVertex *= matViewAnalyzer.getScale();
			distToNearestSegment *= matViewAnalyzer.getScale();

			if (distToNearestVertex < SELECT_POINT_PIXEL_THRESHOLD) {
				// Select a vertex
				activeVertexId = nearestVertexId;
				activeVertexNextId = -1;

				return true;

			} else if (distToNearestSegment < SELECT_LINE_PIXEL_THRESHOLD) {
				// Select a line segment
				activeVertexId = nearestSegmentStartVertexId;
				activeVertexNextId = (activeVertexId + 1) % nVertices;

				return true;
			}

			return false;
		}

	}

}
