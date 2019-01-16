package com.dcs.controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.dcs.constant.Constants;
import com.dcs.utility.EVCUtil;
import com.dcs.utility.ProcessSimUtil;

public class DcsMain {
	ArrayList<Integer> send_simulated = new ArrayList<>();
	ArrayList<Integer> receive_simulated = new ArrayList<>();
	ArrayList<Integer> internal_simulated = new ArrayList<>();

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		System.out.println("Enter number of Processes you want to create which should be atleast 2: ");
		Scanner scanner = new Scanner(System.in);
		int noOfProcess = scanner.nextInt();
		boolean isProcessEnough = true;
		while (isProcessEnough) {
			if (noOfProcess < 2) {
				System.out.println("Number of Processes should be atleast 2. Please enter again: ");
				noOfProcess = scanner.nextInt();
			} else {
				isProcessEnough = false;
			}
		}
		System.out.println("Enter number of Events per process which should be atleast 2: ");
		int noOfEvents = scanner.nextInt();
		boolean isEventNoEnough = true;
		while (isEventNoEnough) {
			if (noOfEvents < 2) {
				System.out.println("Number of Events per process should be atleast 2. Please enter again: ");
				noOfEvents = scanner.nextInt();
			} else {
				isEventNoEnough = false;
			}
		}
		//System.out.println("Enter probability for Internal Events between 0 and 1: ");
		/*double probOfIntrnlEvent = scanner.nextDouble();
		boolean probCheck = true;
		while (probCheck) {
			if (probOfIntrnlEvent < 0 || probOfIntrnlEvent > 1) {
				System.out.println("Probability should be between 0 and 1. Please enter again: ");
				probOfIntrnlEvent = scanner.nextDouble();
			} else {
				probCheck = false;
			}
		}*/
		System.out.println("Enter Y for 32 bit or N for 64 bit:");
		String bitSize = scanner.next();
		if (bitSize.equalsIgnoreCase("Y")) {
			EVCUtil.setBitSize(32);
		} else {
			EVCUtil.setBitSize(64);
		}
		// create and set each threads to number of processors
		List<Processor> processList = new ArrayList<Processor>();
		Map<Long, BigInteger> primeNoMap = new HashMap<Long, BigInteger>();
		for (int i = 0; i < noOfProcess; i++) {
			Processor processor = new Processor();
			long pId = processor.getId();
			BigInteger primeNumOfProcess = EVCUtil.generateNextPrime();
			primeNoMap.put(pId, primeNumOfProcess);
			processList.add(processor);
		}
		EVCUtil.setProcessAssocPrimeNoMap(primeNoMap);
		System.out.println("Prime Number Assoc With Each Thread: " + EVCUtil.getProcessAssocPrimeNoMap());
		ProcessSimUtil.setProcessList(processList);

		// insert in priority queue all send and local events and add pQueue to process
		ProcessSimUtil.insertInPQIntAndSendEvnts(noOfProcess, noOfEvents, Constants.PROB_OF_INTRNL_EVENT);

		// create send map and for each send event create receive events in priority
		// queue
		ProcessSimUtil.createReceiveEventAndSendMap();

		for (int i = 0; i < processList.size(); i++) {
			processList.get(i).start();
		}
		// Execute below at the end.
		if (Constants.IS_PART_A_RUNNING) {
			try {
				Thread hook = new Thread() {
					public void run() {
						System.out.println("Hello from Hook");
						System.out.println("Number of Events: " + ProcessSimUtil.getNumOfEvents());
						System.out.println("EVC Map is: " + EVCUtil.getNumOfEventEvcSizeMap());
						TreeMap<Long, Integer> sorteNumOfEventEvcSizeMap = new TreeMap<>();
						sorteNumOfEventEvcSizeMap.putAll(EVCUtil.getNumOfEventEvcSizeMap());
						System.out.println("Sorted Map " + sorteNumOfEventEvcSizeMap);

						SortedSet<Long> keys = new TreeSet<>(EVCUtil.getNumOfEventEvcSizeMap().keySet());
						Long[] numOfEvent = new Long[keys.size()];
						Integer[] evcSize = new Integer[keys.size()];
						int index = 0;
						int maxSize = 0;
						for (Long key : keys) {
							Integer value = EVCUtil.getNumOfEventEvcSizeMap().get(key);
							numOfEvent[index] = key;
							if (value > maxSize) {
								evcSize[index] = value;
								maxSize = value;
							} else {
								evcSize[index] = maxSize;
							}
							index++;
						}
					}
				};
				hook.setDaemon(true);
				Runtime.getRuntime().addShutdownHook(hook);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
