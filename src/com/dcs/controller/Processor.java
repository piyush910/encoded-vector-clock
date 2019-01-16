package com.dcs.controller;

import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

import com.dcs.constant.Constants;
import com.dcs.model.Event;
import com.dcs.utility.EVCUtil;
import com.dcs.utility.ProcessSimUtil;

public class Processor extends Thread {
	private PriorityBlockingQueue<Event> pQueue;

	@Override
	public void run() {
		try {
			long currentProcessId = Thread.currentThread().getId();
			System.out.println("Thread Id:" + currentProcessId + " started");
			while (!pQueue.isEmpty()) {
				if (!ProcessSimUtil.isProcessSuspended()) {
				Event e = pQueue.peek();
				if (e.getSend().isSend()) {
					// for each send increment a counter at destination, so that destination knows
					// how many events it has received {13={14=1}} 13 has sent 1 event to 14
					ProcessSimUtil.setSendMap(e, currentProcessId);
					pQueue.poll();
					EVCUtil.sendEvcUpdation(currentProcessId, e);
					System.out.println(Constants.SEND_EVENT + " from Process Id:" + currentProcessId
							+ " to destination Process Id:" + e.getSend().getDestinationId() + " and their event id is:"
							+ e.getEventId()
							+ "\n\n\n--------------------------------------------------------------\n\n\n");
				} else if (e.getReceive().isReceive()) {
					// for each receive check if the source is the same in ProcessSendCountMap and
					// then find if current receiving process has its own id in the values and then
					// decrement the count
					boolean checkIfSendrSntMsg = ProcessSimUtil.isSenderPresentInMap(pQueue, currentProcessId);
					if (checkIfSendrSntMsg) {
						ProcessSimUtil.decrementAndSetSendMap(e, currentProcessId);
						pQueue.poll();
						EVCUtil.receiveLcmUpdation(currentProcessId, e);
						System.out.println(Constants.RECEIVE_EVENT + " at Process Id:" + currentProcessId
								+ " from source Process Id:" + e.getReceive().getSourceProcessId()
								+ " and their event id is:" + e.getEventId()
								+ "\n\n\n--------------------------------------------------------------\n\n\n");
					} else {
						boolean hasSenderNotSentMsg = true;
						while (hasSenderNotSentMsg) {
							boolean isDeadLock = ProcessSimUtil.checkIfDeadLockPresent(
									ProcessSimUtil.getProcessSendCountMap(), ProcessSimUtil.getProcessList());
							if (isDeadLock) {
								if (pQueue.size() > 1) {
									pQueue = ProcessSimUtil.genNewProcessIdQ(currentProcessId, e, pQueue);
									break;
								}
							}
							if (ProcessSimUtil.getProcessSendCountMap()
									.containsKey(e.getReceive().getSourceProcessId())) {
								Map<Long, Integer> sourceMap = ProcessSimUtil.getProcessSendCountMap()
										.get(e.getReceive().getSourceProcessId());
								if (sourceMap.containsKey(currentProcessId)) {
									if (sourceMap.get(currentProcessId) == 0) {
										try {
											Thread.sleep(1000);
										} catch (InterruptedException e1) {
											e1.printStackTrace();
										}
									} else {
										ProcessSimUtil.decrementAndSetSendMap(e, currentProcessId);
										pQueue.poll();
										hasSenderNotSentMsg = false;
										EVCUtil.receiveLcmUpdation(currentProcessId, e);
										System.out.println(Constants.RECEIVE_EVENT + " at Process Id:"
												+ currentProcessId + " from source Process Id:"
												+ e.getReceive().getSourceProcessId() + " and their event id is:"
												+ e.getEventId()
												+ "\n\n\n--------------------------------------------------------------\n\n\n");
										break;
									}
								} else {
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e1) {
										e1.printStackTrace();
									}
								}
							} else {
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
							}
						}
					}
				} else {
					pQueue.poll();
					EVCUtil.internalEvcUpdate(currentProcessId);
					System.out.println(Constants.INTERNAL_EVENT + " at Process Id:" + currentProcessId
							+ " and their event id is:" + e.getEventId()
							+ "\n\n\n--------------------------------------------------------------\n\n\n");
				}
				EVCUtil.setEventAndMaxEventBitSize(currentProcessId);
				if (Constants.IS_PART_A_RUNNING) {
					if (EVCUtil.getGlobalPrimeNoMap().get(currentProcessId)
							.bitLength() > ProcessSimUtil.getProcessList().size() * EVCUtil.getBitSize()) {
						System.out.println("System Exiting!!!!");
						System.exit(0);
					}
				} else {
					if (EVCUtil.getEvcVsLogOfEvcMap().size() >= 50 * ProcessSimUtil.getProcessList().size()) {
						ProcessSimUtil.setProcessSuspended(true);
						EVCUtil.findFalsePositiveNegative();
						System.out.println("System Exiting!!!!");
						System.exit(0);
					}
				}
			}
			}
			System.out.println(EVCUtil.getGlobalPrimeNoMap());
			System.out.println("Full Thread Executed " + EVCUtil.getEvcVsLogOfEvcMap());
			System.out.println("Thread Id " + currentProcessId + " shutting down");
		} catch (Exception exc) {
			{
				exc.printStackTrace();
			}

		}
	}

	public PriorityBlockingQueue<Event> getpQueue() {
		return pQueue;
	}

	public void setpQueue(PriorityBlockingQueue<Event> pQueue) {
		this.pQueue = pQueue;
	}
}
