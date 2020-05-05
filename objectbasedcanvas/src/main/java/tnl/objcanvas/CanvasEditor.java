package tnl.objcanvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Linh Ta on 1/8/2018.
 */

public abstract class CanvasEditor {
	public static final int ALREADY_STARTED = 0;
	public static final int INVALID_INIT_DATA = 1;
	public static final int STARTED_SUCCESS = 2;

	// User requests to save the data
	public static final int SAVE_REQUESTED = 3;
	// User requests NOT to save the data
	public static final int NOTSAVE_REQUESTED = 4;
	// User cancel the editor immediately, and the unfinished data can't be used
	// to form any meaningful result
	public static final int CANCEL_IMMEDIATE_REQUEST_UNUSEABLE = 5;
	// User cancel the editor immediately, and but unfinished data can't be used
	// to form meaningful result
	public static final int CANCEL_IMMEDIATE_REQUEST_USEABLE = 6;




	protected OnCanvasGestureListener gestureListener = null;
	protected OnCancelListener cancelListener = null;

	protected ObjectBasedCanvas objectBasedCanvas = null;



	public abstract void draw(
		Canvas canvas,
		ViewMatrixAnalyzer matViewAnalyzer,
		int viewWidth, int viewHeight,
		Paint sharedPaint,
		boolean isOnAnimation
	);


	public abstract Parcelable getDataInProgress();


	public abstract void cancel();


	public abstract void cancelImmediately();


	public OnCanvasGestureListener getGestureListener() {
		return gestureListener;
	}

	/**
	 *
	 * @param objectBasedCanvas
	 * @param initData
	 * @param canceListener
	 *
	 * This method needs to be called by ObjectBasedCanvas to initiate
	 * editing process inside that canvas
	 *
	 * Subclass that override this method must call super.startByCanvas()
	 * at the beginning and check for the status
	 */

	public int startByCanvas(
			ObjectBasedCanvas objectBasedCanvas,
			Object initData,
			OnCancelListener canceListener
	) {
		if (this.objectBasedCanvas != null) {
			return ALREADY_STARTED;
		}

		this.objectBasedCanvas = objectBasedCanvas;
		this.cancelListener = canceListener;

		return STARTED_SUCCESS;
	}


	/**
	 * This method needs to be called by ObjectBasedCanvas to clean up
	 * editing process after it finishes
	 *
	 * Subclass that override this method must call super.endByCanvas()
	 * before this method ends
	 */
	public void endByCanvas() {
		this.objectBasedCanvas = null;
		this.cancelListener = null;
	}






	public interface OnCancelListener {
		void onCancel(int state, Object data);
	}

}
