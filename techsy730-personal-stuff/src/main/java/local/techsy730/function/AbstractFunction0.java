package local.techsy730.function;

public abstract class AbstractFunction0<R> extends AbstractFunctionBase<R, Void> implements Function0<R>
{

    @Override
    public final int getArgumentCount()
    {
        return 0;
    }

    //public abstract R call();

    @Override
    public R callUnsafe(Object... arguments)
    {
        if(arguments == null)
            return call();
        checkArgumentCount(arguments.length);
        return call();
    }
    
    @Override
    public R callRoot(Void argument)
    {
        if(argument != null)
            throw new IllegalArgumentException("Invalid number of arguments. Recieved more than 0, expected 0");
        return call();
    }

}