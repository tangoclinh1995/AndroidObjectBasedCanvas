package tnl.objcanvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;



/**
 * Created by Linh Ta on 1/4/2018.
 */

public class ObjectBasedCanvas extends RelativeLayout {
	public static final String TAG = "ObjectBasedCanvas";



	public static final float FITBOUND_RATIO = 0.8f;

	public static final int FITBOUND_IF_ZOOMIN = 1;
	public static final int FITBOUND_IF_ZOOMOUT = 2;
	public static final int FITBOUND_ALWAYS = 3;



	private MyUnderlyingCanvas viewUnderlyingCanvas;

	private Object viewWidthHeightSynObj = new Object();
	private int viewWidth = 0;
	private int viewHeight = 0;

	private Object matViewSynObj = new Object();
	private Matrix matView = new Matrix();

	private Paint paint = new Paint();

	private CanvasGestureDetector gestureDetector = new CanvasGestureDetector();
	private MyCanvasOnCanvasGestureListener internalGestureListener
		= new MyCanvasOnCanvasGestureListener();

	private OnCanvasGestureListener externalGestureListener = null;

	private AtomicInteger counterCanvasObj = new AtomicInteger();

	private final Map<Integer, CanvasObjectHolder> mapIntObjHolder
		= Collections.synchronizedMap(new HashMap<Integer, CanvasObjectHolder>());

	private final SortedSet<PairZOrderId> setObjZOrder
		= Collections.synchronizedSortedSet(new TreeSet<PairZOrderId>());

	private final SortedSet<Integer> setLazyUpdateObj
		= Collections.synchronizedSortedSet(new TreeSet<Integer>());

	private MyLazyUpdateThread thrLazyUpdate = new MyLazyUpdateThread();

	private boolean editing = false;

	private CanvasEditor editor = null;
	private OnCanvasGestureListener editorGestureListener = null;
	private CanvasEditor.OnCancelListener editorGivenOnCancelListener = null;

	private CanvasEditor.OnCancelListener canvasManagedOnCancelListener
		= new MyEditorOnCancelListener();

	private boolean isTouching = false;



	public ObjectBasedCanvas(Context context) {
		super(context);
		init(context);
	}


	public ObjectBasedCanvas(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}


	public ObjectBasedCanvas(
		Context context, AttributeSet attrs, int defStyleAttr
	) {
		super(context, attrs, defStyleAttr);
		init(context);
	}


	/**
	 *
	 * @param obj
	 * @return id of the newly created object
	 */
	public int addObject(CanvasObject obj) {
		return addObject(obj, 0, true);
	}


	/**
	 *
	 * @param obj
	 * @param zOrder
	 * @param visible
	 * @return id of the newly created object
	 */
	public int addObject(CanvasObject obj, int zOrder, boolean visible) {
		int id = counterCanvasObj.incrementAndGet();

		mapIntObjHolder.put(id, new CanvasObjectHolder(obj, zOrder, visible));
		setObjZOrder.add(new PairZOrderId(zOrder, id));

		if (obj.needLazyUpdate()) {
			setLazyUpdateObj.add(id);
		}

		return id;
	}


	/**
	 *
	 * @param id
	 * @return false if id does not exist
	 */
	public boolean removeObject(int id) {
		CanvasObjectHolder holder = mapIntObjHolder.remove(id);

		// holder can NEVER be null, so if the result is null, then key doesn't exist
		if (holder == null) {
			return false;
		} else {
			if (holder.canvasObject.needLazyUpdate()) {
				setLazyUpdateObj.remove(id);
			}

			return true;
		}

	}


	/**
	 *
	 * @param id
	 * @return null if id does not exist
	 */
	public CanvasObject getObject(int id) {
		CanvasObjectHolder holder = mapIntObjHolder.get(id);

		if (holder == null) {
			return null;
		};

		return holder.canvasObject;
	}


