package local.techsy730.function;

public abstract class AbstractFunction0<R> extends AbstractFunctionBase<R, Void> implements Function0<R>, FunctionTypeWithR<R>
{
    @Override
    public abstract R call();
    
    @Override
    public final int getArgumentCount()
    {
        return 0;
    }

    @Override
    public R callUnsafe(Object... arguments)
    {
        if(arguments == null)
            return call();
        checkArgumentCount(arguments.length);
        return call();
    }
    
    @Override
    //We can take a shortcut over the general purpose "unpacking" logic in this case
    public R callRoot(Void argument)
    {
        if(argument != null)
            throw new IllegalArgumentException("Invalid number of arguments. Recieved more than 0, expected 0");
        return call();
    }
    
    @Override
    public final Class<?>[] getArgumentTypes()
    {
        return local.techsy730.util.ArrayUtils.EMPTY_CLASS_ARRAY;
    }
    
    @Override
    public final Class<?> getArgumentType(int index)
    {
        throw new IndexOutOfBoundsException("Asked for parameter " + index + "'s type; but has no parameters");
    }

}
