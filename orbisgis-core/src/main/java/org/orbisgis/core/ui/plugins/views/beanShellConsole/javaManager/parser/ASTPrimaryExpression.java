/* Generated By:JJTree: Do not edit this line. ASTPrimaryExpression.java Version 4.1 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY= */
package org.orbisgis.core.ui.plugins.views.beanShellConsole.javaManager.parser;

public class ASTPrimaryExpression extends SimpleNode {
	public ASTPrimaryExpression(int id) {
		super(id);
	}

	public ASTPrimaryExpression(JavaParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. **/
	public Object jjtAccept(JavaParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
}
/*
 * JavaCC - OriginalChecksum=a6991ae2e3358280e71a442240fedb0d (do not edit this
 * line)
 */
