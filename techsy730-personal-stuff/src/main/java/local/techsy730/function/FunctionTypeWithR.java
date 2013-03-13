package local.techsy730.function;

/**
 * A marker interface for the built-in Function types that leave their return type R parameterized.
 * Used in a few cases to help reflectively fetch compile time type arguments given to superclasses.
 * All immediate implementors of this interface MUST have their return type as their first type parameter.
 * @author C. Sean Young
 *
 * @param <R> the return type
 */
interface FunctionTypeWithR<R>
{}