	/**
	 *
	 * @param id
	 * @param obj
	 * @return false if id has existed
	 */
	public boolean setObject(int id, CanvasObject obj) {
		if (obj == null) {
			throw new IllegalArgumentException("obj cannot be null");
		}

		CanvasObjectHolder holder = mapIntObjHolder.get(id);

		if (holder == null) {
			return false;
		}

		boolean prevNeedLazyUpdate = holder.canvasObject.needLazyUpdate();

		holder.canvasObject = obj;

		if (obj.needLazyUpdate() && !prevNeedLazyUpdate) {
			setLazyUpdateObj.add(id);
		} else if (!obj.needLazyUpdate() && prevNeedLazyUpdate) {
			setLazyUpdateObj.remove(id);
		}

		postInvalidateCanvas();

		return true;
	}


	/**
	 *
	 * @param id
	 * @return -1 if object id does not exist, 0 if invisible, 1 if visible
	 */
	public int getObjectVisible(int id) {
		CanvasObjectHolder holder = mapIntObjHolder.get(id);

		if (holder == null) {
			return -1;
		}

		if (holder.visible) {
			return 1;
		} else {
			return 0;
		}

	}


	/**
	 *
	 * @param id
	 * @return false if id does not exist
	 */
	public boolean setObjectVisible(int id, boolean visible) {
		CanvasObjectHolder holder = mapIntObjHolder.get(id);

		if (holder == null) {
			return false;
		}

		if (holder.visible != visible) {
			holder.visible = visible;

			postInvalidateCanvas();
		}

		return true;
	}


	/**
	 *
	 * @param id
	 * @return -1 if object id does not exist
	 */
	public int getObjectZOrder(int id) {
		CanvasObjectHolder holder = mapIntObjHolder.get(id);

		if (holder == null) {
			return -1;
		}

		return holder.zOrder;
	}


	/**
	 *
	 * @param id
	 * @return false if id does not exist
	 */
	public boolean setObjectZOrder(int id, int zOrder) {
		CanvasObjectHolder holder = mapIntObjHolder.get(id);

		if (holder == null) {
			return false;
		}

		synchronized (holder.zOrderSyncObj) {
			if (holder.zOrder != zOrder) {
				setObjZOrder.remove(new PairZOrderId(holder.zOrder, id));

				holder.zOrder = zOrder;

				setObjZOrder.add(new PairZOrderId(zOrder, id));

				postInvalidateCanvas();
			}

		}

		return true;
	}


	public boolean isEditing() {
		return editing;
	}


	public int startEditorImmediatey(
		CanvasEditor editor,
		Object initData,
		CanvasEditor.OnCancelListener onCancelListener
	) {
		if (this.editor != null) {
			this.editor.cancelImmediately();
		}

		clearEditor();

		int status = editor.startByCanvas(
			this,
			initData,
			canvasManagedOnCancelListener
		);

		if (status == CanvasEditor.STARTED_SUCCESS) {
			this.editor = editor;
			this.editorGestureListener = editor.getGestureListener();
			this.editorGivenOnCancelListener = onCancelListener;

			editing = true;
		}

		return status;
	}


	public int getViewWidth() {
		int res;

		synchronized (viewWidthHeightSynObj) {
			res = viewWidth;
		}

		return res;
	}


	public int getViewHeight() {
		int res;

		synchronized (viewWidthHeightSynObj) {
			res = viewHeight;
		}

		return res;
	}


	public ViewMatrixAnalyzer getViewMatrixAnalyzer() {
		ViewMatrixAnalyzer res = new ViewMatrixAnalyzer();

		synchronized (matViewSynObj) {
			res.set(matView);
		}

		return res;
	}


	/**
	 *
	 * @param viewMatrix If null, viewMatrix is regarded as identity matrix
	 */
	public void setViewMatrix(Matrix viewMatrix) {
		synchronized (matViewSynObj) {
			matView.set(viewMatrix);
		}

		postInvalidateCanvas();
	}


