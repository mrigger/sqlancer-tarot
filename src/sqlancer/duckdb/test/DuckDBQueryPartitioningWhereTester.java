package sqlancer.duckdb.test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.Main;
import sqlancer.Randomly;
import sqlancer.common.ast.newast.Node;
import sqlancer.duckdb.DuckDBErrors;
import sqlancer.duckdb.DuckDBProvider.DuckDBGlobalState;
import sqlancer.duckdb.DuckDBToStringVisitor;
import sqlancer.duckdb.ast.DuckDBExpression;
import sqlancer.duckdb.gen.DuckDBExpressionListGenerator;

public class DuckDBQueryPartitioningWhereTester extends DuckDBQueryPartitioningBase {

    public DuckDBQueryPartitioningWhereTester(DuckDBGlobalState state) {
        super(state);
        DuckDBErrors.addGroupByErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        select.setWhereClause(null);
        String originalQueryString = DuckDBToStringVisitor.asString(select);

        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        boolean orderBy = Randomly.getBooleanWithRatherLowProbability();
        if (orderBy) {
            select.setOrderByExpressions(gen.generateOrderBys());
        }
        DuckDBExpressionListGenerator expressionListGenerator = new DuckDBExpressionListGenerator(state,
                targetTables.getColumns());
        List<Node<DuckDBExpression>> expressions = expressionListGenerator.generateAllExpressions();
        for (Node<DuckDBExpression> predicate : expressions) {
            Main.nrQueries.addAndGet(1);
            select.setWhereClause(predicate);
            String firstQueryString = DuckDBToStringVisitor.asString(select);
            select.setWhereClause(gen.negatePredicate(predicate));
            String secondQueryString = DuckDBToStringVisitor.asString(select);
            select.setWhereClause(gen.isNull(predicate));
            String thirdQueryString = DuckDBToStringVisitor.asString(select);
            List<String> combinedString = new ArrayList<>();
            List<String> secondResultSet = ComparatorHelper.getCombinedResultSet(firstQueryString, secondQueryString,
                    thirdQueryString, combinedString, !orderBy, state, errors);
            ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                    state, ComparatorHelper::canonicalizeResultValue);
            Main.nrSuccessfulActions.addAndGet(1);
        }
    }

}
