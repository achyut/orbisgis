/* Generated By:JJTree: Do not edit this line. ASTConditionalAndExpression.java Version 4.1 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY= */
package org.orbisgis.core.javaManager.parser;

public class ASTConditionalAndExpression extends SimpleNode {
	public ASTConditionalAndExpression(int id) {
		super(id);
	}

	public ASTConditionalAndExpression(JavaParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. **/
	public Object jjtAccept(JavaParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
}
/*
 * JavaCC - OriginalChecksum=29a5497518620ea2fd19b762d5f03b23 (do not edit this
 * line)
 */