	public void mapViewToOriginalPoint(float x, float y, PointF res) {
		ViewMatrixAnalyzer matViewAnalzyer = new ViewMatrixAnalyzer();

		synchronized (matViewSynObj) {
			matViewAnalzyer.set(matView);
		}

		matViewAnalzyer.mapViewToOriginalPoint(x, y, res);
	}


	public void setOnCanvasGestureListener(OnCanvasGestureListener listener) {
		this.externalGestureListener = listener;
	}


	public void goToBound(RectF bound, int fitBoundMode) {
		if (
			Float.isNaN(bound.top) || Float.isInfinite(bound.top)
			|| Float.isNaN(bound.left) || Float.isInfinite(bound.left)
			|| Float.isNaN(bound.bottom) || Float.isInfinite(bound.bottom)
				|| Float.isNaN(bound.right) || Float.isInfinite(bound.right)
		) {
			return;
		}

		Matrix matViewCpy;
		int viewWidthCpy;
		int viewHeightCpy;

		synchronized (matViewSynObj) {
			matViewCpy = new Matrix(this.matView);
		}

		synchronized (viewWidthHeightSynObj) {
			viewWidthCpy = viewWidth;
			viewHeightCpy = viewHeight;
		}

		RectF viewBound = new RectF();
		matViewCpy.mapRect(viewBound, bound);

		matViewCpy.postTranslate(
			viewWidthCpy / 2f - viewBound.centerX(),
			viewHeightCpy / 2f - viewBound.centerY()
		);

		float scaleRatio;

		if (viewHeightCpy < viewWidthCpy) {
			scaleRatio = viewHeightCpy * FITBOUND_RATIO / viewBound.height();
		} else {
			scaleRatio = viewWidthCpy * FITBOUND_RATIO / viewBound.width();
		}

		boolean needScale = false;

		if (fitBoundMode == FITBOUND_ALWAYS) {
			needScale = true;
		} else if (fitBoundMode == FITBOUND_IF_ZOOMIN && scaleRatio > 1) {
			needScale = true;
		} else if (fitBoundMode == FITBOUND_IF_ZOOMOUT && scaleRatio < 1) {
			needScale = true;
		}

		if (needScale) {
			matViewCpy.postScale(scaleRatio, scaleRatio, viewWidthCpy / 2f, viewHeightCpy / 2f);
		}

		synchronized (matViewSynObj) {
			matView.set(matViewCpy);
		}

		postInvalidateCanvas();
	}


	public void goToPoint(float x, float y) {
		synchronized (viewWidthHeightSynObj) {
			synchronized (matViewSynObj) {
				float[] coors = new float[]{x, y};

				matView.mapPoints(coors);

				matView.postTranslate(
					viewWidth / 2f - coors[0],
					viewHeight / 2f - coors[1]
				);

			}

		}

		postInvalidateCanvas();
	}


	public void invalidateCanvas() {
		viewUnderlyingCanvas.invalidate();
	}


	public void postInvalidateCanvas() {
		viewUnderlyingCanvas.postInvalidate();
	}


	@Override
	protected void onLayout(
		boolean changed, int left, int top, int right, int bottom
	) {
		super.onLayout(changed, left, top, right, bottom);

		synchronized (viewWidthHeightSynObj) {
			viewWidth = this.getWidth();
			viewHeight = this.getHeight();
		}

	}


	private void init(Context context) {
		gestureDetector.setGestureListener(internalGestureListener);

		viewUnderlyingCanvas = new MyUnderlyingCanvas(context);

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		);

		layoutParams.setMargins(0, 0, 0, 0);
		layoutParams.addRule(ALIGN_PARENT_LEFT, TRUE);
		layoutParams.addRule(ALIGN_PARENT_RIGHT, TRUE);
		layoutParams.addRule(ALIGN_PARENT_TOP, TRUE);
		layoutParams.addRule(ALIGN_PARENT_BOTTOM, TRUE);

		viewUnderlyingCanvas.setLayoutParams(layoutParams);

