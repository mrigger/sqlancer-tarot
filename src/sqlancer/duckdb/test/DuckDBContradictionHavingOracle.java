package sqlancer.duckdb.test;

import java.sql.SQLException;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.common.ast.newast.NewBinaryOperatorNode;
import sqlancer.duckdb.DuckDBErrors;
import sqlancer.duckdb.DuckDBToStringVisitor;
import sqlancer.duckdb.DuckDBProvider.DuckDBGlobalState;
import sqlancer.duckdb.ast.DuckDBExpression;
import sqlancer.duckdb.gen.DuckDBExpressionGenerator.DuckDBBinaryLogicalOperator;

public class DuckDBContradictionHavingOracle extends DuckDBQueryPartitioningBase {

    public DuckDBContradictionHavingOracle(DuckDBGlobalState state) {
        super(state);
        DuckDBErrors.addGroupByErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression());
        }
        boolean orderBy = Randomly.getBoolean();
        if (orderBy) {
            select.setOrderByExpressions(gen.generateOrderBys());
        }
        select.setGroupByExpressions(gen.generateExpressions(Randomly.smallNumber() + 1));
        select.setFetchColumns(select.getGroupByExpressions());
        NewBinaryOperatorNode<DuckDBExpression> or = new NewBinaryOperatorNode<>(predicate, negatedPredicate,
                DuckDBBinaryLogicalOperator.AND);
        select.setHavingClause(or);
        String s = DuckDBToStringVisitor.asString(select);
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(s, errors, state);
        if (!resultSet.isEmpty()) {
            throw new AssertionError(s + " " + resultSet);
        }

    }

}
