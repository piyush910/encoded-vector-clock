package com.dcs.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

import com.dcs.controller.Processor;
import com.dcs.model.Event;
import com.dcs.model.Receive;

public class ProcessSimUtil {

	private static List<Processor> processList = new ArrayList<>();
	private static volatile Map<Long, LinkedList<Integer>> remainingEventIdMap = new ConcurrentHashMap<>();
	private static volatile Map<Long, Map<Long, Integer>> processSendCountMap = new ConcurrentHashMap<Long, Map<Long, Integer>>();
	private static volatile Map<Long, Integer> maximumEventNumber = new ConcurrentHashMap<>();
	private static Semaphore semaphore = new Semaphore(1);
	private static volatile long numOfEvents = 0;
	private static volatile boolean isProcessSuspended = false;

	public static long generateRandomProcessId(long selfProcessId) {
		List<Processor> processList = ProcessSimUtil.getProcessList();
		List<Long> processIdList = new ArrayList<>();
		long processId;
		for (int i = 0; i < processList.size(); i++) {
			processIdList.add(processList.get(i).getId());
		}
		Collections.shuffle(processIdList);
		processId = processIdList.get(0);
		if (processId == selfProcessId) {
			processId = processIdList.get(1);
		}
		return processId;
	}

	public static PriorityBlockingQueue<Event> insertInPQueue(int eventSize, long currentProcessId,
			double probOfIntrnlEvent, PriorityBlockingQueue<Event> pQueue, int noOfProcess) {
		LinkedList<Integer> eventIdList = new LinkedList<Integer>();
		for (int k = 1; k <= eventSize * noOfProcess; k++) {
			eventIdList.add(new Integer(k));
		}
		maximumEventNumber.put(currentProcessId, eventSize * eventSize);
		Collections.shuffle(eventIdList);
		for (int i = 1; i <= eventSize; i++) {
			Event evnt = new Event();
			if (ProcessSimUtil.isLocalEvent(probOfIntrnlEvent)) {
				evnt.getInternal().setInternal(true);
			} else {
				evnt.getSend().setSend(true);
				evnt.getSend().setDestinationId(ProcessSimUtil.generateRandomProcessId(currentProcessId));
			}
			int eventId = eventIdList.size() == eventSize * noOfProcess ? 0 : eventIdList.peek();
			eventIdList.poll();
			evnt.setEventId(eventId);
			pQueue.add(evnt);
		}
		remainingEventIdMap.put(currentProcessId, eventIdList);
		return pQueue;
	}

	public static void setSendMap(Event e, Long currentProcessId) {
		try {
			semaphore.acquire();
		} catch (InterruptedException e4) {
			e4.printStackTrace();
		}
		try {
			ProcessSimUtil utils = new ProcessSimUtil();
			utils.setSendMapImplement(e, currentProcessId);
		} finally {
			semaphore.release();
		}
	}

	private void setSendMapImplement(Event e, Long currentProcessId) {
		synchronized (this) {
			Map<Long, Map<Long, Integer>> sendMap = ProcessSimUtil.getProcessSendCountMap();
			if (sendMap.containsKey(currentProcessId)) {
				Map<Long, Integer> destMap = sendMap.get(currentProcessId);
				if (destMap.containsKey(e.getSend().getDestinationId())) {
					destMap.put(e.getSend().getDestinationId(), destMap.get(e.getSend().getDestinationId()) + 1);
				} else {
					destMap.put(e.getSend().getDestinationId(), 1);
				}
				sendMap.put(currentProcessId, destMap);
				setProcessSendCountMap(sendMap);
			} else {
				Map<Long, Integer> destMap = new HashMap<>();
				destMap.put(e.getSend().getDestinationId(), 1);
				sendMap.put(currentProcessId, destMap);
				setProcessSendCountMap(sendMap);
			}
		}
	}

	public static void decrementAndSetSendMap(Event e, Long currentProcessId) {
		try {
			semaphore.acquire();
		} catch (InterruptedException e4) {
			// TODO Auto-generated catch block
			e4.printStackTrace();
		}
		try {
			ProcessSimUtil utils = new ProcessSimUtil();
			utils.decrementAndSetSendMapImpl(e, currentProcessId);
		} finally {
			semaphore.release();
		}
	}

