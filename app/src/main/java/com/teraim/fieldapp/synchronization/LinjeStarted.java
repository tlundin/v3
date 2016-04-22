package com.teraim.fieldapp.synchronization;


public class LinjeStarted extends SyncMessage {

	private static final long serialVersionUID = 261623700299822759L;
	public String linjeId;
	public LinjeStarted(String linjeId) {
		this.linjeId=linjeId;
	}
}
