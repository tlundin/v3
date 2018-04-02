package com.teraim.fieldapp.gis;

public interface TrackerListener {

	class GPS_State {

		public static GPS_State GPS_State_C(State s) {
			GPS_State g = new GPS_State();
			g.state=s;
			g.time=System.currentTimeMillis();
			return g;
		}

		public enum State {

			disabled,
			enabled,
			newValueReceived,
			ping
		}

		public float accuracy;
		public double x;
		public double y;
		public State state;
		public long time;
	}



	void gpsStateChanged(GPS_State newState);
}
