/* Generated By:JJTree: Do not edit this line. ASTPackageDeclaration.java Version 4.1 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY= */
package org.orbisgis.core.ui.plugins.views.beanShellConsole.javaManager.parser;

public class ASTPackageDeclaration extends SimpleNode {
	public ASTPackageDeclaration(int id) {
		super(id);
	}

	public ASTPackageDeclaration(JavaParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. **/
	public Object jjtAccept(JavaParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
}
/*
 * JavaCC - OriginalChecksum=90959a0b89352c064e75a48fb59fa7d0 (do not edit this
 * line)
 */
