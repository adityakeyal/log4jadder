package com.log.adder.walker;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class FileWalker {

    private final File file;
    private List<ReturnStmt> returnStmts = new ArrayList<>();
    private List<MethodDeclaration> methodDeclaration = new ArrayList<>();
    private List<FieldDeclaration> fieldsDeclaration = new ArrayList<>();
    private List<MethodCallExpr> methodCallExprs = new ArrayList<>();
    private CompilationUnit compilationUnit;
    private NameExpr loggerExpression;
    private boolean isProcessable = false;


    public FileWalker(File file) throws FileNotFoundException {
        this.compilationUnit = StaticJavaParser.parse(file);

        this.file = file;
        walk();
    }


    private FileWalker walk() {
        final NodeList<TypeDeclaration<?>> compilationUnitTypes = this.compilationUnit.getTypes();
        processType(compilationUnitTypes.get(0));
        return this;
    }

    private void processType(TypeDeclaration<?> typeDeclaration) {
        if(!(typeDeclaration instanceof ClassOrInterfaceDeclaration)){
            return;
        }
        if(typeDeclaration.getModifiers().contains(Modifier.abstractModifier())){
            return;
        }
        if((((ClassOrInterfaceDeclaration) typeDeclaration).isInterface() )){
            return;
        }
        isProcessable = true;
        final List<MethodDeclaration> methods = typeDeclaration.getMethods();
        methods.stream().forEach(this::processMethod);
        fieldsDeclaration = typeDeclaration.getFields();
    }

    private void processMethod(MethodDeclaration methodDeclaration) {
        if(methodDeclaration.isPublic() ){
                if(!(methodDeclaration.getName().getIdentifier().startsWith("set") || methodDeclaration.getName().getIdentifier().startsWith("get"))) {
                    this.methodDeclaration.add(methodDeclaration);
                    processStatement(methodDeclaration.getBody().get());
                }
        }
    }

    public boolean isLoggerAvailable(){
        boolean flag = false;
        for (FieldDeclaration fieldDeclaration : fieldsDeclaration) {
            if(fieldDeclaration.getVariables().size()==1){
                final VariableDeclarator variableDeclarator = fieldDeclaration.getVariables().get(0);
                final String variableString = variableDeclarator.getNameAsString();
                if ( variableString.equalsIgnoreCase("logger") || variableString.equalsIgnoreCase("log") ){
                    loggerExpression = variableDeclarator.getNameAsExpression();
                    return true;
                }
            }

        }


        return flag;


    }


    private void processStatement(Node node) {
        if(node instanceof ReturnStmt){
            this.returnStmts.add(((ReturnStmt) node).asReturnStmt());
        }

        if(node instanceof MethodCallExpr){

            MethodCallExpr mthdCall = (MethodCallExpr) node;
            methodCallExprs.add(mthdCall);

        }

        node.getChildNodes().stream().forEach(this::processStatement);
    }


    public void addEntryLogToAllPublicMethods() throws IllegalAccessException {
        if(loggerExpression==null){
            throw new IllegalAccessException("Initialize a valid logger first");
        }

        for (MethodDeclaration methodDecl : methodDeclaration) {
            final IfStmt logCondition = Utility.createEnteringLogCondition(loggerExpression, methodDecl);
            final NodeList<Statement> statements = methodDecl.getBody().get().getStatements();
            if(statements.size()>0 && statements.get(0).toString().indexOf("debug") < 0 ){
                statements.add(0,logCondition);
            }
        }
    }


    public void addExitLogToAllPublicMethods() throws IllegalAccessException {
        if(loggerExpression==null){
            throw new IllegalAccessException("Initialize a valid logger first");
        }

        for (MethodDeclaration methodDecl : methodDeclaration) {

            if(methodDecl.getType() instanceof VoidType){
                final IfStmt exitLogCondition = Utility.createExitLogCondition(loggerExpression, methodDecl,Optional.empty());
                final NodeList<Statement> statements = methodDecl.getBody().get().getStatements();
                final Statement statement = statements.get(statements.size() - 1);
                if(statement.toString().indexOf("debug")<0){
                    statements.addLast(exitLogCondition);
                }
            }
        } // all void exit methods added

        for (ReturnStmt returnStmt : returnStmts) {
            final Node node = returnStmt.getParentNode().get();
            if(node instanceof BlockStmt){
                final BlockStmt block = (BlockStmt) node;
                Node method = returnStmt.getParentNode().get();
                while(!(method instanceof MethodDeclaration)){
                    method = method.getParentNode().get();
                }
                final IfStmt exitLogCondition = Utility.createExitLogCondition(loggerExpression, (MethodDeclaration)method , returnStmt.getExpression());
                final int size = block.getStatements().size()-1;
                final Statement statement = block.getStatements().get(block.getStatements().size() - 2);

                if(statement.toString().indexOf("debug")<0){
                    block.addStatement(size,exitLogCondition);
                }
            }
        }
    }



    public int countOfLogging(){
        int count = 0;
        for (MethodCallExpr methodCallExpr : methodCallExprs) {
            if(methodCallExpr.getScope().isPresent()){
                final Expression expression = methodCallExpr.getScope().get();
                if(expression instanceof NameExpr){
                    if(((NameExpr) expression).getName().getIdentifier().equalsIgnoreCase(loggerExpression.getNameAsString())){
                        if(methodCallExpr.getName().getIdentifier().equalsIgnoreCase("debug")){
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }


    public void addLogger(){

        ImportDeclaration importDeclaration = new ImportDeclaration("org.apache.log4j.Logger" , false,false);
        compilationUnit.getImports().addLast(importDeclaration);


        final TypeDeclaration<?> typeDeclaration = compilationUnit.getTypes().get(0);
        final BodyDeclaration<?> bodyDeclaration = StaticJavaParser.parseBodyDeclaration("private static final Logger LOGGER = Logger.getLogger(" + typeDeclaration.getNameAsString() + ".class);");

        typeDeclaration.getMembers().addFirst(bodyDeclaration);
        loggerExpression =  new NameExpr("LOGGER");



    }


    public boolean isProcessable() {
        return isProcessable;
    }

    public void print() {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.write(compilationUnit.toString(), fos, "UTF-8");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
