package com.dcs.utility;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;
import org.apfloat.Apint;
import org.apfloat.ApintMath;

import com.dcs.constant.Constants;
import com.dcs.model.Event;

public class EVCUtil {
	private static volatile BigInteger latestPrimeNo = BigInteger.ZERO;
	private static Map<Long, BigInteger> processAssocPrimeNoMap = new HashMap<Long, BigInteger>();
	private static volatile ConcurrentHashMap<Long, BigInteger> globalEVCMap = new ConcurrentHashMap<Long, BigInteger>();
	private static volatile ConcurrentHashMap<Long, Map<Long, ArrayList<BigInteger>>> senderEVCMap = new ConcurrentHashMap<Long, Map<Long, ArrayList<BigInteger>>>();
	private static volatile int bitSize;
	private static volatile ConcurrentHashMap<Long, Integer> numOfEventEvcSizeMap = new ConcurrentHashMap<>();
	private static volatile ConcurrentHashMap<BigInteger, Apfloat> evcVsLogOfEvcMap = new ConcurrentHashMap<>();
	private static volatile Integer maxEventBitSize = 0;

	/**
	 * Generate next prime number
	 * 
	 * @param latestPrimeNo
	 * @return
	 */
	public static synchronized BigInteger generateNextPrime() {
		latestPrimeNo = latestPrimeNo.add(BigInteger.ONE);
		if (latestPrimeNo.compareTo(BigInteger.valueOf(2)) < 0) {
			latestPrimeNo = BigInteger.valueOf(2);
			return latestPrimeNo;
		}
		for (BigInteger i = BigInteger.valueOf(2); i.compareTo(latestPrimeNo) < 0; i = i.add(BigInteger.ONE)) {
			if (latestPrimeNo.mod(i).compareTo(BigInteger.ZERO) == 0) {
				latestPrimeNo = latestPrimeNo.add(BigInteger.ONE);
				i = BigInteger.valueOf(2);
			} else {
				continue;
			}
		}
		return latestPrimeNo;
	}

	public static boolean checkMaxSizeInt(Integer val) {
		if (val >= Integer.MAX_VALUE) {
			return true;
		}
		return false;
	}

	/**
	 * Finds LCM of 2 numbers.
	 * 
	 * @param senderPrimeNo
	 * @param newPrimeNoOfProcess
	 * @return
	 */
	public static BigInteger findLcm(BigInteger a, BigInteger b) {
		return (a.multiply(b)).divide(a.gcd(b));
	}

	public static synchronized void receiveLcmUpdation(long currentProcessId, Event e) {
		Map<Long, ArrayList<BigInteger>> sendPrimeNumMap = EVCUtil.getSenderPrimeNoMap()
				.get(e.getReceive().getSourceProcessId());
		if (null != sendPrimeNumMap && null != sendPrimeNumMap.get(currentProcessId)) {
			if (sendPrimeNumMap.containsKey(currentProcessId)) {
				BigInteger primeNoOfProcess = EVCUtil.getProcessAssocPrimeNoMap().get(currentProcessId);
				ConcurrentHashMap<Long, BigInteger> globalPNumMap = EVCUtil.getGlobalPrimeNoMap();
				ArrayList<BigInteger> senderPrimeList = sendPrimeNumMap.get(currentProcessId);
				BigInteger senderPrimeNo = senderPrimeList.get(0);

				senderPrimeList.remove(0);
				sendPrimeNumMap.put(currentProcessId, senderPrimeList);
				EVCUtil.getSenderPrimeNoMap().put(e.getReceive().getSourceProcessId(), sendPrimeNumMap);

				BigInteger primeNoOfSenderProcess = globalPNumMap.get(currentProcessId).multiply(primeNoOfProcess);
				BigInteger newPrimeNumber = EVCUtil.findLcm(senderPrimeNo, primeNoOfSenderProcess);
				// Part-B Code-Starts
				if (!Constants.IS_PART_A_RUNNING) {
					Apint antiLogOfSenderEVC = ApfloatMath
							.pow(Constants.BASE_OF_LOG, evcVsLogOfEvcMap.get(senderPrimeNo)).ceil();
					Apint antiLogOfCurrentEvc = ApfloatMath
							.pow(Constants.BASE_OF_LOG, evcVsLogOfEvcMap.get(globalPNumMap.get(currentProcessId)))
							.ceil();

					Apint gcd = ApintMath.gcd(antiLogOfSenderEVC, antiLogOfCurrentEvc);
					Apfloat ti = evcVsLogOfEvcMap.get(senderPrimeNo)
							.add(evcVsLogOfEvcMap.get(globalPNumMap.get(currentProcessId)));
					Apfloat gcdApFloat = new Apfloat(gcd.toBigInteger(), Constants.PRECISION_OF_APFLOAT);
					ti = ti.subtract(ApfloatMath.log(gcdApFloat, Constants.BASE_OF_LOG));
					Apfloat pNumOfProcess = new Apfloat(primeNoOfProcess, Constants.PRECISION_OF_APFLOAT);
					ti = ti.add(ApfloatMath.log(pNumOfProcess, Constants.BASE_OF_LOG));
					evcVsLogOfEvcMap.put(newPrimeNumber, ti);
				}
				// Part-B Code-Ends
				globalPNumMap.put(currentProcessId, newPrimeNumber);
				EVCUtil.setGlobalPrimeNoMap(globalPNumMap);

			}
		}
	}

