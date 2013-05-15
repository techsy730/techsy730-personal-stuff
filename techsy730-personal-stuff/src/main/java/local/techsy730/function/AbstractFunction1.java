package local.techsy730.function;

public abstract class AbstractFunction1<R, T> extends AbstractFunctionBase<R, T>
    implements Function1<R, T>, FunctionTypeWithR<R>
{
    @Override
    public abstract R call(T arg);

    @Override
    public Function0<R> bind1(final T argument)
    {
        return 
            new AbstractFunction0<R>()
            {
                @Override
                public final R call()
                {
                    return AbstractFunction1.this.call(argument);
                }
                
                @Override
                public final Class<?> returnType()
                {
                    return AbstractFunction1.this.returnType();
                }
            };
    }

    @Override
    public final int getArgumentCount()
    {
        return 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public R callUnsafe(Object... arguments)
    {
        checkArgumentCount(arguments);
        return call((T)arguments[0]);
    }
    
    @Override
    //We can take a shortcut over the general purpose "unpacking" logic in this case
    public R callRoot(T argument)
    {
        if(argument instanceof ParameterPair)
            throw new IllegalArgumentException("Invalid number of arguments. Recieved more than 1, expected 1");
        return call(argument);
    }

}
