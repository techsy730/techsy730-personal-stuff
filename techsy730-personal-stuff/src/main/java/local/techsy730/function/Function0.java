package local.techsy730.function;

/**
 * A function that takes no arguments.
 * 
 * @author C. Sean Young
 *
 * @param <R> the return type of this function.
 */
public interface Function0<R> extends FunctionBase<R, Void>, FunctionN, FunctionTypeWithR<R>
{
    public R call();
}
