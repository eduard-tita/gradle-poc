package ca.objectscape.ast;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GradleAstMain
{
  private static final Logger log = LoggerFactory.getLogger(GradleAstMain.class);

  public static void main(String[] args) throws IOException {
    final String buildFile = "src/main/resources/groovy/build.gradle";
    final String text = readFile(buildFile, Charset.defaultCharset());

    final BuildFileProcessor processor = new BuildFileProcessor(text);

    final List<ModuleDependency> dependencies = processor.collectDependencies();
    log.info(" --------------------------- ");
    for (ModuleDependency dependency : dependencies) {
      log.info("dependency: {}", dependency);
    }

    log.info(" --------------------------- ");
    log.info("configurationMap: {}", processor.collectConfigurations());
  }

  static void walkScript(List<ASTNode> nodes, GroovyCodeVisitor visitor)
  {
    for( ASTNode node : nodes )
    {
      node.visit( visitor );
    }
  }

  static String readFile(String path, Charset encoding)
      throws IOException
  {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }

  static List<ASTNode> build(String scriptContents) throws MultipleCompilationErrorsException
  {
    AstBuilder builder = new AstBuilder();
    return builder.buildFromString( scriptContents );
  }
}
