import java.util.*;
import java.io.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.Executors;
import java.util.concurrent.*;

class ConnectedDevicesManager{

	ReadWriteLock devicesLock;
	HashMap<Integer, Device> disconnectedDevices;
	HashMap<Integer, Device> connectedDevices;
	ScheduledExecutorService oldDeviceCleaner;
	int connectionTimeout;

	public ConnectedDevicesManager(){
		this(10);
	}

	public ConnectedDevicesManager(int mtime){
		devicesLock	= new ReentrantReadWriteLock();
		connectedDevices = new HashMap<Integer, Device>();
		oldDeviceCleaner = Executors.newSingleThreadScheduledExecutor();
		connectionTimeout = mtime;
		oldDeviceCleaner.scheduleAtFixedRate(new CleanerService(), 0, 5, TimeUnit.SECONDS);
	}

	private class CleanerService implements Runnable{
		@Override
		public void run(){
			ArrayList<Integer> flaggedForRemoval = new ArrayList<Integer>();
			devicesLock.writeLock().lock();
			for (int deviceKey : connectedDevices.keySet()) {
				if (connectedDevices.get(deviceKey).secondsSinceCreated() > connectionTimeout) {
					flaggedForRemoval.add(deviceKey);
				}
			}
			for (int key : flaggedForRemoval) {
				connectedDevices.remove(key);
			}
			devicesLock.writeLock().unlock();

		}
	}



	public void addDevice(Device deviceToAdd){
		devicesLock.writeLock().lock();
		connectedDevices.put(deviceToAdd.getDeviceID(), deviceToAdd);
		devicesLock.writeLock().unlock();
	}

	public void addDevice(int id, double latitude, double longitude){
		addDevice(new Device(id, latitude, longitude));
	}

	public ArrayList<Device> getDevices(ArrayList<Integer> ids){
		ArrayList<Device> retval = new ArrayList<Device>();
		devicesLock.readLock().lock();
		for (int deviceKey : ids) {
			retval.add(connectedDevices.get(deviceKey).clone()); //return clones of objects so that other threads can't modify them after they've been acquired
		}
		devicesLock.readLock().unlock();
		return retval;
	}

	public Device getDevice(int id){
		Device retval;
		devicesLock.readLock().lock();
		retval = connectedDevices.get(id);
		if (retval != null) {
			retval = retval.clone();
		}
		devicesLock.readLock().unlock();
		return retval;
	}

	public ArrayList<Device> getAllConnectedDevices(){
		ArrayList<Device> retval = new ArrayList<Device>();
		devicesLock.readLock().lock();
		for (Device dev : connectedDevices.values()) {
			retval.add(dev.clone());
		}
		devicesLock.readLock().unlock();
		return retval;
	}
}