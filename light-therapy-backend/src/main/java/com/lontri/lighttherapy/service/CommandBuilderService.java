package com.lontri.lighttherapy.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.device.codec.CommandParamTransform;
import com.lontri.lighttherapy.device.template.CommandTemplate;
import com.lontri.lighttherapy.device.template.CommandTemplateRepository;
import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.enums.DeviceType;
import com.lontri.lighttherapy.repository.DeviceRepository;

@Service
public class CommandBuilderService {

    private final DeviceRepository deviceRepo;
    private final CommandTemplateRepository templateRepo;

    public CommandBuilderService(DeviceRepository deviceRepo, CommandTemplateRepository templateRepo) {
        this.deviceRepo = deviceRepo;
        this.templateRepo = templateRepo;
    }

    /** 生成 DIM + CCT 指令（_485设备类型会生成一条结合指令） */
    public BuiltCommands buildDimCctCommands(String deviceSn, Integer dimP, Integer sumDimP, Integer skyDimP, Integer cctK, Integer skycctK) {
        Device device = deviceRepo.findByDeviceSn(deviceSn)
                .orElseThrow(() -> new BizException(40421, "device not found", HttpStatus.NOT_FOUND));

        DeviceType deviceType = device.getDeviceType();
        Map<String, Object> values = new HashMap<>();
        values.put("dimP", dimP);
        values.put("cctK", cctK);
        
        // 如果是485/MCB设备，添加天空光相关参数
        if (DeviceType._485 == deviceType || DeviceType.MCB == deviceType) {
            values.put("dim_sum", sumDimP);
            values.put("dim_sky", skyDimP);
            values.put("cct_sky", skycctK);
        }

        // 485/MCB设备类型特殊处理：生成一条结合亮度和色温的指令
//        if (DeviceType._485 == deviceType || DeviceType.MCB == deviceType) {
//            String combinedCmd = buildSingle(deviceType.name(), "DIM_CCT", values);
//            // 将同一条指令赋值给dim和cct，保持接口兼容性
//            return new BuiltCommands(null, null, combinedCmd);
//        }

        // 其他设备类型：生成独立指令
        String dimCmd = null;
        String cctCmd = null;
        String dimcctCmd = null;
        
        if ((DeviceType._485 != deviceType && DeviceType.MCB != deviceType) && dimP != null) {
            dimCmd = buildSingle(deviceType.name(), "DIM", values);
        }
        
        if ((DeviceType._485 != deviceType && DeviceType.MCB != deviceType) && cctK != null) {
            cctCmd = buildSingle(deviceType.name(), "CCT", values);
        }

        // 对于非485/MCB设备，如果提供了足够的参数，也生成DIM_CCT指令
        if ((DeviceType._485 == deviceType || DeviceType.MCB == deviceType) && sumDimP != null && skyDimP != null && skycctK != null) {
            dimcctCmd = buildSingle(deviceType.name(), "DIM_CCT", values);
        }
        return new BuiltCommands(dimCmd, cctCmd, dimcctCmd);
    }

    /** 根据 deviceType + functionCode + values 渲染单条指令 */
    public String buildSingle(String deviceType, String functionCode, Map<String, Object> values) {
        CommandTemplate tpl = templateRepo
                .findByDeviceTypeAndFunctionCodeAndEnabled(deviceType, functionCode, true)
                .orElseThrow(() -> new BizException(40422,
                        "command template not found: deviceType=" + deviceType + ", func=" + functionCode,
                        HttpStatus.NOT_FOUND));

        String cmd = tpl.getTemplate();

        // 处理亮度百分比相关的占位符
        if (cmd.contains("{dim_hex}") || cmd.contains("{dim_p}") || cmd.contains("{dim_value}") || cmd.contains("{dim_sum}") || cmd.contains("{dim_sky}")) {
            int dimP = toInt(values.get("dimP"), 0);
            if (cmd.contains("{dim_hex}")) {
                cmd = cmd.replace("{dim_hex}", CommandParamTransform.percentToDaliHex(dimP));
            }
            if (cmd.contains("{dim_p}")) {
                cmd = cmd.replace("{dim_p}", CommandParamTransform.percentToDaliHex(dimP));
            }
            if (cmd.contains("{dim_value}")) {
                // 直接使用百分比值（可根据需要添加转换逻辑）
                cmd = cmd.replace("{dim_value}", CommandParamTransform.percentToDaliHex(dimP));
            }
            if (cmd.contains("{dim_sum}")) {
                // 485设备专用：直接使用百分比值
                int sumDimP = toInt(values.get("dim_sum"), 0);
                cmd = cmd.replace("{dim_sum}", CommandParamTransform.percentToDaliHex(sumDimP));
            }
            if (cmd.contains("{dim_sky}")) {
                // 485设备专用：使用专门的天空光亮度值
                int skyDimP = toInt(values.get("dim_sky"), 0);
                cmd = cmd.replace("{dim_sky}", CommandParamTransform.percentToDaliHex(skyDimP));
            }
        }

        // 处理色温相关的占位符
        if ((cmd.contains("{cct_payload}") || cmd.contains("{cct_k}") || cmd.contains("{CCT_K}") || cmd.contains("{cct_value}")) && (values.get("cctK") != null || values.get("cct_sky") != null)) {
            // 主色温值（用于普通设备）
            int cctK = toInt(values.get("cctK"), 2700);
            // 天空光色温值（用于485/MCB设备）
            int skyCctK = toInt(values.get("cct_sky"), 2700);
            
            if (cctK > 0) { // 确保cctK大于0，避免转换异常
                if (cmd.contains("{cct_payload}")) {
                    cmd = cmd.replace("{cct_payload}", CommandParamTransform.cctKToDt8Payload(cctK));
                }
                if (cmd.contains("{cct_value}")) {
                    // 直接使用色温值（可根据需要添加转换逻辑）
                    cmd = cmd.replace("{cct_value}", CommandParamTransform.cctKToDt8(cctK));
                }
            }
        
            // 处理小写的{cct_k}占位符（用于天空光色温）
            if (cmd.contains("{cct_k}")) {
                cmd = cmd.replace("{cct_k}", CommandParamTransform.cctKToDt8(skyCctK));
            }
            
            // 处理大写的{CCT_K}占位符（用于主色温）
            if (cmd.contains("{CCT_K}")) {
                cmd = cmd.replace("{CCT_K}", CommandParamTransform.cctKToDt8(skyCctK));
            }
        }

        return cmd.toUpperCase();
    }

    private int toInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Long) return ((Long) v).intValue();
        if (v instanceof String) return Integer.parseInt((String) v);
        throw new IllegalArgumentException("cannot convert to int: " + v);
    }

    /** Java 8 compatible */
    public static class BuiltCommands {
        private final String dimCommand;
        private final String cctCommand;
        private final String dimcctCommand;

        public BuiltCommands(String dimCommand, String cctCommand, String dimcctCommand) {
            this.dimCommand = dimCommand;
            this.cctCommand = cctCommand;
            this.dimcctCommand = dimcctCommand;
        }

        public String getDimCommand() {
            return dimCommand;
        }

        public String getCctCommand() {
            return cctCommand;
        }
        public String getDimcctCommand() {
            return dimcctCommand;
        }
    }
}