	private void decrementAndSetSendMapImpl(Event e, Long currentProcessId) {
		synchronized (this) {
			Map<Long, Map<Long, Integer>> sendMap = ProcessSimUtil.getProcessSendCountMap();
			if (sendMap.containsKey(e.getReceive().getSourceProcessId())) {
				Map<Long, Integer> sourceMap = sendMap.get(e.getReceive().getSourceProcessId());
				sourceMap.put(currentProcessId, sourceMap.get(currentProcessId) - 1);
				sendMap.put(e.getReceive().getSourceProcessId(), sourceMap);
				ProcessSimUtil.setProcessSendCountMap(sendMap);
			}
		}
	}

	public static PriorityBlockingQueue<Event> genNewProcessIdQ(Long currentProcessId, Event e,
			PriorityBlockingQueue<Event> pQueue) {
		try {
			semaphore.acquire();
		} catch (InterruptedException e4) {
			e4.printStackTrace();
		}
		try {
			ProcessSimUtil utils = new ProcessSimUtil();
			pQueue = utils.genNewProcessIdQImpl(currentProcessId, e, pQueue);
		} finally {
			semaphore.release();
		}
		return pQueue;
	}

	private PriorityBlockingQueue<Event> genNewProcessIdQImpl(Long currentProcessId, Event e,
			PriorityBlockingQueue<Event> pQueue) {
		synchronized (this) {
			Map<Long, Integer> maxEventNoCurrentProcess = ProcessSimUtil.getMaximumEventNumber();
			int newEventIdForCurrEvnt = maxEventNoCurrentProcess.get(currentProcessId);
			newEventIdForCurrEvnt = newEventIdForCurrEvnt + 1;
			e.setEventId(newEventIdForCurrEvnt);
			maxEventNoCurrentProcess.put(currentProcessId, newEventIdForCurrEvnt);
			setMaximumEventNumber(maxEventNoCurrentProcess);
			pQueue.poll();
			pQueue.add(e);
			return pQueue;
		}
	}

	public static synchronized boolean isSenderPresentInMap(PriorityBlockingQueue<Event> pQueue,
			Long currentProcessId) {
		boolean isSenderPresent = false;
		Map<Long, Map<Long, Integer>> sendMap = getProcessSendCountMap();
		Event firstEvent = pQueue.peek();
		if (sendMap.containsKey(firstEvent.getReceive().getSourceProcessId())) {
			Map<Long, Integer> sourceMap = sendMap.get(firstEvent.getReceive().getSourceProcessId());
			if (sourceMap.containsKey(currentProcessId)) {
				int count = sourceMap.get(currentProcessId); // get the count from
				// sender map
				if (count == 0) {
					return false;
				} else { // if count is not 0 that means sender has sent a message to it, which
					// it
					// is ready to receive so no deadlock should occur
					return true;
				}
			}
		}
		return isSenderPresent;
	}

	public static void insertInPQIntAndSendEvnts(int noOfProcess, int noOfEvents, double probOfIntrnlEvent) {
		for (int i = 0; i < processList.size(); i++) {
			PriorityBlockingQueue<Event> pQueue = new PriorityBlockingQueue<>(noOfEvents, new Comparator<Event>() {
				@Override
				public int compare(Event event1, Event event) {
					if (event1.getEventId() == event.getEventId())
						return 0;
					return event1.getEventId() > event.getEventId() ? 1 : -1;
				}
			});
			pQueue = ProcessSimUtil.insertInPQueue(noOfEvents, processList.get(i).getId(), probOfIntrnlEvent, pQueue,
					noOfProcess);
			processList.get(i).setpQueue(pQueue);
		}
	}

