package local.techsy730.function;

/**
 * This class represents a pair of parameters. When encountered as a generic type argument
 * or as an instance in the argument list to a method call,
 * this indicates that there are actually two or more parameters waiting. It is possible that the second parameter/type is itself another parameter pair,
 * which would need to be recursively "unpacked" with as well.<p>
 * 
 * Users of the API will rarely need to deal with this interface or its subinterfaces or implementors directly.
 * It is mostly to facilitate compile time type safety in the absence of variable argument generics.
 * 
 * @author C. Sean Young
 *
 * @param <T1> The type of the first parameter. Cannot be a ParameterPair or a subclass of it.
 * @param <T2> The type of the second parameter, or another ParameterPair if there are more arguments.
 */
public interface ParameterPair<T1, T2>
{
    public boolean hasParameters();
    
    public T1 getFirst();
    
    public T2 getSecond();
}
