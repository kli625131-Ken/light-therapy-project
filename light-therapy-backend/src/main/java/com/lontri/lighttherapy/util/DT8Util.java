package com.lontri.lighttherapy.util;

public class DT8Util {
	/**
	 * 把0-100之间的数值曲线换算为SR的最大输出0-254
	 * 
	 * @param value
	 * @return
	 */
	public static int luxToSR(int value) {
		double result = 0;
		int resultHex = 0;
		if (value <= 10) {
			result = 16.7 * value;
			resultHex = (int) result;
		} else if (value <= 20) {
			result = 2.5 * value + 143;
			resultHex = (int) result;
		} else if (value <= 30) {
			result = 1.5 * value + 162;
			resultHex = (int) result;
		} else if (value <= 40) {
			result = 1.1 * value + 174;
			resultHex = (int) result;
		} else if (value <= 50) {
			result = 0.8 * value + 186;
			resultHex = (int) result;
		} else if (value <= 60) {
			result = 0.7 * value + 191;
			resultHex = (int) result;
		} else if (value <= 70) {
			result = 0.6 * value + 197;
			resultHex = (int) result;
		} else if (value <= 80) {
			result = 0.6 * value + 197;
			resultHex = (int) result;
		} else if (value <= 90) {
			result = 0.5 * value + 205;
			resultHex = (int) result;
		} else if (value <= 100) {
			result = 0.4 * value + 214;
			resultHex = (int) result;
		}
		return resultHex;
	}

	/**
	 * 把SR的最大输出0-254换算0-100之间的数值换算为
	 * 
	 * @param value
	 * @return
	 */
	public static int SRtoLux(int value) {
		double result = 0;
		int resultHex = 0;
		if (value <= 167) {
			result = value / 16.7;
			resultHex = (int) result;
		} else if (value <= 192) {
			result = value / 2.5 - 57.2;
			resultHex = (int) result;
		} else if (value <= 207) {
			result = value / 1.5 - 108;
			resultHex = (int) result;
		} else if (value <= 218) {
			result = value / 1.1 - 158;
			resultHex = (int) result;
		} else if (value <= 226) {
			result = value / 0.8 - 232.5;
			resultHex = (int) result;
		} else if (value <= 233) {
			result = value / 0.7 - 272.8;
			resultHex = (int) result;
		} else if (value <= 239) {
			result = value / 0.6 - 328.3;
			resultHex = (int) result;
		} else if (value <= 245) {
			result = value / 0.6 - 328.3;
			resultHex = (int) result;
		} else if (value <= 250) {
			result = value / 0.5 - 410;
			resultHex = (int) result;
		} else if (value <= 254) {
			result = value / 0.4 - 535;
			resultHex = (int) result;
		}
		return resultHex;
	}

	/**
	 * 把色温（K）2700-6500换算DT8的可识别的色温变化值指令
	 * 
	 * @param value
	 * @return
	 */
	public static String DT8Temp(int value){
		String data = "";
		if(value>=2700 &&  value<=6500){
			int dt8Out = 1000000 / value;
			String k16Value = String.format("%04X", dt8Out);
			String gbits = k16Value.substring(0, 2);
			String dbits = k16Value.substring(2);
			data = "A3" + dbits + "C3" + gbits + "C108FFE7C108FFE2";
		}
		return data;
	}
	/**
	 * 把色温（K）2700-6500换算DT8的可识别的色温变化值指令
	 * 
	 * @param value
	 * @return
	 */
	public static String ToDT8Temp(int value){
		String data = "";
		if(value>=0 &&  value<=100){
			int dt8Out = 1000000 / value;
			data = String.format("%02X", dt8Out);
		}
		return data;
	}
}
