package tnl.objcanvas;

import android.graphics.Matrix;
import android.graphics.PointF;

import java.util.Iterator;
import java.util.List;

/**
 * Created by LinhTa on 1/7/2018.
 */

public class ViewMatrixAnalyzer {
	private Matrix mat = new Matrix();

	private float scale = 1;
	private float rotation = 0;     // Angle, in range (-180, 180]

	private PointF origin = new PointF(0, 0);
	private PointF vecUnitX = new PointF(1, 0);
	private PointF vecUnitY = new PointF(0, 1);




	public void set(Matrix mat) {
		this.mat.set(mat);
		analyze();
	}

	public Matrix get() {
		return mat;
	}


	public float getTranslateX() {
		return origin.x;
	}


	public float getTranslateY() {
		return origin.y;
	}


	public float getScale() {
		return scale;
	}


	public float getRotation() {
		return rotation;
	}


	public PointF mapOriginalToViewPoint(float x, float y, PointF res) {
		float[] tmp = new float[] {x, y};

		mat.mapPoints(tmp);

		if (res == null) {
			res = new PointF();
		}

		res.set(tmp[0], tmp[1]);

		return res;
	}

	public PointF mapOriginalToViewPoint(PointF src, PointF dst) {
		return mapOriginalToViewPoint(src.x, src.y, dst);
	}


	public float[] mapOriginalToViewPoint(List<PointF> vertices) {
		if (vertices == null || (vertices != null && vertices.size() == 0)) {
			return new float[0];
		}

		float[] res = new float[vertices.size() * 2];

		Iterator<PointF> iter = vertices.iterator();
		int cnt = 0;

		while (iter.hasNext()) {
			PointF p = iter.next();
			res[cnt++] = p.x;
			res[cnt++] = p.y;
		}

		mat.mapPoints(res);

		return res;
	}


	public void mapOriginalToViewPoint(float[] pts) {
		mat.mapPoints(pts);
	}


	public PointF mapViewToOriginalPoint(float x, float y, PointF res) {
		x -= origin.x;
		y -= origin.y;

		if (res == null) {
			res = new PointF();
		}

		res.x = (x * vecUnitX.x + y * vecUnitX.y) / (scale * scale);
		res.y = (x * vecUnitY.x + y * vecUnitY.y) / (scale * scale);

		return res;
	}


	private void analyze() {
		float[] tmp = new float[] {0, 0};

		mat.mapPoints(tmp);
		origin.set(tmp[0], tmp[1]);

		tmp[0] = 1;
		tmp[1] = 0;

		mat.mapVectors(tmp);
		vecUnitX.set(tmp[0], tmp[1]);

		tmp[0] = 0;
		tmp[1] = 1;

		mat.mapVectors(tmp);
		vecUnitY.set(tmp[0], tmp[1]);

		scale = vecUnitX.length();

		rotation = (float)(Math.atan2(vecUnitX.y, vecUnitX.x) / Math.PI * 180);

		while (rotation <= -180) {
			rotation += 360;
		}

		while (rotation > 180) {
			rotation -= 360;
		}

	}

}