		addView(viewUnderlyingCanvas);
	}


	private void lazyUpdate() {
		if (thrLazyUpdate.getState().equals(Thread.State.TERMINATED)) {
			thrLazyUpdate = new MyLazyUpdateThread();
		}

		if (thrLazyUpdate.getState().equals(Thread.State.NEW)) {
			thrLazyUpdate.start();
		}

	}


	private void clearEditor() {
		this.editor = null;
		this.editorGestureListener = null;
		this.editorGivenOnCancelListener = null;
	}






	private class MyUnderlyingCanvas extends View {
		public MyUnderlyingCanvas(Context context) {
			super(context);
		}


		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			gestureDetector.onTouch(ev);

			return true;
		}


		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			ViewMatrixAnalyzer matViewAnalyzer = new ViewMatrixAnalyzer();

			synchronized (matViewSynObj) {
				matViewAnalyzer.set(matView);
			}

			boolean isOnAnimation = false;

			if (isTouching) {
				isOnAnimation = true;
			}

			synchronized (setObjZOrder) {
				Iterator<PairZOrderId> iter = setObjZOrder.iterator();

				while (iter.hasNext()) {
					PairZOrderId p = iter.next();
					CanvasObjectHolder holder = mapIntObjHolder.get(p.id);

					if (holder == null) {
						continue;
					}

					if (!holder.visible) {
						continue;
					}

					holder.canvasObject.draw(canvas, matViewAnalyzer, paint, isOnAnimation);
				}

			}

			if (editor != null) {
				editor.draw(
					canvas, matViewAnalyzer, viewWidth, viewHeight, paint, isOnAnimation
				);

			}

		}

	}



	private class MyCanvasOnCanvasGestureListener implements OnCanvasGestureListener {
		@Override
		public boolean onClick(float x, float y) {
			if (
				editorGestureListener != null
				&& editorGestureListener.onClick(x, y)
			) {
				return true;
			}

			if (
				!editing
				&& externalGestureListener != null
				&& externalGestureListener.onClick(x, y)
			) {
				return true;
			}

			return true;
		}


		@Override
		public boolean onLongPress(float x, float y) {
			if (
				editorGestureListener != null
				&& editorGestureListener.onLongPress(x, y)
			) {
				return true;
			}

			if (
				!editing
				&& externalGestureListener != null
				&& externalGestureListener.onLongPress(x, y)
			) {
				return true;
			}

			return true;
		}

		@Override
		public boolean onTouchMoveStart(float x, float y) {
			isTouching = true;

			if (
				editorGestureListener != null
				&& editorGestureListener.onTouchMoveStart(x, y)
			) {
				return true;
			}

			if (
				!editing
				&& externalGestureListener != null
				&& externalGestureListener.onTouchMoveStart(x, y)
			) {
				return true;
			}

			return true;
		}


		@Override
		public boolean onTouchMove(float x, float y, float[] translateParams) {
			if (
				editorGestureListener != null
				&& editorGestureListener.onTouchMove(x, y, translateParams)
			) {
				return true;
			}

			if (
				!editing
				&& externalGestureListener != null
				&& externalGestureListener.onTouchMove(x, y, translateParams)
			) {
				return true;
			}

			synchronized (matView) {
				matView.postTranslate(translateParams[0], translateParams[1]);
			}

			invalidateCanvas();

			return true;
		}


		@Override
		public boolean onTouchMoveEnd() {
			isTouching = false;

			if (
				editorGestureListener != null
				&& editorGestureListener.onTouchMoveEnd()
			) {
				return true;
			}

			lazyUpdate();

			if (
				!editing
				&& externalGestureListener != null
				&& externalGestureListener.onTouchMoveEnd()
			) {
				return true;
			}

			return true;
		}


		@Override
		public boolean onMultiTouchMoveStart() {
			isTouching = true;

			if (
				editorGestureListener != null
				&& editorGestureListener.onMultiTouchMoveStart()
			) {
				return true;
			}

			if (
				!editing
				&& externalGestureListener != null
				&& externalGestureListener.onMultiTouchMoveStart()
			) {
				return true;
			}

			return false;
		}


		@Override
		public boolean onMultiTouchMove(
			float[] translateParams, float[] scaleParams, float[] rotateParams
		) {
			if (
				editorGestureListener != null
				&& editorGestureListener.onMultiTouchMove(
					translateParams, scaleParams, rotateParams
				)

			) {
				return true;
			}

			if (
				!editing
				&& externalGestureListener != null
				&& externalGestureListener.onMultiTouchMove(
					translateParams, scaleParams, rotateParams
				)
			) {
				return true;
			}

			synchronized (matViewSynObj) {
				matView.postTranslate(translateParams[0], translateParams[1]);
				matView.postScale(
					scaleParams[0], scaleParams[1],
					scaleParams[2], scaleParams[3]
				);
				matView.postRotate(
					rotateParams[0],
					rotateParams[1], rotateParams[2]
				);

			}

			invalidateCanvas();

			return true;
		}


		@Override
		public boolean onMultiTouchMoveEnd() {
			isTouching = false;

			if (
				editorGestureListener != null
				&& editorGestureListener.onMultiTouchMoveEnd()
			) {
				return true;
			}

			lazyUpdate();

			if (
				!editing
				&& externalGestureListener != null
				&& externalGestureListener.onMultiTouchMoveEnd()
			) {
				return true;
			}

			return true;
		}

	}



	private class MyLazyUpdateThread extends Thread {

		@Override
		public void run() {
			int viewWidthCpy, viewHeightCpy;

			synchronized (viewWidthHeightSynObj) {
				viewWidthCpy = viewWidth;
				viewHeightCpy = viewHeight;
			}

			ViewMatrixAnalyzer matViewAnalyzer = new ViewMatrixAnalyzer();

			synchronized (matViewSynObj) {
				matViewAnalyzer.set(matView);
			}

			synchronized (setLazyUpdateObj) {
				Iterator<Integer> iter = setLazyUpdateObj.iterator();

				while (iter.hasNext()) {
					int id = iter.next();
					CanvasObjectHolder holder = mapIntObjHolder.get(id);

					if (holder == null) {
						continue;
					}

					if (!holder.visible) {
						continue;
					}

					holder.canvasObject.lazyUpdate(
						matViewAnalyzer, viewWidthCpy, viewHeightCpy
					);

				}

			}

			// Request the view to redraw (cross-thread execution)
			postInvalidateCanvas();
		}

	}



	private class MyEditorOnCancelListener
	implements CanvasEditor.OnCancelListener {

		@Override
		public void onCancel(int state, Object data) {
			CanvasEditor.OnCancelListener onCancelListener
				= editorGivenOnCancelListener;

			editor.endByCanvas();
			clearEditor();

			invalidateCanvas();

			editing = false;

			if (onCancelListener != null) {
				onCancelListener.onCancel(state, data);
			}

		}

	}



	private class CanvasObjectHolder {
		public CanvasObject canvasObject;

		public int zOrder;
		public boolean visible;

		public final Object zOrderSyncObj = new Object();



		public CanvasObjectHolder(
			CanvasObject obj, int zOrder, boolean visible
		) {
			this.canvasObject = obj;
			this.zOrder = zOrder;
			this.visible = visible;
		}

	}



	private class PairZOrderId implements Comparable {
		public int zOrder;
		public int id;



		public PairZOrderId(int zOrder, int id) {
			this.zOrder = zOrder;
			this.id = id;
		}


		@Override
		public int compareTo(Object o) {
			PairZOrderId other = (PairZOrderId)o;

			if (zOrder < other.zOrder) {
				return -1;
			}

			if (zOrder > other.zOrder) {
				return 1;
			}

			if (id < other.id) {
				return -1;
			}

			if (id > other.id) {
				return 1;
			}

			return 0;
		}

	}

}
