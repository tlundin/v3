package com.teraim.fieldapp.synchronization;

public interface ConnectionListener {

	public enum ConnectionEvent {
		connectionGained,
		connectionBroken,
		connectionFailed,
		connectionClosedGracefully,
		connectionStatus, 
		connectionAttemptFailed, 
		restartRequired, 
		connectionFailedNoPartner, 
		connectionFailedNamedPartnerMissing
	}
	
	public void handleMessage(Object o);
	public void handleEvent(ConnectionEvent e);
	
	
}
