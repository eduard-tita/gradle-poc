package ca.objectscape.ast;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependenciesVisitor extends CodeVisitorSupport
{
  private static final Logger log = LoggerFactory.getLogger(DependenciesVisitor.class);

  private final Deque<String> methodStack = new LinkedList<>();

  private final List<ModuleDependency> dependencies = new ArrayList<>();
  
  @Override
  public void visitMethodCallExpression( MethodCallExpression call )
  {
    String method = call.getMethod().getText();
    methodStack.addLast(method);
    log.info("visitMethodCallExpression: {}, stack: {}", method, methodStack);
    super.visitMethodCallExpression( call );
    methodStack.removeLast();
  }

  @Override
  public void visitArgumentlistExpression( ArgumentListExpression ale )
  {
    if (isInDependencies() && isNotInExcludeClause()) {
      log.info("visitArgumentlistExpression: {}", ale.getExpressions());

      List<Expression> expressions = ale.getExpressions();
      for (Expression expression : expressions) {
        if (expression instanceof ConstantExpression) {
          // External dependencies - string notation format:
          // configurationName "group:name:version:classifier@extension"
          String[] deps = expression.getText().split(":");
          if (deps.length >= 3) {
            ModuleDependency dep = new ModuleDependency();
            dep.group = deps[0];
            dep.name = deps[1];
            dep.version = deps[2];
            dep.configurationName = methodStack.getLast();
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
    if (isInDependencies() && isNotInExcludeClause()) {
      log.info("visitMapExpression: {}", expression);

      // External dependencies - map notation format:
      // configurationName group: group, name: name, version: version, classifier: classifier, ext: extension
      List<MapEntryExpression> mapEntryExpressions = expression.getMapEntryExpressions();
      ModuleDependency dep = new ModuleDependency();
      for (MapEntryExpression mapEntryExpression : mapEntryExpressions) {
        String key = mapEntryExpression.getKeyExpression().getText();
        String value = mapEntryExpression.getValueExpression().getText();
        if ("group".equals(key)) dep.group = value;
        if ("name".equals(key)) dep.name = value;
        if ("version".equals(key)) dep.version = value;
      }
      dep.configurationName = methodStack.getLast();
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

  boolean isInDependencies() {
    return methodStack.contains("dependencies");
  }

  boolean isNotInExcludeClause() {
    return !methodStack.contains("exclude");
  }
}
