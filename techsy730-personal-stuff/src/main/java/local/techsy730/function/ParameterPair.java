package local.techsy730.function;

/**
 * This class represents a pair of parameters. When encountered as a generic type argument
 * or as an instance in the argument list to a method call,
 * this indicates that there are actually two or more parameters waiting. It is possible that the second parameter/type is itself another parameter pair,
 * which would need to be recursively "unpacked" with as well.<p>
 * 
 * As this interface is treated specially by the function framework, one should avoid trying to use these as parameters to functions
 * unless needed to model additional parameters.
 * This is especially true for the types and arguments of the FunctionN interfaces,
 * and for the arguments for the {@link FunctionBase#callUnsafe(Object...) callUnsafe(Object...)} method.<br>
 * Note, this means it is currently impossible to model a function that takes a ParameterPair as an argument itself.
 * If needed, a workaround for this may be added in future releases.
 * 
 * Users of the API will rarely need to deal with this interface or its subinterfaces or implementors directly.
 * It is mostly to facilitate compile time type safety in the absence of variable argument generics.
 * 
 * @author C. Sean Young
 *
 * @param <T1> The type of the first parameter. Cannot be Void or a ParameterPair or a subclass of it.
 * @param <T2> The type of the second parameter, or another ParameterPair if there are more arguments. Cannot be Void.
 */
public interface ParameterPair<T1, T2>
{
    public boolean hasParameters();
    
    public T1 getFirst();
    
    public T2 getSecond();
}
