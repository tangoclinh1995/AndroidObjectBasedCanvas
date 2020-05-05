package tnl.objcanvas;

import android.graphics.PointF;
import android.os.Handler;
import android.view.MotionEvent;



/**
 * Created by tango on 1/1/2018.
 */

public class CanvasGestureDetector {
	public static final String TAG = "CanvasGestureDetector";



	public static final float DISTANCE_NOCHANGE_THRESHOLD = 4f;
	public static final int LONGPRESS_DURATION_MS = 500;
	public static final int CLICK_MS = 200;



	private OnCanvasGestureListener listener = null;

	private PointF[] lastPointers = new PointF[] {
		new PointF(Float.NaN, Float.NaN),
		new PointF(Float.NaN, Float.NaN)
	};

	private int lastPointerCount = 0;

	private PointF[] curPointers = new PointF[] {
		new PointF(Float.NaN, Float.NaN),
		new PointF(Float.NaN, Float.NaN)
	};

	private int curPointerCount = 0;
	private long curPointerTimestamp = 0;

	private boolean touchJustStart = false;
	private long touchStartTimestamp = 0;
	private boolean isMoving = false;

	private boolean longPressEnabled = true;
	private Handler longPressHandler;



	public CanvasGestureDetector() {
		longPressHandler = new Handler();
	}


	public void setGestureListener(OnCanvasGestureListener listener) {
		this.listener = listener;
	}


	public boolean getLongPressEnabled() {
		return longPressEnabled;
	}


	public void setLongPressEnabled(boolean enabled) {
		longPressEnabled = enabled;
	}


	public void onTouch(MotionEvent ev) {
		// Do nothing if no listener set
		if (listener == null) {
			return;
		}

		// Load new pointer info
		makeNonePointer(curPointers[0]);
		makeNonePointer(curPointers[1]);

		curPointerCount = 0;
		curPointerTimestamp = ev.getEventTime();

		int action = ev.getActionMasked();
		int evPointerCount = ev.getPointerCount();
		int actionsIds = ev.getActionIndex();

		for (int i = 0; i < evPointerCount && curPointerCount < 2; ++i) {
			if (
				(
					action == MotionEvent.ACTION_UP
					|| action == MotionEvent.ACTION_POINTER_UP
				)
				&& i == actionsIds
			) {
				continue;
			}

			++curPointerCount;

			curPointers[curPointerCount - 1].set(ev.getX(i), ev.getY(i));
		}

		// Check changes
		boolean pointerChanged = false;

		if (curPointerCount != lastPointerCount) {
			pointerChanged = true;
		} else {
			for (int i = 0; i < curPointerCount; ++i) {
				if (
					distance(curPointers[i], lastPointers[i])
						> DISTANCE_NOCHANGE_THRESHOLD
				) {
					pointerChanged = true;
					break;
				}

			}

		}

		// Do nothing if there is no change
		if (!pointerChanged) {
			return;
		}

		if (lastPointerCount == 0) {
			touchJustStart = true;
			touchStartTimestamp = curPointerTimestamp;

			if (curPointerCount == 1 && longPressEnabled) {
				longPressHandler.postDelayed(
					new LongPressRunnable(), LONGPRESS_DURATION_MS
				);

			}

		} else {
			touchJustStart = false;

			if (
				curPointerCount == 0
				&& curPointerTimestamp - touchStartTimestamp <= CLICK_MS
			) {
				listener.onClick(lastPointers[0].x, lastPointers[0].y);

			} else if (curPointerCount == 1 && lastPointerCount == 1) {
				if (!isMoving) {
					// TouchMove just start
					listener.onTouchMoveStart(lastPointers[0].x, lastPointers[0].y);
				}

				isMoving = true;

				float[] translateParams = new float[] {
					curPointers[0].x - lastPointers[0].x,
					curPointers[0].y - lastPointers[0].y
				};

				listener.onTouchMove(curPointers[0].x, curPointers[0].y, translateParams);

			} else if (curPointerCount == 2 && lastPointerCount == 2) {
				if (!isMoving) {
					// MultiTouchMove just start
					listener.onMultiTouchMoveStart();
				}

				isMoving = true;

				PointF lastMid = new PointF(
					(lastPointers[0].x + lastPointers[1].x) / 2,
					(lastPointers[0].y + lastPointers[1].y) / 2
				);

				PointF curMid = new PointF(
					(curPointers[0].x + curPointers[1].x) / 2,
					(curPointers[0].y + curPointers[1].y) / 2
				);

				float[] translateParams = new float[] {
					curMid.x - lastMid.x,
					curMid.y - lastMid.y
				};

				float scale =
					distance(curPointers[0], curPointers[1])
					/ distance(lastPointers[0], lastPointers[1]);

				float[] scaleParams = new float[] {scale, scale,lastMid.x, lastMid.y};

				double angleRad =
					Math.atan2(curMid.y - curPointers[0].y, curMid.x - curPointers[0].x)
					- Math.atan2(lastMid.y - lastPointers[0].y, lastMid.x - lastPointers[0].x);

				float[] rotateParams = new float[] {
					(float)(angleRad / Math.PI * 180),
					lastMid.x, lastMid.y
				};

				listener.onMultiTouchMove(translateParams, scaleParams, rotateParams);
			}

		}

		if (isMoving && lastPointerCount > curPointerCount) {
			isMoving = false;

			if (lastPointerCount == 2) {
				listener.onMultiTouchMoveEnd();
			} else if (lastPointerCount == 1) {
				listener.onTouchMoveEnd();
			}

		}

		// Replace lastPointers-related params with curPointers-related params
		lastPointerCount = curPointerCount;

		for (int i = 0; i < curPointerCount; ++i) {
			lastPointers[i].set(curPointers[i].x, curPointers[i].y);
		}

	}


	private void makeNonePointer(PointF p) {
		p.set(Float.NaN, Float.NaN);
	}


	private float distance(PointF pa, PointF pb) {
		return (float)Math.sqrt(Math.pow(pa.x - pb.x, 2) + Math.pow(pa.y - pb.y, 2));
	}






	private class LongPressRunnable implements Runnable {
		public void run() {
			if (!longPressEnabled) {
				return;
			}

			if (!touchJustStart) {
				return;
			}

			if (listener != null) {
				listener.onLongPress(lastPointers[0].x, lastPointers[0].y);
			}

		}

	}
}