	public static synchronized void sendEvcUpdation(Long currentProcessId, Event e) {
		BigInteger primeNoOfProcess = EVCUtil.getProcessAssocPrimeNoMap().get(currentProcessId); // current process
																									// associated prime
																									// number
		ConcurrentHashMap<Long, BigInteger> globalPNumMap = EVCUtil.getGlobalPrimeNoMap(); // current global prime
																							// number
		// map
		if (EVCUtil.getSenderPrimeNoMap().containsKey(currentProcessId)) { // check if current process has sent any
																			// message before
			Map<Long, ArrayList<BigInteger>> sendPrimeNumMap = EVCUtil.getSenderPrimeNoMap().get(currentProcessId);
			if (globalPNumMap.containsKey(currentProcessId)) { // check if in global prime number map current process id
																// is present
				Apfloat oldPrimeNum = evcVsLogOfEvcMap.get(globalPNumMap.get(currentProcessId)); // Part-B Code
				BigInteger newPrimeNoOfProcess = globalPNumMap.get(currentProcessId).multiply(primeNoOfProcess); // for
																													// send
																													// event
																													// multiply
																													// previous
																													// EVC
																													// with
																													// prime
				globalPNumMap.put(currentProcessId, newPrimeNoOfProcess);
				EVCUtil.setGlobalPrimeNoMap(globalPNumMap);
				// PartB Code - Starts
				if (!Constants.IS_PART_A_RUNNING) {
					Apfloat pNumOfProcess = new Apfloat(primeNoOfProcess, Constants.PRECISION_OF_APFLOAT);
					evcVsLogOfEvcMap.put(newPrimeNoOfProcess,
							oldPrimeNum.add(ApfloatMath.log(pNumOfProcess, Constants.BASE_OF_LOG)));
				}
				// PartB Code - Ends
				if (sendPrimeNumMap.containsKey((e.getSend().getDestinationId()))) {
					ArrayList<BigInteger> primeNumList = sendPrimeNumMap.get(e.getSend().getDestinationId());
					primeNumList.add(newPrimeNoOfProcess);
					if (primeNumList.size() > 1) {
						Collections.sort(primeNumList);
					}
					sendPrimeNumMap.put(e.getSend().getDestinationId(), primeNumList);
					EVCUtil.getSenderPrimeNoMap().put(currentProcessId, sendPrimeNumMap);
				} else {
					ArrayList<BigInteger> primeNumList = new ArrayList<>();
					primeNumList.add(newPrimeNoOfProcess);
					sendPrimeNumMap.put(e.getSend().getDestinationId(), primeNumList);
					EVCUtil.getSenderPrimeNoMap().put(currentProcessId, sendPrimeNumMap);
				}
			} else {
				globalPNumMap.put(currentProcessId, primeNoOfProcess);
				EVCUtil.setGlobalPrimeNoMap(globalPNumMap);
				ArrayList<BigInteger> primeNumList = new ArrayList<>();
				primeNumList.add(primeNoOfProcess);
				sendPrimeNumMap.put(e.getSend().getDestinationId(), primeNumList);
				EVCUtil.getSenderPrimeNoMap().put(currentProcessId, sendPrimeNumMap);
				// PartB Code - Starts
				if (!Constants.IS_PART_A_RUNNING) {
					Apfloat pNumOfProcess = new Apfloat(primeNoOfProcess, Constants.PRECISION_OF_APFLOAT);
					evcVsLogOfEvcMap.put(primeNoOfProcess, ApfloatMath.log(pNumOfProcess, Constants.BASE_OF_LOG));
				}
				// PartB Code - Ends
			}
		} else {
			globalPNumMap.put(currentProcessId, primeNoOfProcess);
			EVCUtil.setGlobalPrimeNoMap(globalPNumMap);
			ArrayList<BigInteger> primeNumList = new ArrayList<>();
			primeNumList.add(primeNoOfProcess);
			Map<Long, ArrayList<BigInteger>> sendPrimeNumMap = new HashMap<>();
			sendPrimeNumMap.put(e.getSend().getDestinationId(), primeNumList);
			EVCUtil.getSenderPrimeNoMap().put(currentProcessId, sendPrimeNumMap);
			// PartB Code - Starts
			if (!Constants.IS_PART_A_RUNNING) {
				Apfloat pNumOfProcess = new Apfloat(primeNoOfProcess, Constants.PRECISION_OF_APFLOAT);
				evcVsLogOfEvcMap.put(primeNoOfProcess, ApfloatMath.log(pNumOfProcess, Constants.BASE_OF_LOG));
			}
			// PartB Code - Ends
		}

	}

