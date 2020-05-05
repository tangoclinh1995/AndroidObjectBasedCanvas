package tnl.objcanvas;

/**
 * Created by Linh Ta on 3/8/2018.
 */
public interface OnCanvasGestureListener {
	// NOTES: For the following methods:
	//      Return TRUE means event is absorbed, FALSE otherwise
	boolean onClick(float x, float y);

	boolean onLongPress(float x, float y);

	boolean onTouchMoveStart(float x, float y);

	boolean onTouchMove(float x, float y, float[] translateParams);

	boolean onTouchMoveEnd();

	boolean onMultiTouchMoveStart();

	boolean onMultiTouchMove(
		float[] translateParams, float[] scaleParams, float[] rotateParams
	);

	boolean onMultiTouchMoveEnd();
}
