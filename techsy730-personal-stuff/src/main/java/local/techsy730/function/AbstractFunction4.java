package local.techsy730.function;

//TODO Write a code generator for this, as it is getting rather absurd to do all this by hand
public abstract class AbstractFunction4<R, T1, T2, T3, T4>
    extends AbstractFunctionBase<R, ParameterPair<T1, ParameterPair<T2, ParameterPair<T3, T4>>>>
    implements Function4<R, T1, T2, T3, T4>, FunctionTypeWithR<R>
{
    @Override
    public abstract R call(T1 arg1, T2 arg2, T3 arg3, T4 arg4);
    
    @Override
    public Function3<R, T2, T3, T4> bind1(final T1 argument)
    {
        return 
            new AbstractFunction3<R, T2, T3, T4>()
            {
                @Override
                public final R call(T2 arg2, T3 arg3, T4 arg4)
                {
                    return AbstractFunction4.this.call(argument, arg2, arg3, arg4);
                }
                
                @Override
                public final Class<?> returnType()
                {
                    return AbstractFunction4.this.returnType();
                }
            };
    }

    @Override
    public Function3<R, T1, T3, T4> bind2(final T2 argument)
    {
        return 
            new AbstractFunction3<R, T1, T3, T4>()
            {
                @Override
                public final R call(T1 arg1, T3 arg3, T4 arg4)
                {
                    return AbstractFunction4.this.call(arg1, argument, arg3, arg4);
                }
                
                @Override
                public final Class<?> returnType()
                {
                    return AbstractFunction4.this.returnType();
                }
            };
    }
    
    @Override
    public Function3<R, T1, T2, T4> bind3(final T3 argument)
    {
        return 
            new AbstractFunction3<R, T1, T2, T4>()
            {
                @Override
                public final R call(T1 arg1, T2 arg2, T4 arg4)
                {
                    return AbstractFunction4.this.call(arg1, arg2, argument, arg4);
                }
                
                @Override
                public final Class<?> returnType()
                {
                    return AbstractFunction4.this.returnType();
                }
            };
    }

    @Override
    public Function3<R, T1, T2, T3> bind4(final T4 argument)
    {
        return 
            new AbstractFunction3<R, T1, T2, T3>()
            {
                @Override
                public final R call(T1 arg1, T2 arg2, T3 arg3)
                {
                    return AbstractFunction4.this.call(arg1, arg2, arg3, argument);
                }
                
                @Override
                public final Class<?> returnType()
                {
                    return AbstractFunction4.this.returnType();
                }
            };
    }

    @Override
    public final int getArgumentCount()
    {
        return 4;
    }

    @Override
    public R callUnsafe(Object... arguments)
    {
        checkArgumentCount(arguments);
        return call((T1)arguments[0], (T2)arguments[1], (T3)arguments[2], (T4)arguments[3]);
    }



}