	public static void internalEvcUpdate(long currentProcessId) {
		BigInteger primeNoOfProcess = EVCUtil.getProcessAssocPrimeNoMap().get(currentProcessId);
		ConcurrentHashMap<Long, BigInteger> globalPNumMap = EVCUtil.getGlobalPrimeNoMap();
		if (globalPNumMap.containsKey(currentProcessId)) {
			Apfloat oldPrimeNum = evcVsLogOfEvcMap.get(globalPNumMap.get(currentProcessId)); // Part-B Code
			BigInteger newPrimeNoOfProcess = globalPNumMap.get(currentProcessId).multiply(primeNoOfProcess);
			globalPNumMap.put(currentProcessId, newPrimeNoOfProcess);
			EVCUtil.setGlobalPrimeNoMap(globalPNumMap);
			// PartB Code - Starts
			if (!Constants.IS_PART_A_RUNNING) {
				Apfloat pNumOfProcess = new Apfloat(primeNoOfProcess, Constants.PRECISION_OF_APFLOAT);
				evcVsLogOfEvcMap.put(newPrimeNoOfProcess,
						oldPrimeNum.add(ApfloatMath.log(pNumOfProcess, Constants.BASE_OF_LOG)));
			}
			// PartB Code - Ends
		} else {
			globalPNumMap.put(currentProcessId, primeNoOfProcess);
			EVCUtil.setGlobalPrimeNoMap(globalPNumMap);
			// PartB Code - Starts
			if (!Constants.IS_PART_A_RUNNING) {
				Apfloat pNumOfProcess = new Apfloat(primeNoOfProcess, Constants.PRECISION_OF_APFLOAT);
				evcVsLogOfEvcMap.put(primeNoOfProcess, ApfloatMath.log(pNumOfProcess, Constants.BASE_OF_LOG));
			}
			// PartB Code - Ends
		}
	}

	public static synchronized void setEventAndMaxEventBitSize(long currentProcessId) {
		int maxBitSize = Collections.max(EVCUtil.getGlobalPrimeNoMap().values()).bitLength();
		if (maxBitSize > EVCUtil.getMaxEventBitSize()) {
			EVCUtil.setMaxEventBitSize(maxBitSize);
		}
		ProcessSimUtil.setNumOfEvents(ProcessSimUtil.getNumOfEvents() + 1);
		if (ProcessSimUtil.getNumOfEvents() % 5 == 0) {
			EVCUtil.getNumOfEventEvcSizeMap().put(ProcessSimUtil.getNumOfEvents(), maxBitSize);
		}
	}