	/**
	 * Create send map and for each send event create receive events in priority
	 * queue
	 */
	public static void createReceiveEventAndSendMap() {
		// make map of source and value as map of destination with count of send events
		// {13={14=1, 15=2}, 14={13=3}, 15={14=2}} - here 13 sends 14 1 event and 15 2
		// events, similarly 14 sends 3 events to 13 , and 15 sends 2 events to 14
		Map<Long, Map<Long, Integer>> sendMap = new HashMap<>();
		for (int i = 0; i < processList.size(); i++) {
			long sourceId = processList.get(i).getId();
			Map<Long, Integer> destCount = new HashMap<>();
			for (Event e : processList.get(i).getpQueue()) {
				if (e.getSend().isSend()) {

					Long destinationId = e.getSend().getDestinationId();
					if (destCount.containsKey(destinationId)) {
						destCount.put(destinationId, destCount.get(destinationId) + 1);
					} else {
						destCount.put(destinationId, 1);
					}
				}
			}
			sendMap.put(sourceId, destCount);
		}

		// using sendMap, for each send, create a receive event and add it to priority
		// queue
		for (Entry<Long, Map<Long, Integer>> entry : sendMap.entrySet()) {
			Map<Long, Integer> destCount = entry.getValue();
			for (int i = 0; i < processList.size(); i++) {
				if (destCount.containsKey(processList.get(i).getId())) {
					for (int k = 0; k < destCount.get(processList.get(i).getId()); k++) {
						Event e = new Event();
						e.setEventId(ProcessSimUtil.getRemainingEventIdMap().get(processList.get(i).getId()).poll());
						Receive receive = new Receive();
						receive.setReceive(true);
						receive.setSourceProcessId(entry.getKey());
						e.setReceive(receive);
						PriorityBlockingQueue<Event> prtyQ = processList.get(i).getpQueue();
						prtyQ.add(e);
						processList.get(i).setpQueue(prtyQ);
					}
				}
			}
		}

	}

	public static boolean isLocalEvent(double probabilty) {
		return (Math.random() >= 1.0 - probabilty);
	}

	public static synchronized boolean checkIfDeadLockPresent(Map<Long, Map<Long, Integer>> sendMap,
			List<Processor> pList) {
		boolean isDeadLock = false;

		List<Boolean> deadLockList = new ArrayList<>();
		// check if any of the first receive event in any queue is ready to receive, and
		// its sender has already sent to it
		for (int i = 0; i < pList.size(); i++) {
			Event firstEvent = pList.get(i).getpQueue().peek(); // take first event of each
																// process
			if (null != firstEvent && firstEvent.getReceive().isReceive()) { // check if this is a
																				// receive
																				// event
				if (sendMap.containsKey(firstEvent.getReceive().getSourceProcessId())) { // check if any
																							// sender
																							// has
																							// sent and
																							// source of
																							// the
																							// receive
																							// event
																							// matches
					Map<Long, Integer> sourceMap = sendMap.get(firstEvent.getReceive().getSourceProcessId());
					if (sourceMap.containsKey(pList.get(i).getId())) { // check if receiver of
																		// sender
																		// and current process
																		// same
						int count = sourceMap.get(pList.get(i).getId()); // get the count from
																			// sender
																			// map
						if (count == 0) {
							deadLockList.add(true);
						} else { // if count is not 0 that means sender has sent a message to it, which
									// it
									// is ready to receive so no deadlock should occur
							deadLockList.add(false);
							break;
						}
					}
				}
			}
		}
		if (deadLockList.isEmpty() || (!deadLockList.isEmpty() && !deadLockList.contains(false))) {
			isDeadLock = true;
		}
		return isDeadLock;
	}

	public static List<Processor> getProcessList() {
		return processList;
	}

	public static void setProcessList(List<Processor> processList) {
		ProcessSimUtil.processList = processList;
	}

	public static synchronized Map<Long, LinkedList<Integer>> getRemainingEventIdMap() {
		return remainingEventIdMap;
	}

	public synchronized static void setRemainingEventIdMap(Map<Long, LinkedList<Integer>> remainingEventIdMap) {
		ProcessSimUtil.remainingEventIdMap = remainingEventIdMap;
	}

	public synchronized static Map<Long, Map<Long, Integer>> getProcessSendCountMap() {
		return processSendCountMap;
	}

	public synchronized static void setProcessSendCountMap(Map<Long, Map<Long, Integer>> processSendCountMap) {
		ProcessSimUtil.processSendCountMap = processSendCountMap;
	}

	public synchronized static Map<Long, Integer> getMaximumEventNumber() {
		return maximumEventNumber;
	}

	public synchronized static void setMaximumEventNumber(Map<Long, Integer> maximumEventNumber) {
		ProcessSimUtil.maximumEventNumber = maximumEventNumber;
	}

	public static synchronized long getNumOfEvents() {
		return numOfEvents;
	}

	public static synchronized void setNumOfEvents(long numOfEvents) {
		ProcessSimUtil.numOfEvents = numOfEvents;
	}

	public static synchronized boolean isProcessSuspended() {
		return isProcessSuspended;
	}

	public static synchronized void setProcessSuspended(boolean isProcessSuspended) {
		ProcessSimUtil.isProcessSuspended = isProcessSuspended;
	}

}
