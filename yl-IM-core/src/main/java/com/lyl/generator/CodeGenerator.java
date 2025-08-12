package com.lyl.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;

public class CodeGenerator {
    public static void main(String[] args) {
        String dbUrl = "jdbc:mysql://localhost:13306/yl_im?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false";
        //数据库的用户
        String dbUser = "root";
        //数据库的密码
        String dbPassword = "123456";
        //目标的表名
        String[] dbTableName = new String[]{
                "test"
        };
        // 依次输入数据库地址、数据库用户名、数据库密码、作者、表（可以是多个）,注意：重复执行会覆盖
        generator(dbUrl, dbUser, dbPassword, dbTableName);
    }

    private static void generator(String dataBaseUrl, String userName, String password, String... tables) {
        String projectPath = System.getProperty("user.dir");
        String mapperXmlPath = projectPath + "/yl-IM-core/src/main/resources/mybatis/mapper/";

        FastAutoGenerator.create(dataBaseUrl, userName, password)
                // 全局配置
                .globalConfig(builder -> {
                    builder.outputDir(projectPath + "/yl-IM-core/src/main/java") // 设置输出目录
                            .disableOpenDir();   // 禁止打开输出目录
                })
                // 包配置
                .packageConfig(builder -> {
                    builder.parent("com.lyl") // 设置父包名
                            .entity("domain")  // 实体类包名
                            .pathInfo(Collections.singletonMap(OutputFile.xml, mapperXmlPath)); // 设置mapperXml生成路径
                })
                // 策略配置
                .strategyConfig(builder -> {
                    builder.addInclude(tables) // 设置需要生成的表名
                            .entityBuilder()   // 实体类策略配置
                            .enableLombok()    // 开启 lombok 模型
                            .enableTableFieldAnnotation() // 开启生成实体时生成字段注解
                            .logicDeleteColumnName("deleted") // 逻辑删除字段名
                            .naming(NamingStrategy.underline_to_camel)  // 数据库表映射到实体的命名策略
                            .columnNaming(NamingStrategy.underline_to_camel) // 数据库表字段映射到实体的命名策略
                            .superClass("com.lyl.domain.po.BasePO") // 设置父类
                            .addSuperEntityColumns("id", "create_time", "update_time") // 添加父类公共字段
                            .serviceBuilder()  // Service策略配置
                            .formatServiceFileName("I%sService") // 设置 service 接口文件名称
                            .formatServiceImplFileName("%sServiceImpl") // 设置 service 实现类文件名称
                            .mapperBuilder()   // Mapper策略配置
                            .formatMapperFileName("%sMapper") // 设置 mapper 接口文件名称
                            .formatXmlFileName("%sMapper"); // 设置 xml 文件名称
                })
                .templateEngine(new VelocityTemplateEngine()) // 使用Velocity引擎模板
                .execute();
    }
}