	public static synchronized void findFalsePositiveNegative() {
		System.out.println("EVC vs Log Of EVC Map Size" + EVCUtil.getEvcVsLogOfEvcMap().size());
		int falsePositive = 0;
		int falseNegative = 0;
		int truePositive = 0;
		int trueNegative = 0;
		int count = 0;
		ArrayList<BigInteger> keySet = new ArrayList<>();
		keySet.addAll(EVCUtil.getEvcVsLogOfEvcMap().keySet());
		System.out.println("keyset: " + keySet.size());
		for (int i = 0; i < keySet.size(); i++) {
			for (int j = i + 1; j < keySet.size(); j++) {
				count++;
				Apfloat secondEvcLog = EVCUtil.getEvcVsLogOfEvcMap().get(keySet.get(j))
						.precision(Constants.PRECISION_OF_APFLOAT);
				Apfloat firstEvcLog = EVCUtil.getEvcVsLogOfEvcMap().get(keySet.get(i))
						.precision(Constants.PRECISION_OF_APFLOAT);
				Apfloat evcSubtract = secondEvcLog.subtract(firstEvcLog).precision(Constants.PRECISION_OF_APFLOAT);
				Double evcSubtractD = evcSubtract.doubleValue();
				Double evcAntilog = Math.pow(Constants.BASE_OF_LOG_DOUBLE, evcSubtractD);
				if (keySet.get(j).compareTo(keySet.get(i)) == 1
						&& keySet.get(j).mod(keySet.get(i)).compareTo(BigInteger.valueOf(0)) == 0) {
					if (secondEvcLog.compareTo(firstEvcLog) == 1 && (evcAntilog == Math.floor(evcAntilog))
							&& !Double.isInfinite(evcAntilog)) {
						truePositive++;
					} else if (!Double.isInfinite(evcAntilog)) {
						falseNegative++;
					}
				} else if (!(keySet.get(j).compareTo(keySet.get(i)) == 1
						&& keySet.get(j).mod(keySet.get(i)).compareTo(BigInteger.valueOf(0)) != 0)) {
					if (secondEvcLog.compareTo(firstEvcLog) == 1 && (evcAntilog == Math.floor(evcAntilog))
							&& !Double.isInfinite(evcAntilog)) {
						falsePositive++;
					} else if (!Double.isInfinite(evcAntilog)) {
						trueNegative++;
					}
				}

			}
		}
		System.out.println("Total Pairs: " + count);
		System.out.println("True Positive: " + truePositive);
		System.out.println("True Negative: " + trueNegative);
		System.out.println("False Negative: " + falseNegative);
		System.out.println("False Positive: " + falsePositive);
		System.out.println("Error Total: " + (falseNegative + falsePositive));

		System.out.println(
				"False Negative Error Percent: " + ((new Float(falseNegative) / (falseNegative + truePositive)) * 100));
		System.out.println(
				"False Positive Error Percent: " + ((new Float(falsePositive) / (falsePositive + trueNegative)) * 100));
		float errorTotal = falseNegative + falsePositive;
		System.out.println("Total Error Percent: " + (errorTotal / count) * 100);
	}

	public static BigInteger getCurrentPrime() {
		return latestPrimeNo;
	}

	public static synchronized void setCurrentPrime(BigInteger currentPrime) {
		EVCUtil.latestPrimeNo = currentPrime;
	}

	public static ConcurrentHashMap<Long, BigInteger> getGlobalPrimeNoMap() {
		return globalEVCMap;
	}

	public static void setGlobalPrimeNoMap(ConcurrentHashMap<Long, BigInteger> globalPrimeNoMap) {
		EVCUtil.globalEVCMap = globalPrimeNoMap;
	}

	public static Map<Long, BigInteger> getProcessAssocPrimeNoMap() {
		return processAssocPrimeNoMap;
	}

	public static void setProcessAssocPrimeNoMap(Map<Long, BigInteger> processAssocPrimeNoMap) {
		EVCUtil.processAssocPrimeNoMap = processAssocPrimeNoMap;
	}

	public static ConcurrentHashMap<Long, Map<Long, ArrayList<BigInteger>>> getSenderPrimeNoMap() {
		return senderEVCMap;
	}

	public static synchronized void setSenderPrimeNoMap(
			ConcurrentHashMap<Long, Map<Long, ArrayList<BigInteger>>> senderPrimeNoMap) {
		EVCUtil.senderEVCMap = senderPrimeNoMap;
	}

	public static int getBitSize() {
		return bitSize;
	}

	public static void setBitSize(int bitSize) {
		EVCUtil.bitSize = bitSize;
	}

	public static synchronized ConcurrentHashMap<Long, Integer> getNumOfEventEvcSizeMap() {
		return numOfEventEvcSizeMap;
	}

	public static synchronized void setNumOfEventEvcSizeMap(ConcurrentHashMap<Long, Integer> numOfEventEvcSizeMap) {
		EVCUtil.numOfEventEvcSizeMap = numOfEventEvcSizeMap;
	}

	public static synchronized ConcurrentHashMap<BigInteger, Apfloat> getEvcVsLogOfEvcMap() {
		return evcVsLogOfEvcMap;
	}

	public static synchronized void setEvcVsLogOfEvcMap(ConcurrentHashMap<BigInteger, Apfloat> evcVsLogOfEvcMap) {
		EVCUtil.evcVsLogOfEvcMap = evcVsLogOfEvcMap;
	}

	public static Integer getMaxEventBitSize() {
		return maxEventBitSize;
	}

	public static void setMaxEventBitSize(Integer maxEventBitSize) {
		EVCUtil.maxEventBitSize = maxEventBitSize;
	}

}
