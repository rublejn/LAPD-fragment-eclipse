package rules;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceBodyDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTResultType;
import net.sourceforge.pmd.lang.java.ast.ASTType;
import net.sourceforge.pmd.lang.java.ast.ASTTypeArgument;
import net.sourceforge.pmd.lang.java.ast.Comment;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.PropertyFactory;
import utils.CustomDictionary;
import utils.CustomParser;
import utils.Splitter;

public abstract class AbstractLAPDRule extends AbstractJavaRule {

  protected static List<String> collectionKeywords;
  protected static List<String> returningKeywords;
  protected static List<String> ignoreForAntonymsKeywords;
  protected static List<String> validatingKeywords;
  protected static List<String> ignoreForConditionKeywords;
  protected static List<String> aggregationKeywords;
  protected static List<String> predicateKeywords;

  protected final static PropertyDescriptor<List<String>> COLLECTION_KEYWORDS = PropertyFactory
      .stringListProperty("collectionKeywords").desc("keywords to use for collection objects")
      .defaultValue(Arrays.asList("buf", "buffer", "collection", "set", "array", "hash", "linked",
          "vec", "vect", "vector", "list", "iterator", "iterable", "table", "map", "mapping",
          "stack", "hashmap", "hashtable", "arraylist", "queue", "alphabet", "tree"))
      .build();

  protected final static PropertyDescriptor<List<String>> RETURNING_KEYWORDS = PropertyFactory
      .stringListProperty("returningKeywords").desc("keywords to use for returning objects")
      .defaultValue(Arrays.asList("get", "return")).build();

  protected final static PropertyDescriptor<List<String>> IGNORE_FOR_ANTONYMS_KEYWORDS = PropertyFactory
      .stringListProperty("ignoreForAntonymsKeywords")
      .desc("keywords to ignore for antonym relations")
      .defaultValue(Arrays.asList("query", "result", "range", "bar")).build();

  protected final static PropertyDescriptor<List<String>> VALIDATING_KEYWORDS = PropertyFactory
      .stringListProperty("validatingKeywords").desc("keywords to use for validating methods")
      .defaultValue(Arrays.asList("validate", "check", "ensure")).build();

  protected final static PropertyDescriptor<List<String>> IGNORE_FOR_CONDITION_KEYWORDS = PropertyFactory
      .stringListProperty("ignoreForConditionKeywords")
      .desc("terms to ignore as condition keywords").defaultValue(Arrays.asList("ed if", "if you"))
      .build();

  protected final static PropertyDescriptor<List<String>> AGGREGATION_KEYWORDS = PropertyFactory
      .stringListProperty("aggregationKeywords").desc("aggregation keywords")
      .defaultValue(Arrays.asList("count", "number", "length", "width", "nb", "sum", "cnt", "cntr"))
      .build();

  protected final static PropertyDescriptor<List<String>> PREDICATE_KEYWORDS = PropertyFactory
      .stringListProperty("predicateKeywords").desc("predicate keywords")
      .defaultValue(Arrays.asList("is", "has")).build();

  protected final static String SIGNATURE = "Signature: {0}.{1}: {2}";

  protected AbstractLAPDRule() {
    definePropertyDescriptor(COLLECTION_KEYWORDS);
    definePropertyDescriptor(RETURNING_KEYWORDS);
    definePropertyDescriptor(IGNORE_FOR_ANTONYMS_KEYWORDS);
    definePropertyDescriptor(VALIDATING_KEYWORDS);
    definePropertyDescriptor(IGNORE_FOR_CONDITION_KEYWORDS);
    definePropertyDescriptor(AGGREGATION_KEYWORDS);
    definePropertyDescriptor(PREDICATE_KEYWORDS);
  }

  protected static void init() {
    CustomParser.getInstance();
    CustomDictionary.getInstance();
  }

  @Override
  public void start(RuleContext ctx) {
    collectionKeywords = getProperty(COLLECTION_KEYWORDS);
    returningKeywords = getProperty(RETURNING_KEYWORDS);
    ignoreForAntonymsKeywords = getProperty(IGNORE_FOR_ANTONYMS_KEYWORDS);
    validatingKeywords = getProperty(VALIDATING_KEYWORDS);
    ignoreForConditionKeywords = getProperty(IGNORE_FOR_CONDITION_KEYWORDS);
    aggregationKeywords = getProperty(AGGREGATION_KEYWORDS);
    predicateKeywords = getProperty(PREDICATE_KEYWORDS);
  }

  protected static boolean isCollection(final String nodeType) {

    if (nodeType.contains("[")) {
      return true;
    }

    Vector<String> splitted = new Vector<String>();
    final int index = nodeType.indexOf("<");

    if (index != -1) {
      splitted = Splitter.variableSplitter(nodeType.substring(0, index));
    } else {
      splitted = Splitter.variableSplitter(nodeType);
    }

    if (splitted.size() <= 0) {
      return false;
    }

    for (final String word : collectionKeywords) {
      if (splitted.lastElement().equalsIgnoreCase(word)) {
        return true;
      }
    }
    return false;
  }

