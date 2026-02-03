# 脚本用于运行光疗后端项目

# 设置Maven本地仓库路径
$mavenRepo = "$env:USERPROFILE\.m2\repository"

# 设置项目路径
$projectPath = "$PSScriptRoot"

# 定义需要的依赖项
$dependencies = @(
    "org/springframework/boot/spring-boot-starter-web/2.7.18/spring-boot-starter-web-2.7.18.jar",
    "org/springframework/boot/spring-boot-starter-validation/2.7.18/spring-boot-starter-validation-2.7.18.jar",
    "org/springframework/boot/spring-boot-starter-data-jpa/2.7.18/spring-boot-starter-data-jpa-2.7.18.jar",
    "org/springframework/boot/spring-boot-starter-security/2.7.18/spring-boot-starter-security-2.7.18.jar",
    "org/springframework/spring-tx/5.3.30/spring-tx-5.3.30.jar",
    "org/flywaydb/flyway-core/9.22.3/flyway-core-9.22.3.jar",
    "org/flywaydb/flyway-mysql/9.22.3/flyway-mysql-9.22.3.jar",
    "mysql/mysql-connector-java/8.0.33/mysql-connector-java-8.0.33.jar",
    "log4j/log4j/1.2.17/log4j-1.2.17.jar",
    "io/jsonwebtoken/jjwt-api/0.11.5/jjwt-api-0.11.5.jar",
    "io/jsonwebtoken/jjwt-impl/0.11.5/jjwt-impl-0.11.5.jar",
    "io/jsonwebtoken/jjwt-jackson/0.11.5/jjwt-jackson-0.11.5.jar",
    "org/springdoc/springdoc-openapi-ui/1.7.0/springdoc-openapi-ui-1.7.0.jar",
    "org/projectlombok/lombok/1.18.30/lombok-1.18.30.jar"
)

# 构建类路径
$classpath = "$projectPath\target\classes"
foreach ($dep in $dependencies) {
    $depPath = Join-Path -Path $mavenRepo -ChildPath $dep
    if (Test-Path $depPath) {
        $classpath += ";$depPath"
    } else {
        Write-Warning "依赖项未找到: $depPath"
    }
}

# 运行项目
Write-Host "正在运行光疗后端项目..."
Write-Host "类路径: $classpath"
java -cp $classpath com.lontri.lighttherapy.LightTherapyApplication