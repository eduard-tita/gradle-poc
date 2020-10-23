package ca.objectscape.ast;

public class ModuleDependency
{
  public String group;
  public String name;
  public String version;
  public String configurationName;

  public int lineNumber = -1;
  public int columnNumber = -1;
  public int lastLineNumber = -1;
  public int lastColumnNumber = -1;

  @Override
  public String toString() {
    return "ModuleDependency{" +
        "group='" + group + '\'' +
        ", name='" + name + '\'' +
        ", version='" + version + '\'' +
        ", configurationName='" + configurationName + '\'' +
        ", lineNumber=" + lineNumber +
        ", columnNumber=" + columnNumber +
        ", lastLineNumber=" + lastLineNumber +
        ", lastColumnNumber=" + lastColumnNumber +
        '}';
  }
}
