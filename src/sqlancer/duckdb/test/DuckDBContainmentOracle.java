package sqlancer.duckdb.test;

import java.sql.SQLException;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.common.ast.newast.NewBinaryOperatorNode;
import sqlancer.duckdb.DuckDBToStringVisitor;
import sqlancer.duckdb.DuckDBProvider.DuckDBGlobalState;
import sqlancer.duckdb.ast.DuckDBExpression;
import sqlancer.duckdb.gen.DuckDBExpressionGenerator.DuckDBBinaryLogicalOperator;

public class DuckDBContainmentOracle extends DuckDBQueryPartitioningBase {

    public DuckDBContainmentOracle(DuckDBGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        // SELECT * FROM t0 WHERE c0 > 5;
        select.setWhereClause(predicate);
        // SELECT * FROM t0 WHERE c0 > 5 AND c1=1
        String originalQuery = DuckDBToStringVisitor.asString(select);
        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQuery, errors, state);
        select.setWhereClause(new NewBinaryOperatorNode<DuckDBExpression>(predicate, gen.generatePredicate(),
                DuckDBBinaryLogicalOperator.AND));
        String restrictiveQuery = DuckDBToStringVisitor.asString(select);
        List<String> resultSet2 = ComparatorHelper.getResultSetFirstColumnAsString(restrictiveQuery, errors, state);
        if (resultSet.size() < resultSet2.size()) {
            throw new AssertionError(originalQuery + " " + restrictiveQuery);
        }
    }

}
