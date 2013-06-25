package me.ebernie.mapi.model;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

public class AirPolutionIndex {

	@SerializedName("lokasi")
	private final String area;
	@SerializedName("negeri")
	private final String state;
	@SerializedName("pagi")
	private final String time1;
	@SerializedName("tengah_hari")
	private final String time2;
	@SerializedName("petang")
	private final String time3;

	public AirPolutionIndex(String area, String state, String time1,
			String time2, String time3) {
		super();
		this.area = area;
		this.state = state;
		this.time1 = time1;
		this.time2 = time2;
		this.time3 = time3;
	}

	public String getArea() {
		return area;
	}

	public String getState() {
		return state;
	}

	public String getTime1() {
		if (TextUtils.isEmpty(time1)) {
			return "--";
		}
		return time1;
	}

	public String getTime2() {
		if (TextUtils.isEmpty(time2)) {
			return "--";
		}
		return time2;
	}

	public String getTime3() {
		if (TextUtils.isEmpty(time3)) {
			return "--";
		}
		return time3;
	}

	@Override
	public String toString() {
		return "AirPolutionIndex [area=" + area + ", state=" + state
				+ ", time1=" + time1 + ", time2=" + time2 + ", time3=" + time3
				+ "]";
	}

}
