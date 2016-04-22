package com.teraim.fieldapp.synchronization;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.teraim.fieldapp.synchronization.ConnectionListener.ConnectionEvent;

/**
 * 
 * @author Terje
 *
 * A connection provider accepts listeners to a communication interface and offers the possibility to write data
 * and to open and close connections. This base class handles common functions, but a technology specific provider must override
 *
 */




public abstract class ConnectionProvider {

	private final List<ConnectionListener> listeners = new ArrayList<ConnectionListener>();
	public abstract void write(Object o);

	public void registerConnectionListener(ConnectionListener listener) {
		listeners.add(listener);
	}
	
	public void unRegisterConnectionListener(ConnectionListener listener) {
		if(listeners.remove(listener))
			Log.d("vortex","removed listener in unregister!");
		else
			Log.e("vortex","failed to remove listener in unregister!");
	}

	protected void broadcastEvent(ConnectionEvent ce) {
		for (ConnectionListener listener:listeners)
			listener.handleEvent(ce);
	}
	
	protected void broadcastData(Object message) {
		for (ConnectionListener listener:listeners)
			listener.handleMessage(message);
	}
	
	public abstract void openConnection(String partner);
	public abstract void closeConnection();
	public abstract void abortConnection();

	public abstract int getTriesRemaining();

	public abstract boolean isOpen();
	
}
