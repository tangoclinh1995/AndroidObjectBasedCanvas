package tnl.objcanvas;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by tango on 2/27/2018.
 */

public class PolyEditorData implements Parcelable {
	public static final boolean DEFAULT_ISPOLYGON = true;
	public static final boolean DEFAULT_MUSTBE_CONVEX = false;

	public static final int VERTEX_COUNT_NOLIMIT = -1;



	public boolean isPolygon = DEFAULT_ISPOLYGON;

	// This parameter only matters if isPolygon == true
	public boolean mustbeConvex = DEFAULT_MUSTBE_CONVEX;

	// An negative number means no limit at all
	public int minVertexCount = VERTEX_COUNT_NOLIMIT;
	public int maxVertexCount = VERTEX_COUNT_NOLIMIT;

	public ArrayList<PointF> vertices;



	public PolyEditorData() {

	}


	public static final Parcelable.Creator<PolyEditorData> CREATOR =
		new Parcelable.Creator<PolyEditorData>() {
			@Override
			public PolyEditorData createFromParcel(Parcel in) {
				return new PolyEditorData(in);
			}

			@Override
			public PolyEditorData[] newArray(int size) {
				return new PolyEditorData[size];
			}

		};


	@Override
	public int describeContents() {
		return 0;
	}


	@Override
	public void writeToParcel(Parcel out, int i) {
		out.writeInt(isPolygon ? 1 : 0);
		out.writeInt(mustbeConvex ? 1 : 0);
		out.writeInt(minVertexCount);
		out.writeInt(maxVertexCount);
		out.writeList(vertices);
	}


	private PolyEditorData(Parcel in) {
		isPolygon = (in.readInt() == 1 ? true : false);
		mustbeConvex = (in.readInt() == 1 ? true : false);
		minVertexCount = in.readInt();
		maxVertexCount = in.readInt();
		vertices = in.readArrayList(PointF.class.getClassLoader());
	}

}
