/* Generated By:JJTree: Do not edit this line. ASTRelationalExpression.java Version 4.1 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY= */
package org.orbisgis.core.ui.plugins.views.beanShellConsole.javaManager.parser;

public class ASTRelationalExpression extends SimpleNode {
	public ASTRelationalExpression(int id) {
		super(id);
	}

	public ASTRelationalExpression(JavaParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. **/
	public Object jjtAccept(JavaParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
}
/*
 * JavaCC - OriginalChecksum=4fc22b02798c1d2b6eaceeaba5da3594 (do not edit this
 * line)
 */
