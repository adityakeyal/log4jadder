package com.log.adder.walker;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.Optional;

public class Utility {

    public static IfStmt createEnteringLogCondition(NameExpr loggerIdentifier, MethodDeclaration method) {



        final String identifier = loggerIdentifier.getName().getIdentifier();
        StringBuilder builder = new StringBuilder();
        builder.append("if(")
                .append(identifier)
                .append(".isDebugEnabled() ) {")
                .append(identifier)
                .append(".debug(")
                .append("\"Entering method : "  )
                .append( method.getName().getIdentifier())
                .append("\"");

        for (Parameter parameter : method.getParameters()) {
                builder.append(" + ")
                       .append(" \" | \" ")
                       .append(" + ")
                       .append(parameter.getName().getIdentifier());

        }


        builder.append(");").append("}");
        final Statement statement = StaticJavaParser.parseStatement(builder.toString());


        return (IfStmt) statement;


    }





    public static IfStmt createExitLogCondition(NameExpr loggerIdentifier, MethodDeclaration method, Optional<Expression> returnExpression) {


        String template = " if( %1$s.isDebugEnabled()) {"
                + " %1$s.debug( \" Existing method (%2$s)  \" ";


        if(returnExpression.isPresent()){
            template += " + (" + returnExpression.get().toString() + ")";
        }
        template +=");";
        template += "}";


        final String returnFormat = String.format(template, loggerIdentifier.getName().getIdentifier(), method.getName().getIdentifier());

        final Statement statement = StaticJavaParser.parseStatement(returnFormat);

        return (IfStmt) statement;
    }



}
