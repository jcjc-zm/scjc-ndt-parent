package com.scjc.ndt.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scjc.ndt.entity.ReportTemplate;
import com.scjc.ndt.mapper.ReportTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ReportTemplateMapper templateMapper;

    private static final String TEMPLATE_NAME = "胶片射线报告A";

    private static final String LAYOUT_JSON =
        "{" +
        "\"pageSize\":\"A4\"," +
        "\"pages\":[" +

        // ═══ 首页 WJ-11 ═══
        "{" +
        "\"name\":\"首页\"," +
        "\"sections\":[" +

        "{\"type\":\"header\"," +
        "\"text\":\"射线检测报告\"," +
        "\"subtext\":\"报告编号：{reportNo}    共    页  第    页\"," +
        "\"style\":{\"fontSize\":20,\"fontWeight\":\"bold\"}}," +

        "{\"type\":\"table\"," +
        "\"label\":\"工程及检测信息\"," +
        "\"rows\":[" +
        "[{\"label\":\"工程名称\",\"bind\":\"projectName\"},{\"label\":\"工程编号\",\"bind\":\"projectCode\"}]," +
        "[{\"label\":\"单位工程名称\",\"bind\":\"unitProjectName\"},{\"label\":\"施工单位\",\"bind\":\"constructionUnit\"}]," +
        "[{\"label\":\"检测项目\",\"bind\":\"inspectionItem\"},{\"label\":\"原始记录编号\",\"bind\":\"instructionNo\"}]," +
        "[{\"label\":\"材质规格\",\"bind\":\"specification\"},{\"label\":\"承压设备类别\",\"bind\":\"pressureEquipmentCategory\"}]," +
        "[{\"label\":\"坡口形式\",\"bind\":\"grooveType\"},{\"label\":\"热处理状态\",\"bind\":\"heatTreatmentStatus\"}]," +
        "[{\"label\":\"焊接方法\",\"bind\":\"weldingMethod\"},{\"label\":\"检测时机\",\"bind\":\"inspectionTiming\"}]" +
        "]}," +

        "{\"type\":\"table\"," +
        "\"label\":\"RT技术参数\"," +
        "\"rows\":[" +
        "[{\"label\":\"胶片型号\",\"bind\":\"filmModel\"},{\"label\":\"胶片规格\",\"bind\":\"filmSpec\"},{\"label\":\"铅增感屏\",\"bind\":\"leadScreen\"},{\"label\":\"像质计型号\",\"bind\":\"iqiModel\"}]," +
        "[{\"label\":\"像质计位置\",\"bind\":\"iqiPosition\"},{\"label\":\"要求达到像质指数\",\"bind\":\"requiredIqi\"}]," +
        "[{\"label\":\"源的种类\",\"bind\":\"sourceType\"},{\"label\":\"设备型号\",\"bind\":\"equipmentModel\"},{\"label\":\"焦点尺寸／焦距\",\"bind\":\"focalDistance\"}]," +
        "[{\"label\":\"管电压\",\"bind\":\"tubeVoltage\"},{\"label\":\"管电流\",\"bind\":\"tubeCurrent\"},{\"label\":\"曝光时间\",\"bind\":\"exposureTime\"}]," +
        "[{\"label\":\"透照方式\",\"bind\":\"techniqueType\",\"colspan\":4}]," +
        "[{\"label\":\"胶片处理\",\"bind\":\"filmProcessing\"},{\"label\":\"显影时间\",\"bind\":\"developmentTime\"},{\"label\":\"显影温度\",\"bind\":\"developmentTemperature\"},{\"label\":\"底片黑度范围\",\"bind\":\"filmDensityRange\"}]" +
        "]}," +

        "{\"type\":\"table\"," +
        "\"label\":\"检测结果统计\"," +
        "\"rows\":[" +
        "[{\"label\":\"检测技术等级\",\"bind\":\"inspectionTechLevel\"},{\"label\":\"检测比例\",\"bind\":\"ratio\"},{\"label\":\"检测标准\",\"bind\":\"inspectionStandard\"},{\"label\":\"合格级别\",\"bind\":\"qualifiedLevel\"}]," +
        "[{\"label\":\"检测数量（道）\",\"bind\":\"inspectionCount\"},{\"label\":\"返修数量（道）\",\"bind\":\"repairCount\"},{\"label\":\"一次焊接合格率\",\"bind\":\"firstPassYield\"}]," +
        "[{\"label\":\"复探数量（道）\",\"bind\":\"reinspectionCount\"},{\"label\":\"扩探数量（道）\",\"bind\":\"extendedInspectionCount\"},{\"label\":\"最终焊接合格率\",\"bind\":\"finalYield\"}]" +
        "]}," +

        "{\"type\":\"image-area\",\"label\":\"检测部位示意图\"}," +

        "{\"type\":\"text\"," +
        "\"content\":\"检测结论：检测 ____ 道，合格 ____ 道，不合格 ____ 道。\"}," +

        "{\"type\":\"signature\"," +
        "\"signatories\":[" +
        "{\"label\":\"报告人\",\"bind\":\"inspectorName\"}," +
        "{\"label\":\"审核人\",\"bind\":\"reviewerName\"}," +
        "{\"label\":\"检测单位项目技术负责人\",\"bind\":\"technicalLeadName\"}" +
        "]}" +

        "]}," +

        // ═══ 附页 WJ-12 ═══
        "{" +
        "\"name\":\"附页\"," +
        "\"sections\":[" +

        "{\"type\":\"header\"," +
        "\"text\":\"射线检测报告（附页）\"," +
        "\"subtext\":\"报告编号：{reportNo}    共    页  第    页\"}," +

        "{\"type\":\"table\"," +
        "\"label\":\"焊缝射线检测明细\"," +
        "\"headerRows\":[" +
        "[{\"label\":\"单位工程名称\",\"bind\":\"unitProjectName\",\"colspan\":4}," +
        "{\"label\":\"检测项目\",\"bind\":\"inspectionItem\",\"colspan\":5}]," +
        "[{\"label\":\"序号\",\"rowspan\":2}," +
        "{\"label\":\"焊缝编号及底片编号\",\"rowspan\":2}," +
        "{\"label\":\"板厚（mm）\",\"rowspan\":2}," +
        "{\"label\":\"一次透照长度（mm）\",\"rowspan\":2}," +
        "{\"label\":\"像质指数\",\"rowspan\":2}," +
        "{\"label\":\"缺陷情况\",\"colspan\":2}," +
        "{\"label\":\"评定级别\",\"rowspan\":2}," +
        "{\"label\":\"备注\",\"rowspan\":2}]," +
        "[{\"label\":\"性质及长度（mm）\"},{\"label\":\"位置（mm）\"}]" +
        "]," +
        "\"columns\":[" +
        "{\"label\":\"序号\",\"field\":\"seq\",\"width\":50}," +
        "{\"label\":\"焊缝编号及底片编号\",\"field\":\"weldNo\",\"width\":160}," +
        "{\"label\":\"板厚（mm）\",\"field\":\"plateThickness\",\"width\":80}," +
        "{\"label\":\"一次透照长度（mm）\",\"field\":\"transilluminationLength\",\"width\":100}," +
        "{\"label\":\"像质指数\",\"field\":\"iqiValue\",\"width\":80}," +
        "{\"label\":\"性质及长度（mm）\",\"field\":\"reportDefectNature\",\"width\":120}," +
        "{\"label\":\"位置（mm）\",\"field\":\"reportDefectPosition\",\"width\":80}," +
        "{\"label\":\"评定级别\",\"field\":\"resultLevel\",\"width\":80}," +
        "{\"label\":\"备注\",\"field\":\"remark\",\"width\":100}" +
        "]," +
        "\"dataRows\":20" +
        "}," +

        "{\"type\":\"signature\"," +
        "\"signatories\":[" +
        "{\"label\":\"评片\",\"bind\":\"inspectorName\"}," +
        "{\"label\":\"审核\",\"bind\":\"reviewerName\"}" +
        "]}" +

        "]}" +

        "]" +
        "}";

    @Override
    public void run(String... args) {
        try {
            seedTemplate();
        } catch (Exception e) {
            log.error("系统模板播种失败: {}", e.getMessage(), e);
        }
    }

    private void seedTemplate() {
        templateMapper.delete(
            new LambdaQueryWrapper<ReportTemplate>()
                .eq(ReportTemplate::getTemplateName, TEMPLATE_NAME)
                .eq(ReportTemplate::getTemplateType, "SYSTEM")
        );

        log.info("播种系统模板「{}」...", TEMPLATE_NAME);

        // 验证 JSON 合法性
        try {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(LAYOUT_JSON);
            log.info("JSON 格式验证通过, 长度={}", LAYOUT_JSON.length());
        } catch (Exception e) {
            log.error("JSON 格式错误: {}", e.getMessage());
            log.error("JSON 前 200 字符: {}", LAYOUT_JSON.substring(0, Math.min(200, LAYOUT_JSON.length())));
            return;
        }

        ReportTemplate t = new ReportTemplate();
        t.setTemplateName(TEMPLATE_NAME);
        t.setTemplateType("SYSTEM");
        t.setMethodType("RT");
        t.setDescription("基于 WJ-11 首页 + WJ-12 附页标准格式。"
            + "包含工程信息、RT参数、透照参数、底片评定、签字区及附页焊缝明细");
        t.setLayoutConfig(LAYOUT_JSON);
        t.setCreateBy(1L);
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(t);

        log.info("✓ 系统模板「{}」播种成功 (id={})", TEMPLATE_NAME, t.getId());
    }
}