package local.techsy730.function;

/**
 * The root interface for all function types.<p>
 * 
 * Users of the API will rarely need to deal with this interface directly.
 * It is mostly to facilitate compile time type safety in the absence of variable argument generics.
 * Instead, most users will want to look at the FunctionN interfaces, where N is the number of arguments needed.
 * These provide much "cleaner" interfaces to work with, and deal with complexities of the "generics chaining" mechanisms for the user.
 * 
 * 
 * @author C. Sean Young
 *
 * @param <R> The return type of the method, or Void if this function returns no value.
 *              If the argument type(s) cannot be known at compile time, it is OK for this to be defined as Object or left a raw type.
 * @param <T> The argument type of the method, or an ArgumentPair if there is more than one argument.
 *              Can be Void to indicate no parameters.
 *              See {@link #getArgumentCount()} for more rules about T's type.
 *              If the argument type(s) cannot be known at compile time, it is OK for this to be defined as Object or left a raw type.
 *              If T is left as a raw type OR if it is declared to be an Object due to its type not being knowable until runtime,
 *              then T is not considered well defined at compile time. 
 */
public interface FunctionBase<R, T> extends FunctionTypeWithR<R>
{
    
    /**
     * Returns the number of arguments this function takes. Will always be &gt;=0.<p />
     * If T is well defined at compile time, it must be that:<br />
     * T is of type Void if and only if this method returns 0<br />
     * T is not of type ParameterPair and not Void if and only if this method returns 1<br />
     * T is of type ParameterPair if and only if this method returns a value that is &gt;1<p />
     * 
     * It is highly recommended that this count remains consistent for each concrete implementation,
     * which implies that it remains consistent throughout a FunctionBase's lifetime.
     * <br />
     * If this rule is violated, it is impossible to implement T in a well defined way at compile time.
     * This may unavoidable at times though for functions that do not know which types they can accept until runtime,
     * such as those that wrap arbitrary {@link java.lang.reflect.Method Method} objects
     * 
     * @return the number of arguments this function takes.
     */
    public int getArgumentCount();
    
    /**
     * Returns the runtime type that all return values from this function are assured to be an instance of.
     * If this information is not knowable at runtime, it is fine to just return {@code Object.class}.
     * @return the runtime type that all return values from this function are assured to be an instance of.
     */
    public Class<?> returnType();
    
    /**
     * Returns the expected runtime type that the parameter at the given index this function requires.
     * @param argument the index of the parameter to query
     * @return the expected runtime type that the parameter at the given index this function requires
     * @throws IndexOutOfBoundsException if the index given is &lt;0 or &gt;= {@link #getArgumentCount()}
     */
    public Class<?> getArgumentType(int argument);
    
    public Class<?>[] getArgumentTypes();
    
    /**
     * Calls the function with the argument(s) given, packed into ArgumentPairs if needed.<p />
     * Users of the API will rarely need to deal with this method directly.
     * It is mostly to facilitate compile time type safety in the absence of variable argument generics.
     * The abstract helper classes will deal with the complexities of this method.<p>
     * 
     * This interface gives no stipulation on behavior if the given argument(s)
     * are modified concurrently with the execution of this method.
     * Thus, implementations are free to leave the behavior of this case undefined, though
     * subinterfaces and implementations are free to give such assurances.
     * 
     * @param argument the argument to the function (may be a ParameterPair, indicating more than one parameter, or null indicating no parameters)
     * @return the return value of this function
     * @throws ClassCastException if the parameters were of the wrong types (can only happen if "unsafe cast" or raw types warnings would of been thrown)
     *                              One way this can show itself is if the wrong number of arguments were given
     *                              (which would imply ParameterPairs in the wrong places, thus still falling under type violation),
     *                              though it is valid for an implementation to throw an IllegalArgumentException instead if it detects this case.
     * @throws IllegalArgumentException if number of arguments given does not match up with the expected number of arguments this function expects.
     *                              It is valid for an implementation to throw this exception instead of a ClassCastException if the "unwrapped" argument count
     *                              does not match up with the number of arguments this function takes.
     */
    public R callRoot(T argument);
    
    /**
     * Calls the function with the argument(s) given, not packed into ArgumentPairs.
     * {@code null} arrays should be treated the same as empty arrays.
     * As this method does not have compile time type safety, it is not recommended for general use.
     * Instead, prefer use the more focused, type safe call methods given in subinterfaces.
     * <p />
     * Users of the API will rarely need to deal with this method directly.
     * It is mostly to facilitate a general purpose way to call methods without sacrificing a large amount of performance
     * by dealing with ArgumentPairs
     * The abstract helper classes will deal with the complexities of this method.
     * <p />
     * NOTE, this method should NOT throw an IndexOutOfBoundsException (or subclasses, including the {@link ArrayIndexOutOfBoundsException array specific subclass})
     * due to a bad number of arguments.
     * Thus, implementors must check the argument count first before trying to use the arguments. <p />
     * 
     * This interface gives no stipulation on behavior if the given array
     * is modified concurrently with the execution of this method.
     * Thus, implementations are free to leave the behavior of this case undefined, though
     * subinterfaces and implementations are free to give such assurances.
     * 
     * @param arguments the arguments to the function (not packed into ParameterPairs)
     * @return the return value of this function
     * @throws IllegalArgumentException if number of arguments given does not match up with the expected number of arguments this function expects.
     * @throws ClassCastException if the parameters were of the wrong types this function was expecting.
     */
    public R callUnsafe(Object... arguments);
}
