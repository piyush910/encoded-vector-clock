package com.dcs.model;

public class Event {
	private Integer eventId;
	private Internal internal = new Internal();
	private Send send = new Send();
	private Receive receive = new Receive();
	
	public Integer getEventId() {
		return eventId;
	}
	public void setEventId(Integer eventId) {
		this.eventId = eventId;
	}
	public Internal getInternal() {
		return internal;
	}
	public void setInternal(Internal internal) {
		this.internal = internal;
	}
	public Send getSend() {
		return send;
	}
	public void setSend(Send send) {
		this.send = send;
	}
	public Receive getReceive() {
		return receive;
	}
	public void setReceive(Receive receive) {
		this.receive = receive;
	}
	
}

