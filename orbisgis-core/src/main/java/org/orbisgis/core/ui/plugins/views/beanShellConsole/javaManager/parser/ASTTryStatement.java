/* Generated By:JJTree: Do not edit this line. ASTTryStatement.java Version 4.1 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY= */
package org.orbisgis.core.ui.plugins.views.beanShellConsole.javaManager.parser;

public class ASTTryStatement extends SimpleNode {
	public ASTTryStatement(int id) {
		super(id);
	}

	public ASTTryStatement(JavaParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. **/
	public Object jjtAccept(JavaParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
}
/*
 * JavaCC - OriginalChecksum=e7e27e68a1e878d940a41449017ed19f (do not edit this
 * line)
 */
