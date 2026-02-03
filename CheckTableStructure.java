import java.sql.*;

public class CheckTableStructure {
    public static void main(String[] args) {
        String url = "'" + $jdbcUrl + "'";
        try (Connection conn = DriverManager.getConnection(url)) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "survey_question", null);
            System.out.println("survey_question表结构:");
            System.out.println("列名, 数据类型, 是否为空");
            while (columns.next()) {
                String columnName = columns.getString(4);
                String columnType = columns.getString(6);
                int nullable = columns.getInt(11);
                String isNullable = (nullable == 0) ? "NOT NULL" : "NULL";
                System.out.printf("%s, %s, %s%n", columnName, columnType, isNullable);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
