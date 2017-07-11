package com.yahoo.validatar.assertion;

import com.yahoo.validatar.assertion.Expression.BinaryStateOperation;
import com.yahoo.validatar.assertion.Expression.UnaryStateOperation;
import com.yahoo.validatar.common.Column;
import com.yahoo.validatar.common.Operations;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.TypeSystem;
import com.yahoo.validatar.common.TypedObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Handles evaluating the parsed Assertion parse tree generated by ANTLR.
 */
@Slf4j
public class AssertVisitor extends GrammarBaseVisitor<Expression> {
    // These are read-only (including values)! Do not modify ever!
    private final Map<String, Column> columns;
    private final Map<String, Map<String, Column>> datasets;

    // These change per assert.
    @Getter
    private Map<String, String> examinedColumns;
    private Map<String, Column> joinedColumns;

    private static String stripQuotes(String literal) {
        return literal.substring(1, literal.length() - 1);
    }

    private Column getColumnValue(Map<String, Column> columns, String name) {
        // Check for no mapping explicitly
        if (!columns.containsKey(name)) {
            log.error("Column not found: {}.\nAvailable Columns: {}", name, columns.keySet());
            throw new NoSuchElementException("Unable to find value for column: " + name + " in results");
        }
        Column result = columns.get(name);
        examinedColumns.put(name, Objects.toString(result));
        return result;
    }

    // This function matches the UnaryStateOperation functional interface
    private static Column negate(Column column) {
        Column negativeOnes = TypeSystem.asColumn(TypeSystem.asTypedObject(-1L), column.size());
        return TypeSystem.perform(Operations.BinaryOperation.MULTIPLY, negativeOnes, column);
    }

    // Helpers to partially apply perform
    private static UnaryStateOperation curry(Operations.UnaryOperation operation) {
        return input -> TypeSystem.perform(operation, input);
    }

    private static BinaryStateOperation curry(Operations.BinaryOperation operation) {
        return (a, b) -> TypeSystem.perform(operation, a, b);
    }

    /**
     * Creates a vistor to walk the assertion parse tree.
     *
     * @param results A non-null {@link List} of {@link Result}.
     */
    public AssertVisitor(List<Result> results) {
        Objects.requireNonNull(results);

        datasets = new HashMap<>();
        examinedColumns = new HashMap<>();
        joinedColumns = new HashMap<>();

        Result merged = new Result();
        for (Result result : results) {
            merged.merge(result);
            datasets.put(result.getNamespace(), result.getColumns());
        }

        columns = merged.getColumns();
    }

    /**
     * Resets the visited state. Call after each walk from the top-level statement.
     */
    public void reset() {
        examinedColumns = new HashMap<>();
        joinedColumns = new HashMap<>();
    }

    @Override
    public Expression visitTruthy(GrammarParser.TruthyContext context) {
        String bool = context.getText();
        return Expression.wrap(TypeSystem.asTypedObject(Boolean.valueOf(bool)));
    }

    @Override
    public Expression visitWholeNumber(GrammarParser.WholeNumberContext context) {
        String text = context.getText();
        TypedObject object;
        try {
            object = TypeSystem.asTypedObject(Long.valueOf(text));
        } catch (NumberFormatException nfe) {
            log.info("Integer could not be parsed as a Long: {}. Trying BigDecimal...", text);
            object = TypeSystem.asTypedObject(new BigDecimal(text));
        }
        return Expression.wrap(object);
    }

    @Override
    public Expression visitDecimalNumber(GrammarParser.DecimalNumberContext context) {
        String text = context.getText();
        TypedObject object;
        try {
            object = TypeSystem.asTypedObject(Double.valueOf(text));
        } catch (NumberFormatException nfe) {
            log.info("Integer could not be parsed as a Double: {}. Trying BigDecimal...", text);
            object = TypeSystem.asTypedObject(new BigDecimal(text));
        }
        return Expression.wrap(object);
    }

    @Override
    public Expression visitTruthValue(GrammarParser.TruthValueContext context) {
        return visit(context.truthy());
    }

    @Override
    public Expression visitNumericValue(GrammarParser.NumericValueContext context) {
        return visit(context.numeric());
    }

    @Override
    public Expression visitStringValue(GrammarParser.StringValueContext context) {
        String text = context.getText();
        return Expression.wrap(TypeSystem.asTypedObject(StringEscapeUtils.unescapeJava(stripQuotes(text))));
    }

    @Override
    public Expression visitIdentifier(GrammarParser.IdentifierContext context) {
        String text = context.getText();
        // We want to return an expression which will pull the value from a data set that we will pass in later.
        return new Expression(data -> getColumnValue(data, text));
    }

    @Override
    public Expression visitApproxValue(GrammarParser.ApproxValueContext context) {
        Expression a = visit(context.l);
        Expression b = visit(context.r);
        Expression percent = visit(context.p);
        return new Expression(data -> TypeSystem.approx(a.evaluate(data), b.evaluate(data), percent.evaluate(data)));
    }

    @Override
    public Expression visitBaseValue(GrammarParser.BaseValueContext context) {
        return visit(context.base());
    }

    @Override
    public Expression visitFunctionalValue(GrammarParser.FunctionalValueContext context) {
        return visit(context.functionalExpression());
    }

    @Override
    public Expression visitParenthesizedValue(GrammarParser.ParenthesizedValueContext context) {
        return visit(context.orExpression());
    }

    @Override
    public Expression visitNegateValue(GrammarParser.NegateValueContext context) {
        Expression expression = visit(context.baseExpression());
        return Expression.compose(AssertVisitor::negate, expression);
    }

