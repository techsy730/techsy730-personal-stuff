package local.techsy730.function;

public abstract class AbstractFunction3<R, T1, T2, T3> extends AbstractFunctionBase<R, ParameterPair<T1, ParameterPair<T2, T3>>>
    implements Function3<R, T1, T2, T3>, FunctionTypeWithR<R>
{
    @Override
    public abstract R call(T1 arg1, T2 arg2, T3 arg3);
    
    @Override
    public Function2<R, T2, T3> bind1(final T1 argument)
    {
        return 
            new AbstractFunction2<R, T2, T3>()
            {
                @Override
                public final R call(T2 arg2, T3 arg3)
                {
                    return AbstractFunction3.this.call(argument, arg2, arg3);
                }
                
                @Override
                public final Class<?> returnType()
                {
                    return AbstractFunction3.this.returnType();
                }
            };
    }

    @Override
    public Function2<R, T1, T3> bind2(final T2 argument)
    {
        return 
            new AbstractFunction2<R, T1, T3>()
            {
                @Override
                public final R call(T1 arg1, T3 arg3)
                {
                    return AbstractFunction3.this.call(arg1, argument, arg3);
                }
                
                @Override
                public final Class<?> returnType()
                {
                    return AbstractFunction3.this.returnType();
                }
            };
    }
    

    @Override
    public Function2<R, T1, T2> bind3(final T3 argument)
    {
        return 
            new AbstractFunction2<R, T1, T2>()
            {
                @Override
                public final R call(T1 arg1, T2 arg2)
                {
                    return AbstractFunction3.this.call(arg1, arg2, argument);
                }
                
                @Override
                public final Class<?> returnType()
                {
                    return AbstractFunction3.this.returnType();
                }
            };
    }

    @Override
    public final int getArgumentCount()
    {
        return 3;
    }

    @SuppressWarnings("unchecked")
    @Override
    public R callUnsafe(Object... arguments)
    {
        checkArgumentCount(arguments);
        return call((T1)arguments[0], (T2)arguments[1], (T3)arguments[2]);
    }


}
