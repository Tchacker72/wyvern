package wyvern.tools.types;

import wyvern.tools.typedAST.transformers.Types.TypeTransformer;
import wyvern.tools.util.TreeWritable;

import java.util.HashSet;
import java.util.Map;

public interface Type extends TreeWritable {
	public boolean subtype(Type other, HashSet<SubtypeRelation> subtypes);
	public boolean subtype(Type other);

	/**
	 * @return whether this type is simple or compound.  Used in toString().
	 */
	public boolean isSimple();

	/**
	 * Gets the children of a composite node
	 * @return The children of the node
	 */
	Map<String, Type> getChildren();
	/**
	 * Clones the current AST node with the given set of children
	 * @param newChildren The children to create
	 * @param transformer
	 * @return The deep-copied Type node
	 */
	Type cloneWithChildren(Map<String, Type> newChildren, TypeTransformer transformer);
}