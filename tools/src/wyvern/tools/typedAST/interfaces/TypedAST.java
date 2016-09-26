package wyvern.tools.typedAST.interfaces;

import java.util.Map;
import java.util.Optional;

import wyvern.target.corewyvernIL.support.TopLevelContext;
import wyvern.tools.errors.HasLocation;
import wyvern.tools.parsing.HasParser;
import wyvern.tools.typedAST.transformers.GenerationEnvironment;
import wyvern.tools.typedAST.transformers.ILWriter;
import wyvern.tools.types.Environment;
import wyvern.tools.types.Type;
import wyvern.tools.util.EvaluationEnvironment;
import wyvern.tools.util.TreeWritable;

public interface TypedAST extends TreeWritable, HasLocation {

	/**
	 * Gets the children of a composite node
	 * @return The children of the node
	 */
	Map<String, TypedAST> getChildren();
	/**
	 * Clones the current AST node with the given set of children
	 * @param newChildren The children to create
	 * @return The deep-copied AST node
	 */
	TypedAST cloneWithChildren(Map<String, TypedAST> newChildren);

	public default void genTopLevel(TopLevelContext tlc) {
		throw new RuntimeException("genTopLevel not implemented for " + this.getClass());
	}
}
