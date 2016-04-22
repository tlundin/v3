package com.teraim.fieldapp.synchronization;
public class SyncEntry extends SyncMessage {

	enum Type {
		insert,
		delete,
		deleteMany,
		unknown, insertArray
	}
	private static final long serialVersionUID = 862826293136691824L;
	private Type mType;
	private String keys;
	private String values;
	private String changes;
	private String timeStamp;
	private String target;
	private boolean invalid = false;
	public SyncEntry() {};
	public SyncEntry(String type,String changes,String timeStamp,String target) {
		this.changes=changes;
		if (type.equals("I"))
			mType = Type.insert;
		else if (type.equals("D"))
			mType = Type.delete;
		else if (type.equals("M"))
			mType = Type.deleteMany;
		else if (type.equals("A"))
			mType = Type.insertArray;
		else {
			System.err.println("Unknown type of Sync action!: "+type);
			mType = Type.unknown;
		}
		if(!isDeleteMany()&&!isDelete()) {	
			String[] tmp = changes.split("_\\$_");
			if (tmp==null||tmp.length!=2) 
				System.err.println("something wrong with syncentry with changes: ["+changes+"]");
			else {
				keys = tmp[0];
				values = tmp[1];
			}
		}
		this.timeStamp=timeStamp;
		this.target=target;
	}

	public Type getAction() {
		return mType;
	}

	public String getKeys() {
		return keys;
	}
	
	public String getChange() {
		return changes;
	}
	
	public String getValues() {
		return values;
	}
	
	public boolean isInsert() {
		return (mType==Type.insert);
	}
	
	public boolean isInsertArray() {
		return (mType==Type.insertArray);
	}
	
	public boolean isDelete() {
		return (mType==Type.delete);
	}
	
	public boolean isDeleteMany() {
		return (mType==Type.deleteMany);
	}
	
	public String getTimeStamp() {
		return timeStamp;
	}

	public String getTarget() {
		return target;
	}

	public void markAsInvalid() {
		invalid=true;
	}

	public boolean isInvalid() {
		return invalid;
	}


}
