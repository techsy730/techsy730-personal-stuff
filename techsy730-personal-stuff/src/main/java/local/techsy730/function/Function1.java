package local.techsy730.function;

public interface Function1<R, T>
    extends
        OneOrMoreParameterFunction<R, T,
            Void, //AggregateWithNoFirstType
            T, //AggregateType
            Function0<R>>, //Bound1stFunctionType
        FunctionBase<R, T>, FunctionN, FunctionTypeWithR<R>
{
    public R call(T arg);
    
    @Override
    public Function0<R> bind1(final T argument);
}
