package tnl.objcanvas;

import android.graphics.PointF;
import android.graphics.RectF;

import java.util.Iterator;
import java.util.List;

/**
 * Created by Linh Ta on 2/27/2018.
 */

public class GeometryUtil {
	public static final float EPS = 1e-6f;


	/**
	 *
	 * @param la Line A, described as a vector
	 * @param lb Line B, described as a vector
	 * @return Acute angle (0 <= x <= 90) between line A and B
	 */
	public static float angleInDegreeBetweenLines(PointF la, PointF lb) {
		double res =
			Math.acos((la.x * lb.x + la.y * lb.y) / (la.length() * lb.length()))
				/ Math.PI * 180;

		if (res > 90 + EPS) {
			res = 180 - res;
		}

		return (float)res;
	}


	/**
	 *
	 * @param fromX
	 * @param fromY
	 * @param toX
	 * @param toY
	 * @return 	Signed magnitude of result vector
	 * 			Negative means result vector is in opposite direction with
	 *			projector vector (toX, toY)
	 * 			Positive means otherwise
	 */
	public static float projectionVectorToVector(
		float fromX, float fromY, float toX, float toY
	) {
		return (fromX * toX + fromY * toY)
			/ (float)Math.sqrt(toX * toX + toY * toY);
	}


	public static float distancePointToPoint(
		float x1, float y1, float x2, float y2
	) {
		return (float)Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
	}


	public static float distancePointToLine(
		float x, float y,
		float ax, float ay, float bx, float by
	) {
		float vecAPx = x - ax;
		float vecAPy = y - ay;

		float proj = projectionVectorToVector(
			vecAPx, vecAPy,
			bx - ax, by - ay
		);

		return (float)Math.sqrt(vecAPx * vecAPx + vecAPy * vecAPy - proj * proj);
	}


	public static PointF makeVector(PointF start, PointF end) {
		return new PointF(end.x - start.x, end.y - start.y);
	}


	/**
	 *
	 * @param points
	 * @return  RectF(NaN, NaN, NaN, NaN) if points is null or have 0 length,
	 *          Specific RectF otherwise
	 */
	public static RectF getBound(List<PointF> points) {
		if (points == null) {
			return new RectF(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
		}

		if (points.size() == 0) {
			return new RectF(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
		}

		RectF res = new RectF(
			Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,
			Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY
		);

		for (PointF p : points) {
			res.left = Math.min(res.left, p.x);
			res.top = Math.min(res.top, p.y);

			res.right = Math.max(res.right, p.x);
			res.bottom = Math.max(res.bottom, p.y);
		}

		return res;
	}


	public static boolean pointInsidePolygon(
		float x, float y, List<PointF> polygon
	) {
		if (polygon == null) {
			return false;
		}

		if (polygon.size() == 0) {
			return false;
		}

		if (polygon.size() == 1) {
			PointF p = polygon.get(0);

			return Math.abs(x - p.x) < EPS && Math.abs(y - p.y) < EPS;
		}

		if (polygon.size() == 2) {
			PointF p0 = polygon.get(0);
			PointF p1 = polygon.get(1);

			return pointInSegment(x, y, p0.x, p0.y, p1.x, p1.y);
		}

		Iterator<PointF> iter;
		PointF curPoint, nextPoint;
		int windingNumber = 0;

		iter = polygon.iterator();
		curPoint = iter.next();

		while (true) {
			boolean cancellable = false;

			if (iter.hasNext()) {
				nextPoint = iter.next();
			} else {
				nextPoint = polygon.get(0);
				cancellable = true;
			}

			if (curPoint.y <= y) {
				if (
					nextPoint.y > y
					&&
					pointSideToLine(
						curPoint.x, curPoint.y, nextPoint.x, nextPoint.y, x, y
					) > 0
				) {
					++windingNumber;
				}

			} else {
				if (
					nextPoint.y <= y
					&&
					pointSideToLine(
						curPoint.x, curPoint.y, nextPoint.x, nextPoint.y, x, y
					) < 0
				) {
					--windingNumber;
				}

			}

			if (cancellable) {
				break;
			}

			curPoint = nextPoint;
		}

		return windingNumber != 0;
	}


	public static boolean pointInSegment(
		float x, float y,
		float ax, float ay, float bx, float by
	) {
		if (pointSideToLine(ax, ay, bx, by, x, y) != 0) {
			return false;
		}

		float d = (bx - ax) * (x - ax) + (by - ay) * (y - ay);

		if (d <= -EPS) {
			return false;
		}

		if (d + EPS >= (bx - ax) * (bx - ax) + (by - ay) * (by - ay)) {
			return false;
		}

		return true;
	}


	public static void addListToListPointF(
		List<PointF> source, List<PointF> destination
	) {
		for (PointF p : source) {
			destination.add(new PointF(p.x, p.y));
		}

	}


	public static boolean isConvexPolygon(List<PointF> polygon) {
		if (polygon == null) {
			return false;
		}

		if (polygon.size() < 3) {
			return false;
		}

		if (polygon.size() == 3) {
			return true;
		}

		Iterator<PointF> iter = polygon.iterator();
		PointF pA = iter.next();
		PointF pB = iter.next();
		PointF pC;

		int keptTurn = 0;
		int newTurn;

		while (true) {
			boolean cancellable = false;

			if (iter.hasNext()) {
				pC = iter.next();
			} else {
				cancellable = true;
				pC = polygon.get(0);
			}

			newTurn = pointSideToLine(pA.x, pA.y, pB.x, pB.y, pC.x, pC.y);

			if (newTurn != 0) {
				if (keptTurn == 0) {
					keptTurn = newTurn;
				} else if (newTurn != keptTurn) {
					return false;
				}

			}

			pA = pB;
			pB = pC;

			if (cancellable) {
				break;
			}

		}

		pC = polygon.get(1);

		newTurn = pointSideToLine(pA.x, pA.y, pB.x, pB.y, pC.x, pC.y);

		if (newTurn != 0) {
			if (keptTurn == 0) {
				keptTurn = newTurn;
			} else if (newTurn != keptTurn) {
				return false;
			}

		}

		return keptTurn != 0;
	}


	/**
	 * Check which side of the vector passing through (lx0, ly0) and (ly0, ly1)
	 * that point (x, y) is located at
	 * @param lx0,ly0
	 * @param lx1,ly1
	 * @param x
	 * @param y
	 * @return 	-1 if point (x, y) is on the RIGHT side
	 * 			0 if point (x, y) lies on the line
	 * 			1 if point (x, y) is on the LEFT side
	 */
	private static int pointSideToLine(
		float lx0, float ly0, float lx1, float ly1,
		float x, float y
	) {
		float v = (lx1 - lx0) * (y - ly0) - (x - lx0) * (ly1 - ly0);

		if (v <= -EPS) {
			return -1;
		}

		if (v >= EPS) {
			return 1;
		}

		return 0;
	}

}