    @Override
    public Expression visitLogicalNegateValue(GrammarParser.LogicalNegateValueContext context) {
        Expression expression = visit(context.baseExpression());
        return Expression.compose(curry(Operations.UnaryOperation.NOT), expression);
    }

    @Override
    public Expression visitBaseUnaryValue(GrammarParser.BaseUnaryValueContext context) {
        return visit(context.unaryExpression());
    }

    @Override
    public Expression visitMultiplyValue(GrammarParser.MultiplyValueContext context) {
        Expression left = visit(context.multiplicativeExpression());
        Expression right = visit(context.unaryExpression());
        return Expression.compose(curry(Operations.BinaryOperation.MULTIPLY), left, right);
    }

    @Override
    public Expression visitDivideValue(GrammarParser.DivideValueContext context) {
        Expression left = visit(context.multiplicativeExpression());
        Expression right = visit(context.unaryExpression());
        return Expression.compose(curry(Operations.BinaryOperation.DIVIDE), left, right);
    }

    @Override
    public Expression visitModValue(GrammarParser.ModValueContext context) {
        Expression left = visit(context.multiplicativeExpression());
        Expression right = visit(context.unaryExpression());
        return Expression.compose(curry(Operations.BinaryOperation.MODULUS), left, right);
    }

    @Override
    public Expression visitBaseMultiplicativeValue(GrammarParser.BaseMultiplicativeValueContext context) {
        return visit(context.multiplicativeExpression());
    }

    @Override
    public Expression visitAddValue(GrammarParser.AddValueContext context) {
        Expression left = visit(context.additiveExpression());
        Expression right = visit(context.multiplicativeExpression());
        return Expression.compose(curry(Operations.BinaryOperation.ADD), left, right);
    }

    @Override
    public Expression visitSubtractValue(GrammarParser.SubtractValueContext context) {
        Expression left = visit(context.additiveExpression());
        Expression right = visit(context.multiplicativeExpression());
        return Expression.compose(curry(Operations.BinaryOperation.SUBTRACT), left, right);
    }

    @Override
    public Expression visitBaseAdditiveValue(GrammarParser.BaseAdditiveValueContext context) {
        return visit(context.additiveExpression());
    }

    @Override
    public Expression visitGreaterValue(GrammarParser.GreaterValueContext context) {
        Expression left = visit(context.relationalExpression());
        Expression right = visit(context.additiveExpression());
        return Expression.compose(curry(Operations.BinaryOperation.GREATER), left, right);
    }

    @Override
    public Expression visitLessValue(GrammarParser.LessValueContext context) {
        Expression left = visit(context.relationalExpression());
        Expression right = visit(context.additiveExpression());
        return Expression.compose(curry(Operations.BinaryOperation.LESS), left, right);
    }

    @Override
    public Expression visitLessEqualValue(GrammarParser.LessEqualValueContext context) {
        Expression left = visit(context.relationalExpression());
        Expression right = visit(context.additiveExpression());
        return Expression.compose(curry(Operations.BinaryOperation.LESS_EQUAL), left, right);
    }

    @Override
    public Expression visitGreaterEqualValue(GrammarParser.GreaterEqualValueContext context) {
        Expression left = visit(context.relationalExpression());
        Expression right = visit(context.additiveExpression());
        return Expression.compose(curry(Operations.BinaryOperation.GREATER_EQUAL), left, right);
    }

    @Override
    public Expression visitBaseRelativeValue(GrammarParser.BaseRelativeValueContext context) {
        return visit(context.relationalExpression());
    }
    @Override
    public Expression visitEqualityValue(GrammarParser.EqualityValueContext context) {
        Expression left = visit(context.equalityExpression());
        Expression right = visit(context.relationalExpression());
        return Expression.compose(curry(Operations.BinaryOperation.EQUAL), left, right);
    }

    @Override
    public Expression visitNotEqualityValue(GrammarParser.NotEqualityValueContext context) {
        Expression left = visit(context.equalityExpression());
        Expression right = visit(context.relationalExpression());
        return Expression.compose(curry(Operations.BinaryOperation.NOT_EQUAL), left, right);
    }

    @Override
    public Expression visitBaseEqualityValue(GrammarParser.BaseEqualityValueContext context) {
        return visit(context.equalityExpression());
    }

    @Override
    public Expression visitAndValue(GrammarParser.AndValueContext context) {
        Expression left = visit(context.andExpression());
        Expression right = visit(context.equalityExpression());
        return Expression.compose(curry(Operations.BinaryOperation.AND), left, right);
    }

    @Override
    public Expression visitBaseAndValue(GrammarParser.BaseAndValueContext context) {
        return visit(context.andExpression());
    }

    @Override
    public Expression visitOrValue(GrammarParser.OrValueContext context) {
        Expression left = visit(context.orExpression());
        Expression right = visit(context.andExpression());
        return Expression.compose(curry(Operations.BinaryOperation.OR), left, right);
    }

    @Override
    public Expression visitBaseOrValue(GrammarParser.BaseOrValueContext context) {
        return visit(context.orExpression());
    }

    @Override
    public Expression visitJoinValue(GrammarParser.JoinValueContext context) {
        Expression assertion = visit(context.o);
        Expression join = visit(context.j);
        // TODO: Construct the joined dataset (for each column, construct every row possibility)
        joinedColumns = columns;
        return Expression.wrap(assertion.evaluate(joinedColumns));
    }
}
