package com.midlandroid.apps.android.laptimer.background.timers;


public abstract class TimerMode {
	public  static enum RunningState {
		RUNNING, STOPPED, RESETTED, TIMER_DELAY
	}
	
	public abstract void procTimerUpdate(long updateTime);
	public abstract void procLapEvent();
	public abstract void procResetTimer();
	public abstract void procRefreshUI();
	
	public abstract void setUpdateServiceListener(TimerUpdateServiceListener updateServiceListener);
	public abstract TimerUpdateServiceListener getUpdateServiceListener();
	
	public abstract void setData(TimerModeData modeData);
	public abstract TimerModeData getData();

	public abstract long getCurTime();

	protected abstract String getTimerModeName();
	
	private String timerName;
	/**
	 * Returns the name of the timer or if none is provided
	 * the name of the timer mode class.
	 * @return
	 */
	public String getTimerName() {
		if (timerName == null)
			return getTimerModeName();
		
		return timerName;
	}
	
	/**
	 * Sets the name of the timer
	 * @param name
	 */
	public void setTimerName(String name) {
		timerName = name;
	}	
}