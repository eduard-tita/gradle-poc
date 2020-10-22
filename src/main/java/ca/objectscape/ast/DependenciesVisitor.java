package ca.objectscape.ast;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependenciesVisitor extends CodeVisitorSupport
{
  private static final Logger log = LoggerFactory.getLogger(DependenciesVisitor.class);

  // FIXME collect configurations as well
  public String implementationConfig = "implementation";
  public String compileConfig = "compile";
  public String compileOnlyConfig = "compileOnly";
  public String runtimeConfig = "runtime";
  public String runtimeOnlyConfig = "runtimeOnly";

  private Map<String, Set<String>> configurationMap = new HashMap<>();

  private Deque<String> methodStack = new LinkedList<>();

  private List<ModuleDependency> dependencies = new ArrayList<>();
  
  @Override
  public void visitMethodCallExpression( MethodCallExpression call )
  {
    String method = call.getMethod().getText();
    methodStack.addLast(method);
    log.info("visitMethodCallExpression: {}, stack: {}", method, methodStack);

    if (isInConfigurations()) {
      if ("extendsFrom".equals(method)) {
        final Expression objectExpression = call.getObjectExpression();
        if (objectExpression instanceof VariableExpression) {
          VariableExpression variableExpression = (VariableExpression) objectExpression;
          String variable = variableExpression.getAccessedVariable().getName();
          log.info("variable: {}", variable);
          if (!configurationMap.containsKey(variable)) {
            configurationMap.put(variable, new HashSet<>());
          }

          final Expression arguments = call.getArguments();
          if (arguments instanceof ArgumentListExpression) {
            ArgumentListExpression ale = (ArgumentListExpression) arguments;
            for (Expression expression : ale) {
              if (expression instanceof VariableExpression) {
                VariableExpression varExpression = (VariableExpression) expression;
                configurationMap.get(variable).add(varExpression.getAccessedVariable().getName());
              }
            }
          }
        }
      }
    }

    super.visitMethodCallExpression( call );

    methodStack.removeLast();
  }

  @Override
  public void visitArgumentlistExpression( ArgumentListExpression ale )
  {
    if (isInDependencies()) {
      log.info("visitArgumentlistExpression: {}", ale.getExpressions());

      List<Expression> expressions = ale.getExpressions();
      for (Expression expression : expressions) {
        if (expression instanceof ConstantExpression) {
          // TODO improve this e.g. handle '@jar'
          String[] deps = expression.getText().split(":");
          if (deps.length >= 3) {
            ModuleDependency dep = new ModuleDependency();
            dep.group = deps[0];
            dep.name = deps[1];
            dep.version = deps[2];
            dep.configuration = methodStack.getLast();
            dep.lineNumber = expression.getLineNumber();
            dep.columnNumber = expression.getColumnNumber();
            dep.lastLineNumber = expression.getLastLineNumber();
            dep.lastColumnNumber = expression.getLastColumnNumber();
            dependencies.add(dep);
          }
        }
      }
    }
    super.visitArgumentlistExpression( ale );
  }

  @Override
  public void visitMapExpression( MapExpression expression )
  {
    if (isInDependencies()) {
      log.info("visitMapExpression: {}", expression);

      List<MapEntryExpression> mapEntryExpressions = expression.getMapEntryExpressions();
      ModuleDependency dep = new ModuleDependency();
      for (MapEntryExpression mapEntryExpression : mapEntryExpressions) {
        String key = mapEntryExpression.getKeyExpression().getText();
        String value = mapEntryExpression.getValueExpression().getText();
        if ("group".equals(key)) dep.group = value;
        if ("name".equals(key)) dep.name = value;
        if ("version".equals(key)) dep.version = value;
      }
      dep.configuration = methodStack.getLast();
      dep.lineNumber = expression.getLineNumber();
      dep.columnNumber = expression.getColumnNumber();
      dep.lastLineNumber = expression.getLastLineNumber();
      dep.lastColumnNumber = expression.getLastColumnNumber();
      dependencies.add(dep);
    }
    super.visitMapExpression( expression );
  }

  public List<ModuleDependency> getDependencies() {
    return dependencies;
  }

  public Map<String, Set<String>> getConfigurationMap() {
    return configurationMap;
  }

  boolean isInConfigurations() {
    return methodStack.contains("configurations");
  }

  boolean isInDependencies() {
    return methodStack.contains("dependencies");
  }
}
