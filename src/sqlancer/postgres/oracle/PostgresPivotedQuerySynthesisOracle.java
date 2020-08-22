package sqlancer.postgres.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.postgresql.util.PSQLException;

import sqlancer.Main.StateLogger;
import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.common.oracle.PivotedQuerySynthesisBase;
import sqlancer.common.query.Query;
import sqlancer.common.query.QueryAdapter;
import sqlancer.postgres.PostgresGlobalState;
import sqlancer.postgres.PostgresSchema.PostgresColumn;
import sqlancer.postgres.PostgresSchema.PostgresRowValue;
import sqlancer.postgres.PostgresSchema.PostgresTables;
import sqlancer.postgres.PostgresToStringVisitor;
import sqlancer.postgres.PostgresVisitor;
import sqlancer.postgres.ast.PostgresColumnValue;
import sqlancer.postgres.ast.PostgresConstant;
import sqlancer.postgres.ast.PostgresExpression;
import sqlancer.postgres.ast.PostgresSelect;
import sqlancer.postgres.ast.PostgresSelect.PostgresFromTable;
import sqlancer.postgres.gen.PostgresExpressionGenerator;

public class PostgresPivotedQuerySynthesisOracle
        extends PivotedQuerySynthesisBase<PostgresGlobalState, PostgresRowValue, PostgresExpression> {

    private List<PostgresColumn> fetchColumns;
    private final MainOptions options;
    private final StateLogger logger;

    public PostgresPivotedQuerySynthesisOracle(PostgresGlobalState globalState) throws SQLException {
        super(globalState);
        options = globalState.getOptions();
        logger = globalState.getLogger();
    }

    @Override
    public Query getQueryThatContainsAtLeastOneRow() throws SQLException {
        PostgresTables randomFromTables = globalState.getSchema().getRandomTableNonEmptyTables();

        PostgresSelect selectStatement = new PostgresSelect();
        selectStatement.setSelectType(Randomly.fromOptions(PostgresSelect.SelectType.values()));
        List<PostgresColumn> columns = randomFromTables.getColumns();
        pivotRow = randomFromTables.getRandomRowValue(globalState.getConnection());

        fetchColumns = columns;
        selectStatement.setFromList(randomFromTables.getTables().stream().map(t -> new PostgresFromTable(t, false))
                .collect(Collectors.toList()));
        selectStatement.setFetchColumns(fetchColumns.stream()
                .map(c -> new PostgresColumnValue(c, pivotRow.getValues().get(c))).collect(Collectors.toList()));
        PostgresExpression whereClause = generateWhereClauseThatContainsRowValue(columns, pivotRow);
        selectStatement.setWhereClause(whereClause);
        List<PostgresExpression> groupByClause = generateGroupByClause(columns, pivotRow);
        selectStatement.setGroupByExpressions(groupByClause);
        PostgresExpression limitClause = generateLimit();
        selectStatement.setLimitClause(limitClause);
        if (limitClause != null) {
            PostgresExpression offsetClause = generateOffset();
            selectStatement.setOffsetClause(offsetClause);
        }
        List<PostgresExpression> orderBy = new PostgresExpressionGenerator(globalState).setColumns(columns)
                .generateOrderBy();
        selectStatement.setOrderByExpressions(orderBy);

        StringBuilder sb2 = new StringBuilder();
        sb2.append("SELECT * FROM (SELECT 1 FROM ");
        sb2.append(randomFromTables.tableNamesAsString());
        sb2.append(" WHERE ");
        int i = 0;
        for (PostgresColumn c : fetchColumns) {
            if (i++ != 0) {
                sb2.append(" AND ");
            }
            sb2.append(c.getFullQualifiedName());
            if (pivotRow.getValues().get(c).isNull()) {
                sb2.append(" IS NULL");
            } else {
                sb2.append(" = ");
                sb2.append(pivotRow.getValues().get(c).getTextRepresentation());
            }
        }
        sb2.append(") as result;");

        PostgresToStringVisitor visitor = new PostgresToStringVisitor();
        visitor.visit(selectStatement);
        return new QueryAdapter(visitor.get());
    }

    private List<PostgresExpression> generateGroupByClause(List<PostgresColumn> columns, PostgresRowValue rw) {
        if (Randomly.getBoolean()) {
            return columns.stream().map(c -> PostgresColumnValue.create(c, rw.getValues().get(c)))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private PostgresConstant generateLimit() {
        if (Randomly.getBoolean()) {
            return PostgresConstant.createIntConstant(Integer.MAX_VALUE);
        } else {
            return null;
        }
    }

    private PostgresExpression generateOffset() {
        if (Randomly.getBoolean()) {
            // OFFSET 0
            return PostgresConstant.createIntConstant(0);
        } else {
            return null;
        }
    }

    private PostgresExpression generateWhereClauseThatContainsRowValue(List<PostgresColumn> columns,
            PostgresRowValue rw) {
        return PostgresExpressionGenerator.generateTrueCondition(columns, rw, globalState);
    }

    @Override
    protected boolean isContainedIn(Query query) throws SQLException {
        Statement createStatement;
        createStatement = globalState.getConnection().createStatement();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ("); // ANOTHER SELECT TO USE ORDER BY without restrictions
        sb.append(query.getQueryString());
        sb.append(") as result WHERE ");
        int i = 0;
        for (PostgresColumn c : fetchColumns) {
            if (i++ != 0) {
                sb.append(" AND ");
            }
            sb.append("result.");
            sb.append(c.getTable().getName());
            sb.append(c.getName());
            if (pivotRow.getValues().get(c).isNull()) {
                sb.append(" IS NULL");
            } else {
                sb.append(" = ");
                sb.append(pivotRow.getValues().get(c).getTextRepresentation());
            }
        }
        String resultingQueryString = sb.toString();
        // log both SELECT queries at the bottom of the error log file
        if (options.logEachSelect()) {
            logger.writeCurrent(resultingQueryString);
        }
        try (ResultSet result = createStatement.executeQuery(resultingQueryString)) {
            boolean isContainedIn = result.next();
            createStatement.close();
            return isContainedIn;
        } catch (PSQLException e) {
            if (e.getMessage().contains("out of range") || e.getMessage().contains("cannot cast")
                    || e.getMessage().contains("invalid input syntax for ") || e.getMessage().contains("must be type")
                    || e.getMessage().contains("operator does not exist")
                    || e.getMessage().contains("Could not choose a best candidate function.")
                    || e.getMessage().contains("division by zero")
                    || e.getMessage().contains("zero raised to a negative power is undefined")
                    || e.getMessage().contains("canceling statement due to statement timeout")
                    || e.getMessage().contains("operator is not unique")
                    || e.getMessage().contains("could not determine which collation to use for string comparison")) {
                return true;
            } else {
                throw e;
            }
        }
    }

    @Override
    protected String asString(PostgresExpression expr) {
        return PostgresVisitor.asString(expr);
    }

}
