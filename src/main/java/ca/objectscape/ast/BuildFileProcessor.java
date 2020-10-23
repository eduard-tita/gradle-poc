package ca.objectscape.ast;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.builder.AstBuilder;

public class BuildFileProcessor
{

  private final List<ASTNode> astNodes;

  public BuildFileProcessor(String codeAsText) {
    AstBuilder builder = new AstBuilder();
    astNodes = builder.buildFromString(codeAsText);
  }

  public Map<String, Set<String>> collectConfigurations() {
    ConfigurationsVisitor configurationsVisitor = new ConfigurationsVisitor();
    walkScript(astNodes, configurationsVisitor);
    return configurationsVisitor.getConfigurationMap();
  }

  public List<ModuleDependency> collectDependencies() {
    DependenciesVisitor dependenciesVisitor = new DependenciesVisitor();
    walkScript(astNodes, dependenciesVisitor);
    return dependenciesVisitor.getDependencies();
  }

  private void walkScript(List<ASTNode> nodes, GroovyCodeVisitor visitor) {
    for (ASTNode node : nodes) {
      node.visit(visitor);
    }
  }
}
