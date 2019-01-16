package com.dcs.constant;

import org.apfloat.Apfloat;

public class Constants {
	public static final String RECEIVE_EVENT = "Receive Event";
	
	public static final String SEND_EVENT = "Send Event";
	
	public static final String INTERNAL_EVENT = "Internal Event";
	
	public static final Apfloat BASE_OF_LOG = new Apfloat(25);
	
	public static final Double BASE_OF_LOG_DOUBLE = BASE_OF_LOG.doubleValue();
	
	public static final Long PRECISION_OF_APFLOAT = new Long(20);
	
	public static final boolean IS_PART_A_RUNNING = true;
	
	public static final Double PROB_OF_INTRNL_EVENT = new Double(0.4); 
}
