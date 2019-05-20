package com.log.adder;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LogIdentifier {


    public static void main(String[] args) throws Exception {


        final Collection<File> files = FileUtils.listFiles(new File("D:\\code\\"), new String[]{"java"}, true);

        for (File file : files) {
            CompilationUnit compilationUnit = StaticJavaParser.parse(file);
                final NodeList<TypeDeclaration<?>> types = compilationUnit.getTypes();
                        if(types.get(0) instanceof ClassOrInterfaceDeclaration){
                            if( !( (((ClassOrInterfaceDeclaration) types.get(0)).isInterface() ) || types.get(0).getModifiers().contains(Modifier.abstractModifier()))){
                                if(process(types.get(0))){
                                    IOUtils.write(compilationUnit.toString(),System.out, Charset.forName("UTF-8"));
                                }
                            }
                  }
        }
    }

    private static boolean process(TypeDeclaration<?> typeDeclaration) {

        boolean isPresent = false;

        final List<FieldDeclaration> fields = typeDeclaration.getFields();

        for (FieldDeclaration field : fields) {
            final NodeList<VariableDeclarator> variables = field.getVariables();
            for (VariableDeclarator variable : variables) {
                if(isLogVariable(variable.getName().getIdentifier())){

                    addLogStatementToMethod(typeDeclaration,variable.getName().getIdentifier());

                    isPresent = true;
                }
            }

        }

        return true;

    }

    private static void addLogStatementToMethod(TypeDeclaration<?> typeDeclaration, String identifier) {


        final List<MethodDeclaration> methods = typeDeclaration.getMethods();

        for (MethodDeclaration method : methods) {
            if(method.isPublic()){


                final BlockStmt blockStmt = method.getBody().get();

                visitor(blockStmt);

                if(!method.getBody().get().getStatements().get(0).toString().contains(".debug(")){
                    IfStmt ifStmt = createEnteringLogCondition(typeDeclaration, identifier, method);
                    method.getBody().get().addStatement(0,ifStmt);
                }


            }
        }
    }

    private static boolean visitor(Node blockStmt) {


        if(blockStmt instanceof ReturnStmt){

//            Node parentNode = blockStmt.getParentNode().get();
//            while(!(parentNode instanceof BlockStmt)){
//                parentNode = parentNode.getParentNode().get();
//            }
//
//            BlockStmt block = (BlockStmt) parentNode;
//            final int size = block.getStatements().size();
//            block.addStatement(size-2 , new StringLiteralExpr("Hello"));

            return true;

        }

        final List<Node> collect = blockStmt.getChildNodes().stream().filter(LogIdentifier::visitor).collect(Collectors.toList());
        if(collect.size()>0){
            for (Node node : collect) {
            Node parentNode = node.getParentNode().get();
            while(!(parentNode instanceof BlockStmt)){
                parentNode = parentNode.getParentNode().get();
            }

            BlockStmt block = (BlockStmt) parentNode;
            final int size = block.getStatements().size();
            System.out.println(size);
            block.addStatement(size-1 == -1 ? 0 : size-1  , new StringLiteralExpr("Hello"));
                
            }
        }

        return false;


    }

    private static IfStmt createEnteringLogCondition(TypeDeclaration<?> typeDeclaration, String identifier, MethodDeclaration method) {

        final NodeList<Parameter> parameters = method.getParameters();

        MethodCallExpr expression = new MethodCallExpr();
        final NameExpr loggerVariableExpression = new NameExpr(identifier);
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

    private static boolean isLogVariable(String identifier) {

        if(identifier.equalsIgnoreCase("log") || identifier.equalsIgnoreCase("logger")){
            return true;
        }


        return false;
    }
}
