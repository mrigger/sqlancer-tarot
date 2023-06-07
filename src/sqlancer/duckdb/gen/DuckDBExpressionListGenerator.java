package sqlancer.duckdb.gen;

import java.util.ArrayList;
import java.util.List;

import sqlancer.common.ast.newast.ColumnReferenceNode;
import sqlancer.common.ast.newast.NewBinaryOperatorNode;
import sqlancer.common.ast.newast.Node;
import sqlancer.duckdb.DuckDBProvider.DuckDBGlobalState;
import sqlancer.duckdb.DuckDBSchema.DuckDBColumn;
import sqlancer.duckdb.ast.DuckDBConstant;
import sqlancer.duckdb.ast.DuckDBExpression;
import sqlancer.duckdb.gen.DuckDBExpressionGenerator.DuckDBBinaryComparisonOperator;

public class DuckDBExpressionListGenerator {

    private DuckDBGlobalState globalState;
    private List<DuckDBColumn> columns;

    public DuckDBExpressionListGenerator(DuckDBGlobalState globalState, List<DuckDBColumn> columns) {
        this.globalState = globalState;
        this.columns = columns;
    }

    public List<Node<DuckDBExpression>> generateAllExpressions() {
        List<Node<DuckDBExpression>> expressions = new ArrayList<>();
        for (int i = 1; i <= globalState.getOptions().getMaxExpressionDepth(); i++) {
            expressions.addAll(generateExpression(i));
        }
        return expressions;
    }

    public List<Node<DuckDBExpression>> generateExpression(int depth) {
        if (depth >= globalState.getOptions().getMaxExpressionDepth()) {
            return generateLeafNodes();
        } else {
            List<Node<DuckDBExpression>> expressions = new ArrayList<>();
            for (DuckDBBinaryComparisonOperator comparisonOperators : DuckDBBinaryComparisonOperator.values()) {
                List<Node<DuckDBExpression>> leftOperands = generateExpression(depth + 1);
                List<Node<DuckDBExpression>> rightOperands = generateExpression(depth + 1);
                for (Node<DuckDBExpression> leftOperand : leftOperands) {
                    for (Node<DuckDBExpression> rightOperand : rightOperands) {
                        expressions.add(new NewBinaryOperatorNode<>(leftOperand, rightOperand, comparisonOperators));
                    }
                }
            }
            return expressions;
        }
    }

    private List<Node<DuckDBExpression>> generateLeafNodes() {
        List<Node<DuckDBExpression>> expressions = new ArrayList<>();
        for (DuckDBColumn c : columns) {
            expressions.add(new ColumnReferenceNode<DuckDBExpression, DuckDBColumn>(c));
        }
        expressions.add(DuckDBConstant.createIntConstant(0));
        expressions.add(DuckDBConstant.createIntConstant(-0));
        expressions.add(DuckDBConstant.createIntConstant(1));
        expressions.add(DuckDBConstant.createNullConstant());
        return expressions;
    }

}
