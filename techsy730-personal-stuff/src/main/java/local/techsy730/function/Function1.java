package local.techsy730.function;

public interface Function1<R, T>
    extends
        FunctionBase<R, T>,
        
        OneOrMoreParameterFunction<R, T,
            Void, //AggregateWithNoFirstType
            T, //AggregateType
            Function0<R>>, //Bound1stFunctionType
        FunctionN, FunctionTypeWithR<R>
{
    public R call(T arg);
    
    @Override
    public Function0<R> bind1(final T argument);
}
