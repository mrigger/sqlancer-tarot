package sqlancer.sqlite3.gen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sqlancer.Query;
import sqlancer.QueryAdapter;
import sqlancer.Randomly;
import sqlancer.sqlite3.SQLite3Provider.SQLite3GlobalState;
import sqlancer.sqlite3.schema.SQLite3Schema.SQLite3Column;

public final class SQLite3CreateVirtualRtreeTabelGenerator {

    private SQLite3CreateVirtualRtreeTabelGenerator() {
    }

    public static Query createTableStatement(String rTreeTableName, SQLite3GlobalState globalState) {
        Set<String> errors = new HashSet<>();
        List<SQLite3Column> columns = new ArrayList<>();
        StringBuilder sb = new StringBuilder("CREATE VIRTUAL TABLE ");
        sb.append(rTreeTableName);
        sb.append(" USING ");
        sb.append(Randomly.fromOptions("rtree_i32", "rtree"));
        sb.append("(");
        int size = 3 + Randomly.smallNumber();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            SQLite3Column c = SQLite3Common.createColumn(i);
            columns.add(c);
            sb.append(c.getName());
        }
        for (int i = 0; i < Randomly.smallNumber(); i++) {
            sb.append(", ");
            sb.append("+");
            String columnName = SQLite3Common.createColumnName(size + i);
            SQLite3ColumnBuilder columnBuilder = new SQLite3ColumnBuilder().allowPrimaryKey(false).allowNotNull(false)
                    .allowUnique(false).allowCheck(false);
            String c = columnBuilder.createColumn(columnName, globalState, columns);
            sb.append(c);
            sb.append(" ");
        }
        errors.add("virtual tables cannot use computed columns");
        sb.append(")");

        errors.add("Wrong number of columns for an rtree table");
        errors.add("Too many columns for an rtree table");
        return new QueryAdapter(sb.toString(), errors);
    }

}
