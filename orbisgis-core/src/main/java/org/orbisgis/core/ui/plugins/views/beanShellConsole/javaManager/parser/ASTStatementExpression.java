/* Generated By:JJTree: Do not edit this line. ASTStatementExpression.java Version 4.1 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY= */
package org.orbisgis.core.ui.plugins.views.beanShellConsole.javaManager.parser;

public class ASTStatementExpression extends SimpleNode {
	public ASTStatementExpression(int id) {
		super(id);
	}

	public ASTStatementExpression(JavaParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. **/
	public Object jjtAccept(JavaParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
}
/*
 * JavaCC - OriginalChecksum=270d396d2a57ea3efcbfb288c40c658c (do not edit this
 * line)
 */
