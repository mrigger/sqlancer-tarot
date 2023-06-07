package sqlancer.duckdb.test;

import java.sql.SQLException;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.common.ast.newast.NewBinaryOperatorNode;
import sqlancer.duckdb.DuckDBProvider.DuckDBGlobalState;
import sqlancer.duckdb.DuckDBToStringVisitor;
import sqlancer.duckdb.ast.DuckDBExpression;
import sqlancer.duckdb.gen.DuckDBExpressionGenerator.DuckDBBinaryLogicalOperator;

public class DuckDBContradictionWhereOracle extends DuckDBQueryPartitioningBase {

    public DuckDBContradictionWhereOracle(DuckDBGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        NewBinaryOperatorNode<DuckDBExpression> or = new NewBinaryOperatorNode<>(predicate, negatedPredicate,
                DuckDBBinaryLogicalOperator.AND);
        select.setWhereClause(or);
        String s = DuckDBToStringVisitor.asString(select);
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(s, errors, state);
        if (!resultSet.isEmpty()) {
            throw new AssertionError(s + " " + resultSet);
        }

    }

}
