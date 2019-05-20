package com.log.adder.walker;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;

import java.util.Optional;

public class Utility {

    public static IfStmt createEnteringLogCondition(NameExpr loggerIdentifier, MethodDeclaration method) {

        final NodeList<Parameter> parameters = method.getParameters();

        MethodCallExpr expression = new MethodCallExpr();
        final NameExpr loggerVariableExpression = loggerIdentifier;
        expression.setScope(loggerVariableExpression);
        expression.setName("debug");

        //expression.addArgument()

        StringLiteralExpr stringLiteralExpr  = new StringLiteralExpr("Entering method " + method.getName().getIdentifier());


        BinaryExpr binaryExpr= new BinaryExpr();
        binaryExpr.setOperator(BinaryExpr.Operator.PLUS);
        binaryExpr.setLeft(stringLiteralExpr);

        Expression right = getParameterExpression(parameters);

        binaryExpr.setRight(right);


        expression.addArgument(binaryExpr );
        ExpressionStmt expressionStmt = new ExpressionStmt(expression);

        IfStmt ifStmt = new IfStmt();
        MethodCallExpr methodCallExpr = new MethodCallExpr();
        methodCallExpr.setScope(loggerVariableExpression);
        methodCallExpr.setName("isDebugEnabled");
        ifStmt.setCondition(methodCallExpr);

        BlockStmt block = new BlockStmt();
        block.addStatement(expressionStmt);
        ifStmt.setThenStmt(block);
        return ifStmt;
    }

    private static Expression getParameterExpression(NodeList<Parameter> parameters) {

        if(parameters.size()==0){

            return new StringLiteralExpr("");
        }

        Expression right = null;

        for (Parameter parameter : parameters) {
            if(right == null){
                right = parameter.getNameAsExpression();
                continue;
            }
            BinaryExpr binary = new BinaryExpr();
            binary.setOperator(BinaryExpr.Operator.PLUS);

            binary.setLeft(new BinaryExpr(parameter.getNameAsExpression(), new StringLiteralExpr(" | ")   , BinaryExpr.Operator.PLUS));
            binary.setRight(right);
            right = binary;
        }



        BinaryExpr binaryExpr = new BinaryExpr();
        binaryExpr.setOperator(BinaryExpr.Operator.PLUS);
        binaryExpr.setLeft(new StringLiteralExpr(""));
        binaryExpr.setRight(right);


        return binaryExpr;
    }



    public static IfStmt createExitLogCondition(NameExpr loggerIdentifier, MethodDeclaration method, Optional<Expression> returnExpression) {
        MethodCallExpr expression = new MethodCallExpr();
        final NameExpr loggerVariableExpression = loggerIdentifier;
        expression.setScope(loggerVariableExpression);
        expression.setName("debug");
        //expression.addArgument()
        StringLiteralExpr stringLiteralExpr  = new StringLiteralExpr("Exitting method " + method.getName().getIdentifier());
        BinaryExpr binaryExpr= new BinaryExpr();
        binaryExpr.setOperator(BinaryExpr.Operator.PLUS);
        binaryExpr.setLeft(stringLiteralExpr);
        binaryExpr.setRight(new StringLiteralExpr(""));
        if(returnExpression.isPresent()){
            binaryExpr.setRight(returnExpression.get());
        }

        expression.addArgument(binaryExpr );
        ExpressionStmt expressionStmt = new ExpressionStmt(expression);

        IfStmt ifStmt = new IfStmt();
        MethodCallExpr methodCallExpr = new MethodCallExpr();
        methodCallExpr.setScope(loggerVariableExpression);
        methodCallExpr.setName("isDebugEnabled");
        ifStmt.setCondition(methodCallExpr);

        BlockStmt block = new BlockStmt();
        block.addStatement(expressionStmt);
        ifStmt.setThenStmt(block);
        return ifStmt;
    }



}
