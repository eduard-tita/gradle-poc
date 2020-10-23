package ca.objectscape.ast;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationsVisitor
    extends CodeVisitorSupport
{
  private static final Logger log = LoggerFactory.getLogger(ConfigurationsVisitor.class);

  // default compile and runtime configurations - from the java plugin
  public static String implementationConfig = "implementation";
  public static String compileConfig = "compile";
  public static String compileOnlyConfig = "compileOnly";
  public static String runtimeConfig = "runtime";
  public static String runtimeOnlyConfig = "runtimeOnly";

  /**
   * Map representing configuration hierarchies: parentConfigurationName -> Set of childConfigurationNames
   */
  private final Map<String, Set<String>> configurationMap = new HashMap<>();

  private final Deque<String> methodStack = new LinkedList<>();

  @Override
  public void visitMethodCallExpression( MethodCallExpression call )
  {
    String method = call.getMethod().getText();
    methodStack.addLast(method);
    //log.info("visitMethodCallExpression: {}, stack: {}", method, methodStack);

    if (isInConfigurations()) {
      if ("extendsFrom".equals(method)) {
        final Expression objectExpression = call.getObjectExpression();
        if (objectExpression instanceof VariableExpression) {
          VariableExpression variableExpression = (VariableExpression) objectExpression;
          String variable = variableExpression.getAccessedVariable().getName();
          log.info("variable: {}", variable);

          final Expression arguments = call.getArguments();
          if (arguments instanceof ArgumentListExpression) {
            ArgumentListExpression ale = (ArgumentListExpression) arguments;
            for (Expression expression : ale) {
              if (expression instanceof VariableExpression) {
                VariableExpression varExpression = (VariableExpression) expression;
                final String key = varExpression.getAccessedVariable().getName();
                if (!configurationMap.containsKey(key)) {
                  configurationMap.put(key, new HashSet<>());
                }
                configurationMap.get(key).add(variable);
              }
            }
          }
        }
      }
    }

    super.visitMethodCallExpression( call );

    methodStack.removeLast();
  }

  public Map<String, Set<String>> getConfigurationMap() {
    return configurationMap;
  }

  boolean isInConfigurations() {
    return methodStack.contains("configurations");
  }
}
