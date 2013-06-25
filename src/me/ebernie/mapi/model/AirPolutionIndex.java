package me.ebernie.mapi.model;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

public class AirPolutionIndex implements Comparable<AirPolutionIndex>{

	@SerializedName("lokasi")
	private final String area;
	@SerializedName("negeri")
	private final String state;
	@SerializedName("pagi")
	private final String sevenAmIndex;
	@SerializedName("tengah_hari")
	private final String elevenAmIndex;
	@SerializedName("petang")
	private final String fivePmIndex;

	public AirPolutionIndex(String area, String state, String sevenAmIndex,
			String elevenAmIndex, String fivePmIndex) {
		super();
		this.area = area;
		this.state = state;
		this.sevenAmIndex = sevenAmIndex;
		this.elevenAmIndex = elevenAmIndex;
		this.fivePmIndex = fivePmIndex;
	}

	public String getArea() {
		return area;
	}

	public String getState() {
		return state;
	}

	public String getSevenAmIndex() {
		if (TextUtils.isEmpty(sevenAmIndex)) {
			return "--";
		}
		return sevenAmIndex;
	}

	public String getElevenAmIndex() {
		if (TextUtils.isEmpty(elevenAmIndex)) {
			return "--";
		}
		return elevenAmIndex;
	}

	public String getFivePmIndex() {
		if (TextUtils.isEmpty(fivePmIndex)) {
			return "--";
		}
		return fivePmIndex;
	}

	@Override
	public String toString() {
		return "AirPolutionIndex [area=" + area + ", state=" + state
				+ ", time1=" + sevenAmIndex + ", time2=" + elevenAmIndex + ", time3=" + fivePmIndex
				+ "]";
	}

	@Override
	public int compareTo(AirPolutionIndex another) {
		return this.area.compareTo(another.area);
	}

}
