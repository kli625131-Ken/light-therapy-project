package com.lontri.lighttherapy.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
	
	/**
     * 去除html代码中含有的标签
     * @param htmlStr
     * @return
     */
    public static String delHtmlTags(String htmlStr) {
        //定义script的正则表达式，去除js可以防止注入
        String scriptRegex="<script[^>]*?>[\\s\\S]*?<\\/script>";
        //定义style的正则表达式，去除style样式，防止css代码过多时只截取到css样式代码
        String styleRegex="<style[^>]*?>[\\s\\S]*?<\\/style>";
        //定义HTML标签的正则表达式，去除标签，只提取文字内容
        String htmlRegex="<[^>]+>";
        //定义空格,回车,换行符,制表符
        String spaceRegex = "\\s*|\t|\r|\n";
 
        // 过滤script标签
        htmlStr = htmlStr.replaceAll(scriptRegex, "");
        // 过滤style标签
        htmlStr = htmlStr.replaceAll(styleRegex, "");
        // 过滤html标签
        htmlStr = htmlStr.replaceAll(htmlRegex, "");
        // 过滤空格等
        htmlStr = htmlStr.replaceAll(spaceRegex, "");
        return htmlStr.trim().replaceAll(" ",""); // 返回文本字符串
    }
    
	/**
	 * 是否是数字
	 */
	public static boolean isNumeric(String str) {
		if (!isEmpty(str))
	        //return str.matches("^[0-9]*$");
			return str.matches("^-?\\d+(.\\d+)?$");
	    else
	        return false;
	}
	
	/**
	 * 是否是正数字
	 */
	public static boolean isNumericZ(String str) {
		if (!isEmpty(str))
			return str.matches("^\\d+(.\\d+)?$");
	    else
	        return false;
	}
	
	public static boolean isCarNo(String carNo){
		if (StringUtil.isEmpty(carNo)) return false;
		carNo = carNo.trim().toUpperCase();
		if (carNo.trim().length() < 8 && carNo.trim().length() > 10) return false;
		String regx = "^(([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z](([0-9]{5}[A-Z])|([A-Z]([A-HJ-NP-Z0-9])[0-9]{4})))|([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-HJ-NP-Z0-9]{4}[A-HJ-NP-Z0-9挂学警港澳使领]))$";
		return Pattern.compile(regx).matcher(carNo).find();
	}
	
	/**
	 * 字符串是否相同
	 */
	public static boolean stringSame(String str1,String str2) {
		if (str1 == null) str1 = "";
		if (str2 == null) str2 = "";
		return str1.trim().equalsIgnoreCase(str2.trim());
	}
	
	public static byte byteParse(Object o){
		if (o == null)return 0;
		String oo = o.toString();
		if (oo.length() == 0 || "-".equals(oo)) return (byte)0;
		int i = oo.indexOf(".");
		if (i > -1)
        {
            oo = oo.substring(0,i);
        }
		return Byte.parseByte(oo);
	}
	public static int intParse(Object o){
		if (o == null)return 0;
		String oo = o.toString();
		if (oo.length() == 0 || "-".equals(oo)) return 0;
		int i = oo.indexOf(".");
		if (i > -1)
        {
            oo = oo.substring(0,i);
        }
		return Integer.parseInt(oo);
	}
	public static long longParse(Object o){
		if (o == null)return 0;
		String oo = o.toString();
		if (oo.length() == 0 || "-".equals(oo)) return 0L;
		int i = oo.indexOf(".");
		if (i > -1)
        {
            oo = oo.substring(0,i);
        }
		return Long.parseLong(oo);
	}
	public static double doubleParse(Object o){
		if (o == null)return 0d;
		String oo = o.toString();
		if (oo.length() == 0 || "-".equals(oo)) return 0d;
		double d = 0;
		try {
		    d = Double.parseDouble(oo);
		} catch (NumberFormatException e) {
			return 0d;
		}
        return d;
	}
	/**
	 * pattern为null时候，使用默认"yyyy-MM-dd HH:mm:ss"
	 * */
	public static Date dateParse(Object o, String pattern) {
		if (StringUtil.isEmpty(o)) {
	            return null;
		}
		if (StringUtil.isEmpty(pattern)) {
			pattern = "yyyy-MM-dd HH:mm:ss";
		}
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
	    Date date = null;
        try {
        	if ("yyyy-MM-dd HH:mm:ss".equals(pattern) && o.toString().trim().length()>7 && o.toString().trim().length()<11){//这里加个判断，传过来可能不带时分秒
    			o = o.toString().trim() + " 00:00:00";
    		}
        	date = simpleDateFormat.parse(o.toString());
	    } catch ( ParseException e ) {
	    	
        }
		return date;
	}
	
	/**
	 * @Param format为F1、F2、F3...
	 * */
	public static String toFixed(double d,String format){
		if (format.length() == 0 || !format.startsWith("F")) return String.valueOf(d);
		if (format.startsWith("F")) format = format.substring(1);

		int iFormat = Integer.valueOf(format);
		double Dp = Math.pow(10,iFormat);
		int iDp = (int)Dp; 
		d = (double)(Math.round(d*iDp))/iDp;
		
		return String.format("%."+format+"f",d);
	}
	
	
	public static double toFixedDouble(double d,String format){
		String dstr = toFixed(d,format);
		return Double.valueOf(dstr);
	}
	
	public static byte[] toBytes(String str,String... charset){
		if (str == null) return null;
		byte [] ret = null;
		try{
			if (charset != null && charset.length > 0 && !charset[0].isEmpty()){
				ret = str.getBytes(charset[0]);
			}else{
				ret = str.getBytes();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return ret;
	}
	
	public static String parseBytes(byte[] bytes,String... charset){
		if (bytes == null) return null;
		String ret = null;
		try{
			if (charset != null && charset.length > 0 && !charset[0].isEmpty()){
				ret = new String(bytes , charset[0]);
			}else{
				ret = new String(bytes);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return ret;
	}
	
	
	/**
	 * pattern为null时候，使用默认"yyyy-MM-dd HH:mm:ss"
	 * */
	public static String stringFromDate(Date date, String pattern){
		if (date == null) return "";
		if (pattern == null || pattern.length() == 0) {
			pattern = "yyyy-MM-dd HH:mm:ss";
		}
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(date);
	}
	
	public static final Object trim(Object obj) {
        if ( obj == null ) {
            return null;
        }
        if ( obj instanceof String ) {
            return ((String) obj).trim();
        } else {
            return obj;
        }
    }
		
	/**
	 * 是否包含中文
	 */
	public static boolean containChinese(String text){
		Pattern pattern = Pattern.compile("[\u4e00-\u9fa5]");
		return pattern.matcher(text).find();
	} 
		
	public static boolean isEmpty(String str){
		return str == null || str.isEmpty();
	}
	
	public static boolean isEmpty(Object value) {
		boolean ret = value == null || trim(value).equals("");
		if(!ret){
			if(value instanceof List){//是否是数组
				@SuppressWarnings("rawtypes")
				List values = (List)value;
				if (values == null || values.isEmpty()) return true;
				else{
					for(Object o : values){
						if(o != null && !trim(o).equals("")){
							return false;
						}
					}
					return true;
				}
			}else if(value instanceof Object[]){//是否是数组
				Object[] values = (Object[])value;
				if (values == null || values.length == 0) return true;
				else{
					for(Object o : values){
						if(o != null && !trim(o).equals("")){
							return false;
						}
					}
					return true;
				}
			}
		}
        return ret;
    }
	
	/**
	 * 是否是Utf8mb4（占4个字节）
	 */
	public static boolean isUtf8mb4(String str){
		if (str == null) return false;
		int length = str.length();
	    return length != str.codePointCount(0, length);
	}
	
	public static String nullAsEmpty(Object str) {
		if (str == null) return "";
		else return str.toString().trim();
	}
	
	public static String emptyAs(Object str,String as) {
		if (StringUtil.isEmpty(str)) return as;
		else return str.toString().trim();
	}
	
	/**
	 * 去除数值后面多余的0
	 * */
	public static String removeEnds0(Object needToRemove, boolean IfPrintZero)
    {
		String retVal = "";
		if (needToRemove == null){
			return IfPrintZero ? "0" : "";
		}
        if (needToRemove.getClass() == Float.class){
        	float f = Float.valueOf(needToRemove.toString());
        	f = (float)(Math.round(f*1000000))/1000000;
        	BigDecimal bgF=new BigDecimal(f+""); 
            retVal = String.valueOf(bgF);
        }
        else if (needToRemove.getClass() == Double.class){//保留小数6位
        	double d = Double.valueOf(needToRemove.toString());
        	d = (double)(Math.round(d*1000000))/1000000;
        	BigDecimal bgD=new BigDecimal(d+""); 
            retVal = String.valueOf(bgD);
        }
        else if (needToRemove.getClass() == Integer.class){
            retVal = needToRemove.toString();
        }
        else{
            retVal = needToRemove.toString().trim();
        }
        if (retVal.length() == 0){
        	return IfPrintZero ? "0" : "";
        }
        if (retVal.indexOf("E") > 0 || retVal.indexOf("e") > 0){
    		retVal = new BigDecimal(retVal).toPlainString();
        }
        if (retVal.indexOf(".") > 0){
        	retVal = retVal.replaceAll("0+?$", "");//去掉多余的0    
            retVal = retVal.replaceAll("[.]$", "");//如最后一位是.则去掉   
        }
        if (retVal.length() == 0){
        	return IfPrintZero ? "0" : "";
        }
        else if (retVal.equals("0")){
        	return IfPrintZero ? "0" : "";
        }
        return retVal;
    }
	
	/**
	 * 去除数值后面多余的0后转化为double
	 */
	public static Double removeEnds0d(Object needToRemove, boolean IfPrintZero){
		String ret = removeEnds0(needToRemove,IfPrintZero);
		if(StringUtil.isEmpty(ret)){
			if(IfPrintZero) return 0d;
			else return null;
		}else{
			return Double.valueOf(ret);
		}
	}
	
	/**
	 * 四舍五入取值
	 */
	public static Integer rounding(Object value, boolean IfPrintZero){
		if (value == null){
			return IfPrintZero ? 0 : null;
		}
		Integer ret = null;
        if (value.getClass() == Float.class){
        	float f = Float.valueOf(value.toString());
        	f = (float)(Math.round(f*1000000))/1000000;
        	ret = Math.round(f);
        }
        else if (value.getClass() == Double.class){//保留小数6位
        	double d = Double.valueOf(value.toString());
        	d = (double)(Math.round(d*1000000))/1000000;
        	ret = (int)Math.round(d);
        }
        else if (value.getClass() == Integer.class){
            ret = (int)value;
        }else{
        	ret = Integer.valueOf(value.toString());
        }
        if (ret == null || ret == 0){
        	return IfPrintZero ? 0 : null;
        }
        return ret;
	}
	
	/**
	 * Json字符串去除特殊字符，如><"&等
	 * */
	public static String jsonValueTrim(String jsonValue)
    {
        return jsonValue.trim().replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;").replace("\"", "&quot;").replace("'", "&apos;").replace("\\", "\\\\");
    }
	
	/**
	 * 移除非法字符
	 * */
	public static String removeillegal(String text){
		if (text == null) return null;
		return text.replaceAll("'", "’");
	}
	
	/**
	 * 是否包含特定字符串
	 * 传入str2为空时，判定为存在
	 * */
	public static boolean inCluding(String str1, String str2, String split)
    {
        if (str2 != null && str2.length() > 0){
            if (isEmpty(str1)) return false;
            str1 = str1.trim().toUpperCase();
            str2 = str2.trim().toUpperCase();
            return (split + str1 + split).contains(split + str2 + split);
        }
        else{
            return true;
        }
    }
	public static String encodeUrl(String url){
		String ret = "";
		try {
			ret = URLEncoder.encode(url,"UTF-8");
			ret = ret.replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static String decodeUrl(String url){
		String ret =  "";
		try {
			ret = URLDecoder.decode(url,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return ret;
	}
	//清理特殊字符
	public static String cleanSpecify(String str){
		String regEx="[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]"; 
		Pattern p = Pattern.compile(regEx); 
		Matcher m = p.matcher(str);
		return m.replaceAll("").trim();
	}
	
	/**
	 * 钱转大写(分割)
	 * @param money
	 * @return
	 */
	public static String [] bigMoneySplit(double money){
		money = Math.abs(money);
		String[] retVal = new String[8];
		String ls_money_unit = "";
		String ls_money = "";
		String ls_unit = "";
		String ls_badge = "";
        int li_long = 0;
        int li_i = 0;

        ls_money = StringUtil.toFixed(money, "F2");
        li_long = ls_money.length();

        for (li_i = 1; li_i < li_long + 1; li_i++){
            ls_unit = ls_money.substring(li_long - li_i, li_long);
            ls_unit = ls_unit.substring(0, 1);

            if (!ls_unit.equals(".")){
                switch (ls_unit) {
                    case "0":
                        ls_unit = "零";
                        break;
                    case "1":
                        ls_unit = "壹";
                        break;
                    case "2":
                        ls_unit = "贰";
                        break;
                    case "3":
                        ls_unit = "叁";
                        break;
                    case "4":
                        ls_unit = "肆";
                        break;
                    case "5":
                        ls_unit = "伍";
                        break;
                    case "6":
                        ls_unit = "陆";
                        break;
                    case "7":
                        ls_unit = "柒";
                        break;
                    case "8":
                        ls_unit = "捌";
                        break;
                    case "9":
                        ls_unit = "玖";
                        break;
                }

                switch (li_i) {

                    case 1:
                        ls_badge = "分";
                        break;
                    case 2:
                        ls_badge = "角";
                        break;
                    case 3:
                        ls_badge = "";
                        break;
                    case 4:
                        ls_badge = "元";
                        break;
                    case 5:
                        ls_badge = "拾";
                        break;
                    case 6:
                        ls_badge = "佰";
                        break;
                    case 7:
                        ls_badge = "仟";
                        break;
                    case 8:
                        ls_badge = "万";
                        break;
                    case 9:
                        ls_badge = "拾";
                        break;
                    case 10:
                        ls_badge = "佰";
                        break;
                    case 11:
                        ls_badge = "仟";
                        break;

                    case 12:
                        ls_badge = "亿";
                        break;
                    case 13:
                        ls_badge = "拾";
                        break;
                    case 14:
                        ls_badge = "佰";
                        break;
                    case 15:
                        ls_badge = "仟";
                        break;
                }
                ls_money_unit = ls_unit + ls_badge + ls_money_unit;
            }
        }

        if (ls_money_unit.lastIndexOf("亿") > -1)
        {
            retVal[7] = ls_money_unit.substring(0, ls_money_unit.lastIndexOf("亿"));
        }
        if (ls_money_unit.lastIndexOf("万") > -1)
        {
            int yi = ls_money_unit.lastIndexOf("亿");
            retVal[6] = ls_money_unit.substring(yi + 1, ls_money_unit.lastIndexOf("万"));
        }
        if (ls_money_unit.lastIndexOf("仟") > -1)
        {
            int wan = ls_money_unit.lastIndexOf("万");
            retVal[5] = ls_money_unit.substring(wan + 1, ls_money_unit.lastIndexOf("仟"));
        }
        if (ls_money_unit.lastIndexOf("佰") > -1)
        {
            int qian = ls_money_unit.lastIndexOf("仟");
            retVal[4] = ls_money_unit.substring(qian + 1, ls_money_unit.lastIndexOf("佰"));
        }
        if (ls_money_unit.lastIndexOf("拾") > -1)
        {
            int bai = ls_money_unit.lastIndexOf("佰");
            retVal[3] = ls_money_unit.substring(bai + 1, ls_money_unit.lastIndexOf("拾"));
        }
        if (ls_money_unit.lastIndexOf("元") > -1)
        {
            int shi = ls_money_unit.lastIndexOf("拾");
            retVal[2] = ls_money_unit.substring(shi + 1, ls_money_unit.lastIndexOf("元"));
        }
        if (ls_money_unit.lastIndexOf("角") > -1)
        {
            int yuan = ls_money_unit.lastIndexOf("元");
            retVal[1] = ls_money_unit.substring(yuan + 1, ls_money_unit.lastIndexOf("角"));
        }
        if (ls_money_unit.lastIndexOf("分") > -1)
        {
            int jiao = ls_money_unit.lastIndexOf("角");
            retVal[0] = ls_money_unit.substring(jiao + 1, ls_money_unit.lastIndexOf("分"));
        }
        return retVal;
	}

	public static String bigMoney(double money){
		money = Math.abs(money);
		String UNIT [] = { "万", "千", "佰", "拾", "亿", "千", "佰", "拾", "万", "千", "佰", "拾", "元", "角", "分" };  
	    String NUM [] = { "零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖" };  
	    double MAX_VALUE = 9999999999999.99D;
	    
		if (money < 0 || money > MAX_VALUE)  
            return "参数非法!";  
        long money1 = Math.round(money * 100); // 四舍五入到分  
        if (money1 == 0)  
            return "零元整";  
        String strMoney = String.valueOf(money1);  
        int numIndex = 0; // numIndex用于选择金额数值  
        int unitIndex = UNIT.length - strMoney.length(); // unitIndex用于选择金额单位  
        boolean isZero = false; // 用于判断当前为是否为零  
        String result = "";  
        for (; numIndex < strMoney.length(); numIndex++, unitIndex++) {  
            char num = strMoney.charAt(numIndex);  
            if (num == '0') {  
                isZero = true;  
                if (UNIT[unitIndex] == "亿" || UNIT[unitIndex] == "万"  
                        || UNIT[unitIndex] == "元") { // 如果当前位是亿、万、元，且数值为零  
                    result = result + UNIT[unitIndex]; //补单位亿、万、元  
                    isZero = false;  
                }   
            }else {  
                if (isZero) {  
                    result = result + "零";  
                    isZero = false;  
                }  
                result = result + NUM[Integer.parseInt(String.valueOf(num))] + UNIT[unitIndex];  
            }  
        }  
        //不是角分结尾就加"整"字  
        if (!result.endsWith("角")&&!result.endsWith("分")) {  
            result = result + "整";  
        }  
        //例如没有这行代码，数值"400000001101.2"，输出就是"肆千亿万壹千壹佰零壹元贰角"  
        result = result.replaceAll("亿万", "亿");  
        return result;
	}
	
	/**
	 * 进行Map合计
	 * @param sum被合计的hashMap
	 * @param key需要合计进的key
	 * @param value需要合计进的value
	 */
	public static void sumMap(Map<String,Double> sum,String key,Object value){
		if (value == null) return;
		if (sum == null) sum = new HashMap<String, Double>();
		double d = StringUtil.doubleParse(value);
		if (!sum.containsKey(key)){
			sum.put(key, d);
		}
		else{
			double oValue = StringUtil.doubleParse(sum.get(key));
			sum.put(key, d + oValue);
		}
	}
	
	/**
	 * 拼接list
	 * @param list
	 * @param split
	 * @return
	 */
	public static String join(List<?> list,String split){
		if (list == null || list.isEmpty()) return "";
		StringBuilder sb = new StringBuilder();
		for(Object s : list){
			if (s == null)continue;
			if (sb.length() > 0) sb.append(split);
			sb.append(String.valueOf(s));
		}
		return sb.toString();
	}
	
	/**
	 * 拼接list
	 * @param list
	 * @param split
	 * @return
	 */
	public static String join(Object [] list,String split){
		if (list == null || list.length == 0) return "";
		StringBuilder sb = new StringBuilder();
		for(Object s : list){
			if (s == null)continue;
			if (sb.length() > 0) sb.append(split);
			sb.append(String.valueOf(s));
		}
		return sb.toString();
	}
	
	/**
	 * 拼接String(如果包含)
	 */
	public static String joinInclude(Object str1,String str2,String split){
		if (str1 == null && str2 == null) return "";
		if (str1 == null) return str2;
		String str1s = str1.toString();
		if (str2 == null || str2.isEmpty()) return str1s;
		if (str1s.isEmpty()) return str2;
		if(!inCluding(str1s,str2,split)){
			str1s += split + str2;
		}
		return str1s;
	}
	
	/**
	 * 去掉特定字符串
	 * */
    public static String removeInclude(String OldStr, String partm, String split){
        if (partm != null && !partm.isEmpty()){
            if ((split + OldStr + split).indexOf(split + partm + split) > -1){
                OldStr = (split + OldStr + split).replace(split + partm + split, split);
                OldStr = OldStr.replace(split + split, split);
                if (OldStr.startsWith(split)) OldStr = OldStr.substring(1);
                if (OldStr.endsWith(split)) OldStr = OldStr.substring(0, OldStr.length() - 1);
            }
        }
        return OldStr;
    }
    
	/**
	 * 计算拆分后的价格
	 * @param amount 装载数
	 * @param totalAmount 总件数
	 * @param fee 要计算的运费
	 * @return
	 */
	public static Double calSeperatorFee(Double amount,Double totalAmount,Double fee){
		if (fee == null || amount == null) return 0d;
		if (totalAmount == null || totalAmount <=0) return fee;
		return StringUtil.toFixedDouble(fee * amount / totalAmount, "F2");
	}
	
	/**
	 * 解码前台escape后的字符串
	 */
	public static String unescape(String text) {
		StringBuffer tmp = new StringBuffer();
		try{
			tmp.ensureCapacity(text.length());
			int lastPos = 0, pos = 0;
			char ch;
			while (lastPos < text.length()) {
				pos = text.indexOf("%", lastPos);
				if (pos == lastPos) {
					if (text.charAt(pos + 1) == 'u') {
						ch = (char) Integer.parseInt(text.substring(pos + 2, pos + 6), 16);
						tmp.append(ch);
						lastPos = pos + 6;
					} else {
						ch = (char) Integer.parseInt(text.substring(pos + 1, pos + 3), 16);
						tmp.append(ch);
						lastPos = pos + 3;
					}
			   } else {
				   if (pos == -1) {
					   tmp.append(text.substring(lastPos));
					   lastPos = text.length();
				   } else {
					   tmp.append(text.substring(lastPos, pos));
					   lastPos = pos;
				   }
			   }
			}
		}catch(Exception ex){
			return text;
		}
		return tmp.toString();
	}
	
	/**
	 * 获取字符串出现次数
	 * @param srcText 源字符串
	 * @param findText 查找字符串
	 * @return
	 */
	public static int appearNumber(String srcText, String findText) { 
		  int count = 0; 
		  Pattern p = Pattern.compile(findText); 
		  Matcher m = p.matcher(srcText); 
		  while (m.find()) { 
		    count++; 
		  } 
		  return count; 
	} 
	
	/**
	 * 是否是科学计数法
	 */
	public static boolean isScience(Object object){
		if (object == null) return false;
		if (object instanceof Integer
				|| object instanceof BigInteger
				|| object instanceof Long
				|| object instanceof Double
				|| object instanceof BigDecimal
				) {
			String value = String.valueOf(object);
			return value.contains("E") || value.contains("e");
		}else if (object instanceof String){
			return Pattern.compile("^[+-]?\\d+\\.?\\d*[Ee][+-]?\\d+$").matcher((String)object).find();
		}
		return false;
	}
	
	/**
	 * 准确地拆分字符串（即最后的空字符将被保留，有n个分割,就有n+1个分组）
	 * @param str
	 * @param split
	 * @return
	 */
	public static String[] splitEx(String str, String split){
		if (str == null) return null;
		return str.split(split, -1);
	}
	
	public static Object scienceParse(Object object){
		if (!isScience(object)) return object;
		if (object instanceof Integer
				|| object instanceof BigInteger
				|| object instanceof Long) {
			BigDecimal db = new BigDecimal(String.valueOf(object));
			return db.toPlainString();
		}else if (object instanceof Double) {
			BigDecimal db = new BigDecimal(StringUtil.toFixed((Double)object, "F2"));
			return db.toPlainString();
		}else if (object instanceof BigDecimal) {
			BigDecimal db = new BigDecimal(StringUtil.toFixed(((BigDecimal)object).doubleValue(), "F2"));
			return db.toPlainString();
		}else{
			return object;
		}
	}
	
	/**
	 * 千分位显示
	 */
	public static String thousands(Object money){//千分位
		if (money == null) return "0.00";
		double m = doubleParse(money);
		DecimalFormat df=new DecimalFormat(",###,##0.00"); //保留二位小数  
        return df.format(m);
	}

	/**
	 * 将字符串显示为遮罩模式
	 * @param str
	 * @param frontShow 显示前n个字符
	 * @param minChars 最小显示位数,-1时显示为原数量
	 * @return
	 */
	public static String mask(String str,int frontShow,int minChars){
		String ret = "";
		if (str == null) str = "";
		int length = str.length();
		int frontLength = frontShow <= length ? frontShow : length;
		for(int i = 0 ; i < frontLength ; i++){
			ret += str.substring(i, i + 1);
		}
		int leftLength = 0;
		if(minChars < 0){
			if (length > frontLength)leftLength = length - frontLength;
		}else{
			if (minChars > frontLength)leftLength = minChars - frontLength;
		}
		for(int i = 0 ; i < leftLength ; i++){
			ret += "*";
		}
		return ret;
	}
	public static String inverse(String str) {
		String afterInverse = "";
		for (int i = 0; i < str.length(); i = i + 2) {
			String temp = str.substring(i, i + 2);
			afterInverse = temp + afterInverse;
		}

		return afterInverse;
	}
}