  protected static boolean containsCollectionKeyword(final String item, final boolean substring) {

    for (final String word : collectionKeywords) {

      if (word.equalsIgnoreCase(item) || substring && item.toLowerCase().contains(word)) {
        return true;
      }
    }
    return false;
  }

  protected static boolean isReturning(final String methodName) {

    final Vector<String> splitted = Splitter.methodSplitter(methodName);

    for (final String word : returningKeywords) {
      if (splitted.firstElement().equalsIgnoreCase(word)) {
        return true;
      }
    }
    return false;
  }

  protected static boolean shouldBeIgnoredFromAntonyms(final String source) {

    for (final String target : ignoreForAntonymsKeywords) {
      if (source.equalsIgnoreCase(target)) {
        return true;
      }
    }
    return false;
  }

  protected static boolean isAggregation(final String source) {

    for (final String target : aggregationKeywords) {
      if (source.equalsIgnoreCase(target)) {
        return true;
      }
    }
    return false;
  }

  protected static boolean isCommentedCode(final String comment) {

    boolean openPar = false, closePar = false, commentedCodeChar = false, containThrows = false;

    if (comment.contains("(")) {
      openPar = true;
      if (comment.endsWith("(")) {
        commentedCodeChar = true;
      }
    }
    if (comment.contains(")")) {
      closePar = true;
      if (comment.endsWith(")")) {
        commentedCodeChar = true;
      }
      if (comment.contains("throws")) {
        containThrows = true;
      }
    }
    if (comment.endsWith("{")) {
      commentedCodeChar = true;
    }
    if (comment.endsWith("}")) {
      commentedCodeChar = true;
    }
    if (comment.endsWith(";")) {
      commentedCodeChar = true;
    }
    return commentedCodeChar || openPar && closePar && containThrows;
  }

  /*
   * works with method declarations, local variables, parameters...
   */
  protected static String getNodeType(Node node) {

    String literalType = "void";

    Node child = node.jjtGetChild(0);

    /* initialisation */
    while (!(child instanceof ASTType) && !(child instanceof ASTResultType)) {
      node = node.jjtGetParent();
      child = node.jjtGetChild(0);
    }

    /* an ASTType node has no child if the parent node type is void */
    if (node.jjtGetChild(0).jjtGetNumChildren() != 0) {

      final ASTType type = node.getFirstDescendantOfType(ASTType.class);
      final String lastToken = type.jjtGetLastToken().toString();
      literalType = lastToken;

      if ("]".equals(lastToken)) { // special case for an array
        literalType = type.jjtGetChild(0).jjtGetChild(0).getImage() + "[]";
      }
      if (">".equals(lastToken)) {
        literalType = type.jjtGetChild(0).jjtGetChild(0).getImage() + "<";
        final List<ASTTypeArgument> arguments = type.findDescendantsOfType(ASTTypeArgument.class);
        if (!arguments.isEmpty()) {
          for (ASTTypeArgument argument : arguments) {

            String argumentType = argument.jjtGetChild(0).jjtGetChild(0).getImage();
            if ("]".equals(argument.jjtGetLastToken().toString())) {
              argumentType = argumentType + "[]";
            }
            literalType = literalType + argumentType + ", ";
          }
          literalType = literalType.substring(0, literalType.length() - 2) + ">";
        }

      }

    }

    return literalType;
  }

  protected final static Vector<Comment> getNodeComments(final Node node, List<Comment> comments) {

    final Vector<Comment> result = new Vector<Comment>();
    final Node body = node.getFirstParentOfType(ASTClassOrInterfaceBodyDeclaration.class);
    final int index = body.jjtGetChildIndex();

    if (index > 0) {
      /*
       * case where the current node is not the first from its type to be declared in
       * the body
       */
      final int numChild = body.jjtGetParent().jjtGetChild(index - 1).jjtGetChild(0)
          .jjtGetNumChildren();
      final Node previous = body.jjtGetParent().jjtGetChild(index - 1).jjtGetChild(0)
          .jjtGetChild(numChild - 1);

      if (previous != null) {
        for (Comment comment : comments) {
          if (previous.getEndLine() < comment.getBeginLine()
              && node.getBeginLine() >= comment.getBeginLine()
              && !isCommentedCode(comment.toString())) {

            result.add(comment);
          }
        }
      }
    } else {
      /* case for the first declared variable/method in the body. */
      final Node sup = body.jjtGetParent();

      if (sup != null) {
        for (final Comment comment : comments) {
          if (sup.getBeginLine() < comment.getBeginLine()
              && node.getBeginLine() >= comment.getBeginLine()
              && !isCommentedCode(comment.toString())) {

            result.add(comment);
          }
        }
      }
    }
    return result;
  }

  protected static String toString(final List<Comment> comments) {

    String result = "";
    for (final Comment comment : comments) {
      result = result + comment.toString() + " ";
    }
    if (result.endsWith(" ")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

}
